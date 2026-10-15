package org.papiricoh.create_nuclearindustry.propulsion.blockentity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.accelerator.energy.FeBuffer;
import org.papiricoh.create_nuclearindustry.accelerator.physics.VialBattery;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterAnnihilation;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolder;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.propulsion.EngineStructure;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionConfig;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionRegistry;
import org.papiricoh.create_nuclearindustry.propulsion.physics.AntimatterRocketModel;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.PlantAlarm;

import java.util.List;

/**
 * The antimatter engine's controller: an annihilation chamber fed by an antimatter conduit or a vial in
 * its port, behind it the magnetic coils that turn the blast into thrust and the magnetic nozzle (see
 * {@link AntimatterRocketModel}). The coils run on Forge Energy; without it, or worn through by heat, the
 * field fails, and after a short quench alarm whatever the chamber holds annihilates where the engine
 * really is. The gamma rays the coils miss stream out behind the nozzle and leave the ground the jet
 * hits slightly active.
 */
public class AntimatterEngineBlockEntity extends RocketEngineBlockEntity implements AntimatterHolder {
    private static final String KEY = "create_nuclearindustry.antimatter_engine.";
    private static final int ENERGY = 200_000;
    /** Ticks between gamma pulses behind the nozzle. */
    private static final int GAMMA_INTERVAL = 10;
    /** How far behind the nozzle its gamma rays reach, blocks, and how wide the cone is (cosine of the half angle). */
    private static final double GAMMA_REACH = 24.0, GAMMA_CONE = 0.9;

    private final FeBuffer energy = new FeBuffer(ENERGY, this::setChanged);
    private final ItemStackHandler port = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof AntimatterVialItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return AntimatterVialItem.loaded(getStackInSlot(slot)) ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private double stored;
    private double coil = AntimatterRocketModel.AMBIENT;
    private double wear;
    private double fed;
    private double gamma;
    private boolean confined;
    /** Ticks left of the quench alarm, or -1. */
    private int quench = -1;
    private boolean annihilating;

    public AntimatterEngineBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionRegistry.ANTIMATTER_ENGINE_ENTITY.get(), pos, state);
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public ItemStackHandler port() {
        return port;
    }

    @Override
    protected Block segmentBlock() {
        return PropulsionRegistry.MAGNETIC_COIL.get();
    }

    @Override
    protected Block nozzleBlock() {
        return PropulsionRegistry.MAGNETIC_NOZZLE.get();
    }

    @Override
    protected org.papiricoh.create_nuclearindustry.advancement.NuclearEvent flightEvent() {
        return org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.ANTIMATTER_THRUST;
    }

    @Override
    protected int maxSegments() {
        return AntimatterRocketModel.MAX_COILS;
    }

    @Override
    protected double run(EngineStructure structure) {
        AntimatterRocketModel.Params p = PropulsionConfig.antimatter();
        AntimatterRocketModel.Step step = AntimatterRocketModel.tick(throttle, structure.segments(), stored,
                energy.getEnergyStored(), coil, wear, p);
        energy.take(step.energy());
        confined = step.confined();
        coil = step.coil();
        wear = step.wear();
        fed = step.fed();
        gamma = step.gamma();
        stored = Math.max(0, stored - step.fed());
        tickPort();
        if (fed > 0) {
            releaseActivity(fed * PropulsionConfig.activationPerNg);
            if (level.getGameTime() % GAMMA_INTERVAL == 0) {
                gammaPulse(gamma * GAMMA_INTERVAL / 20.0);
            }
            if (level.getGameTime() % 40 == 0) {
                Vec3 at = worldMouth();
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.5f, 1.6f);
            }
        }
        tickQuench();
        return confined ? step.thrust() : 0;
    }

    @Override
    protected void idle() {
        // Without its coils behind it the chamber has no field at all.
        confined = false;
        fed = 0;
        gamma = 0;
        coil += (AntimatterRocketModel.AMBIENT - coil) * PropulsionConfig.antimatter().coilResponse();
        tickPort();
        tickQuench();
    }

    /** A loaded chamber without its field rings the quench alarm, then goes off. */
    private void tickQuench() {
        if (confined || stored <= 0 || annihilating) {
            quench = -1;
            return;
        }
        if (quench < 0) {
            quench = PropulsionConfig.quenchTicks;
            PlantAlarm.raise(level, worldPosition);
            level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0f, 0.5f);
        } else if (--quench <= 0) {
            annihilate();
        }
    }

    /** What the chamber holds annihilates; the engine and much of what is around it go with it. */
    private void annihilate() {
        if (!(level instanceof ServerLevel server) || annihilating) {
            return;
        }
        annihilating = true;
        double charge = stored + AntimatterVialItem.antimatter(port.getStackInSlot(0));
        stored = 0;
        port.setStackInSlot(0, ItemStack.EMPTY);
        fail("confinement");
        AntimatterAnnihilation.schedule(server, Vec3.atCenterOf(worldPosition), charge);
    }

    /** Empties a vial in the port into the chamber while the field holds, and keeps its battery up. */
    private void tickPort() {
        ItemStack vial = port.getStackInSlot(0);
        if (!(vial.getItem() instanceof AntimatterVialItem)) {
            return;
        }
        VialBattery.Params vp = AntimatterConfig.vial();
        double inVial = AntimatterVialItem.antimatter(vial);
        if (powered() && VialBattery.loaded(inVial)) {
            double moved = VialBattery.drainStep(vp, inVial, space());
            inVial -= moved;
            stored += moved;
        }
        int charge = VialBattery.tick(vp, AntimatterVialItem.charge(vial), inVial, powered());
        if (VialBattery.annihilates(charge, inVial)) {
            port.setStackInSlot(0, ItemStack.EMPTY);
            AntimatterAnnihilation.schedule((ServerLevel) level, Vec3.atCenterOf(worldPosition), inVial);
            return;
        }
        ItemStack updated = vial.copy();
        AntimatterVialItem.set(updated, inVial, charge);
        if (!ItemStack.isSameItemSameComponents(updated, vial)) {
            port.setStackInSlot(0, updated);
        }
    }

    /** The gamma rays the coils miss, in a cone behind the nozzle, through whatever stands in the way. */
    private void gammaPulse(double doseAtOne) {
        if (!Config.radiationEnabled || doseAtOne <= 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 from = worldMouth();
        Vec3 out = worldExhaust();
        AABB reach = new AABB(from, from.add(out.scale(GAMMA_REACH))).inflate(GAMMA_REACH * 0.5);
        for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, reach, e -> e.isAlive() && !e.isSpectator())) {
            Vec3 eyes = entity.getEyePosition();
            Vec3 to = eyes.subtract(from);
            double distance = to.length();
            if (distance > GAMMA_REACH || distance < 1.0e-3 || to.normalize().dot(out) < GAMMA_CONE) {
                continue;
            }
            double dose = doseAtOne / (1 + distance * distance / 16.0) * RadiationRay.transmission(server, from, eyes);
            if (entity instanceof Player player) {
                RadiationHandler.addPromptDose(player, (float) dose);
            } else if (dose >= Config.radiationSicknessThreshold) {
                entity.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIATION_SICKNESS, 20 * 30, 0));
            }
        }
    }

    // ------------------------------------------------------------------ antimatter holder

    @Override
    public double stored() {
        return stored;
    }

    @Override
    public double capacity() {
        return PropulsionConfig.chamberCapacity;
    }

    @Override
    public double insert(double ng, boolean simulate) {
        double taken = Math.min(Math.max(0, ng), space());
        if (!simulate && taken > 0) {
            if (!powered() && level instanceof ServerLevel server) {
                // No field to hold it: it annihilates as it comes in.
                AntimatterAnnihilation.schedule(server, Vec3.atCenterOf(worldPosition), taken);
                return taken;
            }
            stored += taken;
            setChanged();
        }
        return taken;
    }

    @Override
    public double extract(double ng, boolean simulate) {
        return 0;
    }

    /** The coils hold their field: formed, not failed, with energy for at least their own share. */
    @Override
    public boolean powered() {
        // segments() and isFormed() read the synced copy on the client, where the goggles ask.
        return isFormed() && !failed() && wear < 1.0
                && energy.getEnergyStored() >= AntimatterRocketModel.energyNeed(segments(), 0, PropulsionConfig.antimatter());
    }

    @Override
    public boolean givesAntimatter() {
        return false;
    }

    @Override
    public boolean idleForRepair() {
        return fed == 0 && coil < PropulsionConfig.antimatter().coilLimit() * 0.5 && stored <= 0;
    }

    @Override
    public boolean repair(double amount) {
        if (wear <= 0) {
            return false;
        }
        wear = Math.max(0, wear - amount);
        if (wear < 1.0 && !annihilating) {
            clearFailure();
        }
        sendData();
        return true;
    }

    /** For tests: the field buffer runs dry at once. */
    public void drainEnergyForTest() {
        energy.set(0);
    }

    public double coilTemperature() {
        return coil;
    }

    public double wear() {
        return wear;
    }

    public boolean confined() {
        return confined;
    }

    public int quenchTicks() {
        return quench;
    }

    // ------------------------------------------------------------------ by hand

    public void insertVial(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!port.getStackInSlot(0).isEmpty()) {
            player.displayClientMessage(Component.translatable("create_nuclearindustry.penning_trap.port_full"), true);
            return;
        }
        port.setStackInSlot(0, held.copyWithCount(1));
        if (!player.isCreative()) {
            held.shrink(1);
        }
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    public void takeVial(Player player) {
        ItemStack vial = port.getStackInSlot(0);
        if (vial.isEmpty()) {
            player.displayClientMessage(Component.translatable(KEY + "status_line", fmt(stored, 3), Component.translatable(statusKey()),
                    fmt(thrust(), 0)), true);
            return;
        }
        port.setStackInSlot(0, ItemStack.EMPTY);
        if (!player.getInventory().add(vial)) {
            player.drop(vial, false);
        }
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    // ------------------------------------------------------------------ control room

    @Override
    public TargetKind plantKind() {
        return TargetKind.ANTIMATTER_ENGINE;
    }

    @Override
    public boolean plantFormed() {
        return isFormed();
    }

    @Override
    public MonitorReading plantSample() {
        float[] v = new float[MonitorReading.ENGINE_VALUES];
        v[MonitorReading.E_THRUST] = (float) thrust();
        v[MonitorReading.E_THROTTLE] = (float) throttle();
        v[MonitorReading.E_TEMPERATURE] = (float) coil;
        v[MonitorReading.E_LIMIT] = (float) PropulsionConfig.antimatter().coilLimit();
        v[MonitorReading.E_WEAR] = (float) wear;
        v[MonitorReading.E_FUEL] = (float) (stored / Math.max(1.0e-9, capacity()));
        v[MonitorReading.E_FLOW] = (float) (fed * 1200);
        v[MonitorReading.E_FAILED] = failed() ? 1 : 0;
        v[MonitorReading.E_STORED] = (float) stored;
        v[MonitorReading.E_ENERGY] = energy.getEnergyStored() / (float) ENERGY;
        return new MonitorReading(TargetKind.ANTIMATTER_ENGINE, statusKey(), v, v[MonitorReading.E_THROTTLE], v[MonitorReading.E_FUEL]);
    }

    @Override
    public String statusKey() {
        if (failed()) {
            return KEY + "status.failed";
        }
        if (quench >= 0) {
            return KEY + "status.quench";
        }
        if (!isFormed()) {
            return KEY + "status.not_formed";
        }
        if (!powered()) {
            return KEY + "status.no_field";
        }
        if (thrust() > 0) {
            return coil > PropulsionConfig.antimatter().coilLimit() ? KEY + "status.overheating" : KEY + "status.firing";
        }
        return KEY + "status.idle";
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.antimatter_engine"))
                .withStyle(ChatFormatting.GOLD));
        boolean alarm = failed() || quench >= 0;
        tooltip.add(Component.literal("     ").append(Component.translatable(statusKey())).withStyle(alarm ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (!isFormed()) {
            return true;
        }
        tooltip.add(line("coils", segments(), fmt(AntimatterRocketModel.efficiency(segments()) * 100, 0)));
        tooltip.add(line("thrust", fmt(thrust(), 0)).withStyle(thrust() > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(line("throttle", fmt(throttle() * 100, 0)));
        tooltip.add(line("chamber", fmt(stored, 3), fmt(capacity(), 0)).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(line("burn", fmt(fed * 1200, 2)));
        tooltip.add(line("energy", energy.getEnergyStored(), ENERGY));
        boolean hot = coil > PropulsionConfig.antimatter().coilLimit();
        tooltip.add(line("coil", fmt(coil, 0), fmt(PropulsionConfig.antimatter().coilLimit(), 0)).withStyle(hot ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (wear > 0) {
            tooltip.add(line("wear", fmt(wear * 100, 0)).withStyle(wear > 0.5 ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        if (quench >= 0) {
            tooltip.add(line("quench", fmt(quench / 20.0, 1)).withStyle(ChatFormatting.RED));
        }
        return true;
    }

    private static net.minecraft.network.chat.MutableComponent line(String key, Object... args) {
        return Component.literal("     ").append(Component.translatable(KEY + "goggles." + key, args)).withStyle(ChatFormatting.GRAY);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Stored", stored);
        tag.putDouble("Coil", coil);
        tag.putDouble("Wear", wear);
        tag.putDouble("Fed", fed);
        tag.putBoolean("Confined", confined);
        tag.putInt("Quench", quench);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.put("Port", port.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        stored = tag.getDouble("Stored");
        coil = tag.contains("Coil") ? tag.getDouble("Coil") : AntimatterRocketModel.AMBIENT;
        wear = tag.getDouble("Wear");
        fed = tag.getDouble("Fed");
        confined = tag.getBoolean("Confined");
        quench = tag.contains("Quench") ? tag.getInt("Quench") : -1;
        energy.set(tag.getInt("Energy"));
        port.deserializeNBT(registries, tag.getCompound("Port"));
    }

    @Override
    public void destroy() {
        if (org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly.isMoving()) {
            super.destroy();
            return;
        }
        super.destroy();
        if (level instanceof ServerLevel server && !annihilating) {
            double charge = stored + AntimatterVialItem.antimatter(port.getStackInSlot(0));
            if (charge > 0) {
                annihilating = true;
                AntimatterAnnihilation.schedule(server, Vec3.atCenterOf(worldPosition), charge);
            } else if (!port.getStackInSlot(0).isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, port.getStackInSlot(0));
            }
        }
    }
}

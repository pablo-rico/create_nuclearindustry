package org.papiricoh.create_nuclearindustry.propulsion.blockentity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.propulsion.EngineStructure;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionConfig;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionRegistry;
import org.papiricoh.create_nuclearindustry.propulsion.physics.ThermalRocketModel;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;

import java.util.List;

/**
 * The nuclear thermal engine's controller. Pipes fill it with hot primary coolant from a reactor's loop
 * and with propellant (water, deuterium or hydrogen) and take the cooled primary back; the heat
 * exchanger segments behind it heat the propellant, which leaves through the nozzle (see
 * {@link ThermalRocketModel}). The exhaust carries a trace of the primary's activity to wherever the
 * jet lands; a nozzle run too hot wears through and blows up, contaminating the ground around it.
 */
public class ThermalEngineBlockEntity extends RocketEngineBlockEntity {
    public static final int TANK = 8_000;
    private static final String KEY = "create_nuclearindustry.thermal_engine.";

    private final FluidTank hotTank = tank(NuclearFluidHelper::isHotPrimary);
    private final FluidTank coldTank = tank(NuclearFluidHelper::isPrimaryCoolant);
    private final FluidTank propellantTank = tank(stack -> propellantOf(stack.getFluid()) != null);
    private final IFluidHandler fluids = new Handler();
    private double nozzle = ThermalRocketModel.AMBIENT;
    private double wear;
    private double chamber = ThermalRocketModel.AMBIENT;
    private int flow;

    public ThermalEngineBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionRegistry.THERMAL_ENGINE_ENTITY.get(), pos, state);
    }

    private FluidTank tank(java.util.function.Predicate<FluidStack> valid) {
        return new FluidTank(TANK, valid) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    /** The propellant a fluid is, or null if it is none. */
    @Nullable
    public static ThermalRocketModel.Propellant propellantOf(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return ThermalRocketModel.Propellant.WATER;
        }
        if (fluid == AllNuclearFluids.DEUTERIUM.get()) {
            return ThermalRocketModel.Propellant.DEUTERIUM;
        }
        if (fluid == AllNuclearFluids.HYDROGEN.get()) {
            return ThermalRocketModel.Propellant.HYDROGEN;
        }
        return null;
    }

    public IFluidHandler fluids() {
        return fluids;
    }

    @Override
    protected Block segmentBlock() {
        return PropulsionRegistry.THERMAL_EXCHANGER.get();
    }

    @Override
    protected Block nozzleBlock() {
        return PropulsionRegistry.THERMAL_NOZZLE.get();
    }

    @Override
    protected org.papiricoh.create_nuclearindustry.advancement.NuclearEvent flightEvent() {
        return org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.NUCLEAR_FLIGHT;
    }

    @Override
    protected int maxSegments() {
        return ThermalRocketModel.MAX_SEGMENTS;
    }

    @Override
    protected double run(EngineStructure structure) {
        ThermalRocketModel.Params p = PropulsionConfig.thermal();
        ThermalRocketModel.Propellant propellant = propellantOf(propellantTank.getFluid().getFluid());
        int coldSpace = coldTank.fill(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), TANK), IFluidHandler.FluidAction.SIMULATE);
        ThermalRocketModel.Step step = ThermalRocketModel.tick(throttle, structure.segments(), propellant,
                propellantTank.getFluidAmount(), hotTank.getFluidAmount(), coldSpace, nozzle, wear, p);
        if (step.propellantUsed() > 0) {
            propellantTank.drain(step.propellantUsed(), IFluidHandler.FluidAction.EXECUTE);
            hotTank.drain(step.primaryCooled(), IFluidHandler.FluidAction.EXECUTE);
            coldTank.fill(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), step.primaryCooled()), IFluidHandler.FluidAction.EXECUTE);
            releaseActivity(step.contamination());
            if (level.getGameTime() % 30 == 0) {
                Vec3 at = worldMouth();
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS,
                        (float) (0.4 + 0.8 * step.share(structure.segments(), p)), 0.5f);
            }
        }
        flow = step.propellantUsed();
        chamber = step.chamber();
        nozzle = step.nozzle();
        wear = step.wear();
        if (step.failed()) {
            blowUp();
            return 0;
        }
        return step.thrust();
    }

    @Override
    protected void idle() {
        flow = 0;
        chamber = ThermalRocketModel.AMBIENT;
        nozzle += (ThermalRocketModel.AMBIENT - nozzle) * PropulsionConfig.thermal().nozzleResponse();
    }

    /** The nozzle gives way: the engine bursts where it really is, spilling the primary's activity around it. */
    private void blowUp() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        fail("nozzle");
        Vec3 at = worldMouth();
        BlockPos ground = BlockPos.containing(at);
        float activity = (float) Math.min(20, 2 + hotTank.getFluidAmount() / 500.0);
        hotTank.setFluid(FluidStack.EMPTY);
        server.explode(null, at.x, at.y, at.z, PropulsionConfig.thermalFailurePower, Level.ExplosionInteraction.BLOCK);
        Contamination.fallout(server, ground, 8, activity);
    }

    @Override
    public boolean idleForRepair() {
        return flow == 0 && nozzle < PropulsionConfig.thermal().nozzleLimit() * 0.5;
    }

    @Override
    public boolean repair(double amount) {
        if (wear <= 0 && !failed()) {
            return false;
        }
        wear = Math.max(0, wear - amount);
        if (wear < 1.0) {
            clearFailure();
        }
        sendData();
        return true;
    }

    /** For tests: a nozzle already this worn and this hot. */
    public void wearForTest(double wear, double nozzle) {
        this.wear = wear;
        this.nozzle = nozzle;
    }

    public double nozzleTemperature() {
        return nozzle;
    }

    public double wear() {
        return wear;
    }

    public int flow() {
        return flow;
    }

    public FluidTank hotTank() {
        return hotTank;
    }

    public FluidTank coldTank() {
        return coldTank;
    }

    public FluidTank propellantTank() {
        return propellantTank;
    }

    // ------------------------------------------------------------------ control room

    @Override
    public TargetKind plantKind() {
        return TargetKind.THERMAL_ENGINE;
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
        v[MonitorReading.E_TEMPERATURE] = (float) nozzle;
        v[MonitorReading.E_LIMIT] = (float) PropulsionConfig.thermal().nozzleLimit();
        v[MonitorReading.E_WEAR] = (float) wear;
        v[MonitorReading.E_FUEL] = propellantTank.getFluidAmount() / (float) TANK;
        v[MonitorReading.E_FLOW] = flow;
        v[MonitorReading.E_FAILED] = failed() ? 1 : 0;
        return new MonitorReading(TargetKind.THERMAL_ENGINE, statusKey(), v, v[MonitorReading.E_THROTTLE],
                (float) (nozzle / Math.max(1, PropulsionConfig.thermal().nozzleLimit())));
    }

    @Override
    public String statusKey() {
        if (failed()) {
            return KEY + "status.failed";
        }
        if (!isFormed()) {
            return KEY + "status.not_formed";
        }
        if (thrust() > 0) {
            return nozzle > PropulsionConfig.thermal().nozzleLimit() ? KEY + "status.overheating" : KEY + "status.firing";
        }
        return KEY + "status.idle";
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.nuclear_thermal_engine"))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("     ").append(Component.translatable(statusKey())).withStyle(failed() ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (!isFormed()) {
            return true;
        }
        tooltip.add(line("segments", segments()));
        tooltip.add(line("thrust", fmt(thrust(), 0)).withStyle(thrust() > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(line("throttle", fmt(throttle() * 100, 0)));
        tooltip.add(line("flow", flow));
        boolean hot = nozzle > PropulsionConfig.thermal().nozzleLimit();
        tooltip.add(line("nozzle", fmt(nozzle, 0), fmt(PropulsionConfig.thermal().nozzleLimit(), 0))
                .withStyle(hot ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (wear > 0) {
            tooltip.add(line("wear", fmt(wear * 100, 0)).withStyle(wear > 0.5 ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        tooltip.add(line("hot_primary", hotTank.getFluidAmount(), TANK));
        tooltip.add(line("propellant", propellantTank.isEmpty() ? Component.translatable(KEY + "none")
                : propellantTank.getFluid().getHoverName(), propellantTank.getFluidAmount()));
        return true;
    }

    private static net.minecraft.network.chat.MutableComponent line(String key, Object... args) {
        return Component.literal("     ").append(Component.translatable(KEY + "goggles." + key, args)).withStyle(ChatFormatting.GRAY);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Hot", hotTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Cold", coldTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Propellant", propellantTank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("Nozzle", nozzle);
        tag.putDouble("Wear", wear);
        tag.putDouble("Chamber", chamber);
        tag.putInt("Flow", flow);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        hotTank.readFromNBT(registries, tag.getCompound("Hot"));
        coldTank.readFromNBT(registries, tag.getCompound("Cold"));
        propellantTank.readFromNBT(registries, tag.getCompound("Propellant"));
        nozzle = tag.contains("Nozzle") ? tag.getDouble("Nozzle") : ThermalRocketModel.AMBIENT;
        wear = tag.getDouble("Wear");
        chamber = tag.getDouble("Chamber");
        flow = tag.getInt("Flow");
    }

    /** Hot primary and propellant go in, the cooled primary comes out. */
    private final class Handler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 3;
        }

        private FluidTank tankAt(int tank) {
            return switch (tank) {
                case 0 -> hotTank;
                case 1 -> coldTank;
                default -> propellantTank;
            };
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tankAt(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank != 1 && tankAt(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (NuclearFluidHelper.isHotPrimary(resource)) {
                return hotTank.fill(resource, action);
            }
            return propellantOf(resource.getFluid()) != null ? propellantTank.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return NuclearFluidHelper.isPrimaryCoolant(resource) ? coldTank.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return coldTank.drain(maxDrain, action);
        }
    }
}

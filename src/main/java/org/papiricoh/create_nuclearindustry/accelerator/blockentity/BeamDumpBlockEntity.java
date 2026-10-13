package org.papiricoh.create_nuclearindustry.accelerator.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTerminal;
import org.papiricoh.create_nuclearindustry.accelerator.physics.ActivationModel;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;

import java.util.List;
import java.util.Locale;

/**
 * Where a beamline ends when there is nothing to hit: a block of graphite and copper that soaks up
 * the whole beam. While the beam runs it gives off a prompt dose in proportion to the power it takes;
 * the activation it builds up keeps radiating after the beam stops and decays with its half-life,
 * also while its chunk is unloaded. Breaking an activated dump leaves radioactive waste and
 * contaminates the ground around it.
 */
public class BeamDumpBlockEntity extends SmartBlockEntity implements BeamTerminal, RadiationEmitter, IHaveGoggleInformation {
    /** Fallout per unit of activity when an activated part is broken, and how long it lasts. */
    public static final double CONTAMINATION_PER_ACTIVITY = 0.05;
    public static final int CONTAMINATION_TICKS = 6000;
    private static final int SYNC_INTERVAL = 20;

    private double activity;
    private double incomingPower;
    private double incomingEnergy;
    private long lastBeamTick = Long.MIN_VALUE;
    private long lastUpdate = Long.MIN_VALUE;
    private double syncedActivity;
    private double syncedPower;

    public BeamDumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            RadiationEmitters.register(this);
        }
    }

    @Override
    public void invalidate() {
        if (level != null && !level.isClientSide) {
            RadiationEmitters.unregister(this);
        }
        super.invalidate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        ActivationModel.Params params = AcceleratorConfig.activation();
        if (lastUpdate != Long.MIN_VALUE && now - lastUpdate > 1) {
            activity = ActivationModel.decay(params, activity, now - lastUpdate - 1);
        }
        lastUpdate = now;
        if (now - lastBeamTick > 1) {
            incomingPower = 0;
            incomingEnergy = 0;
        }
        activity = ActivationModel.tick(params, activity, incomingPower);
        if (now % SYNC_INTERVAL == 0 && (Math.abs(activity - syncedActivity) > 0.01 * Math.max(1, syncedActivity)
                || Math.abs(incomingPower - syncedPower) > 0.01 * Math.max(1, syncedPower))) {
            syncedActivity = activity;
            syncedPower = incomingPower;
            sendData();
        }
    }

    @Override
    public void receiveBeam(double energyMev, double current, double powerKw) {
        if (level == null) {
            return;
        }
        incomingPower = Math.max(0, powerKw);
        incomingEnergy = Math.max(0, energyMev);
        lastBeamTick = level.getGameTime();
    }

    @Override
    public double activity() {
        return activity;
    }

    /** Beam power the dump took on the last tick, kW. */
    public double incomingPower() {
        return incomingPower;
    }

    /** Sets the activity (game tests). */
    public void setActivity(double activity) {
        this.activity = Math.max(0, activity);
    }

    // ------------------------------------------------------------------ radiation

    @Override
    public Vec3 radiationOrigin() {
        return getBlockPos().getCenter();
    }

    @Override
    public double radiationStrength() {
        return ActivationModel.strength(AcceleratorConfig.activation(), activity, incomingPower);
    }

    /** An activated block was broken: radioactive waste falls out and the ground around is contaminated. */
    public static void dismantle(Level level, BlockPos pos, double activity) {
        ActivationModel.Waste waste = ActivationModel.wasteFor(AcceleratorConfig.activation(), activity);
        if (waste.isEmpty()) {
            return;
        }
        if (waste.lowLevel() > 0) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(org.papiricoh.create_nuclearindustry.waste.WasteRegistry.LOW_LEVEL_WASTE.get(), waste.lowLevel()));
        }
        if (waste.intermediateLevel() > 0) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), waste.intermediateLevel()));
        }
        FalloutData.contaminate(level, pos, activity * CONTAMINATION_PER_ACTIVITY, CONTAMINATION_TICKS);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null && !level.isClientSide) {
            dismantle(level, getBlockPos(), activity);
        }
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.accelerator.dump.goggles.";
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "beam", fmt(incomingPower)))
                .withStyle(incomingPower > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "activity", String.format(Locale.ROOT, "%.2f", activity)))
                .withStyle(activity >= AcceleratorConfig.activation().wasteThreshold() ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        return true;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Activity", activity);
        tag.putLong("LastUpdate", lastUpdate);
        if (clientPacket) {
            tag.putDouble("Incoming", incomingPower);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        activity = tag.getDouble("Activity");
        lastUpdate = tag.contains("LastUpdate") ? tag.getLong("LastUpdate") : Long.MIN_VALUE;
        if (clientPacket) {
            incomingPower = tag.getDouble("Incoming");
        }
    }
}

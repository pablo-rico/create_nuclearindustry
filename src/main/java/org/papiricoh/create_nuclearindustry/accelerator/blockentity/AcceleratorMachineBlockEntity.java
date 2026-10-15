package org.papiricoh.create_nuclearindustry.accelerator.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorAttachment;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorSupply;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;
import org.papiricoh.create_nuclearindustry.accelerator.energy.FeBuffer;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;
import org.papiricoh.create_nuclearindustry.accelerator.physics.FeDrive;
import org.papiricoh.create_nuclearindustry.fusion.heating.HeatingDrive;

import java.util.List;
import java.util.Locale;

/**
 * A machine that serves the beamline: a klystron (radio-frequency power for the cavities), a magnet
 * power supply or a vacuum pump; the ion source shares its drive. It runs on its shaft, or on Forge
 * Energy from another mod: what rotation does not cover, energy from the buffer tops up while the
 * accelerator wants the machine. Without an energy mod nobody fills the buffer and nothing changes.
 */
public class AcceleratorMachineBlockEntity extends KineticBlockEntity implements AcceleratorAttachment {
    /** Without news from a controller for this long, the machine counts as outside any accelerator. */
    static final int OBSERVE_TIMEOUT = 60;
    /** Ticks of demand the energy buffer holds. */
    static final int BUFFER_TICKS = 20;

    private final AttachmentRole role;
    protected final FeBuffer energy;
    private double drive;
    private double energyDrive;
    private boolean linked;
    private BeamPhase phase = BeamPhase.OFFLINE;
    private long lastObserved = Long.MIN_VALUE;

    public AcceleratorMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, @Nullable AttachmentRole role) {
        super(type, pos, state);
        this.role = role;
        this.energy = new FeBuffer(FeDrive.buffer(1024, BUFFER_TICKS), this::setChanged);
    }

    /** What this machine is; null for the ion source. */
    @Nullable
    public AttachmentRole role() {
        return role;
    }

    // ------------------------------------------------------------------ drive

    public double stressPerRpm() {
        if (role == null) {
            return AcceleratorConfig.ionSourceStress;
        }
        return switch (role) {
            case KLYSTRON -> AcceleratorConfig.klystronStress;
            case MAGNET_POWER -> AcceleratorConfig.magnetSupplyStress;
            case VACUUM_PUMP, CRYO, INTERLOCK -> AcceleratorConfig.pumpStress;
        };
    }

    public double ratedSpeed() {
        if (role == null) {
            return AcceleratorConfig.ionSourceRatedSpeed;
        }
        return switch (role) {
            case KLYSTRON -> AcceleratorConfig.klystronRatedSpeed;
            case MAGNET_POWER -> AcceleratorConfig.magnetSupplyRatedSpeed;
            case VACUUM_PUMP, CRYO, INTERLOCK -> AcceleratorConfig.pumpRatedSpeed;
        };
    }

    public double minSpeed() {
        return role == AttachmentRole.KLYSTRON ? AcceleratorConfig.klystronMinSpeed : 0;
    }

    /** Share of full drive the shaft gives, 0..1. */
    public double rotationDrive() {
        if (role == AttachmentRole.KLYSTRON) {
            return HeatingDrive.load(getSpeed(), AcceleratorConfig.klystron());
        }
        double rpm = Math.abs(getSpeed());
        double min = minSpeed();
        if (rpm <= min) {
            return 0;
        }
        return Math.min(1, (rpm - min) / Math.max(1, ratedSpeed() - min));
    }

    /** FE per tick that stands in for full rotation. */
    public double energyDemand() {
        return FeDrive.demand(stressPerRpm(), ratedSpeed(), AcceleratorConfig.fePerSu);
    }

    /** Share of full drive this tick, from the shaft and the energy buffer, 0..1. */
    public double drive() {
        return drive;
    }

    public boolean linked() {
        return linked;
    }

    public BeamPhase phase() {
        return phase;
    }

    @Nullable
    public IEnergyStorage energy() {
        return AcceleratorConfig.feInput ? energy : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (linked && level.getGameTime() - lastObserved > OBSERVE_TIMEOUT) {
            observe(BeamReadings.NONE);
        }
        double demand = energyDemand();
        int capacity = FeDrive.buffer(demand, BUFFER_TICKS);
        if (capacity != energy.getMaxEnergyStored()) {
            energy.resize(capacity);
        }
        double rotation = rotationDrive();
        energyDrive = 0;
        if (AcceleratorConfig.feInput && wantsPower() && rotation < 1 && demand > 0) {
            int need = (int) Math.ceil(demand * (1 - rotation));
            energyDrive = FeDrive.fromEnergy(energy.take(need), demand);
        }
        drive = Math.min(1, rotation + energyDrive);
    }

    /**
     * The drive this machine offers the accelerator: what it gives while running, or, while the
     * accelerator is off, what its shaft and stored energy could give, so a start can count on it.
     */
    public double availableDrive() {
        if (wantsPower()) {
            return drive;
        }
        double demand = energyDemand();
        double stored = AcceleratorConfig.feInput && demand > 0 ? FeDrive.fromEnergy(energy.getEnergyStored(), demand) : 0;
        return Math.min(1, Math.max(drive, rotationDrive() + stored));
    }

    /** The accelerator is running and wants this machine. */
    protected boolean wantsPower() {
        return linked && phase != BeamPhase.OFFLINE;
    }

    @Override
    public void contribute(AcceleratorSupply.Builder supply) {
        double offered = availableDrive();
        if (role == null) {
            supply.source(offered);
            return;
        }
        switch (role) {
            case KLYSTRON -> supply.radioFrequency(AcceleratorConfig.klystronPower * offered);
            case MAGNET_POWER -> supply.magnets(AcceleratorConfig.magnetsPerSupply * offered);
            case VACUUM_PUMP -> supply.pump(offered);
            default -> {
            }
        }
    }

    @Override
    public void observe(BeamReadings readings) {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean formed = readings.formed();
        lastObserved = formed ? level.getGameTime() : Long.MIN_VALUE;
        BeamPhase next = formed ? readings.phase() : BeamPhase.OFFLINE;
        if (formed != linked || next != phase) {
            linked = formed;
            phase = next;
            sendData();
        }
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) stressPerRpm();
        this.lastStressApplied = impact;
        return impact;
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.accelerator.machine.goggles.";
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        float speed = Math.abs(getSpeed());
        tooltip.add(line(Component.translatable(k + "speed", fmt(speed), fmt(ratedSpeed())),
                speed >= ratedSpeed() ? ChatFormatting.GREEN : speed > minSpeed() ? ChatFormatting.YELLOW : ChatFormatting.RED));
        if (AcceleratorConfig.feInput) {
            tooltip.add(line(Component.translatable(k + "energy", energy.getEnergyStored(), energy.getMaxEnergyStored(),
                    fmt(energyDemand())), ChatFormatting.GRAY));
        }
        tooltip.add(line(Component.translatable(k + "drive", fmt(drive * 100)), drive > 0.99 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        if (role != null) {
            Component output = switch (role) {
                case KLYSTRON -> Component.translatable(k + "klystron", fmt1(AcceleratorConfig.klystronPower * drive),
                        fmt1(AcceleratorConfig.klystronPower));
                case MAGNET_POWER -> Component.translatable(k + "magnets", fmt(AcceleratorConfig.magnetsPerSupply * drive),
                        AcceleratorConfig.magnetsPerSupply);
                default -> Component.translatable(k + "pump", fmt(drive * 100));
            };
            tooltip.add(line(output, ChatFormatting.AQUA));
        }
        tooltip.add(line(Component.translatable(linked ? k + "linked" : k + "not_linked",
                Component.translatable(phase.langKey())), linked ? ChatFormatting.GRAY : ChatFormatting.RED));
        tooltip.add(Component.empty());
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    protected static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    protected static String fmt(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    protected static String fmt1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putDouble("Drive", drive);
        if (clientPacket) {
            tag.putBoolean("Linked", linked);
            tag.putString("Phase", phase.name());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        energy.set(tag.getInt("Energy"));
        drive = tag.getDouble("Drive");
        if (clientPacket) {
            linked = tag.getBoolean("Linked");
            phase = BeamReadings.phase(tag.getString("Phase"));
        }
    }
}

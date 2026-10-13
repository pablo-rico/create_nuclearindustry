package org.papiricoh.create_nuclearindustry.fusion.heating;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakAuxiliary;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;

import java.util.List;
import java.util.Locale;

/**
 * A kinetic heater in the tokamak shell: turns shaft speed into raw heating power the controller
 * collects each tick through {@link TokamakAuxiliary#contribute}. It also keeps what the plasma was
 * doing the last time the controller told it, for its goggles.
 */
public abstract class PlasmaHeaterBlockEntity extends KineticBlockEntity implements TokamakAuxiliary {
    /** Without news from a controller for this long, the heater counts as outside any tokamak. */
    static final int OBSERVE_TIMEOUT = 60;

    private boolean inTokamak;
    private TokamakPhase plasmaPhase = TokamakPhase.OFFLINE;
    private double plasmaTemperature;
    private double plasmaDensity;
    private boolean heatingEnabled = true;
    private double heatingLevel = 1.0;
    private long lastObserved = Long.MIN_VALUE;

    protected PlasmaHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The speed-to-power curve, from the current config. */
    protected abstract HeatingDrive.Params drive();

    /** Lang key prefix of this heater's goggle lines. */
    protected abstract String langPrefix();

    /** Raw heating power at the current shaft speed, MW; 0 when the network is overstressed. */
    public double power() {
        return HeatingDrive.power(getSpeed(), drive());
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && inTokamak && level.getGameTime() - lastObserved > OBSERVE_TIMEOUT) {
            // The controller went away without saying goodbye (broken, or its chunk unloaded).
            observe(TokamakReadings.NONE);
        }
    }

    @Override
    public void observe(TokamakReadings readings) {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean formed = readings.formed();
        lastObserved = formed ? level.getGameTime() : Long.MIN_VALUE;
        boolean changed = formed != inTokamak || readings.phase() != plasmaPhase
                || readings.heatingEnabled() != heatingEnabled
                || Math.abs(readings.heatingLevel() - heatingLevel) > 0.005
                || Math.abs(readings.temperature() - plasmaTemperature) > Math.max(5.0, 0.02 * plasmaTemperature)
                || Math.abs(readings.density() - plasmaDensity) > 0.01;
        inTokamak = formed;
        plasmaPhase = formed ? readings.phase() : TokamakPhase.OFFLINE;
        plasmaTemperature = formed ? readings.temperature() : 0;
        plasmaDensity = formed ? readings.density() : 0;
        heatingEnabled = !formed || readings.heatingEnabled();
        heatingLevel = formed ? readings.heatingLevel() : 1.0;
        if (changed) {
            sendData();
        }
    }

    public boolean isInTokamak() {
        return inTokamak;
    }

    public TokamakPhase plasmaPhase() {
        return plasmaPhase;
    }

    public double plasmaDensity() {
        return plasmaDensity;
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) stressImpact();
        this.lastStressApplied = impact;
        return impact;
    }

    protected abstract double stressImpact();

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.plasma_heating.goggles.";
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        HeatingDrive.Params drive = drive();
        float speed = Math.abs(getSpeed());
        double power = power();
        if (speed <= drive.minSpeed()) {
            tooltip.add(line(Component.translatable(k + "too_slow", fmt(speed), fmt(drive.minSpeed())), ChatFormatting.RED));
        } else {
            tooltip.add(line(Component.translatable(k + "speed", fmt(speed), fmt(drive.ratedSpeed())),
                    speed >= drive.ratedSpeed() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
        tooltip.add(line(Component.translatable(k + "power", fmt1(power), fmt1(drive.ratedPower())),
                power > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        addHeaterLines(tooltip, power);
        if (!inTokamak) {
            tooltip.add(line(Component.translatable(k + "no_tokamak"), ChatFormatting.RED));
        } else {
            tooltip.add(line(Component.translatable(k + "plasma", Component.translatable(plasmaPhase.langKey()),
                    fmt(plasmaTemperature)), plasmaPhase.hasPlasma() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            if (!heatingEnabled) {
                tooltip.add(line(Component.translatable(k + "heating_off"), ChatFormatting.YELLOW));
            } else if (heatingLevel < 0.995) {
                tooltip.add(line(Component.translatable(k + "heating_level", fmt(heatingLevel * 100)), ChatFormatting.GRAY));
            }
            if (!plasmaPhase.hasPlasma()) {
                tooltip.add(line(Component.translatable(k + "waiting"), ChatFormatting.GRAY));
            } else {
                addPlasmaLines(tooltip);
            }
        }
        tooltip.add(Component.empty());
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    /** Extra lines about this heater's own supply (deuterium for the beams). */
    protected void addHeaterLines(List<Component> tooltip, double power) {}

    /** Extra lines while the plasma is lit (antenna coupling). */
    protected void addPlasmaLines(List<Component> tooltip) {}

    protected static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    protected static String fmt(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    protected static String fmt1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    protected static String fmt2(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) {
            tag.putBoolean("InTokamak", inTokamak);
            tag.putString("PlasmaPhase", plasmaPhase.name());
            tag.putDouble("PlasmaTemperature", plasmaTemperature);
            tag.putDouble("PlasmaDensity", plasmaDensity);
            tag.putBoolean("HeatingEnabled", heatingEnabled);
            tag.putDouble("HeatingLevel", heatingLevel);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            inTokamak = tag.getBoolean("InTokamak");
            try {
                plasmaPhase = TokamakPhase.valueOf(tag.getString("PlasmaPhase"));
            } catch (IllegalArgumentException ignored) {
                plasmaPhase = TokamakPhase.OFFLINE;
            }
            plasmaTemperature = tag.getDouble("PlasmaTemperature");
            plasmaDensity = tag.getDouble("PlasmaDensity");
            heatingEnabled = !tag.contains("HeatingEnabled") || tag.getBoolean("HeatingEnabled");
            heatingLevel = tag.contains("HeatingLevel") ? tag.getDouble("HeatingLevel") : 1.0;
        }
    }
}

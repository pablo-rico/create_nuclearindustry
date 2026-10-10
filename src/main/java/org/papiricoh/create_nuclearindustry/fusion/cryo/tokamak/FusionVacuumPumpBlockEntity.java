package org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakAuxiliary;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoConfig;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel;

import java.util.List;
import java.util.Locale;

/**
 * A tokamak vacuum pump: a stress consumer that offers the controller pumping speed in proportion
 * to its shaft speed (1 = one pump at rated speed). It hears the chamber pressure back from the
 * controller for its goggles.
 */
public class FusionVacuumPumpBlockEntity extends KineticBlockEntity implements TokamakAuxiliary {
    private boolean tokamakFormed;
    private double chamberPressure = TokamakReadings.ATMOSPHERE_VACUUM;

    public FusionVacuumPumpBlockEntity(BlockPos pos, BlockState state) {
        super(CryoRegistry.FUSION_VACUUM_PUMP_ENTITY.get(), pos, state);
    }

    /** Pumping speed, in pumps at rated speed: 0 below the minimum speed or while overstressed. */
    public double pumpSpeed() {
        if (isOverStressed()) {
            return 0.0;
        }
        return CryoplantModel.speedFactor(getSpeed(), CryoConfig.pumpMinSpeed, CryoConfig.pumpRatedSpeed);
    }

    @Override
    public void contribute(TokamakSupply.Builder supply) {
        supply.pump(pumpSpeed());
    }

    @Override
    public void observe(TokamakReadings readings) {
        boolean formed = readings.formed();
        double pressure = readings.vacuum();
        if (formed != tokamakFormed || Math.abs(pressure - chamberPressure) > 0.05) {
            tokamakFormed = formed;
            chamberPressure = pressure;
            setChanged();
            sendData();
        }
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) CryoConfig.pumpStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("TokamakFormed", tokamakFormed);
        tag.putDouble("ChamberPressure", chamberPressure);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tokamakFormed = tag.getBoolean("TokamakFormed");
        chamberPressure = tag.contains("ChamberPressure") ? tag.getDouble("ChamberPressure") : TokamakReadings.ATMOSPHERE_VACUUM;
    }

    /** A chamber pressure given as log10 of pascals, written out in pascals. */
    public static String formatPressure(double log10Pascals) {
        return String.format(Locale.ROOT, "%.1e", Math.pow(10.0, log10Pascals));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        String k = "create_nuclearindustry.vacuum_pump.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.fusion_vacuum_pump"))
                .withStyle(ChatFormatting.GOLD));
        double speed = pumpSpeed();
        if (speed <= 0) {
            tooltip.add(line(Component.translatable(k + "spin_up", Math.round(CryoConfig.pumpMinSpeed)), ChatFormatting.RED));
        } else {
            tooltip.add(line(Component.translatable(k + "pumping", Math.round(speed * 100)), ChatFormatting.GREEN));
        }
        if (tokamakFormed) {
            tooltip.add(line(Component.translatable(k + "chamber", formatPressure(chamberPressure)), ChatFormatting.AQUA));
        } else {
            tooltip.add(line(Component.translatable(k + "unlinked"), ChatFormatting.RED));
        }
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }
}

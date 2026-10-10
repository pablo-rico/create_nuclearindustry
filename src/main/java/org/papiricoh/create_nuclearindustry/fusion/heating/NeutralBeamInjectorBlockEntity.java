package org.papiricoh.create_nuclearindustry.fusion.heating;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;

import java.util.List;

/**
 * Neutral beam injector: an ion source and accelerator driven by the shaft fire fast deuterium
 * atoms into the plasma. Heavy on stress, it heats at any density, and it eats a little of the
 * tokamak's deuterium; the controller scales the beam power down by whatever share of that
 * deuterium its tank cannot give.
 */
public class NeutralBeamInjectorBlockEntity extends PlasmaHeaterBlockEntity {

    public NeutralBeamInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR_ENTITY.get(), pos, state);
    }

    @Override
    protected HeatingDrive.Params drive() {
        return PlasmaHeatingConfig.neutralBeam();
    }

    @Override
    protected double stressImpact() {
        return PlasmaHeatingConfig.neutralBeamStress;
    }

    @Override
    protected String langPrefix() {
        return "create_nuclearindustry.plasma_heating.neutral_beam.";
    }

    /** Deuterium the beams want this tick at the current speed, mB. */
    public double deuteriumDemand() {
        return HeatingDrive.beamDeuterium(power(), PlasmaHeatingConfig.neutralBeamDeuteriumPerMw);
    }

    @Override
    public void contribute(TokamakSupply.Builder supply) {
        double power = power();
        supply.neutralBeam(power, HeatingDrive.beamDeuterium(power, PlasmaHeatingConfig.neutralBeamDeuteriumPerMw));
    }

    @Override
    protected void addHeaterLines(List<Component> tooltip, double power) {
        if (power > 0) {
            tooltip.add(line(Component.translatable(langPrefix() + "deuterium",
                    fmt2(HeatingDrive.beamDeuterium(power, PlasmaHeatingConfig.neutralBeamDeuteriumPerMw))), ChatFormatting.GRAY));
        }
    }
}

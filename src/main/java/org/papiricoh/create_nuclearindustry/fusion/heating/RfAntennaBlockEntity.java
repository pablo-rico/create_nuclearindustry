package org.papiricoh.create_nuclearindustry.fusion.heating;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;

import java.util.List;

/**
 * Radio-frequency antenna: a generator on the shaft feeds a strap antenna behind a Faraday screen
 * on the plasma side. Cheaper in stress than the beams and needs no fuel, but the waves only couple
 * to a plasma dense enough to carry them; the core physics applies that coupling.
 */
public class RfAntennaBlockEntity extends PlasmaHeaterBlockEntity {

    public RfAntennaBlockEntity(BlockPos pos, BlockState state) {
        super(PlasmaHeatingRegistry.RF_ANTENNA_ENTITY.get(), pos, state);
    }

    @Override
    protected HeatingDrive.Params drive() {
        return PlasmaHeatingConfig.radioFrequency();
    }

    @Override
    protected double stressImpact() {
        return PlasmaHeatingConfig.radioFrequencyStress;
    }

    @Override
    protected String langPrefix() {
        return "create_nuclearindustry.plasma_heating.rf_antenna.";
    }

    @Override
    public void contribute(TokamakSupply.Builder supply) {
        supply.radioFrequency(power());
    }

    @Override
    protected void addPlasmaLines(List<Component> tooltip) {
        double density = plasmaDensity();
        double coupling = HeatingDrive.rfCoupling(density);
        if (coupling <= 0) {
            tooltip.add(line(Component.translatable(langPrefix() + "too_thin", fmt(density * 100),
                    fmt(HeatingDrive.RF_MIN_COUPLING_DENSITY * 100)), ChatFormatting.YELLOW));
        } else {
            tooltip.add(line(Component.translatable(langPrefix() + "coupling", fmt(coupling * 100), fmt(density * 100)),
                    coupling >= 0.99 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
    }
}

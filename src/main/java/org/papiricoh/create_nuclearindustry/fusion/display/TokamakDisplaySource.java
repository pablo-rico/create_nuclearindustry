package org.papiricoh.create_nuclearindustry.fusion.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.physics.TokamakPhysicsSimulator;

import java.util.List;

/** Display Link readout attached to a tokamak controller. */
public class TokamakDisplaySource extends DisplaySource {

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof FusionReactorBlockEntity tokamak)) {
            return List.of(Component.literal("No tokamak"));
        }
        if (tokamak.getStructure().isEmpty()) {
            return List.of(Component.literal("Not formed"), Component.literal(tokamak.getValidationMessage()));
        }
        TokamakPhysicsSimulator physics = tokamak.getPhysics();
        return List.of(
                Component.literal(String.format("Plasma: %.0f deg (%s)", physics.getPlasmaTemperature(), physics.getMode())),
                Component.literal(String.format("Power: %.0f MW", physics.getFusionPower())),
                Component.literal(String.format("Confinement: %.0f%%", physics.getConfinement() * 100)),
                Component.literal(String.format("Instability: %.0f%%", physics.getInstability())),
                Component.literal("D: " + tokamak.getDeuteriumTank().getFluidAmount() + " mB  T: " + tokamak.getTritiumTank().getFluidAmount() + " mB"),
                Component.literal(String.format("TBR: %.2f", physics.getTritiumBreedingRatio())));
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}

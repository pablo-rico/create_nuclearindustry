package org.papiricoh.create_nuclearindustry.fuelcycle.jei;

import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.processing.recipe.AcidLeachingRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.IrradiationRecipe;

/** JEI categories of the two fan treatments, drawn like Create's: the fan, and its catalyst in front of it. */
final class FanTreatmentCategories {

    private FanTreatmentCategories() {}

    /** A fan blowing through a pool of sulfuric acid. */
    static final class AcidLeaching extends ProcessingViaFanCategory.MultiOutput<AcidLeachingRecipe> {
        AcidLeaching(Info<AcidLeachingRecipe> info) {
            super(info);
        }

        @Override
        protected void renderAttachedBlock(GuiGraphics graphics) {
            GuiGameElement.of(AllNuclearFluids.SULFURIC_ACID.get())
                    .scale(SCALE)
                    .atLocal(0, 0, 2)
                    .lighting(AnimatedKinetics.DEFAULT_LIGHTING)
                    .render(graphics);
        }
    }

    /** A fan blowing past an Irradiation Source, lit by its glow like Create's haunting. */
    static final class Irradiation extends ProcessingViaFanCategory.MultiOutput<IrradiationRecipe> {
        Irradiation(Info<IrradiationRecipe> info) {
            super(info);
        }

        @Override
        protected AllGuiTextures getBlockShadow() {
            return AllGuiTextures.JEI_LIGHT;
        }

        @Override
        protected void renderAttachedBlock(GuiGraphics graphics) {
            GuiGameElement.of(FuelCycleRegistry.IRRADIATION_SOURCE.get().defaultBlockState())
                    .scale(SCALE)
                    .atLocal(0, 0, 2)
                    .lighting(AnimatedKinetics.DEFAULT_LIGHTING)
                    .render(graphics);
        }
    }
}

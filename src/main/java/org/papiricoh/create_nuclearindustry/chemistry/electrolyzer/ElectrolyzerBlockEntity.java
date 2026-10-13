package org.papiricoh.create_nuclearindustry.chemistry.electrolyzer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlockEntity;

/**
 * A dynamo-driven electrolytic cell. Rotation stands in for the electric current, so it
 * draws more stress than a centrifuge.
 */
public class ElectrolyzerBlockEntity extends KineticProcessingBlockEntity {

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.ELECTROLYZER.get(), pos, state);
    }

    @Override
    protected AllNuclearRecipeTypes recipeType() {
        return AllNuclearRecipeTypes.ELECTROLYSIS;
    }

    @Override
    protected float minimumSpeed() {
        return 32.0f;
    }

    @Override
    protected float stressImpact() {
        return 24.0f;
    }

    @Override
    protected Component machineName() {
        return Component.translatable("block.create_nuclearindustry.electrolyzer");
    }
}

package org.papiricoh.create_nuclearindustry.turbine.client;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import net.minecraft.world.level.block.Block;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;

/** Wraps the baked block models that use connected textures. The item model stays plain. */
public final class NuclearModelSwaps {

    private NuclearModelSwaps() {}

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        connect(event, AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get(), NuclearSpriteShifts.TURBINE_GLASS);
        connect(event, AllNuclearBlocks.TURBINE_CASING.get(), NuclearSpriteShifts.TURBINE_CASING);
        connect(event, AllNuclearBlocks.FUSION_TURBINE_CASING.get(), NuclearSpriteShifts.FUSION_TURBINE_CASING);
        SimpleCTBehaviour viewport = new SimpleCTBehaviour(NuclearSpriteShifts.REACTOR_VIEWPORT);
        for (BlockState state : ReactorVisualsRegistry.REACTOR_VIEWPORT.get().getStateDefinition().getPossibleStates()) {
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, model) -> new CTModel(model, viewport));
        }
    }

    private static void connect(ModelEvent.ModifyBakingResult event, Block block, CTSpriteShiftEntry shift) {
        SimpleCTBehaviour behaviour = new SimpleCTBehaviour(shift);
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, model) -> new CTModel(model, behaviour));
        }
    }
}

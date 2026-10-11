package org.papiricoh.create_nuclearindustry.turbine.client;

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
        SimpleCTBehaviour glass = new SimpleCTBehaviour(NuclearSpriteShifts.TURBINE_GLASS);
        for (BlockState state : AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get().getStateDefinition().getPossibleStates()) {
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, model) -> new CTModel(model, glass));
        }
        SimpleCTBehaviour viewport = new SimpleCTBehaviour(NuclearSpriteShifts.REACTOR_VIEWPORT);
        for (BlockState state : ReactorVisualsRegistry.REACTOR_VIEWPORT.get().getStateDefinition().getPossibleStates()) {
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, model) -> new CTModel(model, viewport));
        }
    }
}

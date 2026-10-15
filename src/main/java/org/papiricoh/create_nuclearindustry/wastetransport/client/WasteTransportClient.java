package org.papiricoh.create_nuclearindustry.wastetransport.client;

import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportRegistry;

/** Client setup for the waste containers: their connected textures. */
public final class WasteTransportClient implements NuclearFeatureClient {
    public static final WasteTransportClient INSTANCE = new WasteTransportClient();

    private WasteTransportClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        WasteTransportSpriteShifts.init();
        modEventBus.addListener(WasteTransportClient::onModifyBakingResult);
    }

    /** Wraps the block models in Create's CT model; the item models stay plain. */
    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // A cistern joins like a fluid tank: sides by side sheet, top and bottom by the lid sheet.
        wrap(event, WasteTransportRegistry.RADIOACTIVE_WASTE_CISTERN.get(), new FluidTankCTBehaviour(
                WasteTransportSpriteShifts.CISTERN_SIDE, WasteTransportSpriteShifts.CISTERN_TOP, WasteTransportSpriteShifts.CISTERN_TOP));
        wrap(event, WasteTransportRegistry.SHIELDED_WASTE_CASK.get(), new WasteCaskCTBehaviour());
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block block, ConnectedTextureBehaviour behaviour) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (location, model) -> new CTModel(model, behaviour));
        }
    }
}

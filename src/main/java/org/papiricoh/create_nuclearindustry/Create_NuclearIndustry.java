package org.papiricoh.create_nuclearindustry;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.createmod.ponder.foundation.PonderIndex;
import org.papiricoh.create_nuclearindustry.data.NuclearDataGenerators;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.NuclearExplosionSoundManager;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.NuclearPonderPlugin;
import org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearIntegration;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockChangeHandler;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.commands.NuclearCommands;
import org.papiricoh.create_nuclearindustry.processing.ProcessingMachineRenderer;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Create_NuclearIndustry.MODID)
public class Create_NuclearIndustry {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "create_nuclearindustry";


    public Create_NuclearIndustry(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(Config::onLoad);
        modEventBus.addListener(Config::onReload);
        modEventBus.addListener(AllNuclearCapabilities::registerCapabilities);
        modEventBus.addListener(NuclearDataGenerators::gatherData);

        AllCreativeTabs.init();

        AllNuclearDataComponents.init();
        AllNuclearDataComponents.DATA_COMPONENTS.register(modEventBus);

        AllNuclearRecipes.init();
        AllNuclearRecipes.SERIALIZERS.register(modEventBus);

        AllNuclearRecipeTypes.register(modEventBus);
        AllNuclearRadiation.register(modEventBus);
        AllNuclearParticles.register(modEventBus);

        AllNuclearIngredients.init();
        AllNuclearIngredients.INGREDIENT_TYPES.register(modEventBus);

        AllNuclearBlocks.init();

        // Register the Deferred Register to the mod event bus so blocks get registered
        AllNuclearBlocks.BLOCKS.register(modEventBus);


        AllNuclearItems.init();
        // Register the Deferred Register to the mod event bus so items get registered
        AllNuclearItems.ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        AllCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        AllNuclearEntities.init();
        AllNuclearEntities.ENTITIES.register(modEventBus);
        AllNuclearEntities.ENTITY_TYPES.register(modEventBus);

        AllNuclearDisplaySources.register(modEventBus);

        AllNuclearGUIs.init();
        AllNuclearGUIs.MENUS.register(modEventBus);

        AllNuclearSounds.init();
        AllNuclearSounds.SOUNDS.register(modEventBus);

        CBCNuclearIntegration.init(modEventBus);

        AllNuclearFluids.FLUID_TYPES.register(modEventBus);
        AllNuclearFluids.FLUIDS.register(modEventBus);
        AllNuclearFluids.BLOCKS.register(modEventBus);
        AllNuclearFluids.ITEMS.register(modEventBus);

        MultiblockChangeHandler.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(RadiationHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.create_nuclearindustry.radiation.FalloutData::onLevelTick);
        NeoForge.EVENT_BUS.addListener(Contamination::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(Contamination::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(Contamination::onChunkWatch);
        NeoForge.EVENT_BUS.addListener(NuclearCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(NuclearBlastManager::onLevelTick);
        NeoForge.EVENT_BUS.addListener(NuclearExplosionSoundManager::onLevelTick);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            PonderIndex.addPlugin(new NuclearPonderPlugin());
            org.papiricoh.create_nuclearindustry.turbine.client.NuclearClient.init(modEventBus);
            modEventBus.addListener(ClientModEvents::onRegisterScreens);
            modEventBus.addListener(ClientModEvents::onRegisterRenderers);
            modEventBus.addListener(AllNuclearFluids::registerClientExtensions);
            modEventBus.addListener(ClientConfig::onLoad);
            modEventBus.addListener(ClientConfig::onReload);
            modEventBus.addListener(org.papiricoh.create_nuclearindustry.radiation.contamination.client.ContaminationParticles::registerProviders);
            NeoForge.EVENT_BUS.addListener(org.papiricoh.create_nuclearindustry.radiation.contamination.client.ContaminationParticles::onClientTick);
            modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        }

        // Register our mod's ForgeConfigSpec so that NeoForge can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static class ClientModEvents {

        public static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(AllNuclearEntities.MISSILE.get(),
                    org.papiricoh.create_nuclearindustry.missile.client.MissileRenderer::new);
            event.registerBlockEntityRenderer(AllNuclearEntities.LAUNCH_PAD.get(),
                    org.papiricoh.create_nuclearindustry.missile.client.LaunchPadRenderer::new);
            event.registerBlockEntityRenderer(AllNuclearEntities.CENTRIFUGE.get(), ProcessingMachineRenderer::new);
            event.registerBlockEntityRenderer(AllNuclearEntities.ELECTROLYZER.get(), ProcessingMachineRenderer::new);
            event.registerBlockEntityRenderer(AllNuclearEntities.RTG.get(),
                    org.papiricoh.create_nuclearindustry.rtg.client.RtgRenderer::new);
            event.registerBlockEntityRenderer(AllNuclearEntities.TURBINE_OUTPUT.get(),
                    org.papiricoh.create_nuclearindustry.turbine.client.TurbineRenderer::new);
            event.registerBlockEntityRenderer(AllNuclearEntities.FUSION_PLASMA_TURBINE.get(),
                    org.papiricoh.create_nuclearindustry.turbine.client.TurbineRenderer::new);
        }

        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(AllNuclearGUIs.REACTOR_MENU.get(),
                    org.papiricoh.create_nuclearindustry.reactor.gui.ReactorControlScreen::new);
            event.register(AllNuclearGUIs.NUCLEAR_BOMB_MENU.get(),
                    org.papiricoh.create_nuclearindustry.explosive.gui.NuclearBombScreen::new);
            event.register(AllNuclearGUIs.LAUNCH_PAD_MENU.get(),
                    org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadScreen::new);
            event.register(AllNuclearGUIs.TOKAMAK_MENU.get(),
                    org.papiricoh.create_nuclearindustry.fusion.gui.TokamakScreen::new);
        }
    }

}

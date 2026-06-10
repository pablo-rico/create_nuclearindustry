package org.papiricoh.create_nuclearindustry;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;

import java.lang.reflect.Method;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.createmod.ponder.foundation.PonderIndex;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.NuclearExplosionSoundManager;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.NuclearPonderPlugin;
import org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearIntegration;
import org.papiricoh.create_nuclearindustry.reactor.event.ReactorBlockChangeHandler;
import org.papiricoh.create_nuclearindustry.fusion.event.FusionBlockChangeHandler;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Create_NuclearIndustry.MODID)
public class Create_NuclearIndustry {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "create_nuclearindustry";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();


    public Create_NuclearIndustry(IEventBus modEventBus, ModContainer modContainer) {

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(Config::onLoad);
        modEventBus.addListener(AllNuclearCapabilities::registerCapabilities);

        AllCreativeTabs.init();

        AllNuclearDataComponents.init();
        AllNuclearDataComponents.DATA_COMPONENTS.register(modEventBus);

        AllNuclearRecipes.init();
        AllNuclearRecipes.TYPES.register(modEventBus);
        AllNuclearRecipes.SERIALIZERS.register(modEventBus);

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






        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onBlockMultiPlace);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onNeighborNotify);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onFluidPlaceBlock);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onBlockToolModification);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(FusionBlockChangeHandler::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(FusionBlockChangeHandler::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(FusionBlockChangeHandler::onBlockMultiPlace);
        NeoForge.EVENT_BUS.addListener(FusionBlockChangeHandler::onNeighborNotify);
        NeoForge.EVENT_BUS.addListener(FusionBlockChangeHandler::onFluidPlaceBlock);
        NeoForge.EVENT_BUS.addListener(FusionBlockChangeHandler::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(FusionBlockChangeHandler::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(NuclearBlastManager::onLevelTick);
        NeoForge.EVENT_BUS.addListener(NuclearExplosionSoundManager::onLevelTick);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            PonderIndex.addPlugin(new NuclearPonderPlugin());
            modEventBus.addListener(ClientModEvents::onClientSetup);
            modEventBus.addListener(ClientModEvents::onRegisterRenderers);
            modEventBus.addListener(AllNuclearFluids::registerClientExtensions);
        }

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that NeoForge can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        /*if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(AllNuclearItems.EXAMPLE_BLOCK_ITEM);
            */
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(AllNuclearEntities.MISSILE.get(),
                    org.papiricoh.create_nuclearindustry.missile.client.MissileRenderer::new);
            event.registerBlockEntityRenderer(AllNuclearEntities.LAUNCH_PAD.get(),
                    org.papiricoh.create_nuclearindustry.missile.client.LaunchPadRenderer::new);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            // Register screens via reflection since MenuScreens.register is private
            event.enqueueWork(() -> {
                try {
                    Method registerMethod = MenuScreens.class.getDeclaredMethod("register", net.minecraft.world.inventory.MenuType.class, net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor.class);
                    registerMethod.setAccessible(true);
                    registerMethod.invoke(null, AllNuclearGUIs.REACTOR_MENU.get(), (net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<org.papiricoh.create_nuclearindustry.reactor.gui.ReactorControlMenu, org.papiricoh.create_nuclearindustry.reactor.gui.ReactorControlScreen>)org.papiricoh.create_nuclearindustry.reactor.gui.ReactorControlScreen::new);
                    registerMethod.invoke(null, AllNuclearGUIs.NUCLEAR_BOMB_MENU.get(), (net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<org.papiricoh.create_nuclearindustry.explosive.gui.NuclearBombMenu, org.papiricoh.create_nuclearindustry.explosive.gui.NuclearBombScreen>)org.papiricoh.create_nuclearindustry.explosive.gui.NuclearBombScreen::new);
                    registerMethod.invoke(null, AllNuclearGUIs.LAUNCH_PAD_MENU.get(), (net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadMenu, org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadScreen>)org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadScreen::new);
                } catch (Exception e) {
                    LOGGER.error("Failed to register nuclear screens", e);
                }
            });
        }
    }

}

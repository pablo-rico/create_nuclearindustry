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
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.NuclearPonderPlugin;
import org.papiricoh.create_nuclearindustry.reactor.event.ReactorBlockChangeHandler;
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

        AllNuclearGUIs.init();
        AllNuclearGUIs.MENUS.register(modEventBus);

        AllNuclearFluids.FLUID_TYPES.register(modEventBus);
        AllNuclearFluids.FLUIDS.register(modEventBus);
        AllNuclearFluids.BLOCKS.register(modEventBus);
        AllNuclearFluids.ITEMS.register(modEventBus);






        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(ReactorBlockChangeHandler::onBlockPlace);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            PonderIndex.addPlugin(new NuclearPonderPlugin());
            modEventBus.addListener(ClientModEvents::onClientSetup);
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
                } catch (Exception e) {
                    LOGGER.error("Failed to register reactor control screen", e);
                }
            });
        }
    }

}

package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import org.papiricoh.create_nuclearindustry.*;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

public final class SiloRegistry {
    public static final DeferredHolder<Block, RocketSectionBlock> ENGINE = section("rocket_engine", RocketComposition.Section.ENGINE);
    public static final DeferredHolder<Block, RocketSectionBlock> TANK = section("rocket_tank", RocketComposition.Section.TANK);
    public static final DeferredHolder<Block, RocketSectionBlock> GUIDANCE = section("rocket_guidance", RocketComposition.Section.GUIDANCE);
    public static final DeferredHolder<Block, RocketSectionBlock> WARHEAD = section("rocket_warhead", RocketComposition.Section.WARHEAD);
    public static final DeferredHolder<Block, Block> WALL = AllNuclearBlocks.BLOCKS.register("silo_wall", () -> new Block(wallProperties()));
    /** A wall block pipes connect to, to pump propellant into the rocket. */
    public static final DeferredHolder<Block, Block> FUEL_PORT = AllNuclearBlocks.BLOCKS.register("silo_fuel_port", () -> new Block(wallProperties()));
    /** What a silo's floor and walls may be built of. */
    public static final net.minecraft.tags.TagKey<Block> SILO_WALLS = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "silo_walls"));
    public static final DeferredHolder<Block, SiloControllerBlock> CONTROLLER = AllNuclearBlocks.BLOCKS.register("silo_controller", () -> new SiloControllerBlock(properties()));
    public static final DeferredHolder<Item, Item> PROPELLANT = AllNuclearItems.ITEMS.register("propellant_cartridge", () -> new Item(new Item.Properties()));
    /** Goes in a guidance module for stellar guidance, the most accurate. */
    public static final DeferredHolder<Item, Item> STAR_TRACKER = AllNuclearItems.ITEMS.register("star_tracker", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RocketSectionEntity>> SECTION_ENTITY = AllNuclearEntities.ENTITIES.register("rocket_section",
            () -> BlockEntityType.Builder.of(RocketSectionEntity::new, ENGINE.get(), TANK.get(), GUIDANCE.get(), WARHEAD.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SiloControllerEntity>> CONTROLLER_ENTITY = AllNuclearEntities.ENTITIES.register("silo_controller",
            () -> BlockEntityType.Builder.of(SiloControllerEntity::new, CONTROLLER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SiloMenu>> MENU = AllNuclearGUIs.MENUS.register("silo_controller", () -> IMenuTypeExtension.create(SiloMenu::read));
    static {
        for (var block : java.util.List.of(ENGINE, TANK, GUIDANCE, WARHEAD, WALL, FUEL_PORT, CONTROLLER))
            AllNuclearItems.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }
    /** Reinforced concrete: full, opaque blocks that stand up to more than a rocket's exhaust. */
    private static BlockBehaviour.Properties wallProperties() { return BlockBehaviour.Properties.of().strength(5, 600).requiresCorrectToolForDrops(); }
    private static BlockBehaviour.Properties properties() { return BlockBehaviour.Properties.of().strength(4, 12).noOcclusion(); }
    private static DeferredHolder<Block, RocketSectionBlock> section(String name, RocketComposition.Section section) {
        return AllNuclearBlocks.BLOCKS.register(name, () -> new RocketSectionBlock(properties(), section));
    }
    public static void register(IEventBus bus) { SiloControlPayload.register(bus); }
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlock(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                (level, pos, state, entity, side) -> SiloFuelPort.at(level, pos), FUEL_PORT.get());
    }
    public static void fillCreativeTab(SectionedOutput output) {
        for (var block : java.util.List.of(ENGINE, TANK, GUIDANCE, WARHEAD, WALL, FUEL_PORT, CONTROLLER)) output.accept(block.get());
        output.accept(PROPELLANT.get());
        output.accept(STAR_TRACKER.get());
    }
}

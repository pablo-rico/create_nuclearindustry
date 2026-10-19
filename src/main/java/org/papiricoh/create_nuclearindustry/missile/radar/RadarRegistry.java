package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.*;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

public final class RadarRegistry {
    public static final DeferredHolder<Block, RadarBlock> RADAR = AllNuclearBlocks.BLOCKS.register("missile_radar",
            () -> new RadarBlock(BlockBehaviour.Properties.of().strength(4, 12).noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> ITEM = AllNuclearItems.ITEMS.register("missile_radar",
            () -> new BlockItem(RADAR.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarBlockEntity>> ENTITY = AllNuclearEntities.ENTITIES.register("missile_radar",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, RADAR.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<RadarMenu>> MENU = AllNuclearGUIs.MENUS.register("missile_radar",
            () -> IMenuTypeExtension.create(RadarMenu::fromNetwork));
    public static void register(IEventBus bus) { RadarPayload.register(bus); }
    public static void fillCreativeTab(SectionedOutput output) { output.accept(ITEM.get()); }
    private RadarRegistry() {}
}

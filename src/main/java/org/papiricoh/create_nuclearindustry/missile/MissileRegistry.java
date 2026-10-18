package org.papiricoh.create_nuclearindustry.missile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.missile.block.LaunchPadBlock;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;
import org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadMenu;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;

/**
 * Missiles: the launch pad, the missile it fires and the target designator that tells it where.
 * The ids are the ones these had in the shared registries, so existing worlds keep their pads and
 * missiles in flight.
 */
public final class MissileRegistry implements NuclearFeature {
    public static final MissileRegistry INSTANCE = new MissileRegistry();

    public static final DeferredHolder<net.minecraft.world.level.block.Block, LaunchPadBlock> LAUNCH_PAD =
            AllNuclearBlocks.BLOCKS.register("launch_pad", () -> new LaunchPadBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(4.0f, 12.0f).noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> LAUNCH_PAD_ITEM = AllNuclearItems.ITEMS.register("launch_pad",
            () -> new BlockItem(LAUNCH_PAD.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> MISSILE_ITEM = AllNuclearItems.ITEMS.register("missile",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, TargetDesignatorItem> TARGET_DESIGNATOR = AllNuclearItems.ITEMS.register("target_designator",
            () -> new TargetDesignatorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaunchPadBlockEntity>> LAUNCH_PAD_ENTITY =
            AllNuclearEntities.ENTITIES.register("launch_pad", () -> BlockEntityType.Builder.of(LaunchPadBlockEntity::new,
                    LAUNCH_PAD.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<MissileEntity>> MISSILE =
            AllNuclearEntities.ENTITY_TYPES.register("missile", () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                    .sized(0.6f, 2.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("missile"));

    public static final DeferredHolder<MenuType<?>, MenuType<LaunchPadMenu>> LAUNCH_PAD_MENU =
            AllNuclearGUIs.MENUS.register("launch_pad", () -> IMenuTypeExtension.create(LaunchPadMenu::fromNetwork));

    private MissileRegistry() {}

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.WEAPONS);
        output.accept(LAUNCH_PAD_ITEM.get());
        output.accept(MISSILE_ITEM.get());
        output.accept(TARGET_DESIGNATOR.get());
    }
}

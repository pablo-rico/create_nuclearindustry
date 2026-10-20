package org.papiricoh.create_nuclearindustry.missile.defense;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.BatteryConfigurePayload;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryBlock;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryItem;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryMenu;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryPartBlock;

/**
 * Missile defence: the interceptor battery (a master block and its seven parts, placed together by one item), the interceptor
 * rounds it fires and their part-made form. The launch controller's kill vehicle is a rocket section, in {@code SiloRegistry}.
 */
public final class DefenseRegistry {
    public static final DeferredHolder<Block, InterceptorBatteryBlock> BATTERY = AllNuclearBlocks.BLOCKS.register("interceptor_battery",
            () -> new InterceptorBatteryBlock(properties()));
    public static final DeferredHolder<Block, InterceptorBatteryPartBlock> BATTERY_PART = AllNuclearBlocks.BLOCKS.register("interceptor_battery_part",
            () -> new InterceptorBatteryPartBlock(properties()));
    public static final DeferredHolder<Item, InterceptorBatteryItem> BATTERY_ITEM = AllNuclearItems.ITEMS.register("interceptor_battery",
            () -> new InterceptorBatteryItem(BATTERY.get(), new Item.Properties()));
    /** A battery's round: one goes in each tube. */
    public static final DeferredHolder<Item, Item> INTERCEPTOR = AllNuclearItems.ITEMS.register("interceptor",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_INTERCEPTOR = AllNuclearItems.ITEMS.register("incomplete_interceptor",
            () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InterceptorBatteryEntity>> BATTERY_ENTITY =
            AllNuclearEntities.ENTITIES.register("interceptor_battery", () -> BlockEntityType.Builder.of(InterceptorBatteryEntity::new, BATTERY.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<InterceptorBatteryMenu>> BATTERY_MENU = AllNuclearGUIs.MENUS.register("interceptor_battery",
            () -> IMenuTypeExtension.create(InterceptorBatteryMenu::fromNetwork));

    private DefenseRegistry() {
    }

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(5, 12).noOcclusion().requiresCorrectToolForDrops();
    }

    public static void register(IEventBus bus) {
        BatteryConfigurePayload.register(bus);
    }

    /** Funnels and mechanical arms load a battery's tubes through any of its eight blocks. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BATTERY_ENTITY.get(), (battery, side) -> battery.tubes());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, entity, side) ->
                        level.getBlockEntity(InterceptorBatteryPartBlock.master(pos, state)) instanceof InterceptorBatteryEntity battery ? battery.tubes() : null,
                BATTERY_PART.get());
    }

    public static void fillCreativeTab(SectionedOutput output) {
        output.accept(BATTERY_ITEM.get());
        output.accept(INTERCEPTOR.get());
    }
}

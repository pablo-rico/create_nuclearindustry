package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeature;

import java.util.List;
import java.util.function.Supplier;

/**
 * What a nuclear detonation leaves and how it reaches players: trinitite (the glass the fireball fuses
 * from the crater's floor), charred logs, the flash's damage type, the jobs that work the crater and its
 * zones through ({@link NuclearBlastManager}) and the packet that shows clients the explosion
 * ({@link DetonationPayload}).
 */
public final class ExplosiveRegistry implements NuclearFeature {
    public static final ExplosiveRegistry INSTANCE = new ExplosiveRegistry();
    /** Burns from the thermal flash of a nuclear fireball; a fire damage, so Fire Protection helps. */
    public static final ResourceKey<DamageType> NUCLEAR_FLASH = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "nuclear_flash"));

    public static final DeferredHolder<Block, Block> TRINITITE = AllNuclearBlocks.BLOCKS.register("trinitite",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.5f, 6.0f)
                    .sound(SoundType.GLASS).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, RotatedPillarBlock> CHARRED_LOG = AllNuclearBlocks.BLOCKS.register("charred_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f)
                    .sound(SoundType.WOOD)));

    public static final DeferredHolder<Item, BlockItem> TRINITITE_ITEM = blockItem("trinitite", TRINITITE);
    public static final DeferredHolder<Item, BlockItem> CHARRED_LOG_ITEM = blockItem("charred_log", CHARRED_LOG);

    private ExplosiveRegistry() {}

    private static DeferredHolder<Item, BlockItem> blockItem(String name, Supplier<? extends Block> block) {
        return AllNuclearItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static List<DeferredHolder<Item, BlockItem>> items() {
        return List.of(TRINITITE_ITEM, CHARRED_LOG_ITEM);
    }

    @Override
    public void register(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(NuclearBlastManager::onLevelTick);
        NeoForge.EVENT_BUS.addListener(NuclearBlastManager::onChunkLoad);
        DetonationPayload.register(modEventBus);
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        items().forEach(item -> output.accept(item.get()));
    }
}

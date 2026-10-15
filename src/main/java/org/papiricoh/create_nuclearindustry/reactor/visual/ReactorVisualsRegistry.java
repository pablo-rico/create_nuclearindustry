package org.papiricoh.create_nuclearindustry.reactor.visual;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

/**
 * Content for the visible and audible fission reactor: the casing viewport, the fuel channels'
 * looks and the reactor's sounds. Blocks and items register here.
 */
public final class ReactorVisualsRegistry implements NuclearFeature {
    public static final ReactorVisualsRegistry INSTANCE = new ReactorVisualsRegistry();

    public static final DeferredHolder<Block, ReactorViewportBlock> REACTOR_VIEWPORT = AllNuclearBlocks.BLOCKS.register(
            "reactor_viewport", () -> new ReactorViewportBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(4.0f, 12.0f).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));

    public static final DeferredHolder<Item, BlockItem> REACTOR_VIEWPORT_ITEM = AllNuclearItems.ITEMS.register(
            "reactor_viewport", () -> new BlockItem(REACTOR_VIEWPORT.get(), new Item.Properties()));

    private ReactorVisualsRegistry() {}

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.FISSION);
        output.accept(REACTOR_VIEWPORT_ITEM.get());
    }
}

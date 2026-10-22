package org.papiricoh.create_nuclearindustry.radiation.decontamination;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

/** Ways to clean up contaminated ground: the Decontamination Sprinkler and sunflowers that wilt into Irradiated Sunflowers. */
public final class DecontaminationRegistry implements NuclearFeature {
    public static final DecontaminationRegistry INSTANCE = new DecontaminationRegistry();

    public static final DeferredHolder<Block, DecontaminationSprinklerBlock> SPRINKLER = AllNuclearBlocks.BLOCKS.register(
            "decontamination_sprinkler", () -> new DecontaminationSprinklerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Block, IrradiatedSunflowerBlock> IRRADIATED_SUNFLOWER = AllNuclearBlocks.BLOCKS.register(
            "irradiated_sunflower", () -> new IrradiatedSunflowerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SUNFLOWER)
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)));

    public static final DeferredHolder<Item, BlockItem> SPRINKLER_ITEM = AllNuclearItems.ITEMS.register("decontamination_sprinkler",
            () -> new BlockItem(SPRINKLER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> IRRADIATED_SUNFLOWER_ITEM = AllNuclearItems.ITEMS.register("irradiated_sunflower",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(IRRADIATED_SUNFLOWER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRRADIATED_BIOMASS = AllNuclearItems.ITEMS.register("irradiated_biomass",
            () -> new Item(new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecontaminationSprinklerBlockEntity>> SPRINKLER_ENTITY =
            AllNuclearEntities.ENTITIES.register("decontamination_sprinkler", () -> BlockEntityType.Builder.of(
                    DecontaminationSprinklerBlockEntity::new, SPRINKLER.get()).build(null));

    private DecontaminationRegistry() {}

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SPRINKLER_ENTITY.get(),
                (sprinkler, direction) -> sprinkler.getFluidHandler(direction));
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.SAFETY);
        output.accept(SPRINKLER_ITEM.get());
        output.accept(IRRADIATED_SUNFLOWER_ITEM.get());
        output.accept(IRRADIATED_BIOMASS.get());
    }
}

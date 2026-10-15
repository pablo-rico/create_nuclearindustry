package org.papiricoh.create_nuclearindustry.energy;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

/**
 * Forge Energy helpers shared by the mod's machines: the creative generator for testing, and
 * {@link FeSource} for the generators that make energy.
 */
public final class EnergyRegistry implements NuclearFeature {
    public static final EnergyRegistry INSTANCE = new EnergyRegistry();

    public static final DeferredHolder<Block, CreativeRfGeneratorBlock> CREATIVE_RF_GENERATOR = AllNuclearBlocks.BLOCKS.register(
            "creative_rf_generator", () -> new CreativeRfGeneratorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3_600_000.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 7)
                    .pushReaction(PushReaction.BLOCK)));
    public static final DeferredHolder<Item, BlockItem> CREATIVE_RF_GENERATOR_ITEM = AllNuclearItems.ITEMS.register(
            "creative_rf_generator", () -> new BlockItem(CREATIVE_RF_GENERATOR.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeRfGeneratorBlockEntity>> CREATIVE_RF_GENERATOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("creative_rf_generator", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new CreativeRfGeneratorBlockEntity(EnergyRegistry.CREATIVE_RF_GENERATOR_ENTITY.get(), pos, state),
                    CREATIVE_RF_GENERATOR.get()).build(null));

    private EnergyRegistry() {}

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CREATIVE_RF_GENERATOR_ENTITY.get(), (be, side) -> be.energy());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.POWER);
        output.accept(CREATIVE_RF_GENERATOR_ITEM.get());
    }
}

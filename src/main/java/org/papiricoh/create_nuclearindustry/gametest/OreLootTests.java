package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class OreLootTests {
    private static final BlockPos ORE = new BlockPos(1, 1, 1);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void uraniumOreDropsRawUranium(GameTestHelper helper) {
        assertDrops(helper, AllNuclearBlocks.URANIUM_ORE.get(), plainPickaxe(), AllNuclearItems.RAW_URANIUM.get(), 1);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void silkTouchKeepsTheOreBlock(GameTestHelper helper) {
        ItemStack pickaxe = plainPickaxe();
        pickaxe.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        assertDrops(helper, AllNuclearBlocks.URANIUM_ORE.get(), pickaxe, AllNuclearBlocks.URANIUM_ORE.get().asItem(), 1);
        assertDrops(helper, AllNuclearBlocks.SPODUMENE_ORE.get(), pickaxe, AllNuclearBlocks.SPODUMENE_ORE.get().asItem(), 1);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void spodumeneOreDropsRawSpodumene(GameTestHelper helper) {
        assertDrops(helper, AllNuclearBlocks.SPODUMENE_ORE.get(), plainPickaxe(), AllNuclearItems.RAW_SPODUMENE.get(), 1);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void boraxOreDropsSeveralSalts(GameTestHelper helper) {
        assertDrops(helper, AllNuclearBlocks.BORAX_ORE.get(), plainPickaxe(), AllNuclearItems.BORAX_SALT.get(), 2);
        helper.succeed();
    }

    private static ItemStack plainPickaxe() {
        return new ItemStack(Items.DIAMOND_PICKAXE);
    }

    private static void assertDrops(GameTestHelper helper, Block block, ItemStack tool, Item expected, int minCount) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(ORE);
        BlockState state = block.defaultBlockState();
        helper.setBlock(ORE, state);
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, tool);
        int count = drops.stream().filter(stack -> stack.is(expected)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(count >= minCount, block + " should drop at least " + minCount + " " + expected + ", got " + drops);
    }
}

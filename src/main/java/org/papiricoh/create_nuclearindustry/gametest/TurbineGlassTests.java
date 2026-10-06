package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TurbineGlassTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void glassIsTransparentAndDropsItself(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState glass = AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get().defaultBlockState();
        helper.setBlock(pos, glass);
        helper.assertFalse(glass.canOcclude(), "glass must not hide the faces behind it");
        helper.assertTrue(glass.is(MultiblockTags.MULTIBLOCK_PARTS), "glass is a turbine part");
        var drops = Block.getDrops(glass, helper.getLevel(), helper.absolutePos(pos), null, null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get().asItem()),
                "glass should drop itself without silk touch, got " + drops);
        helper.succeed();
    }
}

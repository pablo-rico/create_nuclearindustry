package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerGeometry;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;

/**
 * An unformed cooling tower controller only listens where its tower can be: behind it (it faces
 * out of the tower), and once the floor is laid, on that floor and the walls it can carry. Block
 * changes elsewhere cost nothing, and a tower still forms and re-forms as it is built.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CoolingTowerBoundsTests {
    private static final BlockPos CENTER = new BlockPos(6, 1, 6);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anUnformedControllerListensBehindItself(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 6);
        helper.setBlock(pos, AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        helper.runAfterDelay(3, () -> {
            CoolingTowerControllerBlockEntity controller = helper.getBlockEntity(pos);
            helper.assertTrue(controller != null && controller.getStructure().isEmpty(), "a lone controller, unformed");
            BoundingBox box = MultiblockRegistry.indexedBounds(controller);
            BlockPos at = helper.absolutePos(pos);
            int depth = 2 * CoolingTowerGeometry.MAX_BASE_RADIUS + 2;
            int side = CoolingTowerGeometry.MAX_BASE_RADIUS + 1;
            helper.assertTrue(box != null && box.minX() == at.getX() - 1 && box.maxX() == at.getX() + depth,
                    "facing west, it listens east of itself only: " + box);
            helper.assertTrue(box.minZ() == at.getZ() - side && box.maxZ() == at.getZ() + side, "as wide as the widest tower: " + box);
            helper.assertTrue(box.maxY() == at.getY() + CoolingTowerGeometry.ABSOLUTE_MAX_HEIGHT, "as high as the tallest: " + box);
            int before = (2 * (2 * CoolingTowerGeometry.MAX_BASE_RADIUS + 1) + 1);
            helper.assertTrue(box.getXSpan() * box.getZSpan() < before * before / 2, "less than half the old footprint: " + box);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aLaidFloorNarrowsTheListeningAndTheTowerStillReforms(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = CoolingTowerTestBuilder.build(helper, CENTER, 4, 12);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        BoundingBox[] formed = new BoundingBox[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(controller != null && controller.getStructure().isPresent(), "the tower forms: " + controller.getValidationMessage());
            formed[0] = MultiblockRegistry.indexedBounds(controller);
            helper.setBlock(tower.eastWall(6), Blocks.AIR);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(controller.getStructure().isEmpty(), "a hole in the wall unforms it");
            BoundingBox box = MultiblockRegistry.indexedBounds(controller);
            BlockPos center = helper.absolutePos(CENTER);
            int reach = 4 + 2;
            helper.assertTrue(box != null && box.minX() == center.getX() - reach && box.maxX() == center.getX() + reach
                            && box.minZ() == center.getZ() - reach && box.maxZ() == center.getZ() + reach,
                    "with its floor laid it listens on the floor and its walls: " + box);
            helper.assertTrue(box.maxY() == center.getY() + CoolingTowerGeometry.maxHeight(4) + 2, "up to the tallest tower on that floor: " + box);
            helper.assertTrue(box.getXSpan() < 2 * CoolingTowerGeometry.MAX_BASE_RADIUS, "far narrower than the search box: " + box);
            helper.setBlock(tower.eastWall(6), AllNuclearBlocks.COOLING_TOWER_CASING.get());
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(controller.getStructure().isPresent(), "mending the wall forms it again: " + controller.getValidationMessage());
            helper.assertTrue(formed[0].equals(MultiblockRegistry.indexedBounds(controller)), "with the tower's own bounds");
            helper.succeed();
        });
    }
}

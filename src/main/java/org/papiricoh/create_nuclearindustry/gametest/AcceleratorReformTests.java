package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;

/**
 * An unformed accelerator controller listens only {@link AcceleratorControllerBlockEntity#UNFORMED_REACH}
 * blocks around itself, but one that was formed keeps listening where its old structure stood: a long
 * linac broken and repaired far from the controller forms again by itself, as it did before the
 * box shrank. Built in the wide arena, the only one long enough for a linac past that reach.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AcceleratorReformTests {
    private static final BlockPos CONTROLLER = new BlockPos(20, 2, 1);
    private static final BlockPos SOURCE = CONTROLLER.south();
    /** Beamline blocks between the source and the dump. */
    private static final int LENGTH = 35;
    private static final int CAVITIES = 8;
    /** The block taken out and put back: 35 blocks from the controller. */
    private static final int BROKEN_CELL = 33;

    private static BlockPos cell(int i) {
        return SOURCE.south(1 + i);
    }

    /** A straight linac along z like {@link AcceleratorTestBuilder#linac}, {@link #LENGTH} blocks long. */
    private static void longLinac(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, AcceleratorRegistry.CONTROLLER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        helper.setBlock(SOURCE, AcceleratorRegistry.ION_SOURCE.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        int placedCavities = 0;
        for (int i = 0; i < LENGTH; i++) {
            Block block;
            if (i % 4 == 0) {
                block = AcceleratorRegistry.QUADRUPOLE.get();
            } else if (placedCavities < CAVITIES) {
                block = AcceleratorRegistry.RF_CAVITY.get();
                placedCavities++;
            } else {
                block = AcceleratorRegistry.BEAM_PIPE.get();
            }
            helper.setBlock(cell(i), AcceleratorTestBuilder.along(block, Direction.Axis.Z));
        }
        helper.setBlock(cell(LENGTH), AcceleratorRegistry.BEAM_DUMP.get().defaultBlockState());
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 200)
    public static void aLongLinacRepairedFarFromItsControllerFormsAgain(GameTestHelper helper) {
        longLinac(helper);
        BlockPos broken = cell(BROKEN_CELL);
        BlockState part = helper.getBlockState(broken);
        helper.runAfterDelay(5, () -> {
            AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.entity(helper, CONTROLLER,
                    AcceleratorControllerBlockEntity.class);
            helper.assertTrue(controller != null, "the controller is there");
            controller.attemptFormation(null);
            helper.assertTrue(controller.isFormed(), "the long linac forms: " + controller.validationText().getString());
            helper.assertTrue(Math.abs(broken.getZ() - CONTROLLER.getZ()) > AcceleratorControllerBlockEntity.UNFORMED_REACH,
                    "the part to break lies beyond the unformed reach");
        });
        helper.runAfterDelay(10, () -> helper.setBlock(broken, Blocks.AIR.defaultBlockState()));
        helper.runAfterDelay(15, () -> {
            AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.entity(helper, CONTROLLER,
                    AcceleratorControllerBlockEntity.class);
            helper.assertTrue(controller != null, "the controller is there");
            helper.assertFalse(controller.isFormed(), "taking a block out of the line unforms it");
            BoundingBox bounds = MultiblockRegistry.indexedBounds(controller);
            helper.assertTrue(bounds != null && bounds.isInside(helper.absolutePos(broken)),
                    "the unformed controller still listens where its line stood, got " + bounds);
            helper.setBlock(broken, part);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 15, "waiting for the repair");
            AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.entity(helper, CONTROLLER,
                    AcceleratorControllerBlockEntity.class);
            helper.assertTrue(controller != null, "the controller is there");
            helper.assertTrue(controller != null && controller.isFormed(), "putting the block back forms it again without a click");
        });
    }
}

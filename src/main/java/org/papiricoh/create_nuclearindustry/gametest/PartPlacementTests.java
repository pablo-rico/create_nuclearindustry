package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;
import java.util.function.Supplier;

/**
 * Ports, sensors and turbine controllers face the player who places them, whatever face was clicked:
 * set into a wall from outside they face out, without a block to click behind them.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PartPlacementTests {
    private static final List<Supplier<? extends Block>> PARTS = List.of(
            AllNuclearBlocks.REACTOR_FLUID_PORT, AllNuclearBlocks.REACTOR_FUEL_PORT, AllNuclearBlocks.REACTOR_REDSTONE_PORT,
            AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR, AllNuclearBlocks.COOLING_TOWER_FLUID_PORT,
            AllNuclearBlocks.TURBINE_OUTPUT, AllNuclearBlocks.TURBINE_FLUID_PORT, AllNuclearBlocks.TURBINE_EXHAUST_PORT,
            AllNuclearBlocks.FUSION_MAGNET_INPUT, AllNuclearBlocks.FUSION_FLUID_PORT, AllNuclearBlocks.FUSION_FUEL_INJECTOR,
            AllNuclearBlocks.FUSION_TURBINE_FLUID_PORT, AllNuclearBlocks.FUSION_PLASMA_TURBINE);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void partsFaceThePlayerNotTheClickedFace(GameTestHelper helper) {
        // Looking west at the wall, clicking the top of the block below: the part faces east, back at the player.
        expectFacing(helper, 90, 0, false, Direction.EAST);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void sneakingTurnsPartsAway(GameTestHelper helper) {
        expectFacing(helper, 90, 0, true, Direction.WEST);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void lookingDownFacesUp(GameTestHelper helper) {
        // Looking south and steeply down at a floor: the part faces up.
        expectFacing(helper, 0, 80, false, Direction.UP);
        helper.succeed();
    }

    private static void expectFacing(GameTestHelper helper, float yRot, float xRot, boolean sneaking, Direction expected) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(yRot);
        player.setYHeadRot(yRot);
        player.setXRot(xRot);
        player.setShiftKeyDown(sneaking);
        BlockPos below = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false);
        for (Supplier<? extends Block> part : PARTS) {
            Block block = part.get();
            BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block), hit));
            helper.assertTrue(state != null, block + " gave no state");
            helper.assertTrue(state.getValue(BlockStateProperties.FACING) == expected,
                    block + " should face " + expected + ", got " + state.getValue(BlockStateProperties.FACING));
        }
    }
}

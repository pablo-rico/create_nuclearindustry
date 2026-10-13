package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionTurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructureValidator;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/**
 * Builds a box turbine along Z with the controller facing north at {@code controller}
 * (relative). Ports default to one input on the top face and one output on the bottom face,
 * both one block in from the controller.
 */
public final class TurbineTestBuilder {

    private TurbineTestBuilder() {}

    /** Geometry helper with the same coordinates the validator uses. */
    public static TurbineStructure shape(TurbineTier tier, BlockPos controller, int width, int height, int length) {
        // axis Z: u = UP (height), v = EAST (width)
        return new TurbineStructure(tier, controller, Direction.NORTH, (height - 1) / 2, (width - 1) / 2, length);
    }

    static TurbineStructure build(GameTestHelper helper, TurbineTier tier, BlockPos controller, int width, int height, int length) {
        return build(BlockPlacer.of(helper), tier, controller, width, height, length);
    }

    /** The turbine of {@code width} x {@code height} x {@code length} with its controller at {@code controller} of {@code placer}. */
    public static TurbineStructure build(BlockPlacer helper, TurbineTier tier, BlockPos controller, int width, int height, int length) {
        TurbineStructure s = shape(tier, controller, width, height, length);
        BlockState casing = casing(tier).defaultBlockState();
        BlockState rotor = rotor(tier).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        for (int a = 0; a < s.length; a++) {
            for (int i = -s.hu; i <= s.hu; i++) {
                for (int j = -s.hv; j <= s.hv; j++) {
                    boolean wall = a == 0 || a == s.length - 1 || Math.abs(i) == s.hu || Math.abs(j) == s.hv;
                    if (wall) {
                        helper.set(s.at(a, i, j), casing);
                    } else if (i == 0 && j == 0) {
                        helper.set(s.at(a, i, j), rotor);
                    }
                }
            }
        }
        port(helper, tier, s.at(1, s.hu, 0), Direction.UP, ReactorFluidPortMode.INPUT);
        port(helper, tier, s.at(1, -s.hu, 0), Direction.DOWN, ReactorFluidPortMode.OUTPUT);
        helper.set(controller, controller(tier).defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        return s;
    }

    static void port(GameTestHelper helper, TurbineTier tier, BlockPos pos, Direction facing, ReactorFluidPortMode mode) {
        port(BlockPlacer.of(helper), tier, pos, facing, mode);
    }

    public static void port(BlockPlacer helper, TurbineTier tier, BlockPos pos, Direction facing, ReactorFluidPortMode mode) {
        BlockState state = tier == TurbineTier.STEAM
                ? AllNuclearBlocks.TURBINE_FLUID_PORT.get().defaultBlockState()
                        .setValue(TurbineFluidPortBlock.FACING, facing).setValue(TurbineFluidPortBlock.MODE, mode)
                : AllNuclearBlocks.FUSION_TURBINE_FLUID_PORT.get().defaultBlockState()
                        .setValue(FusionTurbineFluidPortBlock.FACING, facing).setValue(FusionTurbineFluidPortBlock.MODE, mode);
        helper.set(pos, state);
    }

    static MultiblockValidationResult<TurbineStructure> validate(GameTestHelper helper, TurbineTier tier, BlockPos controller) {
        return TurbineStructureValidator.validate(helper.getLevel(), helper.absolutePos(controller), tier);
    }

    static Block casing(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? AllNuclearBlocks.TURBINE_CASING.get() : AllNuclearBlocks.FUSION_TURBINE_CASING.get();
    }

    static Block rotor(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? AllNuclearBlocks.TURBINE_ROTOR.get() : AllNuclearBlocks.FUSION_TURBINE_ROTOR.get();
    }

    static Block controller(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? AllNuclearBlocks.TURBINE_OUTPUT.get() : AllNuclearBlocks.FUSION_PLASMA_TURBINE.get();
    }
}

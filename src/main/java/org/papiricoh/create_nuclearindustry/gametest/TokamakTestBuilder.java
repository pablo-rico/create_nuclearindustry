package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFuelInjectorBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionMagnetInputBlock;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

/**
 * Builds a complete tokamak in the 41x9x41 arena: coils on the sides of the cross-section,
 * breeding blankets on top and bottom, controller on the east equator, ports above and
 * below it, an injector on the west and a magnet drive on the south driven by a creative motor.
 */
final class TokamakTestBuilder {
    static final BlockPos CENTER = new BlockPos(20, 4, 20);

    private TokamakTestBuilder() {}

    static BlockPos controller(int radius) {
        return CENTER.offset(TokamakGeometry.controllerToAxis(radius), 0, 0);
    }

    static BlockPos injector(int radius) {
        return CENTER.offset(-TokamakGeometry.controllerToAxis(radius), 0, 0);
    }

    static BlockPos magnetInput(int radius) {
        return CENTER.offset(0, 0, TokamakGeometry.controllerToAxis(radius));
    }

    static BlockPos coolantPort(int radius) {
        return controller(radius).above();
    }

    static BlockPos steamPort(int radius) {
        return controller(radius).below();
    }

    static void build(GameTestHelper helper, int radius, boolean blankets, int motorRpm) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        BlockState coil = AllNuclearBlocks.FUSION_ELECTROMAGNET.get().defaultBlockState();
        BlockState blanket = (blankets ? AllNuclearBlocks.FUSION_BREEDING_BLANKET : AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT)
                .get().defaultBlockState();
        for (int i = 0; i < geometry.shellCount(); i++) {
            int dy = geometry.shellY(i);
            BlockPos pos = CENTER.offset(geometry.shellX(i), dy, geometry.shellZ(i));
            helper.setBlock(pos, Math.abs(dy) == TokamakGeometry.HALF_HEIGHT ? blanket : coil);
        }
        for (int dy = -TokamakGeometry.HALF_HEIGHT; dy <= TokamakGeometry.HALF_HEIGHT; dy++) {
            helper.setBlock(CENTER.offset(0, dy, 0), AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get());
        }

        helper.setBlock(coolantPort(radius), AllNuclearBlocks.FUSION_FLUID_PORT.get().defaultBlockState()
                .setValue(FusionFluidPortBlock.FACING, Direction.EAST).setValue(FusionFluidPortBlock.MODE, ReactorFluidPortMode.INPUT));
        helper.setBlock(steamPort(radius), AllNuclearBlocks.FUSION_FLUID_PORT.get().defaultBlockState()
                .setValue(FusionFluidPortBlock.FACING, Direction.EAST).setValue(FusionFluidPortBlock.MODE, ReactorFluidPortMode.OUTPUT));
        helper.setBlock(injector(radius), AllNuclearBlocks.FUSION_FUEL_INJECTOR.get().defaultBlockState()
                .setValue(FusionFuelInjectorBlock.FACING, Direction.WEST).setValue(FusionFuelInjectorBlock.MODE, ReactorFluidPortMode.INPUT));
        helper.setBlock(magnetInput(radius), AllNuclearBlocks.FUSION_MAGNET_INPUT.get().defaultBlockState()
                .setValue(FusionMagnetInputBlock.FACING, Direction.SOUTH));
        if (motorRpm > 0) {
            BlockPos motorPos = magnetInput(radius).south();
            Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
            helper.setBlock(motorPos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
            CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(motorPos);
            motorEntity.generatedSpeed.setValue(motorRpm);
        }
        helper.setBlock(controller(radius), AllNuclearBlocks.FUSION_CONTROLLER.get());
    }

    static FusionReactorBlockEntity tokamak(GameTestHelper helper, int radius) {
        return helper.getBlockEntity(controller(radius));
    }
}

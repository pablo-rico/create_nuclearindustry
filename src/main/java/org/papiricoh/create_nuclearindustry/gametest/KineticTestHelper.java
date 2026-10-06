package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlock;

/** Places a horizontal kinetic machine and drives it with a Create creative motor. */
final class KineticTestHelper {

    private KineticTestHelper() {}

    /**
     * Places {@code machine} at {@code pos} facing north with a creative motor behind it
     * (south) turning at {@code rpm}.
     */
    static void placePoweredMachine(GameTestHelper helper, BlockPos pos, Block machine, int rpm) {
        helper.setBlock(pos, machine.defaultBlockState().setValue(KineticProcessingBlock.FACING, Direction.NORTH));
        BlockPos motorPos = pos.south();
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(motorPos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(motorPos);
        motorEntity.generatedSpeed.setValue(rpm);
    }
}

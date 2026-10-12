package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantCompressorBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortMode;

/**
 * Builds a 3x3 cryogenic plant around {@code bottomCenter}: casing, the controller in the north wall
 * of the second layer, a warm helium inlet on the west wall and a liquid helium outlet on the east
 * wall of that layer, and compressors up the south wall, each driven by its own creative motor so
 * no single motor carries the whole plant's stress.
 */
public final class CryoplantTestBuilder {

    private CryoplantTestBuilder() {}

    static void build(GameTestHelper helper, BlockPos bottomCenter, int height, int compressors, int rpm) {
        build(BlockPlacer.of(helper), bottomCenter, height, compressors, rpm);
    }

    /** The plant standing on {@code bottomCenter} of {@code placer}, each compressor driven by a creative motor at {@code rpm}. */
    public static void build(BlockPlacer helper, BlockPos bottomCenter, int height, int compressors, int rpm) {
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.set(bottomCenter.offset(dx, y, dz), CryoRegistry.CRYOPLANT_CASING.get());
                }
            }
        }
        port(helper, heliumIn(bottomCenter), CryoplantPortMode.HELIUM_IN);
        port(helper, liquidOut(bottomCenter), CryoplantPortMode.LIQUID_HELIUM_OUT);
        for (int i = 0; i < compressors; i++) {
            BlockPos pos = compressor(bottomCenter, i);
            helper.set(pos, CryoRegistry.CRYOPLANT_COMPRESSOR.get().defaultBlockState()
                    .setValue(CryoplantCompressorBlock.FACING, Direction.SOUTH));
            motor(helper, pos.south(), Direction.NORTH, rpm);
        }
        helper.set(controller(bottomCenter), CryoRegistry.CRYOPLANT_CONTROLLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    static void port(GameTestHelper helper, BlockPos pos, CryoplantPortMode mode) {
        port(BlockPlacer.of(helper), pos, mode);
    }

    public static void port(BlockPlacer helper, BlockPos pos, CryoplantPortMode mode) {
        helper.set(pos, CryoRegistry.CRYOPLANT_PORT.get().defaultBlockState().setValue(CryoplantPortBlock.MODE, mode));
    }

    /** Places a Create creative motor at {@code pos} with its shaft toward {@code shaft}, turning at {@code rpm}. */
    static CreativeMotorBlockEntity motor(GameTestHelper helper, BlockPos pos, Direction shaft, int rpm) {
        return motor(BlockPlacer.of(helper), pos, shaft, rpm);
    }

    @Nullable
    public static CreativeMotorBlockEntity motor(BlockPlacer helper, BlockPos pos, Direction shaft, int rpm) {
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.set(pos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, shaft));
        CreativeMotorBlockEntity motorEntity = helper.blockEntity(pos, CreativeMotorBlockEntity.class);
        if (motorEntity != null) {
            motorEntity.generatedSpeed.setValue(rpm);
        }
        return motorEntity;
    }

    public static BlockPos controller(BlockPos bottomCenter) {
        return bottomCenter.offset(0, 1, -1);
    }

    static BlockPos heliumIn(BlockPos bottomCenter) {
        return bottomCenter.offset(-1, 1, 0);
    }

    static BlockPos liquidOut(BlockPos bottomCenter) {
        return bottomCenter.offset(1, 1, 0);
    }

    /** West wall of the bottom layer: free for an extra port. */
    static BlockPos spareWall(BlockPos bottomCenter) {
        return bottomCenter.offset(-1, 0, 0);
    }

    /** The {@code index}-th compressor, up the middle of the south wall. */
    static BlockPos compressor(BlockPos bottomCenter, int index) {
        return bottomCenter.offset(0, index, 1);
    }
}

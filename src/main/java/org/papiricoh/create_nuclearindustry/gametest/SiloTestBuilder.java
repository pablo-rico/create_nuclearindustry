package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * A 5×5 rocket silo in a {@link NuclearGameTests#SMALL_ARENA}: a full floor at y=1, walls up past the nose, the controller in
 * the middle of the north edge turned by a creative motor, and a one-stage rocket (engine, fueled tanks, guidance and an
 * inert warhead) on the centre column. Positions are relative to the test.
 */
final class SiloTestBuilder {
    static final BlockPos FLOOR = new BlockPos(6, 1, 6);
    static final BlockPos CONTROLLER = FLOOR.north(2);
    static final BlockPos MOTOR = CONTROLLER.north();

    /** Where {@link #port()} puts a fuel port in the wall: the middle of the east wall, one block up. */
    static final BlockPos PORT = FLOOR.offset(2, 1, 0);

    private int tanks = 1;
    private int rpm = 64;
    private boolean liquid;
    private boolean fueled = true;
    private boolean port;

    /** Tanks full of rocket fuel and liquid oxygen instead of cartridges. */
    SiloTestBuilder liquid() {
        this.liquid = true;
        return this;
    }

    /** Empty tanks. */
    SiloTestBuilder dry() {
        this.fueled = false;
        return this;
    }

    /** A fuel port at {@link #PORT} instead of a wall block. */
    SiloTestBuilder port() {
        this.port = true;
        return this;
    }

    SiloTestBuilder tanks(int tanks) {
        this.tanks = tanks;
        return this;
    }

    SiloTestBuilder rpm(int rpm) {
        this.rpm = rpm;
        return this;
    }

    /** The rocket's height in sections. */
    int height() {
        return tanks + 3;
    }

    SiloControllerEntity build(GameTestHelper helper) {
        Block wall = SiloRegistry.WALL.get();
        int height = height();
        for (int y = 0; y <= height; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (y == 0 || Math.abs(x) == 2 || Math.abs(z) == 2) helper.setBlock(FLOOR.offset(x, y, z), wall);
                }
            }
        }
        List<Block> column = new ArrayList<>();
        column.add(SiloRegistry.ENGINE.get());
        for (int i = 0; i < tanks; i++) column.add(SiloRegistry.TANK.get());
        column.add(SiloRegistry.GUIDANCE.get());
        column.add(SiloRegistry.WARHEAD.get());
        for (int i = 0; i < column.size(); i++) {
            BlockPos pos = FLOOR.above(i + 1);
            helper.setBlock(pos, column.get(i));
            if (column.get(i) == SiloRegistry.TANK.get() && fueled) {
                RocketSectionEntity tank = helper.getBlockEntity(pos);
                if (liquid) fill(tank);
                else tank.contents().setStackInSlot(0, new ItemStack(SiloRegistry.PROPELLANT.get()));
            }
        }
        if (port) helper.setBlock(PORT, SiloRegistry.FUEL_PORT.get());
        helper.setBlock(CONTROLLER, SiloRegistry.CONTROLLER.get().defaultBlockState().setValue(SiloControllerBlock.FACING, Direction.NORTH));
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(MOTOR, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(MOTOR);
        motorEntity.generatedSpeed.setValue(rpm);
        clearExit(helper, height);
        return helper.getBlockEntity(CONTROLLER);
    }

    /** Fills a tank with both liquids. */
    static void fill(RocketSectionEntity tank) {
        tank.fuel().fill(new FluidStack(AllNuclearFluids.ROCKET_FUEL.get(), PropellantModel.TANK_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        tank.oxidizer().fill(new FluidStack(AllNuclearFluids.LIQUID_OXYGEN.get(), PropellantModel.TANK_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
    }

    /** The test is encased in barriers: opens the 3×3 shaft above the nose up to the build limit, as a silo needs. */
    static void clearExit(GameTestHelper helper, int height) {
        BlockPos floor = helper.absolutePos(FLOOR);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = floor.getY() + height + 1; y < helper.getLevel().getMaxBuildHeight(); y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    at.set(floor.getX() + x, y, floor.getZ() + z);
                    if (!helper.getLevel().getBlockState(at).isAir()) helper.getLevel().setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}

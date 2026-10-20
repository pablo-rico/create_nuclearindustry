package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.silo.*;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionBlock.Joint;

/** What the launch site's blocks show of their state: a formed base, a rocket's joints, its tanks and its warhead. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class MissileArtStateTests {
    private static final String BATCH = "missile_art";

    /** A controller forms its 3×3 base into one launch table, and gives it back when it is broken. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void controllerFormsAndClearsItsBase(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().platform().tanks(1);
        SiloControllerEntity silo = SiloTests.aimed(helper, builder, helper.makeMockPlayer(GameType.SURVIVAL));
        boolean[] broken = {false};
        helper.runAfterDelay(5, () -> {
            silo.validate();
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                LaunchBaseBlock.Part part = helper.getBlockState(SiloTestBuilder.FLOOR.offset(x, 0, z)).getValue(LaunchBaseBlock.PART);
                helper.assertTrue(part == LaunchBaseBlock.Part.of(LaunchSiteGeometry.part(x, z)), "pad " + x + "," + z + " formed, got " + part);
            }
            helper.assertTrue(helper.getBlockState(SiloTestBuilder.FLOOR).getValue(LaunchBaseBlock.PART) == LaunchBaseBlock.Part.C, "centre pad");
            helper.setBlock(builder.controllerPos(), Blocks.AIR);
            broken[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(broken[0], "controller broken");
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                helper.assertTrue(helper.getBlockState(SiloTestBuilder.FLOOR.offset(x, 0, z)).getValue(LaunchBaseBlock.PART) == LaunchBaseBlock.Part.NONE,
                        "pad " + x + "," + z + " loose again");
        });
    }

    /** A stack draws one fuselage: joints only where two kinds of section meet, bare ends at the bottom and the nose. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void stackedSectionsKnowTheirJoints(GameTestHelper helper) {
        new SiloTestBuilder().platform().tanks(2).build(helper);
        // engine, tank, tank, guidance, warhead from the base up
        Joint[][] expected = {{Joint.NONE, Joint.OTHER}, {Joint.OTHER, Joint.SAME}, {Joint.SAME, Joint.OTHER}, {Joint.OTHER, Joint.OTHER}, {Joint.OTHER, Joint.NONE}};
        helper.succeedWhen(() -> {
            for (int i = 0; i < expected.length; i++) {
                BlockState state = helper.getBlockState(SiloTestBuilder.FLOOR.above(i + 1));
                helper.assertTrue(state.getBlock() instanceof RocketSectionBlock, "section " + i);
                helper.assertTrue(state.getValue(RocketSectionBlock.BOTTOM) == expected[i][0], "section " + i + " bottom " + state.getValue(RocketSectionBlock.BOTTOM));
                helper.assertTrue(state.getValue(RocketSectionBlock.TOP) == expected[i][1], "section " + i + " top " + state.getValue(RocketSectionBlock.TOP));
            }
        });
    }

    /** Tanks show a solid cartridge, or liquids by how full they are; emptied they go bare again. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void tanksShowTheirPropellant(GameTestHelper helper) {
        BlockPos solid = new BlockPos(2, 2, 2), liquid = new BlockPos(4, 2, 2), half = new BlockPos(6, 2, 2);
        for (BlockPos pos : new BlockPos[]{solid, liquid, half}) helper.setBlock(pos, SiloRegistry.TANK.get());
        RocketSectionEntity cartridge = helper.getBlockEntity(solid);
        cartridge.contents().setStackInSlot(0, new ItemStack(SiloRegistry.PROPELLANT.get()));
        SiloTestBuilder.fill(helper.getBlockEntity(liquid));
        RocketSectionEntity halfFull = helper.getBlockEntity(half);
        halfFull.fuel().fill(new net.neoforged.neoforge.fluids.FluidStack(org.papiricoh.create_nuclearindustry.AllNuclearFluids.ROCKET_FUEL.get(),
                org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel.TANK_CAPACITY), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        boolean[] emptied = {false};
        helper.runAfterDelay(5, () -> {
            assertTank(helper, solid, RocketTankBlock.Propellant.SOLID, 0);
            assertTank(helper, liquid, RocketTankBlock.Propellant.LIQUID, 4);
            assertTank(helper, half, RocketTankBlock.Propellant.LIQUID, 2);
            cartridge.contents().setStackInSlot(0, ItemStack.EMPTY);
            emptied[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(emptied[0], "cartridge taken out");
            assertTank(helper, solid, RocketTankBlock.Propellant.EMPTY, 0);
        });
    }

    private static void assertTank(GameTestHelper helper, BlockPos pos, RocketTankBlock.Propellant propellant, int fill) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.getValue(RocketTankBlock.PROPELLANT) == propellant && state.getValue(RocketTankBlock.FILL) == fill,
                "tank at " + pos + " shows " + propellant + " " + fill + ", got " + state.getValue(RocketTankBlock.PROPELLANT) + " " + state.getValue(RocketTankBlock.FILL));
    }

    /** A warhead shows what it is armed with. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void warheadShowsItsCharge(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, SiloRegistry.WARHEAD.get());
        RocketSectionEntity warhead = helper.getBlockEntity(pos);
        helper.assertTrue(helper.getBlockState(pos).getValue(RocketWarheadBlock.CHARGE) == RocketWarheadBlock.Charge.EMPTY, "empty warhead");
        warhead.contents().setStackInSlot(WarheadBay.CHARGE[0], new ItemStack(Items.TNT, 4));
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(pos).getValue(RocketWarheadBlock.CHARGE) == RocketWarheadBlock.Charge.CONVENTIONAL,
                "TNT shows conventional, got " + helper.getBlockState(pos).getValue(RocketWarheadBlock.CHARGE)));
    }

    /** The controller turns with a shaft from below and not with one at its side. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void controllerIsDrivenFromBelow(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().platform().tanks(1);
        SiloControllerEntity silo = builder.build(helper);
        boolean[] moved = {false};
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(silo.getSpeed() != 0, "turns from below");
            Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
            helper.setBlock(builder.motorPos(), Blocks.AIR);
            BlockPos side = builder.controllerPos().north();
            helper.setBlock(side, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH));
            CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(side);
            motorEntity.generatedSpeed.setValue(64);
            moved[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(moved[0] && helper.getTick() > 20, "motor moved to the side");
            helper.assertTrue(silo.getSpeed() == 0, "a side shaft does not drive it, got " + silo.getSpeed());
        });
    }
}

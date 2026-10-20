package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketContraption;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketScan;
import org.papiricoh.create_nuclearindustry.missile.silo.*;

/** Open platforms and legacy silos share the same rocket, fueling and launch rules. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class LaunchBaseTests {
    private static final String BATCH = "launch_base";

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void platformWithoutWallsIsReady(GameTestHelper helper) {
        SiloControllerEntity silo = SiloTests.aimed(helper, new SiloTestBuilder().platform().tanks(2), helper.makeMockPlayer(GameType.SURVIVAL));
        helper.succeedWhen(() -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.READY, "open platform ready, got " + silo.state() + " " + silo.error());
            helper.assertTrue(silo.floor().equals(helper.absolutePos(SiloTestBuilder.FLOOR)), "finds the base centre");
            helper.assertTrue(silo.shaftHeight() == 0, "an open platform has no shaft");
            helper.assertTrue(silo.rocketHeight() == 5, "sees the whole rocket");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 240)
    public static void controllerFindsBaseOnEverySide(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int index = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) for (int lateral = -1; lateral <= 1; lateral++) {
            SiloTestBuilder builder = new SiloTestBuilder().platform().tanks(2).controllerSide(side, lateral);
            int start = 1 + 15 * index++;
            SiloControllerEntity[] silo = new SiloControllerEntity[1];
            helper.runAfterDelay(start, () -> silo[0] = SiloTests.aimed(helper, builder, player));
            helper.runAfterDelay(start + 10, () -> {
                helper.assertTrue(silo[0] != null, "controller exists");
                silo[0].validate();
                helper.assertTrue(silo[0].floor().equals(helper.absolutePos(SiloTestBuilder.FLOOR)), "finds base from " + builder.controllerPos());
                helper.assertTrue(silo[0].state() == SiloControllerEntity.State.READY, "ready from " + builder.controllerPos() + ": " + silo[0].error());
                helper.setBlock(builder.controllerPos(), Blocks.AIR);
                helper.setBlock(builder.motorPos(), Blocks.AIR);
            });
        }
        helper.runAfterDelay(180, helper::succeed);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void legacySiloWithoutBaseTagKeepsFloor(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().tanks(2).build(helper);
        var registries = helper.getLevel().registryAccess();
        CompoundTag old = silo.saveWithoutMetadata(registries);
        old.remove("Base");
        old.remove("Shaft");
        silo.loadWithComponents(old, registries);
        BlockPos expected = helper.absolutePos(SiloTestBuilder.FLOOR);
        helper.assertTrue(silo.floor().equals(expected), "old saves use the facing-derived floor before validation");
        silo.validate();
        helper.assertTrue(silo.floor().equals(expected), "legacy candidate wins after validation");
        helper.assertTrue(silo.saveWithoutMetadata(registries).getLong("Base") == expected.asLong(), "resolved base is persisted");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void ambiguousBasePrefersRocketColumn(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().platform().tanks(2).build(helper);
        // Both the legacy centre and its eastern neighbour have complete 3×3 bases.
        for (int z = -1; z <= 1; z++) helper.setBlock(SiloTestBuilder.FLOOR.offset(2, 0, z), SiloRegistry.LAUNCH_BASE.get());
        helper.setBlock(SiloTestBuilder.FLOOR.above(), Blocks.AIR);
        BlockPos alternative = SiloTestBuilder.FLOOR.east();
        helper.setBlock(alternative.above(), SiloRegistry.ENGINE.get());
        silo.validate();
        helper.assertTrue(silo.floor().equals(helper.absolutePos(alternative)), "prefers the candidate supporting a rocket");
        var registries = helper.getLevel().registryAccess();
        SiloControllerEntity client = new SiloControllerEntity(silo.getBlockPos(), silo.getBlockState());
        client.handleUpdateTag(silo.getUpdateTag(registries), registries);
        helper.assertTrue(client.floor().equals(silo.floor()), "client receives a base different from the legacy floor");
        silo.loadWithComponents(silo.saveWithoutMetadata(registries), registries);
        helper.assertTrue(silo.floor().equals(helper.absolutePos(alternative)), "stored base survives reloading");
        helper.setBlock(SiloTestBuilder.FLOOR.above(), SiloRegistry.ENGINE.get());
        silo.validate();
        helper.assertTrue(silo.floor().equals(helper.absolutePos(alternative)), "valid stored base stays sticky even when legacy also has a rocket");
        // Breaking that base makes it discover the remaining complete candidate again.
        helper.setBlock(SiloTestBuilder.FLOOR.offset(2, 0, 0), Blocks.AIR);
        silo.validate();
        helper.assertTrue(silo.floor().equals(helper.absolutePos(SiloTestBuilder.FLOOR)), "rediscovers a base when the stored one is broken");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void clutterInClearanceIsReported(GameTestHelper helper) {
        SiloControllerEntity silo = SiloTests.aimed(helper, new SiloTestBuilder().platform().tanks(2), helper.makeMockPlayer(GameType.SURVIVAL));
        BlockPos clutter = SiloTestBuilder.FLOOR.offset(-1, 2, 1);
        helper.setBlock(clutter, Blocks.STONE);
        helper.succeedWhen(() -> {
            helper.assertTrue("interior".equals(silo.error()), "clearance error, got " + silo.error());
            helper.assertTrue(silo.errorPos().equals(helper.absolutePos(clutter)), "points at the obstruction");
            helper.assertTrue(silo.errorMessage().startsWith("silo.create_nuclearindustry.error.clearance|"), "uses platform-neutral text");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void blockedExitOverPlatformIsReported(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().platform().tanks(2);
        SiloControllerEntity silo = SiloTests.aimed(helper, builder, helper.makeMockPlayer(GameType.SURVIVAL));
        BlockPos cap = SiloTestBuilder.FLOOR.above(builder.height() + 12).west();
        helper.setBlock(cap, Blocks.STONE);
        helper.succeedWhen(() -> {
            helper.assertTrue("exit".equals(silo.error()), "exit error, got " + silo.error());
            helper.assertTrue(silo.errorPos().equals(helper.absolutePos(cap)), "points at the cap high above the platform");
        });
    }

    private static void fuelThrough(GameTestHelper helper, BlockPos port) {
        SiloControllerEntity silo = new SiloTestBuilder().platform().dry().tanks(2).controllerSide(Direction.EAST, 1).build(helper);
        helper.setBlock(port, SiloRegistry.FUEL_PORT.get());
        helper.runAfterDelay(5, () -> {
            silo.validate();
            IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(port), Direction.DOWN);
            helper.assertTrue(handler != null, "port beside or under the detected base exposes its tanks");
            int amount = 2 * PropellantModel.TANK_CAPACITY;
            helper.assertTrue(handler.fill(new FluidStack(AllNuclearFluids.ROCKET_FUEL.get(), amount), IFluidHandler.FluidAction.EXECUTE) == amount, "fills fuel");
            helper.assertTrue(handler.fill(new FluidStack(AllNuclearFluids.LIQUID_OXYGEN.get(), amount), IFluidHandler.FluidAction.EXECUTE) == amount, "fills oxidizer");
            for (int y = 2; y <= 3; y++) {
                RocketSectionEntity tank = helper.getBlockEntity(SiloTestBuilder.FLOOR.above(y));
                helper.assertTrue(tank != null, "tank exists");
                helper.assertTrue(tank.fuel().getFluidAmount() == PropellantModel.TANK_CAPACITY, "tank has fuel");
                helper.assertTrue(tank.oxidizer().getFluidAmount() == PropellantModel.TANK_CAPACITY, "tank has oxidizer");
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void fuelPortBesideBaseFillsTanks(GameTestHelper helper) {
        fuelThrough(helper, SiloTestBuilder.FLOOR.west(2));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void fuelPortUnderBaseFillsTanks(GameTestHelper helper) {
        fuelThrough(helper, SiloTestBuilder.FLOOR.below());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void fuelPortAwayFromBaseIsInert(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().platform().dry().tanks(2).build(helper);
        BlockPos port = SiloTestBuilder.FLOOR.offset(3, 0, 0);
        helper.setBlock(port, SiloRegistry.FUEL_PORT.get());
        helper.runAfterDelay(5, () -> {
            silo.validate();
            IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(port), Direction.WEST);
            helper.assertTrue(handler == null, "port away from the base exposes no handler");
            for (RocketSectionEntity tank : silo.columnTanks()) helper.assertTrue(tank.fuel().isEmpty() && tank.oxidizer().isEmpty(), "remote port cannot fill tanks");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void launchBaseIsNeverCaptured(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().platform().tanks(2).build(helper);
        silo.validate();
        helper.assertTrue(silo.rocket() != null, "valid column");
        RocketScan scan = RocketScan.scan(helper.getLevel(), silo.floor(), silo.rocket());
        helper.assertTrue(scan.valid() && scan.contraption() != null, "rocket assembles on a platform");
        helper.assertTrue(scan.captured().size() == 5, "only rocket sections are captured");
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            helper.assertTrue(!scan.captured().contains(silo.floor().offset(x, 0, z)), "base stays behind");
        helper.assertTrue(RocketContraption.isSilo(SiloRegistry.LAUNCH_BASE.get().defaultBlockState()), "launch bases are immovable to the rocket");
        helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(), new AABB(silo.floor().getX(), silo.floor().getY(), silo.floor().getZ(), silo.floor().getX() + 1, silo.floor().getY() + 2, silo.floor().getZ() + 1)));
        RocketScan glued = RocketScan.scan(helper.getLevel(), silo.floor(), silo.rocket());
        helper.assertTrue(!glued.valid(), "gluing the engine to its base prevents launch");
        helper.assertTrue(glued.captured().isEmpty(), "glue cannot capture the base");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 400)
    public static void platformLaunchRemovesRocketKeepsBase(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SiloTestBuilder builder = new SiloTestBuilder().platform().tanks(2).controllerSide(Direction.WEST, -1);
        SiloControllerEntity silo = SiloTests.aimed(helper, builder, player);
        FlightTracker tracker = FlightTracker.get(helper.getLevel());
        helper.runAfterDelay(10, () -> {
            silo.arm(player);
            helper.assertTrue(silo.state() == SiloControllerEntity.State.ARMED, "platform arms, got " + silo.error());
            helper.setBlock(builder.controllerPos().above(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> {
            Flight flight = tracker.flights().stream().filter(f -> helper.absolutePos(SiloTestBuilder.FLOOR).above().equals(f.origin())).findFirst().orElse(null);
            helper.assertTrue(flight != null, "platform launches, got " + silo.state() + " " + silo.error());
            helper.assertTrue(silo.state() == SiloControllerEntity.State.LAUNCHED, "controller records launch");
            for (int y = 1; y <= builder.height(); y++) helper.assertTrue(helper.getBlockState(SiloTestBuilder.FLOOR.above(y)).isAir(), "rocket section left " + y);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                helper.assertTrue(helper.getBlockState(SiloTestBuilder.FLOOR.offset(x, 0, z)).is(SiloRegistry.LAUNCH_BASE.get()), "base block stays");
            tracker.remove(helper.getLevel(), flight);
            helper.setBlock(builder.controllerPos().above(), Blocks.AIR);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void shaftHeightSyncsForLegacySilo(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().tanks(2);
        SiloControllerEntity silo = builder.build(helper);
        silo.validate();
        helper.assertTrue(silo.shaftHeight() == builder.height(), "legacy wall rings form a complete shaft");
        var registries = helper.getLevel().registryAccess();
        SiloControllerEntity client = new SiloControllerEntity(silo.getBlockPos(), silo.getBlockState());
        client.handleUpdateTag(silo.getUpdateTag(registries), registries);
        helper.assertTrue(client.shaftHeight() == builder.height(), "shaft height reaches a client replica");
        helper.assertTrue(client.floor().equals(silo.floor()), "resolved base reaches the client");
        helper.assertTrue(silo.saveWithoutMetadata(registries).getInt("Shaft") == builder.height(), "shaft is saved");
        helper.setBlock(SiloTestBuilder.FLOOR.offset(-2, 3, 0), Blocks.AIR);
        silo.validate();
        client.handleUpdateTag(silo.getUpdateTag(registries), registries);
        helper.assertTrue(client.shaftHeight() == 2, "a wall gap updates the synced shaft");
        helper.succeed();
    }
}

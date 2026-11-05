package org.papiricoh.create_nuclearindustry.gametest;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.dyson.DysonConfig;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorOrbitPayload;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EjectorStatus;
import org.papiricoh.create_nuclearindustry.dyson.ejector.EmRailEjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.receiver.RayReceiverBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.receiver.ReceiverStatus;
import org.papiricoh.create_nuclearindustry.dyson.swarm.DysonSwarm;
import org.papiricoh.create_nuclearindustry.dyson.swarm.OrbitShape;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SailCohorts;
import org.papiricoh.create_nuclearindustry.dyson.terminal.ConfigureSwarmOrbitPayload;
import org.papiricoh.create_nuclearindustry.dyson.terminal.SwarmTerminalBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Dyson swarm: ejectors launch sails only by day under an open sky into an open orbit, sails wear out, receivers share the
 * swarm's power and take none at night or under a roof, and the terminal's and ejector's payloads are checked. The swarm is the
 * world's, and the sun is too, so each case runs in a batch of its own that sets the time and weather first and puts everything
 * back after.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class DysonTests {
    private static final String DAY = "dyson_day", ROOF = "dyson_roof", NIGHT = "dyson_night", SHARE = "dyson_share",
            WEAR = "dyson_wear", PAYLOAD = "dyson_payload", CLOSED = "dyson_closed", FULL = "dyson_full";
    private static final BlockPos MOTOR = new BlockPos(4, 1, 4), EJECTOR = new BlockPos(4, 2, 4);
    private static long savedDayTime = -1;
    /** How far round the test chunks earlier arenas are looked for, in chunks: the test grid lays them out a few chunks apart. */
    private static final int SEARCH_CHUNKS = 16;

    private static void begin(ServerLevel level, long dayTime) {
        if (savedDayTime < 0) savedDayTime = level.getDayTime();
        level.setDayTime(dayTime);
        level.setWeatherParameters(12000, 0, false, false);
        clearOtherMachines(level);
        DysonSwarm.get(level).reset();
    }

    /**
     * Earlier tests' arenas stay in the world with their chunks forced, and their ejectors and receivers go on working: an ejector
     * left with sails fires them once a later batch makes it day, a receiver asks the swarm for a share. Each batch starts with
     * none but its own.
     */
    private static void clearOtherMachines(ServerLevel level) {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        List<BlockPos> machines = new ArrayList<>();
        for (long packed : level.getForcedChunks()) {
            ChunkPos centre = new ChunkPos(packed);
            for (int dx = -SEARCH_CHUNKS; dx <= SEARCH_CHUNKS; dx++) for (int dz = -SEARCH_CHUNKS; dz <= SEARCH_CHUNKS; dz++) {
                if (!seen.add(ChunkPos.asLong(centre.x + dx, centre.z + dz))) continue;
                LevelChunk chunk = level.getChunkSource().getChunkNow(centre.x + dx, centre.z + dz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values())
                    if (be instanceof EmRailEjectorBlockEntity || be instanceof RayReceiverBlockEntity) machines.add(be.getBlockPos());
            }
        }
        for (BlockPos pos : machines) {
            if (level.getBlockEntity(pos) instanceof EmRailEjectorBlockEntity ejector)
                for (int slot = 0; slot < ejector.magazine().getSlots(); slot++) ejector.magazine().extractItem(slot, Integer.MAX_VALUE, false);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void end(ServerLevel level) {
        if (savedDayTime >= 0) level.setDayTime(savedDayTime);
        savedDayTime = -1;
        DysonSwarm.get(level).reset();
    }

    @BeforeBatch(batch = DAY) public static void beforeDay(ServerLevel level) { begin(level, 6000); }
    @AfterBatch(batch = DAY) public static void afterDay(ServerLevel level) { end(level); }
    @BeforeBatch(batch = ROOF) public static void beforeRoof(ServerLevel level) { begin(level, 6000); }
    @AfterBatch(batch = ROOF) public static void afterRoof(ServerLevel level) { end(level); }
    @BeforeBatch(batch = NIGHT) public static void beforeNight(ServerLevel level) { begin(level, 18000); }
    @AfterBatch(batch = NIGHT) public static void afterNight(ServerLevel level) { end(level); }
    @BeforeBatch(batch = SHARE) public static void beforeShare(ServerLevel level) { begin(level, 6000); }
    @AfterBatch(batch = SHARE) public static void afterShare(ServerLevel level) { end(level); }
    @BeforeBatch(batch = WEAR) public static void beforeWear(ServerLevel level) { begin(level, 6000); }
    @AfterBatch(batch = WEAR) public static void afterWear(ServerLevel level) { end(level); }
    @BeforeBatch(batch = PAYLOAD) public static void beforePayload(ServerLevel level) { begin(level, 6000); }
    @AfterBatch(batch = PAYLOAD) public static void afterPayload(ServerLevel level) { end(level); }
    @BeforeBatch(batch = CLOSED) public static void beforeClosed(ServerLevel level) { begin(level, 6000); }
    @AfterBatch(batch = CLOSED) public static void afterClosed(ServerLevel level) { end(level); }
    @BeforeBatch(batch = FULL) public static void beforeFull(ServerLevel level) { begin(level, 6000); }
    @AfterBatch(batch = FULL) public static void afterFull(ServerLevel level) { end(level); }

    private static DysonSwarm swarm(GameTestHelper h) {
        return DysonSwarm.get(h.getLevel());
    }

    /** An ejector on a creative motor at {@code rpm}, loaded with {@code sails}. */
    private static EmRailEjectorBlockEntity ejector(GameTestHelper h, int rpm, int sails) {
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        h.setBlock(MOTOR, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        CreativeMotorBlockEntity drive = h.getBlockEntity(MOTOR);
        drive.generatedSpeed.setValue(rpm);
        h.setBlock(EJECTOR, DysonRegistry.EM_RAIL_EJECTOR.get());
        EmRailEjectorBlockEntity ejector = h.getBlockEntity(EJECTOR);
        if (sails > 0) ejector.magazine().insertItem(0, new ItemStack(DysonRegistry.SOLAR_SAIL.get(), sails), false);
        return ejector;
    }

    private static RayReceiverBlockEntity receiver(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, DysonRegistry.RAY_RECEIVER.get());
        return h.getBlockEntity(pos);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = DAY, timeoutTicks = 300)
    public static void anEjectorLaunchesSailsByDay(GameTestHelper h) {
        EmRailEjectorBlockEntity ejector = ejector(h, 256, 4);
        h.succeedWhen(() -> {
            h.assertTrue(ejector.status() == EjectorStatus.CHARGING || ejector.sailsLoaded() < 4,
                    "it charges by day under an open sky, but is " + ejector.status());
            h.assertTrue(swarm(h).sails(0) >= 1, "a sail reached orbit 1");
            h.assertTrue(ejector.sailsLoaded() == 4 - swarm(h).sails(0), "each sail in orbit left the magazine");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ROOF, timeoutTicks = 200)
    public static void ejectorsAndReceiversNeedTheSky(GameTestHelper h) {
        EmRailEjectorBlockEntity ejector = ejector(h, 256, 4);
        RayReceiverBlockEntity receiver = receiver(h, new BlockPos(8, 2, 8));
        h.setBlock(EJECTOR.above(4), Blocks.STONE);
        h.setBlock(new BlockPos(8, 6, 8), Blocks.STONE);
        swarm(h).addSails(0, 1000, h.getLevel().getGameTime() + 1_000_000);
        h.runAfterDelay(100, () -> {
            h.assertTrue(ejector.status() == EjectorStatus.NO_SKY, "a roofed ejector waits for the sky, but is " + ejector.status());
            h.assertTrue(ejector.sailsLoaded() == 4, "a roofed ejector launches nothing");
            h.assertTrue(receiver.status() == ReceiverStatus.NO_SKY && receiver.share() == 0, "a roofed receiver takes nothing");
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = NIGHT, timeoutTicks = 200)
    public static void nothingHappensAtNight(GameTestHelper h) {
        EmRailEjectorBlockEntity ejector = ejector(h, 256, 4);
        RayReceiverBlockEntity receiver = receiver(h, new BlockPos(8, 2, 8));
        swarm(h).addSails(0, 1000, h.getLevel().getGameTime() + 1_000_000);
        h.runAfterDelay(100, () -> {
            h.assertTrue(ejector.status() == EjectorStatus.SUN_DOWN, "an ejector waits for the sun, but is " + ejector.status());
            h.assertTrue(ejector.sailsLoaded() == 4, "nothing launched at night");
            h.assertTrue(receiver.share() == 0 && receiver.status() == ReceiverStatus.NO_SUN,
                    "a receiver takes nothing at night, but is " + receiver.status());
            h.assertTrue(swarm(h).pool() == 0, "the swarm beams nothing down at night");
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = SHARE, timeoutTicks = 100)
    public static void receiversShareTheSwarm(GameTestHelper h) {
        RayReceiverBlockEntity a = receiver(h, new BlockPos(3, 2, 3));
        RayReceiverBlockEntity b = receiver(h, new BlockPos(9, 2, 9));
        swarm(h).addSails(0, 1000, h.getLevel().getGameTime() + 1_000_000);
        // Nothing takes their energy, so their stores fill within a second of the first split and they stop asking: look then.
        h.succeedWhen(() -> {
            DysonSwarm swarm = swarm(h);
            h.assertTrue(a.share() > 0 && b.share() > 0, "both receive: " + a.status() + ", " + b.status() + " receivers=" + swarm.receivers() + " pool=" + swarm.pool() + " shares=" + a.share() + "/" + b.share());
            h.assertTrue(swarm.pool() > 0.9 * 1000 * DysonConfig.fePerSail, "a clear noon sends nearly all the sails' power: " + swarm.pool());
            h.assertTrue(a.share() == b.share(), "two receivers share alike: " + a.share() + " and " + b.share());
            h.assertTrue(a.share() <= DysonConfig.receiverMaxFe, "none takes more than its most");
            h.assertTrue(a.share() + b.share() <= swarm.pool(), "together they take no more than the swarm sends");
            h.assertTrue(a.status() == ReceiverStatus.RECEIVING && swarm.receivers() == 2, "both receive");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = FULL, timeoutTicks = 200)
    public static void aFullReceiverStopsAsking(GameTestHelper h) {
        RayReceiverBlockEntity receiver = receiver(h, new BlockPos(6, 2, 6));
        swarm(h).addSails(0, 1000, h.getLevel().getGameTime() + 1_000_000);
        h.runAfterDelay(100, () -> {
            h.assertTrue(receiver.energy().getEnergyStored() == receiver.energy().getMaxEnergyStored(),
                    "with nothing taking its energy its store fills");
            h.assertTrue(receiver.status() == ReceiverStatus.FULL && receiver.share() == 0, "full, it takes nothing more: " + receiver.status());
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = WEAR, timeoutTicks = 100)
    public static void sailsWearOut(GameTestHelper h) {
        long now = h.getLevel().getGameTime();
        DysonSwarm swarm = swarm(h);
        swarm.addSails(0, 10, Math.floorDiv(now, SailCohorts.BUCKET) * SailCohorts.BUCKET);
        swarm.addSails(1, 5, now + 1_000_000);
        h.succeedWhen(() -> {
            h.assertTrue(swarm.sails(0) == 0, "worn-out sails leave the swarm");
            h.assertTrue(swarm.sails(1) == 5, "sails still young stay");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = CLOSED, timeoutTicks = 200)
    public static void ejectorsLaunchOnlyIntoOpenOrbits(GameTestHelper h) {
        EmRailEjectorBlockEntity ejector = ejector(h, 256, 4);
        ejector.setOrbit(2);
        h.runAfterDelay(100, () -> {
            h.assertTrue(ejector.status() == EjectorStatus.ORBIT_CLOSED, "orbit 3 starts closed, but it is " + ejector.status());
            h.assertTrue(swarm(h).totalSails() == 0 && ejector.sailsLoaded() == 4, "nothing launched into a closed orbit: sails=" + swarm(h).sails(0) + "/" + swarm(h).sails(1) + "/" + swarm(h).sails(2) + " loaded=" + ejector.sailsLoaded());
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = PAYLOAD)
    public static void payloadsAreChecked(GameTestHelper h) {
        BlockPos at = new BlockPos(6, 2, 6);
        h.setBlock(at, DysonRegistry.SWARM_TERMINAL.get());
        SwarmTerminalBlockEntity terminal = h.getBlockEntity(at);
        EmRailEjectorBlockEntity ejector = ejector(h, 0, 0);
        BlockPos pos = terminal.getBlockPos();
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "dyson_test"));
        player.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        boolean accepted = ConfigureSwarmOrbitPayload.apply(player, new ConfigureSwarmOrbitPayload(pos, 1, true, 22, 40, 90));
        boolean tooClose = ConfigureSwarmOrbitPayload.apply(player, new ConfigureSwarmOrbitPayload(pos, 1, true, 5, 40, 90));
        boolean noOrbit = ConfigureSwarmOrbitPayload.apply(player, new ConfigureSwarmOrbitPayload(pos, 7, true, 22, 40, 90));
        boolean notATerminal = ConfigureSwarmOrbitPayload.apply(player, new ConfigureSwarmOrbitPayload(pos.above(), 1, false, 22, 40, 90));
        EjectorOrbitPayload.apply(player, new EjectorOrbitPayload(ejector.getBlockPos(), 1));
        int picked = ejector.orbit();
        EjectorOrbitPayload.apply(player, new EjectorOrbitPayload(ejector.getBlockPos(), 9));
        int afterBad = ejector.orbit();
        player.moveTo(pos.getX() + 100, pos.getY(), pos.getZ());
        boolean remote = ConfigureSwarmOrbitPayload.apply(player, new ConfigureSwarmOrbitPayload(pos, 1, false, 30, 10, 10));

        DysonSwarm swarm = swarm(h);
        swarm.addSails(1, 42, h.getLevel().getGameTime() + 100_000);
        DysonSwarm loaded = DysonSwarm.load(swarm.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.succeedWhen(() -> {
            OrbitShape shape = swarm.shape(1);
            h.assertTrue(accepted && shape.enabled() && shape.radius() == 22 && shape.inclination() == 40 && shape.node() == 90,
                    "a terminal opens and shapes an orbit");
            h.assertTrue(!tooClose && !noOrbit && !notATerminal, "out-of-range values, orbits and blocks are refused");
            h.assertTrue(!remote && swarm.shape(1).enabled(), "a far player cannot change the swarm");
            h.assertTrue(picked == 1 && afterBad == 1, "an ejector takes only an orbit that exists");
            h.assertTrue(loaded.sails(1) == 42 && loaded.shape(1).equals(shape), "the swarm saves and loads");
        });
    }
}

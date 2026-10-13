package org.papiricoh.create_nuclearindustry.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationStats;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentShape;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentSurvey;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Timings of the mod's hot paths, logged as {@code [benchmark]} lines: a running reactor's tick
 * and its client update, a containment survey, a burning tokamak's tick and the radiation update
 * of a player among sources. Optional (they measure, they do not judge), in their own batch so
 * nothing else runs beside them.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PerformanceBenchmarks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "benchmark";
    /**
     * The radiation benchmarks, with random ticks off for the whole batch so the waste barrels never
     * leak; the game rule is put back by {@link #afterRadiationBatch}, which runs however the batch ends.
     */
    private static final String RADIATION_BATCH = "benchmark_radiation";
    private static int savedRandomTickSpeed = -1;
    /** Untimed runs first, so the timed ones measure compiled code. */
    private static final int WARMUP = 5;

    /** Mean, minimum and maximum of a series of timed runs. */
    static final class Timer {
        private final String name;
        private long count;
        private long total;
        private long min = Long.MAX_VALUE;
        private long max;

        Timer(String name) {
            this.name = name;
        }

        void time(Runnable run) {
            long start = System.nanoTime();
            run.run();
            add(System.nanoTime() - start);
        }

        void add(long nanos) {
            count++;
            total += nanos;
            min = Math.min(min, nanos);
            max = Math.max(max, nanos);
        }

        double meanMicros() {
            return count == 0 ? 0 : total / 1000.0 / count;
        }

        void log(String extra) {
            LOGGER.info("[benchmark] {}: mean {} us, min {} us, max {} us over {} runs{}", name,
                    String.format("%.1f", meanMicros()), String.format("%.1f", min / 1000.0),
                    String.format("%.1f", max / 1000.0), count, extra.isEmpty() ? "" : " (" + extra + ")");
        }
    }

    /**
     * A running 5x5x5 reactor with its coolant topped up: each tick it is ticked once more by hand,
     * timed. Its update tag is measured too, since that is what each client sync costs.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, required = false, timeoutTicks = 300)
    public static void reactorTick(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        Timer timer = new Timer("reactor tick");
        int[] ticks = {0};
        int[] syncs = {0};
        helper.runAfterDelay(5, () -> {
            reactor.getPhysicsSimulator().loadFuelAssembly(20f);
            reactor.getPhysicsSimulator().loadFuelAssembly(20f);
        });
        helper.onEachTick(() -> {
            if (ticks[0] >= 0 && reactor.isFormed() && reactor.getPhysicsSimulator().getLoadedAssemblyCount() > 0) {
                if (ticks[0] == 0) {
                    syncs[0] = reactor.clientSyncCount();
                }
                reactor.fillCoolant(new FluidStack(Fluids.WATER, 16_000), IFluidHandler.FluidAction.EXECUTE);
                reactor.drainSteam(16_000, IFluidHandler.FluidAction.EXECUTE);
                Level level = helper.getLevel();
                BlockPos pos = reactor.getBlockPos();
                timer.time(() -> ReactorBlockEntity.tick(level, pos, level.getBlockState(pos), reactor));
                ticks[0]++;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(ticks[0] >= 100, "the reactor runs for 100 ticks");
            ticks[0] = -1;
            int sent = reactor.clientSyncCount() - syncs[0];
            CompoundTag full = reactor.getUpdateTag(helper.getLevel().registryAccess());
            // The first packet carries the structure; the steady state is the one after it.
            reactor.getUpdatePacket();
            var packet = reactor.getUpdatePacket();
            int packetBytes = packet == null ? 0 : packet.getTag().sizeInBytes();
            StringBuilder parts = new StringBuilder();
            full.getAllKeys().stream()
                    .sorted((a, b) -> Integer.compare(full.get(b).sizeInBytes(), full.get(a).sizeInBytes()))
                    .limit(5)
                    .forEach(key -> parts.append(", ").append(key).append(' ').append(full.get(key).sizeInBytes()).append(" B"));
            timer.log(sent + " client updates in 100 ticks; update tag " + full.sizeInBytes() + " B" + parts
                    + "; update packet " + packetBytes + " B; "
                    + reactor.getReactorState() + ", running " + reactor.getPhysicsSimulator().isRunning() + ", "
                    + String.format("%.0f", reactor.getCoreTemperature()) + " C");
            reactor.setManualScram(true);
        });
    }

    /** A survey of a 21x14x21 room, the most the small budget of a tick would spread over two ticks. */
    @GameTest(template = NuclearGameTests.PLANT_ARENA, batch = BATCH, required = false, timeoutTicks = 100)
    public static void containmentSurvey(GameTestHelper helper) {
        BlockPos min = new BlockPos(1, 1, 1);
        BlockPos max = new BlockPos(23, 16, 23);
        ContainmentTestBuilder.room(helper, min, max, PwrRegistry.CONTAINMENT_CONCRETE.get());
        helper.runAfterDelay(2, () -> {
            Timer timer = new Timer("containment survey");
            Timer analysis = new Timer("containment shape analysis");
            BlockPos origin = helper.absolutePos(new BlockPos(12, 2, 12));
            SealedVolume[] last = new SealedVolume[1];
            for (int i = 0; i < WARMUP; i++) {
                ContainmentShape.analyze(helper.getLevel(), ContainmentSurvey.measure(helper.getLevel(), origin, 32_768));
            }
            for (int i = 0; i < 20; i++) {
                timer.time(() -> last[0] = ContainmentSurvey.measure(helper.getLevel(), origin, 32_768));
                analysis.time(() -> ContainmentShape.analyze(helper.getLevel(), last[0]));
            }
            helper.assertTrue(last[0].state() == SealedVolume.State.SEALED, "the room is sealed");
            timer.log(last[0].volume() + " cells");
            analysis.log(last[0].wallCount() + " walls");
            ContainmentTestBuilder.clear(helper, min, max);
            helper.succeed();
        });
    }

    /** A burning R=4 tokamak, ticked once more by hand each tick and timed. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = BATCH, required = false, timeoutTicks = 400)
    public static void tokamakTick(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40, new AtomicBoolean(true));
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 8000, 8000, 16000);
            TokamakTestBuilder.primeAndStart(tokamak);
        });
        Timer timer = new Timer("tokamak tick");
        int[] ticks = {0};
        helper.onEachTick(() -> {
            if (ticks[0] >= 0 && tokamak.isFormed() && helper.getTick() > 25) {
                Level level = helper.getLevel();
                BlockPos pos = tokamak.getBlockPos();
                timer.time(() -> FusionReactorBlockEntity.tick(level, pos, level.getBlockState(pos), tokamak));
                ticks[0]++;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(ticks[0] >= 100, "the tokamak runs for 100 ticks");
            ticks[0] = -1;
            timer.log("phase " + tokamak.phase());
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }

    /**
     * A player among four waste barrels, behind a wall, near two emitters: the once-a-second
     * update, for players who stay put (warm, updated every 20 ticks as in play) and for one who
     * just arrived (cold). Random ticks are off for this batch ({@link #beforeRadiationBatch}), so
     * the barrels never leak.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = RADIATION_BATCH, required = false, timeoutTicks = 200)
    public static void radiationUpdate(GameTestHelper helper) {
        Level level = helper.getLevel();
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(5, y, 7), Blocks.STONE);
        }
        List<BlockPos> barrels = List.of(new BlockPos(4, 1, 6), new BlockPos(8, 1, 6), new BlockPos(6, 1, 4), new BlockPos(6, 3, 8));
        List<RadiationEmitter> emitters = new ArrayList<>();
        emitters.add(emitter(level, helper.absoluteVec(new Vec3(2.5, 2.5, 10.5))));
        emitters.add(emitter(level, helper.absoluteVec(new Vec3(10.5, 2.5, 2.5))));
        helper.runAfterDelay(2, () -> {
            barrels.forEach(barrel -> helper.setBlock(barrel, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get()));
            emitters.forEach(RadiationEmitters::register);
        });
        timeUpdates(helper, "among barrels", 3, () -> {
            emitters.forEach(RadiationEmitters::unregister);
            barrels.forEach(barrel -> helper.setBlock(barrel, Blocks.AIR));
        });
    }

    /**
     * The common case: nothing radioactive within a few blocks, three running sources 15 to 30
     * blocks away, one behind a wall.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = RADIATION_BATCH, required = false, timeoutTicks = 200)
    public static void radiationUpdateInTheOpen(GameTestHelper helper) {
        Level level = helper.getLevel();
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(10, y, 6), Blocks.STONE);
        }
        List<RadiationEmitter> emitters = new ArrayList<>();
        emitters.add(emitter(level, helper.absoluteVec(new Vec3(6.5 + 25, 2.5, 6.5))));
        emitters.add(emitter(level, helper.absoluteVec(new Vec3(6.5, 2.5, 6.5 - 20))));
        emitters.add(emitter(level, helper.absoluteVec(new Vec3(6.5 - 15, 2.5, 6.5 + 15))));
        helper.runAfterDelay(2, () -> emitters.forEach(RadiationEmitters::register));
        timeUpdates(helper, "in the open", 3, () -> emitters.forEach(RadiationEmitters::unregister));
    }

    @BeforeBatch(batch = RADIATION_BATCH)
    public static void beforeRadiationBatch(ServerLevel level) {
        GameRules.IntegerValue randomTicks = level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING);
        savedRandomTickSpeed = randomTicks.get();
        randomTicks.set(0, level.getServer());
    }

    /** Puts random ticks back; the game test runner calls it when the batch passes, fails or times out. */
    @AfterBatch(batch = RADIATION_BATCH)
    public static void afterRadiationBatch(ServerLevel level) {
        if (savedRandomTickSpeed >= 0) {
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(savedRandomTickSpeed, level.getServer());
            savedRandomTickSpeed = -1;
        }
    }

    /** Players kept standing at the spot, each updated every {@link #UPDATE_INTERVAL} ticks. */
    private static final int WARM_PLAYERS = 200;
    /** Ticks between two updates of a player, as {@code RadiationHandler.onPlayerTick} runs them. */
    private static final int UPDATE_INTERVAL = 20;

    /**
     * Times, at (6.5, 2, 6.5), the update of players who stay put at the real pace (each updated,
     * then updated again {@link #UPDATE_INTERVAL} ticks later, timed; a first round warms the JIT
     * up) and of players who just arrived (cold). Starts at {@code tick}, then runs {@code cleanup}
     * and succeeds.
     */
    private static void timeUpdates(GameTestHelper helper, String scene, int tick, Runnable cleanup) {
        Vec3 spot = helper.absoluteVec(new Vec3(6.5, 2, 6.5));
        Timer warmTimer = new Timer("radiation update " + scene + ", player standing still");
        Timer coldTimer = new Timer("radiation update " + scene + ", player just arrived");
        List<Player> warm = new ArrayList<>();
        String[] coldStats = {""};
        helper.runAfterDelay(tick, () -> {
            for (int round = 0; round < 2; round++) {
                // The first round warms the JIT up; only the second is kept.
                Timer coldRound = round == 0 ? new Timer("") : coldTimer;
                for (int i = 0; i < 500; i++) {
                    Player cold = helper.makeMockPlayer(GameType.SURVIVAL);
                    cold.setPos(spot);
                    coldRound.time(() -> RadiationHandler.update(cold));
                }
            }
            coldStats[0] = RadiationStats.last().toString();
            for (int i = 0; i < WARM_PLAYERS; i++) {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.setPos(spot);
                RadiationHandler.update(player);
                warm.add(player);
            }
        });
        // The first update a second later warms the JIT up; only the one after is kept.
        helper.runAfterDelay(tick + UPDATE_INTERVAL, () -> {
            Timer discard = new Timer("");
            warm.forEach(player -> discard.time(() -> RadiationHandler.update(player)));
        });
        helper.runAfterDelay(tick + 2L * UPDATE_INTERVAL, () -> {
            double rate;
            String warmStats;
            try {
                warm.forEach(player -> warmTimer.time(() -> RadiationHandler.update(player)));
                warmStats = RadiationStats.last().toString();
                rate = warm.isEmpty() ? 0 : warm.get(0).getData(AllNuclearRadiation.RATE);
            } finally {
                cleanup.run();
            }
            helper.assertTrue(warm.size() == WARM_PLAYERS && rate > 0, "the players are exposed");
            warmTimer.log(String.format("%.2f rad/s; ", rate) + warmStats);
            coldTimer.log(coldStats[0]);
            helper.succeed();
        });
    }

    static RadiationEmitter emitter(Level level, Vec3 origin) {
        return new RadiationEmitter() {
            @Override
            public Level getLevel() {
                return level;
            }

            @Override
            public boolean isRemoved() {
                return false;
            }

            @Override
            public Vec3 radiationOrigin() {
                return origin;
            }

            @Override
            public double radiationStrength() {
                return 5.0;
            }
        };
    }
}

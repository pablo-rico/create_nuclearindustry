package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Flights persist independently of entities and only radars expose their contacts. Live warheads
 * are taken out of the air before they land, so no test leaves fallout behind.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class MissileFlightTests {
    private static final String BATCH = "missile_flight";
    private static final BlockPos PAD = new BlockPos(6, 3, 6);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void aTrackedFlightIsSilentAndLands(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 start = Vec3.atCenterOf(helper.absolutePos(PAD));
        BlockPos target = helper.absolutePos(new BlockPos(2, 1, 2));
        Vec3 aim = Vec3.atCenterOf(target);
        FlightTracker tracker = FlightTracker.get(level);
        Flight flight = tracker.launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO, aim, target, Warhead.INERT, null);
        int predicted = Mth.ceil(MissileFlight.predict(MissileDesign.LEGACY_PAD, Flight.v3(start), Flight.v3(aim), Flight.v3(Vec3.ZERO),
                MissileConfig.flightParams()).ticks());
        long launched = level.getGameTime();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(ThreatRegistry.find(level, flight.id()) == null, "missiles need radar and never warn sirens automatically");
            helper.assertTrue(flight.position().y > start.y, "climbing away from its pad");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tracker.get(flight.id()) == null, "it lands");
            long flown = level.getGameTime() - launched;
            helper.assertTrue(flown <= predicted + 2, "on time: predicted " + predicted + ", flew " + flown);
            helper.assertTrue(ThreatRegistry.find(level, flight.id()) == null, "and its warning goes with it");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aSavedFlightFliesOnTheSame(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 start = Vec3.atCenterOf(helper.absolutePos(PAD));
        BlockPos target = helper.absolutePos(PAD).offset(1500, 0, 400);
        FlightTracker tracker = FlightTracker.get(level);
        Flight flight = tracker.launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO, Vec3.atCenterOf(target), target,
                Warhead.dirty(5.0f), null);
        helper.runAfterDelay(30, () -> {
            CompoundTag saved = tracker.save(new CompoundTag(), level.registryAccess());
            tracker.remove(level, flight);
            FlightTracker loaded = FlightTracker.load(saved, level.registryAccess());
            Flight copy = loaded.get(flight.id());
            helper.assertTrue(copy != null, "the flight is saved");
            helper.assertTrue(copy.warhead().kind() == Warhead.Kind.DIRTY && copy.target().equals(target), "with its warhead and target");
            for (int i = 0; i < 300; i++) {
                flight.flight().step(MissileConfig.flightParams());
                copy.flight().step(MissileConfig.flightParams());
            }
            helper.assertTrue(copy.position().distanceTo(flight.position()) < 1.0e-6, "and flies on exactly as it would have");
            helper.assertTrue(tracker.get(flight.id()) == null && ThreatRegistry.find(level, flight.id()) == null,
                    "removing a flight withdraws its warning");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void legacyLaunchPadIsDisabled(GameTestHelper helper) {
        helper.setBlock(PAD, MissileRegistry.LAUNCH_PAD.get());
        LaunchPadBlockEntity pad = helper.getBlockEntity(PAD);
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.MISSILE_SLOT, new ItemStack(MissileRegistry.MISSILE_ITEM.get()));
        helper.setBlock(PAD.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(pad.hasMissile(), "legacy platform retains its missile for extraction");
            helper.assertTrue(ThreatRegistry.find(helper.getLevel(), helper.absolutePos(PAD)) == null, "legacy platform is silent");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void modularLaunchReceiptsAndVisualsSurviveReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FlightTracker tracker = new FlightTracker();
        Vec3 start = Vec3.atCenterOf(helper.absolutePos(PAD));
        BlockPos target = helper.absolutePos(PAD).offset(400, 0, 0);
        java.util.UUID id = java.util.UUID.randomUUID();
        List<String> sections = List.of("minecraft:iron_block", "minecraft:iron_block", "minecraft:gold_block", "minecraft:diamond_block");
        Flight flight = tracker.launchModular(level, id, MissileDesign.LEGACY_PAD, start, Vec3.atCenterOf(target), target,
                Warhead.INERT, null, sections, List.of(0));
        helper.assertTrue(tracker.launchModular(level, id, MissileDesign.LEGACY_PAD, start, Vec3.atCenterOf(target), target,
                Warhead.INERT, null, sections, List.of(0)) == flight && tracker.flights().size() == 1, "duplicate transaction returns original flight");
        CompoundTag saved = tracker.save(new CompoundTag(), level.registryAccess());
        FlightTracker loaded = FlightTracker.load(saved, level.registryAccess());
        Flight restored = loaded.get(id);
        helper.assertTrue(restored != null && restored.visualSections().equals(sections)
                && restored.origin().equals(BlockPos.containing(start)), "modular composition and origin survive reload");
        loaded.remove(level, restored);
        loaded = FlightTracker.load(loaded.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        helper.assertTrue(loaded.hasLaunched(id) && loaded.launchModular(level, id, MissileDesign.LEGACY_PAD, start,
                Vec3.atCenterOf(target), target, Warhead.INERT, null, sections, List.of(0)) == null,
                "finished launches cannot replay after reload");
        CompoundTag oldFlight = saved.getList("Flights", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0);
        oldFlight.remove("FormatVersion"); oldFlight.remove("Origin"); oldFlight.remove("VisualSections"); oldFlight.remove("VisualStageStarts");
        Flight legacy = FlightTracker.load(saved, level.registryAccess()).get(id);
        helper.assertTrue(legacy.origin() == null && legacy.visualSections().isEmpty(), "old flights preserve unknown origin and legacy rendering");
        helper.succeed();
    }

    /**
     * Many missiles flying at once far from everyone: the tracker flies them on without loading a chunk
     * on their way or forcing one before their last moments. A tracker of its own, ticked by hand, so
     * the level's flights are not touched.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void distantFlightsLoadNoChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FlightTracker tracker = new FlightTracker();
        List<Flight> flights = launchFar(level, tracker, helper.absolutePos(BlockPos.ZERO), 32);
        LongSet forcedBefore = new LongOpenHashSet(level.getForcedChunks());
        List<Vec3> starts = flights.stream().map(Flight::position).toList();
        boolean loadedAny = false;
        try {
            for (int t = 0; t < 200; t++) {
                tracker.tick(level);
                for (Flight flight : flights) {
                    Vec3 at = flight.position();
                    loadedAny |= level.hasChunk(Mth.floor(at.x) >> 4, Mth.floor(at.z) >> 4);
                }
            }
        } finally {
            flights.forEach(flight -> tracker.remove(level, flight));
        }
        helper.assertTrue(!loadedAny, "no chunk under a distant flight is loaded");
        helper.assertTrue(level.getForcedChunks().equals(forcedBefore), "and none is forced");
        for (int i = 0; i < flights.size(); i++) {
            helper.assertTrue(flights.get(i).position().distanceTo(starts.get(i)) > 50, "every flight flies on");
        }
        helper.assertTrue(flights.stream().allMatch(f -> ThreatRegistry.find(level, f.id()) == null), "their warnings go with them");
        helper.succeed();
    }

    /**
     * {@code count} inert missiles launched into {@code tracker} some 20,000 blocks from {@code origin},
     * each on its own heading towards a target 3,000 blocks off, so all fly long and apart.
     */
    static List<Flight> launchFar(ServerLevel level, FlightTracker tracker, BlockPos origin, int count) {
        List<Flight> flights = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double heading = 2 * Math.PI * i / count;
            Vec3 start = new Vec3(origin.getX() + 20_000.5 + 200 * i, 120, origin.getZ() + 20_000.5);
            BlockPos target = BlockPos.containing(start.add(3000 * Math.cos(heading), -56, 3000 * Math.sin(heading)));
            flights.add(tracker.launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO, Vec3.atCenterOf(target), target, Warhead.INERT, null));
        }
        return flights;
    }
}

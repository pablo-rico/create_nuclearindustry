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
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatBoard;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;

/**
 * Missiles fly in their level's {@link FlightTracker}: they warn the sirens of their target all the
 * way, land when their flight says, survive a save and are what a launch pad now fires. Live warheads
 * are taken out of the air before they land, so no test leaves fallout behind.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class MissileFlightTests {
    private static final String BATCH = "missile_flight";
    private static final BlockPos PAD = new BlockPos(6, 3, 6);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void aTrackedFlightWarnsItsTargetAndLands(GameTestHelper helper) {
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
            ThreatBoard.Threat threat = ThreatRegistry.find(level, flight.id());
            helper.assertTrue(threat != null, "a missile in flight warns the sirens");
            helper.assertTrue(threat.x() == target.getX() && threat.z() == target.getZ(), "around where it comes down");
            long eta = threat.ticksToImpact(level.getGameTime());
            helper.assertTrue(Math.abs(eta - (predicted - 5)) <= 2, "landing when its flight says, expected " + (predicted - 5) + " got " + eta);
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

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aLaunchPadFiresIntoTheTracker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(PAD, MissileRegistry.LAUNCH_PAD.get());
        LaunchPadBlockEntity pad = helper.getBlockEntity(PAD);
        BlockPos at = helper.absolutePos(PAD);
        BlockPos target = at.offset(400, 0, 0);
        ItemStack designator = new ItemStack(MissileRegistry.TARGET_DESIGNATOR.get());
        designator.set(AllNuclearDataComponents.TARGET_POS.get(), target);
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.MISSILE_SLOT, new ItemStack(MissileRegistry.MISSILE_ITEM.get()));
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.DESIGNATOR_SLOT, designator);
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT, new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get()));
        helper.setBlock(PAD.east(), Blocks.REDSTONE_BLOCK);
        FlightTracker tracker = FlightTracker.get(level);
        helper.succeedWhen(() -> {
            Flight launched = tracker.flights().stream().filter(f -> f.target().equals(target)).findFirst().orElse(null);
            helper.assertTrue(launched != null, "the pad launches its missile into the tracker");
            tracker.remove(level, launched);
            helper.setBlock(PAD.east(), Blocks.AIR);
            helper.assertTrue(launched.warhead().kind() == Warhead.Kind.DIRTY, "carrying the dirty warhead it was loaded with");
            helper.assertTrue(!pad.hasMissile(), "and the pad is empty");
            helper.assertTrue(ThreatRegistry.find(level, at) == null, "its countdown warning handed over");
        });
    }
}

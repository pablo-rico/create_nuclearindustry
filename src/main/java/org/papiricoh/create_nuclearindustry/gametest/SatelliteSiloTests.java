package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.orbit.*;
import org.papiricoh.create_nuclearindustry.missile.silo.*;
import java.util.UUID;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SatelliteSiloTests {
    private static final String BATCH = "satellite_silo";
    private static SiloControllerEntity satellite(GameTestHelper h, int stages, boolean loaded) {
        SiloTestBuilder builder = new SiloTestBuilder().liquid().tanks(4).stages(stages);
        SiloControllerEntity controller = builder.build(h);
        BlockPos at = SiloTestBuilder.FLOOR.above(builder.height());
        h.setBlock(at, SiloRegistry.SATELLITE_FAIRING.get());
        RocketSectionEntity fairing = h.getBlockEntity(at);
        if (loaded) fairing.contents().insertItem(0, new ItemStack(OrbitRegistry.COMMS_SATELLITE.get()), false);
        controller.setSpeed((float) MissileConfig.minimumRpm);
        controller.validate();
        return controller;
    }
    private static Orbit leo(GameTestHelper h) {
        return OrbitConfig.leo(h.absolutePos(SiloTestBuilder.FLOOR).getX(), h.absolutePos(SiloTestBuilder.FLOOR).getZ(), .5,
                h.getLevel().getGameTime(), OrbitConfig.bounds(h.getLevel().getWorldBorder()));
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void emptyFairingRequiresSatellite(GameTestHelper h) {
        var controller = satellite(h, 1, false);
        h.succeedWhen(() -> h.assertTrue(controller.error().equals("satellite"), "empty fairing reports satellite"));
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void loadedFairingReadyWithoutTarget(GameTestHelper h) {
        var controller = satellite(h, 2, true);
        var diagram = LaunchDiagrams.build(controller);
        var fairing = (RocketSectionEntity) h.getLevel().getBlockEntity(controller.floor().above(controller.rocketHeight()));
        var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, fairing.getBlockPos(), null);
        h.succeedWhen(() -> {
            h.assertTrue(controller.state() == SiloControllerEntity.State.READY && controller.target() == null, "satellite ready without a target");
            h.assertTrue(controller.satelliteKind() == SatelliteKind.COMMS, "fairing kind exposed");
            h.assertTrue(handler != null && handler.getSlots() == 1, "fairing exposes automation slot");
            h.assertTrue(diagram.column().stream().anyMatch(c -> c.section() == RocketComposition.Section.SATELLITE.ordinal() && (c.flags() & LaunchDiagram.Cell.LOADED) != 0), "diagram shows loaded fairing");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void fairingAcceptsHandAndAutomationLoading(GameTestHelper h) {
        var controller = satellite(h, 1, false);
        RocketSectionEntity fairing = h.getBlockEntity(SiloTestBuilder.FLOOR.above(7));
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(OrbitRegistry.EARLY_WARNING_SATELLITE.get()));
        fairing.insert(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean hand = SatelliteItem.kindOf(fairing.contents().getStackInSlot(0)) == SatelliteKind.EARLY_WARNING;
        var description = fairing.description().getContents();
        boolean describesKind = description instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && java.util.Arrays.stream(text.getArgs()).anyMatch(arg -> arg instanceof net.minecraft.network.chat.Component component
                && component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents detail
                && detail.getKey().equals("satellite.create_nuclearindustry.kind.early_warning"));
        fairing.unload(player);
        boolean extracted = fairing.contents().getStackInSlot(0).isEmpty();
        var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, fairing.getBlockPos(), null);
        boolean automated = handler != null && handler.insertItem(0, new ItemStack(OrbitRegistry.COMMS_SATELLITE.get()), false).isEmpty();
        boolean rejectsOther = handler != null && !handler.insertItem(0, new ItemStack(net.minecraft.world.item.Items.STONE), true).isEmpty();
        h.succeedWhen(() -> {
            h.assertTrue(hand && extracted, "hand inserts and extracts satellite");
            h.assertTrue(describesKind, "description names satellite kind");
            h.assertTrue(automated && rejectsOther, "automation accepts only satellite payloads");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void redstoneCannotLaunch(GameTestHelper h) {
        var controller = satellite(h, 2, true);
        controller.arm(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        h.setBlock(SiloTestBuilder.CONTROLLER.north(), Blocks.REDSTONE_BLOCK);
        long start = h.getLevel().getGameTime();
        h.succeedWhen(() -> {
            h.assertTrue(h.getLevel().getGameTime() >= start + 30, "wait for redstone");
            h.assertTrue(controller.state() == SiloControllerEntity.State.READY, "redstone leaves satellite ready");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void liquidStageOrbitReach(GameTestHelper h) {
        OrbitType one = satellite(h, 1, true).orbitReach();
        OrbitType two = satellite(h, 2, true).orbitReach();
        OrbitType three = satellite(h, 3, true).orbitReach();
        h.succeedWhen(() -> {
            h.assertTrue(one == null, "one stage cannot reach orbit");
            h.assertTrue(two == OrbitType.LEO, "two stages reach low orbit");
            h.assertTrue(three == OrbitType.GEO, "three stages reach GEO");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void refusesUnreachableOrbitAndFullNetwork(GameTestHelper h) {
        var controller = satellite(h, 1, true);
        var unreachable = controller.launchSatellite((UUID) null, leo(h), "S2NO");
        controller = satellite(h, 3, true);
        int old = OrbitConfig.maxSatellitesPerCode;
        SiloControllerEntity.SatLaunchResult full;
        var occupied = new Satellite(UUID.randomUUID(), "CM-F", "S2FL", SatelliteKind.COMMS, leo(h), null, h.getLevel().getGameTime());
        SatelliteTracker.get(h.getLevel()).add(h.getLevel(), occupied);
        try { OrbitConfig.maxSatellitesPerCode = 1; full = controller.launchSatellite((UUID) null, leo(h), "S2FL"); }
        finally {
            OrbitConfig.maxSatellitesPerCode = old;
            SatelliteTracker.get(h.getLevel()).remove(h.getLevel(), occupied.id(), SatelliteTracker.Removal.DEORBITED);
        }
        h.succeedWhen(() -> {
            h.assertTrue(unreachable == SiloControllerEntity.SatLaunchResult.NO_ORBIT, "rejects unreachable orbit");
            h.assertTrue(full == SiloControllerEntity.SatLaunchResult.FULL, "rejects full network");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void pendingOrderSurvivesReload(GameTestHelper h) {
        var controller = satellite(h, 3, true);
        Orbit orbit = leo(h);
        UUID owner = UUID.randomUUID();
        var result = controller.launchSatellite(owner, orbit, "S2SV");
        int before = controller.countdown();
        controller.loadWithComponents(controller.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        controller.setSpeed((float) MissileConfig.minimumRpm);
        controller.validate();
        var state = controller.state();
        int after = controller.countdown();
        var saved = controller.saveWithoutMetadata(h.getLevel().registryAccess());
        boolean payload = saved.getString("SatelliteCode").equals("S2SV") && saved.getString("SatelliteKind").equals("COMMS")
                && saved.getCompound("SatelliteOrbit").equals(orbit.save()) && owner.equals(controller.operator());
        controller.disarm();
        h.succeedWhen(() -> {
            h.assertTrue(result == SiloControllerEntity.SatLaunchResult.OK, "order accepted");
            h.assertTrue(state == SiloControllerEntity.State.COUNTDOWN && before == after, "reload preserves countdown state");
            h.assertTrue(payload, "reload preserves orbit, code, kind and owner");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void savedOrderCountsDownIntoSatelliteFlight(GameTestHelper h) {
        var controller = satellite(h, 3, true);
        int old = MissileConfig.launchCountdownSeconds;
        SiloControllerEntity.SatLaunchResult result;
        try { MissileConfig.launchCountdownSeconds = 1; result = controller.launchSatellite((UUID) null, leo(h), "S2LA"); }
        finally { MissileConfig.launchCountdownSeconds = old; }
        CompoundTag saved = controller.saveWithoutMetadata(h.getLevel().registryAccess());
        controller.loadWithComponents(saved, h.getLevel().registryAccess());
        controller.setSpeed((float) MissileConfig.minimumRpm);
        controller.validate();
        var state = controller.state();
        int countdown = controller.countdown();
        BlockPos origin = controller.floor().above();
        h.succeedWhen(() -> {
            h.assertTrue(result == SiloControllerEntity.SatLaunchResult.OK, "accepts satellite order");
            h.assertTrue(state == SiloControllerEntity.State.COUNTDOWN && countdown == 20, "reload preserves countdown");
            h.assertTrue(controller.state() == SiloControllerEntity.State.LAUNCHED, "countdown launches");
            h.assertTrue(FlightTracker.get(h.getLevel()).flights().stream().anyMatch(f -> f.kind() == Flight.Kind.SATELLITE
                    && origin.equals(f.origin()) && f.satellite() != null && f.satellite().kind() == SatelliteKind.COMMS && f.satellite().code().equals("S2LA")), "flight carries code and satellite kind");
            // Its launch vehicle would climb on through later batches and be tracked by their radars.
            var level = h.getLevel();
            var flights = FlightTracker.get(level);
            for (Flight f : java.util.List.copyOf(flights.flights())) if (f.kind() == Flight.Kind.SATELLITE && origin.equals(f.origin())) flights.remove(level, f);
            var orbit = org.papiricoh.create_nuclearindustry.missile.orbit.SatelliteTracker.get(level);
            for (var sat : orbit.ofCode("S2LA")) orbit.remove(level, sat.id(), org.papiricoh.create_nuclearindustry.missile.orbit.SatelliteTracker.Removal.DEORBITED);
        });
    }
}

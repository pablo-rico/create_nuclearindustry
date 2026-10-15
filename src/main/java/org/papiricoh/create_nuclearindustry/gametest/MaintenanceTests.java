package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.commands.StructurePreset;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.maintenance.Earthquakes;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceConfig;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceKitItem;
import org.papiricoh.create_nuclearindustry.maintenance.PartWear;
import org.papiricoh.create_nuclearindustry.maintenance.ReactorMaintenance;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentLog;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentType;
import org.papiricoh.create_nuclearindustry.maintenance.physics.WearPart;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbinePortBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/**
 * Plant operation and maintenance: a reactor takes fuel and repairs only in a refuelling outage
 * (unless the config says otherwise), a jammed rod drive holds its channel until serviced, the
 * Maintenance Kit wants the machine stopped, a burst steam generator tube leaks the primary and
 * irradiates, failed turbine blades keep the turbine tripped, an earthquake scrams a reactor close
 * by, and every feature follows its switch. The log is the server's, so tests look for their own
 * entries by position, in a batch of their own.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class MaintenanceTests {
    private static final String BATCH = "maintenance";

    private static ItemStack freshFuel() {
        return UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM_REACTOR_FUEL.get()), 5f);
    }

    private static boolean logged(GameTestHelper helper, BlockPos absolute, IncidentType type) {
        return IncidentLog.get(helper.getLevel().getServer()).incidents().stream()
                .anyMatch(incident -> incident.type() == type && incident.pos().equals(absolute));
    }

    // ------------------------------------------------------------------ the refuelling outage

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aColdReactorWithEveryRodInIsInAnOutageAndTakesFuel(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.runAfterDelay(10, () -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the reactor should form");
            helper.assertTrue(reactor.inOutage(), "formed, cold and with every rod in, it is in an outage: rods "
                    + reactor.getControlRodInsertionRatio());
            ItemStack rest = reactor.getFuelItemHandler(ReactorFluidPortMode.INPUT).insertItem(0, freshFuel(), false);
            helper.assertTrue(rest.isEmpty(), "and the fuel port takes fresh fuel");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void withTheRodsOutNoFuelGoesInUnlessTheServerAllowsIt(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        helper.runAfterDelay(10, () -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the reactor should form");
            helper.assertFalse(reactor.inOutage(), "with its rods out it is not in an outage");
            ItemStack rest = reactor.getFuelItemHandler(ReactorFluidPortMode.INPUT).insertItem(0, freshFuel(), false);
            helper.assertTrue(rest.getCount() == 1, "so the fuel port refuses fresh fuel");
            boolean saved = MaintenanceConfig.requireOutage;
            try {
                MaintenanceConfig.requireOutage = false;
                helper.assertTrue(reactor.getFuelItemHandler(ReactorFluidPortMode.INPUT).insertItem(0, freshFuel(), false).isEmpty(),
                        "with maintenance.requireRefuellingOutage off it refuels at power as before");
            } finally {
                MaintenanceConfig.requireOutage = saved;
            }
            helper.assertTrue(MaintenanceKitItem.service(helper.getLevel(), helper.absolutePos(FissionTestBuilder.WALL_CASING), 0.5)
                    == MaintenanceKitItem.Outcome.NOT_STOPPED, "and the kit will not service it until it is stopped");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ rod drives

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aJammedRodHoldsItsChannelUntilServicedInAnOutage(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.runAfterDelay(10, () -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the reactor should form");
            reactor.forceStuckRod(0);
            ReactorMaintenance maintenance = reactor.getMaintenance();
            helper.assertTrue(maintenance.stuckChannel() == 0, "the first channel's drive jams");
            helper.assertTrue(maintenance.rodDrives().failed(), "and the drives read failed");
            double[] held = maintenance.applyStuck(new double[]{0.25});
            helper.assertTrue(held[0] == 1.0, "the channel keeps the insertion it jammed at, not the rods' new one: " + held[0]);
            helper.assertTrue(logged(helper, reactor.getBlockPos(), IncidentType.STUCK_ROD), "the incident log has it");
            helper.assertTrue(MaintenanceKitItem.service(helper.getLevel(), helper.absolutePos(FissionTestBuilder.WALL_CASING), 0.5)
                    == MaintenanceKitItem.Outcome.MENDED, "in its outage the kit services it");
            helper.assertTrue(maintenance.stuckChannel() < 0 && !maintenance.rodDrives().failed(), "and frees the rod");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aWornReliefValveSticksMoreOften(GameTestHelper helper) {
        ReactorMaintenance maintenance = new ReactorMaintenance();
        helper.assertTrue(maintenance.reliefStickScale() == 1.0, "a new valve sticks at the configured chance");
        for (int i = 0; i < 5; i++) {
            maintenance.onReliefLift();
        }
        helper.assertTrue(maintenance.reliefStickScale() > 3.0, "five lifts later it sticks far more often: " + maintenance.reliefStickScale());
        boolean saved = MaintenanceConfig.wear;
        try {
            MaintenanceConfig.wear = false;
            helper.assertTrue(maintenance.reliefStickScale() == 1.0, "with wear off it never matters");
        } finally {
            MaintenanceConfig.wear = saved;
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void partsWearOnlyWithWearOn(GameTestHelper helper) {
        PartWear blades = new PartWear(WearPart.TURBINE_BLADES);
        boolean saved = MaintenanceConfig.wear;
        try {
            MaintenanceConfig.wear = false;
            for (int i = 0; i < 1000; i++) {
                blades.tick(helper.getLevel(), 1.0);
            }
            helper.assertTrue(blades.health() == 1.0, "with wear off a part stays new");
        } finally {
            MaintenanceConfig.wear = saved;
        }
        blades.shock(3.0);
        helper.assertTrue(blades.health() < 1.0, "with wear on a transient takes its step");
        helper.assertTrue(blades.repair(1.0) && blades.health() == 1.0, "and a repair puts it right");
        helper.succeed();
    }

    // ------------------------------------------------------------------ steam generator and turbine

    private static SteamGeneratorControllerBlockEntity generatorAt(GameTestHelper helper, StructurePreset.Spawned spawned) {
        return spawned != null && helper.getLevel().getBlockEntity(spawned.controller()) instanceof SteamGeneratorControllerBlockEntity generator
                ? generator : null;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 120)
    public static void aBurstTubeLeaksThePrimaryAndIrradiates(GameTestHelper helper) {
        StructurePreset.Spawned[] spawned = new StructurePreset.Spawned[1];
        helper.runAfterDelay(1, () -> spawned[0] = StructurePreset.STEAM_GENERATOR.spawn(helper.getLevel(),
                helper.absolutePos(new BlockPos(1, 1, 1)), false));
        int[] start = new int[1];
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(spawned[0] != null && StructurePreset.formed(helper.getLevel(), spawned[0].controller()),
                    "the steam generator should form: " + (spawned[0] == null ? "" : StructurePreset.problem(helper.getLevel(), spawned[0].controller()).getString()));
            SteamGeneratorControllerBlockEntity generator = generatorAt(helper, spawned[0]);
            helper.assertTrue(generator != null, "the controller should be there");
            helper.assertTrue(generator.radiationStrength() == 0, "an intact one does not irradiate");
            generator.getHotTank().fill(new FluidStack(AllNuclearFluids.HOT_PRIMARY_COOLANT.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
            start[0] = generator.getHotTank().getFluidAmount();
            generator.rupture();
        });
        helper.runAfterDelay(40, () -> {
            SteamGeneratorControllerBlockEntity generator = generatorAt(helper, spawned[0]);
            helper.assertTrue(generator != null && generator.tubeWear().failed(), "the tube stays burst");
            helper.assertTrue(generator.getHotTank().getFluidAmount() <= start[0] - 20 * SteamGeneratorControllerBlockEntity.RUPTURE_LEAK / 2,
                    "the primary leaks away: " + start[0] + " -> " + generator.getHotTank().getFluidAmount());
            helper.assertTrue(generator.radiationStrength() > 0, "and the secondary side irradiates");
            helper.assertTrue(logged(helper, generator.getBlockPos(), IncidentType.SG_TUBE_RUPTURE), "the incident log has the rupture");
            helper.assertTrue(generator.repair(1.0) && !generator.tubeWear().failed(), "stopped, it can be repaired");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 120)
    public static void failedBladesKeepTheTurbineTripped(GameTestHelper helper) {
        BlockPos controller = new BlockPos(6, 6, 1);
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, controller, 3, 3, 6);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(controller);
        for (int t = 5; t < 115; t += 5) {
            helper.runAfterDelay(t, () -> {
                TurbinePortBlockEntity input = helper.getBlockEntity(s.at(1, s.hu, 0));
                input.getFluidHandler(Direction.UP).fill(new FluidStack(AllNuclearFluids.STEAM.get(), 4_000), IFluidHandler.FluidAction.EXECUTE);
                turbine.getCondensateTank().drain(4_000, IFluidHandler.FluidAction.EXECUTE);
            });
        }
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(turbine.getCurrentUse() > 0, "the turbine runs: " + turbine.getCurrentUse());
            turbine.trip();
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(turbine.getCurrentUse() == 0, "tripped on vibration, it expands no steam");
            helper.assertTrue(logged(helper, turbine.getBlockPos(), IncidentType.TURBINE_TRIP), "the incident log has the trip");
            helper.assertTrue(MaintenanceKitItem.service(helper.getLevel(), turbine.getBlockPos(), 1.0) == MaintenanceKitItem.Outcome.MENDED,
                    "the kit services its blades");
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(turbine.getCurrentUse() > 0, "and it runs again");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ earthquakes and the log

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void anEarthquakeScramsAReactorCloseBy(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.runAfterDelay(10, () -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            helper.assertTrue(reactor != null && reactor.isFormed() && !reactor.isScramRequested(), "a formed reactor, not scrammed");
            Earthquakes.strike(helper.getLevel(), reactor.getBlockPos().east(4), 5.5);
            helper.assertTrue(reactor.isScramRequested(), "a magnitude 5.5 next door trips its seismic SCRAM");
            helper.assertTrue(logged(helper, reactor.getBlockPos().east(4), IncidentType.EARTHQUAKE), "the log has the earthquake");
            helper.assertTrue(reactor.getMaintenance().rodDrives().health() < 1.0, "and the shaking wore its rod drives");
        });
        helper.runAfterDelay(15, () -> {
            ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
            helper.assertTrue(logged(helper, reactor.getBlockPos(), IncidentType.SEISMIC_SCRAM), "and the seismic SCRAM");
            reactor.setManualScram(false);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 40)
    public static void theDrillCommandSoundsTheAlarmAndIsLogged(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(6, 2, 6));
        helper.runAfterDelay(1, () -> {
            CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel())
                    .withPosition(Vec3.atCenterOf(at)).withSuppressedOutput();
            helper.getLevel().getServer().getCommands().performPrefixedCommand(source,
                    "nuclear incident drill " + at.getX() + " " + at.getY() + " " + at.getZ());
            helper.assertTrue(logged(helper, at, IncidentType.DRILL), "the drill is in the log");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theIncidentLogFollowsItsSwitches(GameTestHelper helper) {
        BlockPos off = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos on = helper.absolutePos(new BlockPos(3, 2, 2));
        boolean saved = MaintenanceConfig.incidentLog;
        boolean master = MaintenanceConfig.enabled;
        try {
            MaintenanceConfig.incidentLog = false;
            IncidentLog.record(helper.getLevel(), off, IncidentType.DRILL);
            MaintenanceConfig.incidentLog = true;
            MaintenanceConfig.enabled = false;
            IncidentLog.record(helper.getLevel(), off, IncidentType.DRILL);
        } finally {
            MaintenanceConfig.incidentLog = saved;
            MaintenanceConfig.enabled = master;
        }
        helper.assertFalse(logged(helper, off, IncidentType.DRILL), "nothing is logged with the log or the master switch off");
        IncidentLog.record(helper.getLevel(), on, IncidentType.SG_TUBE_RUPTURE);
        helper.assertTrue(IncidentLog.get(helper.getLevel().getServer()).incidents().stream()
                .anyMatch(incident -> incident.pos().equals(on) && incident.ines() == 2), "a tube rupture is logged as INES 2");
        helper.succeed();
    }

    /** The Ponder scene's reactor (one block above the test origin, as every template) forms and sits in an outage. */
    @GameTest(template = "gametest/maintenance_ponder", batch = BATCH, timeoutTicks = 60)
    public static void thePonderReactorFormsInAnOutage(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            ReactorBlockEntity reactor = AcceleratorTestBuilder.entity(helper, new BlockPos(3, 6, 3), ReactorBlockEntity.class);
            helper.assertTrue(reactor != null && reactor.isFormed(), "the scene's reactor forms: "
                    + StructurePreset.problem(helper.getLevel(), helper.absolutePos(new BlockPos(3, 6, 3))).getString());
            helper.assertTrue(reactor.inOutage(), "cold with its rods in, it is in an outage");
            helper.assertTrue(reactor.refuelAllowed(), "so its fuel port takes fuel");
        });
    }
}

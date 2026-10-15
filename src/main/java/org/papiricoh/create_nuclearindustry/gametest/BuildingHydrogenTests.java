package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.maintenance.incident.Incident;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentLog;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentType;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.ReactorAccidents;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.ReactorBuilding;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;

/**
 * Hydrogen from a reactor without a containment: in a closed room of ordinary blocks it gathers
 * until it burns and blows the roof off; in the open it is lost; a sealed containment still takes
 * it first. The building's atmosphere survives a save and load.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class BuildingHydrogenTests {
    private static final String BATCH = "contamination_building_hydrogen";
    /** A stone hall in the plant arena, 15x15x15 blocks of air, with the reactor on its floor. */
    private static final BlockPos HALL_MIN = new BlockPos(0, 1, 0);
    private static final BlockPos HALL_MAX = new BlockPos(16, 17, 16);
    private static final BlockPos HALL_REACTOR = new BlockPos(6, 2, 6);
    private static final BlockPos HALL_ROOF_CENTRE = new BlockPos(8, 17, 8);
    /** A smaller room in the small arena. */
    private static final BlockPos ROOM_MIN = new BlockPos(0, 1, 0);
    private static final BlockPos ROOM_MAX = new BlockPos(12, 10, 12);
    private static final BlockPos ROOM_REACTOR = new BlockPos(4, 2, 4);
    private static final BlockPos ROOM_MONITOR = new BlockPos(2, 2, 2);

    @GameTest(template = NuclearGameTests.PLANT_ARENA, batch = BATCH, timeoutTicks = 300)
    public static void hydrogenInAStoneHallBlowsTheRoof(GameTestHelper helper) {
        int events = ContainmentTestBuilder.falloutEvents(helper);
        ContainmentTestBuilder.room(helper, HALL_MIN, HALL_MAX, Blocks.STONE);
        FissionTestBuilder.build(helper, HALL_REACTOR, true);
        ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(HALL_REACTOR));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(reactor.isFormed(), "the reactor formed");
            ReactorAccidents accidents = reactor.getAccidents();
            accidents.releaseHydrogen(helper.getLevel(), 1.0, ContainmentModel.WALL_TEMPERATURE);
            ReactorBuilding building = accidents.building();
            helper.assertTrue(building.sealed() && building.shape() != null, "the first hydrogen measures the stone hall around the reactor");
            helper.assertTrue(building.shape().weakestRating() <= 1.5, "stone holds no more than an ordinary wall, "
                    + building.shape().weakestRating());
            accidents.releaseHydrogen(helper.getLevel(), building.model().totalMoles() * 0.1, ContainmentModel.WALL_TEMPERATURE);
            helper.assertTrue(reactor.getBuildingHydrogen() > ContainmentModel.CERTAIN_IGNITION,
                    "the hall holds it: " + reactor.getBuildingHydrogen());
        });
        helper.succeedWhen(() -> {
            ReactorBuilding building = reactor.getAccidents().building();
            helper.assertTrue(building.explosions() == 1, "above 8% it burns for certain, explosions " + building.explosions());
            helper.assertTrue(!building.sealed() && reactor.getBuildingHydrogen() == 0, "the building is gone");
            helper.assertTrue(helper.getBlockState(HALL_ROOF_CENTRE).isAir(), "the roof over the middle of the hall is torn open");
            helper.assertTrue(helper.getBlockState(HALL_ROOF_CENTRE.east(2)).isAir(), "and around it");
            helper.assertTrue(!helper.getBlockState(HALL_MIN.above()).isAir(), "the walls stand");
            BlockPos roof = helper.absolutePos(HALL_ROOF_CENTRE);
            boolean found = false;
            for (Incident incident : IncidentLog.get(helper.getLevel().getServer()).incidents()) {
                if (incident.type() == IncidentType.HYDROGEN_EXPLOSION && incident.pos().closerThan(roof, 3)) {
                    found = incident.ines() == 3;
                    break;
                }
            }
            helper.assertTrue(found, "logged as a hydrogen explosion, INES 3 with intact cladding");
            ContainmentTestBuilder.clear(helper, HALL_MIN, HALL_MAX);
            helper.killAllEntitiesOfClass(ItemEntity.class);
            ThreatRegistry.clear(helper.getLevel());
            ContainmentTestBuilder.cleanFallout(helper, events, 2);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void inTheOpenTheHydrogenIsLost(GameTestHelper helper) {
        FissionTestBuilder.build(helper, ROOM_REACTOR, true);
        ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(ROOM_REACTOR));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(reactor.isFormed(), "the reactor formed");
            reactor.getAccidents().releaseHydrogen(helper.getLevel(), 5_000.0, ContainmentModel.WALL_TEMPERATURE);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!reactor.getAccidents().building().sealed(), "no roof, no building");
            helper.assertTrue(reactor.getBuildingHydrogen() == 0, "the hydrogen went up into the sky: " + reactor.getBuildingHydrogen());
            helper.assertTrue(reactor.getAccidents().building().explosions() == 0, "and nothing burnt");
            ContainmentTestBuilder.clearArena(helper);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aSealedContainmentTakesItFirst(GameTestHelper helper) {
        ContainmentTestBuilder.room(helper, ROOM_MIN, ROOM_MAX, PwrRegistry.CONTAINMENT_CONCRETE.get());
        FissionTestBuilder.build(helper, ROOM_REACTOR, true);
        helper.setBlock(ROOM_MONITOR, PwrRegistry.CONTAINMENT_MONITOR.get());
        ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(ROOM_REACTOR));
        boolean[] injected = new boolean[1];
        helper.onEachTick(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ROOM_MONITOR);
            if (!injected[0] && monitor != null && monitor.sealed() && reactor.isFormed()
                    && reactor.getAccidents().containment(helper.getLevel()) != null) {
                injected[0] = true;
                // 3%: below the flammability limit, so it stays where it went
                reactor.getAccidents().releaseHydrogen(helper.getLevel(), ContainmentTestBuilder.airMoles(monitor) * 0.03,
                        ContainmentModel.WALL_TEMPERATURE);
            }
        });
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ROOM_MONITOR);
            helper.assertTrue(injected[0] && monitor != null, "the reactor found its containment");
            helper.assertTrue(monitor.getHydrogen() > 0.02, "the containment took the hydrogen: " + monitor.getHydrogen());
            helper.assertTrue(!reactor.getAccidents().building().sealed() && reactor.getBuildingHydrogen() == 0,
                    "the building never measured it: " + reactor.getBuildingHydrogen());
            ContainmentTestBuilder.clear(helper, ROOM_MIN, ROOM_MAX);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theBuildingSurvivesSaveAndLoad(GameTestHelper helper) {
        ContainmentTestBuilder.room(helper, ROOM_MIN, ROOM_MAX, Blocks.STONE);
        FissionTestBuilder.build(helper, ROOM_REACTOR, true);
        ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(ROOM_REACTOR));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(reactor.isFormed(), "the reactor formed");
            ReactorAccidents accidents = reactor.getAccidents();
            accidents.releaseHydrogen(helper.getLevel(), 1.0, ContainmentModel.WALL_TEMPERATURE);
            helper.assertTrue(accidents.building().sealed(), "the stone room holds hydrogen");
            // 3%: below the flammability limit
            accidents.releaseHydrogen(helper.getLevel(), accidents.building().model().totalMoles() * 0.03, ContainmentModel.WALL_TEMPERATURE);
            double hydrogen = accidents.buildingHydrogen();
            CompoundTag saved = accidents.save();
            helper.assertTrue(saved.getCompound("Building").getBoolean("Sealed"), "saved as sealed");

            ReactorAccidents loaded = new ReactorAccidents();
            loaded.load(saved);
            helper.assertTrue(loaded.building().sealed(), "loaded as sealed, to be measured again");
            helper.assertTrue(Math.abs(loaded.buildingHydrogen() - hydrogen) < 1e-6,
                    "the same hydrogen: " + hydrogen + " -> " + loaded.buildingHydrogen());
            helper.assertTrue(accidents.building().bounds().equals(loaded.building().bounds()), "and the same room");

            // A building with no hydrogen left is not worth restoring.
            CompoundTag empty = new ReactorAccidents().save();
            loaded.load(empty);
            helper.assertTrue(!loaded.building().sealed() && loaded.buildingHydrogen() == 0, "an empty save restores no building");
        });
        helper.runAfterDelay(40, () -> {
            // The live reactor keeps it below the flammability limit; the walls leak a little of it.
            helper.assertTrue(reactor.getAccidents().building().sealed(), "still sealed");
            helper.assertTrue(reactor.getBuildingHydrogen() > 0.025 && reactor.getBuildingHydrogen() < 0.031,
                    "below the flammability limit it neither burns nor vanishes: " + reactor.getBuildingHydrogen());
            ContainmentTestBuilder.clear(helper, ROOM_MIN, ROOM_MAX);
            helper.succeed();
        });
    }
}

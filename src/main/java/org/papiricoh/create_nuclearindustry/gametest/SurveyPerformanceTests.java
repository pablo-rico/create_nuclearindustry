package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterCertification.Status;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterFiltrationUnitBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelPoolSurvey;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelRackBlock;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelRackBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryInspectorBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationMelterBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Surveys that are not repeated when nothing changed: racks in one pool share a single flood
 * fill, a sealed containment or shelter whose blocks are untouched is not measured again (an open
 * one backs off), the containment monitor only updates clients when what they show changes, the
 * repository inspector surveys every ten seconds and skips sections without packages, and the
 * vitrification melter looks its recipe up only when its contents or heat change.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SurveyPerformanceTests {

    // ------------------------------------------------------------------ spent fuel pools

    /** Four racks on the floor of a 2x2 pool, three blocks deep: one flood fill serves all of them. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_spentfuel_pool")
    public static void racksInOnePoolShareOneFloodFill(GameTestHelper helper) {
        BlockPos min = new BlockPos(5, 2, 5);
        int depth = 3;
        for (int x = -1; x <= 2; x++) {
            for (int z = -1; z <= 2; z++) {
                helper.setBlock(min.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= depth; y++) {
                    boolean wall = x == -1 || x == 2 || z == -1 || z == 2;
                    if (wall) {
                        helper.setBlock(min.offset(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        List<BlockPos> racks = new ArrayList<>();
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                BlockPos rack = min.offset(x, 0, z);
                racks.add(rack);
                helper.setBlock(rack, SpentFuelRegistry.SPENT_FUEL_RACK.get().defaultBlockState()
                        .setValue(SpentFuelRackBlock.WATERLOGGED, true));
                for (int y = 1; y <= depth; y++) {
                    helper.setBlock(rack.above(y), Blocks.WATER);
                }
            }
        }
        helper.runAfterDelay(1, () -> {
            int before = SpentFuelPoolSurvey.fills();
            List<SpentFuelRackBlockEntity> entities = new ArrayList<>();
            for (BlockPos pos : racks) {
                if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof SpentFuelRackBlockEntity rack) {
                    entities.add(rack);
                }
            }
            helper.assertTrue(entities.size() == 4, "four racks, got " + entities.size());
            entities.get(0).scanPool();
            for (int i = 1; i < entities.size(); i++) {
                entities.get(i).lookUpPool();
            }
            int fills = SpentFuelPoolSurvey.fills() - before;
            helper.assertTrue(fills == 1, "four racks in one pool should cost one flood fill, cost " + fills);
            for (SpentFuelRackBlockEntity rack : entities) {
                SpentFuelRackBlockEntity.Pool pool = rack.getPool();
                helper.assertTrue(pool.sealed() && pool.volume() == 4 * (depth + 1) && pool.racks() == 4,
                        "every rack sees the whole pool and its four racks, got " + pool);
            }
            entities.get(0).scanPool();
            helper.assertTrue(SpentFuelPoolSurvey.fills() - before == 2, "an explicit scan still measures the water again");
            for (BlockPos pos : racks) {
                helper.setBlock(pos, Blocks.AIR);
            }
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ containment

    /**
     * Once sealed, the routine re-measurement is skipped while the room's blocks are untouched; a
     * torch placed inside (no wall, so no immediate survey) makes the next routine one run.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_containment", timeoutTicks = 300)
    public static void sealedContainmentIsNotMeasuredAgainWithoutChanges(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        int[] surveys = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "the room seals");
                })
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null, "the monitor is there");
                    surveys[0] = monitor.getSurveysStarted();
                    monitor.expireRoutineTimer();
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.getSurveysStarted() == surveys[0] && !monitor.isSurveying(),
                            "an untouched sealed room is not measured again, surveys " + surveys[0] + " -> "
                                    + (monitor == null ? -1 : monitor.getSurveysStarted()));
                    helper.setBlock(new BlockPos(4, 2, 4), Blocks.TORCH);
                    helper.assertTrue(monitor.getSurveysStarted() == surveys[0], "a torch is no wall: nothing is measured at once");
                    monitor.expireRoutineTimer();
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.getSurveysStarted() == surveys[0] + 1,
                            "after a change inside, the routine measurement runs");
                })
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "and finds the room still sealed");
                })
                .thenSucceed();
    }

    /** A sealed room at rest sends its clients an update every 100 ticks, not every 10. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_containment", timeoutTicks = 400)
    public static void idleContainmentUpdatesClientsRarely(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        int[] syncs = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "the room seals");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null, "the monitor is there");
                    syncs[0] = monitor.getSyncsSent();
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null, "the monitor is there");
                    int sent = monitor.getSyncsSent() - syncs[0];
                    helper.assertTrue(sent >= 1 && sent <= 3,
                            "200 quiet ticks should send 2 forced updates (not 20), sent " + sent);
                })
                .thenSucceed();
    }

    /**
     * In open air the monitor keeps looking for walls, less and less often: 600, 1200, 2400 ticks.
     * Each step waits for the measurement the expired timer starts: a sequence runs its next steps
     * in the same tick, before the monitor has ticked.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_containment_open", timeoutTicks = 400)
    public static void openContainmentMonitorBacksOff(GameTestHelper helper) {
        helper.setBlock(ContainmentTestBuilder.MONITOR.below(), Blocks.STONE);
        helper.setBlock(ContainmentTestBuilder.MONITOR, PwrRegistry.CONTAINMENT_MONITOR.get());
        int[] intervals = new int[3];
        int[] surveys = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.getSurveysStarted() >= 1 && !monitor.isSurveying() && !monitor.sealed(),
                            "the first measurement finds open air");
                })
                .thenExecute(() -> expireOpen(helper, surveys))
                .thenWaitUntil(() -> openAndIdleAfter(helper, surveys))
                .thenExecute(() -> {
                    intervals[0] = openInterval(helper);
                    expireOpen(helper, surveys);
                })
                .thenWaitUntil(() -> openAndIdleAfter(helper, surveys))
                .thenExecute(() -> {
                    intervals[1] = openInterval(helper);
                    expireOpen(helper, surveys);
                })
                .thenWaitUntil(() -> openAndIdleAfter(helper, surveys))
                .thenExecute(() -> {
                    intervals[2] = openInterval(helper);
                    int base = ContainmentMonitorBlockEntity.RESURVEY_INTERVAL;
                    helper.assertTrue(intervals[0] == 2 * base && intervals[1] == 4 * base
                                    && intervals[2] == ContainmentMonitorBlockEntity.MAX_UNSEALED_INTERVAL,
                            "the wait doubles up to the cap, got " + intervals[0] + ", " + intervals[1] + ", " + intervals[2]);
                })
                .thenSucceed();
    }

    /** Expires the routine timer, noting how many measurements had started before. */
    private static void expireOpen(GameTestHelper helper, int[] surveys) {
        ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
        helper.assertTrue(monitor != null, "the monitor is there");
        surveys[0] = monitor.getSurveysStarted();
        monitor.expireRoutineTimer();
    }

    /** The routine measurement the expired timer asked for has run and found open air. */
    private static void openAndIdleAfter(GameTestHelper helper, int[] surveys) {
        ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
        helper.assertTrue(monitor != null && monitor.getSurveysStarted() > surveys[0] && !monitor.isSurveying() && !monitor.sealed(),
                "measured again, open and done measuring");
    }

    private static int openInterval(GameTestHelper helper) {
        ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
        helper.assertTrue(monitor != null, "the monitor is there");
        return monitor.getUnsealedInterval();
    }

    // ------------------------------------------------------------------ shelter

    private static final BlockPos UNIT = new BlockPos(6, 2, 6);

    private static ShelterFiltrationUnitBlockEntity unit(GameTestHelper helper) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(UNIT)) instanceof ShelterFiltrationUnitBlockEntity unit ? unit : null;
    }

    private static void placeUnit(GameTestHelper helper) {
        helper.setBlock(UNIT, CivilDefenseRegistry.SHELTER_FILTRATION_UNIT.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
    }

    /** A certified room is not surveyed again until one of its blocks changes. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_shelter", timeoutTicks = 200)
    public static void closedShelterIsNotSurveyedAgainWithoutChanges(GameTestHelper helper) {
        for (int x = 3; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 3; z <= 9; z++) {
                    boolean shell = x == 3 || x == 9 || y == 1 || y == 5 || z == 3 || z == 9;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE_BRICKS : Blocks.AIR);
                }
            }
        }
        placeUnit(helper);
        int[] surveys = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ShelterFiltrationUnitBlockEntity unit = unit(helper);
                    helper.assertTrue(unit != null && unit.getStatus() == Status.CERTIFIED, "the room is certified");
                })
                .thenExecute(() -> {
                    ShelterFiltrationUnitBlockEntity unit = unit(helper);
                    helper.assertTrue(unit != null, "the unit is there");
                    surveys[0] = unit.getSurveysStarted();
                    unit.expireSurveyTimer();
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    ShelterFiltrationUnitBlockEntity unit = unit(helper);
                    helper.assertTrue(unit != null && unit.getSurveysStarted() == surveys[0],
                            "an untouched certified room is not surveyed again");
                    helper.setBlock(new BlockPos(4, 2, 4), Blocks.TORCH);
                    unit.expireSurveyTimer();
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    ShelterFiltrationUnitBlockEntity unit = unit(helper);
                    helper.assertTrue(unit != null && unit.getSurveysStarted() == surveys[0] + 1,
                            "after a change inside it is surveyed again");
                    helper.assertTrue(unit.getStatus() == Status.CERTIFIED, "and is still certified");
                })
                .thenSucceed();
    }

    /** A unit in the open surveys less and less often while nothing changes around it. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_shelter_open", timeoutTicks = 200)
    public static void openShelterBacksOff(GameTestHelper helper) {
        helper.setBlock(UNIT.below(), Blocks.STONE);
        placeUnit(helper);
        int[] intervals = new int[3];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ShelterFiltrationUnitBlockEntity unit = unit(helper);
                    helper.assertTrue(unit != null && unit.getSurveysStarted() >= 1
                                    && (unit.getStatus() == Status.OPEN || unit.getStatus() == Status.TOO_BIG),
                            "the first survey finds no room, got " + (unit == null ? null : unit.getStatus()));
                })
                .thenExecute(() -> expireShelter(helper))
                .thenIdle(12)
                .thenExecute(() -> {
                    intervals[0] = shelterInterval(helper);
                    expireShelter(helper);
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    intervals[1] = shelterInterval(helper);
                    expireShelter(helper);
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    intervals[2] = shelterInterval(helper);
                    int rescan = org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig.shelterRescanTicks;
                    helper.assertTrue(intervals[0] == 2 * rescan && intervals[1] == 4 * rescan && intervals[2] == 4 * rescan,
                            "the wait doubles up to four rescan intervals, got " + intervals[0] + ", " + intervals[1] + ", " + intervals[2]);
                })
                .thenSucceed();
    }

    private static void expireShelter(GameTestHelper helper) {
        ShelterFiltrationUnitBlockEntity unit = unit(helper);
        helper.assertTrue(unit != null, "the unit is there");
        unit.expireSurveyTimer();
    }

    private static int shelterInterval(GameTestHelper helper) {
        ShelterFiltrationUnitBlockEntity unit = unit(helper);
        helper.assertTrue(unit != null, "the unit is there");
        return unit.getOpenInterval();
    }

    // ------------------------------------------------------------------ repository inspector

    /** One survey when placed, none for the next 150 ticks, and sections without packages are not read. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_perf_repository", timeoutTicks = 300)
    public static void repositoryInspectorSurveysSeldomAndSkipsEmptySections(GameTestHelper helper) {
        BlockPos inspectorPos = new BlockPos(6, 2, 6);
        BlockPos drumPos = new BlockPos(8, 2, 6);
        helper.setBlock(inspectorPos, WasteRegistry.REPOSITORY_INSPECTOR.get().defaultBlockState());
        helper.runAfterDelay(2, () -> {
            RepositoryInspectorBlockEntity inspector = inspector(helper, inspectorPos);
            helper.assertTrue(inspector != null && inspector.getScans() == 1, "it surveys as soon as it is placed");
            helper.assertTrue(inspector.getBlocksRead() == 0,
                    "no section around holds a package, so no block is read, read " + inspector.getBlocksRead());
        });
        helper.runAfterDelay(150, () -> {
            RepositoryInspectorBlockEntity inspector = inspector(helper, inspectorPos);
            helper.assertTrue(inspector != null && inspector.getScans() == 1,
                    "no other survey within " + RepositoryInspectorBlockEntity.SCAN_INTERVAL + " ticks, got "
                            + (inspector == null ? -1 : inspector.getScans()));
            helper.setBlock(drumPos, WasteRegistry.CEMENTED_WASTE_DRUM.get().defaultBlockState());
            inspector.scan();
            helper.assertTrue(inspector.packages() == 1, "the drum is found, got " + inspector.packages());
            int box = 17 * 17 * 17;
            helper.assertTrue(inspector.getBlocksRead() > 0 && inspector.getBlocksRead() < box,
                    "only the sections holding a package are read, " + inspector.getBlocksRead() + " of " + box);
            helper.setBlock(drumPos, Blocks.AIR);
            helper.succeed();
        });
    }

    private static RepositoryInspectorBlockEntity inspector(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof RepositoryInspectorBlockEntity inspector ? inspector : null;
    }

    // ------------------------------------------------------------------ vitrification melter

    private static final BlockPos BURNER = new BlockPos(6, 1, 6);
    private static final BlockPos MELTER = BURNER.above();

    /** While melting with steady contents, the recipe is not looked up again and clients are not flooded. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_perf_melter", timeoutTicks = 200)
    public static void melterLooksItsRecipeUpOnlyWhenSomethingChanges(GameTestHelper helper) {
        Block burner = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "blaze_burner"));
        helper.setBlock(BURNER, burner.defaultBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
        helper.setBlock(MELTER, WasteRegistry.VITRIFICATION_MELTER.get().defaultBlockState());
        BlockPos burnerAt = helper.absolutePos(BURNER);
        BlockState burnerState = helper.getLevel().getBlockState(burnerAt);
        BlazeBurnerBlock.tryInsert(burnerState, helper.getLevel(), burnerAt, new ItemStack(Items.COAL), false, false, false);
        int[] start = new int[2];
        helper.runAfterDelay(2, () -> {
            VitrificationMelterBlockEntity melter = melter(helper);
            helper.assertTrue(melter != null, "the melter is there");
            IItemHandler handler = melter.getAutomationHandler();
            ItemStack[] batch = {new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get(), 4), new ItemStack(Items.SAND, 2),
                    new ItemStack(AllNuclearItems.BORAX_SALT.get(), 1)};
            for (ItemStack stack : batch) {
                ItemStack rest = stack;
                for (int slot = 0; slot < VitrificationMelterBlockEntity.INPUT_SLOTS && !rest.isEmpty(); slot++) {
                    rest = handler.insertItem(slot, rest, false);
                }
                helper.assertTrue(rest.isEmpty(), "the melter should take " + stack);
            }
        });
        helper.runAfterDelay(5, () -> {
            VitrificationMelterBlockEntity melter = melter(helper);
            helper.assertTrue(melter != null && melter.isMelting(), "it melts on a lit burner");
            start[0] = melter.getRecipeLookups();
            start[1] = melter.getSyncsSent();
        });
        helper.runAfterDelay(45, () -> {
            VitrificationMelterBlockEntity melter = melter(helper);
            helper.assertTrue(melter != null && melter.isMelting(), "still melting");
            int lookups = melter.getRecipeLookups() - start[0];
            int syncs = melter.getSyncsSent() - start[1];
            helper.assertTrue(lookups <= 1, "40 ticks of the same melt need no recipe lookup but for a heat change (was one a tick), did " + lookups);
            helper.assertTrue(syncs <= 1, "nor updates to clients (was one every 10 ticks), sent " + syncs);
            for (int slot = 0; slot <= VitrificationMelterBlockEntity.OUTPUT_SLOT; slot++) {
                melter.getInventory().setStackInSlot(slot, ItemStack.EMPTY);
            }
            helper.setBlock(MELTER, Blocks.AIR);
            helper.succeed();
        });
    }

    private static VitrificationMelterBlockEntity melter(GameTestHelper helper) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(MELTER)) instanceof VitrificationMelterBlockEntity melter ? melter : null;
    }
}

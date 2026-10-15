package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveManager;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.ShipBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.CoriumBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.integration.sable.SableCompat;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipContamination;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;

/**
 * Create Aeronautics ships, through Sable: only with Sable installed. Sable classes stay in
 * {@link AeronauticsShips}, so this class loads without it.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AeronauticsTests {
    private static final String BATCH = "aeronautics";

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void shipBlocksMapBackToTheWorld(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        Vec3 spawn = helper.absoluteVec(new Vec3(6.5, 8.5, 6.5));
        AeronauticsShips.Spawned ship = AeronauticsShips.single(helper, spawn, Blocks.STONE.defaultBlockState());
        helper.assertTrue(ship != null, "no ship spawned");
        BlockPos plot = ship.firstBlock();
        helper.assertTrue(plot != null, "the ship has no block");
        helper.assertTrue(ShipSpace.onShip(helper.getLevel(), plot), "its block is not on a ship");
        helper.assertFalse(ShipSpace.onShip(helper.getLevel(), BlockPos.containing(spawn)), "the world is not a ship");
        Vec3 world = ShipSpace.toWorld(helper.getLevel(), plot);
        helper.assertTrue(world.distanceTo(spawn) < 1.5, "the block maps to " + world + ", not near " + spawn);
        helper.runAfterDelay(5, () -> {
            var found = ShipSpace.shipsIn(helper.getLevel(), new net.minecraft.world.phys.AABB(spawn, spawn).inflate(2));
            ShipSpace.Ship onShip = ShipSpace.shipAt(helper.getLevel(), plot);
            String box = onShip == null ? "none" : onShip.worldBounds().toString();
            ship.remove();
            helper.assertTrue(!found.isEmpty(), "the ship is not found around its world position; its box is " + box);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void assemblingKeepsAntimatterContained(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        BlockPos trapPos = AntimatterTestBuilder.MIDDLE;
        AntimatterTestBuilder.fedTrap(helper, trapPos);
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, trapPos);
            helper.assertTrue(trap != null && trap.fill(10) == 10, "the trap takes 10 ng");
            AeronauticsShips.Spawned ship = AeronauticsShips.assemble(helper, trapPos.below(), trapPos);
            helper.assertTrue(ship != null, "no ship assembled");
            helper.runAfterDelay(20, () -> {
                PenningTrapBlockEntity carried = ship.find(PenningTrapBlockEntity.class);
                int blasts = AntimatterTestBuilder.blastsNear(helper, trapPos, 64);
                ship.remove();
                helper.assertTrue(blasts == 0, "assembling set off " + blasts + " annihilations");
                helper.assertTrue(carried != null, "the trap is not on the ship");
                helper.assertTrue(carried.stored() == 10, "the trap on the ship holds " + carried.stored() + " ng, not 10");
                helper.assertTrue(carried.state() == TrapState.STABLE, "and keeps its field: " + carried.state());
                helper.succeed();
            });
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 300)
    public static void aGroundDeskStillReadsAReactorTakenAboard(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        BlockPos desk = helper.absolutePos(new BlockPos(11, 1, 11));
        GlobalPos link = GlobalPos.of(helper.getLevel().dimension(), reactor.getBlockPos());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(reactor.isFormed(), "the reactor should form on the ground");
            boolean scrammed = reactor.isScramRequested();
            BlockPos far = FissionTestBuilder.ORIGIN.offset(FissionTestBuilder.SIZE - 1, FissionTestBuilder.SIZE - 1, FissionTestBuilder.SIZE - 1);
            AeronauticsShips.Spawned ship = AeronauticsShips.assemble(helper, FissionTestBuilder.ORIGIN, far);
            helper.assertTrue(ship != null, "no ship assembled");
            helper.runAfterDelay(40, () -> {
                ReactorBlockEntity aboard = ship.find(ReactorBlockEntity.class);
                LinkedTarget.Resolved resolved = LinkedTarget.resolve(helper.getLevel(), desk, link);
                boolean formed = aboard != null && aboard.isFormed();
                boolean scramAfter = aboard != null && aboard.isScramRequested();
                ship.remove();
                helper.assertTrue(aboard != null, "the reactor is not on the ship");
                helper.assertTrue(formed, "the reactor re-forms aboard");
                helper.assertTrue(scramAfter == scrammed, "taking it aboard changed its scram from " + scrammed + " to " + scramAfter);
                helper.assertTrue(resolved instanceof LinkedTarget.Reactor found && found.reactor() == aboard,
                        "a desk linked on the ground reads " + resolved + ", not the reactor aboard");
                helper.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ blasts

    /** A small detonation: severe to 7 blocks, wrecking to 9.5, the flash to 24. */
    private static DetonationProfile smallBlast() {
        return new DetonationProfile(10, 2500, 12, 1, 0, false, 2, 6, 2, 8, 1, 10, 5, 7, 9.5, 24, 8, 0, 0);
    }

    private static final BlockPos BLAST = new BlockPos(20, 4, 20);

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = BATCH + "_blast", timeoutTicks = 200)
    public static void aDetonationTearsNearShipsAndCharsFarOnes(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        for (int x = 22; x <= 23; x++) {
            for (int z = 20; z <= 21; z++) {
                helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(30, 5, 20), Blocks.OAK_LOG);
        AeronauticsShips.Spawned near = AeronauticsShips.assemble(helper, new BlockPos(22, 5, 20), new BlockPos(23, 5, 21));
        AeronauticsShips.Spawned far = AeronauticsShips.assemble(helper, new BlockPos(30, 5, 20), new BlockPos(30, 5, 20));
        helper.assertTrue(near != null && far != null, "no ships assembled");
        helper.runAfterDelay(3, () -> ShipBlastManager.start(helper.getLevel(), helper.absoluteVec(Vec3.atCenterOf(BLAST)), smallBlast(), null));
        helper.succeedWhen(() -> {
            helper.assertTrue(ShipBlastManager.pending() == 0, "the blast is still working through the ships");
            helper.assertTrue(near.isRemoved() || near.firstBlock() == null, "the ship in the severe zone still stands");
            BlockPos log = far.firstBlock();
            helper.assertTrue(log != null && helper.getLevel().getBlockState(log).is(ExplosiveRegistry.CHARRED_LOG.get()),
                    "the log ship in the flash is not charred: " + (log == null ? "gone" : helper.getLevel().getBlockState(log)));
            far.remove();
        });
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = BATCH + "_wave", timeoutTicks = 100)
    public static void theBlastWaveThrowsAShipAway(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(24, 4, 20, 25, 5, 21)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        AeronauticsShips.Spawned ship = AeronauticsShips.assemble(helper, new BlockPos(24, 4, 20), new BlockPos(25, 5, 21));
        helper.assertTrue(ship != null, "no ship assembled");
        helper.runAfterDelay(3, () -> BlastWaveManager.start(helper.getLevel(), helper.absoluteVec(Vec3.atCenterOf(BLAST)), 2.5, 18));
        helper.runAfterDelay(15, () -> {
            double vx = ship.velocity().x;
            ship.remove();
            helper.assertTrue(vx > 1, "the wave from the west should throw the ship east, its velocity is " + vx);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_aeronautics_bomb")
    public static void aDirtyBombAboardGoesOffWhereTheShipIs(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        Vec3 spawn = helper.absoluteVec(new Vec3(6.5, 4.5, 6.5));
        AeronauticsShips.Spawned ship = AeronauticsShips.single(helper, spawn, Blocks.STONE.defaultBlockState());
        helper.assertTrue(ship != null && ship.firstBlock() != null, "no ship spawned");
        BlockPos aboard = ship.firstBlock();
        ServerLevel level = helper.getLevel();
        FalloutData fallout = FalloutData.get(level);
        ContaminationData data = ContaminationData.get(level);
        int sources = fallout.sources().size();
        int events = data.events().size();
        try {
            ContaminationTests.loadAround(level, new ChunkPos(BlockPos.containing(spawn)), 4);
            NuclearDetonation.detonateDirty(level, aboard.getCenter(), 25f);
            helper.assertTrue(fallout.sources().size() == sources + 1, "the bomb leaves a hot spot");
            FalloutData.Source spot = fallout.sources().get(fallout.sources().size() - 1);
            helper.assertTrue(Vec3.atCenterOf(spot.pos()).distanceTo(spawn) < 2.5,
                    "the hot spot is at " + spot.pos() + ", not where the ship was, " + BlockPos.containing(spawn));
            helper.assertTrue(Contamination.get(level, new ChunkPos(BlockPos.containing(spawn))) > 0, "the ground under the ship is contaminated");
            fallout.sources().remove(fallout.sources().size() - 1);
            while (data.events().size() > events) {
                data.events().removeLast();
            }
        } finally {
            ContaminationTests.clean(helper, 4);
            if (!ship.isRemoved()) {
                ship.remove();
            }
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "_corium", timeoutTicks = 400)
    public static void coriumFallsOutOfAShip(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        Vec3 spawn = helper.absoluteVec(new Vec3(6.5, 9.5, 6.5));
        BlockState corium = PwrRegistry.CORIUM.get().defaultBlockState();
        AeronauticsShips.Spawned ship = AeronauticsShips.single(helper, spawn, corium);
        helper.assertTrue(ship != null && ship.firstBlock() != null, "no ship spawned");
        helper.getLevel().scheduleTick(ship.firstBlock(), corium.getBlock(), 1);
        helper.succeedWhen(() -> {
            // Out of the ship and into the ground under it, however deep it has melted in.
            int landed = 0;
            BlockPos under = BlockPos.containing(spawn);
            for (BlockPos pos : BlockPos.betweenClosed(under.offset(-2, -24, -2), under.offset(2, 0, 2))) {
                BlockState state = helper.getLevel().getBlockState(pos);
                if (state.getBlock() instanceof CoriumBlock || state.is(PwrRegistry.SOLIDIFIED_CORIUM.get())) {
                    landed++;
                }
            }
            helper.assertTrue(landed > 0, "no corium came down under the ship");
            helper.assertTrue(ship.isRemoved() || ship.firstBlock() == null, "the corium is still aboard");
            if (!ship.isRemoved()) {
                ship.remove();
            }
        });
    }

    // ------------------------------------------------------------------ contamination

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_aeronautics_uptake", timeoutTicks = 100)
    public static void aShipTakesUpContaminationAndKeepsIt(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        Vec3 spawn = helper.absoluteVec(new Vec3(6.5, 3.5, 6.5));
        ChunkPos under = new ChunkPos(BlockPos.containing(spawn));
        AeronauticsShips.Spawned spawned = AeronauticsShips.single(helper, spawn, Blocks.STONE.defaultBlockState());
        helper.assertTrue(spawned != null && spawned.firstBlock() != null, "no ship spawned");
        BlockPos aboard = spawned.firstBlock();
        try {
            ShipSpace.Ship ship = ShipSpace.shipAt(level, aboard);
            helper.assertTrue(ship != null, "the ship is not found");
            helper.assertTrue(ShipContamination.get(ship) == 0, "a new ship is clean");
            ContaminationTests.contaminate(helper, 2, 40f);
            ShipContamination.uptake(level, ship);
            float taken = ShipContamination.get(ship);
            helper.assertTrue(taken > 0 && taken < 40, "flying through it the ship takes up some of it, not all: " + taken);
            helper.assertTrue(ship.data(ShipContamination.KEY).contains("Level"), "and keeps it with the ship");
            helper.assertTrue(Contamination.get(level, new ChunkPos(aboard)) == taken, "its chunks read the ship's contamination");
            // What a scrubber aboard does: lowering a ship chunk cleans the ship.
            Contamination.set(level.getChunkAt(aboard), taken / 2);
            helper.assertTrue(Math.abs(ShipContamination.get(ship) - taken / 2) < 1.0e-3, "cleaning its chunk cleans the ship");
            helper.assertTrue(Contamination.get(level, under) == 40f, "the ground is left as it was");
        } finally {
            ContaminationTests.clean(helper, 2);
            spawned.remove();
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_aeronautics_release")
    public static void falloutReleasedAboardLandsUnderTheShip(GameTestHelper helper) {
        if (!SableCompat.isActive()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        Vec3 spawn = helper.absoluteVec(new Vec3(6.5, 3.5, 6.5));
        AeronauticsShips.Spawned spawned = AeronauticsShips.single(helper, spawn, Blocks.STONE.defaultBlockState());
        helper.assertTrue(spawned != null && spawned.firstBlock() != null, "no ship spawned");
        BlockPos aboard = spawned.firstBlock();
        ContaminationData data = ContaminationData.get(level);
        int events = data.events().size();
        try {
            ContaminationTests.loadAround(level, new ChunkPos(BlockPos.containing(spawn)), 2);
            // Wide enough to reach the middle of the chunk under the ship wherever the test stands.
            Contamination.fallout(level, aboard, 24, 20f);
            ShipSpace.Ship ship = ShipSpace.shipAt(level, aboard);
            helper.assertTrue(Contamination.get(level, new ChunkPos(BlockPos.containing(spawn))) > 0, "the ground under the ship is contaminated");
            helper.assertTrue(ship != null && ShipContamination.get(ship) > 0, "and so is the ship");
            ContaminationData.FalloutEvent event = data.events().getLast();
            helper.assertTrue(Math.abs(event.x() - spawn.x) < 2 && Math.abs(event.z() - spawn.z) < 2,
                    "the fallout is centred under the ship, not in its plot: " + event.x() + ", " + event.z());
            while (data.events().size() > events) {
                data.events().removeLast();
            }
        } finally {
            ContaminationTests.clean(helper, 2);
            spawned.remove();
        }
        helper.succeed();
    }
}

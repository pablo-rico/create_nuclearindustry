package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.GeologicalStorage;
import org.papiricoh.create_nuclearindustry.waste.WasteConfig;
import org.papiricoh.create_nuclearindustry.waste.WasteForm;
import org.papiricoh.create_nuclearindustry.waste.WastePackage;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryInspectorBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryProbe;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryQuality;

/**
 * The geological repository: a waste package is stored when the score of its surroundings
 * (depth, host rock, backfill, its own form, no groundwater) reaches the configured minimum.
 * A test arena is only a few blocks deep, so the tests shrink the depth span to match.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RepositoryTests {
    /** Where the package sits: in the middle of a 7x7x7 block of deepslate whose top is at least 3 blocks above it. */
    private static final BlockPos PACKAGE = new BlockPos(6, 5, 6);
    private static final int ROCK_ABOVE = 3;

    /** A deepslate block around {@link #PACKAGE}, bentonite on its faces if asked, and the package inside. */
    private static BlockPos buildRepository(GameTestHelper helper, BlockState pack, boolean bentonite) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    helper.setBlock(PACKAGE.offset(dx, dy, dz), Blocks.DEEPSLATE.defaultBlockState());
                }
            }
        }
        if (bentonite) {
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                helper.setBlock(PACKAGE.relative(direction), WasteRegistry.BENTONITE.get().defaultBlockState());
            }
        }
        helper.setBlock(PACKAGE, pack);
        RepositoryProbe.clearCache();
        return helper.absolutePos(PACKAGE);
    }

    private static BlockState canister() {
        return WasteRegistry.VITRIFIED_WASTE_CANISTER.get().defaultBlockState();
    }

    /** Runs {@code body} with the depth span shrunk to the arena and the new rule on. */
    private static void withShallowRepository(Runnable body) {
        int span = WasteConfig.depthSpan;
        boolean legacy = WasteConfig.legacyDepthRule;
        try {
            WasteConfig.depthSpan = ROCK_ABOVE;
            WasteConfig.legacyDepthRule = false;
            body.run();
        } finally {
            WasteConfig.depthSpan = span;
            WasteConfig.legacyDepthRule = legacy;
            RepositoryProbe.clearCache();
        }
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void vitrifiedCanisterInDeepslateWithBentoniteIsStored(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = buildRepository(helper, canister(), true);
        withShallowRepository(() -> {
            RepositoryQuality.Site site = RepositoryProbe.sample(level, pos);
            // The test harness may put blocks higher up the column; at least our own rock is above it.
            helper.assertTrue(site.depthBelowSurface() >= ROCK_ABOVE, "the package is under at least 3 blocks of rock, got "
                    + site.depthBelowSurface());
            helper.assertTrue(site.hostRockFraction() == 1.0, "deepslate all round should be full host rock, got " + site.hostRockFraction());
            helper.assertTrue(site.backfilledFaces() == 6 && !site.fluidNearby(), "six bentonite faces and no water: " + site);
            double score = GeologicalStorage.score(level, level.getBlockState(pos), pos);
            helper.assertTrue(score > 0.99, "a full repository should score 1, got " + score);
            helper.assertTrue(GeologicalStorage.isStored(level, level.getBlockState(pos), pos), "it should count as stored");
            helper.assertTrue(GeologicalStorage.emission(level, level.getBlockState(pos), pos, 8.0) == 0, "a stored canister gives off nothing");
            helper.assertTrue(GeologicalStorage.leak(level, level.getBlockState(pos), pos) == 0, "a stored canister leaks nothing");
        });
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void canisterNearWaterIsNotStored(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = buildRepository(helper, canister(), true);
        // A pocket of groundwater two blocks away, sealed in the rock.
        helper.setBlock(PACKAGE.east(2), Blocks.WATER.defaultBlockState());
        RepositoryProbe.clearCache();
        withShallowRepository(() -> {
            RepositoryQuality.Breakdown breakdown = RepositoryProbe.assess(level, pos, WastePackage.of(level.getBlockState(pos)));
            helper.assertTrue(breakdown.fluidPenalty() > 0, "the water should be noticed");
            helper.assertTrue(breakdown.score() < WasteConfig.minScore, "groundwater should spoil the repository, score " + breakdown.score());
            helper.assertFalse(GeologicalStorage.isStored(level, level.getBlockState(pos), pos), "a wet repository does not store");
            helper.assertTrue(GeologicalStorage.leak(level, level.getBlockState(pos), pos) > 0, "a wet canister leaks a little");
            helper.assertTrue(GeologicalStorage.emission(level, level.getBlockState(pos), pos, 8.0) < 8.0 * 0.5,
                    "but still less than a canister in the open");
        });
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void depthAndRockAloneAreNotEnough(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = buildRepository(helper, canister(), false);
        withShallowRepository(() -> {
            double score = GeologicalStorage.score(level, level.getBlockState(pos), pos);
            helper.assertTrue(Math.abs(score - 0.75) < 1e-6, "deep in rock without backfill should score 0.75, got " + score);
            helper.assertFalse(GeologicalStorage.isStored(level, level.getBlockState(pos), pos), "it needs backfill to be stored");
        });
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void betterFormsScoreHigherInTheSameSpot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = buildRepository(helper, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get().defaultBlockState(), false);
        withShallowRepository(() -> {
            double barrel = GeologicalStorage.score(level, level.getBlockState(pos), pos);
            level.setBlockAndUpdate(pos, WasteRegistry.CEMENTED_WASTE_DRUM.get().defaultBlockState());
            double cemented = GeologicalStorage.score(level, level.getBlockState(pos), pos);
            level.setBlockAndUpdate(pos, canister());
            double vitrified = GeologicalStorage.score(level, level.getBlockState(pos), pos);
            helper.assertTrue(barrel < cemented && cemented < vitrified,
                    "raw < cemented < vitrified expected, got " + barrel + ", " + cemented + ", " + vitrified);
            helper.assertTrue(WastePackage.of(level.getBlockState(pos)).form() == WasteForm.VITRIFIED, "the canister is vitrified");
        });
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void legacyDepthRuleKeepsTheOldBehaviour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 1, 6));
        level.setBlockAndUpdate(pos, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get().defaultBlockState());
        BlockState barrel = level.getBlockState(pos);
        int storage = Config.geologicalStorageMaxY;
        boolean legacy = WasteConfig.legacyDepthRule;
        try {
            Config.geologicalStorageMaxY = pos.getY();
            WasteConfig.legacyDepthRule = true;
            helper.assertTrue(GeologicalStorage.isStored(level, barrel, pos), "under the old rule, depth alone stores a barrel");
            helper.assertTrue(GeologicalStorage.leak(level, barrel, pos) == 0, "and it does not leak");

            WasteConfig.legacyDepthRule = false;
            RepositoryProbe.clearCache();
            helper.assertFalse(GeologicalStorage.isStored(level, barrel, pos), "under the new rule a bare barrel is not stored");
            double leak = GeologicalStorage.leak(level, barrel, pos);
            helper.assertTrue(leak > 0 && leak < Config.barrelLeak, "it leaks, a little less than the full rate: " + leak);

            WasteConfig.legacyDepthRule = true;
            Config.geologicalStorageMaxY = pos.getY() - 1;
            helper.assertTrue(Math.abs(GeologicalStorage.leak(level, barrel, pos) - Config.barrelLeak) < 1e-9,
                    "above the old depth it leaks the full rate");
        } finally {
            Config.geologicalStorageMaxY = storage;
            WasteConfig.legacyDepthRule = legacy;
            RepositoryProbe.clearCache();
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_packagesLeakByTheirFactor")
    public static void packagesLeakByTheirFactor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos barrelPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos canisterPos = helper.absolutePos(new BlockPos(10, 1, 10));
        level.setBlockAndUpdate(barrelPos, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get().defaultBlockState());
        level.setBlockAndUpdate(canisterPos, canister());
        boolean legacy = WasteConfig.legacyDepthRule;
        try {
            WasteConfig.legacyDepthRule = false;
            RepositoryProbe.clearCache();
            ContaminationTests.clean(helper, 0);
            BlockState barrel = level.getBlockState(barrelPos);
            double expected = Config.barrelLeak * (1 - GeologicalStorage.score(level, barrel, barrelPos));
            barrel.randomTick(level, barrelPos, level.random);
            float leaked = Contamination.get(level, new ChunkPos(barrelPos));
            helper.assertTrue(Math.abs(leaked - expected) < 1e-5, "a surface barrel should leak " + expected + ", got " + leaked);

            double canisterLeak = GeologicalStorage.leak(level, level.getBlockState(canisterPos), canisterPos);
            helper.assertTrue(canisterLeak > 0 && canisterLeak < expected / 10,
                    "a vitrified canister leaks a fraction of a barrel, got " + canisterLeak + " vs " + expected);
            helper.assertTrue(level.getBlockState(canisterPos).isRandomlyTicking(), "packages take random ticks");
        } finally {
            WasteConfig.legacyDepthRule = legacy;
            RepositoryProbe.clearCache();
            ContaminationTests.clean(helper, 0);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void inspectorReportsStoredPackagesAndTheWeakest(GameTestHelper helper) {
        buildRepository(helper, canister(), true);
        BlockPos barrel = new BlockPos(1, 1, 6);
        helper.setBlock(barrel, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get().defaultBlockState());
        BlockPos inspectorPos = new BlockPos(1, 5, 1);
        helper.setBlock(inspectorPos, WasteRegistry.REPOSITORY_INSPECTOR.get().defaultBlockState());
        withShallowRepository(() -> {
            RepositoryInspectorBlockEntity inspector = helper.getBlockEntity(inspectorPos);
            helper.assertTrue(inspector != null, "the inspector should have its block entity");
            inspector.scan();
            helper.assertTrue(inspector.packages() == 2, "two packages in range, got " + inspector.packages());
            helper.assertTrue(inspector.stored() == 1, "only the buried canister is stored, got " + inspector.stored());
            helper.assertTrue(inspector.weakestPos().equals(helper.absolutePos(barrel)), "the bare barrel is the weakest");
            helper.assertTrue(inspector.weakest().score() < WasteConfig.minScore, "and it is below the minimum");
            helper.assertTrue(inspector.signal() == 8, "half stored should give 8, got " + inspector.signal());
            helper.assertTrue(DisplaySource.getAll(helper.getLevel(), helper.absolutePos(inspectorPos))
                    .contains(WasteRegistry.REPOSITORY_INSPECTOR_SOURCE.get()), "the inspector should feed a Display Link");
        });
        helper.succeed();
    }
}

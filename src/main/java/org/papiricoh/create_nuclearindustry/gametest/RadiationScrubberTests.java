package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.scrubber.RadiationScrubberBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.function.Consumer;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RadiationScrubberTests {
    private static final BlockPos SCRUBBER = new BlockPos(6, 2, 6);

    /** A scrubber driven from below at {@code rpm}; {@code check} runs once the network has spun up. */
    private static void withScrubber(GameTestHelper helper, int rpm, Consumer<RadiationScrubberBlockEntity> check) {
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(SCRUBBER.below(), motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(SCRUBBER.below());
        motorEntity.generatedSpeed.setValue(rpm);
        helper.setBlock(SCRUBBER, AllNuclearBlocks.RADIATION_SCRUBBER.get().defaultBlockState());
        helper.runAfterDelay(10, () -> {
            try {
                check.accept(helper.getBlockEntity(SCRUBBER));
            } finally {
                ContaminationTests.clean(helper, Config.scrubberRadius);
            }
            helper.succeed();
        });
    }

    private static ChunkPos home(GameTestHelper helper) {
        return new ChunkPos(helper.absolutePos(SCRUBBER));
    }

    private static void set(GameTestHelper helper, int dx, int dz, float value) {
        ChunkPos home = home(helper);
        Contamination.set(helper.getLevel().getChunk(home.x + dx, home.z + dz), value);
    }

    private static float get(GameTestHelper helper, int dx, int dz) {
        ChunkPos home = home(helper);
        return Contamination.get(helper.getLevel(), new ChunkPos(home.x + dx, home.z + dz));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_scrubberAtFullSpeed", timeoutTicks = 100)
    public static void scrubberAtFullSpeed(GameTestHelper helper) {
        withScrubber(helper, 256, scrubber -> {
            set(helper, 0, 0, 1);
            float removed = scrubber.scrub();
            helper.assertTrue(Math.abs(removed - (float) Config.scrubberRate) < 1e-5, "256 RPM should take the full rate, took " + removed);
            helper.assertTrue(Math.abs(get(helper, 0, 0) - (1 - (float) Config.scrubberRate)) < 1e-5, "the chunk should be cleaner");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_scrubberAtHalfSpeed", timeoutTicks = 100)
    public static void scrubberAtHalfSpeed(GameTestHelper helper) {
        withScrubber(helper, 128, scrubber -> {
            set(helper, 0, 0, 1);
            float removed = scrubber.scrub();
            helper.assertTrue(Math.abs(removed - (float) Config.scrubberRate / 2) < 1e-5, "128 RPM should take half, took " + removed);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_stoppedScrubberDoesNothing", timeoutTicks = 100)
    public static void stoppedScrubberDoesNothing(GameTestHelper helper) {
        withScrubber(helper, 0, scrubber -> {
            set(helper, 0, 0, 1);
            helper.assertTrue(scrubber.scrub() == 0, "without rotation nothing is absorbed");
            helper.assertTrue(get(helper, 0, 0) == 1f, "the chunk should stay as it was");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_everyChunkInReach", timeoutTicks = 100)
    public static void everyChunkInReachIsCleanedAtTheFullRate(GameTestHelper helper) {
        withScrubber(helper, 256, scrubber -> {
            float rate = (float) Config.scrubberRate;
            float under = rate * 0.4f;
            set(helper, 0, 0, under);
            set(helper, Config.scrubberRadius, 0, 5);
            set(helper, Config.scrubberRadius + 1, 0, 5);
            float removed = scrubber.scrub();
            helper.assertTrue(get(helper, 0, 0) == 0, "its own chunk, under the rate, should come out clean, left " + get(helper, 0, 0));
            helper.assertTrue(Math.abs(get(helper, Config.scrubberRadius, 0) - (5 - rate)) < 1e-5,
                    "a chunk at the edge of its reach should lose the full rate, not a share");
            helper.assertTrue(get(helper, Config.scrubberRadius + 1, 0) == 5f, "a chunk out of reach should stay as it was");
            helper.assertTrue(Math.abs(removed - (under + rate)) < 1e-5, "it should count what it took from each, took " + removed);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_scrubberWeakensHotSpots", timeoutTicks = 100)
    public static void hotSpotsInReachFadeSooner(GameTestHelper helper) {
        withScrubber(helper, 256, scrubber -> {
            ServerLevel level = helper.getLevel();
            FalloutData fallout = FalloutData.get(level);
            long now = level.getGameTime();
            BlockPos near = helper.absolutePos(SCRUBBER.east(3));
            fallout.add(near, 4, 24000, now);
            FalloutData.Source source = fallout.sources().get(fallout.sources().size() - 1);
            try {
                float removed = scrubber.scrub();
                double left = source.currentStrength(now);
                helper.assertTrue(Math.abs(left - (4 - Config.scrubberRate)) < 1e-3,
                        "the hot spot should be weaker by the rate, left " + left);
                helper.assertTrue(removed >= Config.scrubberRate - 1e-5, "and count as absorbed, took " + removed);
                helper.assertTrue(source.expires() < now + 24000, "it should run out sooner");
            } finally {
                fallout.sources().remove(source);
            }
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_brokenScrubberDropsWaste", timeoutTicks = 100)
    public static void aBrokenScrubberDropsItsWaste(GameTestHelper helper) {
        withScrubber(helper, 256, scrubber -> {
            scrubber.getOutput().setStackInSlot(0, new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get(), 5));
            helper.destroyBlock(SCRUBBER);
            int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(SCRUBBER)).inflate(2)).stream()
                    .filter(item -> item.getItem().is(WasteRegistry.LOW_LEVEL_WASTE.get())).mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(dropped == 5, "the waste inside should drop, dropped " + dropped);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_absorbedBecomesWaste", timeoutTicks = 100)
    public static void absorbedBecomesWasteUntilFull(GameTestHelper helper) {
        withScrubber(helper, 256, scrubber -> {
            set(helper, 0, 0, 10);
            double per = Config.scrubberWastePer;
            Config.scrubberWastePer = Config.scrubberRate;
            try {
                scrubber.scrub();
                scrubber.scrub();
                helper.assertTrue(scrubber.getOutput().getStackInSlot(0).getCount() == 2, "each rate's worth should be one waste");
                helper.assertTrue(scrubber.radiationStrength() > 0, "waste inside should irradiate");
                helper.assertTrue(scrubber.getAutomationHandler().extractItem(0, 1, false).getCount() == 1, "a hopper should take the waste");

                scrubber.getOutput().setStackInSlot(0, new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 64));
                float before = get(helper, 0, 0);
                helper.assertTrue(scrubber.isFull() && scrubber.scrub() == 0, "a full scrubber should stop");
                helper.assertTrue(get(helper, 0, 0) == before, "and leave the ground alone");
            } finally {
                Config.scrubberWastePer = per;
            }
        });
    }
}

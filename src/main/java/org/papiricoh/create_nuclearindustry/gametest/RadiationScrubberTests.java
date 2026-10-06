package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.scrubber.RadiationScrubberBlockEntity;

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
                ContaminationTests.clean(helper, 1);
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

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_mostContaminatedFirst", timeoutTicks = 100)
    public static void mostContaminatedFirst(GameTestHelper helper) {
        withScrubber(helper, 256, scrubber -> {
            set(helper, 0, 0, 0.5f);
            set(helper, 1, 0, 5);
            scrubber.scrub();
            helper.assertTrue(get(helper, 0, 0) == 0.5f, "its own, cleaner chunk should wait");
            helper.assertTrue(get(helper, 1, 0) < 5f, "the worse neighbour should go first");
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

package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.DecontaminationConfig;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.DecontaminationRegistry;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.DecontaminationSprinklerBlockEntity;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.Phytoremediation;

import java.util.function.Consumer;

/** The Decontamination Sprinkler washing the ground, filling its contaminated water tank and rinsing players; sunflowers absorbing. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class DecontaminationTests {
    private static final BlockPos SPRINKLER = new BlockPos(6, 2, 6);
    private static final BlockPos FLOWER = new BlockPos(3, 2, 3);

    /** A sprinkler driven from below at {@code rpm}; {@code check} runs once the network has spun up. */
    private static void withSprinkler(GameTestHelper helper, int rpm, Consumer<DecontaminationSprinklerBlockEntity> check) {
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(SPRINKLER.below(), motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(SPRINKLER.below());
        motorEntity.generatedSpeed.setValue(rpm);
        helper.setBlock(SPRINKLER, DecontaminationRegistry.SPRINKLER.get().defaultBlockState());
        helper.runAfterDelay(10, () -> {
            try {
                DecontaminationSprinklerBlockEntity sprinkler = helper.getBlockEntity(SPRINKLER);
                helper.assertTrue(sprinkler != null, "the sprinkler should have its block entity");
                check.accept(sprinkler);
            } finally {
                ContaminationTests.clean(helper, DecontaminationConfig.sprinklerRadius);
            }
            helper.succeed();
        });
    }

    private static LevelChunk chunk(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getChunkAt(helper.absolutePos(relative));
    }

    private static void fillWater(DecontaminationSprinklerBlockEntity sprinkler) {
        IFluidHandler pipes = sprinkler.getFluidHandler(Direction.NORTH);
        pipes.fill(new FluidStack(Fluids.WATER, DecontaminationSprinklerBlockEntity.CAPACITY), IFluidHandler.FluidAction.EXECUTE);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_sprinklerWashesTheGround", timeoutTicks = 100)
    public static void theSprinklerWashesTheGroundIntoItsTank(GameTestHelper helper) {
        withSprinkler(helper, 256, sprinkler -> {
            fillWater(sprinkler);
            Contamination.set(chunk(helper, SPRINKLER), 5);
            float removed = sprinkler.spray();
            float rate = (float) DecontaminationConfig.sprinklerRate;
            helper.assertTrue(sprinkler.status() == DecontaminationSprinklerBlockEntity.Status.SPRAYING, "it should spray, " + sprinkler.status());
            helper.assertTrue(Math.abs(removed - rate) < 1e-4 && Math.abs(Contamination.read(chunk(helper, SPRINKLER)) - (5 - rate)) < 1e-4,
                    "it should wash the full rate off its chunk, took " + removed);
            helper.assertTrue(sprinkler.waterTank().getFluidAmount() == DecontaminationSprinklerBlockEntity.CAPACITY - DecontaminationConfig.sprinklerWaterPerSecond,
                    "it should use its water, " + sprinkler.waterTank().getFluidAmount());
            int collected = sprinkler.contaminatedTank().getFluidAmount();
            helper.assertTrue(collected == (int) (rate * DecontaminationConfig.sprinklerContaminatedPerRad),
                    "what it washed off should collect as contaminated water, " + collected);
            helper.assertTrue(sprinkler.getFluidHandler(Direction.EAST).drain(1000, IFluidHandler.FluidAction.SIMULATE)
                    .is(AllNuclearFluids.CONTAMINATED_WATER.get()), "pipes should drain the contaminated water");
            helper.assertTrue(sprinkler.getFluidHandler(Direction.DOWN) == null, "not through the shaft");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_sprinklerNeedsWaterAndRoom", timeoutTicks = 100)
    public static void theSprinklerNeedsWaterAndRoom(GameTestHelper helper) {
        withSprinkler(helper, 256, sprinkler -> {
            Contamination.set(chunk(helper, SPRINKLER), 5);
            helper.assertTrue(sprinkler.spray() == 0 && sprinkler.status() == DecontaminationSprinklerBlockEntity.Status.NO_WATER,
                    "without water it should not spray");
            fillWater(sprinkler);
            sprinkler.contaminatedTank().fill(new FluidStack(AllNuclearFluids.CONTAMINATED_WATER.get(), DecontaminationSprinklerBlockEntity.CAPACITY),
                    IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(sprinkler.spray() == 0 && sprinkler.status() == DecontaminationSprinklerBlockEntity.Status.FULL,
                    "with its contaminated water tank full it should stop");
            helper.assertTrue(Contamination.read(chunk(helper, SPRINKLER)) == 5f, "and leave the ground alone");
            // Half full is room enough for what it washes off.
            sprinkler.contaminatedTank().drain(DecontaminationSprinklerBlockEntity.CAPACITY / 2, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(sprinkler.spray() > 0 && sprinkler.status() == DecontaminationSprinklerBlockEntity.Status.SPRAYING,
                    "with room for what it washes off it should spray, " + sprinkler.status());
            helper.assertTrue(sprinkler.getFluidHandler(Direction.NORTH).fill(new FluidStack(AllNuclearFluids.CONTAMINATED_WATER.get(), 100),
                    IFluidHandler.FluidAction.SIMULATE) == 0, "contaminated water is not sprayed again");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_stoppedSprinkler", timeoutTicks = 100)
    public static void aStoppedSprinklerDoesNothing(GameTestHelper helper) {
        withSprinkler(helper, 0, sprinkler -> {
            fillWater(sprinkler);
            Contamination.set(chunk(helper, SPRINKLER), 5);
            helper.assertTrue(sprinkler.spray() == 0 && sprinkler.status() == DecontaminationSprinklerBlockEntity.Status.NO_ROTATION,
                    "without rotation it should not spray");
            helper.assertTrue(sprinkler.waterTank().getFluidAmount() == DecontaminationSprinklerBlockEntity.CAPACITY, "nor use water");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_sprinklerShower", timeoutTicks = 100)
    public static void itsSprayRinsesTheDoseOff(GameTestHelper helper) {
        withSprinkler(helper, 256, sprinkler -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(SPRINKLER.east(2))));
            helper.assertTrue(sprinkler.sprayArea().contains(player.position()), "two blocks out is in the spray");
            player.setData(AllNuclearRadiation.DOSE, 100f);
            player.setData(AllNuclearRadiation.TOTAL_DOSE, 300f);
            float rinsed = RadiationHandler.rinse(player, (float) DecontaminationConfig.sprinklerShowerPerSecond);
            helper.assertTrue(rinsed == (float) DecontaminationConfig.sprinklerShowerPerSecond
                    && player.getData(AllNuclearRadiation.DOSE) == 100f - rinsed, "the shower should wash dose off");
            helper.assertTrue(player.getData(AllNuclearRadiation.TOTAL_DOSE) == 300f, "but never the lifetime total");
            player.setData(AllNuclearRadiation.DOSE, 1f);
            helper.assertTrue(RadiationHandler.rinse(player, 5) == 1f && player.getData(AllNuclearRadiation.DOSE) == 0f, "down to zero");
        });
    }

    private static void plantSunflower(GameTestHelper helper) {
        helper.setBlock(FLOWER.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(FLOWER, Blocks.SUNFLOWER.defaultBlockState());
        helper.setBlock(FLOWER.above(), Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_sunflowersAbsorb", timeoutTicks = 100)
    public static void sunflowersAbsorbUntilTheyWilt(GameTestHelper helper) {
        plantSunflower(helper);
        LevelChunk chunk = chunk(helper, FLOWER);
        try {
            helper.assertTrue(helper.getBlockState(FLOWER).isRandomlyTicking(), "a sunflower should get random ticks");
            helper.assertFalse(helper.getBlockState(FLOWER.above()).isRandomlyTicking(), "only its lower half absorbs");
            RandomSource random = RandomSource.create(5);
            helper.assertTrue(Phytoremediation.randomTick(helper.getLevel(), helper.absolutePos(FLOWER), helper.getBlockState(FLOWER), random) == 0,
                    "on clean ground it takes nothing");
            Contamination.set(chunk, 10);
            float taken = 0;
            for (int i = 0; i < 1000 && helper.getBlockState(FLOWER).is(Blocks.SUNFLOWER); i++) {
                taken += Phytoremediation.randomTick(helper.getLevel(), helper.absolutePos(FLOWER), helper.getBlockState(FLOWER), random);
            }
            helper.assertTrue(taken > 0 && Contamination.read(chunk) < 10f, "it should draw contamination out, took " + taken);
            helper.assertBlockPresent(DecontaminationRegistry.IRRADIATED_SUNFLOWER.get(), FLOWER);
            helper.assertBlockPresent(DecontaminationRegistry.IRRADIATED_SUNFLOWER.get(), FLOWER.above());
            float left = Contamination.read(chunk);
            helper.assertTrue(Phytoremediation.randomTick(helper.getLevel(), helper.absolutePos(FLOWER), helper.getBlockState(FLOWER), random) == 0
                    && Contamination.read(chunk) == left, "a wilted one absorbs no more");

            helper.getLevel().destroyBlock(helper.absolutePos(FLOWER), true);
            int biomass = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FLOWER)).inflate(2)).stream()
                    .filter(item -> item.getItem().is(DecontaminationRegistry.IRRADIATED_BIOMASS.get()))
                    .mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(biomass >= 1 && biomass <= 2, "it should drop one or two biomass, dropped " + biomass);
        } finally {
            ContaminationTests.clean(helper, 0);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_biomassRecipe")
    public static void biomassIsPressedIntoLowLevelWaste(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "compacting/low_level_waste_from_irradiated_biomass");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isPresent(), "missing " + id);
        helper.succeed();
    }
}

package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.fuelcycle.fan.NuclearFanTypes;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.List;

/**
 * The acid leaching and irradiation fan treatments in a real fan current: an Encased Fan driven by
 * a creative motor blows east down a walled lane, past its catalysts, over a depot or a belt set
 * into the floor and onto items lying at the far end against a glass wall.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class FanTreatmentTests {
    private static final int LANE_Z = 6;
    private static final int Y = 2;
    private static final BlockPos FAN = new BlockPos(2, Y, LANE_Z);
    /** Where the current ends: the glass wall stands one further. */
    private static final int LAST_X = 9;
    /** The depot (or belt) under the current, four blocks from the fan. */
    private static final BlockPos UNDER = new BlockPos(6, Y - 1, LANE_Z);
    private static final int TIMEOUT = 500;

    private static Block create(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id));
    }

    /**
     * Builds the lane: a stone floor with glass walls, the fan and its motor at the west end, and
     * {@code catalysts} in the current right in front of the fan, in order.
     */
    private static void lane(GameTestHelper helper, BlockState... catalysts) {
        for (int x = FAN.getX() - 1; x <= LAST_X + 1; x++) {
            for (int z = LANE_Z - 1; z <= LANE_Z + 1; z++) {
                helper.setBlock(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState());
                if (z != LANE_Z || x > LAST_X) {
                    helper.setBlock(new BlockPos(x, Y, z), Blocks.GLASS.defaultBlockState());
                    helper.setBlock(new BlockPos(x, Y + 1, z), Blocks.GLASS.defaultBlockState());
                }
            }
            helper.setBlock(new BlockPos(x, Y + 1, LANE_Z), Blocks.GLASS.defaultBlockState());
        }
        for (int i = 0; i < catalysts.length; i++) {
            helper.setBlock(FAN.east(i + 1), catalysts[i]);
        }
        helper.setBlock(FAN, create("encased_fan").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        BlockPos motorPos = FAN.west();
        helper.setBlock(motorPos, create("creative_motor").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        CreativeMotorBlockEntity motor = helper.getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(128);
    }

    /** Turns the motor round if the fan pulls, so every test sees a fan blowing east. */
    private static void blowEast(GameTestHelper helper) {
        helper.runAfterDelay(3, () -> {
            EncasedFanBlockEntity fan = fan(helper);
            if (fan != null && fan.getAirFlowDirection() != Direction.EAST) {
                CreativeMotorBlockEntity motor = helper.getBlockEntity(FAN.west());
                motor.generatedSpeed.setValue(-128);
            }
        });
    }

    private static EncasedFanBlockEntity fan(GameTestHelper helper) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(FAN)) instanceof EncasedFanBlockEntity fan ? fan : null;
    }

    private static BlockState acid() {
        return AllNuclearFluids.SULFURIC_ACID_BLOCK.get().defaultBlockState();
    }

    private static BlockState source() {
        return FuelCycleRegistry.IRRADIATION_SOURCE.get().defaultBlockState();
    }

    private static void depot(GameTestHelper helper, ItemStack stack) {
        helper.setBlock(UNDER, create("depot").defaultBlockState());
        helper.runAfterDelay(2, () -> {
            IItemHandler depot = handler(helper, UNDER);
            helper.assertTrue(depot != null, "the depot should take items");
            if (depot != null) {
                helper.assertTrue(depot.insertItem(0, stack, false).isEmpty(), "the depot should take " + stack);
            }
        });
    }

    private static IItemHandler handler(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
    }

    private static boolean holds(IItemHandler handler, Item item) {
        if (handler == null) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean onTheGround(GameTestHelper helper, Item item) {
        List<ItemEntity> items = helper.getEntities(EntityType.ITEM);
        return items.stream().anyMatch(entity -> entity.getItem().is(item));
    }

    /** The current's treatment at {@code offset} blocks from the fan, or null. */
    private static FanProcessingType typeAt(GameTestHelper helper, int offset) {
        EncasedFanBlockEntity fan = fan(helper);
        return fan == null || fan.airCurrent == null ? null : fan.airCurrent.getTypeAt(offset);
    }

    // ------------------------------------------------------------------ acid leaching

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void acidLeachesItemsOnADepot(GameTestHelper helper) {
        lane(helper, acid());
        blowEast(helper);
        depot(helper, new ItemStack(FuelCycleRegistry.HEAVY_MINERAL_SAND.get()));
        helper.succeedWhen(() -> {
            IItemHandler depot = handler(helper, UNDER);
            helper.assertTrue(holds(depot, FuelCycleRegistry.THORIUM_DIOXIDE.get()),
                    "heavy mineral sand on the depot should be leached into thorium dioxide");
            helper.assertFalse(holds(depot, FuelCycleRegistry.HEAVY_MINERAL_SAND.get()), "no sand should be left");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void acidLeachesItemsOnTheGround(GameTestHelper helper) {
        lane(helper, acid());
        blowEast(helper);
        helper.runAfterDelay(10, () -> helper.spawnItem(WasteRegistry.LOW_LEVEL_WASTE.get(), 7.5f, Y + 0.2f, LANE_Z + 0.5f));
        helper.succeedWhen(() -> {
            helper.assertTrue(onTheGround(helper, net.minecraft.world.item.Items.IRON_NUGGET),
                    "low-level waste blown through acid should come out as clean iron nuggets");
            helper.assertTrue(onTheGround(helper, FuelCycleRegistry.RADIOACTIVE_SLUDGE.get()), "and its activity as sludge");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void acidLeachesItemsOnABelt(GameTestHelper helper) {
        lane(helper, acid());
        blowEast(helper);
        BlockPos start = UNDER.west();
        BlockPos end = UNDER.east();
        BeltConnectorItem.createBelts(helper.getLevel(), helper.absolutePos(start), helper.absolutePos(end));
        helper.runAfterDelay(5, () -> {
            IItemHandler belt = handler(helper, UNDER);
            helper.assertTrue(belt != null, "the belt should take items");
            if (belt != null) {
                helper.assertTrue(belt.insertItem(0, new ItemStack(FuelCycleRegistry.HEAVY_MINERAL_SAND.get()), false).isEmpty(),
                        "the belt should take heavy mineral sand");
            }
        });
        helper.succeedWhen(() -> {
            boolean leached = false;
            for (int x = start.getX(); x <= end.getX(); x++) {
                leached |= holds(handler(helper, new BlockPos(x, UNDER.getY(), LANE_Z)), FuelCycleRegistry.THORIUM_DIOXIDE.get());
            }
            helper.assertTrue(leached, "heavy mineral sand on the belt should be leached into thorium dioxide");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void acidSprayBurnsCreatures(GameTestHelper helper) {
        lane(helper, acid());
        blowEast(helper);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 7.5f, Y, LANE_Z + 0.5f);
        helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the acid spray should hurt the pig"));
    }

    // ------------------------------------------------------------------ acid and water in one current

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void acidAfterWaterLeaches(GameTestHelper helper) {
        lane(helper, Blocks.WATER.defaultBlockState(), acid());
        blowEast(helper);
        // Low-level waste has no washing recipe: only the acid can turn it into nuggets.
        depot(helper, new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(typeAt(helper, 3) == NuclearFanTypes.ACID_LEACHING.get(), "past the acid the current should leach");
            helper.assertTrue(holds(handler(helper, UNDER), net.minecraft.world.item.Items.IRON_NUGGET),
                    "the last catalyst, the acid, should treat the waste");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void waterAfterAcidWashes(GameTestHelper helper) {
        lane(helper, acid(), Blocks.WATER.defaultBlockState());
        blowEast(helper);
        // Crushed uranium has no leaching recipe: only the water can wash it into concentrate.
        depot(helper, new ItemStack(AllNuclearItems.CRUSHED_RAW_URANIUM.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(typeAt(helper, 3) != NuclearFanTypes.ACID_LEACHING.get(), "past the water the current should not leach");
            helper.assertTrue(holds(handler(helper, UNDER), AllNuclearItems.URANIUM_CONCENTRATE.get()),
                    "the last catalyst, the water, should wash the ore");
        });
    }

    // ------------------------------------------------------------------ irradiation

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void airBlowsThroughTheIrradiationSource(GameTestHelper helper) {
        lane(helper, source());
        blowEast(helper);
        helper.succeedWhen(() -> {
            EncasedFanBlockEntity fan = fan(helper);
            helper.assertTrue(fan != null && fan.airCurrent != null, "the fan should have a current");
            if (fan != null && fan.airCurrent != null) {
                helper.assertTrue(fan.airCurrent.maxDistance >= LAST_X - FAN.getX() - 0.5f,
                        "the source should let the air through, current ends after " + fan.airCurrent.maxDistance);
            }
            helper.assertTrue(typeAt(helper, 4) == NuclearFanTypes.IRRADIATION.get(), "past the source the current should irradiate");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void irradiationSourceIrradiatesItemsOnADepot(GameTestHelper helper) {
        lane(helper, source());
        blowEast(helper);
        depot(helper, new ItemStack(FuelCycleRegistry.THORIUM_DIOXIDE.get()));
        helper.succeedWhen(() -> helper.assertTrue(holds(handler(helper, UNDER), FuelCycleRegistry.IRRADIATED_THORIUM.get()),
                "thorium dioxide past the source should come out irradiated"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void irradiationTreatsItemsOnTheGround(GameTestHelper helper) {
        lane(helper, source());
        blowEast(helper);
        helper.runAfterDelay(10, () -> helper.spawnItem(FuelCycleRegistry.THORIUM_DIOXIDE.get(), 7.5f, Y + 0.2f, LANE_Z + 0.5f));
        helper.succeedWhen(() -> helper.assertTrue(onTheGround(helper, FuelCycleRegistry.IRRADIATED_THORIUM.get()),
                "thorium dioxide blown past the source should come out irradiated"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = TIMEOUT)
    public static void irradiationDosesCreatures(GameTestHelper helper) {
        lane(helper, source());
        blowEast(helper);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 7.5f, Y, LANE_Z + 0.5f);
        helper.succeedWhen(() -> helper.assertTrue(pig.getData(AllNuclearRadiation.DOSE) > 1.0f,
                "the pig in the current should take a dose, has " + pig.getData(AllNuclearRadiation.DOSE)));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void onlyRadioactiveBlocksAreIrradiationCatalysts(GameTestHelper helper) {
        helper.assertTrue(NuclearFanTypes.isIrradiationCatalyst(source()), "the irradiation source is a catalyst");
        helper.assertFalse(NuclearFanTypes.isIrradiationCatalyst(Blocks.STONE.defaultBlockState()), "stone is not");
        helper.assertFalse(NuclearFanTypes.isIrradiationCatalyst(Blocks.AIR.defaultBlockState()), "air is not");
        helper.assertTrue(NuclearFanTypes.ACID_LEACHING.get().isValidAt(helper.getLevel(), helper.absolutePos(placeAcid(helper))),
                "a pool of sulfuric acid is the acid leaching catalyst");
        helper.succeed();
    }

    private static BlockPos placeAcid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(pos, acid());
        return pos;
    }
}

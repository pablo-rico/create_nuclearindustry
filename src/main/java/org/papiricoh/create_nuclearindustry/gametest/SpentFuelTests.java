package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.physics.FuelKind;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelData;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelHooks;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.block.DryCaskBlockEntity;
import org.papiricoh.create_nuclearindustry.spentfuel.block.PoolCoolerBlock;
import org.papiricoh.create_nuclearindustry.spentfuel.block.PoolCoolerBlockEntity;
import org.papiricoh.create_nuclearindustry.spentfuel.block.RackLoad;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelRackBlock;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelRackBlockEntity;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelStorageBlockEntity;

import java.util.List;

/**
 * Spent fuel cooling: discharge dating, grades, the pool rack (water shielding and boil-off), the
 * pool cooler, the dry cask and graded reprocessing. Tests with loaded racks run in their own batch
 * and empty them at the end, so fresh fuel never irradiates other tests.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SpentFuelTests {
    private static final String BATCH = "spentfuel";

    // ------------------------------------------------------------------ stamping and grades

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void reactorDischargesHotDatedFuel(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            var registries = helper.getLevel().registryAccess();
            CompoundTag tag = reactor.saveWithoutMetadata(registries);
            ListTag pending = new ListTag();
            CompoundTag entry = new CompoundTag();
            entry.putString("type", FuelKind.URANIUM.id());
            entry.putFloat("enrichment", 5f);
            pending.add(entry);
            tag.put("pendingSpent", pending);
            reactor.loadWithComponents(tag, registries);
        });
        helper.succeedWhen(() -> {
            ItemStack out = reactor.getFuelInventory().getStackInSlot(ReactorBlockEntity.FUEL_INPUT_SLOTS);
            helper.assertTrue(out.is(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()), "spent fuel should come out, got " + out);
            SpentFuelData data = SpentFuel.data(out);
            helper.assertTrue(data != null, "discharged fuel should carry its cooling history");
            if (data != null) {
                helper.assertTrue(data.grade() == CoolingGrade.HOT, "fresh out of the core it is Hot, got " + data.grade());
                helper.assertTrue(data.dischargeDay() == SpentFuel.day(helper.getLevel().getGameTime()),
                        "dated today, got day " + data.dischargeDay());
            }
            helper.assertTrue(UraniumItem.getEnrichment(out) == 5f, "the tier is kept");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void sameDayAssembliesStack(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        ItemStack first = new ItemStack(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get());
        ItemStack second = new ItemStack(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get());
        SpentFuelHooks.onDischarge(first, helper.getLevel());
        SpentFuelHooks.onDischarge(second, helper.getLevel());
        helper.assertTrue(SpentFuel.gradeOf(first) == CoolingGrade.HOT, "spent MOX is stamped Hot too");
        helper.assertTrue(ItemStack.isSameItemSameComponents(first, second), "assemblies discharged the same day stack");
        ItemStack older = new ItemStack(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get());
        older.set(SpentFuelRegistry.SPENT_FUEL.get(), new SpentFuelData(SpentFuel.day(now) - 1, CoolingGrade.HOT));
        helper.assertTrue(!ItemStack.isSameItemSameComponents(first, older), "another day's assemblies do not");
        ItemStack uranium = new ItemStack(AllNuclearItems.URANIUM.get());
        SpentFuelHooks.onDischarge(uranium, helper.getLevel());
        helper.assertTrue(SpentFuel.data(uranium) == null, "only spent assemblies get a history");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void gradesCatchUpWithTime(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        ItemStack old = dated(SpentFuel.day(now) - 30, CoolingGrade.HOT);
        helper.assertTrue(SpentFuel.refresh(old, now) && SpentFuel.gradeOf(old) == CoolingGrade.AGED,
                "a month after discharge it is Aged, got " + SpentFuel.gradeOf(old));
        ItemStack week = dated(SpentFuel.day(now) - 10, CoolingGrade.HOT);
        SpentFuel.refresh(week, now);
        helper.assertTrue(SpentFuel.gradeOf(week) == CoolingGrade.COOLED, "ten days on it is Cooled, got " + SpentFuel.gradeOf(week));

        ItemStack legacy = new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get());
        helper.assertTrue(SpentFuel.gradeOf(legacy) == CoolingGrade.WARM, "fuel from before cooling counts as Warm");
        SpentFuel.refresh(legacy, now);
        helper.assertTrue(SpentFuel.data(legacy) != null && SpentFuel.gradeOf(legacy) == CoolingGrade.WARM,
                "and gets a history that keeps it Warm today");

        ItemStack example = new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get());
        example.set(SpentFuelRegistry.SPENT_FUEL.get(), SpentFuelData.undated(CoolingGrade.COOLED));
        SpentFuel.refresh(example, now);
        SpentFuelData exampleData = SpentFuel.data(example);
        helper.assertTrue(exampleData != null && exampleData.dated() && exampleData.grade() == CoolingGrade.COOLED,
                "an undated example from a recipe viewer is dated back to the start of its grade, got " + exampleData);

        ItemStack aged = dated(SpentFuel.day(now), CoolingGrade.AGED);
        SpentFuel.refresh(aged, now);
        helper.assertTrue(SpentFuel.gradeOf(aged) == CoolingGrade.AGED, "grades never go back");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(dated(SpentFuel.day(now) - 30, CoolingGrade.HOT));
        SpentFuelHooks.refreshInventory(player.getInventory(), now);
        helper.assertTrue(SpentFuel.gradeOf(player.getInventory().getItem(0)) == CoolingGrade.AGED, "carried fuel cools too");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void hotFuelIsHarsherThanCooledFuel(GameTestHelper helper) {
        long today = SpentFuel.day(helper.getLevel().getGameTime());
        double hot = RadiationHandler.stackRate(dated(today, CoolingGrade.HOT));
        double legacy = RadiationHandler.stackRate(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()));
        double cooled = RadiationHandler.stackRate(dated(today, CoolingGrade.COOLED));
        double aged = RadiationHandler.stackRate(dated(today, CoolingGrade.AGED));
        helper.assertTrue(Math.abs(legacy - 4.0) < 1e-6, "fuel without a history keeps its old 4/s, got " + legacy);
        helper.assertTrue(hot > 2.5 * cooled, "fresh fuel is about three times the dose of cooled fuel, " + hot + " vs " + cooled);
        helper.assertTrue(cooled > aged, "aged fuel is the mildest, " + cooled + " vs " + aged);

        Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
        bare.getInventory().add(dated(today, CoolingGrade.HOT));
        helper.assertTrue(RadiationHandler.inventoryHeat(bare) > 0, "Hot fuel gives off decay heat");
        float before = bare.getHealth();
        RadiationHandler.update(bare);
        helper.assertTrue(bare.getHealth() < before, "Hot fuel burns a bare hand like plutonium");

        Player careful = helper.makeMockPlayer(GameType.SURVIVAL);
        careful.getInventory().add(dated(today, CoolingGrade.COOLED));
        helper.assertTrue(RadiationHandler.inventoryHeat(careful) == 0, "Cooled fuel no longer burns");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the pool

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void submergedRackShieldsBetterThanUncovered(GameTestHelper helper) {
        BlockPos submergedPos = new BlockPos(3, 2, 3);
        BlockPos uncoveredPos = new BlockPos(9, 2, 9);
        pool(helper, submergedPos, 3);
        helper.setBlock(uncoveredPos.below(), Blocks.STONE);
        helper.setBlock(uncoveredPos, SpentFuelRegistry.SPENT_FUEL_RACK.get().defaultBlockState());
        SpentFuelRackBlockEntity submerged = helper.getBlockEntity(submergedPos);
        SpentFuelRackBlockEntity uncovered = helper.getBlockEntity(uncoveredPos);
        long today = SpentFuel.day(helper.getLevel().getGameTime());

        IItemHandler hopperSide = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(submergedPos), Direction.UP);
        helper.assertTrue(hopperSide != null, "hoppers and funnels reach the rack");
        if (hopperSide != null) {
            ItemStack rest = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(hopperSide, dated(today, CoolingGrade.HOT, 2), false);
            helper.assertTrue(rest.isEmpty(), "a hopper loads assemblies one per cell, left " + rest);
        }
        uncovered.insert(dated(today, CoolingGrade.HOT, 2));
        submerged.update();
        uncovered.update();

        helper.assertTrue(submerged.getWaterDepth() == 3 && uncovered.getWaterDepth() == 0,
                "depths " + submerged.getWaterDepth() + " and " + uncovered.getWaterDepth());
        helper.assertTrue(submerged.getBareEmission() == uncovered.getBareEmission() && uncovered.getBareEmission() > 0,
                "both hold the same fuel");
        helper.assertTrue(submerged.radiationStrength() < 0.01 * uncovered.radiationStrength(),
                "three blocks of water stop over 99%, " + submerged.radiationStrength() + " vs " + uncovered.radiationStrength());
        Vec3 aboveSubmerged = helper.absoluteVec(Vec3.atCenterOf(submergedPos.above(5)));
        Vec3 aboveUncovered = helper.absoluteVec(Vec3.atCenterOf(uncoveredPos.above(5)));
        double swimmer = RadiationHandler.pointExposure(helper.getLevel(), submerged.radiationOrigin(), submerged.radiationStrength(), aboveSubmerged, null);
        double exposed = RadiationHandler.pointExposure(helper.getLevel(), uncovered.radiationOrigin(), uncovered.radiationStrength(), aboveUncovered, null);
        helper.assertTrue(swimmer < 0.05 * exposed, "safe over the pool, deadly over the bare rack: " + swimmer + " vs " + exposed);
        helper.assertTrue(helper.getBlockState(submergedPos).getValue(SpentFuelRackBlock.LOAD) == RackLoad.GLOWING,
                "fresh fuel under water glows");
        helper.assertTrue(helper.getBlockState(uncoveredPos).getValue(SpentFuelRackBlock.LOAD) == RackLoad.LOADED,
                "out of the water it does not");
        discard(helper, submergedPos);
        discard(helper, uncoveredPos);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void undersizedPoolBoilsOffFromTheTop(GameTestHelper helper) {
        BlockPos rackPos = new BlockPos(6, 2, 6);
        pool(helper, rackPos, 3);
        SpentFuelRackBlockEntity rack = helper.getBlockEntity(rackPos);
        long today = SpentFuel.day(helper.getLevel().getGameTime());
        rack.insert(dated(today, CoolingGrade.HOT, 4));
        rack.update();
        SpentFuelRackBlockEntity.Pool pool = rack.getPool();
        helper.assertTrue(pool.sealed() && pool.volume() == 4 && pool.racks() == 1,
                "the pool is the rack's cell and three blocks of water, got " + pool);
        helper.assertTrue(rack.getDecayHeat() > rack.getCooling(), "four fresh assemblies outrun four blocks of water, "
                + rack.getDecayHeat() + " vs " + rack.getCooling() + " kW");

        rack.applyHeat(100);
        helper.assertTrue(rack.isBoiling(), "the surplus boils the pool");
        helper.assertTrue(helper.getBlockState(rackPos.above(3)).isAir(), "the top layer went first");
        helper.assertTrue(rack.getWaterDepth() == 2, "two blocks left, got " + rack.getWaterDepth());
        rack.applyHeat(100);
        rack.applyHeat(100);
        helper.assertTrue(rack.getWaterDepth() == 0, "left alone the rack ends up uncovered, depth " + rack.getWaterDepth());
        helper.assertTrue(helper.getBlockState(rackPos).getValue(SpentFuelRackBlock.WATERLOGGED), "the water in its own cell goes last");
        discard(helper, rackPos);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void poolCoolerKeepsUpWithTheHeat(GameTestHelper helper) {
        BlockPos rackPos = new BlockPos(4, 2, 6);
        BlockPos coolerPos = rackPos.east(2);
        // A 3x1 trench: the rack at one end, the cooler at the other, a water column between.
        helper.setBlock(rackPos.below(), Blocks.STONE);
        helper.setBlock(rackPos.east().below(), Blocks.STONE);
        helper.setBlock(coolerPos.below(), Blocks.STONE);
        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                helper.setBlock(rackPos.offset(dx, dy, -1), Blocks.STONE);
                helper.setBlock(rackPos.offset(dx, dy, 1), Blocks.STONE);
            }
        }
        for (int dy = 0; dy <= 2; dy++) {
            helper.setBlock(rackPos.offset(-1, dy, 0), Blocks.STONE);
            helper.setBlock(rackPos.offset(3, dy, 0), Blocks.STONE);
        }
        helper.setBlock(rackPos, SpentFuelRegistry.SPENT_FUEL_RACK.get().defaultBlockState().setValue(SpentFuelRackBlock.WATERLOGGED, true));
        helper.setBlock(rackPos.east(), Blocks.WATER);
        helper.setBlock(rackPos.above(), Blocks.WATER);
        helper.setBlock(rackPos.east().above(), Blocks.WATER);
        helper.setBlock(coolerPos, SpentFuelRegistry.POOL_COOLER.get().defaultBlockState().setValue(PoolCoolerBlock.WATERLOGGED, true));
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(coolerPos.above(), motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(coolerPos.above());
        motorEntity.generatedSpeed.setValue(64);
        SpentFuelRackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.insert(dated(SpentFuel.day(helper.getLevel().getGameTime()) - 3, CoolingGrade.WARM, 1));

        helper.succeedWhen(() -> {
            PoolCoolerBlockEntity cooler = helper.getBlockEntity(coolerPos);
            helper.assertTrue(cooler.coolingCapacity() > 0, "the cooler turns, at " + cooler.getSpeed() + " RPM");
            rack.update();
            rack.scanPool();
            rack.applyHeat(1);
            helper.assertTrue(rack.getPool().coolerCapacity() > 0, "the rack counts the cooler in its pool");
            helper.assertTrue(rack.getCooling() > rack.getDecayHeat() && !rack.isBoiling(),
                    "with the cooler the pool keeps up: " + rack.getCooling() + " vs " + rack.getDecayHeat() + " kW");
            discard(helper, rackPos);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void racksBringStoredGradesUpToDate(GameTestHelper helper) {
        BlockPos rackPos = new BlockPos(6, 2, 6);
        pool(helper, rackPos, 2);
        SpentFuelRackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.insert(dated(SpentFuel.day(helper.getLevel().getGameTime()) - 30, CoolingGrade.HOT, 1));
        rack.update();
        helper.assertTrue(SpentFuel.gradeOf(rack.getInventory().getStackInSlot(0)) == CoolingGrade.AGED,
                "a month in the pool, it comes out Aged");
        helper.assertTrue(rack.getHottest() == CoolingGrade.AGED, "the rack reports it");
        discard(helper, rackPos);
        helper.succeed();
    }

    // ------------------------------------------------------------------ the dry cask

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void dryCaskRefusesHotFuel(GameTestHelper helper) {
        BlockPos caskPos = new BlockPos(6, 2, 6);
        helper.setBlock(caskPos, SpentFuelRegistry.DRY_CASK.get());
        DryCaskBlockEntity cask = helper.getBlockEntity(caskPos);
        long today = SpentFuel.day(helper.getLevel().getGameTime());

        ItemStack hot = dated(today, CoolingGrade.HOT, 2);
        helper.assertTrue(!cask.accepts(hot), "Hot fuel is refused");
        helper.assertTrue(cask.insert(hot.copy()).getCount() == 2, "not even through a hopper");
        ItemStack warm = dated(today - 3, CoolingGrade.WARM, 1);
        helper.assertTrue(cask.insert(warm).isEmpty(), "Warm fuel goes in");
        helper.assertTrue(cask.insert(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get())).isEmpty(),
                "so does fuel from before cooling existed");
        helper.assertTrue(cask.insert(new ItemStack(AllNuclearItems.URANIUM.get())).getCount() == 1, "only spent fuel");
        cask.update();
        helper.assertTrue(cask.getLoaded() == 2, "two assemblies inside, got " + cask.getLoaded());
        helper.assertTrue(cask.radiationStrength() <= 0.05 * cask.getBareEmission() && cask.radiationStrength() > 0,
                "the cask lets almost nothing out: " + cask.radiationStrength() + " of " + cask.getBareEmission());
        discard(helper, caskPos);
        helper.succeed();
    }

    // ------------------------------------------------------------------ reprocessing

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void cooledFuelReprocessesBetter(GameTestHelper helper) {
        for (String fuel : List.of("low", "high", "mox")) {
            Item item = fuel.equals("mox") ? AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get() : AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get();
            float enrichment = fuel.equals("high") ? 20f : 5f;
            String base = "crushing/" + fuel + "_spent_fuel_fragments";
            expectRecipe(helper, spent(item, enrichment, null), base);
            expectRecipe(helper, spent(item, enrichment, CoolingGrade.HOT), base);
            expectRecipe(helper, spent(item, enrichment, CoolingGrade.WARM), base);
            expectRecipe(helper, spent(item, enrichment, CoolingGrade.COOLED), base + "_cooled");
            expectRecipe(helper, spent(item, enrichment, CoolingGrade.AGED), base + "_aged");

            ProcessingRecipe<?, ?> fresh = processing(helper, base);
            ProcessingRecipe<?, ?> cooled = processing(helper, base + "_cooled");
            ProcessingRecipe<?, ?> aged = processing(helper, base + "_aged");
            if (fresh == null || cooled == null || aged == null) {
                return;
            }
            Item fragments = AllNuclearItems.SPENT_FUEL_FRAGMENTS.get();
            Item waste = AllNuclearItems.RADIOACTIVE_WASTE.get();
            helper.assertTrue(expected(cooled, fragments) > expected(fresh, fragments) && expected(aged, fragments) > expected(cooled, fragments),
                    fuel + ": cooler fuel gives more fragments");
            helper.assertTrue(expected(cooled, waste) < expected(fresh, waste) && expected(aged, waste) < expected(cooled, waste),
                    fuel + ": and less waste");
        }
        helper.succeed();
    }

    /** Exactly one crushing recipe takes {@code stack}, and it is {@code path}. */
    private static void expectRecipe(GameTestHelper helper, ItemStack stack, String path) {
        ItemStackHandler slot = new ItemStackHandler(1);
        slot.setStackInSlot(0, stack);
        RecipeWrapper input = new RecipeWrapper(slot);
        List<ResourceLocation> matching = helper.getLevel().getRecipeManager().getAllRecipesFor(AllRecipeTypes.CRUSHING.<RecipeWrapper, Recipe<RecipeWrapper>>getType())
                .stream()
                .filter(holder -> holder.value().matches(input, helper.getLevel()))
                .map(RecipeHolder::id)
                .toList();
        helper.assertTrue(matching.equals(List.of(id(path))), SpentFuel.gradeOf(stack) + " " + stack + " should only match " + path + ", matched " + matching);
    }

    private static double expected(ProcessingRecipe<?, ?> recipe, Item item) {
        double total = 0;
        for (ProcessingOutput output : recipe.getRollableResults()) {
            if (output.getStack().is(item)) {
                total += output.getChance() * output.getStack().getCount();
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ helpers

    private static ItemStack dated(long day, CoolingGrade grade) {
        return dated(day, grade, 1);
    }

    private static ItemStack dated(long day, CoolingGrade grade, int count) {
        ItemStack stack = new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get(), count);
        UraniumItem.setEnrichment(stack, 5f);
        stack.set(SpentFuelRegistry.SPENT_FUEL.get(), new SpentFuelData(day, grade));
        return stack;
    }

    private static ItemStack spent(Item item, float enrichment, CoolingGrade grade) {
        ItemStack stack = new ItemStack(item);
        if (item == AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()) {
            UraniumItem.setEnrichment(stack, enrichment);
        }
        if (grade != null) {
            stack.set(SpentFuelRegistry.SPENT_FUEL.get(), SpentFuelData.undated(grade));
        }
        return stack;
    }

    /** A 1x1 stone shaft with a waterlogged rack at the bottom and {@code depth} water source blocks above it. */
    private static void pool(GameTestHelper helper, BlockPos rack, int depth) {
        helper.setBlock(rack.below(), Blocks.STONE);
        for (int dy = 0; dy <= depth; dy++) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                helper.setBlock(rack.above(dy).relative(side), Blocks.STONE);
            }
        }
        helper.setBlock(rack, SpentFuelRegistry.SPENT_FUEL_RACK.get().defaultBlockState().setValue(SpentFuelRackBlock.WATERLOGGED, true));
        for (int dy = 1; dy <= depth; dy++) {
            helper.setBlock(rack.above(dy), Blocks.WATER);
        }
    }

    /** Empties a store and removes it, so no fuel is left to drop or irradiate the next tests. */
    private static void discard(GameTestHelper helper, BlockPos pos) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof SpentFuelStorageBlockEntity store) {
            for (int slot = 0; slot < SpentFuelStorageBlockEntity.SLOTS; slot++) {
                store.getInventory().setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        helper.setBlock(pos, Blocks.AIR);
    }

    private static ProcessingRecipe<?, ?> processing(GameTestHelper helper, String path) {
        var holder = helper.getLevel().getRecipeManager().byKey(id(path));
        helper.assertTrue(holder.isPresent(), "missing recipe " + path);
        return holder.map(h -> (ProcessingRecipe<?, ?>) h.value()).orElse(null);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}

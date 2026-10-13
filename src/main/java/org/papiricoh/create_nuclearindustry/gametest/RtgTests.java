package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.rtg.RtgBlockEntity;
import org.papiricoh.create_nuclearindustry.rtg.RtgFuel;

import java.util.List;

/** The RTG: a steady 16 RPM from a decaying pellet, and a source to shield. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RtgTests {
    private static final BlockPos RTG = new BlockPos(6, 3, 6);

    private static RtgBlockEntity rtg(GameTestHelper helper) {
        helper.setBlock(RTG, AllNuclearBlocks.RTG.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        return helper.getBlockEntity(RTG);
    }

    private static void load(RtgBlockEntity rtg, ItemStack pellet) {
        rtg.getInventory().insertItem(RtgBlockEntity.INPUT_SLOT, pellet, false);
        rtg.refuel();
        rtg.refreshGeneration();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plutoniumPelletSpinsAtSixteenRpm(GameTestHelper helper) {
        RtgBlockEntity rtg = rtg(helper);
        load(rtg, new ItemStack(AllNuclearItems.PLUTONIUM_RTG_PELLET.get()));
        helper.assertTrue(Math.abs(rtg.getGeneratedSpeed()) == RtgBlockEntity.RPM, "16 RPM, got " + rtg.getGeneratedSpeed());
        helper.assertTrue(rtg.calculateAddedStressCapacity() == 256f, "256 per RPM fresh, got " + rtg.calculateAddedStressCapacity());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void emptyRtgIsStill(GameTestHelper helper) {
        RtgBlockEntity rtg = rtg(helper);
        rtg.refuel();
        rtg.refreshGeneration();
        helper.assertTrue(rtg.getGeneratedSpeed() == 0 && rtg.radiationStrength() == 0, "an empty RTG does nothing");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wastePelletIsWeaker(GameTestHelper helper) {
        RtgBlockEntity rtg = rtg(helper);
        load(rtg, new ItemStack(AllNuclearItems.WASTE_RTG_PELLET.get()));
        helper.assertTrue(rtg.calculateAddedStressCapacity() == 64f, "a waste pellet gives 64 per RPM, got " + rtg.calculateAddedStressCapacity());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void chargeHalvesEveryHalfLife(GameTestHelper helper) {
        for (RtgFuel fuel : RtgFuel.values()) {
            double half = fuel.decay(1.0, fuel.halfLifeTicks());
            helper.assertTrue(Math.abs(half - 0.5) < 1e-9, fuel.id() + " should halve in one half-life, got " + half);
        }
        helper.assertTrue(RtgFuel.PLUTONIUM.halfLifeTicks() > RtgFuel.WASTE.halfLifeTicks(), "plutonium lasts longer than waste");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void exhaustedPelletComesOutSpent(GameTestHelper helper) {
        RtgBlockEntity rtg = rtg(helper);
        ItemStack tired = new ItemStack(AllNuclearItems.PLUTONIUM_RTG_PELLET.get());
        tired.set(AllNuclearDataComponents.RTG_CHARGE.get(), 0.26f);
        load(rtg, tired);
        helper.assertTrue(Math.abs(rtg.calculateAddedStressCapacity() - 256 * 0.26f) < 0.5f, "capacity follows the charge");
        rtg.advanceDecay(RtgFuel.PLUTONIUM.halfLifeTicks());
        rtg.refuel();
        rtg.refreshGeneration();
        helper.assertTrue(rtg.getInventory().getStackInSlot(RtgBlockEntity.OUTPUT_SLOT).is(AllNuclearItems.DEPLETED_RTG_PELLET.get()),
                "the spent pellet should come out");
        helper.assertTrue(rtg.getFuel() == null && rtg.getGeneratedSpeed() == 0, "and the RTG stops with nothing else to burn");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void automationOnlyTakesSpentPellets(GameTestHelper helper) {
        RtgBlockEntity rtg = rtg(helper);
        var handler = rtg.getAutomationHandler();
        ItemStack fresh = new ItemStack(AllNuclearItems.PLUTONIUM_RTG_PELLET.get(), 2);
        helper.assertTrue(handler.insertItem(RtgBlockEntity.INPUT_SLOT, fresh, false).isEmpty(), "pellets go in");
        helper.assertTrue(handler.extractItem(RtgBlockEntity.INPUT_SLOT, 1, false).isEmpty(), "fresh pellets cannot be pulled out");
        ItemStack spent = new ItemStack(AllNuclearItems.DEPLETED_RTG_PELLET.get());
        helper.assertFalse(handler.insertItem(RtgBlockEntity.OUTPUT_SLOT, spent, false).isEmpty(), "nothing goes into the spent slot");
        rtg.getInventory().setStackInSlot(RtgBlockEntity.OUTPUT_SLOT, spent);
        helper.assertTrue(handler.extractItem(RtgBlockEntity.OUTPUT_SLOT, 1, false).is(AllNuclearItems.DEPLETED_RTG_PELLET.get()),
                "spent pellets come out");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void depletedUraniumShieldsAnRtg(GameTestHelper helper) {
        RtgBlockEntity rtg = rtg(helper);
        load(rtg, new ItemStack(AllNuclearItems.PLUTONIUM_RTG_PELLET.get()));
        Vec3 body = Vec3.atCenterOf(helper.absolutePos(RTG.west(2)));
        double open = RadiationHandler.pointExposure(helper.getLevel(), rtg.radiationOrigin(), rtg.radiationStrength(), body, null);
        helper.assertTrue(open > 1.0, "an unshielded plutonium RTG two blocks away is dangerous, got " + open);
        helper.setBlock(RTG.west(), AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get());
        double shielded = RadiationHandler.pointExposure(helper.getLevel(), rtg.radiationOrigin(), rtg.radiationStrength(), body, null);
        helper.assertTrue(shielded <= 0.1 * open + 1e-6, "a depleted uranium block cuts it to a tenth, " + open + " -> " + shielded);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rtgRecipesLoaded(GameTestHelper helper) {
        for (String path : List.of("crafting/rtg", "compacting/plutonium_rtg_pellet", "compacting/waste_rtg_pellet",
                "crushing/depleted_rtg_pellet")) {
            helper.assertTrue(helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path)).isPresent(), "missing " + path);
        }
        helper.succeed();
    }
}

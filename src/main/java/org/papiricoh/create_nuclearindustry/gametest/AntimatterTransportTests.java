package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationBlastModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.VialBattery;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterAnnihilation;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.conduit.AntimatterConduitBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.conduit.ConduitNetwork;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.trap.VialPortMode;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;

/**
 * Moving antimatter: the vial and its battery (filled and recharged in a trap's port, safe empty,
 * annihilating where it lies once its battery is flat with a charge), the conduit (nothing moves
 * until every segment has its energy, then traps share their charge without losing any), and the
 * annihilation itself, which leaves no fallout or contamination behind.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AntimatterTransportTests {
    private static final BlockPos TRAP = AntimatterTestBuilder.MIDDLE;

    // ------------------------------------------------------------------ the vial

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void emptyVialsStackAndLoadedOnesDoNot(GameTestHelper helper) {
        AntimatterVialItem item = AntimatterRegistry.ANTIMATTER_VIAL.get();
        ItemStack empty = new ItemStack(item);
        helper.assertTrue(empty.getMaxStackSize() == AntimatterVialItem.EMPTY_STACK, "empty vials stack to 16");
        helper.assertFalse(AntimatterVialItem.loaded(empty), "and are harmless");
        ItemStack loaded = item.filled(2.0);
        helper.assertTrue(loaded.getMaxStackSize() == 1, "a loaded vial does not stack");
        helper.assertTrue(AntimatterVialItem.antimatter(loaded) == 2.0, "it holds its charge");
        helper.assertTrue(AntimatterVialItem.charge(loaded) == AntimatterConfig.vial().batteryTicks(), "with a full battery");
        helper.assertTrue(AntimatterVialItem.antimatter(item.filled(100)) == AntimatterConfig.vial().capacity(), "never more than 4 ng");
        AntimatterVialItem.set(loaded, 0, 0);
        helper.assertTrue(ItemStack.isSameItemSameComponents(loaded, empty) && loaded.getMaxStackSize() == AntimatterVialItem.EMPTY_STACK,
                "emptied, it stacks with new vials again");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aTrapsPortFillsAndRechargesAVialThenTakesItBack(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        VialBattery.Params vial = AntimatterConfig.vial();
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null && trap.powered(), "a fed trap");
            trap.fill(10);
            ItemStack low = AntimatterRegistry.ANTIMATTER_VIAL.get().filled(1.0);
            AntimatterVialItem.set(low, 1.0, 40);
            helper.assertTrue(trap.port().insertItem(0, low, false).isEmpty(), "a vial with a nearly flat battery goes into the port");
            helper.assertTrue(trap.port().extractItem(0, 1, true).isEmpty(), "automation cannot take it back half full");
        });
        helper.succeedWhen(() -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null, "the trap should be there");
            ItemStack inPort = trap.vial();
            helper.assertTrue(Math.abs(AntimatterVialItem.antimatter(inPort) - vial.capacity()) < 1e-6,
                    "waiting for the vial to fill: " + AntimatterVialItem.antimatter(inPort));
            helper.assertTrue(AntimatterVialItem.charge(inPort) == vial.batteryTicks(), "its battery recharged: " + AntimatterVialItem.charge(inPort));
            helper.assertTrue(Math.abs(trap.stored() - 7.0) < 1e-6, "the trap gave what the vial took: " + trap.stored());
            ItemStack out = trap.port().extractItem(0, 1, false);
            helper.assertTrue(AntimatterVialItem.loaded(out) && trap.vial().isEmpty(), "full, automation may take it out");
            trap.setStored(0);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aTrapsPortEmptiesAVialIntoTheTrap(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, TRAP);
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null && trap.powered(), "a fed trap");
            trap.setMode(VialPortMode.DRAIN);
            helper.assertTrue(trap.port().insertItem(0, AntimatterRegistry.ANTIMATTER_VIAL.get().filled(3.0), false).isEmpty(),
                    "a loaded vial goes into the port");
        });
        helper.succeedWhen(() -> {
            PenningTrapBlockEntity trap = AntimatterTestBuilder.trap(helper, TRAP);
            helper.assertTrue(trap != null, "the trap should be there");
            helper.assertTrue(Math.abs(trap.stored() - 3.0) < 1e-6, "waiting for the vial to empty: " + trap.stored());
            helper.assertFalse(AntimatterVialItem.loaded(trap.vial()), "the vial is empty");
            ItemStack out = trap.port().extractItem(0, 1, false);
            helper.assertTrue(out.is(AntimatterRegistry.ANTIMATTER_VIAL.get()) && out.getMaxStackSize() == AntimatterVialItem.EMPTY_STACK,
                    "and comes out as a plain empty vial");
            trap.setStored(0);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = PenningTrapTests.BLASTS, timeoutTicks = 80)
    public static void aLoadedVialOnTheGroundAnnihilatesWhenItsBatteryRunsOut(GameTestHelper helper) {
        AntimatterTestBuilder.listen();
        ItemStack stack = AntimatterRegistry.ANTIMATTER_VIAL.get().filled(1.0);
        AntimatterVialItem.set(stack, 1.0, 20);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(TRAP));
        ItemEntity dropped = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack);
        dropped.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(dropped);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(dropped.isAlive(), "the vial lies there");
            int charge = AntimatterVialItem.charge(dropped.getItem());
            helper.assertTrue(charge > 0 && charge <= 12, "its battery running down: " + charge);
        });
        helper.succeedWhen(() -> {
            helper.assertFalse(dropped.isAlive(), "waiting for the battery to go flat");
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, TRAP, 2.5) == 1, "the vial annihilated where it lay");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 60)
    public static void anEmptyVialOnTheGroundIsSafe(GameTestHelper helper) {
        AntimatterTestBuilder.listen();
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(TRAP));
        ItemEntity dropped = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(AntimatterRegistry.ANTIMATTER_VIAL.get()));
        dropped.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(dropped);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(dropped.isAlive(), "an empty vial just lies there");
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, TRAP, 2.5) == 0, "no blast");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ the conduit

    private static final BlockPos FROM = new BlockPos(3, 2, 6);
    private static final BlockPos TO = new BlockPos(7, 2, 6);
    /** Above the middle segment, once the test powers the conduit. */
    private static final BlockPos CONDUIT_FEED = new BlockPos(5, 3, 6);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void theConduitMovesNothingUnpoweredAndSharesTheChargeOncePowered(GameTestHelper helper) {
        AntimatterTestBuilder.fedTrap(helper, FROM);
        AntimatterTestBuilder.fedTrap(helper, TO);
        for (int x = FROM.getX() + 1; x < TO.getX(); x++) {
            helper.setBlock(new BlockPos(x, 2, 6), AntimatterRegistry.ANTIMATTER_CONDUIT.get());
        }
        helper.runAfterDelay(5, () -> {
            PenningTrapBlockEntity from = AntimatterTestBuilder.trap(helper, FROM);
            helper.assertTrue(from != null && from.powered(), "a fed trap at one end");
            from.fill(10);
        });
        helper.runAfterDelay(30, () -> {
            PenningTrapBlockEntity from = AntimatterTestBuilder.trap(helper, FROM);
            PenningTrapBlockEntity to = AntimatterTestBuilder.trap(helper, TO);
            AntimatterConduitBlockEntity conduit = AcceleratorTestBuilder.entity(helper, new BlockPos(5, 2, 6), AntimatterConduitBlockEntity.class);
            helper.assertTrue(from != null && to != null && conduit != null, "the traps and the conduit should be there");
            ConduitNetwork network = conduit.network();
            helper.assertTrue(network != null && network.size() == 3, "three segments: " + (network == null ? 0 : network.size()));
            helper.assertTrue(network.holderPositions().size() == 2, "joining the two traps");
            helper.assertFalse(network.powered(), "with no energy");
            helper.assertTrue(from.stored() == 10 && to.stored() == 0, "it moves nothing: " + from.stored() + " / " + to.stored());
            AntimatterTestBuilder.generator(helper, CONDUIT_FEED);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 31, "waiting for the conduit's energy");
            PenningTrapBlockEntity from = AntimatterTestBuilder.trap(helper, FROM);
            PenningTrapBlockEntity to = AntimatterTestBuilder.trap(helper, TO);
            AntimatterConduitBlockEntity conduit = AcceleratorTestBuilder.entity(helper, new BlockPos(5, 2, 6), AntimatterConduitBlockEntity.class);
            helper.assertTrue(from != null && to != null && conduit != null, "the traps and the conduit should be there");
            helper.assertTrue(conduit.powered(), "powered, the conduit runs");
            helper.assertTrue(Math.abs(from.stored() + to.stored() - 10) < 1e-6, "nothing is lost on the way: " + (from.stored() + to.stored()));
            helper.assertTrue(Math.abs(from.stored() - to.stored()) < 0.01, "the traps even out: " + from.stored() + " / " + to.stored());
            from.setStored(0);
            to.setStored(0);
        });
    }

    // ------------------------------------------------------------------ no fallout

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_antimatter", timeoutTicks = 40)
    public static void anAnnihilationLeavesNoContamination(GameTestHelper helper) {
        AntimatterTestBuilder.listen();
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(TRAP);
        ChunkPos origin = new ChunkPos(centre);
        FalloutData fallout = FalloutData.get(level);
        ContaminationData data = ContaminationData.get(level);
        int sources = fallout.sources().size();
        int events = data.events().size();
        try {
            ContaminationTests.loadAround(level, origin, 2);
            ContaminationTests.clean(helper, 2);
            double ng = 2.0;
            helper.assertTrue(AnnihilationBlastModel.explodes(AntimatterConfig.blast(), ng), "2 ng is a real blast");
            AntimatterAnnihilation.explode(level, centre.getCenter(), ng);
            helper.assertTrue(AntimatterTestBuilder.blastsNear(helper, TRAP, 1) == 1, "the annihilation went off as an antimatter detonation");
            helper.assertTrue(fallout.sources().size() == sources, "it leaves no hot spot");
            helper.assertTrue(data.events().size() == events, "no fallout event");
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.assertTrue(Contamination.get(level, new ChunkPos(origin.x + dx, origin.z + dz)) == 0,
                            "and no contamination in the chunks around it");
                }
            }
        } finally {
            ContaminationTests.clean(helper, 2);
        }
        helper.succeed();
    }
}

package org.papiricoh.create_nuclearindustry.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.neoforge.gametest.*;
import org.papiricoh.create_nuclearindustry.*;
import org.papiricoh.create_nuclearindustry.missile.silo.*;
import java.util.UUID;

/** Controller slots proxy a section in the contiguous column and retain no inventory of their own. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class LaunchControllerMenuTests {
    private static final String BATCH = "launch_controller_menu";
    private static FakePlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "launch_menu_test"));
        BlockPos at = helper.absolutePos(SiloTestBuilder.CONTROLLER);
        player.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() - 1.5);
        return player;
    }
    private static SiloMenu menu(GameTestHelper helper, FakePlayer player) {
        return new SiloMenu(1, player.getInventory(), helper.absolutePos(SiloTestBuilder.CONTROLLER));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void menuSelectsSectionAndLoadsWarhead(GameTestHelper helper) {
        new SiloTestBuilder().build(helper);
        var player = player(helper);
        var menu = menu(helper, player);
        helper.assertTrue(menu.slots.size() == SiloMenu.INV_END, "ten proxy slots followed by player inventory");
        helper.assertTrue(menu.select(3) && menu.selectedSection() == RocketComposition.Section.WARHEAD, "warhead selected");
        player.getInventory().setItem(9, new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get(), 4));
        menu.quickMoveStack(player, SiloMenu.INV_START);
        RocketSectionEntity warhead = helper.getBlockEntity(SiloTestBuilder.FLOOR.above(4));
        helper.assertTrue(warhead.contents().getStackInSlot(0).getCount() == 2
                && warhead.contents().getStackInSlot(1).getCount() == 2, "shift click respects per-slot core caps");
        helper.assertTrue(menu.getSlot(0).getItem() == warhead.contents().getStackInSlot(0), "proxy reads live inventory");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(warhead.contents().getStackInSlot(0).isEmpty(), "proxy extracts into player inventory");
        helper.assertTrue(menu.select(1) && menu.selectedSection() == RocketComposition.Section.TANK, "selection switches handler");
        helper.assertTrue(menu.getSlot(0).getItem().is(SiloRegistry.PROPELLANT.get()) && !menu.getSlot(1).isActive(), "tank exposes only cartridge slot");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void menuRefusesSectionOutsideColumn(GameTestHelper helper) {
        new SiloTestBuilder().build(helper);
        var menu = menu(helper, player(helper));
        helper.setBlock(SiloTestBuilder.FLOOR.above(7), SiloRegistry.WARHEAD.get());
        helper.assertTrue(!menu.select(6) && !menu.select(SiloControllerEntity.MAX_ROCKET), "floating and out-of-range sections refused");
        helper.assertTrue(menu.selected() == -1, "no selection left behind");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void menuDropsSelectionWhenSectionBroken(GameTestHelper helper) {
        new SiloTestBuilder().build(helper);
        var menu = menu(helper, player(helper));
        helper.assertTrue(menu.select(3), "selected warhead");
        helper.setBlock(SiloTestBuilder.FLOOR.above(4), Blocks.AIR);
        helper.setBlock(SiloTestBuilder.FLOOR.above(4), SiloRegistry.WARHEAD.get());
        menu.broadcastChanges();
        helper.assertTrue(menu.selected() == -1 && !menu.getSlot(0).isActive(), "a replacement has a different identity");
        helper.assertTrue(menu.select(3), "replacement can be explicitly selected");
        helper.setBlock(SiloTestBuilder.FLOOR.above(2), Blocks.AIR);
        menu.broadcastChanges();
        helper.assertTrue(menu.selected() == -1, "breaking below selection also invalidates it");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void menuLocksWhileArmed(GameTestHelper helper) {
        var controller = new SiloTestBuilder().tanks(2).build(helper);
        var player = player(helper);
        var menu = menu(helper, player);
        helper.assertTrue(menu.select(4), "selected warhead");
        controller.configure(player, helper.absolutePos(SiloTestBuilder.FLOOR).offset(150, 0, 0), false);
        helper.runAfterDelay(10, () -> {
            controller.arm(player);
            helper.assertTrue(controller.state() == SiloControllerEntity.State.ARMED, "controller armed: " + controller.error());
            helper.assertTrue(menu.locked(), "menu locks immediately");
            helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.TNT)) && !menu.getSlot(0).mayPickup(player), "both directions locked");
            player.getInventory().setItem(9, new ItemStack(Items.TNT));
            helper.assertTrue(menu.quickMoveStack(player, SiloMenu.INV_START).isEmpty(), "shift click refused");
            controller.disarm();
            helper.assertTrue(!menu.locked() && menu.getSlot(0).mayPlace(new ItemStack(Items.TNT)), "disarm unlocks immediately");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void diagramPinsChargeErrorToWarhead(GameTestHelper helper) {
        var controller = new SiloTestBuilder().build(helper);
        RocketSectionEntity warhead = helper.getBlockEntity(SiloTestBuilder.FLOOR.above(4));
        warhead.contents().setStackInSlot(0, new ItemStack(Items.TNT));
        warhead.contents().setStackInSlot(1, new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get()));
        controller.validate();
        var diagram = LaunchDiagrams.build(controller);
        helper.assertTrue(diagram.error().equals("charge_mixed_conventional"), "specific charge error");
        helper.assertTrue(diagram.errorWhere() == LaunchDiagram.WHERE_CELL && diagram.errorCell() == 3, "error pinned to warhead");
        helper.assertTrue(diagram.column().get(3).has(LaunchDiagram.Cell.ERROR), "warhead cell blinks");
        warhead.contents().setStackInSlot(1, ItemStack.EMPTY);
        controller.validate();
        var loaded = LaunchDiagrams.build(controller);
        helper.assertTrue(loaded.stages().size() == 1 && loaded.stages().getFirst().fill() == 100, "fueled stage appears in the table");
        helper.assertTrue(loaded.stages().getFirst().twr() > 0 && loaded.stages().getFirst().deltaV() > 0, "performance includes payload mass");
        helper.assertTrue(loaded.warhead().kind() == org.papiricoh.create_nuclearindustry.missile.flight.Warhead.Kind.CONVENTIONAL.ordinal()
                && loaded.warhead().fallout() == 0, "conventional diagram has no fallout");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            loaded.write(buffer);
            helper.assertTrue(LaunchDiagram.read(buffer).equals(loaded), "diagram wire format round trips");
        } finally { buffer.release(); }
        helper.succeed();
    }
}

package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.*;
import org.papiricoh.create_nuclearindustry.explosive.*;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.*;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.silo.*;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.*;

import java.util.*;

/** Physical loads, bomb item round trips, and the five-slot save migration. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class WarheadLoadingTests {
    private static final String BATCH = "warhead_loading";
    private static final BlockPos AT = new BlockPos(5, 2, 5);
    private static ItemStack core(int count) { return new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get(), count); }
    private static ItemStack lithium(int count) { return new ItemStack(ExplosiveRegistry.LITHIUM_DEUTERIDE.get(), count); }
    private static RocketSectionEntity bay(GameTestHelper helper) {
        helper.setBlock(AT, SiloRegistry.WARHEAD.get());
        return helper.getBlockEntity(AT);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void warheadBayCapsMatchBombs(GameTestHelper helper) {
        var bay = bay(helper);
        int cores = 0, secondary = 0, tamper = 0;
        for (int slot : WarheadBay.CHARGE) {
            ItemStack rest = bay.contents().insertItem(slot, core(16), false);
            cores += bay.contents().getStackInSlot(slot).getCount();
            helper.assertTrue(rest.getCount() == 16 - NuclearBombBlockEntity.CORES_PER_SLOT, "same core slot cap as a bomb");
        }
        for (int slot : WarheadBay.SECONDARY) {
            bay.contents().insertItem(slot, lithium(64), false);
            secondary += bay.contents().getStackInSlot(slot).getCount();
        }
        for (int slot : WarheadBay.TAMPER) {
            bay.contents().insertItem(slot, new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get(), 64), false);
            tamper += bay.contents().getStackInSlot(slot).getCount();
        }
        helper.assertTrue(cores == NuclearBombBlockEntity.MAX_CORES, "six cores total");
        helper.assertTrue(secondary == ThermonuclearStats.SECONDARY_FULL && tamper == ThermonuclearStats.TAMPER_FULL, "same full secondary and tamper as a bomb");
        helper.assertTrue(bay.contents().insertItem(WarheadBay.BOMB, new ItemStack(AllNuclearItems.NUCLEAR_BOMB.get(), 2), false).getCount() == 1, "one whole bomb");
        helper.setBlock(AT.east(), SiloRegistry.TANK.get());
        RocketSectionEntity tank = helper.getBlockEntity(AT.east());
        SiloTestBuilder.fill(tank);
        helper.assertTrue(!tank.contents().insertItem(0, new ItemStack(SiloRegistry.PROPELLANT.get()), true).isEmpty(), "liquid tank refuses a cartridge");
        helper.assertTrue(!WarheadBay.accepts(RocketComposition.Section.GUIDANCE, 0, new ItemStack(Items.DIRT))
                && WarheadBay.accepts(RocketComposition.Section.GUIDANCE, 0, new ItemStack(SiloRegistry.STAR_TRACKER.get())), "guidance takes only an upgrade");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void tntMakesConventionalWarhead(GameTestHelper helper) {
        var bay = bay(helper);
        bay.contents().insertItem(0, new ItemStack(Items.TNT, 8), false);
        var warhead = bay.warhead();
        helper.assertTrue(bay.validCharge() && warhead.kind() == Warhead.Kind.CONVENTIONAL, "TNT makes a conventional warhead");
        helper.assertTrue(warhead.power() == 8 && warhead.threatRadius() == 12 && !warhead.nuclear(), "cube-root TNT power without radiation");
        helper.assertTrue(Warhead.load(warhead.save()).equals(warhead), "conventional kind survives saves");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_conventional")
    public static void conventionalDetonationLeavesNoContamination(GameTestHelper helper) {
        var level = helper.getLevel();
        var data = ContaminationData.get(level);
        var fallout = FalloutData.get(level);
        int events = data.events().size(), sources = fallout.sources().size();
        ContaminationTests.clean(helper, 1);
        try {
            Warhead.conventional(4).detonate(level, helper.absolutePos(AT).getCenter());
            helper.assertTrue(Contamination.get(level, new ChunkPos(helper.absolutePos(AT))) == 0, "TNT leaves the ground clean");
            helper.assertTrue(data.events().size() == events && fallout.sources().size() == sources, "no fallout event or source");
        } finally { ContaminationTests.clean(helper, 1); }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void mixingTntAndCoresIsRefused(GameTestHelper helper) {
        var bay = bay(helper);
        bay.contents().setStackInSlot(0, new ItemStack(Items.TNT));
        bay.contents().setStackInSlot(1, core(1));
        helper.assertTrue("charge_mixed_conventional".equals(WarheadBay.validate(bay.contents())), "mixed conventional load is invalid");
        helper.assertTrue(bay.warhead().equals(Warhead.INERT), "invalid loads never produce a usable warhead");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void thermonuclearNeedsExactlyOnePrimary(GameTestHelper helper) {
        var bay = bay(helper);
        bay.contents().setStackInSlot(3, lithium(4));
        helper.assertTrue("charge_thermo_primary".equals(WarheadBay.validate(bay.contents())), "no primary fails");
        bay.contents().setStackInSlot(0, core(2));
        helper.assertTrue("charge_thermo_primary".equals(WarheadBay.validate(bay.contents())), "two cores fail");
        bay.contents().setStackInSlot(0, core(1));
        helper.assertTrue(bay.validCharge() && bay.warhead().kind() == Warhead.Kind.THERMONUCLEAR, "one primary works");
        bay.contents().setStackInSlot(3, ItemStack.EMPTY);
        bay.contents().setStackInSlot(7, new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get()));
        helper.assertTrue("charge_tamper_alone".equals(WarheadBay.validate(bay.contents())), "tamper alone fails");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void brokenBombDropsItsContents(GameTestHelper helper) {
        helper.setBlock(AT, AllNuclearBlocks.NUCLEAR_BOMB.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(AT);
        bomb.getInventory().setStackInSlot(0, core(2));
        BlockPos at = helper.absolutePos(AT);
        helper.getLevel().destroyBlock(at, true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(1));
        ItemStack found = ItemStack.EMPTY;
        for (var drop : drops) if (drop.getItem().is(AllNuclearItems.NUCLEAR_BOMB.get())) found = drop.getItem();
        helper.assertTrue(!found.isEmpty(), "breaking drops a bomb item");
        var contents = found.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        helper.assertTrue(contents.getStackInSlot(0).getCount() == 2, "its cores stay in the item");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void placedBombItemRestoresContents(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(AllNuclearItems.NUCLEAR_BOMB.get());
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(core(2))));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos below = helper.absolutePos(AT.below());
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(below.getCenter().add(0, 0.5, 0), Direction.UP, below, false)));
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(AT)) instanceof NuclearBombBlockEntity, "bomb item places a bomb");
        NuclearBombBlockEntity placed = helper.getBlockEntity(AT);
        helper.assertTrue(placed.getCoreCount() == 2, "placing restores both cores");
        helper.succeed();
    }

    private static void bombInBay(GameTestHelper helper, ItemStack item, List<ItemStack> items, Warhead.Kind kind) {
        var bay = bay(helper);
        item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        helper.assertTrue(bay.contents().insertItem(WarheadBay.BOMB, item, false).isEmpty(), "the whole bomb fits");
        helper.assertTrue(bay.validCharge() && bay.warhead().kind() == kind, "bomb contents give " + kind);
        helper.assertTrue(bay.warhead().threatRadius() > 0, "the charge has its radius");
        bay.contents().setStackInSlot(0, core(1));
        helper.assertTrue("charge_bomb_and_loose".equals(WarheadBay.validate(bay.contents())), "whole and loose cannot mix");
        helper.succeed();
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void bombItemInBayGivesFission(GameTestHelper helper) {
        bombInBay(helper, new ItemStack(AllNuclearItems.NUCLEAR_BOMB.get()), List.of(core(2)), Warhead.Kind.FISSION);
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void bombItemInBayGivesDirty(GameTestHelper helper) {
        bombInBay(helper, new ItemStack(AllNuclearBlocks.DIRTY_BOMB.get()),
                List.of(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 8)), Warhead.Kind.DIRTY);
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void bombItemInBayGivesThermonuclear(GameTestHelper helper) {
        bombInBay(helper, new ItemStack(ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get()), List.of(core(1), lithium(4)), Warhead.Kind.THERMONUCLEAR);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void fiveSlotSectionMigrates(GameTestHelper helper) {
        var bay = bay(helper);
        ItemStackHandler old = new ItemStackHandler(5);
        old.setStackInSlot(0, core(1));
        old.setStackInSlot(3, lithium(10));
        old.setStackInSlot(4, new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get(), 2));
        var registries = helper.getLevel().registryAccess();
        var tag = bay.saveWithoutMetadata(registries);
        tag.remove("Layout");
        tag.put("Contents", old.serializeNBT(registries));
        bay.loadWithComponents(tag, registries);
        helper.assertTrue(bay.contents().getSlots() == WarheadBay.SLOTS, "live handler remains ten slots");
        helper.assertTrue(bay.contents().getStackInSlot(3).getCount() == 4 && bay.contents().getStackInSlot(4).getCount() == 4
                && bay.contents().getStackInSlot(5).getCount() == 2, "old deuteride splits without loss");
        helper.assertTrue(bay.contents().getStackInSlot(7).getCount() == 2 && bay.validCharge(), "tamper moves to typed slot");
        helper.assertTrue(bay.saveWithoutMetadata(registries).getInt("Layout") == 2, "new saves record the layout");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void rightClickRefusalLeavesStack(GameTestHelper helper) {
        var bay = bay(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 8));
        bay.insert(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 8, "refused item stays held");
        for (int i = 0; i < WarheadBay.SLOTS; i++) helper.assertTrue(bay.contents().getStackInSlot(i).isEmpty(), "nothing inserted");
        helper.succeed();
    }
}

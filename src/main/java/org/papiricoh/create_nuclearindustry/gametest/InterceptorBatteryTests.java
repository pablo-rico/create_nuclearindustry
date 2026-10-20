package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseSettings;
import org.papiricoh.create_nuclearindustry.missile.defense.Doctrine;
import org.papiricoh.create_nuclearindustry.missile.defense.EngagementSource;
import org.papiricoh.create_nuclearindustry.missile.defense.FireResult;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryItem;

import java.util.UUID;

/**
 * The interceptor battery as a block: placed whole and broken whole, loaded one round a tube by hand or automation, reloading,
 * turning only when driven, held by redstone, and keeping its settings and rounds through a save.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class InterceptorBatteryTests {
    static final String BATCH = "battery";
    /** Where the tests put a battery's master, relative to the arena. */
    static final BlockPos MASTER = new BlockPos(3, 1, 3);
    static final float RPM = 64;

    /** A battery built facing north at {@link #MASTER}. */
    static InterceptorBatteryEntity battery(GameTestHelper helper) {
        InterceptorBatteryItem.build(helper.getLevel(), helper.absolutePos(MASTER), Direction.NORTH);
        InterceptorBatteryEntity battery = helper.getBlockEntity(MASTER);
        helper.assertTrue(battery != null, "the battery's master has its block entity");
        return battery;
    }

    /** Keeps {@code battery} turning: a kinetic block with no source slows to a stop otherwise. */
    static void spin(GameTestHelper helper, InterceptorBatteryEntity battery) {
        helper.onEachTick(() -> battery.setSpeed(RPM));
        battery.setSpeed(RPM);
    }

    static ItemStack rounds(int count) {
        return new ItemStack(DefenseRegistry.INTERCEPTOR.get(), count);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theItemPlacesTheWholeBatteryOrNothing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(180); // looking north
        BlockPos clicked = helper.absolutePos(new BlockPos(5, 0, 5));
        ItemStack item = new ItemStack(DefenseRegistry.BATTERY_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)));
        BlockPos at = context.getClickedPos();
        InteractionResult placed = DefenseRegistry.BATTERY_ITEM.get().place(context);
        helper.assertTrue(placed.consumesAction(), "the battery goes down on free ground");
        BlockPos master = InterceptorBatteryItem.master(at, Direction.NORTH);
        helper.assertTrue(master.equals(at.north()), "it grows away from the player (north) and to their right (east), got " + master + " from " + at);
        int parts = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            var state = helper.getLevel().getBlockState(master.offset(x, y, z));
            if (x + y + z == 0) helper.assertTrue(state.is(DefenseRegistry.BATTERY.get()), "the master at the lowest corner");
            else if (state.is(DefenseRegistry.BATTERY_PART.get())) parts++;
        }
        helper.assertTrue(parts == 7, "seven parts, got " + parts);
        helper.assertTrue(item.getCount() == 1, "one item used");
        helper.assertTrue(helper.getLevel().getBlockEntity(master) instanceof InterceptorBatteryEntity battery
                && player.getUUID().equals(battery.owner()), "the placer owns it");

        // Beside it, one block in the way stops a second battery and costs nothing.
        BlockPos second = helper.absolutePos(new BlockPos(9, 0, 9));
        BlockPlaceContext blocked = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(second), Direction.UP, second, false)));
        BlockPos secondMaster = InterceptorBatteryItem.master(blocked.getClickedPos(), Direction.NORTH);
        helper.getLevel().setBlockAndUpdate(secondMaster.offset(1, 1, 1), Blocks.STONE.defaultBlockState());
        helper.assertTrue(DefenseRegistry.BATTERY_ITEM.get().place(blocked) == InteractionResult.FAIL, "an obstructed footprint is refused");
        helper.assertTrue(!helper.getLevel().getBlockState(secondMaster).is(DefenseRegistry.BATTERY.get()), "nothing of it is placed");
        helper.assertTrue(item.getCount() == 1, "and no item used");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void breakingAnyPartTakesTheWholeBatteryDown(GameTestHelper helper) {
        InterceptorBatteryEntity battery = battery(helper);
        helper.assertTrue(battery.loadOne(rounds(1)) && battery.loadOne(rounds(1)), "two rounds load");
        helper.getLevel().destroyBlock(helper.absolutePos(MASTER.offset(1, 1, 0)), true);
        helper.succeedWhen(() -> {
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
                helper.assertBlockPresent(Blocks.AIR, MASTER.offset(x, y, z));
            AABB around = new AABB(helper.absolutePos(MASTER)).inflate(3);
            int batteries = 0, interceptors = 0;
            for (ItemEntity drop : helper.getLevel().getEntitiesOfClass(ItemEntity.class, around)) {
                if (drop.getItem().is(DefenseRegistry.BATTERY_ITEM.get())) batteries += drop.getItem().getCount();
                if (drop.getItem().is(DefenseRegistry.INTERCEPTOR.get())) interceptors += drop.getItem().getCount();
            }
            helper.assertTrue(batteries == 1, "the battery drops once, got " + batteries);
            helper.assertTrue(interceptors == 2, "and its rounds, got " + interceptors);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void anyPartTakesRoundsOneATubeAndGivesNoneBack(GameTestHelper helper) {
        InterceptorBatteryEntity battery = battery(helper);
        IItemHandler tubes = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MASTER.offset(1, 1, 1)), null);
        helper.assertTrue(tubes != null && tubes.getSlots() == InterceptorBatteryEntity.TUBES, "a part exposes the four tubes");
        helper.assertTrue(tubes.insertItem(0, new ItemStack(Items.STONE), false).getCount() == 1, "only interceptors go in");
        int loaded = 0;
        for (int i = 0; i < tubes.getSlots(); i++) {
            ItemStack left = tubes.insertItem(i, rounds(2), false);
            helper.assertTrue(left.getCount() == 1, "one round a tube, tube " + i);
            loaded++;
        }
        helper.assertTrue(loaded == 4, "all four tubes load");
        helper.assertTrue(tubes.extractItem(0, 1, false).isEmpty(), "automation never pulls a live round out");
        helper.assertTrue(!battery.tubeInventory().getStackInSlot(3).isEmpty(), "the fourth tube is reachable too");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aLoadedRoundIsReadyAfterTheReloadTime(GameTestHelper helper) {
        InterceptorBatteryEntity battery = battery(helper);
        int saved = DefenseConfig.batteryReloadSeconds;
        try {
            DefenseConfig.batteryReloadSeconds = 1;
            battery.loadOne(rounds(1));
        } finally {
            DefenseConfig.batteryReloadSeconds = saved;
        }
        helper.assertTrue(battery.loadingTicks(0) == 20, "a second to seat the round");
        helper.runAfterDelay(10, () -> helper.assertTrue(battery.loadingTicks(0) > 0, "not ready halfway"));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(battery.loadingTicks(0) == 0, "ready after the reload time");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void withoutRotationItNeitherTurnsNorTakesOrders(GameTestHelper helper) {
        InterceptorBatteryEntity battery = battery(helper);
        battery.loadOne(rounds(1));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!battery.operational(), "a still battery is not operational");
            helper.assertTrue(battery.status() == InterceptorBatteryEntity.Status.UNPOWERED, "it says so, got " + battery.status());
            helper.assertTrue(battery.order(helper.getLevel(), UUID.randomUUID(), EngagementSource.MANUAL_TABLE) == FireResult.OFFLINE,
                    "and refuses orders");
            helper.assertTrue(battery.reactionTicks(new org.papiricoh.create_nuclearindustry.missile.physics.V3(1, 1, 0)) >= 100_000,
                    "it would never come round");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void redstoneHoldsItsAutomaticFire(GameTestHelper helper) {
        InterceptorBatteryEntity battery = battery(helper);
        spin(helper, battery);
        battery.loadOne(rounds(1));
        helper.setBlock(MASTER.offset(2, 1, 0), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(battery.operational(), "it is turning");
            helper.assertTrue(battery.status() == InterceptorBatteryEntity.Status.HOLD, "a signal into a part holds it, got " + battery.status());
            helper.assertTrue(battery.order(helper.getLevel(), UUID.randomUUID(), EngagementSource.AUTO_BATTERY) == FireResult.HOLD,
                    "it refuses its own automatic orders");
            helper.setBlock(MASTER.offset(2, 1, 0), Blocks.AIR);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void itKeepsItsSettingsAndRoundsThroughASave(GameTestHelper helper) {
        InterceptorBatteryEntity battery = battery(helper);
        UUID owner = UUID.randomUUID();
        battery.setOwner(owner);
        battery.setCode("ab12");
        battery.setSettings(new DefenseSettings(true, Doctrine.SALVO_TWO, 10, 20, 350));
        battery.loadOne(rounds(1));
        battery.loadOne(rounds(1));
        CompoundTag saved = battery.saveWithFullMetadata(helper.getLevel().registryAccess());
        InterceptorBatteryEntity copy = new InterceptorBatteryEntity(battery.getBlockPos(), battery.getBlockState());
        copy.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(owner.equals(copy.owner()), "its owner");
        helper.assertTrue("AB12".equals(copy.code()), "its link code, tidied, got " + copy.code());
        helper.assertTrue(copy.settings().equals(new DefenseSettings(true, Doctrine.SALVO_TWO, 10, 20, 350)), "its settings");
        helper.assertTrue(!copy.tubeInventory().getStackInSlot(0).isEmpty() && !copy.tubeInventory().getStackInSlot(1).isEmpty()
                && copy.tubeInventory().getStackInSlot(2).isEmpty(), "its rounds");
        helper.assertTrue(copy.loadingTicks(0) == battery.loadingTicks(0), "and how far they are from ready");
        helper.succeed();
    }
}

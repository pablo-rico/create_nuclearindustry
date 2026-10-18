package org.papiricoh.create_nuclearindustry.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatBoard;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.ThermonuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearIntegration;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

/** The thermonuclear bomb: its three parts, its countdown, what it tells the world and its cannon shell. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ThermonuclearTests {
    private static final String BATCH = "thermonuclear";
    private static final BlockPos BOMB = new BlockPos(6, 3, 6);

    private static ThermonuclearBombBlockEntity bomb(GameTestHelper helper) {
        helper.setBlock(BOMB, ExplosiveRegistry.THERMONUCLEAR_BOMB.get());
        return helper.getBlockEntity(BOMB);
    }

    private static ItemStack deuteride(int count) {
        return new ItemStack(ExplosiveRegistry.LITHIUM_DEUTERIDE.get(), count);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void eachSlotTakesOnlyItsPart(GameTestHelper helper) {
        ThermonuclearBombBlockEntity bomb = bomb(helper);
        ItemStack core = new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get(), 3);
        ItemStack tamper = new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get(), 5);
        helper.assertTrue(bomb.isValidInSlot(ThermonuclearBombBlockEntity.PRIMARY_SLOT, core)
                && !bomb.isValidInSlot(ThermonuclearBombBlockEntity.PRIMARY_SLOT, deuteride(1)), "the primary takes a core");
        helper.assertTrue(bomb.isValidInSlot(ThermonuclearBombBlockEntity.FIRST_SECONDARY_SLOT, deuteride(1))
                && !bomb.isValidInSlot(ThermonuclearBombBlockEntity.FIRST_SECONDARY_SLOT, core), "the secondary takes deuteride");
        helper.assertTrue(bomb.isValidInSlot(ThermonuclearBombBlockEntity.FIRST_TAMPER_SLOT, tamper)
                && !bomb.isValidInSlot(ThermonuclearBombBlockEntity.FIRST_TAMPER_SLOT, deuteride(1)), "the tamper takes depleted uranium");
        helper.assertFalse(bomb.isValidCharge(new ItemStack(AllNuclearItems.PLUTONIUM.get())), "loose plutonium has no slot");
        helper.assertTrue(bomb.getInventory().insertItem(ThermonuclearBombBlockEntity.PRIMARY_SLOT, core, false).getCount() == 2,
                "one core is the primary");
        helper.assertTrue(bomb.getInventory().insertItem(ThermonuclearBombBlockEntity.FIRST_SECONDARY_SLOT, deuteride(10), false).getCount() == 6,
                "four deuteride to a slot");
        helper.assertTrue(bomb.getInventory().insertItem(ThermonuclearBombBlockEntity.FIRST_TAMPER_SLOT, tamper, false).getCount() == 3,
                "two tamper blocks to a slot");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void withoutAPrimaryItCannotCountDown(GameTestHelper helper) {
        ThermonuclearBombBlockEntity bomb = bomb(helper);
        for (int slot = ThermonuclearBombBlockEntity.FIRST_SECONDARY_SLOT; slot < ThermonuclearBombBlockEntity.FIRST_TAMPER_SLOT; slot++) {
            bomb.getInventory().setStackInSlot(slot, deuteride(4));
        }
        helper.setBlock(BOMB.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            helper.setBlock(BOMB.east(), Blocks.AIR);
            helper.assertFalse(bomb.hasCharge(), "deuteride alone is no charge");
            helper.assertTrue(bomb.getCountdown() < 0, "so the bomb does not count down");
            helper.assertTrue(ThreatRegistry.find(helper.getLevel(), helper.absolutePos(BOMB)) == null, "nor warns anyone");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void itsPartsSetItsYield(GameTestHelper helper) {
        ThermonuclearBombBlockEntity bomb = bomb(helper);
        bomb.getInventory().setStackInSlot(ThermonuclearBombBlockEntity.PRIMARY_SLOT, new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get()));
        ThermonuclearStats.Result fizzle = bomb.stats();
        helper.assertTrue(fizzle.fizzle() && fizzle.radius() == WarheadStats.horizontalRadius(8, 90f),
                "a primary alone fizzles as one core, got " + fizzle.radius());
        bomb.getInventory().setStackInSlot(ThermonuclearBombBlockEntity.FIRST_SECONDARY_SLOT, deuteride(4));
        ThermonuclearStats.Result four = bomb.stats();
        helper.assertTrue(!four.fizzle() && four.radius() > WarheadStats.horizontalRadius(48, 90f),
                "four deuteride outreach a full atomic bomb, got " + four.radius());
        bomb.getInventory().setStackInSlot(ThermonuclearBombBlockEntity.FIRST_TAMPER_SLOT, new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get(), 2));
        ThermonuclearStats.Result tamped = bomb.stats();
        helper.assertTrue(tamped.radius() > four.radius() && tamped.falloutRadius() > four.falloutRadius() && tamped.hotSpot() > 0,
                "the tamper adds yield, wider fallout and a hot spot");
        for (int slot = 0; slot < ThermonuclearBombBlockEntity.SLOTS; slot++) {
            ThermonuclearBombBlockEntity.Part part = ThermonuclearBombBlockEntity.part(slot);
            bomb.getInventory().setStackInSlot(slot, switch (part) {
                case PRIMARY -> new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get());
                case SECONDARY -> deuteride(ThermonuclearBombBlockEntity.DEUTERIDE_PER_SLOT);
                case TAMPER -> new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get(), ThermonuclearBombBlockEntity.TAMPER_PER_SLOT);
            });
        }
        helper.assertTrue(bomb.getEstimatedRadius() == ExplosiveConfig.thermonuclearMaxRadius,
                "a full bomb reaches the server's cap, got " + bomb.getEstimatedRadius());
        helper.assertTrue(bomb.getEstimatedCraterRadius() <= ExplosiveConfig.thermonuclearMaxCraterRadius, "with its crater kept small");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void itCountsDownLongerAndWarnsOfItsFallout(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ThermonuclearBombBlockEntity bomb = bomb(helper);
        bomb.getInventory().setStackInSlot(ThermonuclearBombBlockEntity.PRIMARY_SLOT, new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get()));
        bomb.getInventory().setStackInSlot(ThermonuclearBombBlockEntity.FIRST_SECONDARY_SLOT, deuteride(2));
        BlockPos at = helper.absolutePos(BOMB);
        helper.setBlock(BOMB.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            ThreatBoard.Threat threat = ThreatRegistry.find(level, at);
            long ticks = threat == null ? 0 : threat.ticksToImpact(level.getGameTime());
            helper.setBlock(BOMB.east(), Blocks.AIR);
            helper.assertTrue(bomb.getCountdownDuration() == ExplosiveConfig.thermonuclearCountdownSeconds * 20,
                    "it counts down for " + ExplosiveConfig.thermonuclearCountdownSeconds + " seconds");
            helper.assertTrue(threat != null, "a counting bomb warns the sirens");
            helper.assertTrue(ticks > bomb.getCountdownDuration() - 10, "lands when the countdown ends, got " + ticks);
            helper.assertTrue(threat.radius() == bomb.stats().falloutRadius(), "reaches as far as its fallout");
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(ThreatRegistry.find(level, at) == null && bomb.getCountdown() < 0, "taking the signal away stops it");
            helper.succeed();
        });
    }

    /** Listeners and clients learn it was thermonuclear; the old way of posting the event still reads the same. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theWorldLearnsItWasThermonuclear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        NuclearDetonationEvent hydrogen = new NuclearDetonationEvent(level, Vec3.ZERO, 1000, 500, DetonationStyle.THERMONUCLEAR);
        NuclearDetonationEvent dirty = new NuclearDetonationEvent(level, Vec3.ZERO, 5, 30, true);
        NuclearDetonationEvent fission = new NuclearDetonationEvent(level, Vec3.ZERO, 160, 250, false);
        helper.assertTrue(hydrogen.isThermonuclear() && !hydrogen.isDirty(), "a thermonuclear event");
        helper.assertTrue(dirty.isDirty() && dirty.getStyle() == DetonationStyle.DIRTY, "a dirty one");
        helper.assertTrue(fission.getStyle() == DetonationStyle.FISSION && !fission.isThermonuclear(), "and a fission one");
        DetonationPayload sent = new DetonationPayload(new Vec3(1, 2, 3), 750, 90, 0, false, 1.0e6f, 8, 768, 1050, 42L,
                DetonationStyle.THERMONUCLEAR);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        DetonationPayload.STREAM_CODEC.encode(buf, sent);
        DetonationPayload received = DetonationPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(received.equals(sent), "the payload arrives as sent: " + received);
        helper.assertTrue(received.style() == DetonationStyle.THERMONUCLEAR && !received.dirty(), "drawn in its own style");
        helper.succeed();
    }

    /** A missile is too small for a thermonuclear warhead. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aLaunchPadTakesNoThermonuclearParts(GameTestHelper helper) {
        helper.setBlock(BOMB, MissileRegistry.LAUNCH_PAD.get());
        LaunchPadBlockEntity pad = helper.getBlockEntity(BOMB);
        int warhead = LaunchPadBlockEntity.FIRST_URANIUM_SLOT;
        helper.assertFalse(pad.isItemValid(warhead, deuteride(1)), "no lithium deuteride");
        helper.assertFalse(pad.isItemValid(warhead, new ItemStack(ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get())), "no thermonuclear bomb");
        helper.assertTrue(pad.isItemValid(warhead, new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get())), "a core still goes in");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aThermonuclearShellCarriesItsParts(GameTestHelper helper) {
        if (!CBCNuclearIntegration.isLoaded()) {
            helper.succeed();
            return;
        }
        ItemStack bomb = new ItemStack(ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get());
        ItemStack core = new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get());
        ItemStack lid = deuteride(1);
        ItemStack tamper = new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get());
        ThermonuclearLoad small = CannonShellRecipes.thermonuclear(helper, bomb, core, lid);
        ThermonuclearLoad full = CannonShellRecipes.thermonuclear(helper, bomb, core, lid, lid, lid, lid, tamper, tamper);
        helper.assertTrue(small != null && full != null, "a shell takes one to four deuteride and up to two tamper blocks");
        helper.assertTrue(small.secondary() == 1 && full.secondary() == 4 && full.tamper() == 2
                && full.primary() == ThermonuclearStats.Primary.PLUTONIUM, "and carries them: " + small + ", " + full);
        ThermonuclearStats.Caps caps = ExplosiveConfig.thermonuclearCaps();
        helper.assertTrue(small.stats(caps).radius() < full.stats(caps).radius(), "more deuteride reaches further");
        helper.assertTrue(CannonShellRecipes.thermonuclear(helper, bomb, lid, lid) == null, "no core, no shell");
        helper.assertTrue(CannonShellRecipes.thermonuclear(helper, bomb, core) == null, "no deuteride, no shell");
        helper.assertTrue(CannonShellRecipes.thermonuclear(helper, new ItemStack(AllNuclearItems.NUCLEAR_BOMB.get()), core, lid) == null,
                "a nuclear bomb is no radiation case");
        helper.succeed();
    }
}

package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveManager;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalDetonation;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalDetonationEvent;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalKind;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalRegistry;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuard;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuards;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import org.papiricoh.create_nuclearindustry.missile.silo.WarheadBay;

import java.util.function.Consumer;

/**
 * Conventional and thermobaric warheads: what a blast digs and leaves, who it hurts and who walls and claims keep safe, whose
 * blast it is, a cancelled blast, and what a warhead bay takes. Charges are kept small to fit the 41 wide arena.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ConventionalTests {
    private static final BlockPos CENTRE = new BlockPos(20, 4, 20);

    private static void ground(GameTestHelper helper) {
        for (int x = 2; x <= 38; x++) {
            for (int z = 2; z <= 38; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.DIRT);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.GRASS_BLOCK);
            }
        }
    }

    private static Pig pig(GameTestHelper helper, BlockPos at) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, at);
        pig.setHealth(pig.getMaxHealth());
        return pig;
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "conventional_crater", timeoutTicks = 300)
    public static void aHighExplosiveWarheadDigsAPlainCrater(GameTestHelper helper) {
        ground(helper);
        // Fresh rock under the bowl too: an earlier test's glass must not be taken for this one's.
        for (int x = 14; x <= 26; x++) {
            for (int z = 14; z <= 26; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        ServerLevel level = helper.getLevel();
        int before = NuclearBlastManager.pending(level);
        helper.runAfterDelay(2, () -> Warhead.conventional(3).detonate(level, helper.absoluteVec(Vec3.atCenterOf(CENTRE))));
        helper.succeedWhen(() -> {
            helper.assertTrue(NuclearBlastManager.pending(level) == before, "the crater is still being dug");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(20, 3, 20));
            int glass = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int y = 0; y <= 3; y++) {
                        glass += helper.getBlockState(new BlockPos(20 + dx, y, 20 + dz)).is(ExplosiveRegistry.TRINITITE.get()) ? 1 : 0;
                    }
                }
            }
            helper.assertTrue(glass == 0, "high explosive fuses no glass: " + glass);
            helper.assertTrue(BlastWaveManager.active(level) == 0, "its wave is still travelling");
            helper.assertFalse(helper.getBlockState(new BlockPos(20, 3, 34)).isAir(), "the ground far off is untouched");
        });
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "conventional_cover", timeoutTicks = 200)
    public static void wallsKeepTheBlastAndFragmentsOff(GameTestHelper helper) {
        ground(helper);
        // An obsidian wall, blast-proof for a conventional blast, between the charge and one of two pigs equally far off.
        // Both pigs stand past the crater's rim and ejecta, where only the wave and the fragments reach.
        for (int x = 14; x <= 27; x++) {
            for (int y = 4; y <= 8; y++) {
                helper.setBlock(new BlockPos(x, y, 31), Blocks.OBSIDIAN);
            }
        }
        // The open pig stands on a pillar, over the crater's rim and ejecta, which would shelter it at ground level.
        helper.setBlock(new BlockPos(20, 4, 7), Blocks.STONE);
        helper.setBlock(new BlockPos(20, 5, 7), Blocks.STONE);
        Pig open = pig(helper, new BlockPos(20, 6, 7));
        Pig covered = pig(helper, new BlockPos(20, 4, 33));
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(2, () -> ConventionalDetonation.detonate(level, helper.absoluteVec(new Vec3(20.5, 5, 20.5)),
                ConventionalKind.HIGH_EXPLOSIVE, 8, DetonationCause.NONE));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!open.isAlive() || open.getHealth() < open.getMaxHealth(), "the pig in the open is hurt: "
                    + BlastWaveManager.overpressureAt(level, helper.absoluteVec(new Vec3(20.5, 5, 20.5)),
                    org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalProfile.of(ConventionalKind.HIGH_EXPLOSIVE, 8, 1,
                            org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalConfig.settings()).yieldKg(),
                    open.getBoundingBox().getCenter()) + " kPa, at " + helper.relativeVec(open.position()));
            helper.assertTrue(covered.isAlive() && covered.getHealth() == covered.getMaxHealth(), "the pig behind the wall is not: "
                    + covered.getHealth() + " by " + (covered.getLastDamageSource() == null ? "nothing" : covered.getLastDamageSource().getMsgId()));
            open.discard();
            covered.discard();
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "conventional_owner", timeoutTicks = 200)
    public static void theBlastIsWhoeverSetItOffs(GameTestHelper helper) {
        ground(helper);
        Pig owner = pig(helper, new BlockPos(4, 4, 4));
        Pig victim = pig(helper, new BlockPos(20, 4, 14));
        ServerLevel level = helper.getLevel();
        DetonationCause cause = DetonationCause.of(owner);
        helper.assertTrue(cause.damage(level, ConventionalRegistry.SHRAPNEL, Vec3.ZERO).getEntity() == owner, "its damage names the owner");
        helper.runAfterDelay(2, () -> ConventionalDetonation.detonate(level, helper.absoluteVec(new Vec3(20.5, 5, 20.5)),
                ConventionalKind.HIGH_EXPLOSIVE, 8, cause));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(victim.getLastHurtByMob() == owner, "the victim was hurt by the owner, not by nobody");
            owner.discard();
            victim.discard();
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "conventional_cancel", timeoutTicks = 100)
    public static void aCancelledBlastDoesNothing(GameTestHelper helper) {
        ground(helper);
        Pig pig = pig(helper, new BlockPos(20, 4, 17));
        ServerLevel level = helper.getLevel();
        int jobs = NuclearBlastManager.pending(level);
        int waves = BlastWaveManager.active(level);
        Consumer<ConventionalDetonationEvent> cancel = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            ConventionalDetonation.detonate(level, helper.absoluteVec(new Vec3(20.5, 4.5, 20.5)), ConventionalKind.HIGH_EXPLOSIVE, 8,
                    DetonationCause.NONE);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertTrue(NuclearBlastManager.pending(level) == jobs && BlastWaveManager.active(level) == waves, "no crater and no wave");
        helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "and nobody hurt");
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(20, 3, 20));
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "conventional_claims", timeoutTicks = 200)
    public static void claimsKeepTheirWindows(GameTestHelper helper) {
        ground(helper);
        BlockPos claimedPane = new BlockPos(6, 4, 20);
        BlockPos openPane = new BlockPos(34, 4, 20);
        helper.setBlock(claimedPane, Blocks.GLASS);
        helper.setBlock(openPane, Blocks.GLASS);
        // Open sky over both, whatever an earlier test left above the arena: a covered window is sheltered by its cover.
        for (int y = 5; y <= 30; y++) {
            helper.setBlock(claimedPane.atY(y), Blocks.AIR);
            helper.setBlock(openPane.atY(y), Blocks.AIR);
        }
        BlockPos claimed = helper.absolutePos(claimedPane);
        ClaimGuard guard = (level, cx, cz) -> cx == claimed.getX() >> 4 && cz == claimed.getZ() >> 4;
        ClaimGuards.add(guard);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(2, () -> ConventionalDetonation.detonate(level, helper.absoluteVec(new Vec3(20.5, 4.5, 20.5)),
                ConventionalKind.HIGH_EXPLOSIVE, 6, DetonationCause.NONE));
        helper.runAfterDelay(4, () -> helper.assertTrue(true, "waves " + BlastWaveManager.active(level)));
        helper.runAfterDelay(40, () -> {
            try {
                double kPa = org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel.overpressure(
                        helper.absoluteVec(Vec3.atCenterOf(openPane)).distanceTo(helper.absoluteVec(new Vec3(20.5, 4.5, 20.5))),
                        org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalProfile.of(ConventionalKind.HIGH_EXPLOSIVE, 6, 0.5,
                                org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalConfig.settings()).yieldKg());
                helper.assertTrue(helper.getBlockState(openPane).isAir(), "the open window breaks at " + kPa + " kPa, still "
                        + helper.getBlockState(openPane) + ", top " + helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        helper.absolutePos(openPane).getX(), helper.absolutePos(openPane).getZ()) + " vs " + helper.absolutePos(openPane).getY());
                helper.assertBlockPresent(Blocks.GLASS, claimedPane);
            } finally {
                ClaimGuards.remove(guard);
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "conventional_thermobaric", timeoutTicks = 100)
    public static void aThermobaricFireballSetsBodiesAlight(GameTestHelper helper) {
        ground(helper);
        Pig pig = pig(helper, new BlockPos(23, 4, 20));
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(2, () -> ConventionalDetonation.detonate(level, helper.absoluteVec(new Vec3(20.5, 4.2, 20.5)),
                ConventionalKind.THERMOBARIC, 3, DetonationCause.NONE));
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!pig.isAlive() || pig.getRemainingFireTicks() > 0, "the pig in the fireball burns");
            pig.discard();
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "conventional_bay")
    public static void aBayTakesOneKindOfConventionalCharge(GameTestHelper helper) {
        BlockPos at = new BlockPos(5, 2, 5);
        helper.setBlock(at, SiloRegistry.WARHEAD.get());
        RocketSectionEntity bay = helper.getBlockEntity(at);
        bay.contents().insertItem(0, new ItemStack(ConventionalRegistry.THERMOBARIC_CHARGE.get(), 16), false);
        ItemStack refused = bay.contents().insertItem(1, new ItemStack(Items.TNT, 4), false);
        helper.assertTrue(refused.getCount() == 4, "a thermobaric bay refuses TNT");
        Warhead thermobaric = bay.warhead();
        helper.assertTrue(thermobaric.kind() == Warhead.Kind.THERMOBARIC && thermobaric.charge() == 64, "16 charges of 4: " + thermobaric);
        helper.assertTrue(thermobaric.airburstMode() == Warhead.AirburstMode.ALWAYS && thermobaric.burstHeight(false) > 0,
                "it always goes off in the air");
        bay.contents().setStackInSlot(1, new ItemStack(Items.TNT, 4));
        helper.assertTrue("charge_mixed_types".equals(WarheadBay.validate(bay.contents())), "a mixed load is refused");
        bay.contents().setStackInSlot(0, new ItemStack(ConventionalRegistry.PLASTIC_EXPLOSIVE.get(), 16));
        helper.assertTrue(bay.validCharge() && bay.warhead().kind() == Warhead.Kind.CONVENTIONAL && bay.warhead().charge() == 68,
                "TNT and plastic explosive make one high-explosive warhead: " + bay.warhead());
        bay.contents().setStackInSlot(0, new ItemStack(ConventionalRegistry.CLUSTER_BOMBLET.get(), 12));
        bay.contents().setStackInSlot(1, ItemStack.EMPTY);
        Warhead cluster = bay.warhead();
        helper.assertTrue(cluster.kind() == Warhead.Kind.CLUSTER && cluster.bomblets() == 12 && Math.abs(cluster.charge() - 18) < 1e-3,
                "twelve bomblets: " + cluster);
        helper.assertTrue(Warhead.load(cluster.save()).equals(cluster), "a cluster warhead survives saves");
        helper.assertTrue(Warhead.airburstMode(Warhead.Kind.DIRTY) == Warhead.AirburstMode.NEVER, "a dirty bomb stays on the ground");
        helper.succeed();
    }
}

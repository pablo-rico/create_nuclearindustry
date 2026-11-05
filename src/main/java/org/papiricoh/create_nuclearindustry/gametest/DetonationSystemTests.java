package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveManager;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaves;
import org.papiricoh.create_nuclearindustry.explosive.ActiveDetonations;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;
import org.papiricoh.create_nuclearindustry.explosive.NuclearFlash;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;

import java.util.List;
import java.util.function.Consumer;

/**
 * What every detonation shares: its wave outliving a save, its flash and fallout spread over ticks, standing clouds told to
 * latecomers and forgotten when they clear, a cancelled detonation, the sirens a missile in flight sets off, and what the
 * flash and the shock do to a player's eyes and ears.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class DetonationSystemTests {
    /** A detonation small enough for the 41 wide arena, as {@code ExplosionTests} uses. */
    private static DetonationProfile small() {
        return new DetonationProfile(10, 2500, 12, 1, 0, false, 2, 6, 2, 8, 1, 10, 5, 7, 9.5, 18, 8, 0, 0, 0);
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "system_wave_save", timeoutTicks = 100)
    public static void aWaveSurvivesASave(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlastWaveManager.start(level, helper.absoluteVec(new Vec3(20.5, 5, 20.5)), 2.5, 18, DetonationCause.NONE, 0.5);
        BlastWaves waves = BlastWaves.get(level);
        CompoundTag tag = waves.save(new CompoundTag(), level.registryAccess());
        BlastWaves loaded = BlastWaves.fromTag(tag, level.registryAccess());
        helper.assertTrue(!waves.waves().isEmpty() && loaded.waves().size() == waves.waves().size(), "the waves are saved");
        helper.assertTrue(loaded.waves().getLast().radius() == waves.waves().getLast().radius(), "where each had got to");
        helper.succeedWhen(() -> helper.assertTrue(BlastWaveManager.active(level) == 0, "the wave goes its way"));
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "system_flash_spread", timeoutTicks = 100)
    public static void theFlashReachesBodiesNearestFirst(GameTestHelper helper) {
        Pig near = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(20, 2, 24));
        Pig far = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(20, 2, 32));
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(2, () -> {
            int traces = ExplosiveConfig.flashTracesPerTick;
            ExplosiveConfig.flashTracesPerTick = 1;
            try {
                NuclearFlash.strike(level, helper.absoluteVec(new Vec3(20.5, 2.5, 20.5)), small());
            } finally {
                ExplosiveConfig.flashTracesPerTick = traces;
            }
            helper.assertTrue(NuclearFlash.pending(level) == 1, "one body this tick, the other later");
            helper.assertTrue(near.getRemainingFireTicks() > 0 && far.getRemainingFireTicks() <= 0, "the nearest burns first");
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(NuclearFlash.pending(level) == 0 && far.getRemainingFireTicks() > 0, "and the flash reaches the far one");
            near.discard();
            far.discard();
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_fallout_spread", timeoutTicks = 100)
    public static void falloutSettlesOverTicksNearestFirst(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(6, 1, 6));
        ChunkPos middle = new ChunkPos(centre);
        ChunkPos aside = new ChunkPos(middle.x + 2, middle.z);
        ContaminationTests.clean(helper, 3);
        int chunks = ExplosiveConfig.falloutChunksPerTick;
        ExplosiveConfig.falloutChunksPerTick = 1;
        try {
            Contamination.fallout(level, centre, 48, 20, 0, 0);
        } finally {
            ExplosiveConfig.falloutChunksPerTick = chunks;
        }
        boolean waiting = Contamination.pendingSweeps(level) > 0;
        float first = Contamination.get(level, middle);
        float before = Contamination.get(level, aside);
        helper.runAfterDelay(5, () -> {
            try {
                helper.assertTrue(waiting && first > 0, "ground zero gets its fallout at once");
                helper.assertTrue(before == 0, "a chunk further out waits its turn");
                helper.assertTrue(Contamination.pendingSweeps(level) == 0
                        && Contamination.read(level.getChunk(aside.x, aside.z)) > 0, "then catches up");
            } finally {
                ContaminationTests.clean(helper, 3);
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "system_active", timeoutTicks = 250)
    public static void aStandingCloudIsForgottenOnceItClears(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int before = ActiveDetonations.count(level);
        long old = level.getGameTime() - ActiveDetonations.NUCLEAR_TICKS - 5;
        ActiveDetonations.send(level, new DetonationPayload(helper.absoluteVec(new Vec3(6, 2, 6)), DetonationStyle.FISSION, 1L, old,
                10, 6, 0, false, 2500, 8, 40, 18, 7, 0, 0, 1));
        helper.assertTrue(ActiveDetonations.count(level) == before + 1, "it is kept for latecomers");
        // Clouds of earlier tests may clear meanwhile too: never more than there were.
        helper.succeedWhen(() -> helper.assertTrue(ActiveDetonations.count(level) <= before, "until its cloud has cleared"));
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "system_cancel", timeoutTicks = 100)
    public static void aCancelledDetonationDoesNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int jobs = NuclearBlastManager.pending(level);
        int waves = BlastWaveManager.active(level);
        int clouds = ActiveDetonations.count(level);
        Consumer<NuclearDetonationEvent> cancel = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            NuclearDetonation.detonate(level, helper.absoluteVec(new Vec3(20.5, 3, 20.5)), 10, 12, 1, 0);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertTrue(NuclearBlastManager.pending(level) == jobs, "no crater");
        helper.assertTrue(BlastWaveManager.active(level) == waves && NuclearFlash.pending(level) == 0, "no wave and no flash");
        helper.assertTrue(ActiveDetonations.count(level) == clouds, "no cloud");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "system_threats")
    public static void aMissileInFlightSetsOffSirens(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FlightTracker tracker = new FlightTracker();
        Vec3 start = new Vec3(20_000_000, 1000, 20_000_000);
        Flight flight = tracker.launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO, start.add(1500, -937, 0),
                BlockPos.containing(start.x + 1500, 63, start.z), Warhead.conventional(48), null);
        try {
            for (int i = 0; i < 3; i++) tracker.tick(level);
            helper.assertTrue(ThreatRegistry.find(level, flight.id()) != null, "a live missile in flight is a threat");
            tracker.remove(level, flight);
            helper.assertTrue(ThreatRegistry.find(level, flight.id()) == null, "until it is gone");
        } finally {
            for (Flight f : List.copyOf(tracker.flights())) tracker.remove(level, f);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "system_senses")
    public static void theFlashDazzlesAndTheShockDeafens(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player looking = helper.makeMockPlayer(GameType.SURVIVAL);
        looking.moveTo(helper.absoluteVec(new Vec3(20.5, 4, 10.5)), 0, 0);
        Vec3 fireball = helper.absoluteVec(new Vec3(20.5, 5.6, 22.5));
        helper.assertTrue(NuclearFlash.dazzle(level, looking, fireball, 6), "a player looking at the fireball is dazzled");
        helper.assertTrue(looking.hasEffect(ExplosionFxRegistry.FLASH_BLINDED), "and has the effect");
        Player goggles = helper.makeMockPlayer(GameType.SURVIVAL);
        goggles.moveTo(helper.absoluteVec(new Vec3(20.5, 4, 10.5)), 0, 0);
        goggles.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AllNuclearItems.HAZMAT_HELMET.get()));
        helper.assertFalse(NuclearFlash.dazzle(level, goggles, fireball, 6), "a hazmat visor keeps the flash out");
        Player away = helper.makeMockPlayer(GameType.SURVIVAL);
        away.moveTo(helper.absoluteVec(new Vec3(20.5, 4, 10.5)), 180, 0);
        helper.assertFalse(NuclearFlash.dazzle(level, away, fireball, 6), "the back turned sees nothing");
        helper.assertFalse(BlastWaveManager.Wave.deafen(away, ExplosiveConfig.deafeningThresholdKPa / 2), "a faint shock does not deafen");
        helper.assertTrue(BlastWaveManager.Wave.deafen(away, ExplosiveConfig.deafeningThresholdKPa * 5), "a strong one does");
        helper.assertTrue(away.hasEffect(ExplosionFxRegistry.DEAFENED), "and the player is deafened");
        helper.succeed();
    }
}

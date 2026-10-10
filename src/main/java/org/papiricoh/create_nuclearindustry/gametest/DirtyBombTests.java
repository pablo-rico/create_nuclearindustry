package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;

/** Radiological warheads: the dirty bomb and dirty or salted missiles. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class DirtyBombTests {
    private static final BlockPos DIRTY = new BlockPos(2, 1, 2);
    private static final BlockPos NUCLEAR = new BlockPos(5, 1, 2);
    private static final BlockPos PAD = new BlockPos(8, 1, 2);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void statsGrowWithTheLoadWithinBounds(GameTestHelper helper) {
        helper.assertTrue(DirtyWarheadStats.blastPower(0) == DirtyWarheadStats.BASE_BLAST, "an empty charge is just the explosive");
        helper.assertTrue(DirtyWarheadStats.blastPower(1.0e6f) == DirtyWarheadStats.MAX_BLAST, "the blast stays small");
        helper.assertTrue(DirtyWarheadStats.falloutRadius(400) > DirtyWarheadStats.falloutRadius(25), "more load spreads further");
        helper.assertTrue(DirtyWarheadStats.falloutRadius(1.0e6f) == DirtyWarheadStats.MAX_FALLOUT_RADIUS, "up to a limit");
        helper.assertTrue(DirtyWarheadStats.falloutIntensity(1.0e6f) == (float) Config.contaminationMax, "capped at the chunk maximum");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void dirtyBombTakesWasteButNotFissile(GameTestHelper helper) {
        helper.setBlock(DIRTY, AllNuclearBlocks.DIRTY_BOMB.get());
        helper.setBlock(NUCLEAR, AllNuclearBlocks.NUCLEAR_BOMB.get());
        NuclearBombBlockEntity dirty = helper.getBlockEntity(DIRTY);
        NuclearBombBlockEntity nuclear = helper.getBlockEntity(NUCLEAR);
        ItemStack waste = new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 10);
        ItemStack core = new ItemStack(AllNuclearItems.PLUTONIUM.get());
        helper.assertTrue(dirty.isDirty() && !nuclear.isDirty(), "the block decides the warhead");
        helper.assertTrue(dirty.isValidCharge(waste) && !dirty.isValidCharge(core), "a dirty bomb takes waste, not plutonium");
        helper.assertTrue(!nuclear.isValidCharge(waste) && nuclear.isValidCharge(core), "a nuclear bomb is the other way round");
        helper.assertFalse(dirty.isValidCharge(new ItemStack(AllNuclearItems.GRAPHITE.get())), "nor anything inert");
        dirty.getInventory().setStackInSlot(0, waste);
        helper.assertTrue(dirty.hasCharge() && Math.abs(dirty.getRadiologicalLoad() - 60f) < 0.01f,
                "ten waste are a load of 60 rad/s, got " + dirty.getRadiologicalLoad());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void missilesGoDirtyOrSalted(GameTestHelper helper) {
        helper.setBlock(PAD, AllNuclearBlocks.LAUNCH_PAD.get());
        LaunchPadBlockEntity pad = helper.getBlockEntity(PAD);
        ItemStack waste = new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get(), 4);
        helper.assertTrue(pad.isItemValid(LaunchPadBlockEntity.FIRST_URANIUM_SLOT, waste), "the warhead slots take waste");
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT, waste);
        helper.assertTrue(pad.isDirtyWarhead(), "waste alone is a dirty warhead");
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT + 1, new ItemStack(AllNuclearItems.PLUTONIUM.get(), 4));
        helper.assertFalse(pad.isDirtyWarhead(), "with plutonium it is a nuclear warhead");
        helper.assertTrue(pad.getRadiologicalLoad() > 0, "salted with the waste");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_dirtyDetonationSpreadsFalloutAndAHotSpot")
    public static void dirtyDetonationSpreadsFalloutAndAHotSpot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(6, 2, 6));
        ChunkPos origin = new ChunkPos(center);
        ContaminationData data = ContaminationData.get(level);
        FalloutData fallout = FalloutData.get(level);
        int sources = fallout.sources().size();
        float load = 25f;
        try {
            ContaminationTests.loadAround(level, origin, 4);
            NuclearDetonation.detonateDirty(level, center.getCenter(), load);
            helper.assertTrue(Contamination.get(level, origin) > 0, "ground zero is contaminated");
            helper.assertTrue(Contamination.get(level, new ChunkPos(origin.x + 4, origin.z + 4)) == 0,
                    "but not beyond the fallout radius of " + DirtyWarheadStats.falloutRadius(load));
            helper.assertTrue(fallout.sources().size() == sources + 1, "a hot spot is left behind");
            FalloutData.Source spot = fallout.sources().get(fallout.sources().size() - 1);
            helper.assertTrue(spot.pos().equals(center) && spot.strength() == DirtyWarheadStats.hotSpot(load), "at ground zero");
            fallout.sources().remove(fallout.sources().size() - 1);
            data.events().removeLast();
        } finally {
            ContaminationTests.clean(helper, 4);
        }
        helper.succeed();
    }
}

package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicDetectorBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicEvent;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicModel;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;

/**
 * The seismic detector, fed {@link NuclearDetonationEvent}s directly: it records distance, power and
 * bearing, ignores other dimensions, keeps eight events and drives a comparator. Every detector in the
 * level hears every event, so this runs in a batch of its own.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SeismicDetectorTests {
    private static final BlockPos DETECTOR = new BlockPos(6, 3, 6);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "civildefense_seismic")
    public static void detectorRecordsDetonationsInItsDimension(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(DETECTOR, CivilDefenseRegistry.SEISMIC_DETECTOR.get());
        BlockPos at = helper.absolutePos(DETECTOR);
        Vec3 here = Vec3.atCenterOf(at);
        // The detector joins the network on its first tick.
        helper.runAfterDelay(2, () -> {
            SeismicDetectorBlockEntity detector = helper.getBlockEntity(DETECTOR);
            helper.assertTrue(detector != null, "the detector has its block entity");
            ServerLevel nether = level.getServer().getLevel(Level.NETHER);
            if (nether != null) {
                NeoForge.EVENT_BUS.post(new NuclearDetonationEvent(nether, here.add(0, 0, -100), 100, 80, false));
            }
            helper.assertTrue(detector.getEvents().isEmpty(), "detonations in other dimensions are not felt");
            helper.assertTrue(comparator(helper) == 0, "no signal before anything happens");

            NeoForge.EVENT_BUS.post(new NuclearDetonationEvent(level, here.add(0, 0, -300), 100, 80, false));
            SeismicEvent north = detector.latest().orElse(null);
            helper.assertTrue(north != null, "a detonation in the same dimension is recorded");
            helper.assertTrue(Math.abs(north.distance() - 300) < 0.01 && north.power() == 100 && !north.dirty(),
                    "with its distance and power, got " + north);
            helper.assertTrue(Math.abs(north.bearing()) < 0.01, "300 blocks north is a bearing of 0, got " + north.bearing());
            int strong = SeismicModel.comparatorSignal(SeismicModel.groundMotion(100, 300, CivilDefenseConfig.seismicFalloff),
                    CivilDefenseConfig.seismicFullScale);
            helper.assertTrue(strong > 0 && comparator(helper) == strong, "the comparator follows it: " + strong + " vs " + comparator(helper));

            NeoForge.EVENT_BUS.post(new NuclearDetonationEvent(level, here.add(2000, 0, 0), 5, 30, true));
            SeismicEvent east = detector.latest().orElse(null);
            helper.assertTrue(east != null && east.dirty() && Math.abs(east.bearing() - 90) < 0.01, "a dirty bomb to the east, newest first");
            helper.assertTrue(SeismicModel.COMPASS[SeismicModel.compassPoint(east.bearing())].equals("e"), "on the east compass point");
            helper.assertTrue(comparator(helper) == strong, "the strongest recent event sets the signal");

            for (int i = 0; i < SeismicDetectorBlockEntity.CAPACITY + 2; i++) {
                NeoForge.EVENT_BUS.post(new NuclearDetonationEvent(level, here.add(-1000 - i, 0, 0), 10, 20, false));
            }
            helper.assertTrue(detector.getEvents().size() == SeismicDetectorBlockEntity.CAPACITY, "keeps the last eight");
            helper.assertTrue(Math.abs(detector.getEvents().get(0).distance() - (1000 + SeismicDetectorBlockEntity.CAPACITY + 1)) < 0.01,
                    "the newest first");
            helper.assertTrue(detector.latest().get().describe().getContents() instanceof TranslatableContents,
                    "described with translated text");
            helper.assertTrue(DisplaySource.BY_BLOCK_ENTITY.get(CivilDefenseRegistry.SEISMIC_DETECTOR_BE.get())
                    .contains(AllNuclearDisplaySources.SEISMIC_DETECTOR.get()), "a Display Link can read it");
            helper.succeed();
        });
    }

    private static int comparator(GameTestHelper helper) {
        return helper.getBlockState(DETECTOR).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(DETECTOR));
    }
}

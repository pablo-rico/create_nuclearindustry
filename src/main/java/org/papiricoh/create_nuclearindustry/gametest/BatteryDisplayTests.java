package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.BatteryDisplaySource;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;

/** The interceptor battery exposes its state, rounds and mode to a Display Link. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class BatteryDisplayTests {
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = InterceptorBatteryTests.BATCH)
    public static void aDisplayLinkReadsTheBattery(GameTestHelper helper) {
        InterceptorBatteryEntity battery = InterceptorBatteryTests.battery(helper);
        battery.loadOne(InterceptorBatteryTests.rounds(1));
        battery.setCode("AB12");
        var registered = DisplaySource.BY_BLOCK_ENTITY.get(DefenseRegistry.BATTERY_ENTITY.get());
        helper.assertTrue(registered != null && registered.contains(AllNuclearDisplaySources.INTERCEPTOR_BATTERY.get()),
                "the battery has its Display Link source");
        var lines = new BatteryDisplaySource().read(battery);
        helper.assertTrue(lines.size() == 3, "state, rounds and mode");
        // Read the arguments, not the text: a test server may not have the mod's language loaded.
        Object[] rounds = ((net.minecraft.network.chat.contents.TranslatableContents) lines.get(1).getContents()).getArgs();
        helper.assertTrue(rounds.length == 3 && Integer.valueOf(0).equals(rounds[0]) && Integer.valueOf(4).equals(rounds[1])
                && Integer.valueOf(1).equals(rounds[2]), "none ready of four, one loading");
        Object[] mode = ((net.minecraft.network.chat.contents.TranslatableContents) lines.get(2).getContents()).getArgs();
        helper.assertTrue(mode.length == 1 && "AB12".equals(mode[0]), "its radar code");
        helper.succeed();
    }
}

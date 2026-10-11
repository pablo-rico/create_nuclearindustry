package org.papiricoh.create_nuclearindustry.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;

/**
 * Runs only some game test classes: {@code ./gradlew runGameTestServer -Pgametests=AcceleratorTests,BeamTests}
 * keeps the tests of those classes (by simple name, case-insensitive) and drops the rest. Without the
 * property every test runs. The game test server copies its tests before it builds its batches, and
 * fires this event in between.
 */
@EventBusSubscriber(modid = Create_NuclearIndustry.MODID)
public final class GameTestFilter {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String PROPERTY = "create_nuclearindustry.gametests";

    private GameTestFilter() {}

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        String filter = System.getProperty(PROPERTY, "").trim();
        if (filter.isEmpty() || !(event.getServer() instanceof GameTestServer server)) {
            return;
        }
        // The tests carry no class prefix (@PrefixGameTestTemplate(false)), so match their method names.
        Set<String> names = new HashSet<>();
        for (String className : filter.split(",")) {
            try {
                Class<?> type = Class.forName(GameTestFilter.class.getPackageName() + "." + className.trim());
                for (Method method : type.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(GameTest.class)) {
                        names.add(method.getName().toLowerCase(Locale.ROOT));
                    }
                }
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Game test filter: no test class {}", className.trim());
            }
        }
        try {
            Field field = GameTestServer.class.getDeclaredField("testFunctions");
            field.setAccessible(true);
            List<TestFunction> tests = (List<TestFunction>) field.get(server);
            int before = tests.size();
            tests.removeIf(test -> !names.contains(test.testName().toLowerCase(Locale.ROOT)));
            LOGGER.info("Game test filter {}: running {} of {} tests", filter, tests.size(), before);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not filter the game tests; running them all", e);
        }
    }
}

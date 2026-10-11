package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The guide's Ponder text: every scene method takes exactly as many lines as its list holds, every
 * scene is registered, every line has both languages and reached the generated lang files, and
 * every item of this mod a scene is shown on exists.
 */
class GuidePonderTextTest {
    private static final Path SCENE_DIR = Path.of("src/main/java/org/papiricoh/create_nuclearindustry/infrastructure/ponder/scenes");
    private static final List<String> SCENE_CLASSES = List.of("CivilDefenseScenes", "WeaponsScenes", "FuelCycleScenes", "FanTreatmentScenes",
            "AcceleratorScenes");
    private static final Path REGISTRATION = Path.of("src/main/java/org/papiricoh/create_nuclearindustry/infrastructure/ponder/GuidePonderScenes.java");
    private static final Path LANG = Path.of("src/generated/resources/assets/create_nuclearindustry/lang");
    private static final Pattern SCENE_FIELD = Pattern.compile("PlantPonderText\\.Scene text = GuidePonderText\\.(\\w+);");
    private static final Pattern REGISTERED = Pattern.compile("boards\\.put\\(GuidePonderText\\.(\\w+),");
    private static final int MAX_LENGTH = 240;

    @Test
    void everySceneShowsExactlyItsLines() throws IOException, ReflectiveOperationException {
        Set<String> seen = new HashSet<>();
        for (String className : SCENE_CLASSES) {
            String source = Files.readString(SCENE_DIR.resolve(className + ".java"), StandardCharsets.UTF_8);
            for (String method : source.split("public static void ")) {
                Matcher matcher = SCENE_FIELD.matcher(method);
                if (!matcher.find()) {
                    assertEquals(0, count(method, ".showText("), className + ": a text box outside a scene method");
                    continue;
                }
                PlantPonderText.Scene scene = (PlantPonderText.Scene) GuidePonderText.class.getField(matcher.group(1)).get(null);
                int nexts = count(method, "lines.next()");
                assertEquals(count(method, ".showText("), nexts, scene.id() + ": every text box must take the next line");
                assertEquals(scene.lines().size(), nexts, scene.id() + ": text boxes and lang lines differ");
                assertTrue(seen.add(scene.id()), scene.id() + " has two scene methods");
            }
        }
        for (PlantPonderText.Scene scene : GuidePonderText.SCENES) {
            assertTrue(seen.contains(scene.id()), scene.id() + " has no scene method");
        }
    }

    @Test
    void everySceneIsRegistered() throws IOException, ReflectiveOperationException {
        String source = Files.readString(REGISTRATION, StandardCharsets.UTF_8);
        Matcher matcher = REGISTERED.matcher(source);
        Set<String> registered = new HashSet<>();
        while (matcher.find()) {
            registered.add(((PlantPonderText.Scene) GuidePonderText.class.getField(matcher.group(1)).get(null)).id());
        }
        for (PlantPonderText.Scene scene : GuidePonderText.SCENES) {
            assertTrue(registered.contains(scene.id()), scene.id() + " is not registered in GuidePonderScenes");
            assertFalse(scene.components().isEmpty(), scene.id() + " is shown on no item");
        }
    }

    @Test
    void everyLineHasBothLanguages() {
        Set<String> ids = new HashSet<>();
        PlantPonderText.SCENES.forEach(scene -> ids.add(scene.id()));
        for (PlantPonderText.Scene scene : GuidePonderText.SCENES) {
            assertTrue(ids.add(scene.id()), "duplicate scene id " + scene.id());
            check(scene.id() + " title", scene.title());
            assertFalse(scene.lines().isEmpty(), scene.id());
            for (int i = 0; i < scene.lines().size(); i++) {
                check(scene.textKey(i + 1), scene.lines().get(i));
            }
        }
    }

    @Test
    void everyLineReachedTheLangFiles() throws IOException {
        for (String file : new String[]{"en_us.json", "es_es.json"}) {
            String json = Files.readString(LANG.resolve(file), StandardCharsets.UTF_8);
            for (PlantPonderText.Scene scene : GuidePonderText.SCENES) {
                assertTrue(json.contains("\"" + scene.headerKey() + "\""), file + " lacks " + scene.headerKey() + " (run runData)");
                for (int i = 1; i <= scene.lines().size(); i++) {
                    assertTrue(json.contains("\"" + scene.textKey(i) + "\""), file + " lacks " + scene.textKey(i) + " (run runData)");
                }
                assertFalse(json.contains("\"" + scene.textKey(scene.lines().size() + 1) + "\""), file + " has a line too many for " + scene.id());
            }
        }
    }

    @Test
    void everyComponentOfTheModExists() {
        for (PlantPonderText.Scene scene : GuidePonderText.SCENES) {
            assertEquals(new HashSet<>(scene.components()).size(), scene.components().size(), scene.id() + " lists an item twice");
            for (String component : scene.components()) {
                if (component.contains(":")) {
                    assertTrue(component.startsWith("create:"), scene.id() + " is shown on an item of a mod it may lack: " + component);
                    continue;
                }
                String model = "assets/create_nuclearindustry/models/item/" + component + ".json";
                assertTrue(Files.exists(Path.of("src/generated/resources", model)) || Files.exists(Path.of("src/main/resources", model)),
                        "no item model for " + component + " of " + scene.id());
            }
        }
    }

    private static void check(String what, PlantPonderText.Line line) {
        assertFalse(line.english().isBlank(), what + " has no English");
        assertFalse(line.spanish().isBlank(), what + " has no Spanish");
        assertNotEquals(line.english(), line.spanish(), what + " is not translated");
        assertTrue(line.english().length() <= MAX_LENGTH && line.spanish().length() <= MAX_LENGTH, what + " is too long for a text box");
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }
}

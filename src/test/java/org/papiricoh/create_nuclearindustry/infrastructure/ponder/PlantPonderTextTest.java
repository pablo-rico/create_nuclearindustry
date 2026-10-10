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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plant series' text: every scene method takes exactly as many lines as its list holds (the
 * lang keys {@code text_N} are numbered from that list), every line has both languages, and every
 * item named for a scene or the tag has a model, so it exists.
 */
class PlantPonderTextTest {
    private static final Path SCENES = Path.of("src/main/java/org/papiricoh/create_nuclearindustry/infrastructure/ponder/scenes/PlantScenes.java");
    private static final Pattern SCENE_FIELD = Pattern.compile("PlantPonderText\\.Scene text = PlantPonderText\\.(\\w+);");
    private static final int MAX_LENGTH = 240;

    @Test
    void everySceneShowsExactlyItsLines() throws IOException, ReflectiveOperationException {
        String source = Files.readString(SCENES, StandardCharsets.UTF_8);
        Set<String> seen = new HashSet<>();
        for (String method : source.split("public static void ")) {
            Matcher matcher = SCENE_FIELD.matcher(method);
            if (!matcher.find()) {
                continue;
            }
            PlantPonderText.Scene scene = (PlantPonderText.Scene) PlantPonderText.class.getField(matcher.group(1)).get(null);
            int nexts = count(method, "lines.next()");
            int boxes = count(method, ".showText(");
            assertEquals(boxes, nexts, scene.id() + ": every text box must take the next line");
            assertEquals(scene.lines().size(), nexts, scene.id() + ": text boxes and lang lines differ");
            seen.add(scene.id());
        }
        for (PlantPonderText.Scene scene : PlantPonderText.SCENES) {
            assertTrue(seen.contains(scene.id()), scene.id() + " has no scene method");
        }
    }

    @Test
    void everyLineHasBothLanguages() {
        Set<String> ids = new HashSet<>();
        for (PlantPonderText.Scene scene : PlantPonderText.SCENES) {
            assertTrue(ids.add(scene.id()), "duplicate scene id " + scene.id());
            assertTrue(scene.id().startsWith("plant_"), scene.id());
            check(scene.id() + " title", scene.title());
            assertFalse(scene.lines().isEmpty(), scene.id());
            for (int i = 0; i < scene.lines().size(); i++) {
                check(scene.textKey(i + 1), scene.lines().get(i));
            }
        }
        check("tag", PlantPonderText.TAG_TITLE);
        check("tag description", PlantPonderText.TAG_DESCRIPTION);
    }

    @Test
    void keysFollowPonderNaming() {
        assertEquals("create_nuclearindustry.ponder.plant_heat_loop.header", PlantPonderText.HEAT_LOOP.headerKey());
        assertEquals("create_nuclearindustry.ponder.plant_heat_loop.text_1", PlantPonderText.HEAT_LOOP.textKey(1));
        assertEquals("create_nuclearindustry.ponder.tag.nuclear_power_plant", PlantPonderText.TAG_KEY);
    }

    @Test
    void scriptsHandOutLinesInOrderAndNeverThrow() {
        PlantPonderText.Script script = PlantPonderText.WASTE.script();
        for (PlantPonderText.Line line : PlantPonderText.WASTE.lines()) {
            assertEquals(line.english(), script.next());
        }
        assertEquals("", script.next());
    }

    @Test
    void everyComponentIsAnItemOfTheMod() {
        List<String> components = PlantPonderText.tagComponents();
        assertEquals(new HashSet<>(components).size(), components.size(), "duplicate tag components");
        for (PlantPonderText.Scene scene : PlantPonderText.SCENES) {
            assertTrue(components.containsAll(PlantPonderText.componentsOf(scene)), scene.id() + " items must be tagged");
            assertTrue(PlantPonderText.componentsOf(scene).containsAll(PlantPonderText.HUBS), "hubs show every scene");
        }
        for (String id : components) {
            assertTrue(itemModelExists(id), "no item model for " + id);
        }
    }

    private static boolean itemModelExists(String id) {
        String model = "assets/create_nuclearindustry/models/item/" + id + ".json";
        return Files.exists(Path.of("src/generated/resources", model)) || Files.exists(Path.of("src/main/resources", model));
    }

    private static void check(String what, PlantPonderText.Line line) {
        assertNotNull(line, what);
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

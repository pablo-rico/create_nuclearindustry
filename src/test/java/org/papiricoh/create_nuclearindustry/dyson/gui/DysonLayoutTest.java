package org.papiricoh.create_nuclearindustry.dyson.gui;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The swarm's screens keep their regions on their sheets and apart, and the generator paints them from the same numbers. */
class DysonLayoutTest {
    private static void onSheetAndApart(List<LayoutRect> regions, int width, int height) {
        for (LayoutRect r : regions) {
            assertTrue(r.w() > 0 && r.h() > 0, r.name());
            assertTrue(r.x() >= 0 && r.y() >= 0 && r.right() <= width && r.bottom() <= height, r.name() + " on the sheet");
        }
        for (int i = 0; i < regions.size(); i++)
            for (int j = i + 1; j < regions.size(); j++)
                assertFalse(regions.get(i).overlaps(regions.get(j)), regions.get(i).name() + " and " + regions.get(j).name() + " overlap");
    }

    @Test
    void theEjectorScreenFits() {
        onSheetAndApart(EjectorLayout.ALL, EjectorLayout.WIDTH, EjectorLayout.HEIGHT);
        List<LayoutRect> all = new ArrayList<>(EjectorLayout.ALL);
        all.addAll(EjectorLayout.SPRITES);
        onSheetAndApart(all, EjectorLayout.SHEET, EjectorLayout.SHEET);
        for (int i = 0; i < 16; i++) {
            LayoutRect frame = new LayoutRect("sail" + i, EjectorLayout.SAIL_X - 1 + (i % 8) * 18, EjectorLayout.SAIL_Y - 1 + (i / 8) * 18, 18, 18);
            assertTrue(EjectorLayout.SAILS.encloses(frame), frame.name());
        }
        LayoutRect inventory = new LayoutRect("inventory_slots", EjectorLayout.INV_X - 1, EjectorLayout.INV_Y - 1, 9 * 18,
                EjectorLayout.HOTBAR_Y + 18 - EjectorLayout.INV_Y + 1);
        assertTrue(EjectorLayout.INVENTORY.encloses(inventory));
        assertTrue(EjectorLayout.HOTBAR_Y >= EjectorLayout.INV_Y + 3 * 18);
        assertEquals(EjectorLayout.SPRITE_ORBIT.h(), 3 * EjectorLayout.ORBIT_0.h());
        assertEquals(EjectorLayout.SPRITE_ORBIT.w(), EjectorLayout.ORBIT_0.w());
        assertEquals(EjectorLayout.SPRITE_CHARGE.w(), EjectorLayout.CHARGE.w());
        assertEquals(EjectorLayout.SPRITE_CHARGE.h(), EjectorLayout.CHARGE.h());
    }

    @Test
    void theTerminalScreenFits() {
        onSheetAndApart(TerminalLayout.ALL, TerminalLayout.WIDTH, TerminalLayout.HEIGHT);
        onSheetAndApart(TerminalLayout.PANEL_PARTS, TerminalLayout.WIDTH, TerminalLayout.HEIGHT);
        for (LayoutRect part : TerminalLayout.PANEL_PARTS) assertTrue(TerminalLayout.PANEL.encloses(part), part.name());
        List<LayoutRect> all = new ArrayList<>(TerminalLayout.ALL);
        all.addAll(TerminalLayout.SPRITES);
        onSheetAndApart(all, TerminalLayout.SHEET, TerminalLayout.SHEET);
        assertEquals(TerminalLayout.SPRITE_TAB.h(), 2 * TerminalLayout.TAB_0.h());
        assertEquals(TerminalLayout.SPRITE_TOGGLE.w(), TerminalLayout.ENABLE.w());
        assertEquals(TerminalLayout.SPRITE_TOGGLE.h(), 2 * TerminalLayout.ENABLE.h());
        assertEquals(TerminalLayout.SPRITE_SLIDER.w(), TerminalLayout.RADIUS.w());
        int r = TerminalLayout.SKY_RADIUS;
        assertTrue(TerminalLayout.SKY.encloses(new LayoutRect("plot", TerminalLayout.SKY_CX - r, TerminalLayout.SKY_CY - r, 2 * r, 2 * r)));
        assertTrue(40 * TerminalLayout.OrbitShapeScale.PIXELS_PER_DEGREE < r, "the outermost orbit inside the plot");
    }

    private static Map<String, int[]> pythonConstants() throws IOException {
        Path file = Path.of("tools", "texturegen", "dyson_layouts.py");
        assertTrue(Files.exists(file), "run from the project directory: " + file.toAbsolutePath());
        Map<String, int[]> values = new HashMap<>();
        Pattern line = Pattern.compile("^([A-Z][A-Z0-9_]*)\\s*=\\s*\\(?([-0-9, ]+?)\\)?\\s*$");
        for (String text : Files.readAllLines(file)) {
            Matcher m = line.matcher(text.strip());
            if (!m.matches()) continue;
            values.put(m.group(1), Arrays.stream(m.group(2).split(",")).map(String::trim).filter(s -> !s.isEmpty()).mapToInt(Integer::parseInt).toArray());
        }
        return values;
    }

    private static void mirrors(Class<?> layout, String prefix, Map<String, int[]> python) throws IllegalAccessException {
        int checked = 0;
        for (Field field : layout.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            Object value = field.get(null);
            int[] expected;
            if (value instanceof LayoutRect r) expected = new int[] {r.x(), r.y(), r.w(), r.h()};
            else if (value instanceof Integer i) expected = new int[] {i};
            else continue;
            int[] actual = python.get(prefix + field.getName());
            assertNotNull(actual, prefix + field.getName() + " missing from dyson_layouts.py");
            assertTrue(Arrays.equals(expected, actual), prefix + field.getName() + ": Java " + Arrays.toString(expected) + ", Python " + Arrays.toString(actual));
            checked++;
        }
        assertTrue(checked > 10, "checked " + checked + " constants of " + layout.getSimpleName());
    }

    @Test
    void theGeneratorPaintsFromTheSameNumbers() throws Exception {
        Map<String, int[]> python = pythonConstants();
        mirrors(EjectorLayout.class, "EJECTOR_", python);
        mirrors(TerminalLayout.class, "TERMINAL_", python);
    }
}

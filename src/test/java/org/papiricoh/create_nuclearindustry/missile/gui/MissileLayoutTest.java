package org.papiricoh.create_nuclearindustry.missile.gui;

import org.junit.jupiter.api.Test;

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

/**
 * The launch controller's and command table's regions stay on their sheets and apart, and the texture generator paints
 * them from the same numbers as the screens draw over.
 */
class MissileLayoutTest {

    private static void onSheetAndApart(List<LayoutRect> regions, int width, int height) {
        for (LayoutRect r : regions) {
            assertTrue(r.w() > 0 && r.h() > 0, r.name());
            assertTrue(r.x() >= 0 && r.y() >= 0 && r.right() <= width && r.bottom() <= height, r.name() + " on the sheet");
        }
        for (int i = 0; i < regions.size(); i++)
            for (int j = i + 1; j < regions.size(); j++)
                assertFalse(regions.get(i).overlaps(regions.get(j)), regions.get(i).name() + " and " + regions.get(j).name() + " overlap");
    }

    private static List<LayoutRect> join(List<LayoutRect> a, List<LayoutRect> b) {
        List<LayoutRect> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    @Test
    void eachControllerTabStaysOnItsSheetAndApart() {
        onSheetAndApart(join(SiloLayout.COMMON, SiloLayout.ROCKET), SiloLayout.WIDTH, SiloLayout.HEIGHT);
        onSheetAndApart(join(SiloLayout.COMMON, SiloLayout.LAUNCH), SiloLayout.WIDTH, SiloLayout.HEIGHT);
        onSheetAndApart(SiloLayout.TARGET_PARTS, SiloLayout.WIDTH, SiloLayout.HEIGHT);
        for (LayoutRect part : SiloLayout.TARGET_PARTS) assertTrue(SiloLayout.TARGET.encloses(part), part.name() + " in the target box");
        assertTrue(SiloLayout.SECTION.encloses(SiloLayout.SECTION_STATS));
        onSheetAndApart(SiloLayout.SPRITES, SiloLayout.WIDGETS_SIZE, SiloLayout.WIDGETS_SIZE);
    }

    @Test
    void theSectionSlotsFitTheirPanelWithoutTouching() {
        assertEquals(SiloLayout.SLOT_X.length, SiloLayout.SLOT_Y.length);
        List<LayoutRect> frames = new ArrayList<>();
        for (int i = 0; i < SiloLayout.SLOT_X.length; i++) {
            LayoutRect frame = new LayoutRect("slot" + i, SiloLayout.SLOT_X[i] - 1, SiloLayout.SLOT_Y[i] - 1, 18, 18);
            assertTrue(SiloLayout.SECTION.encloses(frame), frame.name() + " in the section panel");
            assertTrue(frame.y() >= SiloLayout.SECTION.y() + SiloLayout.SECTION_TITLE, frame.name() + " under the title strip");
            assertFalse(frame.overlaps(SiloLayout.SECTION_STATS), frame.name() + " clear of the figures");
            frames.add(frame);
        }
        onSheetAndApart(frames, SiloLayout.WIDTH, SiloLayout.HEIGHT);
        LayoutRect inventory = new LayoutRect("inventory_slots", SiloLayout.INV_X - 1, SiloLayout.INV_Y - 1, 9 * 18, SiloLayout.HOTBAR_Y + 18 - SiloLayout.INV_Y + 1);
        assertTrue(SiloLayout.INVENTORY.encloses(inventory));
        assertTrue(SiloLayout.HOTBAR_Y >= SiloLayout.INV_Y + 3 * 18);
    }

    @Test
    void theDiagramHoldsTheTallestRocketAndAGhost() {
        assertTrue(SiloLayout.cellY(SiloLayout.CELLS - 1) >= SiloLayout.DIAGRAM.y(), "the top cell in the diagram");
        assertTrue(SiloLayout.baseY() + SiloLayout.BASE_H <= SiloLayout.DIAGRAM.bottom());
        assertTrue(SiloLayout.cellX() - 2 > SiloLayout.BRACKET_X + SiloLayout.BRACKET_W, "the bracket left of the cells");
        assertTrue(SiloLayout.cellX() + SiloLayout.CELL_W < SiloLayout.LABEL_X, "the labels right of the cells");
        assertTrue(SiloLayout.COL_CX - SiloLayout.BASE_W / 2 >= SiloLayout.DIAGRAM.x() && SiloLayout.COL_CX + SiloLayout.BASE_W / 2 <= SiloLayout.DIAGRAM.right());
        assertEquals(SiloLayout.STAGES.h(), SiloLayout.STAGE_HEADER + 3 * SiloLayout.STAGE_ROW);
        assertTrue(SiloLayout.C_REACH < SiloLayout.STAGES.w() - 20);
        assertEquals(SiloLayout.SPRITE_CELLS.w(), 4 * SiloLayout.CELL_W);
        assertEquals(SiloLayout.SPRITE_CELLS.h(), 4 * SiloLayout.CELL_H);
        assertEquals(SiloLayout.SPRITE_BUTTONS.w(), SiloLayout.ARM.w());
        assertEquals(SiloLayout.SPRITE_BUTTONS.h(), 4 * SiloLayout.ARM.h());
        assertEquals(SiloLayout.SPRITE_TOGGLES.w(), SiloLayout.AIRBURST.w());
        assertEquals(SiloLayout.SPRITE_TOGGLES.h(), 2 * SiloLayout.AIRBURST.h());
    }

    @Test
    void theTableStaysOnItsSheetAndApart() {
        onSheetAndApart(HoloLayout.ALL, HoloLayout.WIDTH, HoloLayout.HEIGHT);
        assertTrue(HoloLayout.MAP_FRAME.encloses(HoloLayout.MAP));
        assertEquals(HoloLayout.LIST.h(), HoloLayout.LIST_HEADER + HoloLayout.LIST_ROW * HoloLayout.LIST_ROWS);
        assertTrue(HoloLayout.COL_UNLINK + 8 <= HoloLayout.LIST.w());
        onSheetAndApart(HoloLayout.SPRITES, HoloLayout.WIDGETS_W, HoloLayout.WIDGETS_H);
        assertEquals(HoloLayout.SPRITE_BUTTONS.w(), HoloLayout.ARM.w());
    }

    /** {@code NAME = 12} and {@code NAME = (1, 2, 3)} lines of the generator's mirror. */
    private static Map<String, int[]> pythonConstants() throws IOException {
        Path file = Path.of("tools", "texturegen", "missile_layouts.py");
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
            else if (value instanceof int[] a && field.getName().startsWith("SLOT_")) expected = a;
            else continue; // colours and lists are code-only
            if (field.getName().endsWith("_COLOUR") || field.getName().equals("STAGE_COLOURS")) continue;
            int[] actual = python.get(prefix + field.getName());
            assertNotNull(actual, prefix + field.getName() + " missing from missile_layouts.py");
            assertTrue(Arrays.equals(expected, actual), prefix + field.getName() + ": Java " + Arrays.toString(expected) + ", Python " + Arrays.toString(actual));
            checked++;
        }
        assertTrue(checked > 10, "checked " + checked + " constants of " + layout.getSimpleName());
    }

    @Test
    void theBatteryStaysOnItsSheetAndApart() {
        onSheetAndApart(BatteryLayout.ALL, BatteryLayout.WIDTH, BatteryLayout.HEIGHT);
        assertEquals(BatteryLayout.SLOT_X.length, BatteryLayout.SLOT_Y.length);
        for (int i = 0; i < BatteryLayout.SLOT_X.length; i++) {
            LayoutRect frame = new LayoutRect("tube" + i, BatteryLayout.SLOT_X[i] - 1, BatteryLayout.SLOT_Y[i] - 1, 18, 18);
            assertTrue(BatteryLayout.TUBES.encloses(frame), frame.name() + " in the tube frame");
        }
        LayoutRect inventory = new LayoutRect("inventory_slots", BatteryLayout.INV_X - 1, BatteryLayout.INV_Y - 1, 9 * 18,
                BatteryLayout.HOTBAR_Y + 18 - BatteryLayout.INV_Y + 1);
        assertTrue(BatteryLayout.INVENTORY.encloses(inventory));
        assertTrue(BatteryLayout.HOTBAR_Y >= BatteryLayout.INV_Y + 3 * 18);
    }

    @Test
    void theGroundStationStaysOnItsSheetAndApart() {
        onSheetAndApart(StationLayout.ALL, StationLayout.WIDTH, StationLayout.HEIGHT);
        LayoutRect sky = new LayoutRect("sky_plot", StationLayout.SKY_CX - StationLayout.SKY_RADIUS - 2, StationLayout.SKY_CY - StationLayout.SKY_RADIUS - 2,
                2 * StationLayout.SKY_RADIUS + 4, 2 * StationLayout.SKY_RADIUS + 4);
        assertTrue(StationLayout.SKY.encloses(sky), "the sky plot in its window");
    }

    @Test
    void theTableKeepsItsSatelliteSprites() {
        assertEquals(HoloLayout.SPRITE_SAT_KINDS.w(), 2 * 8);
        assertEquals(HoloLayout.SPRITE_SAT_KINDS.h(), 2 * 8);
        assertTrue(HoloLayout.SCALE_INSET > HoloLayout.SCALE_W && HoloLayout.SCALE_INSET < HoloLayout.TITLE.w() / 2, "the scale window in the title strip");
        assertEquals(SiloLayout.SPRITE_CELL_SATELLITE.w(), 4 * SiloLayout.CELL_W);
        assertEquals(SiloLayout.SPRITE_GHOSTS.w(), 16 * (SiloLayout.GHOST_SATELLITE + 1));
    }

    @Test
    void theTextureGeneratorUsesTheSameNumbers() throws IOException, IllegalAccessException {
        Map<String, int[]> python = pythonConstants();
        mirrors(SiloLayout.class, "SILO_", python);
        mirrors(HoloLayout.class, "HOLO_", python);
        mirrors(BatteryLayout.class, "BATTERY_", python);
        mirrors(StationLayout.class, "STATION_", python);
    }
}

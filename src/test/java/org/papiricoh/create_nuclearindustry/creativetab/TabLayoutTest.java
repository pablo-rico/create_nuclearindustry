package org.papiricoh.create_nuclearindustry.creativetab;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Laying the creative tab out in sections with a banner row before each. */
class TabLayoutTest {
    private static final List<String> ORDER = List.of("a", "b", "c", "other");

    private static TabLayout.Layout<String, String> layout(List<String> items) {
        // An item's section is its first letter; anything unknown goes to "other".
        return TabLayout.build(items, item -> item.substring(0, 1), ORDER, "other", "");
    }

    @Test
    void eachSectionOpensWithABannerRowAndFillsItsLastRow() {
        TabLayout.Layout<String, String> layout = layout(List.of("a1", "a2", "b1"));
        List<String> slots = layout.slots();
        assertEquals(4 * TabLayout.COLUMNS, slots.size(), "banner + a row, banner + b row");
        assertEquals(Collections.nCopies(9, ""), slots.subList(0, 9));
        assertEquals(List.of("a1", "a2"), slots.subList(9, 11));
        assertEquals(Collections.nCopies(7, ""), slots.subList(11, 18));
        assertEquals(Collections.nCopies(9, ""), slots.subList(18, 27));
        assertEquals("b1", slots.get(27));
        assertEquals(Map.of(0, "a", 2, "b"), layout.headerRows());
    }

    @Test
    void aFullRowGetsNoPadding() {
        List<String> nine = List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9");
        TabLayout.Layout<String, String> layout = layout(nine);
        assertEquals(2 * TabLayout.COLUMNS, layout.slots().size());
    }

    @Test
    void sectionsFollowTheOrderNotTheInput() {
        TabLayout.Layout<String, String> layout = layout(List.of("c1", "a1", "c2", "b1"));
        assertEquals(List.of("a", "b", "c"), List.copyOf(layout.headerRows().values()));
        int cRow = layout.headerRows().entrySet().stream().filter(e -> e.getValue().equals("c")).findFirst().orElseThrow().getKey();
        assertEquals(List.of("c1", "c2"), layout.slots().subList((cRow + 1) * 9, (cRow + 1) * 9 + 2), "kept in input order");
    }

    @Test
    void emptySectionsAreLeftOutAndUnknownItemsGoLast() {
        TabLayout.Layout<String, String> layout = layout(List.of("z1", "a1"));
        assertEquals(Map.of(0, "a", 2, "other"), layout.headerRows());
        assertEquals("z1", layout.slots().get(3 * 9));
    }

    @Test
    void anEmptyTabHasNoBanners() {
        assertTrue(layout(List.of()).slots().isEmpty());
        assertTrue(layout(List.of()).headerRows().isEmpty());
    }

    @Test
    void everySectionHasABannerSheetOfTheRightSize() throws IOException {
        for (NuclearTabSection section : NuclearTabSection.values()) {
            File file = new File("src/main/resources/assets/create_nuclearindustry/textures/gui/creative_section/" + section.id() + ".png");
            assertTrue(file.isFile(), "no banner for " + section.id());
            BufferedImage image = ImageIO.read(file);
            assertNotNull(image, section.id());
            assertEquals(NuclearTabSection.WIDTH, image.getWidth(), section.id());
            assertEquals(NuclearTabSection.HEIGHT * NuclearTabSection.FRAMES, image.getHeight(), section.id());
        }
    }
}

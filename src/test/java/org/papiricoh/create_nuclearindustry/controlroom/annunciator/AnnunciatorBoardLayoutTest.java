package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout.Box;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout.Button;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout.Hit;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout.HitKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The face of an annunciator board: windows, strips, buttons and what a click lands on. */
class AnnunciatorBoardLayoutTest {

    private static boolean overlaps(Box a, Box b) {
        return a.x0() < b.x1() && b.x0() < a.x1() && a.y0() < b.y1() && b.y0() < a.y1();
    }

    private static float cx(Box box) {
        return (box.x0() + box.x1()) / 2;
    }

    private static float cy(Box box) {
        return (box.y0() + box.y1()) / 2;
    }

    @ParameterizedTest
    @CsvSource({"1,1", "4,1", "3,2", "16,1", "2,8"})
    void everyWindowIsHitWhereItIsDrawn(int width, int height) {
        AnnunciatorBoardLayout layout = new AnnunciatorBoardLayout(width, height);
        for (int bx = 0; bx < width; bx++) {
            for (int by = 0; by < height; by++) {
                for (int i = 0; i < 6; i++) {
                    Box window = layout.window(bx, by, i);
                    Hit hit = layout.hit(cx(window), cy(window));
                    assertEquals(new Hit(HitKind.WINDOW, bx, by, i), hit);
                    assertTrue(window.x0() >= bx * 16 && window.x1() <= bx * 16 + 16, "inside its panel");
                    assertTrue(window.y0() >= by * 16 && window.y1() <= by * 16 + 13, "above the strip");
                    assertFalse(overlaps(window, layout.strip(bx, by)));
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "4,1", "3,2", "16,1"})
    void theButtonsSitAtTheRightOfTheBottomStripOnly(int width, int height) {
        AnnunciatorBoardLayout layout = new AnnunciatorBoardLayout(width, height);
        float previous = -1;
        for (Button button : Button.values()) {
            Box box = layout.button(button);
            assertTrue(box.x0() > previous, "left to right, apart: " + button);
            previous = box.x1();
            assertEquals((height - 1) * 16 + 13, box.y0(), 1e-6, "in the bottom strip");
            assertTrue(box.x0() >= 0 && box.x1() <= width * 16 - 1, "on the board");
            assertSame(button, layout.hit(cx(box), cy(box)).button());
            for (int bx = 0; bx < width; bx++) {
                for (int i = 0; i < 6; i++) {
                    assertFalse(overlaps(box, layout.window(bx, height - 1, i)));
                }
            }
        }
    }

    @Test
    void aLonePanelSqueezesTheFourButtonsIn() {
        AnnunciatorBoardLayout layout = new AnnunciatorBoardLayout(1, 1);
        assertTrue(layout.button(Button.TEST).x0() >= 1);
        assertNull(layout.nameplate(0, 0), "no room left for a nameplate");
    }

    @Test
    void nameplatesFillTheStripsTheButtonsLeave() {
        AnnunciatorBoardLayout layout = new AnnunciatorBoardLayout(4, 2);
        for (int bx = 0; bx < 4; bx++) {
            Box top = layout.nameplate(bx, 0);
            assertNotNull(top, "every upper panel has its nameplate");
            assertEquals(14, top.width(), 1e-6);
            assertEquals(HitKind.NAMEPLATE, layout.hit(cx(top), cy(top)).kind());
        }
        assertNotNull(layout.nameplate(0, 1));
        assertNotNull(layout.nameplate(1, 1));
        Box last = layout.nameplate(3, 1);
        assertNull(last, "the buttons take the last panel's strip");
        for (int bx = 0; bx < 4; bx++) {
            Box plate = layout.nameplate(bx, 1);
            if (plate != null) {
                for (Button button : Button.values()) {
                    assertFalse(overlaps(plate, layout.button(button)));
                }
            }
        }
    }

    @Test
    void clicksBetweenThingsAndOffTheBoardHitNothing() {
        AnnunciatorBoardLayout layout = new AnnunciatorBoardLayout(2, 1);
        assertEquals(HitKind.NONE, layout.hit(8, 2).kind(), "between the columns");
        assertEquals(HitKind.NONE, layout.hit(0.5f, 6).kind(), "the frame");
        assertEquals(HitKind.NONE, layout.hit(-1, 6).kind());
        assertEquals(HitKind.NONE, layout.hit(40, 6).kind());
        assertEquals(HitKind.NONE, layout.hit(10, 16).kind());
    }
}

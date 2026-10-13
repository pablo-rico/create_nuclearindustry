package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * Where things go on an LED display's window, in pixels (1/16 block) from its top-left corner as the
 * operator reads it, y downwards. A display one module wide shows four digits over its unit; a wider
 * one puts a label strip on top and the unit to the right of up to {@link #MAX_DIGITS} digits.
 *
 * @param width        window width: each module is 8 px wide, less a 0.5 px margin at either end
 * @param height       window height
 * @param digits       how many digits
 * @param digitsRight  right edge of the last digit
 * @param digitsTop    top of the digits
 * @param digitWidth   width of one digit
 * @param digitHeight  height of one digit
 * @param pitch        distance from one digit to the next
 * @param label        whether there is a label strip
 * @param labelHeight  height of the label strip, from {@link #MARGIN} down
 * @param unitRight    right edge of the unit
 * @param unitTop      top of the unit
 * @param unitHeight   height of the unit's text
 */
public record LedLayout(float width, float height, int digits, float digitsRight, float digitsTop, float digitWidth,
                        float digitHeight, float pitch, boolean label, float labelHeight, float unitRight, float unitTop,
                        float unitHeight) {
    public static final float CELL = 8;
    public static final float HEIGHT = 6;
    public static final float MARGIN = 0.5f;
    public static final int MAX_DIGITS = 8;
    private static final float UNIT_WIDTH = 3;
    private static final float MIN_PITCH = 1.8f;
    private static final float MAX_PITCH = 2.6f;

    public static LedLayout of(int cells) {
        float width = Math.max(1, cells) * CELL - 2 * MARGIN;
        if (cells <= 1) {
            return new LedLayout(width, HEIGHT, 4, width - MARGIN, 0.6f, 1.05f, 2.6f, 1.5f, false, 0,
                    width - MARGIN, 3.7f, 1.6f);
        }
        float digitHeight = 3.2f;
        float right = width - MARGIN - UNIT_WIDTH - 0.3f;
        float room = right - MARGIN;
        int digits = Math.max(4, Math.min(MAX_DIGITS, (int) (room / MIN_PITCH)));
        float pitch = Math.min(MAX_PITCH, room / digits);
        return new LedLayout(width, HEIGHT, digits, right, 2.2f, digitHeight * 0.55f, digitHeight, pitch, true, 1.4f,
                width - MARGIN, 2.2f + digitHeight - 1.6f, 1.6f);
    }

    /** Left edge of digit {@code i}, counting from the left. */
    public float digitLeft(int i) {
        return digitsRight - (digits - i) * pitch + (pitch - digitWidth);
    }
}

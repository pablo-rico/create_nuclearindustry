package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * Which of a seven-segment digit's bars light for a character. Bit 0 is the top bar (a), then
 * clockwise: top right (b), bottom right (c), bottom (d), bottom left (e), top left (f), and bit 6
 * the middle bar (g). Characters a display cannot show are blank.
 */
public final class SevenSegment {
    public static final int A = 1, B = 1 << 1, C = 1 << 2, D = 1 << 3, E = 1 << 4, F = 1 << 5, G = 1 << 6;
    public static final int ALL = 0x7F;
    private static final int[] DIGITS = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};

    private SevenSegment() {}

    public static int mask(char c) {
        if (c >= '0' && c <= '9') {
            return DIGITS[c - '0'];
        }
        return switch (c) {
            case '-' -> G;
            case 'O' -> DIGITS[0];
            case 'E' -> A | D | E | F | G;
            case 'r' -> E | G;
            case 'o' -> C | D | E | G;
            case 'L' -> D | E | F;
            case 'H' -> B | C | E | F | G;
            default -> 0;
        };
    }

    public static boolean lit(int mask, int segment) {
        return (mask & segment) != 0;
    }
}

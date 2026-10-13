package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * Cuts a run of LED modules side by side, counted left to right as the operator sees them, into
 * displays of at most {@code max} modules from its left end.
 */
public final class LedRows {
    private LedRows() {}

    /** A display: {@code length} cells of the run starting at cell {@code start}, the leftmost being its head. */
    public record Span(int start, int length) {
        public boolean contains(int cell) {
            return cell >= start && cell < start + length;
        }
    }

    /** The display that cell {@code index} of a run of {@code runLength} LED modules belongs to. */
    public static Span pieceOf(int index, int runLength, int max) {
        int start = index - index % max;
        return new Span(start, Math.min(max, runLength - start));
    }
}

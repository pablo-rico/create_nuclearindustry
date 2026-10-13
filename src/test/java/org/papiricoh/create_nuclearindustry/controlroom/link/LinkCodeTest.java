package org.papiricoh.create_nuclearindustry.controlroom.link;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Advanced Redstone Link codes: four capitals or digits, whatever was typed or sent. */
class LinkCodeTest {

    @Test
    void codesAreCapitalsAndDigits() {
        assertEquals("AB12", LinkCode.sanitize("ab-12c"));
        assertEquals("Z9", LinkCode.sanitize(" z 9 "));
        assertEquals("", LinkCode.sanitize(null));
        assertEquals("", LinkCode.sanitize("ñ§€"), "letters outside A-Z are dropped");
        assertEquals("ABCD", LinkCode.sanitize("abcdefgh"), "at most four");
    }

    @Test
    void onlyFullCodesLink() {
        assertTrue(LinkCode.complete("AB12"));
        assertFalse(LinkCode.complete("AB1"));
        assertFalse(LinkCode.complete("ab12"), "kept codes are in capitals");
        assertFalse(LinkCode.complete(""));
        assertFalse(LinkCode.complete(null));
    }

    @Test
    void typingTakesLettersAndDigits() {
        assertTrue(LinkCode.allowed('a'));
        assertTrue(LinkCode.allowed('Q'));
        assertTrue(LinkCode.allowed('7'));
        assertFalse(LinkCode.allowed('-'));
        assertFalse(LinkCode.allowed(' '));
    }
}

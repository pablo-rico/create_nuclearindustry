package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IllegalFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Ponder runs every scene text through {@link String#format}, so a bare "%" (as in "80% a")
 * shows "Format error" in game. Every ponder line in both languages must format cleanly.
 */
class PonderLangFormatTest {
    private static final Path LANG = Path.of("src/generated/resources/assets/create_nuclearindustry/lang");
    private static final Pattern ENTRY = Pattern.compile("\"(create_nuclearindustry\\.ponder\\.[^\"]+)\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Test
    void everyPonderLineFormats() throws IOException {
        for (String file : new String[]{"en_us.json", "es_es.json"}) {
            String json = Files.readString(LANG.resolve(file), StandardCharsets.UTF_8);
            Matcher matcher = ENTRY.matcher(json);
            int lines = 0;
            while (matcher.find()) {
                lines++;
                try {
                    String.format(matcher.group(2));
                } catch (IllegalFormatException e) {
                    fail(file + " " + matcher.group(1) + ": " + e.getMessage() + " (write a percent sign as %%)");
                }
            }
            assertTrue(lines > 0, "found the ponder lines in " + file);
        }
    }
}

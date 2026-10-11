package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.network.chat.Component;

import java.util.Arrays;

/**
 * A translatable message kept as a plain string, so validation results (which are saved and synced
 * as strings) can carry text in the player's language: a lang key followed by its arguments,
 * separated by {@code |}. Plain text from older saves or other code still reads as itself.
 */
public final class MultiblockText {
    private static final String MOD_KEY_PREFIX = "create_nuclearindustry.";

    private MultiblockText() {}

    /** A lang key and its arguments, as stored in a validation result. */
    public static String message(String key, Object... args) {
        StringBuilder out = new StringBuilder(key);
        for (Object arg : args) {
            out.append('|').append(arg);
        }
        return out.toString();
    }

    /** The lang key of a message from {@link #message}. */
    public static String key(String encoded) {
        int bar = encoded.indexOf('|');
        return bar < 0 ? encoded : encoded.substring(0, bar);
    }

    /** A message from {@link #message}, or plain text from elsewhere, as a component. */
    public static Component text(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return Component.empty();
        }
        if (!encoded.startsWith(MOD_KEY_PREFIX)) {
            return Component.literal(encoded);
        }
        String[] parts = encoded.split("\\|");
        return Component.translatable(parts[0], (Object[]) Arrays.copyOfRange(parts, 1, parts.length));
    }
}

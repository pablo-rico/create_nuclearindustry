package org.papiricoh.create_nuclearindustry.network;

import java.util.function.BooleanSupplier;

/**
 * Why the server turns down a player's request to change a block from a screen, or {@link #OK}.
 * The order of {@link ServerPayloadGuard}'s checks, apart from the game so it can be unit tested:
 * cheap and decisive ones first, the costlier ones only asked once those pass.
 */
public enum PayloadVerdict {
    OK,
    /** Dead, or a spectator. */
    NOT_PLAYING,
    /** The block is in another level, or not loaded. */
    UNKNOWN_BLOCK,
    /** Too far from the block. */
    OUT_OF_REACH,
    /** Spawn protection or the world border forbid the block to this player. */
    PROTECTED,
    /** In adventure mode, or otherwise not allowed to build. */
    MAY_NOT_BUILD;

    public static PayloadVerdict decide(boolean alive, boolean spectator, boolean sameLevel, boolean loaded,
                                        BooleanSupplier inReach, BooleanSupplier mayInteract, BooleanSupplier mayBuild) {
        if (!alive || spectator) {
            return NOT_PLAYING;
        }
        if (!sameLevel || !loaded) {
            return UNKNOWN_BLOCK;
        }
        if (!inReach.getAsBoolean()) {
            return OUT_OF_REACH;
        }
        if (!mayInteract.getAsBoolean()) {
            return PROTECTED;
        }
        return mayBuild.getAsBoolean() ? OK : MAY_NOT_BUILD;
    }
}

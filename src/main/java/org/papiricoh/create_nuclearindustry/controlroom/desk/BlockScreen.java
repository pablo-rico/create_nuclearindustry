package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * Screens that fill a whole block: four modules of a type that {@link ModuleType#mergesBlock() merges}
 * filling every slot of one desk, table or wall panel show as one screen four times the size, set up and
 * drawn by the module in slot 0. Anything else and each module is its own small screen. No game
 * classes, so the rule is tested on its own.
 */
public final class BlockScreen {
    /** The slot whose module sets up and draws a merged screen. */
    public static final int HEAD = 0;

    private BlockScreen() {}

    /**
     * Whether the modules of a block's four slots, in slot order ({@code null} for an empty or closed
     * slot), make one screen.
     */
    public static boolean merged(ModuleType... types) {
        if (types.length != ControlDeskBlockEntity.SLOTS || types[0] == null || !types[0].mergesBlock()) {
            return false;
        }
        for (ModuleType type : types) {
            if (type != types[0]) {
                return false;
            }
        }
        return true;
    }
}

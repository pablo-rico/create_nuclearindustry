package org.papiricoh.create_nuclearindustry.civildefense.blastdoor;

import net.minecraft.util.StringRepresentable;

/** {@link BlastDoorTravel.Cell} as a block state value of a {@link BlastDoorBlock}. */
public enum LeafState implements StringRepresentable {
    CLOSED("closed"),
    MOVING("moving"),
    OPEN("open");

    private final String name;

    LeafState(String name) {
        this.name = name;
    }

    public static LeafState of(BlastDoorTravel.Cell cell) {
        return switch (cell) {
            case CLOSED -> CLOSED;
            case MOVING -> MOVING;
            case OPEN -> OPEN;
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

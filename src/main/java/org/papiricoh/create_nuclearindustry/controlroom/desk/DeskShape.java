package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * How a control desk joins its neighbours, like stairs: straight, or turning a corner. Each shape
 * names, in native model space (operator to the north), the two ends where the console carries on
 * and the operator side at each, so LEFT and RIGHT can tell whether a neighbour closes that end,
 * plus the extra turns of its body and end-cap models.
 */
public enum DeskShape implements StringRepresentable {
    STRAIGHT("straight", 0, 0, Direction.EAST, Direction.NORTH, Direction.WEST, Direction.NORTH),
    INNER_LEFT("inner_left", 270, 0, Direction.NORTH, Direction.WEST, Direction.WEST, Direction.NORTH),
    INNER_RIGHT("inner_right", 0, 90, Direction.EAST, Direction.NORTH, Direction.NORTH, Direction.EAST),
    OUTER_LEFT("outer_left", 0, 270, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST),
    OUTER_RIGHT("outer_right", 90, 0, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH);

    private final String name;
    private final int leftCapRotation;
    private final int rightCapRotation;
    private final Direction leftEnd;
    private final Direction leftOperator;
    private final Direction rightEnd;
    private final Direction rightOperator;

    DeskShape(String name, int leftCapRotation, int rightCapRotation, Direction leftEnd, Direction leftOperator,
              Direction rightEnd, Direction rightOperator) {
        this.name = name;
        this.leftCapRotation = leftCapRotation;
        this.rightCapRotation = rightCapRotation;
        this.leftEnd = leftEnd;
        this.leftOperator = leftOperator;
        this.rightEnd = rightEnd;
        this.rightOperator = rightOperator;
    }

    /**
     * Straight desks hold all four slots. A corner holds one module on each arm where its slope is
     * wide enough: the back row on an inner corner, which opens out towards the rails, and the front
     * row on an outer corner, which narrows to the post behind.
     */
    public boolean hasSlot(int slot) {
        if (this == STRAIGHT) {
            return slot >= 0 && slot < 4;
        }
        return isInner() ? slot == 2 || slot == 3 : slot == 0 || slot == 1;
    }

    /**
     * Clockwise turn, in native corner space, that brings a straight desk's slot onto its corner arm:
     * slots 3 (inner) and 0 (outer) sit on the arm carrying on like the native straight desk, slots 2
     * and 1 on the other arm, whose operator stands to the west.
     */
    public int slotYaw(int slot) {
        return this != STRAIGHT && (slot == 2 && isInner() || slot == 1 && !isInner()) ? 270 : 0;
    }

    public boolean isInner() {
        return this == INNER_LEFT || this == INNER_RIGHT;
    }

    /** The right-hand corners reuse the left-hand models, turned a further quarter clockwise. */
    public int bodyExtraRotation() {
        return this == INNER_RIGHT || this == OUTER_RIGHT ? 90 : 0;
    }

    public int leftCapRotation() {
        return leftCapRotation;
    }

    public int rightCapRotation() {
        return rightCapRotation;
    }

    /** World direction of the left end for a desk whose operator stands on {@code facing}. */
    public Direction leftEnd(Direction facing) {
        return turn(leftEnd, facing);
    }

    public Direction leftOperator(Direction facing) {
        return turn(leftOperator, facing);
    }

    public Direction rightEnd(Direction facing) {
        return turn(rightEnd, facing);
    }

    public Direction rightOperator(Direction facing) {
        return turn(rightOperator, facing);
    }

    /** Native (operator to the north) direction to world direction. */
    static Direction turn(Direction nativeDirection, Direction facing) {
        int steps = (facing.get2DDataValue() + 2) % 4;
        Direction turned = nativeDirection;
        for (int i = 0; i < steps; i++) {
            turned = turned.getClockWise();
        }
        return turned;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

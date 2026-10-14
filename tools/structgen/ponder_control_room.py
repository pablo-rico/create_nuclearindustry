#!/usr/bin/env python3
"""Writes the Ponder schematic of the control room series (ControlRoomScenes), also a GameTest template.

    python3 tools/structgen/ponder_control_room.py

A main control room laid out like a real plant's, 19x7x19 on a polished floor at y 0 with a darker band along the back wall. Ponder looks
from the north-west, so the room is open to the north and west and every front faces north or west:
- back wall z 18 (and the corner column x 17, z 17), east wall x 18, concrete, y 1..6;
- the back wall's instruments at z 17, facing north: a row of annunciator panels x 1..16 at y 5;
  under it, west section x 1..5 of wall control panels y 1..4 with chart recorders at (1..2, 1);
  centre x 6..11 a 6x3 monitor screen at y 2..4 over a row of wall panels at y 1; east section
  x 12..16 of wall panels y 1..4 with chart recorders at (15..16, 1); nuclear alarms at (6, 6) and
  (11, 6) on the ledge;
- the east wall's instruments at x 17, facing west: annunciators z 7..16 at y 4; two 3x2 monitor
  screens at z 8..10 and z 13..15, y 2..3, with wall panels around them and along y 1;
- the operators' console, set back three blocks from the back wall: control desks along z 13
  (x 5..13) facing north, turning inner corners at x 4 and x 14 into arms along x 4 (facing east)
  and x 14 (facing west) at z 11..12, so it wraps round the operators; seats at z 12 and z 10;
- the shift supervisor's control tables x 7..11 at z 6 facing north, with seats at z 5.
Desk shapes and the LEFT/RIGHT joins are what the game works out for this layout; a GameTest
(ControlRoomPonderSchematicTests) checks them against the game's own rules.
"""
from ponder_plant_parts import put, write, NS

SIZE = (19, 7, 19)
WALL = "minecraft:light_gray_concrete"
FLOOR = "minecraft:polished_andesite"
TRIM = "minecraft:gray_concrete"
SEAT = "create:black_seat"


def panel(blocks, pos, facing):
    put(blocks, pos, NS + "wall_control_panel", {"facing": facing, "left": "false", "right": "false"})


def table(blocks, pos):
    put(blocks, pos, NS + "control_table", {"facing": "north", "left": "false", "right": "false"})


def desk(blocks, pos, facing, shape="straight"):
    put(blocks, pos, NS + "reactor_control_panel", {"facing": facing, "shape": shape, "left": "false", "right": "false"})


def facing_block(blocks, pos, name, facing):
    put(blocks, pos, NS + name, {"facing": facing})


def build():
    blocks = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            # A darker band runs along the foot of the back wall's instruments.
            band = z in (16, 17) and x < 18
            put(blocks, (x, 0, z), TRIM if band else FLOOR)
    for y in range(1, 7):
        for x in range(SIZE[0]):
            put(blocks, (x, y, 18), WALL)
        for z in range(18):
            put(blocks, (18, y, z), WALL)
        put(blocks, (17, y, 17), WALL)

    # Back wall, facing north.
    for x in range(1, 17):
        facing_block(blocks, (x, 5, 17), "annunciator_panel", "north")
    for x in range(1, 17):
        for y in range(1, 5):
            if 6 <= x <= 11 and y >= 2:
                facing_block(blocks, (x, y, 17), "control_monitor", "north")
            elif y == 1 and x in (1, 2, 15, 16):
                facing_block(blocks, (x, y, 17), "chart_recorder", "north")
            else:
                panel(blocks, (x, y, 17), "north")
    put(blocks, (6, 6, 17), NS + "nuclear_alarm", {"lit": "false"})
    put(blocks, (11, 6, 17), NS + "nuclear_alarm", {"lit": "false"})

    # East wall, facing west.
    for z in range(7, 17):
        facing_block(blocks, (17, 4, z), "annunciator_panel", "west")
        panel(blocks, (17, 1, z), "west")
        for y in (2, 3):
            if 8 <= z <= 10 or 13 <= z <= 15:
                facing_block(blocks, (17, y, z), "control_monitor", "west")
            else:
                panel(blocks, (17, y, z), "west")

    # The operators' console.
    for x in range(5, 14):
        desk(blocks, (x, 1, 13), "north")
    desk(blocks, (4, 1, 13), "north", "inner_right")
    desk(blocks, (14, 1, 13), "north", "inner_left")
    for z in (11, 12):
        desk(blocks, (4, 1, z), "east")
        desk(blocks, (14, 1, z), "west")
    for x in (7, 9, 11):
        put(blocks, (x, 1, 12), SEAT)
    put(blocks, (5, 1, 10), SEAT)
    put(blocks, (13, 1, 10), SEAT)

    # The shift supervisor's tables.
    for x in range(7, 12):
        table(blocks, (x, 1, 6))
    for x in (8, 10):
        put(blocks, (x, 1, 5), SEAT)
    joins(blocks)
    return blocks


CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
FLAT = (NS + "wall_control_panel", NS + "control_table")


def joins(blocks):
    """LEFT and RIGHT of tables and wall panels: the same block on that side, facing the same way.
    The operator's left is clockwise of the side they stand on. Desks are left to the overrides."""
    for (x, y, z), (name, props) in list(blocks.items()):
        if name not in FLAT:
            continue
        facing = props["facing"]
        for side, key in ((CW[facing], "left"), (CW[CW[CW[facing]]], "right")):
            dx, dz = STEP[side]
            other = blocks.get((x + dx, y, z + dz))
            props[key] = "true" if other and other[0] == name and other[1]["facing"] == facing else "false"
    for pos, left, right in DESK_JOINS:
        blocks[pos][1]["left"] = left
        blocks[pos][1]["right"] = right


# (pos, left, right) of every desk, as the game joins them (see ControlDeskBlock.withConnections).
DESK_JOINS = [((x, 1, 13), "true", "true") for x in range(5, 14)] + [
    ((4, 1, 13), "true", "true"), ((14, 1, 13), "true", "true"),
    ((4, 1, 12), "true", "true"), ((4, 1, 11), "false", "true"),
    ((14, 1, 12), "true", "true"), ((14, 1, 11), "true", "false"),
]


def main():
    write("control_room", SIZE, build())


if __name__ == "__main__":
    main()

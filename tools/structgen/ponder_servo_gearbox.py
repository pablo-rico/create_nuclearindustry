#!/usr/bin/env python3
"""Writes the Ponder schematic of the Servo Gearbox scene (ControlRoomScenes.servo), also a GameTest template.

    python3 tools/structgen/ponder_servo_gearbox.py

7x9x7 on a smooth stone floor at y 0. Ponder looks from the north-west, so the front faces north:
- a block of graphite x 3..5, y 1..3, z 2..4 with an open channel down its middle at (4, 1..3, 3);
- a Rope Pulley at (4, 7, 3), shaft along x, holding three control rods at (4, 4..6, 3) over the channel;
- the Servo Gearbox at (3, 7, 3) along x, driven from the west by a creative motor at (2, 7, 3) facing
  east on a pillar of andesite casing (2, 1..6, 3);
- an analog lever hanging under the servo at (3, 6, 3);
- a comparator at (3, 7, 2) reading the servo, on a casing at (3, 6, 2), lighting a lamp at (3, 7, 1)
  on a casing at (3, 6, 1).
"""
from ponder_plant_parts import put, write, NS

SIZE = (7, 9, 7)


def build():
    blocks = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            put(blocks, (x, 0, z), "minecraft:smooth_stone")
    for x in range(3, 6):
        for y in range(1, 4):
            for z in range(2, 5):
                if (x, z) != (4, 3):
                    put(blocks, (x, y, z), NS + "graphite_moderator")
    for y in range(4, 7):
        put(blocks, (4, y, 3), NS + "control_rod")
    put(blocks, (4, 7, 3), "create:rope_pulley", {"axis": "x"})
    put(blocks, (3, 7, 3), NS + "servo_gearbox", {"axis": "x", "motion": "idle"})
    put(blocks, (2, 7, 3), "create:creative_motor", {"facing": "east"})
    for y in range(1, 7):
        put(blocks, (2, y, 3), "create:andesite_casing")
    put(blocks, (3, 6, 3), "create:analog_lever", {"face": "ceiling", "facing": "north"})
    put(blocks, (3, 6, 2), "create:andesite_casing")
    put(blocks, (3, 7, 2), "minecraft:comparator", {"facing": "south", "mode": "compare", "powered": "false"})
    put(blocks, (3, 6, 1), "create:andesite_casing")
    put(blocks, (3, 7, 1), "minecraft:redstone_lamp", {"lit": "false"})
    return blocks


def main():
    write("servo_gearbox", SIZE, build())


if __name__ == "__main__":
    main()

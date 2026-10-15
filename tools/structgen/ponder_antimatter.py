#!/usr/bin/env python3
"""Writes the Ponder schematic of antimatter containment (AntimatterScenes).

    python3 tools/structgen/ponder_antimatter.py

Also written as a GameTest template, so a GameTest can prove the traps stand whole.
- antimatter_containment (8x4x8): the three Penning traps in a row along x at z 3, their fronts
  (vial port and status light) north, where the Ponder camera sees them: a creative RF generator
  (0,1,3), the small trap (1,1,3), an antimatter conduit (2,1,3) with its own generator behind it
  (2,1,4), the medium trap (3,1..2,3), a second generator (4,1,3) feeding both the medium and the
  large trap, and the large trap (5..6, 1..3, 3..4), its master the bottom north-west block (5,1,3).
"""
from ponder_plant_parts import NS, floor, put, write

CONTAINMENT_SIZE = (8, 4, 8)
CORNERS = {(0, 0): "nw", (1, 0): "ne", (0, 1): "sw", (1, 1): "se"}
LEVELS = ("bottom", "middle", "top")


def large_trap(blocks, origin):
    ox, oy, oz = origin
    for dy, level in enumerate(LEVELS):
        for (dx, dz), corner in CORNERS.items():
            put(blocks, (ox + dx, oy + dy, oz + dz), NS + "large_penning_trap", {"corner": corner, "level": level})


def containment():
    blocks = {}
    floor(blocks, CONTAINMENT_SIZE)
    put(blocks, (0, 1, 3), NS + "creative_rf_generator")
    put(blocks, (1, 1, 3), NS + "small_penning_trap", {"facing": "north"})
    sides = {d: "false" for d in ("north", "south", "east", "west", "up", "down")}
    put(blocks, (2, 1, 3), NS + "antimatter_conduit", dict(sides, east="true", west="true"))
    put(blocks, (2, 1, 4), NS + "creative_rf_generator")
    put(blocks, (3, 1, 3), NS + "penning_trap", {"facing": "north", "half": "lower"})
    put(blocks, (3, 2, 3), NS + "penning_trap", {"facing": "north", "half": "upper"})
    put(blocks, (4, 1, 3), NS + "creative_rf_generator")
    large_trap(blocks, (5, 1, 3))
    return blocks


def main():
    write("antimatter_containment", CONTAINMENT_SIZE, containment())


if __name__ == "__main__":
    main()

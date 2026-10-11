#!/usr/bin/env python3
"""Writes the Ponder schematic of the containment door scene.

    python3 tools/structgen/ponder_containment_door.py

A 7x7 plate with a stretch of containment wall along x at z 3 (x 1..5, y 1..4) and a Containment
Door in its middle (3, 1..2, 3), facing south with its hinge on the left: it pulls out north,
towards the camera, and slides east into the wall. A containment monitor stands behind the wall.
Layout matches ContainmentDoorScenes.
"""
import pathlib

import nbtwriter

SIZE = (7, 6, 7)
ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/containment_door.nbt"
NS = "create_nuclearindustry:"


def door(half):
    return (NS + "containment_door", {"facing": "south", "half": half, "hinge": "left", "open": "false",
                                       "powered": "false", "visible": "true"})


def main():
    blocks = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            blocks[(x, 0, z)] = ("minecraft:smooth_stone", {})
    for x in range(1, 6):
        for y in range(1, 5):
            blocks[(x, y, 3)] = (NS + "containment_concrete", {})
    blocks[(3, 1, 3)] = door("lower")
    blocks[(3, 2, 3)] = door("upper")
    blocks[(3, 1, 5)] = (NS + "containment_monitor", {"facing": "north"})
    nbtwriter.write(OUT, nbtwriter.structure(SIZE, blocks))
    print("wrote", OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()

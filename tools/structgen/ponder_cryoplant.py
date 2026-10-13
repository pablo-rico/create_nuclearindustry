#!/usr/bin/env python3
"""Writes the Ponder schematic of a cryogenic plant: a 3x3x4 box on a 7x7 plate.

    python3 tools/structgen/ponder_cryoplant.py

Ponder looks from the north-west, so the controller and both compressors go up the middle of the
north wall (shafts running north) and the two helium ports sit on the west wall. Layout matches
CryoplantScenes; a GameTest checks the copy in the test structures forms.
"""
import pathlib

import nbtwriter

SIZE = 7
HEIGHT = 4
ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/cryoplant.nbt"
TEST_OUT = ROOT / "src/main/resources/data/create_nuclearindustry/structure/gametest/cryoplant_ponder.nbt"
NS = "create_nuclearindustry:"


def main():
    blocks = {}
    for x in range(SIZE):
        for z in range(SIZE):
            blocks[(x, 0, z)] = ("minecraft:smooth_stone", {})
    for y in range(1, HEIGHT + 1):
        for x in range(2, 5):
            for z in range(2, 5):
                blocks[(x, y, z)] = (NS + "cryoplant_casing", {})

    blocks[(3, 2, 2)] = (NS + "cryoplant_controller", {"facing": "north", "running": "false"})
    for y in (1, 3):
        blocks[(3, y, 2)] = (NS + "cryoplant_compressor", {"facing": "north"})
        blocks[(3, y, 1)] = ("create:shaft", {"axis": "z"})
        blocks[(3, y, 0)] = ("create:shaft", {"axis": "z"})
    blocks[(2, 2, 3)] = (NS + "cryoplant_port", {"mode": "helium_in"})
    blocks[(2, 3, 3)] = (NS + "cryoplant_port", {"mode": "liquid_helium_out"})

    data = nbtwriter.structure((SIZE, HEIGHT + 2, SIZE), blocks)
    for path in (OUT, TEST_OUT):
        path.parent.mkdir(parents=True, exist_ok=True)
        nbtwriter.write(path, data)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

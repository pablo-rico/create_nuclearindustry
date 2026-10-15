#!/usr/bin/env python3
"""Writes the Ponder schematic of radioactive waste transport.

    python3 tools/structgen/ponder_waste_transport.py

Layout (matches WasteTransportScenes), 9x4x9: a smooth stone base plate at y 0; a 2x2x2 Radioactive
Waste Cistern at x 1..2, y 1..2, z 1..2 with a Create fluid pipe running into its west face at
(0, 1, 2); a Shielded Waste Cask lying along x, two blocks across and three long, at x 4..6,
y 1..2, z 1..2. Ponder looks from the north-west, so both show their north and west faces. The
blocks are written unconnected: the scene joins each multiblock itself (Ponder levels are client
side, where Create's tanks never form on their own).
"""
import pathlib

import nbtwriter

ROOT = pathlib.Path(__file__).resolve().parents[2]
SIZE = (9, 4, 9)
CISTERN = "create_nuclearindustry:radioactive_waste_cistern"
CASK = "create_nuclearindustry:shielded_waste_cask"


def build():
    blocks = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            blocks[(x, 0, z)] = ("minecraft:smooth_stone", {})
    for x in (1, 2):
        for y in (1, 2):
            for z in (1, 2):
                blocks[(x, y, z)] = (CISTERN, {"bottom": "true" if y == 1 else "false", "top": "true" if y == 2 else "false",
                                               "shape": "plain"})
    blocks[(0, 1, 2)] = ("create:fluid_pipe", {"east": "true", "west": "true", "north": "false", "south": "false",
                                               "up": "false", "down": "false", "waterlogged": "false"})
    for x in (4, 5, 6):
        for y in (1, 2):
            for z in (1, 2):
                blocks[(x, y, z)] = (CASK, {"axis": "x"})
    return nbtwriter.structure(SIZE, blocks)


def main():
    path = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/waste_transport.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    nbtwriter.write(path, build())
    print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

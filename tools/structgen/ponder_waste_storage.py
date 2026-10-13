#!/usr/bin/env python3
"""Writes the Ponder schematic of a cross-section through the ground with a waste gallery.

    python3 tools/structgen/ponder_waste_storage.py

Layout (matches WasteStorageScenes), 7x13x7: deepslate y 0..3 with a gallery carved at
x 1..5, y 1..2, z 0..3, open on the north face so Ponder's north-west view looks straight in;
stone y 4..7; dirt y 8..9; grass y 10. A shaft at (5, 3..10, 4) joins the gallery to the
surface. The scene places the barrels itself.
"""
import pathlib

import nbtwriter

ROOT = pathlib.Path(__file__).resolve().parents[2]
SIZE = (7, 13, 7)


def layer(y):
    if y <= 3:
        return "minecraft:deepslate"
    if y <= 7:
        return "minecraft:stone"
    if y <= 9:
        return "minecraft:dirt"
    return "minecraft:grass_block"


def build():
    blocks = {}
    for x in range(SIZE[0]):
        for y in range(11):
            for z in range(SIZE[2]):
                gallery = 1 <= x <= 5 and 1 <= y <= 2 and z <= 3
                shaft = x == 5 and z == 4 and 3 <= y <= 10
                if not (gallery or shaft):
                    props = {"snowy": "false"} if y == 10 else {}
                    blocks[(x, y, z)] = (layer(y), props)
    return nbtwriter.structure(SIZE, blocks)


def main():
    path = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/waste_storage.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    nbtwriter.write(path, build())
    print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

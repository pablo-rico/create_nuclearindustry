#!/usr/bin/env python3
"""Writes the Ponder schematic of spent fuel and waste (scene plant_waste).

    python3 tools/structgen/ponder_plant_waste.py

Layout (matches PlantScenes.waste), 15x11x15, a cut through the ground: deepslate y 0..4, stone
y 5..6, dirt y 7, grass y 8, so the surface is at y 9. Ponder looks from the north-west:
- spent fuel pool lined with smooth stone at x 0..6, z 0..6, y 4..8, full of water from y 5;
  six waterlogged racks on its floor at (2..4, 5, 2) and (2..4, 5, 4), a pool cooler (5,5,5)
  with its shaft rising through the water to a creative motor (5,10,5) on the surface;
- on the surface: two dry casks (8,9,1) and (9,9,1), a vitrification melter (12,10,1) on a lit
  blaze burner (12,9,1), and a compacted drum (8,9,5), a cemented drum (10,9,5) and a
  vitrified canister (12,9,5);
- the repository: a gallery carved in the deepslate at x 7..13, y 1..2, z 0..3, open on the
  north face so the camera looks in. A canister (8,1,1), a cemented drum (10,1,1) and a
  compacted drum (12,1,1) sit in it, packed with bentonite on every other side, and a
  repository inspector (7,1,1) faces north.
"""
from ponder_plant_parts import put, write, NS

SIZE = (15, 11, 15)
POOL = range(0, 7)
GALLERY_X, GALLERY_Y, GALLERY_Z = range(7, 14), range(1, 3), range(0, 4)
RACKS = {(2, 5, 2): "glowing", (3, 5, 2): "glowing", (4, 5, 2): "loaded",
         (2, 5, 4): "loaded", (3, 5, 4): "loaded", (4, 5, 4): "empty"}
STORED = {(8, 1, 1): "vitrified_waste_canister", (10, 1, 1): "cemented_waste_drum", (12, 1, 1): "compacted_waste_drum"}


def ground(y):
    if y <= 4:
        return "minecraft:deepslate", {"axis": "y"}
    if y <= 6:
        return "minecraft:stone", {}
    if y == 7:
        return "minecraft:dirt", {}
    return "minecraft:grass_block", {"snowy": "false"}


def build():
    blocks = {}
    for x in range(SIZE[0]):
        for y in range(9):
            for z in range(SIZE[2]):
                in_pool = x in POOL and z in POOL and y >= 4
                in_gallery = x in GALLERY_X and y in GALLERY_Y and z in GALLERY_Z
                if in_pool:
                    pool_interior = 1 <= x <= 5 and 1 <= z <= 5 and y >= 5
                    if not pool_interior:
                        put(blocks, (x, y, z), "minecraft:smooth_stone")
                elif not in_gallery:
                    put(blocks, (x, y, z), *ground(y))

    # The pool: racks on the floor, the cooler in a corner, water everywhere else up to y 8.
    for pos, load in RACKS.items():
        put(blocks, pos, NS + "spent_fuel_rack", {"waterlogged": "true", "load": load})
    put(blocks, (5, 5, 5), NS + "pool_cooler", {"waterlogged": "true"})
    for y in range(6, 9):
        put(blocks, (5, y, 5), "create:shaft", {"axis": "y", "waterlogged": "true"})
    for x in range(1, 6):
        for y in range(5, 9):
            for z in range(1, 6):
                if (x, y, z) not in blocks:
                    put(blocks, (x, y, z), "minecraft:water", {"level": "0"})
    put(blocks, (5, 9, 5), "create:shaft", {"axis": "y", "waterlogged": "false"})
    put(blocks, (5, 10, 5), "create:creative_motor", {"facing": "down"})

    # The surface: dry casks, the melter on its burner, and one package of each kind.
    put(blocks, (8, 9, 1), NS + "dry_cask")
    put(blocks, (9, 9, 1), NS + "dry_cask")
    put(blocks, (12, 9, 1), "create:blaze_burner", {"blaze": "kindled", "facing": "north"})
    put(blocks, (12, 10, 1), NS + "vitrification_melter")
    put(blocks, (8, 9, 5), NS + "compacted_waste_drum")
    put(blocks, (10, 9, 5), NS + "cemented_waste_drum")
    put(blocks, (12, 9, 5), NS + "vitrified_waste_canister")

    # The repository gallery: packages backfilled with bentonite, open only on the cut face (z 0).
    for pos, package in STORED.items():
        put(blocks, pos, NS + package)
    put(blocks, (7, 1, 1), NS + "repository_inspector", {"facing": "north"})
    for x in GALLERY_X:
        for y in GALLERY_Y:
            for z in range(1, 4):
                if (x, y, z) not in blocks:
                    put(blocks, (x, y, z), NS + "bentonite")
    return blocks


def main():
    write("plant_waste", SIZE, build())


if __name__ == "__main__":
    main()

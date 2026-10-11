#!/usr/bin/env python3
"""Writes the Ponder schematic of the fuel cycle scene, from ore to fuel (FuelCycleScenes).

    python3 tools/structgen/ponder_fuel_cycle.py

Only for Ponder: its washing station holds a free water source, which would run off in a GameTest.
Layout, 14x6x6, stations along x so the camera (north-west) sees each from the front:
- crushing wheels (1,2,2) and (3,2,2) on the z axis, the gap between them at x 2;
- washing: an encased fan (6,1,4) blowing north through water (6,1,3) onto a depot (6,1,2);
- leaching: a mechanical mixer (8,4,2) over a basin (8,2,2) on a lit blaze burner (8,1,2);
- enrichment: two centrifuges (10,1,2) and (11,1,2) facing north, and an electrolyzer (10,1,4);
- pellets and assembly: a mechanical press (13,3,2) over a depot (13,1,2).
"""
import pathlib

import nbtwriter
from ponder_plant_parts import NS, ROOT, floor, put

SIZE = (14, 6, 6)


def build():
    blocks = {}
    floor(blocks, SIZE)
    put(blocks, (1, 2, 2), "create:crushing_wheel", {"axis": "z"})
    put(blocks, (3, 2, 2), "create:crushing_wheel", {"axis": "z"})

    put(blocks, (6, 1, 4), "create:encased_fan", {"facing": "north"})
    put(blocks, (6, 1, 3), "minecraft:water", {"level": "0"})
    put(blocks, (6, 1, 2), "create:depot")

    put(blocks, (8, 1, 2), "create:blaze_burner", {"blaze": "kindled", "facing": "north"})
    put(blocks, (8, 2, 2), "create:basin", {"facing": "down"})
    put(blocks, (8, 4, 2), "create:mechanical_mixer")

    put(blocks, (10, 1, 2), NS + "centrifuge", {"facing": "north"})
    put(blocks, (11, 1, 2), NS + "centrifuge", {"facing": "north"})
    put(blocks, (10, 1, 4), NS + "electrolyzer", {"facing": "north"})

    put(blocks, (13, 1, 2), "create:depot")
    put(blocks, (13, 3, 2), "create:mechanical_press", {"facing": "north"})
    return blocks


def main():
    blocks = build()
    for (x, y, z) in blocks:
        if not (0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]):
            raise ValueError(f"{(x, y, z)} lies outside {SIZE}")
    path = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/fuel_cycle.nbt"
    nbtwriter.write(path, nbtwriter.structure(SIZE, blocks))
    print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

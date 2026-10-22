#!/usr/bin/env python3
"""Writes the Ponder schematics of the centrifuge cascade and the electrolyzer (CentrifugeScenes).

    python3 tools/structgen/ponder_centrifuge.py

Ponder looks from the north-west, so the machines face north (product port towards the camera) and
stand one block up, so the feed pipe on top, the product pipe in front and the tails pipe underneath
all show; the drive comes in from behind through cogwheels.

- centrifuge_cascade, 7x5x7: three centrifuges (2,2,2) (3,2,2) (4,2,2) side by side, their headers
  joined; cogwheels behind them at z 3 turned by a shaft (3,2,4)-(3,2,6). The single-centrifuge scene
  shows only the middle one.
- electrolyzer, 5x5x5: one electrolyzer (2,2,2) driven by a cogwheel (2,2,3) and a shaft (2,2,4).

Each also gets a GameTest copy, which checks that the schematic's header joints are the ones the
block works out itself.
"""
import pathlib

import nbtwriter
from ponder_plant_parts import NS, ROOT, floor, pipe, put

PONDER = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder"
TESTS = ROOT / "src/main/resources/data/create_nuclearindustry/structure/gametest"


def machine_with_pipes(blocks, pos, name, props):
    """A machine one block up, with stub pipes on top (feed), in front (product) and underneath (tails)."""
    x, y, z = pos
    put(blocks, pos, NS + name, props)
    pipe(blocks, (x, y + 1, z), "up", "down")
    pipe(blocks, (x, y, z - 1), "north", "south")
    pipe(blocks, (x, y - 1, z), "up", "down")


def cascade():
    size = (7, 5, 7)
    blocks = {}
    floor(blocks, size)
    xs = (2, 3, 4)
    for x in xs:
        joints = {"facing": "north", "left": str(x != xs[0]).lower(), "right": str(x != xs[-1]).lower()}
        machine_with_pipes(blocks, (x, 2, 2), "centrifuge", joints)
        put(blocks, (x, 2, 3), "create:cogwheel", {"axis": "z"})
    put(blocks, (3, 2, 4), "create:shaft", {"axis": "z"})
    put(blocks, (3, 2, 5), "create:shaft", {"axis": "z"})
    put(blocks, (3, 2, 6), "create:shaft", {"axis": "z"})
    return size, blocks


def electrolyzer():
    size = (5, 5, 5)
    blocks = {}
    floor(blocks, size)
    machine_with_pipes(blocks, (2, 2, 2), "electrolyzer", {"facing": "north"})
    put(blocks, (2, 2, 3), "create:cogwheel", {"axis": "z"})
    put(blocks, (2, 2, 4), "create:shaft", {"axis": "z"})
    return size, blocks


def main():
    for name, build in (("centrifuge_cascade", cascade), ("electrolyzer", electrolyzer)):
        size, blocks = build()
        for (x, y, z) in blocks:
            if not (0 <= x < size[0] and 0 <= y < size[1] and 0 <= z < size[2]):
                raise ValueError(f"{(x, y, z)} lies outside {size}")
        data = nbtwriter.structure(size, blocks)
        for path in (PONDER / f"{name}.nbt", TESTS / f"{name}_ponder.nbt"):
            path.parent.mkdir(parents=True, exist_ok=True)
            nbtwriter.write(path, data)
            print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

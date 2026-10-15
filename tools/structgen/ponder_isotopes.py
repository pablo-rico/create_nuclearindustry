#!/usr/bin/env python3
"""Writes the Ponder schematic of isotopes and nuclear medicine (IsotopeScenes.isotopes).

    python3 tools/structgen/ponder_isotopes.py

Also written as a GameTest template. Ponder looks from the north-west, so the parts face west.
- isotopes (9x4x7):
  - a piece of fission reactor wall, reactor casing at x=1, y=1..3, z=1..3, with the irradiation
    channel in its middle (1,2,2) facing west at the camera;
  - a Create depot at (4,1,4) with the gamma irradiator two blocks above it (4,3,4), like a press;
  - a smoke detector on the floor at (7,1,2), a redstone lamp west of it (6,1,2) and a campfire two
    blocks south (7,1,4), unlit until the scene lights it.
"""
from ponder_plant_parts import NS, floor, put, replace, write

SIZE = (9, 4, 7)


def isotopes():
    blocks = {}
    floor(blocks, SIZE)
    for y in range(1, 4):
        for z in range(1, 4):
            put(blocks, (1, y, z), NS + "reactor_casing")
    replace(blocks, (1, 2, 2), NS + "reactor_irradiation_channel", {"facing": "west", "status": "empty"})
    put(blocks, (4, 1, 4), "create:depot")
    put(blocks, (4, 3, 4), NS + "gamma_irradiator")
    put(blocks, (7, 1, 2), NS + "smoke_detector", {"face": "floor", "facing": "west", "power": "0"})
    put(blocks, (6, 1, 2), "minecraft:redstone_lamp", {"lit": "false"})
    put(blocks, (7, 1, 4), "minecraft:campfire",
        {"facing": "west", "lit": "false", "signal_fire": "false", "waterlogged": "false"})
    return blocks


def main():
    write("isotopes", SIZE, isotopes())


if __name__ == "__main__":
    main()

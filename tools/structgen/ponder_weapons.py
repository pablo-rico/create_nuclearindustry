#!/usr/bin/env python3
"""Writes the Ponder schematics of the weapons scenes (WeaponsScenes).

    python3 tools/structgen/ponder_weapons.py

Also written as GameTest templates, so GuidePonderSchematicTests can check they load. The nuclear
cannon shell has no scene: it exists only with Create Big Cannons, and a schematic naming it would
lose the block without that mod.

- weapons_bombs, 7x3x5: a Nuclear Bomb (2,1,2) and a Dirty Bomb (4,1,2), each with a lever on
  its north face, towards the camera.
- weapons_thermonuclear, 5x3x5: a Thermonuclear Bomb (2,1,2) with a lever on its north face (2,1,1).
- weapons_missile, 5x3x5: a Missile Launch Pad (2,1,2) with a lever on its north face (2,1,1).
"""
from ponder_plant_parts import NS, floor, put, write

LEVER = {"face": "wall", "facing": "north", "powered": "false"}


def bombs():
    size = (7, 3, 5)
    blocks = {}
    floor(blocks, size)
    put(blocks, (2, 1, 2), NS + "nuclear_bomb", {"powered": "false"})
    put(blocks, (2, 1, 1), "minecraft:lever", LEVER)
    put(blocks, (4, 1, 2), NS + "dirty_bomb", {"powered": "false"})
    put(blocks, (4, 1, 1), "minecraft:lever", LEVER)
    write("weapons_bombs", size, blocks)


def thermonuclear():
    size = (5, 3, 5)
    blocks = {}
    floor(blocks, size)
    put(blocks, (2, 1, 2), NS + "thermonuclear_bomb", {"powered": "false"})
    put(blocks, (2, 1, 1), "minecraft:lever", LEVER)
    write("weapons_thermonuclear", size, blocks)


def missile():
    size = (5, 3, 5)
    blocks = {}
    floor(blocks, size)
    put(blocks, (2, 1, 2), NS + "launch_pad", {"powered": "false"})
    put(blocks, (2, 1, 1), "minecraft:lever", LEVER)
    write("weapons_missile", size, blocks)


def main():
    bombs()
    thermonuclear()
    missile()


if __name__ == "__main__":
    main()

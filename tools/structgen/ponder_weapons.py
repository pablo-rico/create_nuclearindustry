#!/usr/bin/env python3
"""Writes the Ponder schematics of the weapons scenes (WeaponsScenes).

    python3 tools/structgen/ponder_weapons.py

Also written as GameTest templates, so GuidePonderSchematicTests can check they load. The nuclear
cannon shell has no scene: it exists only with Create Big Cannons, and a schematic naming it would
lose the block without that mod.

- weapons_bombs, 7x3x5: a Nuclear Bomb (2,1,2) and a Dirty Bomb (4,1,2), each with a lever on
  its north face, towards the camera.
- weapons_thermonuclear, 5x3x5: a Thermonuclear Bomb (2,1,2) with a lever on its north face (2,1,1).
- weapons_missile, 5x8x5: a cutaway silo with a fuel port (0,1,2) and a four-section modular rocket.
- weapons_holo_table, 5x3x5: a formed command table (1..3, 1, 1..3), a radar (4,1,0) and a silo
  controller (0,1,4) on a smooth stone floor.
- weapons_launch_base, 7x6x7: an open 3x3 launch base with its controller on the east side, a fuel
  port and pipe to the north, a creative-motor shaft and a two-stage rocket ready for a redstone launch.
"""
from ponder_plant_parts import NS, floor, pipe, put, replace, write

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
    size = (5, 8, 5)
    blocks = {}
    for x in range(5):
        for z in range(5):
            put(blocks, (x, 0, z), NS + "silo_wall")
            if x == 0 or z == 4:
                for y in range(1, 5):
                    put(blocks, (x, y, z), NS + "silo_wall")
    replace(blocks, (2, 0, 0), NS + "silo_controller")
    replace(blocks, (0, 1, 2), NS + "silo_fuel_port")
    for y, name in enumerate(("rocket_engine", "rocket_tank", "rocket_guidance", "rocket_warhead"), 1):
        put(blocks, (2, y, 2), NS + name)
    write("weapons_missile", size, blocks)


def launch_base():
    size = (7, 6, 7)
    blocks = {}
    for x in range(2, 5):
        for z in range(2, 5):
            put(blocks, (x, 0, z), NS + "launch_base")
    # East is deliberately not the legacy north side: the controller can sit on any side of a base.
    put(blocks, (5, 0, 3), NS + "silo_controller", {"facing": "east"})
    put(blocks, (6, 0, 3), "create:shaft", {"axis": "x", "waterlogged": "false"})
    put(blocks, (6, 0, 4), "create:creative_motor", {"facing": "north"})
    put(blocks, (5, 1, 3), "minecraft:lever", {"face": "floor", "facing": "north", "powered": "false"})
    put(blocks, (3, 0, 1), NS + "silo_fuel_port")
    pipe(blocks, (3, 0, 0), "north", "south")
    for y, name in enumerate(("rocket_engine", "rocket_tank", "rocket_engine", "rocket_guidance", "rocket_warhead"), 1):
        put(blocks, (3, y, 3), NS + name)
    write("weapons_launch_base", size, blocks)


PARTS = ("nw", "n", "ne", "w", "center", "e", "sw", "s", "se")


def holo_table():
    size = (5, 3, 5)
    blocks = {}
    floor(blocks, size)
    for i, part in enumerate(PARTS):
        put(blocks, (1 + i % 3, 1, 1 + i // 3), NS + "holo_table", {"part": part})
    put(blocks, (4, 1, 0), NS + "missile_radar")
    put(blocks, (0, 1, 4), NS + "silo_controller", {"facing": "north"})
    write("weapons_holo_table", size, blocks)


def main():
    bombs()
    thermonuclear()
    missile()
    launch_base()
    holo_table()


if __name__ == "__main__":
    main()

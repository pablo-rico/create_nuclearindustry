#!/usr/bin/env python3
"""Writes OrbitScenes schematics and their matching GameTest templates.

    python3 tools/structgen/ponder_orbit.py

The north and west sides face the Ponder camera. The ground station stands on a
motor so its shaft is driven from below; the launch keeps weapons_launch_base's
coordinates, with a command table and a ground station to the south.
"""
from ponder_plant_parts import NS, floor, put, replace, write
from ponder_weapons import PARTS, launch_site


def ground_station():
    size = (7, 4, 7)
    blocks = {}
    floor(blocks, size)
    replace(blocks, (2, 0, 2), "create:creative_motor", {"facing": "up"})
    put(blocks, (2, 1, 2), NS + "ground_station")
    for i, part in enumerate(PARTS):
        put(blocks, (3 + i % 3, 1, 3 + i // 3), NS + "holo_table", {"part": part})
    write("orbit_ground_station", size, blocks)


def satellite_launch():
    size = (7, 10, 10)
    blocks = {}
    for x in range(2, 5):
        for z in range(2, 5):
            put(blocks, (x, 0, z), NS + "launch_base")
    put(blocks, (5, 0, 3), NS + "silo_controller", {"facing": "east"})
    put(blocks, (5, 1, 3), "minecraft:lever", {"face": "floor", "facing": "north", "powered": "false"})
    for y, name in enumerate(("rocket_engine", "rocket_tank", "rocket_tank", "rocket_engine", "rocket_tank", "rocket_tank",
                              "rocket_guidance", "rocket_satellite_fairing"), 1):
        put(blocks, (3, y, 3), NS + name)
    launch_site(blocks, (3, 0, 3))
    for i, part in enumerate(PARTS):
        x, z = 1 + i % 3, 6 + i // 3
        put(blocks, (x, 0, z), "minecraft:smooth_stone")
        put(blocks, (x, 1, z), NS + "holo_table", {"part": part})
    put(blocks, (5, 0, 7), "create:creative_motor", {"facing": "up"})
    put(blocks, (5, 1, 7), NS + "ground_station")
    write("orbit_satellite_launch", size, blocks)


if __name__ == "__main__":
    ground_station()
    satellite_launch()

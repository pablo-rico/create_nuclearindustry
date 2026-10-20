#!/usr/bin/env python3
"""Writes DefenseScenes schematics and their matching GameTest templates.

    python3 tools/structgen/ponder_defense.py

The north and west sides face the Ponder camera. The battery's raised plinth
leaves its bottom shaft visible; the silo preserves weapons_launch_base's
coordinates and adds a command table to the south.
"""
from nbtwriter import Float
from ponder_plant_parts import NS, floor, pipe, put, replace, write
from ponder_weapons import PARTS, launch_site


def interceptor_battery():
    size = (7, 5, 7)
    blocks = {}
    floor(blocks, size)
    replace(blocks, (2, 0, 2), "create:creative_motor", {"facing": "up"})
    put(blocks, (2, 1, 2), "create:shaft", {"axis": "y", "waterlogged": "false"})
    put(blocks, (2, 2, 2), NS + "interceptor_battery", {"facing": "north"})
    # The BER needs the schematic's master BE, including matching rest/target angles.
    name, props = blocks[(2, 2, 2)]
    blocks[(2, 2, 2)] = (name, props, {
        "id": NS + "interceptor_battery", "Yaw": Float(0), "Pitch": Float(45),
        "TargetYaw": Float(0), "TargetPitch": Float(45),
    })
    for offset in range(1, 8):
        put(blocks, (2 + (offset & 1), 2 + ((offset >> 1) & 1), 2 + ((offset >> 2) & 1)),
            NS + "interceptor_battery_part", {"offset": str(offset)})
    put(blocks, (1, 2, 2), "create:andesite_funnel", {"facing": "west", "extracting": "false"})
    put(blocks, (1, 2, 3), "minecraft:lever", {"face": "wall", "facing": "west", "powered": "false"})
    put(blocks, (3, 1, 1), "minecraft:smooth_stone")
    put(blocks, (3, 2, 1), "minecraft:comparator", {"facing": "north", "mode": "compare", "powered": "false"})
    put(blocks, (5, 1, 4), NS + "missile_radar")
    write("defense_interceptor_battery", size, blocks)


def interceptor_silo():
    size = (7, 7, 10)
    blocks = {}
    for x in range(2, 5):
        for z in range(2, 5):
            put(blocks, (x, 0, z), NS + "launch_base")
    put(blocks, (5, 0, 3), NS + "silo_controller", {"facing": "east"})
    put(blocks, (5, 1, 3), "minecraft:lever", {"face": "floor", "facing": "north", "powered": "false"})
    put(blocks, (3, 0, 1), NS + "silo_fuel_port", {"facing": "north"})
    pipe(blocks, (3, 0, 0), "north", "south")
    for y, name in enumerate(("rocket_engine", "rocket_tank", "rocket_engine", "rocket_tank",
                              "rocket_guidance", "rocket_kill_vehicle"), 1):
        put(blocks, (3, y, 3), NS + name)
    launch_site(blocks, (3, 0, 3))
    for i, part in enumerate(PARTS):
        x, z = 1 + i % 3, 6 + i // 3
        put(blocks, (x, 0, z), "minecraft:smooth_stone")
        put(blocks, (x, 1, z), NS + "holo_table", {"part": part})
    write("defense_interceptor_silo", size, blocks)


if __name__ == "__main__":
    interceptor_battery()
    interceptor_silo()

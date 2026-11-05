#!/usr/bin/env python3
"""Writes DysonScenes schematics and their matching GameTest templates.

    python3 tools/structgen/ponder_dyson.py

The north and west sides face the Ponder camera. The ejector stands on a motor so its shaft is driven from below, with a
chest of sails over a hopper feeding it from the south; the receivers stand in a row with nothing over them; the terminal faces west,
towards the camera.
"""
from ponder_plant_parts import NS, floor, put, replace, write


def ejector():
    size = (7, 4, 7)
    blocks = {}
    floor(blocks, size)
    replace(blocks, (3, 0, 3), "create:creative_motor", {"facing": "up"})
    put(blocks, (3, 1, 3), NS + "em_rail_ejector")
    put(blocks, (3, 1, 4), "minecraft:hopper", {"facing": "north", "enabled": "true"})
    put(blocks, (3, 2, 4), "minecraft:chest", {"facing": "west", "type": "single", "waterlogged": "false"})
    write("dyson_ejector", size, blocks)


def receivers():
    size = (7, 3, 7)
    blocks = {}
    floor(blocks, size)
    for x in (1, 3, 5):
        put(blocks, (x, 1, 3), NS + "ray_receiver")
    write("dyson_receivers", size, blocks)


def terminal():
    size = (5, 3, 5)
    blocks = {}
    floor(blocks, size)
    put(blocks, (2, 1, 2), NS + "swarm_terminal", {"facing": "west"})
    write("dyson_terminal", size, blocks)


if __name__ == "__main__":
    ejector()
    receivers()
    terminal()

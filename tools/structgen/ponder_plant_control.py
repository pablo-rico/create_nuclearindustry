#!/usr/bin/env python3
"""Writes the Ponder schematic of the control room and the containment (scene plant_control_safety).

    python3 tools/structgen/ponder_plant_control.py

Layout (matches PlantScenes.controlSafety), 15x10x15. Ponder looks from the north-west, so the
control room stands in the north-west corner and the containment's front walls are north and west:
- control room: two joined control desks (1..2,1,1) facing north, an annunciator panel (1,1,3)
  and a chart recorder (2,1,3) behind them, facing north;
- containment x 5..13, z 5..13: Containment Concrete floor at y 0, walls y 1..8, roof y 9; two
  penetrations (9,3,5) and (9,4,5) carry the pipes through the north wall, a glass window
  (5,4,9) is the weak spot of the west wall and a filtered vent (13,7,9) sits in the east wall;
- inside: a Core Catcher floor x 7..11, z 7..11 at y 1 under the 5x5x5 reactor x 7..11,
  y 2..6, its controller (9,6,9); coolant ports in its north wall (9,3,7) INPUT and (9,4,7)
  OUTPUT; Redstone Ports Withdraw rods (7,4,8) and Insert rods (7,4,10) in its west wall;
  a sticky mechanical piston (8,8,8) holding the rods of the north-west channel, driven along
  the lid by a shaft, a clutch (10,8,8), a gearshift (11,8,8) and a creative motor (12,8,8);
  a Containment Monitor (6,1,12) facing north and recombiners at (6,1,6) and (12,1,6).
"""
from ponder_plant_parts import (floor, pipe, put, reactor, reactor_port, replace, write, NS)

SIZE = (15, 10, 15)
LO, HI = 5, 13


def containment(blocks):
    for x in range(LO, HI + 1):
        for z in range(LO, HI + 1):
            replace(blocks, (x, 0, z), NS + "containment_concrete")
            put(blocks, (x, 9, z), NS + "containment_concrete")
            if x in (LO, HI) or z in (LO, HI):
                for y in range(1, 9):
                    put(blocks, (x, y, z), NS + "containment_concrete")
    replace(blocks, (9, 3, 5), NS + "containment_penetration", {"axis": "z"})
    replace(blocks, (9, 4, 5), NS + "containment_penetration", {"axis": "z"})
    replace(blocks, (5, 4, 9), "minecraft:glass")
    replace(blocks, (13, 7, 9), NS + "filtered_vent", {"open": "false"})


def build():
    blocks = {}
    floor(blocks, SIZE)

    desk = {"facing": "north", "shape": "straight"}
    put(blocks, (1, 1, 1), NS + "reactor_control_panel", dict(desk, left="true", right="false"))
    put(blocks, (2, 1, 1), NS + "reactor_control_panel", dict(desk, left="false", right="true"))
    put(blocks, (1, 1, 3), NS + "annunciator_panel", {"facing": "north"})
    put(blocks, (2, 1, 3), NS + "chart_recorder", {"facing": "north"})

    containment(blocks)
    for x in range(7, 12):
        for z in range(7, 12):
            put(blocks, (x, 1, z), NS + "core_catcher")
    reactor(blocks, (7, 2, 7), open_rod=(1, 1))
    reactor_port(blocks, (9, 3, 7), "north", "input")
    reactor_port(blocks, (9, 4, 7), "north", "output")
    replace(blocks, (7, 4, 8), NS + "reactor_redstone_port", {"facing": "west", "mode": "rod_withdraw", "power": "0"})
    replace(blocks, (7, 4, 10), NS + "reactor_redstone_port", {"facing": "west", "mode": "rod_insert", "power": "0"})

    # Rod drive on the lid, as in the reactor_controller scene: head in the open channel.
    put(blocks, (8, 6, 8), "create:mechanical_piston_head", {"facing": "down", "type": "sticky"})
    put(blocks, (8, 7, 8), "create:piston_extension_pole", {"facing": "down", "waterlogged": "false"})
    put(blocks, (8, 8, 8), "create:sticky_mechanical_piston",
        {"facing": "down", "axis_along_first_coordinate": "true", "state": "extended"})
    put(blocks, (9, 8, 8), "create:shaft", {"axis": "x", "waterlogged": "false"})
    put(blocks, (10, 8, 8), "create:clutch", {"axis": "x", "powered": "false"})
    put(blocks, (11, 8, 8), "create:gearshift", {"axis": "x", "powered": "false"})
    put(blocks, (12, 8, 8), "create:creative_motor", {"facing": "west"})

    # Pipes from the coolant ports out through the penetrations.
    for y in (3, 4):
        pipe(blocks, (9, y, 6), "north", "south")
        pipe(blocks, (9, y, 4), "north", "south")
        pipe(blocks, (9, y, 3), "north", "south")

    put(blocks, (6, 1, 12), NS + "containment_monitor", {"facing": "north"})
    put(blocks, (6, 1, 6), NS + "recombiner")
    put(blocks, (12, 1, 6), NS + "recombiner")
    return blocks


def main():
    write("plant_control", SIZE, build())


if __name__ == "__main__":
    main()

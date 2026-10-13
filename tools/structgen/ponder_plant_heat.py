#!/usr/bin/env python3
"""Writes the Ponder schematic of the pressurized primary loop (scene plant_heat_loop).

    python3 tools/structgen/ponder_plant_heat.py

Layout (matches PlantScenes.heatLoop), 13x9x13 on a smooth stone floor at y 0. Ponder looks
from the north-west, so both legs run along the north side, in front of everything:
- reactor x 1..5, y 1..5, z 5..9, controller (3,5,7); cold leg port INPUT (2,2,5) and hot leg
  port OUTPUT (4,3,5) in its north wall; pressurizer on the north-west corner of the lid:
  heater port (1,6,5), pressurizer (1,7,5), relief valve (1,8,5);
- steam generator x 9..11, y 1..4, z 5..7: controller (10,2,5) facing north, primary in
  (9,3,5), primary out (11,2,5), feedwater in (9,2,6), steam out (10,4,6), with pipe stubs
  that lead on to the next scene;
- hot leg at y 3 along z 4, pump (7,3,4) pushing east; cold leg at y 2 along z 2, pump
  (7,2,2) pushing west. Each pump turns through a cogwheel on top from a creative motor.
"""
from ponder_plant_parts import (floor, pipe, pipe_run, pump, put, reactor, reactor_port, steam_generator, write, NS)

SIZE = (13, 9, 13)


def build():
    blocks = {}
    floor(blocks, SIZE)
    reactor(blocks, (1, 1, 5))
    reactor_port(blocks, (2, 2, 5), "north", "input")
    reactor_port(blocks, (4, 3, 5), "north", "output")
    put(blocks, (1, 6, 5), NS + "pressurizer_heater_port", {"automatic": "true"})
    put(blocks, (1, 7, 5), NS + "pressurizer")
    put(blocks, (1, 8, 5), NS + "relief_valve", {"open": "false"})

    steam_generator(blocks, (10, 1, 6), 4, "north", {
        (9, 3, 5): "primary_in",
        (11, 2, 5): "primary_out",
        (9, 2, 6): "feed_in",
        (10, 4, 6): "steam_out",
    })

    # Hot leg: reactor output port -> pump -> steam generator primary in.
    pipe(blocks, (4, 3, 4), "south", "east")
    pipe_run(blocks, (5, 3, 4), (6, 3, 4))
    pump(blocks, (7, 3, 4), "east", "west")
    pipe(blocks, (8, 3, 4), "east", "west")
    pipe(blocks, (9, 3, 4), "west", "south")

    # Cold leg: steam generator primary out -> pump -> reactor input port.
    pipe_run(blocks, (11, 2, 4), (11, 2, 3))
    pipe(blocks, (11, 2, 2), "south", "west")
    pipe_run(blocks, (8, 2, 2), (10, 2, 2))
    pump(blocks, (7, 2, 2), "west", "east")
    pipe_run(blocks, (3, 2, 2), (6, 2, 2))
    pipe(blocks, (2, 2, 2), "east", "south")
    pipe_run(blocks, (2, 2, 3), (2, 2, 4))

    # Stubs of the secondary side, continued in plant_generation.
    pipe(blocks, (10, 5, 6), "down", "north")
    pipe(blocks, (10, 5, 5), "south", "north")
    pipe(blocks, (8, 2, 6), "east", "west")
    pipe(blocks, (7, 2, 6), "east", "west")
    return blocks


def main():
    write("plant_heat", SIZE, build())


if __name__ == "__main__":
    main()

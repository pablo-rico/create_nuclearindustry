#!/usr/bin/env python3
"""Writes the Ponder schematic of a reactor shut down safely or losing its cooling
(scenes plant_safe_shutdown and plant_loss_of_cooling).

    python3 tools/structgen/ponder_plant_shutdown.py

Layout (matches PlantScenes.safeShutdown and lossOfCooling), 15x9x15 on a smooth stone floor at
y 0. Ponder looks from the north-west, so the feed and bleed lines run north of the reactor, in
front of everything:
- reactor x 4..8, y 1..5, z 5..9 (12 fuel and 12 control rod blocks), controller (6,5,7); coolant
  port INPUT (5,2,5) and output port OUTPUT (7,3,5) in its north wall;
- feed line: a water tank (5,2,0) and a boric acid tank (3,2,1), each on a pedestal, join at
  (5,2,1); the feed pump (5,2,2) pushes south into the input port, turned through a cogwheel on
  top by a creative motor (5,3,1) of its own;
- bleed line: from the output port north to (7,3,2), then east through the bleed pump (8,3,2),
  turned by its own motor (9,4,2), and on along z 2 to the east edge, towards a cooling tower;
- an ordinary building of stone bricks around the reactor: walls x 2..10, z 3..11, y 1..7 and a
  roof at y 8; the two pipes pass through its north wall. Only the loss of cooling scene shows it.
"""
from ponder_plant_parts import (floor, pipe, pipe_run, pump, put, reactor, reactor_port, write, NS)

SIZE = (15, 9, 15)
WALL_LO, WALL_HI = (2, 3), (10, 11)
ROOF = 8


def tank(blocks, pos):
    """A one-block Create fluid tank on a pedestal."""
    put(blocks, (pos[0], pos[1] - 1, pos[2]), "minecraft:polished_andesite")
    put(blocks, pos, "create:fluid_tank", {"bottom": "true", "top": "true", "shape": "window"})


def building(blocks):
    (x0, z0), (x1, z1) = WALL_LO, WALL_HI
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            put(blocks, (x, ROOF, z), "minecraft:stone_bricks")
            if x in (x0, x1) or z in (z0, z1):
                for y in range(1, ROOF):
                    if (x, y, z) not in blocks:  # the pipes go through the wall
                        put(blocks, (x, y, z), "minecraft:stone_bricks")


def build():
    blocks = {}
    floor(blocks, SIZE)
    reactor(blocks, (4, 1, 5))
    reactor_port(blocks, (5, 2, 5), "north", "input")
    reactor_port(blocks, (7, 3, 5), "north", "output")

    # Feed: water and boric acid tanks -> pump with its own motor -> input port.
    tank(blocks, (5, 2, 0))
    tank(blocks, (3, 2, 1))
    pipe(blocks, (4, 2, 1), "east", "west")
    pipe(blocks, (5, 2, 1), "north", "south", "west")
    pump(blocks, (5, 2, 2), "south", "north")
    pipe_run(blocks, (5, 2, 3), (5, 2, 4))

    # Bleed: output port -> pump with its own motor -> east, towards a cooling tower.
    pipe_run(blocks, (7, 3, 3), (7, 3, 4))
    pipe(blocks, (7, 3, 2), "south", "east")
    pump(blocks, (8, 3, 2), "east", "east")
    pipe_run(blocks, (9, 3, 2), (14, 3, 2))

    building(blocks)
    return blocks


def main():
    write("plant_shutdown", SIZE, build())


if __name__ == "__main__":
    main()

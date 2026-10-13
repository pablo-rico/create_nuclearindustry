#!/usr/bin/env python3
"""Writes the Ponder schematic of the secondary loop (scene plant_generation).

    python3 tools/structgen/ponder_plant_generation.py

Layout (matches PlantScenes.generation), 15x14x15 on a smooth stone floor at y 0. Ponder looks
from the north-west, so the steam generator and the turbine stand in front and the cooling
tower at the back:
- steam generator x 1..3, y 1..4, z 1..3: controller (2,2,1) facing north, primary in (1,3,2)
  and out (1,2,2) with stubs from the reactor at x 0, steam out (2,4,2), feedwater in (2,2,3);
- steam line at y 5 with a pump (4,5,2) pushing east, down into the turbine's input (7,3,2);
- turbine x 5..9, y 1..3, z 1..3 along x: Turbine Output (5,2,2) facing west with its shaft
  at (4,2,2), rotors (6..8,2,2), glass on the north face (6..8,2,1), exhaust port (7,2,3);
- exhaust line south with a pump (7,2,5) into the tower's input port (7,2,7);
- the radius 4, 12 high cooling tower of ponder_cooling_tower.py centred on (10,1,10), its
  controller (10,2,6) facing north and its water output port (6,2,10) facing west;
- feedwater line from that port west and north with a pump (2,2,7) into the steam generator.
"""
from ponder_cooling_tower import classify, profile
from ponder_plant_parts import (floor, pipe, pipe_run, pump, put, replace, steam_generator, write, NS)

# One free layer over the tower: GameTests encase templates, and a ceiling on its top would read as a taller tower.
SIZE = (15, 14, 15)
RB, H = 4, 12
CX, CY, CZ = 10, 1, 10


def cooling_tower(blocks):
    radii = profile(RB, H)
    outer = [RB] + [max(radii[y], radii[y - 1] - 1, radii[y + 1] - 1 if y + 1 < H else 0) for y in range(1, H)]
    for y in range(H):
        for dx in range(-RB, RB + 1):
            for dz in range(-RB, RB + 1):
                if classify(radii, outer, RB, dx, y, dz) in ("floor", "wall"):
                    put(blocks, (CX + dx, CY + y, CZ + dz), NS + "cooling_tower_casing")
    replace(blocks, (CX, CY + 1, CZ - 4), NS + "cooling_tower_controller", {"facing": "north"})
    replace(blocks, (7, 2, 7), NS + "cooling_tower_fluid_port", {"facing": "north", "mode": "input"})
    replace(blocks, (6, 2, 10), NS + "cooling_tower_fluid_port", {"facing": "west", "mode": "output"})


def turbine(blocks):
    for x in range(5, 10):
        for y in range(1, 4):
            for z in range(1, 4):
                if y != 2 or z != 2 or x in (5, 9):
                    put(blocks, (x, y, z), NS + "turbine_casing")
    for x in range(6, 9):
        put(blocks, (x, 2, 2), NS + "turbine_rotor", {"axis": "x", "formed": "false"})
        replace(blocks, (x, 2, 1), NS + "reinforced_turbine_glass")
    replace(blocks, (5, 2, 2), NS + "turbine_output", {"facing": "west"})
    put(blocks, (4, 2, 2), "create:shaft", {"axis": "x", "waterlogged": "false"})
    replace(blocks, (7, 3, 2), NS + "turbine_fluid_port", {"facing": "up", "mode": "input"})
    replace(blocks, (7, 2, 3), NS + "turbine_exhaust_port", {"facing": "south"})


def build():
    blocks = {}
    floor(blocks, SIZE)
    steam_generator(blocks, (2, 1, 2), 4, "north", {
        (1, 3, 2): "primary_in",
        (1, 2, 2): "primary_out",
        (2, 4, 2): "steam_out",
        (2, 2, 3): "feed_in",
    })
    pipe(blocks, (0, 3, 2), "east", "west")
    pipe(blocks, (0, 2, 2), "east", "west")
    turbine(blocks)
    cooling_tower(blocks)

    # Steam: steam generator -> pump -> turbine input.
    pipe(blocks, (2, 5, 2), "down", "east")
    pipe(blocks, (3, 5, 2), "west", "east")
    pump(blocks, (4, 5, 2), "east", "west")
    pipe_run(blocks, (5, 5, 2), (6, 5, 2))
    pipe(blocks, (7, 5, 2), "west", "down")
    pipe(blocks, (7, 4, 2), "up", "down")

    # Exhaust: turbine exhaust port -> pump -> tower input.
    pipe(blocks, (7, 2, 4), "north", "south")
    pump(blocks, (7, 2, 5), "south", "north")
    pipe(blocks, (7, 2, 6), "north", "south")

    # Feedwater: tower output -> pump -> steam generator feedwater in.
    pipe_run(blocks, (3, 2, 10), (5, 2, 10))
    pipe(blocks, (2, 2, 10), "east", "north")
    pipe_run(blocks, (2, 2, 8), (2, 2, 9))
    pump(blocks, (2, 2, 7), "north", "south")
    pipe_run(blocks, (2, 2, 4), (2, 2, 6))
    return blocks


def main():
    write("plant_generation", SIZE, build())


if __name__ == "__main__":
    main()

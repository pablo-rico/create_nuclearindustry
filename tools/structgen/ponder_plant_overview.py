#!/usr/bin/env python3
"""Writes the Ponder schematic of the whole plant (scene plant_overview).

    python3 tools/structgen/ponder_plant_overview.py

Every part of a pressurized water plant on one 26x26 plate, joined the way a player joins them;
the scene zooms in on each in turn. Ponder looks from the north-west, so the low parts stand at
the front (north and west) and the cooling tower at the back. Layout (matches PlantScenes.overview):
- containment x 1..15, z 1..13: Containment Concrete floor at y 0, walls y 1..9, roof y 10, a
  filtered vent (15,8,4), a recombiner (14,1,2) and a monitor (8,1,12) inside. The scene shows
  it cut away, so its walls and roof only appear for one step; a containment door (12,1..2,1) in the
  north wall;
- inside it the primary loop of ponder_plant_heat.py moved by (+2, 0, +1): the reactor x 3..7,
  y 1..5, z 6..10 (controller (5,5,8), cold leg port (4,2,6), hot leg port (6,3,6)), the
  pressurizer on its lid (3,6..8,6), the steam generator x 11..13, y 1..4, z 6..8 (controller
  (12,2,6), primary in (11,3,6), primary out (13,2,6), feedwater in (11,2,7), steam out
  (12,4,7)), the hot leg at y 3 / z 5 and the cold leg at y 2 / z 3 with their pumps;
- the steam line from the steam out port, east at y 5 through a penetration (15,5,7) and a
  pump (17,5,7), down into the turbine's input (20,3,7);
- the turbine x 18..22, y 1..3, z 6..8 along x: rotors (19..21,2,7), glass (19..21,2,6), the
  Turbine Output (22,2,7) facing east with its shaft (23..24,2,7), the exhaust port (20,2,8);
- the exhaust line south with a pump (20,2,10) and west into the tower's input port (17,2,13);
- the radius 4, 12 high cooling tower centred on (20,1,16): controller (20,2,12), water output
  port (16,2,16) facing west;
- the feedwater line from that port north to (15,2,14), west with a pump (12,2,14), through a
  penetration in the south wall (10,2,13) and north inside to the feedwater port;
- the control room in front of the turbine: two control desks (18..19,1,1) facing north with an
  annunciator panel (18,1,3) hung on a concrete wall (18..19,1,4) and a chart recorder (19,1,3) behind them;
- the spent fuel pool x 1..7, z 16..22, walls y 1..4, water y 1..4, five racks on its floor at
  (3..5,1,18) and (3..4,1,20), a pool cooler (6,1,21) turned from a motor above the water, and
  two dry casks (9,1,18) and (9,1,20) beside it.
"""
from ponder_cooling_tower import classify, profile
from ponder_plant_parts import (floor, pipe, pipe_run, pump, put, reactor, reactor_port, replace, steam_generator, write, NS)

SIZE = (26, 14, 26)
CONTAINMENT_X, CONTAINMENT_Z, CONTAINMENT_TOP = (1, 15), (1, 13), 10
RB, H = 4, 12
CX, CY, CZ = 20, 1, 16


def containment(blocks):
    x0, x1 = CONTAINMENT_X
    z0, z1 = CONTAINMENT_Z
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            replace(blocks, (x, 0, z), NS + "containment_concrete")
            put(blocks, (x, CONTAINMENT_TOP, z), NS + "containment_concrete")
            if x in (x0, x1) or z in (z0, z1):
                for y in range(1, CONTAINMENT_TOP):
                    put(blocks, (x, y, z), NS + "containment_concrete")
    replace(blocks, (15, 5, 7), NS + "containment_penetration", {"axis": "x"})
    replace(blocks, (10, 2, 13), NS + "containment_penetration", {"axis": "z"})
    replace(blocks, (15, 8, 4), NS + "filtered_vent", {"open": "false"})
    # The way in: a containment door in the north wall, facing the control room.
    for y, half in ((1, "lower"), (2, "upper")):
        replace(blocks, (12, y, 1), NS + "containment_door", {"facing": "south", "half": half, "hinge": "left",
                                                              "open": "false", "powered": "false", "visible": "true"})
    put(blocks, (14, 1, 2), NS + "recombiner")
    put(blocks, (8, 1, 12), NS + "containment_monitor", {"facing": "north"})


def primary_loop(blocks):
    reactor(blocks, (3, 1, 6))
    reactor_port(blocks, (4, 2, 6), "north", "input")
    reactor_port(blocks, (6, 3, 6), "north", "output")
    put(blocks, (3, 6, 6), NS + "pressurizer_heater_port", {"automatic": "true"})
    put(blocks, (3, 7, 6), NS + "pressurizer")
    put(blocks, (3, 8, 6), NS + "relief_valve", {"open": "false"})
    steam_generator(blocks, (12, 1, 7), 4, "north", {
        (11, 3, 6): "primary_in",
        (13, 2, 6): "primary_out",
        (11, 2, 7): "feed_in",
        (12, 4, 7): "steam_out",
    })
    # Hot leg: reactor output port -> pump -> steam generator primary in.
    pipe(blocks, (6, 3, 5), "south", "east")
    pipe_run(blocks, (7, 3, 5), (8, 3, 5))
    pump(blocks, (9, 3, 5), "east", "west")
    pipe(blocks, (10, 3, 5), "east", "west")
    pipe(blocks, (11, 3, 5), "west", "south")
    # Cold leg: steam generator primary out -> pump -> reactor input port.
    pipe_run(blocks, (13, 2, 5), (13, 2, 4))
    pipe(blocks, (13, 2, 3), "south", "west")
    pipe_run(blocks, (10, 2, 3), (12, 2, 3))
    pump(blocks, (9, 2, 3), "west", "east")
    pipe_run(blocks, (5, 2, 3), (8, 2, 3))
    pipe(blocks, (4, 2, 3), "east", "south")
    pipe_run(blocks, (4, 2, 4), (4, 2, 5))


def turbine(blocks):
    for x in range(18, 23):
        for y in range(1, 4):
            for z in range(6, 9):
                if y != 2 or z != 7 or x in (18, 22):
                    put(blocks, (x, y, z), NS + "turbine_casing")
    for x in range(19, 22):
        put(blocks, (x, 2, 7), NS + "turbine_rotor", {"axis": "x", "formed": "false"})
        replace(blocks, (x, 2, 6), NS + "reinforced_turbine_glass")
    replace(blocks, (22, 2, 7), NS + "turbine_output", {"facing": "east"})
    put(blocks, (23, 2, 7), "create:shaft", {"axis": "x", "waterlogged": "false"})
    put(blocks, (24, 2, 7), "create:shaft", {"axis": "x", "waterlogged": "false"})
    replace(blocks, (20, 3, 7), NS + "turbine_fluid_port", {"facing": "up", "mode": "input"})
    replace(blocks, (20, 2, 8), NS + "turbine_exhaust_port", {"facing": "south"})


def cooling_tower(blocks):
    radii = profile(RB, H)
    outer = [RB] + [max(radii[y], radii[y - 1] - 1, radii[y + 1] - 1 if y + 1 < H else 0) for y in range(1, H)]
    for y in range(H):
        for dx in range(-RB, RB + 1):
            for dz in range(-RB, RB + 1):
                if classify(radii, outer, RB, dx, y, dz) in ("floor", "wall"):
                    put(blocks, (CX + dx, CY + y, CZ + dz), NS + "cooling_tower_casing")
    replace(blocks, (CX, CY + 1, CZ - 4), NS + "cooling_tower_controller", {"facing": "north"})
    replace(blocks, (CX - 3, CY + 1, CZ - 3), NS + "cooling_tower_fluid_port", {"facing": "north", "mode": "input"})
    replace(blocks, (CX - 4, CY + 1, CZ), NS + "cooling_tower_fluid_port", {"facing": "west", "mode": "output"})


def secondary_loop(blocks):
    # Steam: steam out -> through the east wall -> pump -> turbine input.
    pipe(blocks, (12, 5, 7), "down", "east")
    pipe_run(blocks, (13, 5, 7), (14, 5, 7))
    pipe(blocks, (16, 5, 7), "east", "west")
    pump(blocks, (17, 5, 7), "east", "west")
    pipe_run(blocks, (18, 5, 7), (19, 5, 7))
    pipe(blocks, (20, 5, 7), "west", "down")
    pipe(blocks, (20, 4, 7), "up", "down")
    # Exhaust: turbine exhaust port -> pump -> tower input.
    pipe(blocks, (20, 2, 9), "north", "south")
    pump(blocks, (20, 2, 10), "south", "north")
    pipe(blocks, (20, 2, 11), "north", "west")
    pipe_run(blocks, (18, 2, 11), (19, 2, 11))
    pipe(blocks, (17, 2, 11), "east", "south")
    pipe(blocks, (17, 2, 12), "north", "south")
    # Feedwater: tower output -> pump -> through the south wall -> steam generator feedwater in.
    pipe(blocks, (15, 2, 16), "north", "east")
    pipe(blocks, (15, 2, 15), "north", "south")
    pipe(blocks, (15, 2, 14), "west", "south")
    pipe_run(blocks, (13, 2, 14), (14, 2, 14))
    pump(blocks, (12, 2, 14), "west", "east")
    pipe(blocks, (11, 2, 14), "east", "west")
    pipe(blocks, (10, 2, 14), "north", "east")
    pipe_run(blocks, (10, 2, 8), (10, 2, 12))
    pipe(blocks, (10, 2, 7), "east", "south")


def control_room(blocks):
    desk = {"facing": "north", "shape": "straight"}
    put(blocks, (18, 1, 1), NS + "reactor_control_panel", dict(desk, left="true", right="false"))
    put(blocks, (19, 1, 1), NS + "reactor_control_panel", dict(desk, left="false", right="true"))
    put(blocks, (18, 1, 3), NS + "annunciator_panel", {"facing": "north", "mounted": "true", "left": "false", "right": "false",
                                                       "up": "false", "down": "false"})
    put(blocks, (19, 1, 3), NS + "chart_recorder", {"facing": "north"})
    # The instrument wall the annunciator hangs on.
    for x in (18, 19):
        put(blocks, (x, 1, 4), "minecraft:light_gray_concrete")


def spent_fuel(blocks):
    for x in range(1, 8):
        for z in range(16, 23):
            wall = x in (1, 7) or z in (16, 22)
            for y in range(1, 5):
                if wall:
                    put(blocks, (x, y, z), "minecraft:stone_bricks")
    for pos, load in {(3, 1, 18): "glowing", (4, 1, 18): "glowing", (5, 1, 18): "loaded",
                      (3, 1, 20): "loaded", (4, 1, 20): "empty"}.items():
        put(blocks, pos, NS + "spent_fuel_rack", {"waterlogged": "true", "load": load})
    put(blocks, (6, 1, 21), NS + "pool_cooler", {"waterlogged": "true"})
    for y in range(2, 5):
        put(blocks, (6, y, 21), "create:shaft", {"axis": "y", "waterlogged": "true"})
    put(blocks, (6, 5, 21), "create:shaft", {"axis": "y", "waterlogged": "false"})
    put(blocks, (6, 6, 21), "create:creative_motor", {"facing": "down"})
    for x in range(2, 7):
        for y in range(1, 5):
            for z in range(17, 22):
                if (x, y, z) not in blocks:
                    put(blocks, (x, y, z), "minecraft:water", {"level": "0"})
    put(blocks, (9, 1, 18), NS + "dry_cask")
    put(blocks, (9, 1, 20), NS + "dry_cask")


def build():
    blocks = {}
    floor(blocks, SIZE)
    containment(blocks)
    primary_loop(blocks)
    turbine(blocks)
    cooling_tower(blocks)
    secondary_loop(blocks)
    control_room(blocks)
    spent_fuel(blocks)
    return blocks


def main():
    write("plant_overview", SIZE, build())


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Writes the Ponder schematics of the nuclear engines (PropulsionScenes: thermal and antimatter).

    python3 tools/structgen/ponder_propulsion.py

Also written as GameTest templates, so a GameTest can prove the engines in the scenes form. Ponder
looks from the north-west: the engines lie along x with their exhaust east, so the controller's
operator panel faces west at the camera and the nozzles open away from it.
- nuclear_thermal_engine (7x4x5): the controller at (1,1,2) facing east, three heat exchanger
  segments (2..4,1,2) along x and the thermal rocket nozzle at (5,1,2) facing east; a lever on top of
  the controller (1,2,2) for the throttle.
- antimatter_engine (8x4x5): the controller at (1,1,2) facing east, four magnetic coils (2..5,1,2)
  along x and the magnetic nozzle at (6,1,2) facing east; a creative RF generator on top of the
  controller (1,2,2) for the coils' field.
"""
from ponder_plant_parts import NS, floor, put, write

THERMAL_SIZE = (7, 4, 5)
ANTIMATTER_SIZE = (8, 4, 5)


def engine(blocks, controller, segment, nozzle, segments):
    put(blocks, (1, 1, 2), NS + controller, {"facing": "east"})
    for x in range(2, 2 + segments):
        put(blocks, (x, 1, 2), NS + segment, {"axis": "x"})
    put(blocks, (2 + segments, 1, 2), NS + nozzle, {"facing": "east", "lit": "false"})


def thermal():
    blocks = {}
    floor(blocks, THERMAL_SIZE)
    engine(blocks, "nuclear_thermal_engine", "thermal_engine_exchanger", "thermal_rocket_nozzle", 3)
    put(blocks, (1, 2, 2), "minecraft:lever", {"face": "floor", "facing": "east", "powered": "false"})
    return blocks


def antimatter():
    blocks = {}
    floor(blocks, ANTIMATTER_SIZE)
    engine(blocks, "antimatter_engine", "magnetic_coil", "magnetic_nozzle", 4)
    put(blocks, (1, 2, 2), NS + "creative_rf_generator")
    return blocks


def main():
    write("nuclear_thermal_engine", THERMAL_SIZE, thermal())
    write("antimatter_engine", ANTIMATTER_SIZE, antimatter())


if __name__ == "__main__":
    main()

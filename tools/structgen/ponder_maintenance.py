#!/usr/bin/env python3
"""Writes the Ponder schematic of plant operation and maintenance (MaintenanceScenes.maintenance).

    python3 tools/structgen/ponder_maintenance.py

Also written as a GameTest template. Ponder looks from the north-west, so the parts face west.
- maintenance (9x6x7): the 5x5x5 fission reactor of the other scenes with its lowest corner at
  (1,1,1) on the floor, its fuel port in the middle of the west wall (1,3,3) between its coolant
  input (1,3,2) and steam output (1,3,4), all facing west, and a seismic detector on the floor at
  (7,1,2).
"""
from ponder_plant_parts import NS, floor, put, reactor, reactor_port, replace, write

SIZE = (9, 6, 7)


def maintenance():
    blocks = {}
    floor(blocks, SIZE)
    reactor(blocks, (1, 1, 1))
    replace(blocks, (1, 3, 3), NS + "reactor_fuel_port", {"facing": "west", "mode": "input"})
    reactor_port(blocks, (1, 3, 2), "west", "input")
    reactor_port(blocks, (1, 3, 4), "west", "output")
    put(blocks, (7, 1, 2), NS + "seismic_detector")
    return blocks


def main():
    write("maintenance", SIZE, maintenance())


if __name__ == "__main__":
    main()

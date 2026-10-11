#!/usr/bin/env python3
"""Writes the Ponder schematics of the particle accelerator (AcceleratorScenes).

    python3 tools/structgen/ponder_accelerator.py

Also written as GameTest templates, so a GameTest can prove each one forms.
- accelerator_linac (9x3x5): a linac along x at z 2. Controller (0,1,2) facing east (the beam's
  way), ion source (1,1,2) with its shaft up, quadrupole (2), RF cavities (3, 4), beam pipe (5),
  quadrupole (6), beam pipe (7), beam dump (8). North of the line, where the Ponder camera sees
  them, a klystron (3,1,1), a magnet power supply (5,1,1) and a vacuum pump (6,1,1), their shafts
  facing north, away from the line.
"""
from ponder_plant_parts import NS, floor, put, write

LINAC_SIZE = (9, 3, 5)


def linac():
    blocks = {}
    floor(blocks, LINAC_SIZE)
    put(blocks, (0, 1, 2), NS + "particle_accelerator_controller", {"facing": "east", "on": "false"})
    put(blocks, (1, 1, 2), NS + "particle_accelerator_ion_source", {"facing": "up"})
    line = ["beam_quadrupole", "beam_rf_cavity", "beam_rf_cavity", "beam_pipe", "beam_quadrupole", "beam_pipe"]
    for i, block in enumerate(line):
        put(blocks, (2 + i, 1, 2), NS + block, {"axis": "x"})
    put(blocks, (8, 1, 2), NS + "beam_dump")
    put(blocks, (3, 1, 1), NS + "particle_accelerator_klystron", {"facing": "north"})
    put(blocks, (5, 1, 1), NS + "particle_accelerator_magnet_power_supply", {"facing": "north"})
    put(blocks, (6, 1, 1), NS + "particle_accelerator_vacuum_pump", {"facing": "north"})
    return blocks


def main():
    write("accelerator_linac", LINAC_SIZE, linac())


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Writes the Ponder schematics of the particle accelerator (AcceleratorScenes).

    python3 tools/structgen/ponder_accelerator.py

Also written as GameTest templates, so a GameTest can prove each one forms.
- accelerator_linac (9x3x5): a linac along x at z 2. Controller (0,1,2) facing east (the beam's
  way), ion source (1,1,2) with its shaft up, quadrupole (2), RF cavities (3, 4), beam pipe (5),
  quadrupole (6), beam pipe (7), beam dump (8). North of the line, where the Ponder camera sees
  them, a klystron (3,1,1), a magnet power supply (5,1,1) and a vacuum pump (6,1,1), their shafts
  facing north, away from the line.
- accelerator_synchrotron (16x3x16): a synchrotron ring of radius 6 round (8,1,8), fed by a short
  injector from the west: controller (0,1,8) facing east, ion source (1,1,8), injection septum (2,1,8)
  facing the centre. Ring dipoles all round, one quadrupole per sector, superconducting dipoles on
  the west arc with a cryo feed (1,1,10) and a beam interlock (1,1,6) beside them, a superconducting
  RF cavity (7,1,2) with its klystron (7,1,1), a magnet power supply (8,1,1) and a vacuum pump
  (10,1,1) north of the ring, where the Ponder camera sees them. The extraction septum (14,1,8)
  sends the beam east into the target station (15,1,8), its window west; a small Penning trap
  (15,1,7) beside the station takes the antiprotons, fed by a creative RF generator (15,1,6).
"""
import math

from ponder_plant_parts import NS, floor, put, write

LINAC_SIZE = (9, 3, 5)
SYNCHROTRON_SIZE = (16, 3, 16)
RING_CENTRE = (8, 8)
RING_RADIUS = 6


def rho(dx, dz):
    """Rounded distance of a column from the ring's centre, as TokamakGeometry.rho works it out."""
    four = 4 * (dx * dx + dz * dz)
    r = int(math.floor(math.sqrt(dx * dx + dz * dz) + 0.5))
    while r > 0 and (2 * r - 1) ** 2 > four:
        r -= 1
    while (2 * r + 1) ** 2 <= four:
        r += 1
    return r


def ring_cells(radius):
    return [(dx, dz) for dx in range(-radius - 1, radius + 2) for dz in range(-radius - 1, radius + 2) if rho(dx, dz) == radius]


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


def synchrotron():
    blocks = {}
    floor(blocks, SYNCHROTRON_SIZE)
    cx, cz = RING_CENTRE
    special = {
        (-6, 0): ("injection_septum", {"facing": "east"}),
        (6, 0): ("extraction_septum", {"facing": "east"}),
        # One quadrupole per sector (five of about eight cells each).
        (-4, -4): ("beam_quadrupole", None),
        (1, -6): ("beam_quadrupole", None),
        (6, 2): ("beam_quadrupole", None),
        (0, 6): ("beam_quadrupole", None),
        (-4, 4): ("beam_quadrupole", None),
        (-1, -6): ("superconducting_rf_cavity", None),
    }
    for dz in (-2, -1, 1, 2):
        special[(-6, dz)] = ("superconducting_ring_dipole", None)
    for dx, dz in ring_cells(RING_RADIUS):
        name, props = special.get((dx, dz), ("ring_dipole_magnet", None))
        if props is None:
            # Along the ring: north-south on the east and west arcs, east-west on the others.
            props = {"axis": "z" if abs(dx) > abs(dz) else "x"}
        put(blocks, (cx + dx, 1, cz + dz), NS + name, props)
    put(blocks, (0, 1, 8), NS + "particle_accelerator_controller", {"facing": "east", "on": "false"})
    put(blocks, (1, 1, 8), NS + "particle_accelerator_ion_source", {"facing": "up"})
    put(blocks, (1, 1, 10), NS + "accelerator_cryo_feed", {"facing": "west"})
    put(blocks, (1, 1, 6), NS + "beam_interlock", {"facing": "west", "secured": "false"})
    put(blocks, (7, 1, 1), NS + "particle_accelerator_klystron", {"facing": "north"})
    put(blocks, (8, 1, 1), NS + "particle_accelerator_magnet_power_supply", {"facing": "north"})
    put(blocks, (10, 1, 1), NS + "particle_accelerator_vacuum_pump", {"facing": "north"})
    put(blocks, (15, 1, 8), NS + "target_station", {"facing": "west"})
    put(blocks, (15, 1, 7), NS + "small_penning_trap", {"facing": "west"})
    put(blocks, (15, 1, 6), NS + "creative_rf_generator")
    return blocks


def main():
    write("accelerator_linac", LINAC_SIZE, linac())
    write("accelerator_synchrotron", SYNCHROTRON_SIZE, synchrotron())


if __name__ == "__main__":
    main()

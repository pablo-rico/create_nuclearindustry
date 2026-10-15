#!/usr/bin/env python3
"""Writes the Ponder schematics of using antimatter (AntimatterScenes: generator and injector).

    python3 tools/structgen/ponder_antimatter_use.py

Also written as GameTest templates, so a GameTest can prove the layouts work. Ponder looks from the
north-west, so the parts face north or west.
- antimatter_generator (7x4x5): the annihilation generator at (1,1,2), its shaft east (FACING east)
  so the core porthole on its back looks west at the camera; a shaft line (2..5,1,2) along x; the
  small Penning trap that feeds it at (1,1,3), south of it, front west beside the porthole (north of
  the generator it would hide it from the camera); a creative RF generator on top of the trap (1,2,3)
  to charge the field until the generator, in ELECTRIC or SPLIT mode, takes over (the scene removes
  it to show the closed loop).
- antiproton_injector (15x7x15): the R=4 tokamak with the parts of tokamak_startup, and the
  antiproton injector replacing a top corner of the shell at (2,5,3) (dx -5, dz -4 from the axis),
  facing west: that block has three free faces, west, north and up. Its port looks west at the camera,
  a small trap north of it (2,5,2), front west, feeds it, and a creative RF generator on each (2,6,3),
  (2,6,2) powers the injector and the trap.
"""
import pathlib

from ponder_plant_parts import NS, floor, put, replace, write
from ponder_tokamak import CENTER, SIZE
from ponder_tokamak import build

ROOT = pathlib.Path(__file__).resolve().parents[2]
GENERATOR_SIZE = (7, 4, 5)
INJECTOR_SIZE = (SIZE, 7, SIZE)


def generator():
    blocks = {}
    floor(blocks, GENERATOR_SIZE)
    put(blocks, (1, 1, 2), NS + "annihilation_generator", {"facing": "east"})
    for x in range(2, 6):
        put(blocks, (x, 1, 2), "create:shaft", {"axis": "x"})
    put(blocks, (1, 1, 3), NS + "small_penning_trap", {"facing": "west"})
    put(blocks, (1, 2, 3), NS + "creative_rf_generator")
    return blocks


def startup_tokamak():
    """The tokamak of the startup scene: ponder_tokamak.build() plus its shell parts."""
    blocks = build()
    cx, cy, cz = CENTER
    north, west = cz - 6, cx - 6
    blocks[(cx - 2, cy, north)] = (NS + "fusion_vacuum_pump", {"facing": "north"})
    blocks[(cx - 2, cy, north - 1)] = ("create:shaft", {"axis": "z"})
    blocks[(cx + 2, cy + 1, north)] = (NS + "fusion_cryo_port", {"mode": "input"})
    blocks[(cx + 2, cy - 1, north)] = (NS + "fusion_cryo_port", {"mode": "output"})
    blocks[(cx - 1, cy + 1, north)] = (NS + "fusion_viewport", {"glow": "0"})
    blocks[(cx + 1, cy + 1, north)] = (NS + "fusion_viewport", {"glow": "0"})
    blocks[(west, cy, cz + 2)] = (NS + "fusion_neutral_beam_injector", {"facing": "west"})
    blocks[(west - 1, cy, cz + 2)] = ("create:shaft", {"axis": "x"})
    blocks[(west, cy, cz - 2)] = (NS + "fusion_rf_antenna", {"facing": "west"})
    blocks[(west - 1, cy, cz - 2)] = ("create:shaft", {"axis": "x"})
    return blocks


def injector():
    blocks = startup_tokamak()
    cx, cy, cz = CENTER
    corner = (cx - 5, cy + 2, cz - 4)
    replace(blocks, corner, NS + "antiproton_injector", {"facing": "west"})
    x, y, z = corner
    put(blocks, (x, y, z - 1), NS + "small_penning_trap", {"facing": "west"})
    put(blocks, (x, y + 1, z), NS + "creative_rf_generator")
    put(blocks, (x, y + 1, z - 1), NS + "creative_rf_generator")
    return blocks


def main():
    write("antimatter_generator", GENERATOR_SIZE, generator())
    write("antiproton_injector", INJECTOR_SIZE, injector())


if __name__ == "__main__":
    main()

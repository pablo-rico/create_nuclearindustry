#!/usr/bin/env python3
"""Writes the Ponder schematics of a 5x5x7 steam turbine and plasma turbine.

    python3 tools/structgen/ponder_turbine.py

Layout (matches TurbineScenes): base plate at y=0; turbine along Z over x 2..6, y 1..5,
z 1..7. Ponder looks from the north-west, so the parts worth seeing face that way: the
controller at (4,3,1) facing north with its shaft at (4,3,0), and glass across the west
wall (x=2, y 2..4, z 2..6) to watch the rotor. Rotors at (4,3,2..6); input port on top at
(4,5,4); output port on the east wall at (6,3,4) (the scene turns the camera for it).
Each schematic is also written as a GameTest template so a test can prove it forms.
"""
import pathlib

import nbtwriter

ROOT = pathlib.Path(__file__).resolve().parents[2]
NS = "create_nuclearindustry:"
SIZE = (9, 7, 9)

TIERS = {
    "turbine": {
        "casing": "turbine_casing", "rotor": "turbine_rotor",
        "port": "turbine_fluid_port", "controller": "turbine_output",
    },
    "fusion_turbine": {
        "casing": "fusion_turbine_casing", "rotor": "fusion_turbine_rotor",
        "port": "fusion_turbine_fluid_port", "controller": "fusion_plasma_turbine",
    },
}


def build(tier):
    blocks = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            blocks[(x, 0, z)] = ("minecraft:smooth_stone", {})
    for x in range(2, 7):
        for y in range(1, 6):
            for z in range(1, 8):
                wall = x in (2, 6) or y in (1, 5) or z in (1, 7)
                if wall:
                    blocks[(x, y, z)] = (NS + tier["casing"], {})
    for z in range(2, 7):
        blocks[(4, 3, z)] = (NS + tier["rotor"], {"axis": "z", "formed": "false"})
    for y in range(2, 5):
        for z in range(2, 7):
            blocks[(2, y, z)] = (NS + "reinforced_turbine_glass", {})
    blocks[(4, 5, 4)] = (NS + tier["port"], {"facing": "up", "mode": "input"})
    blocks[(6, 3, 4)] = (NS + tier["port"], {"facing": "east", "mode": "output"})
    blocks[(4, 3, 1)] = (NS + tier["controller"], {"facing": "north"})
    blocks[(4, 3, 0)] = ("create:shaft", {"axis": "z"})
    return nbtwriter.structure(SIZE, blocks)


def main():
    for name, tier in TIERS.items():
        data = build(tier)
        for path in (ROOT / f"src/main/resources/assets/create_nuclearindustry/ponder/{name}.nbt",
                     ROOT / f"src/main/resources/data/create_nuclearindustry/structure/gametest/{name}_ponder.nbt"):
            path.parent.mkdir(parents=True, exist_ok=True)
            nbtwriter.write(path, data)
            print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Writes the Ponder schematic of an R=4 tokamak.

    python3 tools/structgen/ponder_tokamak.py

Mirrors TokamakGeometry (TokamakScenes relies on this layout): rho = round(sqrt(dx^2 + dz^2)); plasma |rho-R|<=1, |dy|<=1;
the four corners |rho-R|==2, |dy|==2 stay empty (octagonal section); shell is the rest of |rho-R|<=2, |dy|<=2.
Layout matches TokamakScenes.
"""
import math
import pathlib

import nbtwriter

R = 4
SIZE = 15
CENTER = (7, 3, 7)  # y=0 is the base plate
ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/tokamak.nbt"
# Same structure as a GameTest template, so a test can prove the schematic forms.
TEST_OUT = ROOT / "src/main/resources/data/create_nuclearindustry/structure/gametest/tokamak_r4.nbt"
NS = "create_nuclearindustry:"


def rho(dx, dz):
    four_d2 = 4 * (dx * dx + dz * dz)
    r = int(math.floor(math.sqrt(dx * dx + dz * dz) + 0.5))
    while r > 0 and (2 * r - 1) ** 2 > four_d2:
        r -= 1
    while (2 * r + 1) ** 2 <= four_d2:
        r += 1
    return r


def build():
    """Every block of the R=4 tokamak and its base plate, by position."""
    blocks = {}
    for x in range(SIZE):
        for z in range(SIZE):
            blocks[(x, 0, z)] = ("minecraft:smooth_stone", {})

    cx, cy, cz = CENTER
    for dx in range(-R - 2, R + 3):
        for dz in range(-R - 2, R + 3):
            offset = abs(rho(dx, dz) - R)
            if offset > 2:
                continue
            for dy in range(-2, 3):
                plasma = offset <= 1 and abs(dy) <= 1
                if plasma or (offset == 2 and abs(dy) == 2):
                    continue
                if abs(dy) == 2:
                    block = NS + ("fusion_breeding_blanket" if offset <= 1 else "fusion_accelerator_segment")
                else:
                    block = NS + "fusion_electromagnet"
                blocks[(cx + dx, cy + dy, cz + dz)] = (block, {})

    for dy in range(-2, 3):
        segment = "bottom" if dy == -2 else "top" if dy == 2 else "middle"
        blocks[(cx, cy + dy, cz)] = (NS + "fusion_central_solenoid", {"axis": "y", "segment": segment})

    # Ponder looks from the north-west: controller, ports and magnet drive face that way;
    # the injector sits on the east side and the scene turns the camera to show it.
    edge = R + 2
    blocks[(cx - edge, cy, cz)] = (NS + "fusion_controller", {"on": "false", "ignited": "false"})
    blocks[(cx - edge, cy + 1, cz)] = (NS + "fusion_fluid_port", {"facing": "west", "mode": "input"})
    blocks[(cx - edge, cy - 1, cz)] = (NS + "fusion_fluid_port", {"facing": "west", "mode": "output"})
    blocks[(cx + edge, cy, cz)] = (NS + "fusion_fuel_injector", {"facing": "east", "mode": "input"})
    blocks[(cx, cy, cz - edge)] = (NS + "fusion_magnet_input", {"facing": "north"})
    blocks[(cx, cy, cz - edge - 1)] = ("create:shaft", {"axis": "z"})
    return blocks


def main():
    data = nbtwriter.structure((SIZE, 6, SIZE), build())
    for path in (OUT, TEST_OUT):
        path.parent.mkdir(parents=True, exist_ok=True)
        nbtwriter.write(path, data)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

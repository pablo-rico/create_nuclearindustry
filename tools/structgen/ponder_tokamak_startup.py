#!/usr/bin/env python3
"""Writes the Ponder schematic of the tokamak startup scene: the R=4 tokamak of ponder_tokamak.py
with the parts a start needs in its shell.

    python3 tools/structgen/ponder_tokamak_startup.py

Ponder looks from the north-west, so every new part sits on the north or west wall: a vacuum pump
and a pair of cryogenic ports on the north wall beside the magnet drive, two viewports above it,
and a neutral beam injector and an RF antenna on the west wall beside the controller. Layout
matches TokamakScenes.startup; a GameTest checks the copy in the test structures forms.
"""
import pathlib

import nbtwriter
from ponder_tokamak import CENTER, NS, SIZE, build

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/tokamak_startup.nbt"
TEST_OUT = ROOT / "src/main/resources/data/create_nuclearindustry/structure/gametest/tokamak_startup_r4.nbt"


def main():
    blocks = build()
    cx, cy, cz = CENTER
    north = cz - 6
    west = cx - 6
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

    data = nbtwriter.structure((SIZE, 6, SIZE), blocks)
    for path in (OUT, TEST_OUT):
        path.parent.mkdir(parents=True, exist_ok=True)
        nbtwriter.write(path, data)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

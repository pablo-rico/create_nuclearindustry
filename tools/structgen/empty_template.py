#!/usr/bin/env python3
"""Writes empty structure templates used as GameTest arenas.

GameTests build their multiblocks in code, so the templates only need to
reserve space. Usage:

    python3 tools/structgen/empty_template.py
"""
import pathlib

import nbtwriter

OUT = pathlib.Path(__file__).resolve().parents[2] / "src/main/resources/data/create_nuclearindustry/structure/gametest"

# name -> (x, y, z)
TEMPLATES = {
    "empty_13x16x13": (13, 16, 13),
    "empty_41x9x41": (41, 9, 41),
    "empty_11x10x20": (11, 10, 20),
    "empty_19x26x19": (19, 26, 19),
}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, size in TEMPLATES.items():
        path = OUT / f"{name}.nbt"
        nbtwriter.write(path, nbtwriter.structure(size, {}))
        print(f"wrote {path.relative_to(OUT.parents[6])}")


if __name__ == "__main__":
    main()

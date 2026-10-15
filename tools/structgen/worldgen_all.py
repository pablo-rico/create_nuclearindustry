#!/usr/bin/env python3
"""Writes every structure template of the abandoned nuclear sites.

    python3 tools/structgen/worldgen_all.py

The output is deterministic: running it again must not change any .nbt file. The template list must
match RuinsLoot.PIECES (a GameTest checks it); a file under structure/ruins that no script writes any
more (a renamed piece) is reported, so it can be deleted.
"""
import worldgen_civil_shelter
import worldgen_military_bunker
import worldgen_plant
from worldgen_common import OUT, ROOT, WRITTEN


def main():
    for module in (worldgen_plant, worldgen_civil_shelter, worldgen_military_bunker):
        module.main()
    for path in sorted(set(OUT.rglob("*.nbt")) - set(WRITTEN)):
        print(f"stale template, not written by any script: {path.relative_to(ROOT)}")
    print(f"{len(WRITTEN)} templates")


if __name__ == "__main__":
    main()

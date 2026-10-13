#!/usr/bin/env python3
"""Writes the Ponder schematic of a small control room next to a stand-in reactor.

    python3 tools/structgen/ponder_control_room.py

Layout (matches ControlRoomScenes), 9x4x9 on a smooth stone floor at y 0:
a 3x2 monitor wall at x 1..3, y 1..2, z 6 facing north; a console of two control desks at
(1..2, 1, 3) facing north, joined to each other, and the alarm at (4, 1, 3); a 3x2x3 block of reactor casing at x 6..8, z 5..7 with its controller
on top at (7, 3, 6) and a redstone port in Withdraw rods mode on its west face at (6, 1, 6).
Ponder looks from the north-west, so every front faces north or west.
"""
import pathlib

import nbtwriter

ROOT = pathlib.Path(__file__).resolve().parents[2]
SIZE = (9, 4, 9)
MOD = "create_nuclearindustry:"


def build():
    blocks = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            blocks[(x, 0, z)] = ("minecraft:smooth_stone", {})
    for x in range(1, 4):
        for y in (1, 2):
            blocks[(x, y, 6)] = (MOD + "control_monitor", {"facing": "north"})
    # Operator to the north: the desk at x 1 has the other on its left (east), the one at x 2 on its right.
    desk = {"facing": "north", "shape": "straight"}
    blocks[(1, 1, 3)] = (MOD + "reactor_control_panel", dict(desk, left="true", right="false"))
    blocks[(2, 1, 3)] = (MOD + "reactor_control_panel", dict(desk, left="false", right="true"))
    blocks[(4, 1, 3)] = (MOD + "nuclear_alarm", {"lit": "false"})
    for x in range(6, 9):
        for y in (1, 2):
            for z in range(5, 8):
                blocks[(x, y, z)] = (MOD + "reactor_casing", {})
    blocks[(7, 3, 6)] = (MOD + "reactor_controller", {"on": "false"})
    blocks[(6, 1, 6)] = (MOD + "reactor_redstone_port", {"facing": "west", "mode": "rod_withdraw", "power": "0"})
    return nbtwriter.structure(SIZE, blocks)


def main():
    path = ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/control_room.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    nbtwriter.write(path, build())
    print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

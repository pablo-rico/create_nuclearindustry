#!/usr/bin/env python3
"""Writes the Ponder schematic of the smallest cooling tower (base radius 4, 12 high).

    python3 tools/structgen/ponder_cooling_tower.py

Mirrors CoolingTowerGeometry exactly (integer arithmetic), so the tower forms in game.
Layout (matches CoolingTowerScenes): smooth stone plate at y=0, tower floor at y=1 centred
on (5,1,5), layers up to y=12. Ponder looks from the north-west, so everything worth seeing
faces that way: the controller at (1,2,5) facing west, a steam input port in the floor at
(2,1,2), a water output port in the wall at (3,3,3), and a fan at (5,2,1) facing north with
its shaft at (5,2,0). Also written as a GameTest template so a test can prove it forms.
"""
import math
import pathlib

import nbtwriter

ROOT = pathlib.Path(__file__).resolve().parents[2]
NS = "create_nuclearindustry:"
RB, H = 4, 12
CX, CY, CZ = 5, 1, 5
SIZE = (11, 14, 11)


def rho(dx, dz):
    four_d2 = 4 * (dx * dx + dz * dz)
    r = int(math.floor(math.sqrt(dx * dx + dz * dz) + 0.5))
    while r > 0 and (2 * r - 1) ** 2 > four_d2:
        r -= 1
    while (2 * r + 1) ** 2 <= four_d2:
        r += 1
    return r


def waist_radius(rb):
    return max(2, (3 * rb + 2) // 5)


def waist_height(h):
    return (3 * (h - 1) + 2) // 4


def profile(rb, h):
    a, yw = waist_radius(rb), waist_height(h)
    radii = []
    for y in range(h):
        n = 4 * (a * a * yw * yw + (rb * rb - a * a) * (y - yw) ** 2)
        r = round(math.sqrt(n / (4.0 * yw * yw)))
        while r > 0 and (2 * r - 1) ** 2 * yw * yw > n:
            r -= 1
        while (2 * r + 1) ** 2 * yw * yw <= n:
            r += 1
        radii.append(r)
    return radii


def classify(radii, outer, rb, dx, y, dz):
    d = rho(dx, dz)
    if y == 0:
        return "floor" if d <= rb else None
    if d < radii[y]:
        return "interior"
    return "wall" if d <= outer[y] else None


def build():
    radii = profile(RB, H)
    outer = [RB] + [max(radii[y], radii[y - 1] - 1, radii[y + 1] - 1 if y + 1 < H else 0) for y in range(1, H)]
    blocks = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            blocks[(x, 0, z)] = ("minecraft:smooth_stone", {})
    for y in range(H):
        for dx in range(-RB, RB + 1):
            for dz in range(-RB, RB + 1):
                if classify(radii, outer, RB, dx, y, dz) in ("floor", "wall"):
                    blocks[(CX + dx, CY + y, CZ + dz)] = (NS + "cooling_tower_casing", {})
    assert radii[1] == 4
    blocks[(CX - 4, CY + 1, CZ)] = (NS + "cooling_tower_controller", {"facing": "west"})
    blocks[(CX - 3, CY, CZ - 3)] = (NS + "cooling_tower_fluid_port", {"facing": "north", "mode": "input"})
    blocks[(CX - 2, CY + 2, CZ - 2)] = (NS + "cooling_tower_fluid_port", {"facing": "north", "mode": "output"})
    blocks[(CX, CY + 1, CZ - 4)] = (NS + "cooling_tower_fan", {"facing": "north"})
    blocks[(CX, CY + 1, CZ - 5)] = ("create:shaft", {"axis": "z"})
    return nbtwriter.structure(SIZE, blocks)


def main():
    data = build()
    for path in (ROOT / "src/main/resources/assets/create_nuclearindustry/ponder/cooling_tower.nbt",
                 ROOT / "src/main/resources/data/create_nuclearindustry/structure/gametest/cooling_tower_ponder.nbt"):
        path.parent.mkdir(parents=True, exist_ok=True)
        nbtwriter.write(path, data)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()

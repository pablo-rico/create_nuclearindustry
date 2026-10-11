#!/usr/bin/env python3
"""Writes the Ponder schematics of the civil defense scenes (CivilDefenseScenes).

    python3 tools/structgen/ponder_civil_defense.py

Each is also written as a GameTest template, so GuidePonderSchematicTests can prove that the door
shown forms and the shelter shown is certified. Ponder looks from the north-west, so the parts
that matter face north or west.

- civil_defense_sirens, 6x4x5: an Air Raid Siren (1,2,2) on a polished andesite post (1,1,2)
  with a lever on the post's west face (0,1,2); a Hand-Crank Siren (4,2,2) on a gearbox (4,1,2)
  turned by a hand crank on its north face (4,1,1).
- civil_defense_blast_door, 9x7x5: a 3x3 steel blast door, leaves at x 3..5, y 2..4, z 2, in a
  ring of Blast Door Frame (x 2..6, y 1..5) whose controller (2,2,2) runs its shaft along z and
  has a lever on its north face (2,2,1). Gray concrete carries the wall on to x 0..1 and 7..8.
- civil_defense_shelter, 8x5x8: a sealed room of deepslate bricks, shell x 1..6, y 1..4,
  z 1..6, air inside at x 2..5, y 2..3, z 2..5. In it a Shelter Filtration Unit (4,2,4) on the
  x axis driven by a creative motor (5,2,4), and a Seismic Detector (2,2,4) read by a comparator
  (2,2,3) that lights redstone dust (2,2,2). The scene leaves the north and west walls and the
  roof out so the camera sees in.
"""
from ponder_plant_parts import NS, floor, put, write


def sirens():
    size = (6, 4, 5)
    blocks = {}
    floor(blocks, size)
    put(blocks, (1, 1, 2), "minecraft:polished_andesite")
    put(blocks, (1, 2, 2), NS + "air_raid_siren", {"sounding": "false"})
    put(blocks, (0, 1, 2), "minecraft:lever", {"face": "wall", "facing": "west", "powered": "false"})
    put(blocks, (4, 1, 2), "create:gearbox", {"axis": "x"})
    put(blocks, (4, 1, 1), "create:hand_crank", {"facing": "north", "waterlogged": "false"})
    put(blocks, (4, 2, 2), NS + "hand_crank_siren", {"sounding": "false"})
    write("civil_defense_sirens", size, blocks)


def blast_door():
    size = (9, 7, 5)
    blocks = {}
    floor(blocks, size)
    for x in range(2, 7):
        for y in range(1, 6):
            leaf = 3 <= x <= 5 and 2 <= y <= 4
            put(blocks, (x, y, 2), NS + ("steel_blast_door" if leaf else "blast_door_frame"), {"leaf": "closed"} if leaf else {})
    blocks[(2, 2, 2)] = (NS + "blast_door_controller", {"axis": "z"})
    put(blocks, (2, 2, 1), "minecraft:lever", {"face": "wall", "facing": "north", "powered": "false"})
    for x in (0, 1, 7, 8):
        for y in range(1, 6):
            put(blocks, (x, y, 2), "minecraft:gray_concrete")
    write("civil_defense_blast_door", size, blocks)


def shelter():
    size = (8, 5, 8)
    blocks = {}
    floor(blocks, size)
    for x in range(1, 7):
        for y in range(1, 5):
            for z in range(1, 7):
                if x in (1, 6) or y in (1, 4) or z in (1, 6):
                    put(blocks, (x, y, z), "minecraft:deepslate_bricks")
    put(blocks, (4, 2, 4), NS + "shelter_filtration_unit", {"axis": "x"})
    put(blocks, (5, 2, 4), "create:creative_motor", {"facing": "west"})
    put(blocks, (2, 2, 4), NS + "seismic_detector")
    put(blocks, (2, 2, 3), "minecraft:comparator", {"facing": "south", "mode": "compare", "powered": "false"})
    put(blocks, (2, 2, 2), "minecraft:redstone_wire",
        {"north": "none", "south": "side", "east": "none", "west": "none", "power": "0"})
    write("civil_defense_shelter", size, blocks)


def main():
    sirens()
    blast_door()
    shelter()


if __name__ == "__main__":
    main()

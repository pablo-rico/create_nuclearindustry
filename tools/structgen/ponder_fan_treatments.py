#!/usr/bin/env python3
"""Writes the Ponder schematics of the two fan treatments (FanTreatmentScenes).

    python3 tools/structgen/ponder_fan_treatments.py

Also written as GameTest templates, so GuidePonderSchematicTests can prove that the catalyst the
scene shows really makes its treatment. Both are 6x3x5, along x at z 2: a creative motor (0,1,2)
drives an encased fan (1,1,2) blowing east, past the catalyst (2,1,2), onto a depot (3,1,2).
- fan_acid_leaching: the catalyst is a source of sulfuric acid, walled in by glass to its north
  and south (the fan and the depot hold it on the other sides), so it cannot run off;
- fan_irradiation: the catalyst is a vitrified waste canister, radioactive enough on its own.
"""
from ponder_plant_parts import NS, floor, put, write

SIZE = (6, 3, 5)


def station(catalyst, props=None):
    blocks = {}
    floor(blocks, SIZE)
    put(blocks, (0, 1, 2), "create:creative_motor", {"facing": "east"})
    put(blocks, (1, 1, 2), "create:encased_fan", {"facing": "east"})
    put(blocks, (2, 1, 2), catalyst, props)
    put(blocks, (3, 1, 2), "create:depot")
    return blocks


def main():
    acid = station(NS + "sulfuric_acid", {"level": "0"})
    put(acid, (2, 1, 1), "minecraft:glass")
    put(acid, (2, 1, 3), "minecraft:glass")
    write("fan_acid_leaching", SIZE, acid)
    write("fan_irradiation", SIZE, station(NS + "vitrified_waste_canister"))


if __name__ == "__main__":
    main()

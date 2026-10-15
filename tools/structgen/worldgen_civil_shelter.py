#!/usr/bin/env python3
"""Writes the pieces of the abandoned civil fallout shelter (structure create_nuclearindustry:civil_shelter).

    python3 tools/structgen/worldgen_civil_shelter.py

A concrete blockhouse on the surface, with an air raid siren on its roof, covers a stairwell that goes
down eight blocks to the shelter's rooms: dormitories, a supply store and the ventilation room. Rooms
have a second door that leads on to another room or ends in a bricked-up wall. Doors are one block wide
and two tall, with the jigsaw in the floor under them.
"""
from worldgen_common import NS, Piece

S = "civil_shelter"
ENTRY = NS + "shelter_entry"
STAIRS = NS + "shelter_stairs"

FLOOR = "gray_concrete"
WALL = "white_concrete"
STRIPE = "yellow_concrete"
CEILING = "light_gray_concrete"


def pool(name):
    return NS + "ruins/" + S + "/" + name


def entrance():
    """7x6x7: a blockhouse set back one block behind sandbags and its sign; the open stairwell in its floor
    (x 2..4, z 2..4) drops into the stair corridor."""
    p = Piece(S + "/entrance", (7, 6, 7))
    p.fill((0, 1, 0), (6, 5, 6), "air")
    p.fill((0, 0, 0), (6, 0, 6), FLOOR)
    for y in range(1, 4):
        p.walls((0, y, 1), (6, y, 6), WALL)
    for x in range(7):
        p.put((x, 1, 1), STRIPE if x % 2 == 0 else "black_concrete")
    p.fill((0, 4, 1), (6, 4, 6), CEILING)
    for x in range(7):
        for z in range(1, 7):
            if x in (0, 6) or z in (1, 6):
                p.slab((x, 5, z), "smooth_stone_slab")
    p.put((1, 5, 5), NS + "air_raid_siren", {"sounding": "false"})
    p.put((5, 5, 5), "andesite_wall")
    # The door, a barred window, the sign beside the door and sandbags piled in front.
    p.door((3, 1, 1), "iron_door", "south", open_=True)
    p.put((1, 2, 1), "iron_bars")
    p.sign((5, 2, 0), "north", "shelter")
    for x in (0, 1, 6):
        p.put((x, 1, 0), "packed_mud")
    p.slab((0, 2, 0), "mud_brick_slab")
    p.put((5, 1, 0), "mud_brick_slab", {"type": "bottom", "waterlogged": "false"})
    # The stairwell: open floor with a railing on three sides; the stairs start at its north edge.
    p.air_box((2, 0, 2), (4, 0, 4))
    for z in range(2, 6):
        p.put((1, 1, z), "iron_bars")
        p.put((5, 1, z), "iron_bars")
    for x in range(2, 5):
        p.put((x, 1, 5), "iron_bars")
    p.lantern((3, 3, 3))
    p.put((1, 3, 2), "cobweb")
    p.jigsaw((3, 0, 3), "down_north", "minecraft:empty", STAIRS, pool("stairs"), "air")
    p.write()


def stair_corridor():
    """5 wide, 8 deep (y), 11 long: steps from the blockhouse floor down to a landing and the first room's door."""
    p = Piece(S + "/stair_corridor", (5, 8, 11))
    p.fill((0, 0, 0), (4, 7, 10), WALL)
    # Step k sits at z 1 + k, its top at y 7 - k; head room three blocks above it, up into the blockhouse.
    for k in range(7):
        z, y = 1 + k, 7 - k
        for x in range(1, 4):
            p.stairs((x, y, z), "stone_brick_stairs", "north")
            for yy in range(y + 1, min(8, y + 4)):
                p.put((x, yy, z), "air")
    for z in (8, 9):
        for x in range(1, 4):
            p.put((x, 0, z), FLOOR)
            for y in range(1, 4):
                p.put((x, y, z), "air")
    for z in range(1, 10):
        p.put((0, 0, z), FLOOR)
        p.put((4, 0, z), FLOOR)
    # A painted stripe along the landing's walls, and a lamp over it.
    for z in (8, 9):
        p.put((0, 1, z), STRIPE)
        p.put((4, 1, z), STRIPE)
    p.lantern((2, 3, 9))
    p.put((1, 1, 9), "cobweb")
    # The landing's door into the first room.
    p.air_box((2, 1, 10), (2, 2, 10))
    p.jigsaw((2, 7, 2), "up_north", STAIRS, "minecraft:empty", "minecraft:empty", "air")
    p.exit((2, 0, 10), "south", ENTRY, pool("rooms"), FLOOR)
    p.write()


def room_shell(p, door_x):
    """Concrete room filling the piece: floor at y 0, ceiling at the top, a door on the north wall at door_x."""
    sx, sy, sz = p.size
    p.fill((0, 0, 0), (sx - 1, 0, sz - 1), FLOOR)
    p.fill((0, sy - 1, 0), (sx - 1, sy - 1, sz - 1), CEILING)
    for y in range(1, sy - 1):
        p.walls((0, y, 0), (sx - 1, y, sz - 1), WALL)
    p.air_box((1, 1, 1), (sx - 2, sy - 2, sz - 2))
    p.air_box((door_x, 1, 0), (door_x, 2, 0))
    p.back((door_x, 0, 0), "north", ENTRY, FLOOR)


def dormitory():
    p = Piece(S + "/dormitory", (9, 5, 9))
    room_shell(p, 4)
    # Bunks along both walls: a bed on the floor and one stacked on it.
    for z in (2, 4, 6):
        for y in (1, 2):
            p.bed((2, y, z), "west", "red" if y == 1 else "white")
            p.bed((6, y, z), "east", "red" if y == 1 else "white")
    p.put((4, 1, 4), "spruce_fence")
    p.put((4, 2, 4), "spruce_pressure_plate", {"powered": "false"})
    p.put((4, 1, 5), "spruce_fence")
    p.put((4, 2, 5), "spruce_pressure_plate", {"powered": "false"})
    p.chest((2, 1, 7), "east", S + "/dormitory")
    p.lantern((4, 3, 3))
    p.put((1, 1, 7), "cobweb")
    p.put((7, 3, 1), "cobweb")
    p.put((3, 1, 3), "white_carpet")
    p.air_box((4, 1, 8), (4, 2, 8))
    p.exit((4, 0, 8), "south", ENTRY, pool("rooms"), FLOOR)
    p.write()


def supplies():
    p = Piece(S + "/supplies", (9, 5, 9))
    room_shell(p, 4)
    # Shelving along the walls: slabs on fence posts, barrels and crates of rations.
    for z in range(2, 8):
        p.put((1, 1, z), "barrel", {"facing": "east", "open": "false"})
        p.slab((1, 2, z), "spruce_slab", "top")
        p.put((7, 2, z), "spruce_slab", {"type": "top", "waterlogged": "false"})
        p.put((7, 1, z), "spruce_fence" if z % 2 == 0 else "air")
    p.barrel((1, 3, 3), "up", S + "/supplies")
    p.chest((7, 1, 3), "west", S + "/supplies")
    p.put((5, 1, 7), "water_cauldron", {"level": "3"})
    p.put((6, 1, 7), "cauldron")
    p.put((3, 1, 6), "create:andesite_casing")
    p.put((3, 2, 6), "create:andesite_casing")
    p.put((5, 1, 5), "create:andesite_casing")
    p.lantern((4, 3, 4))
    p.put((6, 3, 7), "cobweb")
    p.put((2, 1, 2), "cobweb")
    p.air_box((4, 1, 8), (4, 2, 8))
    p.exit((4, 0, 8), "south", ENTRY, pool("rooms"), FLOOR)
    p.write()


def vent_room():
    """The ventilation room: a hand-cranked filter housing, its ducts and the shelter's vents."""
    p = Piece(S + "/vent_room", (7, 5, 7))
    room_shell(p, 3)
    for x in range(1, 6):
        p.put((x, 3, 5), "create:andesite_casing")
    p.put((1, 3, 4), "create:andesite_casing")
    for y in (1, 2):
        for x in (2, 3, 4):
            p.put((x, y, 5), "create:copper_casing" if x == 3 else "iron_block")
    p.put((3, 4, 5), NS + "filtered_vent", {"open": "false"})
    p.put((1, 4, 2), NS + "filtered_vent", {"open": "false"})
    p.put((5, 1, 2), "create:andesite_casing")
    p.trapdoor((5, 2, 2), "iron_trapdoor", "north")
    p.put((1, 1, 1), "cobweb")
    p.lantern((3, 3, 2))
    p.write()


def end_wall():
    """Where a door leads nowhere: rubble packed into the doorway from behind."""
    p = Piece(S + "/end_wall", (3, 4, 1))
    for x in range(3):
        for y in range(4):
            p.put((x, y, 0), p.rng.choice(["cobblestone", "gravel", "andesite", "white_concrete"]))
    p.back((1, 0, 0), "north", ENTRY, "cobblestone")
    p.write()


def main():
    entrance()
    stair_corridor()
    dormitory()
    supplies()
    vent_room()
    end_wall()


if __name__ == "__main__":
    main()

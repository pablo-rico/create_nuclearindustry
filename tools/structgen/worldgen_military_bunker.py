#!/usr/bin/env python3
"""Writes the pieces of the abandoned military bunker (structure create_nuclearindustry:military_bunker).

    python3 tools/structgen/worldgen_military_bunker.py

A sandbagged guard house on the surface, with a hand-cranked siren on its roof, covers a ladder shaft
that drops one or more twelve-block segments to the bottom hall. From there corridors (straight, cross,
T, corner and a section with a jammed blast door, its controller long gone) branch out to the rooms:
command post, dormitory, armory, filtration plant, waste store and laboratory. Corridors and doors are
three blocks wide and three tall; a corridor that runs out of depth or room ends in a collapse.
"""
from worldgen_common import NS, Piece

S = "military_bunker"
ENTRY = NS + "bunker_entry"
SHAFT = NS + "bunker_shaft"

FLOOR = "deepslate_tiles"
WALL = "polished_deepslate"
CEILING = "deepslate_tiles"
TRIM = "gray_concrete"
STRIPE = "yellow_concrete"
LAMP = "redstone_lamp"


def pool(name):
    return NS + "ruins/" + S + "/" + name


def pipe(piece, pos, *open_sides):
    props = {d: ("true" if d in open_sides else "false") for d in ("north", "south", "east", "west", "up", "down")}
    props["waterlogged"] = "false"
    piece.put(pos, "create:fluid_pipe", props)


def lamp(p, pos):
    p.put(pos, LAMP, {"lit": "false"})


# ---------------------------------------------------------------------- surface and shaft


def guard_house():
    """9x7x9: a concrete post inside a ring of sandbags; the shaft hatch in its floor (x 3..5, z 3..5)."""
    p = Piece(S + "/guard_house", (9, 7, 9))
    p.fill((0, 1, 0), (8, 6, 8), "air")
    p.fill((0, 0, 0), (8, 0, 8), "gray_concrete")
    # Sandbags round the post, gap at the door.
    for x in range(9):
        for z in range(9):
            if (x in (0, 8) or z in (0, 8)) and not (z == 0 and 3 <= x <= 5):
                p.put((x, 1, z), "packed_mud")
                if p.chance(0.55):
                    p.put((x, 2, z), "mud_brick_slab", {"type": "bottom", "waterlogged": "false"})
    for y in range(1, 4):
        p.walls((1, y, 1), (7, y, 7), TRIM)
    for x in range(1, 8):
        p.put((x, 2, 1), "iron_bars" if x in (2, 6) else TRIM)
    for z in range(2, 7):
        p.put((1, 2, z), "iron_bars" if z == 4 else TRIM)
        p.put((7, 2, z), "iron_bars" if z == 4 else TRIM)
    p.fill((1, 4, 1), (7, 4, 7), "smooth_stone")
    for x in range(1, 8):
        for z in range(1, 8):
            if x in (1, 7) or z in (1, 7):
                p.put((x, 5, z), "stone_brick_wall")
    p.put((6, 5, 6), NS + "hand_crank_siren", {"sounding": "false"})
    p.put((2, 5, 6), "iron_bars")
    p.put((2, 6, 6), NS + "air_raid_siren", {"sounding": "false"})
    p.door((4, 1, 1), "iron_door", "south", open_=True)
    p.sign((5, 3, 0), "north", "restricted")
    # The hatch: an open square with a ladder on its north edge, railings round the other three sides.
    p.air_box((3, 0, 3), (5, 0, 5))
    p.ladder((4, 0, 3), "south")
    for z in range(3, 7):
        p.put((2, 1, z), "iron_bars")
        p.put((6, 1, z), "iron_bars")
    for x in range(3, 6):
        p.put((x, 1, 6), "iron_bars")
    p.chest((2, 1, 2), "south", S + "/guard_house")
    p.slab((6, 1, 2), "spruce_slab", "top")
    p.put((6, 2, 2), "lantern", {"hanging": "false", "waterlogged": "false"})
    p.lantern((4, 3, 4))
    p.jigsaw((4, 0, 4), "down_north", "minecraft:empty", SHAFT, pool("shaft"), "air")
    p.write()


def shaft_segment():
    """5x12x5 ladder shaft, open top and bottom; the ladder runs down the north wall."""
    height = 12
    p = Piece(S + "/shaft_segment", (5, height, 5))
    for y in range(height):
        p.walls((0, y, 0), (4, y, 4), TRIM if y % 4 == 0 else WALL)
        p.air_box((1, y, 1), (3, y, 3))
        p.ladder((2, y, 1), "south")
        p.chain((3, y, 3))
    lamp(p, (4, 2, 2))
    lamp(p, (0, 6, 2))
    lamp(p, (4, 10, 2))
    p.jigsaw((2, height - 1, 2), "up_north", SHAFT, "minecraft:empty", "minecraft:empty", "air")
    p.jigsaw((2, 0, 2), "down_north", "minecraft:empty", SHAFT, pool("shaft_lower"), "air")
    p.write()


def shaft_bottom():
    """9x6x9 hall at the foot of the shaft; doors east, west and south lead to the corridors."""
    p = Piece(S + "/shaft_bottom", (9, 6, 9))
    p.fill((0, 0, 0), (8, 0, 8), FLOOR)
    p.fill((0, 5, 0), (8, 5, 8), CEILING)
    for y in range(1, 5):
        p.walls((0, y, 0), (8, y, 8), WALL)
    p.air_box((1, 1, 1), (7, 4, 7))
    # Ceiling hatch and the ladder down a column to the floor.
    p.air_box((3, 5, 3), (5, 5, 5))
    for y in range(1, 6):
        p.ladder((4, y, 3), "south")
    for y in range(1, 5):
        p.put((4, y, 2), WALL)
    for x in range(3, 6):
        for z in range(3, 6):
            if (x, z) != (4, 3):
                p.put((x, 0, z), STRIPE if (x + z) % 2 == 0 else "black_concrete")
    # Lockers and a duty desk.
    for x in (1, 2):
        p.put((x, 1, 1), "iron_block")
        p.put((x, 2, 1), "iron_block")
    p.trapdoor((1, 3, 1), "iron_trapdoor", "south", half="top")
    p.put((7, 1, 7), "create:andesite_casing")
    p.put((7, 1, 6), "spruce_slab", {"type": "top", "waterlogged": "false"})
    lamp(p, (2, 5, 2))
    lamp(p, (6, 5, 6))
    p.put((1, 4, 7), "cobweb")
    for side, pos, door in (("east", (8, 0, 4), ((8, 1, 3), (8, 3, 5))), ("west", (0, 0, 4), ((0, 1, 3), (0, 3, 5))),
                            ("south", (4, 0, 8), ((3, 1, 8), (5, 3, 8)))):
        p.air_box(*door)
        p.exit(pos, side, ENTRY, pool("corridors"), FLOOR)
    p.jigsaw((4, 5, 4), "up_north", SHAFT, "minecraft:empty", "minecraft:empty", "air")
    p.write()


# ---------------------------------------------------------------------- corridors


def corridor(p, length, open_ends=("north", "south")):
    """A 5-wide run along z: floor, walls, ceiling, lamps every four blocks, a pipe under the ceiling."""
    for z in range(length):
        for x in range(5):
            p.put((x, 0, z), STRIPE if x == 2 and z % 4 == 1 else FLOOR)
            p.put((x, 4, z), CEILING)
        for y in range(1, 4):
            p.put((0, y, z), WALL)
            p.put((4, y, z), WALL)
            for x in range(1, 4):
                p.put((x, y, z), "air")
        pipe(p, (1, 3, z), "north", "south")
        if z % 4 == 2:
            lamp(p, (2, 4, z))


def corridor_straight():
    p = Piece(S + "/corridor_straight", (5, 5, 9))
    corridor(p, 9)
    # A side door on the east wall, to a room.
    p.air_box((4, 1, 3), (4, 3, 5))
    if p.chance(0.5):
        p.put((3, 1, 7), "cobweb")
    p.rubble((1, 1, 0), (3, 1, 8), 0.06, names=("gravel", "cobbled_deepslate"))
    p.back((2, 0, 0), "north", ENTRY, FLOOR)
    p.exit((2, 0, 8), "south", ENTRY, pool("corridors"), FLOOR)
    p.exit((4, 0, 4), "east", ENTRY, pool("rooms"), FLOOR)
    p.write()


def junction(rel, exits):
    """A 5x5x5 junction open to the north and towards each of exits (side -> pool)."""
    p = Piece(S + "/" + rel, (5, 5, 5))
    corridor(p, 5)
    sides = {"south": ((1, 1, 4), (3, 3, 4), (2, 0, 4)), "east": ((4, 1, 1), (4, 3, 3), (4, 0, 2)),
             "west": ((0, 1, 1), (0, 3, 3), (0, 0, 2))}
    # Close the south end unless it is an exit; open the sides that are.
    if "south" not in exits:
        for x in range(1, 4):
            for y in range(1, 4):
                p.put((x, y, 4), WALL)
            p.put((x, 0, 4), FLOOR)
    for side, target in exits.items():
        a, b, jig = sides[side]
        p.air_box(a, b)
        p.exit(jig, side, ENTRY, pool(target), FLOOR)
    if p.chance(0.4):
        p.put((3, 3, 2), "cobweb")
    p.back((2, 0, 0), "north", ENTRY, FLOOR)
    p.write()


def corridor_blast_door():
    """7x6x7: a vestibule with a blast door frame across it; one leaf stuck shut, the rest open into the walls."""
    p = Piece(S + "/corridor_blast_door", (7, 6, 7))
    p.fill((0, 0, 0), (6, 0, 6), FLOOR)
    p.fill((0, 5, 0), (6, 5, 6), CEILING)
    for y in range(1, 5):
        p.walls((0, y, 0), (6, y, 6), WALL)
    p.air_box((1, 1, 1), (5, 4, 5))
    p.air_box((2, 1, 0), (4, 3, 0))
    p.air_box((2, 1, 6), (4, 3, 6))
    # The frame fills the vestibule's section at z 3 round a 3x3 doorway.
    for x in range(1, 6):
        for y in range(0, 5):
            if not (2 <= x <= 4 and 1 <= y <= 3):
                p.put((x, y, 3), NS + "blast_door_frame")
    for y in range(1, 4):
        p.put((2, y, 3), NS + "steel_blast_door", {"leaf": "closed"})
    for x in range(1, 6):
        p.put((x, 0, 2), STRIPE if x % 2 else "black_concrete")
        p.put((x, 0, 4), STRIPE if x % 2 else "black_concrete")
    lamp(p, (3, 5, 1))
    lamp(p, (3, 5, 5))
    p.sign((3, 4, 2), "north", "blast_door")
    p.put((5, 3, 1), NS + "hand_crank_siren", {"sounding": "false"})
    p.put((1, 1, 5), "cobweb")
    p.rubble((1, 1, 4), (5, 1, 5), 0.2, names=("gravel", "cobbled_deepslate"))
    p.back((3, 0, 0), "north", ENTRY, FLOOR)
    p.exit((3, 0, 6), "south", ENTRY, pool("corridors"), FLOOR)
    p.write()


# ---------------------------------------------------------------------- rooms


def room_shell(rel, size):
    """A room filling the piece, with its 3-wide door in the middle of the north wall."""
    p = Piece(S + "/" + rel, size)
    sx, sy, sz = size
    p.fill((0, 0, 0), (sx - 1, 0, sz - 1), FLOOR)
    p.fill((0, sy - 1, 0), (sx - 1, sy - 1, sz - 1), CEILING)
    for y in range(1, sy - 1):
        p.walls((0, y, 0), (sx - 1, y, sz - 1), WALL)
    p.air_box((1, 1, 1), (sx - 2, sy - 2, sz - 2))
    c = sx // 2
    p.air_box((c - 1, 1, 0), (c + 1, 3, 0))
    p.back((c, 0, 0), "north", ENTRY, FLOOR)
    return p


def command():
    p = room_shell("command", (11, 6, 11))
    # The situation wall to the south: annunciators, screens, panels; desks facing it.
    p.annunciator_board([(x, 4, 9) for x in range(2, 9)], "north")
    for x in range(3, 8):
        for y in (2, 3):
            p.facing((x, y, 9), NS + "control_monitor", "north")
    p.flat_panel_row("wall_control_panel", [(x, 1, 9) for x in range(1, 10)], "north")
    p.flat_panel_row("wall_control_panel", [(x, 2, 9) for x in (1, 2)], "north")
    p.flat_panel_row("wall_control_panel", [(x, 2, 9) for x in (8, 9)], "north")
    p.desk_row(3, 7, 1, 6, "north")
    for x in (3, 5, 7):
        p.put((x, 1, 5), "create:black_seat")
    # The map table in the middle of the room.
    for x in (4, 5, 6):
        p.put((x, 1, 3), "cartography_table")
    p.put((1, 1, 1), NS + "seismic_detector")
    p.put((9, 1, 1), "bookshelf")
    p.put((9, 2, 1), "bookshelf")
    p.chest((9, 1, 3), "west", S + "/command")
    p.put((1, 4, 8), NS + "nuclear_alarm", {"lit": "false"})
    lamp(p, (5, 5, 5))
    lamp(p, (2, 5, 2))
    p.put((8, 4, 2), "cobweb")
    p.air_box((10, 1, 4), (10, 3, 6))
    p.exit((10, 0, 5), "east", ENTRY, pool("corridors"), FLOOR)
    p.write()


def dormitory():
    p = room_shell("dormitory", (9, 5, 11))
    for z in (2, 4, 6, 8):
        for y in (1, 2):
            p.bed((2, y, z), "west", "green" if y == 1 else "lime")
            p.bed((6, y, z), "east", "green" if y == 1 else "lime")
    for z in (3, 5, 7):
        p.put((1, 1, z), "iron_block")
        p.trapdoor((1, 2, z), "iron_trapdoor", "east", half="top")
    p.chest((7, 1, 9), "west", S + "/dormitory")
    p.put((4, 1, 9), "crafting_table")
    lamp(p, (4, 4, 5))
    p.put((7, 3, 3), "cobweb")
    p.put((4, 1, 6), "gray_carpet")
    p.write()


def armory():
    p = room_shell("armory", (7, 5, 9))
    for z in range(2, 8):
        p.put((1, 1, z), "create:andesite_casing" if z % 2 else "iron_block")
        p.slab((1, 2, z), "smooth_stone_slab", "bottom")
        p.put((5, 1, z), "barrel", {"facing": "west", "open": "false"} if z % 3 else {"facing": "up", "open": "false"})
    p.put((5, 2, 3), "create:andesite_casing")
    p.put((5, 2, 6), "create:andesite_casing")
    p.chest((3, 1, 7), "north", S + "/armory")
    p.put((3, 1, 5), "anvil", {"facing": "east"})
    p.put((2, 1, 7), "grindstone", {"face": "floor", "facing": "north"})
    p.put((5, 3, 7), "cobweb")
    lamp(p, (3, 4, 4))
    p.write()


def filtration():
    """The bunker's air plant: filter housings and ducts. The filtration unit itself was taken out long ago."""
    p = room_shell("filtration", (9, 6, 9))
    for x in range(2, 7):
        for y in range(1, 4):
            for z in (5, 6):
                housing = x in (2, 6) or y == 3
                if housing or (x == 4 and y == 2):
                    p.put((x, y, z), "create:copper_casing" if (x + y) % 2 else "iron_block")
    p.put((4, 1, 5), "air")
    p.put((4, 1, 6), "create:andesite_casing")
    for x in range(1, 8):
        p.put((x, 4, 7), "create:andesite_casing")
    for z in range(2, 7):
        p.put((7, 4, z), "create:andesite_casing")
    p.put((2, 5, 2), NS + "filtered_vent", {"open": "false"})
    p.put((6, 5, 2), NS + "filtered_vent", {"open": "false"})
    for z in range(1, 5):
        pipe(p, (1, 1, z), "north", "south")
    p.put((1, 1, 5), "create:weathered_iron_block")
    p.put((7, 1, 1), "cobweb")
    lamp(p, (4, 5, 3))
    p.write()


def waste_store():
    """Drums of the bunker's waste, some leaking; vitrified canisters on plinths behind a lead door."""
    p = room_shell("waste_store", (9, 5, 11))
    for z in range(1, 10):
        p.put((1, 0, z), STRIPE if z % 2 else "black_concrete")
        p.put((7, 0, z), STRIPE if z % 2 else "black_concrete")
    for z in range(3, 9):
        p.put((1, 1, z), NS + "cemented_waste_drum")
        if z % 2:
            p.put((1, 2, z), NS + "cemented_waste_drum")
    for pos in ((6, 1, 6), (7, 1, 6), (7, 1, 7), (6, 1, 8), (7, 1, 8), (7, 2, 7)):
        p.put(pos, NS + "radioactive_waste_barrel")
    for pos in ((5, 1, 7), (6, 1, 7), (5, 1, 8), (6, 1, 9)):
        p.put(pos, "lime_concrete_powder")
    p.put((3, 1, 9), "smooth_stone")
    p.put((5, 1, 9), "smooth_stone")
    p.put((3, 2, 9), NS + "vitrified_waste_canister")
    p.put((5, 2, 9), NS + "vitrified_waste_canister")
    # A lead blast door leaf jammed in the doorway.
    p.put((3, 1, 0), NS + "lead_blast_door", {"leaf": "closed"})
    p.put((3, 2, 0), NS + "lead_blast_door", {"leaf": "closed"})
    p.chest((7, 1, 2), "west", S + "/waste_store")
    p.sign((6, 3, 1), "south", "waste")
    lamp(p, (4, 4, 5))
    p.write()


def lab():
    p = room_shell("lab", (9, 5, 9))
    for x in range(1, 8):
        p.slab((x, 1, 7), "smooth_stone_slab", "top")
    for z in range(2, 7):
        p.slab((7, 1, z), "smooth_stone_slab", "top")
    p.put((2, 2, 7), "brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "false"})
    p.put((5, 2, 7), "brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "true", "has_bottle_2": "true"})
    p.put((7, 2, 4), "flower_pot")
    for z in (2, 3, 5):
        p.put((7, 3, z), "glass_pane")
    p.put((1, 1, 2), "cauldron")
    p.put((1, 1, 3), "glass")
    p.put((1, 2, 3), "glass_pane")
    p.chest((4, 1, 6), "north", S + "/lab")
    p.put((4, 3, 4), NS + "smoke_detector", {"face": "ceiling", "facing": "north", "power": "0"})
    p.put((2, 3, 5), "cobweb")
    lamp(p, (4, 4, 2))
    p.write()


def collapsed_end():
    """A corridor or door choked by a cave-in."""
    p = Piece(S + "/collapsed_end", (5, 5, 2))
    for x in range(5):
        for y in range(5):
            for z in range(2):
                if z == 1 or p.chance(0.85):
                    p.put((x, y, z), p.rng.choice(["cobbled_deepslate", "gravel", "tuff", "cobbled_deepslate"]))
                else:
                    p.put((x, y, z), "air")
    p.back((2, 0, 0), "north", ENTRY, "cobbled_deepslate")
    p.write()


def main():
    guard_house()
    shaft_segment()
    shaft_bottom()
    corridor_straight()
    junction("corridor_cross", {"south": "corridors", "east": "corridors", "west": "rooms"})
    junction("corridor_tee", {"east": "corridors", "west": "corridors"})
    junction("corridor_corner", {"east": "corridors"})
    corridor_blast_door()
    command()
    dormitory()
    armory()
    filtration()
    waste_store()
    lab()
    collapsed_end()


if __name__ == "__main__":
    main()

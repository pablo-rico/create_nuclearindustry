#!/usr/bin/env python3
"""Writes the pieces of the abandoned nuclear power plant (structure create_nuclearindustry:abandoned_power_plant).

    python3 tools/structgen/worldgen_plant.py

The start piece is the reactor hall: a containment building whose dome has fallen in, with a partial
reactor shell, two steam generators and the polar crane's ring inside. Out of its four sides grow the
turbine hall (east), the control building (west), a waste yard or a cooling tower stump (south) and the
access road (north); the turbine hall and the outbuildings lead on to more road. Every piece is rigid and
sits on the ground (y 0 replaces the grass); the roads follow the terrain (processor road_gravity).
Nothing in here forms a machine: there are casings and rods, but no controller.
"""
import math

from worldgen_common import NS, Piece

S = "abandoned_power_plant"
ENTRY = NS + "plant_entry"


def pool(name):
    return NS + "ruins/" + S + "/" + name


CONCRETE = NS + "containment_concrete"
LINER = NS + "steel_liner"
APRON = "gray_concrete"
WALL = "light_gray_concrete"
TRIM = "gray_concrete"
IRON = "create:industrial_iron_block"
RUST = "create:weathered_iron_block"


def pipe(piece, pos, *open_sides, name="create:fluid_pipe"):
    props = {d: ("true" if d in open_sides else "false") for d in ("north", "south", "east", "west", "up", "down")}
    props["waterlogged"] = "false"
    piece.put(pos, name, props)


def girder(piece, pos, axis):
    piece.put(pos, "create:metal_girder", {"x": "true" if axis == "x" else "false", "z": "true" if axis == "z" else "false",
                                           "top": "false", "bottom": "false", "axis": "y" if axis == "y" else axis,
                                           "waterlogged": "false"})


# ---------------------------------------------------------------------- reactor hall


def reactor_hall():
    p = Piece(S + "/reactor_hall", (23, 16, 23))
    cx = cz = 11
    p.fill((0, 1, 0), (22, 15, 22), "air")
    # The apron round the building, the polished floor inside.
    for x in range(23):
        for z in range(23):
            d = math.hypot(x - cx, z - cz)
            p.put((x, 0, z), "polished_andesite" if d < 8.6 else APRON)
    # Where the dome came down: the low side of the broken ring faces south-east.
    fall = math.radians(135)

    def wall_top(angle):
        """The containment wall stands to y 9, broken down to y 4..6 round the collapse."""
        drop = max(0.0, math.cos(angle - fall))
        return 9 - int(round(drop ** 2 * 5)) - (1 if p.chance(0.25) else 0)

    for x in range(23):
        for z in range(23):
            d = math.hypot(x - cx, z - cz)
            angle = math.atan2(z - cz, x - cx)
            if 8.6 <= d < 10.1:
                top = wall_top(angle)
                for y in range(1, top + 1):
                    p.put((x, y, z), CONCRETE)
            elif 7.9 <= d < 8.6:
                for y in range(1, 8):
                    p.put((x, y, z), LINER)
    # What is left of the dome: a ring springing from the wall, highest on the side away from the collapse.
    R, H = 9.6, 6.4
    for x in range(23):
        for z in range(23):
            angle = math.atan2(z - cz, x - cx)
            keep = 10 + int(round(4.5 * max(0.0, -math.cos(angle - fall))))
            for y in range(10, 16):
                r = math.sqrt(((x - cx) / R) ** 2 + ((z - cz) / R) ** 2 + ((y - 9) / H) ** 2)
                if 0.86 <= r <= 1.0 and y <= keep and not p.chance(0.08):
                    p.put((x, y, z), CONCRETE)
    # The polar crane's rail: a steel ring on corbels, with its bridge fallen across the floor.
    for x in range(23):
        for z in range(23):
            d = math.hypot(x - cx, z - cz)
            if 7.2 <= d < 7.9:
                angle = math.atan2(z - cz, x - cx)
                if math.cos(angle - fall) < 0.55:
                    p.put((x, 8, z), IRON)
    for x in range(4, 10):
        girder(p, (x, 8, 11), "x")
    for x, y in ((12, 1), (13, 1), (14, 2), (15, 2), (16, 1)):
        girder(p, (x, y, 16), "x")

    # The reactor: a casing shell with its top and south-west corner torn open, rods and moderator showing.
    for x in range(9, 14):
        for y in range(1, 6):
            for z in range(9, 14):
                shell = x in (9, 13) or y in (1, 5) or z in (9, 13)
                torn = (y == 5 and (x < 12 or z > 10)) or (x <= 10 and z >= 12 and y >= 3)
                if shell and not torn:
                    p.put((x, y, z), NS + "reactor_casing")
                elif not shell:
                    if (x, z) in ((10, 10), (12, 12), (12, 10), (10, 12)):
                        if y <= (4 if (x, z) != (10, 12) else 2):
                            p.put((x, y, z), NS + "control_rod")
                    elif y <= 3:
                        p.put((x, y, z), NS + "graphite_moderator")
    p.put((11, 5, 11), NS + "beryllium_reflector")
    # Biological shield: a low concrete ring round the vessel, broken on the torn side.
    for x in range(7, 16):
        for z in range(7, 16):
            if x in (7, 15) or z in (7, 15):
                if not (x <= 9 and z >= 13) and not (x == 11 or z == 11):
                    for y in range(1, 3):
                        p.put((x, y, z), "gray_concrete")

    # Two steam generators, one standing, one with its head knocked off; and the pressurizer.
    def steam_generator(x0, z0, height):
        for x in range(x0, x0 + 3):
            for z in range(z0, z0 + 3):
                for y in range(1, height + 1):
                    centre = x == x0 + 1 and z == z0 + 1
                    p.put((x, y, z), NS + ("steam_generator_tube_bundle" if centre else "steam_generator_casing"))

    steam_generator(14, 3, 7)
    steam_generator(3, 14, 4)
    p.put((4, 5, 15), NS + "steam_generator_casing")
    for y in range(1, 6):
        p.put((15, y, 15), NS + "pressurizer")
    p.put((15, 6, 15), NS + "relief_valve", {"open": "true"})
    # Hot legs: pipes from the vessel to the generators, one of them snapped.
    for x in range(14, 15):
        pipe(p, (x, 3, 11), "east", "west")
    for z in range(6, 11):
        pipe(p, (15, 3, z), "north", "south")
    pipe(p, (15, 3, 11), "north", "west")
    for z in range(14, 16):
        pipe(p, (8, 2, z), "north", "south")

    # Openings: the equipment hatch to the turbine hall (east), personnel doors west and south, the
    # main access to the north. Each runs from the wall out to its jigsaw on the edge of the apron.
    for y in range(1, 5):
        for z in range(9, 14):
            for x in range(18, 22):
                p.put((x, y, z), "air")
    for y in range(1, 5):
        for z in (8, 14):
            for x in range(19, 21):
                p.put((x, y, z), LINER)
    for z in range(9, 14):
        p.put((19, 5, z), LINER)
    for x in range(0, 4):
        for y in range(1, 4):
            for z in range(10, 13):
                p.put((x, y, z), "air")
    p.put((2, 1, 10), NS + "containment_door", {"facing": "east", "half": "lower", "hinge": "left", "open": "true",
                                                 "visible": "true"})
    p.put((2, 2, 10), NS + "containment_door", {"facing": "east", "half": "upper", "hinge": "left", "open": "true",
                                                 "visible": "true"})
    for z in range(0, 4):
        for y in range(1, 4):
            for x in range(10, 13):
                p.put((x, y, z), "air")
    for z in range(19, 23):
        for y in range(1, 4):
            for x in range(10, 13):
                p.put((x, y, z), "air")

    # Rubble under the collapse, chunks of dome on the floor and outside.
    for _ in range(70):
        x = p.rng.randint(12, 20)
        z = p.rng.randint(12, 20)
        if math.hypot(x - cx, z - cz) < 8.2 and p.is_air((x, 1, z)):
            p.put((x, 1, z), p.rng.choice(["gravel", "cobblestone", CONCRETE, CONCRETE, "andesite"]))
            if p.chance(0.35) and p.is_air((x, 2, z)):
                p.put((x, 2, z), p.rng.choice(["gravel", CONCRETE]))
    for _ in range(26):
        x = p.rng.randint(15, 22)
        z = p.rng.randint(15, 22)
        if math.hypot(x - cx, z - cz) >= 10.1 and p.is_air((x, 1, z)):
            p.put((x, 1, z), p.rng.choice(["gravel", CONCRETE, "cobblestone"]))
    p.rubble((1, 1, 1), (21, 1, 21), 0.04)

    # Interim waste store in the north-west corner of the hall: leaking barrels and two cemented drums.
    for pos in ((6, 1, 7), (7, 1, 6), (6, 1, 6), (7, 1, 5), (5, 1, 7), (6, 2, 6)):
        p.put(pos, NS + "radioactive_waste_barrel")
    p.put((8, 1, 5), NS + "cemented_waste_drum")
    p.put((5, 1, 8), NS + "cemented_waste_drum")
    p.put((6, 1, 8), "lime_concrete_powder")
    p.put((8, 1, 6), "lime_concrete_powder")
    p.chest((16, 1, 9), "west", S + "/reactor_hall")
    p.sign((21, 2, 7), "east", "radiation")
    p.put((16, 3, 10), "cobweb")
    p.put((6, 3, 16), "cobweb")

    # Jigsaws on the edges of the apron.
    p.exit((22, 0, 11), "east", ENTRY, pool("turbine_hall"), APRON)
    p.exit((0, 0, 11), "west", ENTRY, pool("control_building"), APRON)
    p.exit((11, 0, 22), "south", ENTRY, pool("yard"), APRON)
    p.exit((11, 0, 0), "north", ENTRY, pool("roads"), APRON)
    p.write()


# ---------------------------------------------------------------------- turbine hall


def turbine_hall():
    """25 long (x) by 11 deep (z); attached by the middle of its north side, facing the reactor."""
    p = Piece(S + "/turbine_hall", (25, 12, 11))
    p.fill((0, 1, 0), (24, 11, 10), "air")
    p.fill((0, 0, 0), (24, 0, 10), "smooth_stone")
    for x in range(1, 24):
        p.put((x, 0, 5), TRIM)
    # Walls with pilasters every four blocks and a band of turbine glass under the eaves.
    for y in range(1, 9):
        for x in range(25):
            for z in (0, 10):
                pilaster = x % 4 == 0
                glass = 5 <= y <= 7 and not pilaster and 0 < x < 24
                p.put((x, y, z), TRIM if pilaster else (NS + "reinforced_turbine_glass" if glass else WALL))
        for z in range(1, 10):
            for x in (0, 24):
                p.put((x, y, z), TRIM if z in (1, 9) else WALL)
    # Roof, fallen in over the generator end.
    for x in range(25):
        for z in range(11):
            hole = 15 <= x <= 20 and 2 <= z <= 8 and not (x == 15 and p.chance(0.5))
            if not hole:
                p.put((x, 9, z), "gray_concrete" if z in (0, 10) or x in (0, 24) else WALL)
    for x in range(1, 24, 6):
        for y in (10, 11):
            p.put((x, y, 5), "andesite")
    p.put((1, 11, 5), "iron_bars")
    # Crane rails along the long walls and the crane bridge, its hook still hanging.
    for x in range(1, 24):
        for z in (1, 9):
            p.put((x, 7, z), IRON)
    for z in range(2, 9):
        girder(p, (13, 7, z), "z")
    p.chain((13, 6, 5))
    p.chain((13, 5, 5))
    p.put((13, 4, 5), "anvil", {"facing": "north"})

    # The turbine: a rotor on casing pedestals, its hood half gone; the generator at the east end.
    for x in range(4, 19):
        p.put((x, 3, 5), NS + "turbine_rotor", {"axis": "x", "formed": "false"})
        for z in (4, 5, 6):
            for y in (1, 2):
                if z == 5 or x % 3 == 1:
                    p.put((x, y, z), NS + "turbine_casing")
        if x <= 11:
            for z in (4, 6):
                p.put((x, 3, z), NS + "turbine_casing")
            if x <= 8:
                p.put((x, 4, 5), NS + "turbine_casing")
    for x in range(19, 22):
        for y in range(1, 5):
            for z in range(4, 7):
                p.put((x, y, z), "create:copper_casing" if y in (1, 4) or z != 5 else IRON)
    p.put((22, 1, 5), RUST)
    # Steam lines coming in through the north wall to the turbine's inlet.
    for z in range(1, 4):
        pipe(p, (6, 4, z), "north", "south")
        pipe(p, (12, 4, z), "north", "south")
    # Debris from the roof.
    for _ in range(45):
        x = p.rng.randint(14, 21)
        z = p.rng.randint(1, 9)
        if p.is_air((x, 1, z)):
            p.put((x, 1, z), p.rng.choice(["gravel", "cobblestone", WALL, "andesite"]))
        elif p.is_air((x, 2, z)) and p.chance(0.4):
            p.put((x, 2, z), p.rng.choice(["gravel", WALL]))
    for x, y in ((16, 1), (17, 1), (18, 2), (19, 2)):
        girder(p, (x, y, 7), "x")
    p.rubble((1, 1, 1), (23, 1, 9), 0.03)
    p.chest((22, 1, 8), "west", S + "/turbine_hall")
    p.put((2, 1, 9), NS + "radioactive_waste_barrel")

    # Doors: the big door facing the reactor, a door to the road (south) and one at the east end.
    p.air_box((11, 1, 0), (13, 4, 0))
    p.air_box((11, 1, 10), (13, 3, 10))
    p.air_box((24, 1, 4), (24, 3, 6))
    p.back((12, 0, 0), "north", ENTRY, "smooth_stone")
    p.exit((12, 0, 10), "south", ENTRY, pool("roads"), "smooth_stone")
    p.exit((24, 0, 5), "east", ENTRY, pool("yard"), "smooth_stone")
    p.write()


# ---------------------------------------------------------------------- control building


def control_building():
    """13 wide (x) by 11 deep (z), two storeys; the main control room on the ground floor."""
    p = Piece(S + "/control_building", (13, 8, 11))
    p.fill((0, 1, 0), (12, 7, 10), "air")
    p.fill((0, 0, 0), (12, 0, 10), "polished_andesite")
    for x in range(1, 12):
        for z in range(1, 10):
            if (x + z) % 2 == 0:
                p.put((x, 0, z), "light_gray_concrete")
    # Shell: concrete walls, a ribbon of windows on the ground floor, the upper storey's roof half gone.
    for y in range(1, 7):
        for x in range(13):
            for z in (0, 10):
                window = y in (2, 6) and x % 3 != 0 and z == 0
                p.put((x, y, z), "glass_pane" if window else (TRIM if x in (0, 12) else WALL))
        for z in range(1, 10):
            for x in (0, 12):
                window = y in (2, 6) and z % 3 != 0
                p.put((x, y, z), "glass_pane" if window else WALL)
    for x in range(1, 12):
        for z in range(1, 10):
            if not (x == 11 and z == 1):
                p.put((x, 4, z), "smooth_stone")
            gone = x >= 6 and z >= 3 + (x - 6) // 2
            if not gone:
                p.put((x, 7, z), "smooth_stone_slab", {"type": "bottom", "waterlogged": "false"})
    for x in range(13):
        p.put((x, 7, 0), TRIM)
        p.put((x, 7, 10), TRIM)

    # The back wall's instruments, facing north: annunciators over monitors over panels.
    p.annunciator_board([(x, 3, 9) for x in range(2, 11)], "north")
    for x in range(1, 12):
        if 4 <= x <= 8:
            p.facing((x, 2, 9), NS + "control_monitor", "north")
        elif x in (1, 11):
            p.facing((x, 1, 9), NS + "chart_recorder", "north")
    p.flat_panel_row("wall_control_panel", [(x, 1, 9) for x in range(2, 11)], "north")
    p.flat_panel_row("wall_control_panel", [(x, 2, 9) for x in (1, 2, 3)], "north")
    p.flat_panel_row("wall_control_panel", [(x, 2, 9) for x in (9, 10, 11)], "north")
    p.put((1, 3, 9), NS + "nuclear_alarm", {"lit": "false"})
    p.put((11, 3, 9), NS + "nuclear_alarm", {"lit": "false"})
    # The operators' desks and the supervisor's tables, with their seats.
    p.desk_row(3, 9, 1, 6, "north")
    for x in (4, 6, 8):
        p.put((x, 1, 5), "create:black_seat")
    p.flat_panel_row("control_table", [(x, 1, 3) for x in range(5, 8)], "north")
    p.put((6, 1, 2), "create:gray_seat")
    p.put((2, 1, 2), "cobweb")
    p.put((10, 3, 7), "cobweb")
    # Ladder to the upper floor, in the north-east corner.
    for y in range(1, 5):
        p.ladder((11, y, 1), "south")
    # Upstairs: offices, a filing wall, the chest; broken roof lets the weather in.
    for z in range(5, 10):
        p.put((1, 5, z), "bookshelf")
    p.put((3, 5, 8), "spruce_slab", {"type": "top", "waterlogged": "false"})
    p.put((4, 5, 8), "spruce_slab", {"type": "top", "waterlogged": "false"})
    p.put((3, 5, 7), "create:white_seat")
    p.chest((2, 5, 2), "south", S + "/control_building")
    p.barrel((1, 5, 2), "up")
    p.rubble((6, 5, 3), (11, 5, 9), 0.25)
    p.put((9, 5, 7), "dead_bush")
    p.put((9, 4, 7), "coarse_dirt")

    # Entrance from the reactor (north), a side door to the road (east).
    p.door((6, 1, 0), "iron_door", "south", open_=True)
    p.sign((5, 3, 1), "south", "control")
    p.air_box((12, 1, 4), (12, 2, 4))
    p.back((6, 0, 0), "north", ENTRY, "polished_andesite")
    p.exit((12, 0, 4), "east", ENTRY, pool("roads"), "polished_andesite")
    p.write()


# ---------------------------------------------------------------------- waste yard


def waste_yard():
    p = Piece(S + "/waste_yard", (13, 6, 13))
    p.fill((0, 1, 0), (12, 5, 12), "air")
    for x in range(13):
        for z in range(13):
            edge = x in (0, 12) or z in (0, 12)
            stripe = edge and (x + z) % 2 == 0
            p.put((x, 0, z), ("yellow_concrete" if stripe else "black_concrete") if edge else "gray_concrete")
    # Security fence with a gate gap facing the plant, floodlights at the corners.
    for x in range(13):
        for z in range(13):
            if (x in (0, 12) or z in (0, 12)) and not (z == 0 and 5 <= x <= 7) and not (x == 12 and 5 <= z <= 7):
                for y in (1, 2):
                    if not (y == 2 and p.chance(0.15)):
                        p.put((x, y, z), "iron_bars")
    for x, z in ((0, 0), (12, 0), (0, 12), (12, 12)):
        for y in (1, 2, 3):
            p.put((x, y, z), "andesite_wall")
        p.lantern((x, 4, z), hanging=False)
    # The store: rows of cemented drums, leaking barrels round a spill, vitrified canisters on a plinth.
    for x in (2, 3):
        for z in range(3, 8):
            p.put((x, 1, z), NS + "cemented_waste_drum" if (x + z) % 3 else NS + "compacted_waste_drum")
    for pos in ((6, 1, 8), (7, 1, 8), (6, 1, 9), (8, 1, 9), (7, 2, 8), (8, 1, 7)):
        p.put(pos, NS + "radioactive_waste_barrel")
    for pos in ((7, 1, 9), (7, 1, 10), (8, 1, 10), (9, 1, 9), (6, 1, 10)):
        p.put(pos, "lime_concrete_powder")
    p.put((9, 0, 4), "smooth_stone")
    p.put((10, 0, 4), "smooth_stone")
    p.put((9, 1, 4), NS + "vitrified_waste_canister")
    p.put((10, 1, 4), NS + "vitrified_waste_canister")
    # A small shed in the south-west corner holds the yard's chest.
    for x in range(1, 5):
        for z in range(9, 12):
            for y in range(1, 4):
                if x in (1, 4) or z in (9, 11) or y == 3:
                    p.put((x, y, z), "bricks" if y < 3 else "spruce_slab",
                          None if y < 3 else {"type": "bottom", "waterlogged": "false"})
    p.air_box((3, 1, 9), (3, 2, 9))
    p.chest((2, 1, 10), "north", S + "/waste_yard")
    p.sign((2, 2, 8), "north", "waste")
    p.put((5, 1, 3), "cobweb")
    p.back((6, 0, 0), "north", ENTRY, "gray_concrete")
    p.exit((12, 0, 6), "east", ENTRY, pool("roads"), "gray_concrete")
    p.write()


# ---------------------------------------------------------------------- cooling tower stump


def cooling_tower_stub():
    """A natural-draught tower broken off part way up: hyperboloid shell on V legs over its basin."""
    size, top_y = 19, 17
    p = Piece(S + "/cooling_tower_stub", (size, top_y, size))
    cx = cz = size // 2
    p.fill((0, 1, 0), (size - 1, top_y - 1, size - 1), "air")
    waist, waist_y, c = 5.6, 16.0, 12.5
    break_dir = math.radians(p.rng.uniform(0, 360))

    def radius(y):
        return waist * math.sqrt(1 + ((y - waist_y) / c) ** 2)

    # The basin: still water inside a low rim, an apron round it.
    for x in range(size):
        for z in range(size):
            d = math.hypot(x - cx, z - cz)
            p.put((x, 0, z), "water" if d < 6.9 else ("gray_concrete" if d < 9.4 else APRON))
            if 6.9 <= d < 7.8:
                p.put((x, 1, z), "smooth_stone")
    p.put((cx, 0, cz), "gray_concrete")
    for x in range(size):
        for z in range(size):
            d = math.hypot(x - cx, z - cz)
            angle = math.atan2(z - cz, x - cx)
            # Broken off at y 7 on one side, standing to the top of the box on the other.
            top = 7 + int(round(9 * (0.5 + 0.5 * math.cos(angle - break_dir)) ** 1.5)) + p.rng.randint(0, 1)
            for y in range(1, top_y):
                if abs(d - radius(y)) > 0.62 or y > top:
                    continue
                if y <= 3:
                    # The air intake: legs every thirty degrees, open between them.
                    if int((math.degrees(angle) + 367.5) // 15) % 2 == 0:
                        p.put((x, y, z), TRIM)
                elif y == 4:
                    p.put((x, y, z), TRIM)
                else:
                    streak = (int(math.degrees(angle) + 360) // 18) % 4 == 0
                    stain = y <= 6 and p.chance(0.35)
                    p.put((x, y, z), "light_gray_concrete" if stain else ("white_concrete" if streak or p.chance(0.15)
                                                                         else "white_concrete_powder" if p.chance(0.08)
                                                                         else WALL))
    # Fill packing over the basin, the hot water riser in the middle and its spray pipes.
    for x in range(size):
        for z in range(size):
            if math.hypot(x - cx, z - cz) < 6.2 and (x + z) % 2 == 0 and p.chance(0.55):
                p.put((x, 3, z), "dark_oak_fence")
    for y in range(1, 5):
        p.put((cx, y, cz), RUST)
    for dx in range(1, 5):
        pipe(p, (cx + dx, 4, cz), "east", "west")
        pipe(p, (cx - dx, 4, cz), "east", "west")
    # Fallen pieces of the shell inside and at its foot.
    for _ in range(30):
        x, z = p.rng.randint(1, size - 2), p.rng.randint(1, size - 2)
        d = math.hypot(x - cx, z - cz)
        y = 2 if 6.9 <= d < 7.8 else 1
        if p.is_air((x, y, z)) and (d < 6.0 or d > 9.0):
            p.put((x, y, z), p.rng.choice([WALL, "white_concrete", "gravel"]))
    p.back((cx, 0, 0), "north", ENTRY, APRON)
    p.write()


# ---------------------------------------------------------------------- roads


def road_surface(p, x0, x1, z0, z1, centre):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            line = x == centre and z % 3 != 2
            p.put((x, 0, z), "yellow_concrete" if line else "gray_concrete")
            for y in range(1, p.size[1]):
                p.put((x, y, z), "air")


def road_straight():
    p = Piece(S + "/road_straight", (5, 3, 9))
    road_surface(p, 0, 4, 0, 8, 2)
    p.decay(0.15, ["gray_concrete"], ["gravel", "coarse_dirt", "cobblestone"])
    p.put((0, 1, 4), "short_grass")
    p.back((2, 0, 0), "north", ENTRY, "gray_concrete")
    p.exit((2, 0, 8), "south", ENTRY, pool("roads"), "gray_concrete")
    p.write()


def road_checkpoint():
    """The plant's gate: a guard booth beside the road and its boom lying across it."""
    p = Piece(S + "/road_checkpoint", (9, 5, 9))
    road_surface(p, 2, 6, 0, 8, 4)
    for x in (0, 1, 7, 8):
        for z in range(9):
            p.put((x, 0, z), "smooth_stone" if x in (1, 7) else "coarse_dirt")
            for y in range(1, 5):
                p.put((x, y, z), "air")
    for z in range(3, 7):
        for y in range(1, 4):
            p.put((7, y, z), "glass_pane" if y == 2 and z in (4, 5) else "white_concrete")
            p.put((8, y, z), "white_concrete")
    p.fill((7, 4, 3), (8, 4, 6), "smooth_stone_slab", {"type": "bottom", "waterlogged": "false"})
    p.put((7, 1, 6), "air")
    p.put((7, 2, 6), "air")
    p.sign((7, 2, 2), "north", "checkpoint")
    for x in range(1, 7):
        p.put((x, 1, 4), "red_carpet" if x % 2 else "white_carpet")
    p.put((1, 1, 3), "andesite_wall")
    p.put((1, 2, 3), "andesite_wall")
    p.decay(0.12, ["gray_concrete"], ["gravel", "coarse_dirt"])
    p.back((4, 0, 0), "north", ENTRY, "gray_concrete")
    p.exit((4, 0, 8), "south", ENTRY, pool("roads"), "gray_concrete")
    p.write()


def road_end():
    """Where the road stops: concrete barriers across it and a broken fence behind."""
    p = Piece(S + "/road_end", (5, 3, 4))
    road_surface(p, 0, 4, 0, 1, 2)
    for x in range(5):
        p.put((x, 0, 2), "gravel" if x % 2 else "coarse_dirt")
        p.put((x, 0, 3), "coarse_dirt")
        p.put((x, 1, 2), "smooth_stone" if x != 3 else "air")
        p.put((x, 2, 2), "smooth_stone_slab", {"type": "bottom", "waterlogged": "false"} if x != 3 else None)
        p.put((x, 1, 3), "spruce_fence" if x % 3 != 1 else "air")
        p.put((x, 2, 3), "air")
    p.put((3, 2, 2), "air")
    p.back((2, 0, 0), "north", ENTRY, "gray_concrete")
    p.write()


def main():
    reactor_hall()
    turbine_hall()
    control_building()
    waste_yard()
    cooling_tower_stub()
    road_straight()
    road_checkpoint()
    road_end()


if __name__ == "__main__":
    main()

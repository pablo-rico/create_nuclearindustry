"""Building blocks shared by the worldgen_*.py scripts (the abandoned nuclear sites).

Each piece is a Piece: a box of blocks written to
src/main/resources/data/create_nuclearindustry/structure/ruins/<rel>.nbt, where the structures' jigsaw
pools (data/RuinsWorldgen.java) pick them up. Conventions:

- x runs east, y up, z south. y 0 is the floor (or the road surface); for a surface piece it sits one
  block below the first free block of the ground, replacing the grass.
- Positions left out keep whatever the world has there; explicit air ("air") empties them, so rooms
  dug into the ground carve their own space.
- Horizontal jigsaws sit in the floor layer of a doorway, on the face of the box, facing out; their
  final_state is the floor block. A piece is attached by its "back" jigsaw, whose name the parent's
  jigsaw targets; the back jigsaw's own pool is minecraft:empty.
- Vertical jigsaws (shafts, stairwells) use aligned joints with "north" as their top, so a piece hung
  below another keeps the same rotation and its ladder stays on the same wall.
- Randomness comes from random.Random(crc32(rel)): running the scripts again writes the same bytes.
- No ruin may hold a multiblock controller or a bomb (FORBIDDEN), nor more than MAX_LEAKING_BARRELS
  leaking waste barrels: write() refuses such a piece.
"""
import json
import pathlib
import random
import zlib

import nbtwriter

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/data/create_nuclearindustry/structure/ruins"
NS = "create_nuclearindustry:"
EMPTY = "minecraft:empty"

# Every block whose block entity owns a multiblock, and the bombs (RuinsDataGen.FORBIDDEN, tag ruin_forbidden).
FORBIDDEN = {NS + name for name in (
    "reactor_controller", "cooling_tower_controller", "turbine_output", "fusion_controller", "fusion_plasma_turbine",
    "cryoplant_controller", "steam_generator_controller", "containment_monitor", "particle_accelerator_controller",
    "shelter_filtration_unit", "blast_door_controller", "nuclear_bomb", "dirty_bomb", "thermonuclear_bomb")}
BARREL = NS + "radioactive_waste_barrel"
MAX_LEAKING_BARRELS = 6
WASTE_PACKAGES = {BARREL, NS + "cemented_waste_drum", NS + "compacted_waste_drum", NS + "vitrified_waste_canister"}
MAX_WASTE_PACKAGES = 20
# Every template written in this run, so worldgen_all.py can point out files no script writes any more.
WRITTEN = []

ORIENTATIONS = {"down_east", "down_north", "down_south", "down_west", "up_east", "up_north", "up_south", "up_west",
                "west_up", "east_up", "north_up", "south_up"}
FACING_OUT = {"north": "north_up", "south": "south_up", "east": "east_up", "west": "west_up"}
CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
CCW = {v: k for k, v in CW.items()}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}

# Blocks whose sides connect (panes, bars, fences, walls): their state is worked out from the neighbours
# when the piece is written, since jigsaw pieces are placed without shape updates.
PANES = ("iron_bars", "glass_pane", "create:industrial_iron_window_pane", "create:weathered_iron_window_pane")
FENCES = ("_fence",)
WALLS = ("_wall",)
NOT_SOLID = ("air", "stairs", "slab", "ladder", "torch", "lantern", "chest", "door", "button", "lever", "sign", "bed",
             "carpet", "rail", "cobweb", "chain", "pane", "bars", "fence", "_wall", "pipe", "seat", "scaffolding", "candle",
             "pot", "banner", "jigsaw", "control_panel", "control_table", "annunciator", "siren", "alarm", "smoke_detector",
             "monitor", "chart_recorder", "grass", "fern", "vine", "dead_bush", "lily", "cauldron", "anvil", "hopper",
             "water", "lava", "flower", "sapling", "moss_carpet", "snow", "gate", "trapdoor", "girder", "brewing_stand",
             "rod", "bell", "skull", "head", "glass_fluid_pipe", "fluid_pipe", "shaft", "cogwheel")


def n(name):
    """A full block id: bare names are vanilla."""
    return name if ":" in name else "minecraft:" + name


def solid(name):
    if name is None:
        return False
    short = name.split(":", 1)[1]
    return not any(token in short for token in NOT_SOLID)


class Piece:
    def __init__(self, rel, size):
        self.rel = rel
        self.size = size
        self.blocks = {}
        self.rng = random.Random(zlib.crc32(rel.encode("utf-8")))

    # ------------------------------------------------------------------ basic placement

    def inside(self, pos):
        return all(0 <= pos[i] < self.size[i] for i in range(3))

    def put(self, pos, name, props=None, nbt=None):
        """Places (or replaces) a block; positions outside the box are an error."""
        pos = tuple(pos)
        if not self.inside(pos):
            raise ValueError(f"{self.rel}: {pos} is outside {self.size} ({name})")
        name = n(name)
        props = {k: str(v) for k, v in (props or {}).items()}
        self.blocks[pos] = (name, props, nbt) if nbt else (name, props)

    def get(self, pos):
        entry = self.blocks.get(tuple(pos))
        return entry[0] if entry else None

    def is_air(self, pos):
        return self.get(pos) == "minecraft:air"

    def fill(self, a, b, name, props=None):
        for x in range(min(a[0], b[0]), max(a[0], b[0]) + 1):
            for y in range(min(a[1], b[1]), max(a[1], b[1]) + 1):
                for z in range(min(a[2], b[2]), max(a[2], b[2]) + 1):
                    self.put((x, y, z), name, props)

    def air_box(self, a, b):
        self.fill(a, b, "air")

    def hollow_box(self, a, b, name, props=None, inside="air"):
        """A shell of name from a to b; the inside is emptied (or filled with inside, or left alone when None)."""
        x0, x1 = sorted((a[0], b[0]))
        y0, y1 = sorted((a[1], b[1]))
        z0, z1 = sorted((a[2], b[2]))
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    shell = x in (x0, x1) or y in (y0, y1) or z in (z0, z1)
                    if shell:
                        self.put((x, y, z), name, props)
                    elif inside is not None:
                        self.put((x, y, z), inside)

    def walls(self, a, b, name, props=None):
        """The four vertical walls of a box, without floor or ceiling."""
        x0, x1 = sorted((a[0], b[0]))
        y0, y1 = sorted((a[1], b[1]))
        z0, z1 = sorted((a[2], b[2]))
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put((x, y, z0), name, props)
                self.put((x, y, z1), name, props)
            for z in range(z0, z1 + 1):
                self.put((x0, y, z), name, props)
                self.put((x1, y, z), name, props)

    # ------------------------------------------------------------------ furniture

    def stairs(self, pos, block, facing, half="bottom", shape="straight"):
        self.put(pos, block, {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"})

    def slab(self, pos, block, kind="bottom"):
        self.put(pos, block, {"type": kind, "waterlogged": "false"})

    def facing(self, pos, block, facing, **extra):
        props = {"facing": facing}
        props.update(extra)
        self.put(pos, block, props)

    def ladder(self, pos, facing):
        self.put(pos, "ladder", {"facing": facing, "waterlogged": "false"})

    def lantern(self, pos, hanging=True):
        self.put(pos, "lantern", {"hanging": "true" if hanging else "false", "waterlogged": "false"})

    def chain(self, pos, axis="y"):
        self.put(pos, "chain", {"axis": axis, "waterlogged": "false"})

    def pillar(self, pos, block, axis="y"):
        self.put(pos, block, {"axis": axis})

    def trapdoor(self, pos, block, facing, half="bottom", open_=False):
        self.put(pos, block, {"facing": facing, "half": half, "open": "true" if open_ else "false",
                              "powered": "false", "waterlogged": "false"})

    def door(self, pos, block, facing, hinge="left", open_=False):
        x, y, z = pos
        for half, dy in (("lower", 0), ("upper", 1)):
            self.put((x, y + dy, z), block, {"facing": facing, "half": half, "hinge": hinge,
                                             "open": "true" if open_ else "false", "powered": "false"})

    def bed(self, foot, facing, color="white"):
        """A bed whose head is one step towards facing from its foot."""
        dx, dz = STEP[facing]
        name = f"{color}_bed"
        self.put(foot, name, {"facing": facing, "part": "foot", "occupied": "false"})
        self.put((foot[0] + dx, foot[1], foot[2] + dz), name, {"facing": facing, "part": "head", "occupied": "false"})

    def chest(self, pos, facing, loot):
        """A chest with a loot table, create_nuclearindustry:chests/<loot>; the game seeds it when the piece is placed."""
        self.put(pos, "chest", {"facing": facing, "type": "single", "waterlogged": "false"},
                 {"id": "minecraft:chest", "LootTable": NS + "chests/" + loot})

    def barrel(self, pos, facing="up", loot=None):
        """A vanilla barrel (storage, not waste), with a loot table if given."""
        nbt = {"id": "minecraft:barrel", "LootTable": NS + "chests/" + loot} if loot else None
        self.put(pos, "barrel", {"facing": facing, "open": "false"}, nbt)

    def sign(self, pos, facing, sign_key, lines=2, wood="spruce"):
        """A waxed wall sign hung on the block behind it (opposite facing), its text translated:
        create_nuclearindustry.ruins.sign.<sign_key>.<n> for each of its lines (RuinsDataGen.lang)."""
        messages = [json.dumps({"translate": f"create_nuclearindustry.ruins.sign.{sign_key}.{i + 1}"}) if i < lines
                    else json.dumps("") for i in range(4)]
        side = {"messages": messages, "color": "black", "has_glowing_text": nbtwriter.Byte(0)}
        empty = {"messages": [json.dumps("")] * 4, "color": "black", "has_glowing_text": nbtwriter.Byte(0)}
        self.put(pos, f"{wood}_wall_sign", {"facing": facing, "waterlogged": "false"},
                 {"id": "minecraft:sign", "front_text": side, "back_text": empty, "is_waxed": nbtwriter.Byte(1)})

    def desk_row(self, x0, x1, y, z, facing):
        """A straight run of control desks along x (facing north or south), joined end to end like the game joins them."""
        xs = range(min(x0, x1), max(x0, x1) + 1)
        for x in xs:
            self.put((x, y, z), NS + "reactor_control_panel", {"facing": facing, "shape": "straight",
                                                               "left": "false", "right": "false"})
        for x in xs:
            props = self.blocks[(x, y, z)][1]
            # A straight desk's left end is clockwise of its facing, its right end anticlockwise (DeskShape.STRAIGHT).
            for side, key in ((CW[facing], "left"), (CCW[facing], "right")):
                dx, dz = STEP[side]
                other = self.blocks.get((x + dx, y, z + dz))
                props[key] = "true" if other and other[0] == NS + "reactor_control_panel" and other[1]["facing"] == facing else "false"

    def flat_panel_row(self, block, cells, facing):
        """Wall panels or control tables at cells, joined left/right to the same block facing the same way."""
        for pos in cells:
            self.put(pos, NS + block, {"facing": facing, "left": "false", "right": "false"})
        self._join(NS + block, cells, facing, vertical=False)

    def annunciator_board(self, cells, facing):
        for pos in cells:
            self.put(pos, NS + "annunciator_panel", {"facing": facing, "mounted": "true", "left": "false", "right": "false",
                                                     "up": "false", "down": "false"})
        self._join(NS + "annunciator_panel", cells, facing, vertical=True)

    def _join(self, name, cells, facing, vertical):
        for (x, y, z) in cells:
            props = self.blocks[(x, y, z)][1]
            sides = [((STEP[CW[facing]][0], 0, STEP[CW[facing]][1]), "left"),
                     ((STEP[CCW[facing]][0], 0, STEP[CCW[facing]][1]), "right")]
            if vertical:
                sides += [((0, 1, 0), "up"), ((0, -1, 0), "down")]
            for (dx, dy, dz), key in sides:
                other = self.blocks.get((x + dx, y + dy, z + dz))
                props[key] = "true" if other and other[0] == name and other[1].get("facing") == facing else "false"

    # ------------------------------------------------------------------ jigsaws

    def jigsaw(self, pos, orientation, name, target, pool, final_state, joint=None, placement_priority=0,
               selection_priority=0):
        if orientation not in ORIENTATIONS:
            raise ValueError(f"{self.rel}: bad jigsaw orientation {orientation}")
        if joint is None:
            # Horizontal jigsaws always meet "up" to "up"; vertical ones keep the parent's rotation.
            joint = "aligned"
        self.put(pos, "jigsaw", {"orientation": orientation}, {
            "id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool, "final_state": n(final_state),
            "joint": joint, "placement_priority": placement_priority, "selection_priority": selection_priority})

    def back(self, pos, facing, name, final_state):
        """The jigsaw this piece is attached by: it only answers to name, and grows nothing."""
        self.jigsaw(pos, FACING_OUT[facing], name, EMPTY, EMPTY, final_state)

    def exit(self, pos, facing, target, pool, final_state, priority=0):
        """A jigsaw that grows a piece of pool out of this face."""
        self.jigsaw(pos, FACING_OUT[facing], EMPTY, target, pool, final_state, placement_priority=priority)

    # ------------------------------------------------------------------ decay

    def chance(self, p):
        return self.rng.random() < p

    def decay(self, p, names, to, region=None):
        """Swaps each block named in names, with probability p, for one of to (weighted list of names)."""
        names = {n(x) for x in names}
        for pos in sorted(self.blocks):
            if region and not all(region[0][i] <= pos[i] <= region[1][i] for i in range(3)):
                continue
            if self.blocks[pos][0] in names and self.chance(p):
                self.put(pos, self.rng.choice(to))

    def rubble(self, a, b, p, names=("gravel", "cobblestone", "andesite", "tuff")):
        """Loose rubble on the floor of a region: on air blocks that stand on something solid."""
        for x in range(min(a[0], b[0]), max(a[0], b[0]) + 1):
            for z in range(min(a[2], b[2]), max(a[2], b[2]) + 1):
                for y in range(min(a[1], b[1]), max(a[1], b[1]) + 1):
                    if self.is_air((x, y, z)) and solid(self.get((x, y - 1, z))) and self.chance(p):
                        self.put((x, y, z), self.rng.choice(names))
                        break

    # ------------------------------------------------------------------ output

    def _connections(self):
        for pos in sorted(self.blocks):
            name, props = self.blocks[pos][0], self.blocks[pos][1]
            short = name.split(":", 1)[1]
            pane = name in {n(p) for p in PANES}
            fence = short.endswith(FENCES) and "gate" not in short
            wall = short.endswith(WALLS)
            if not (pane or fence or wall):
                continue
            x, y, z = pos
            for side, (dx, dz) in STEP.items():
                other = self.get((x + dx, y, z + dz))
                if other is None:
                    joined = False
                else:
                    oshort = other.split(":", 1)[1]
                    same = (pane and other in {n(p) for p in PANES}) or (fence and oshort.endswith(FENCES)) \
                        or (wall and oshort.endswith(WALLS))
                    joined = same or solid(other)
                if wall:
                    props[side] = "low" if joined else "none"
                else:
                    props[side] = "true" if joined else "false"
            props["waterlogged"] = "false"
            if wall:
                straight = (props["north"] != "none" and props["south"] != "none" and props["east"] == "none"
                            and props["west"] == "none") or (props["east"] != "none" and props["west"] != "none"
                                                              and props["north"] == "none" and props["south"] == "none")
                props["up"] = "false" if straight else "true"

    def write(self):
        names = {entry[0] for entry in self.blocks.values()}
        bad = names & FORBIDDEN
        if bad:
            raise ValueError(f"{self.rel}: ruins must not hold {sorted(bad)}")
        barrels = sum(1 for entry in self.blocks.values() if entry[0] == BARREL)
        if barrels > MAX_LEAKING_BARRELS:
            raise ValueError(f"{self.rel}: {barrels} leaking barrels, at most {MAX_LEAKING_BARRELS}")
        packages = sum(1 for entry in self.blocks.values() if entry[0] in WASTE_PACKAGES)
        if packages > MAX_WASTE_PACKAGES:
            raise ValueError(f"{self.rel}: {packages} waste packages, at most {MAX_WASTE_PACKAGES}")
        jigsaws = [pos for pos, entry in self.blocks.items() if entry[0] == "minecraft:jigsaw"]
        if not jigsaws:
            raise ValueError(f"{self.rel}: a jigsaw piece needs at least its back jigsaw")
        for pos in jigsaws:
            on_face = any(pos[i] in (0, self.size[i] - 1) for i in range(3))
            if not on_face:
                raise ValueError(f"{self.rel}: jigsaw at {pos} is not on the box's face")
        self._connections()
        path = OUT / f"{self.rel}.nbt"
        path.parent.mkdir(parents=True, exist_ok=True)
        nbtwriter.write(path, nbtwriter.structure(self.size, self.blocks))
        WRITTEN.append(path)
        print(f"wrote {path.relative_to(ROOT)} ({len(self.blocks)} blocks, {barrels} leaking barrels)")
        return path

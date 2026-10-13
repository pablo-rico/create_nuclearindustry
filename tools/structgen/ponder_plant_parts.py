"""Building blocks shared by the ponder_plant_*.py scripts (the "Nuclear power plant" Ponder series).

Every helper writes into a dict {(x, y, z): (name, {prop: value})} for nbtwriter.structure and
refuses to overwrite a block that is already there, so two parts of a layout can never overlap
by accident. Each schematic is written twice: for Ponder (assets/.../ponder/<name>.nbt) and as a
GameTest template (data/.../structure/gametest/<name>_ponder.nbt), so a GameTest can prove that
the multiblocks shown in the scene really form.
"""
import pathlib

import nbtwriter

ROOT = pathlib.Path(__file__).resolve().parents[2]
NS = "create_nuclearindustry:"
DIRECTIONS = ("north", "south", "east", "west", "up", "down")


def put(blocks, pos, name, props=None):
    if pos in blocks:
        raise ValueError(f"{pos} is taken by {blocks[pos][0]}, cannot place {name}")
    blocks[pos] = (name, dict(props or {}))


def replace(blocks, pos, name, props=None):
    """Swaps a block of a shell already placed (a port in a wall, for instance)."""
    if pos not in blocks:
        raise ValueError(f"nothing to replace at {pos} with {name}")
    blocks[pos] = (name, dict(props or {}))


def floor(blocks, size, name="minecraft:smooth_stone", y=0):
    for x in range(size[0]):
        for z in range(size[2]):
            put(blocks, (x, y, z), name)


def pipe(blocks, pos, *open_sides):
    props = {d: ("true" if d in open_sides else "false") for d in DIRECTIONS}
    props["waterlogged"] = "false"
    put(blocks, pos, "create:fluid_pipe", props)


def pipe_run(blocks, start, end, *ends_open):
    """A straight run of pipe from start to end (inclusive), open along its axis."""
    (x0, y0, z0), (x1, y1, z1) = start, end
    if x0 != x1:
        sides = ("east", "west")
    elif y0 != y1:
        sides = ("up", "down")
    else:
        sides = ("north", "south")
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                pipe(blocks, (x, y, z), *sides)


AXIS = {"north": "z", "south": "z", "east": "x", "west": "x", "up": "y", "down": "y"}
OFFSET = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}


def pump(blocks, pos, facing, motor_side):
    """
    A Create pump pushing towards facing, turned through a cogwheel on top of it by a creative
    motor on motor_side of the cog, its shaft pointing into the cog along the pump's axis.
    """
    put(blocks, pos, "create:mechanical_pump", {"facing": facing, "waterlogged": "false"})
    cog = (pos[0], pos[1] + 1, pos[2])
    put(blocks, cog, "create:cogwheel", {"axis": AXIS[facing], "waterlogged": "false"})
    dx, dy, dz = OFFSET[motor_side]
    if AXIS[motor_side] != AXIS[facing]:
        raise ValueError("the motor must sit on the cog's axis")
    put(blocks, (cog[0] + dx, cog[1] + dy, cog[2] + dz), "create:creative_motor", {"facing": OPPOSITE[motor_side]})


def reactor(blocks, origin, rods=((1, 1), (3, 1), (1, 3), (3, 3)), open_rod=None):
    """
    The 5x5x5 fission reactor of the other Ponder scenes with its lowest corner at origin: fuel
    channels on the four sides of the 3x3 core, a heat exchanger in the middle, control rods in
    the given interior columns (dx, dz in 1..3) and the controller at the centre of the lid.
    open_rod leaves the lid above that rod column free for a piston head. Returns the controller.
    """
    ox, oy, oz = origin
    for x in range(5):
        for y in range(5):
            for z in range(5):
                if x in (0, 4) or y in (0, 4) or z in (0, 4):
                    put(blocks, (ox + x, oy + y, oz + z), NS + "reactor_casing")
    for dx in range(1, 4):
        for dz in range(1, 4):
            if (dx, dz) == (2, 2):
                name = "heat_exchanger"
            elif (dx, dz) in rods:
                name = "control_rod"
            elif dx == 2 or dz == 2:
                name = "uranium_rod"
            else:
                continue
            for y in range(1, 4):
                put(blocks, (ox + dx, oy + y, oz + dz), NS + name)
    if open_rod is not None:
        del blocks[(ox + open_rod[0], oy + 4, oz + open_rod[1])]
    controller = (ox + 2, oy + 4, oz + 2)
    replace(blocks, controller, NS + "reactor_controller", {"on": "false"})
    return controller


def reactor_port(blocks, pos, facing, mode):
    replace(blocks, pos, NS + "reactor_fluid_port", {"facing": facing, "mode": mode})


def steam_generator(blocks, bottom_center, height, controller_facing, ports):
    """
    A 3x3 steam generator: casing around a column of tube bundles, the controller in the wall
    on controller_facing's side of the first tube layer, and ports {pos: role}.
    Returns the controller position.
    """
    cx, cy, cz = bottom_center
    for y in range(height):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                tube = dx == 0 and dz == 0 and 0 < y < height - 1
                put(blocks, (cx + dx, cy + y, cz + dz), NS + ("steam_generator_tube_bundle" if tube else "steam_generator_casing"))
    ox, _, oz = OFFSET[controller_facing]
    controller = (cx + ox, cy + 1, cz + oz)
    replace(blocks, controller, NS + "steam_generator_controller", {"facing": controller_facing})
    for pos, role in ports.items():
        replace(blocks, pos, NS + "steam_generator_port", {"role": role})
    return controller


def write(name, size, blocks):
    for (x, y, z) in blocks:
        if not (0 <= x < size[0] and 0 <= y < size[1] and 0 <= z < size[2]):
            raise ValueError(f"{(x, y, z)} lies outside {size}")
    data = nbtwriter.structure(size, blocks)
    for path in (ROOT / f"src/main/resources/assets/create_nuclearindustry/ponder/{name}.nbt",
                 ROOT / f"src/main/resources/data/create_nuclearindustry/structure/gametest/{name}_ponder.nbt"):
        path.parent.mkdir(parents=True, exist_ok=True)
        nbtwriter.write(path, data)
        print("wrote", path.relative_to(ROOT))

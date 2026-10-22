#!/usr/bin/env python3
"""Write the hand-made block and item models of the antimatter, synchrotron, energy and isotope content.

    python tools/modelgen/generate.py [--out <assets dir>]

The traps are too detailed to keep by hand, and the large trap spans 2x2x3 blocks, so each trap is
described once in global pixel coordinates (``Box`` below) and this script cuts it into one model
per block: every box is clipped to the block's 16x16x16 cell, faces created by the cut are dropped,
and the UVs are computed from the block-local coordinates (like vanilla's default UVs) or
interpolated from the box's explicit UVs. Boxes rotated round Y cannot be clipped across X/Z, so
they are clipped only in height and handed whole to the block that holds their centre (model
coordinates may run from -16 to 32). Item models are the same pieces put back together and scaled
to fit one block.

Glass is see-through by vertex alpha (``neoforge_data.color``) on opaque textures; the field bands
and lamps are full-bright (``block_light`` 15). Output is deterministic and sorted, so running it
twice gives identical files. Needs only the standard library.
"""

import argparse
import json
import math
import os

DEFAULT_OUT = os.path.join("src", "main", "resources", "assets", "create_nuclearindustry")
NS = "create_nuclearindustry:"
TAN = math.tan(math.radians(22.5))

GLASS_ALPHA = "48FFFFFF"
CONDUIT_ALPHA = "58FFFFFF"
GLOW = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}

FACES = ("north", "south", "west", "east", "up", "down")
# For each face: which world axis runs along u and along v, and whether it runs backwards
# (vanilla's default UVs: north u = 16 - x, every side v = 16 - y, down v = 16 - z ...).
UV_AXES = {
    "north": ((0, True), (1, True)),
    "south": ((0, False), (1, True)),
    "west": ((2, False), (1, True)),
    "east": ((2, True), (1, True)),
    "up": ((0, False), (2, False)),
    "down": ((0, False), (2, True)),
}
# The axis a face is perpendicular to, and whether it sits on the box's max side.
FACE_PLANE = {"north": (2, False), "south": (2, True), "west": (0, False), "east": (0, True),
              "down": (1, False), "up": (1, True)}


def r4(v):
    """Rounded for JSON: whole numbers stay integers, others keep 4 decimals."""
    v = round(v, 4)
    return int(v) if v == int(v) else v


class Box:
    """One model element in global pixel coordinates.

    ``faces`` maps a face name to a texture key (``"#coil"``: UVs from position) or to
    ``(texture, [u0, v0, u1, v1])`` for explicit UVs over the whole, unclipped face.
    ``rot`` is an angle about Y (+-22.5 / 45) round ``origin``."""

    def __init__(self, frm, to, faces, rot=0.0, origin=None, glow=False, alpha=None, shade=True, cull=True):
        self.frm = list(frm)
        self.to = list(to)
        self.faces = dict(faces)
        self.rot = rot
        self.origin = list(origin) if origin is not None else None
        self.glow = glow
        self.alpha = alpha
        self.shade = shade
        self.cull = cull


def sides(tex, top=None, bottom=None, skip=()):
    """The four side faces with one texture, plus optional top/bottom textures."""
    f = {d: tex for d in ("north", "south", "west", "east") if d not in skip}
    if top and "up" not in skip:
        f["up"] = top
    if bottom and "down" not in skip:
        f["down"] = bottom
    return f


def all_faces(tex, skip=()):
    return {d: tex for d in FACES if d not in skip}


def _face_uv(box, face, lo, hi, spec):
    """UVs of ``face`` of the clipped box [lo, hi] (block-local), from ``spec``."""
    (ua, urev), (va, vrev) = UV_AXES[face]
    if isinstance(spec, tuple):
        tex, uv = spec
        # Interpolate the explicit UV over the box's original extent.
        def lerp(axis, rev, t0, t1, value):
            a, b = box.frm[axis], box.to[axis]
            f = 0.0 if b == a else (value - a) / (b - a)
            if rev:
                f = 1.0 - f
            return t0 + (t1 - t0) * f
        offs = box._offset
        u_lo, u_hi = lo[ua] + offs[ua], hi[ua] + offs[ua]
        v_lo, v_hi = lo[va] + offs[va], hi[va] + offs[va]
        us = sorted((lerp(ua, urev, uv[0], uv[2], u_lo), lerp(ua, urev, uv[0], uv[2], u_hi)))
        vs = sorted((lerp(va, vrev, uv[1], uv[3], v_lo), lerp(va, vrev, uv[1], uv[3], v_hi)))
        # Keep the texture's own orientation when the explicit UV is flipped.
        if uv[0] > uv[2]:
            us = us[::-1]
        if uv[1] > uv[3]:
            vs = vs[::-1]
        return tex, [us[0], vs[0], us[1], vs[1]]
    tex = spec

    def coord(axis, rev, value):
        return 16 - value if rev else value

    u0, u1 = sorted((coord(ua, urev, lo[ua]), coord(ua, urev, hi[ua])))
    v0, v1 = sorted((coord(va, vrev, lo[va]), coord(va, vrev, hi[va])))
    # Rotated pieces may sit outside the block: slide their UVs back into the texture.
    for lo_, hi_ in ((u0, u1), (v0, v1)):
        assert hi_ - lo_ <= 16.0001, (box.frm, box.to, face)
    shift_u = -16 * math.floor(u0 / 16) if u0 < 0 or u1 > 16 else 0
    shift_v = -16 * math.floor(v0 / 16) if v0 < 0 or v1 > 16 else 0
    u0, u1, v0, v1 = u0 + shift_u, u1 + shift_u, v0 + shift_v, v1 + shift_v
    if u1 > 16.0001 or v1 > 16.0001:
        u0, u1 = max(0, min(u0, 16 - (u1 - u0))), min(16, max(u1, u1 - u0))
        v0, v1 = max(0, min(v0, 16 - (v1 - v0))), min(16, max(v1, v1 - v0))
    return tex, [u0, v0, u1, v1]


def _element(box, lo, hi, local_origin, keep):
    """The JSON element for the part [lo, hi] (block-local) of ``box``; ``keep`` = faces to emit."""
    faces = {}
    for face in FACES:
        if face not in box.faces or face not in keep:
            continue
        tex, uv = _face_uv(box, face, lo, hi, box.faces[face])
        entry = {"uv": [r4(v) for v in uv], "texture": tex}
        axis, high = FACE_PLANE[face]
        if box.cull and box.rot == 0 and (hi[axis] if high else lo[axis]) in ((16,) if high else (0,)):
            entry["cullface"] = face
        faces[face] = entry
    if not faces:
        return None
    el = {"from": [r4(v) for v in lo], "to": [r4(v) for v in hi]}
    if box.rot:
        el["rotation"] = {"angle": box.rot, "axis": "y", "origin": [r4(v) for v in local_origin]}
    if not box.shade:
        el["shade"] = False
    data = {}
    if box.glow:
        data.update(GLOW)
        el["shade"] = False
    if box.alpha:
        data["color"] = box.alpha
        data["ambient_occlusion"] = False
    if data:
        el["neoforge_data"] = data
    el["faces"] = faces
    return el


def slice_boxes(boxes, cell):
    """The elements of the block whose min corner is ``cell`` (global pixels)."""
    out = []
    for box in boxes:
        lo, hi = list(box.frm), list(box.to)
        if box.rot:
            # Clip only in height; the whole piece belongs to the block holding its centre.
            cx, cz = (lo[0] + hi[0]) / 2, (lo[2] + hi[2]) / 2
            ox, oz = box.origin[0], box.origin[2]
            a = math.radians(box.rot)
            # Right-handed rotation about +Y, as vanilla bakes it.
            wx = ox + (cx - ox) * math.cos(a) + (cz - oz) * math.sin(a)
            wz = oz - (cx - ox) * math.sin(a) + (cz - oz) * math.cos(a)
            if not (cell[0] <= wx < cell[0] + 16 and cell[2] <= wz < cell[2] + 16):
                continue
            ylo, yhi = max(lo[1], cell[1]), min(hi[1], cell[1] + 16)
            if ylo >= yhi and not (lo[1] == hi[1] and cell[1] <= lo[1] < cell[1] + 16):
                continue
            lo2 = [lo[0] - cell[0], ylo - cell[1], lo[2] - cell[2]]
            hi2 = [hi[0] - cell[0], yhi - cell[1], hi[2] - cell[2]]
        else:
            lo2, hi2 = [], []
            empty = False
            for i in range(3):
                a, b = max(lo[i], cell[i]), min(hi[i], cell[i] + 16)
                if lo[i] == hi[i]:
                    # A flat panel belongs to the cell it lies in (its low face if on a border).
                    if not cell[i] <= lo[i] < cell[i] + 16:
                        empty = True
                    a = b = lo[i]
                elif a >= b:
                    empty = True
                lo2.append(a - cell[i])
                hi2.append(b - cell[i])
            if empty:
                continue
        # Faces made by the cut are inside the trap: keep only the box's own faces.
        keep = set()
        for face in FACES:
            axis, high = FACE_PLANE[face]
            orig = (box.to if high else box.frm)[axis] - cell[axis]
            if abs((hi2 if high else lo2)[axis] - orig) < 1e-6:
                keep.add(face)
        box._offset = cell
        origin = None
        if box.rot:
            origin = [box.origin[0] - cell[0], box.origin[1] - cell[1], box.origin[2] - cell[2]]
        el = _element(box, lo2, hi2, origin, keep)
        if el:
            out.append(el)
    return out


def transform(elements, cell, scale, offset):
    """Elements of one block moved back to global coordinates, scaled and shifted (item models)."""
    out = []
    for el in elements:
        el = json.loads(json.dumps(el))
        el["from"] = [r4((v + cell[i]) * scale + offset[i]) for i, v in enumerate(el["from"])]
        el["to"] = [r4((v + cell[i]) * scale + offset[i]) for i, v in enumerate(el["to"])]
        if "rotation" in el:
            el["rotation"]["origin"] = [r4((v + cell[i]) * scale + offset[i]) for i, v in enumerate(el["rotation"]["origin"])]
        for face in el["faces"].values():
            face.pop("cullface", None)
        out.append(el)
    return out


# ======================================================================================
# glass cylinder
# ======================================================================================

def glass_cylinder(cx, cz, apothem, y0, y1, tex="#glass", alpha=GLASS_ALPHA):
    """An octagonal glass wall: eight flat panels, each showing a 2 px column of the unrolled
    glass texture on both sides (outside, and the far wall seen through the near one). Panels go
    round clockwise from above: N, NE, E, SE, S, SW, W, NW (u = 0, 2, 4 ... 14)."""
    w = apothem * TAN
    boxes = []
    origin = (cx, (y0 + y1) / 2, cz)

    def ns(z, u, rot, outward):
        inward = "south" if outward == "north" else "north"
        return Box((cx - w, y0, z), (cx + w, y1, z),
                   {outward: (tex, [u, 0, u + 2, 16]), inward: (tex, [u + 2, 0, u, 16])},
                   rot=rot, origin=origin, alpha=alpha, cull=False)

    def ew(x, u, outward):
        inward = "west" if outward == "east" else "east"
        return Box((x, y0, cz - w), (x, y1, cz + w),
                   {outward: (tex, [u, 0, u + 2, 16]), inward: (tex, [u + 2, 0, u, 16])},
                   alpha=alpha, cull=False)

    boxes.append(ns(cz - apothem, 0, 0, "north"))
    boxes.append(ns(cz - apothem, 2, -45, "north"))   # NE
    boxes.append(ew(cx + apothem, 4, "east"))
    boxes.append(ns(cz + apothem, 6, 45, "south"))    # SE
    boxes.append(ns(cz + apothem, 8, 0, "south"))
    boxes.append(ns(cz + apothem, 10, -45, "south"))  # SW
    boxes.append(ew(cx - apothem, 12, "west"))
    boxes.append(ns(cz - apothem, 14, 45, "north"))   # NW
    return boxes


def head(cx, cz, y, up, base, coil, band, cap, coil_tex="#coil", rings=None, core=None):
    """A magnet head stacked from ``y`` upwards (``up``) or downwards: end plate, exposed coil
    (or ``rings`` of coil over a steel ``core``), the glowing field band and the pole cap.
    Each part is (half-width, thickness)."""
    boxes = []
    s = 1 if up else -1

    def slab(half, y0, thick, faces, glow=False):
        a, b = (y0, y0 + thick) if up else (y0 - thick, y0)
        return Box((cx - half, a, cz - half), (cx + half, b, cz + half), faces, glow=glow), y0 + s * thick

    plate_faces = sides("#steel", bottom="#plate") if up else sides("#steel", top="#plate")
    b, y = slab(base[0], y, base[1], plate_faces)
    if up:
        b.faces["up"] = "#plate"
    else:
        b.faces["down"] = "#plate"
    boxes.append(b)
    if rings:
        y_core = y
        b, _ = slab(core[0], y, core[1], sides("#steel"))
        boxes.append(b)
        for offset, thick in rings:
            yy = y_core + s * offset
            b, _ = slab(coil[0], yy, thick, sides(coil_tex, top="#copper", bottom="#copper"))
            boxes.append(b)
        y = y_core + s * core[1]
    else:
        b, y = slab(coil[0], y, coil[1], sides(coil_tex))
        boxes.append(b)
    b, y = slab(band[0], y, band[1], all_faces("#band"), glow=True)
    boxes.append(b)
    b, y = slab(cap[0], y, cap[1], sides("#steel", top="#pole") if up else sides("#steel", bottom="#pole"))
    boxes.append(b)
    return boxes, y


def trap_textures(band):
    return {
        "particle": NS + "block/penning_trap_steel",
        "steel": NS + "block/penning_trap_steel",
        "plate": NS + "block/penning_trap_plate",
        "pole": NS + "block/penning_trap_pole",
        "coil": NS + "block/penning_trap_coil",
        "copper": NS + "block/penning_trap_copper",
        "band": NS + "block/penning_trap_band_" + band,
        "glass": NS + "block/penning_trap_glass",
        "port": NS + "block/penning_trap_port",
        "led": NS + "block/penning_trap_led",
        "brass": NS + "block/penning_trap_brass",
        "gauge": NS + "block/penning_trap_gauge",
        "fins": NS + "block/penning_trap_fins",
        "cable": NS + "block/penning_trap_cable",
        "plinth": NS + "block/penning_trap_plinth",
    }


def rod(x, z, y0, y1, half=0.5, tex="#steel"):
    return Box((x - half, y0, z - half), (x + half, y1, z + half), sides(tex))


def front_fitting(x0, y0, x1, y1, z_front, depth, face_tex, side_tex, glow=False):
    """A box on a north face: its front shows the whole of ``face_tex``."""
    return Box((x0, y0, z_front), (x1, y1, z_front + depth),
               dict(sides(side_tex, top=side_tex, bottom=side_tex, skip=("north", "south")),
                    north=(face_tex, [0, 0, 16, 16])), glow=glow)


# ======================================================================================
# the three traps
# ======================================================================================

def small_trap():
    """1x1: an 8 px cylinder between 4 px heads with one coil each, four tie rods."""
    boxes = []
    hb, _ = head(8, 8, 0, True, (7, 1), (6, 1.5), (6.5, 0.75), (6, 0.75))
    boxes += hb
    ht, _ = head(8, 8, 16, False, (7, 1), (6, 1.5), (6.5, 0.75), (6, 0.75))
    boxes += ht
    boxes += glass_cylinder(8, 8, 5, 4, 12)
    for x, z in ((2.75, 2.75), (13.25, 2.75), (2.75, 13.25), (13.25, 13.25)):
        boxes.append(rod(x, z, 4, 12))
    # Front: the vial port on the lower head, the status LED on the upper one.
    boxes.append(front_fitting(5, 0.5, 11, 3.5, 0.5, 1.25, "#port", "#brass"))
    boxes.append(Box((6, 13.25, 0.75), (10, 15.75, 1.5), sides("#steel", top="#steel", bottom="#steel", skip=("south",))))
    boxes.append(front_fitting(7, 13.75, 9, 15.25, 0.5, 0.25, "#led", "#led"))
    return boxes


def medium_trap():
    """1x2: a 20 px, narrower cylinder; heads with three coil rings each; a brass ring clamp a
    third of the way up carrying the analogue field gauge on the front-right diagonal (north-west)."""
    boxes = []
    rings = [(0.25, 0.75), (1.375, 0.75), (2.5, 0.75)]
    hb, _ = head(8, 8, 0, True, (7, 1), (5.5, 0), (6, 0.75), (5, 0.75), rings=rings, core=(4.5, 3.5))
    boxes += hb
    ht, _ = head(8, 8, 32, False, (7, 1), (5.5, 0), (6, 0.75), (5, 0.75), rings=rings, core=(4.5, 3.5))
    boxes += ht
    boxes += glass_cylinder(8, 8, 4, 6, 26)
    # The ring clamp: a square frame round the glass, open in the middle, a third of the way up so
    # it never hides the cloud floating in the middle of the cylinder.
    for lo, hi in (((3.25, 10.5, 3.25), (12.75, 11.5, 4)), ((3.25, 10.5, 12), (12.75, 11.5, 12.75)),
                   ((3.25, 10.5, 4), (4, 11.5, 12)), ((12, 10.5, 4), (12.75, 11.5, 12))):
        boxes.append(Box(lo, hi, all_faces("#brass")))
    for x, z in ((3.75, 12.25), (12.25, 3.75), (12.25, 12.25), (3.75, 3.75)):
        boxes.append(rod(x, z, 6, 26, half=0.45))
    # Gauge: a brass housing on an arm off the clamp, turned 45 degrees to face north-west.
    o = (8, 11, 8)
    boxes.append(Box((5.75, 8.75, 0.25), (10.25, 13.25, 1.5),
                     dict(sides("#brass", top="#brass", bottom="#brass", skip=("north",)),
                          north=("#gauge", [0, 0, 16, 16])), rot=45, origin=o, cull=False))
    boxes.append(Box((7.5, 10.5, 1.5), (8.5, 11.5, 4.0), sides("#brass", top="#brass", bottom="#brass",
                                                                 skip=("north", "south")), rot=45, origin=o, cull=False))
    boxes.append(front_fitting(5, 0.5, 11, 4.25, 0.75, 1.5, "#port", "#brass"))
    boxes.append(Box((6, 27.75, 1.0), (10, 30.25, 2.0), sides("#steel", top="#steel", bottom="#steel", skip=("south",))))
    boxes.append(front_fitting(7, 28.25, 9, 29.75, 0.75, 0.25, "#led", "#led"))
    return boxes


# Large trap: LED ring of 12 lenses at y 44.75..45.75, three per side, clockwise from above.
LARGE_LEDS = []
for _x in (8, 16, 24):
    LARGE_LEDS.append(("north", _x))
for _z in (8, 16, 24):
    LARGE_LEDS.append(("east", _z))
for _x in (24, 16, 8):
    LARGE_LEDS.append(("south", _x))
for _z in (24, 16, 8):
    LARGE_LEDS.append(("west", _z))


def _led_box(side, pos, y0, y1, depth, tex, glow=False, wall=2.0, span=1.0):
    """A lens proud of the large trap's housing (walls at 2 and 30)."""
    if side == "north":
        return Box((pos - span, y0, wall - depth), (pos + span, y1, wall), {"north": (tex, [0, 0, 16, 16])}, glow=glow)
    if side == "south":
        return Box((pos - span, y0, 32 - wall), (pos + span, y1, 32 - wall + depth), {"south": (tex, [0, 0, 16, 16])}, glow=glow)
    if side == "west":
        return Box((wall - depth, y0, pos - span), (wall, y1, pos + span), {"west": (tex, [0, 0, 16, 16])}, glow=glow)
    return Box((32 - wall, y0, pos - span), (32 - wall + depth, y1, pos + span), {"east": (tex, [0, 0, 16, 16])}, glow=glow)


def _fins(y0, y1, skip_side=None):
    """Cooling fins round a housing (walls at 2 and 30), clear of the middle where ports go."""
    out = []
    for p in (4, 7, 10, 22, 25, 28):
        out.append(Box((p, y0, 0.5), (p + 1, y1, 2), sides("#steel", top="#steel", bottom="#steel", skip=("south",))))
        out.append(Box((p, y0, 30), (p + 1, y1, 31.5), sides("#steel", top="#steel", bottom="#steel", skip=("north",))))
        out.append(Box((0.5, y0, p), (2, y1, p + 1), sides("#steel", top="#steel", bottom="#steel", skip=("east",))))
        out.append(Box((30, y0, p), (31.5, y1, p + 1), sides("#steel", top="#steel", bottom="#steel", skip=("west",))))
    return out


def large_trap():
    """2x2x3: a 24 px cylinder of 20 px across between massive heads (plinth, finned housing,
    exposed coil, band, pole cap), four support pillars, cable entry on the east, vial ports and
    status LEDs on north and south, and a ring of 12 LEDs round the top head."""
    boxes = []
    # Bottom head.
    boxes.append(Box((0, 0, 0), (32, 2, 32), sides("#plinth", top="#plate", bottom="#plate")))
    boxes.append(Box((2, 2, 2), (30, 7, 30), sides("#fins")))
    boxes += _fins(2.5, 6.5)
    boxes.append(Box((3, 7, 3), (29, 10, 29), sides("#coil")))
    boxes.append(Box((2.5, 10, 2.5), (29.5, 11, 29.5), all_faces("#band"), glow=True))
    boxes.append(Box((5, 11, 5), (27, 12, 27), sides("#steel", top="#steel")))
    # Pillars with their collars.
    for x, z in ((4.5, 4.5), (27.5, 4.5), (4.5, 27.5), (27.5, 27.5)):
        boxes.append(rod(x, z, 12, 36, half=2.0))
        boxes.append(Box((x - 2.5, 12, z - 2.5), (x + 2.5, 13.5, z + 2.5), sides("#steel", top="#plate")))
        boxes.append(Box((x - 2.5, 34.5, z - 2.5), (x + 2.5, 36, z + 2.5), sides("#steel", bottom="#plate")))
    boxes += glass_cylinder(16, 16, 10, 12, 36)
    # Top head.
    boxes.append(Box((5, 36, 5), (27, 37, 27), sides("#steel", bottom="#steel")))
    boxes.append(Box((2.5, 37, 2.5), (29.5, 38, 29.5), all_faces("#band"), glow=True))
    boxes.append(Box((3, 38, 3), (29, 41, 29), sides("#coil")))
    boxes.append(Box((2, 41, 2), (30, 46, 30), sides("#fins")))
    boxes += _fins(41.5, 44.25)
    boxes.append(Box((0, 46, 0), (32, 48, 32), sides("#steel", top="#plate", bottom="#plate")))
    # Fittings.
    boxes.append(Box((30, 41.5, 12), (31.75, 44.25, 20),
                     dict(sides("#steel", top="#steel", bottom="#steel", skip=("east", "west")), east=("#cable", [0, 0, 16, 16]))))
    boxes.append(front_fitting(12, 2.5, 20, 6.5, 0.5, 1.5, "#port", "#brass"))
    boxes.append(Box((12, 2.5, 30), (20, 6.5, 31.5),
                     dict(sides("#brass", top="#brass", bottom="#brass", skip=("north", "south")), south=("#port", [0, 0, 16, 16]))))
    for side in ("north", "south"):
        z0, z1 = (0.75, 2) if side == "north" else (30, 31.25)
        boxes.append(Box((13, 41.75, z0), (19, 44.5, z1), sides("#steel", top="#steel", bottom="#steel")))
        boxes.append(_led_box(side, 16, 42.25, 44.0, 1.5, "#led", span=2.0) if side == "north"
                     else _led_box(side, 16, 42.25, 44.0, 1.5, "#led", span=2.0))
    for side, pos in LARGE_LEDS:
        boxes.append(_led_box(side, pos, 44.75, 45.75, 0.25, "#led"))
    return boxes


# ======================================================================================
# partial models for the trap renderer
# ======================================================================================

def lit(boxes):
    for b in boxes:
        b.glow = True
        for face, spec in list(b.faces.items()):
            b.faces[face] = ("#lit", spec[1]) if isinstance(spec, tuple) else "#lit"
    return boxes


def partials():
    """Lit overlays the renderer tints and draws over the off-state lenses, plus the gauge needle,
    as name -> (boxes, origin). Coordinates are those of the trap's own unrotated (north-facing) model minus ``origin``: the small and medium trap's partials are in
    their (lower) block's space, the large trap's in the space of its top north-west block, which
    is 2 blocks above the master (models must stay within -16..32)."""
    out = {}
    out["small_status_led"] = ([Box((6.95, 13.7, 0.45), (9.05, 15.3, 0.5), {"north": ("#lit", [0, 0, 16, 16])})], 0)
    out["medium_status_led"] = ([Box((6.95, 28.2, 0.7), (9.05, 29.8, 0.75), {"north": ("#lit", [0, 0, 16, 16])})], 0)
    out["large_status_led"] = ([
        Box((13.95, 42.2, 0.45), (18.05, 44.05, 0.5), {"north": ("#lit", [0, 0, 16, 16])}),
        Box((13.95, 42.2, 31.5), (18.05, 44.05, 31.55), {"south": ("#lit", [0, 0, 16, 16])}),
    ], 32)
    for i, (side, pos) in enumerate(LARGE_LEDS):
        out["large_led_%d" % i] = ([_led_box(side, pos, 44.7, 45.8, 0.3, "#lit", span=1.05)], 32)
    for boxes, _ in out.values():
        for b in boxes:
            b.glow = True
    # Needle: pivot at (8, 8, 8), pointing up (+Y), in the plane z = 7.9..8. Not full-bright: the
    # renderer gives it the gauge's light and a dark red tint.
    out["penning_gauge_needle"] = ([Box((7.8, 8, 7.9), (8.2, 9.7, 8.0),
                                        {"north": ("#lit", [6, 6, 10, 10]), "south": ("#lit", [6, 6, 10, 10])})], 0)
    return out


# ======================================================================================
# conduit
# ======================================================================================

def conduit_core():
    """The coupler where arms meet: a 5 px steel box with a copper winding round its waist."""
    return [Box((5.5, 5.5, 5.5), (10.5, 10.5, 10.5), all_faces("#node")),
            Box((5.25, 7.25, 5.25), (10.75, 8.75, 10.75), sides("#ring"))]


def conduit_arm():
    """The north arm: a 4 px glass tube from the coupler to the block's edge with two focusing
    rings; inside it, the dark field channel the renderer's pulse travels along (centre 8, 8)."""
    boxes = [Box((6, 6, 0), (10, 10, 5.5), sides("#glass", top="#glass", bottom="#glass", skip=("north", "south")),
                 alpha=CONDUIT_ALPHA, cull=False),
             Box((7.5, 7.5, 0), (8.5, 8.5, 5.5), sides("#channel", top="#channel", bottom="#channel",
                                                       skip=("north", "south")), cull=False)]
    for z in (1, 3.25):
        boxes.append(Box((5.5, 5.5, z), (10.5, 10.5, z + 1), all_faces("#ring")))
    return boxes


def mirror_z(boxes):
    """The same boxes reflected front to back (the north arm made into the south arm)."""
    swap = {"north": "south", "south": "north"}
    out = []
    for b in boxes:
        faces = {swap.get(k, k): v for k, v in b.faces.items()}
        out.append(Box((b.frm[0], b.frm[1], 16 - b.to[2]), (b.to[0], b.to[1], 16 - b.frm[2]), faces,
                       glow=b.glow, alpha=b.alpha, shade=b.shade, cull=b.cull))
    return out


CONDUIT_TEXTURES = {
    "particle": NS + "block/antimatter_conduit_node",
    "node": NS + "block/antimatter_conduit_node",
    "ring": NS + "block/antimatter_conduit_ring",
    "glass": NS + "block/penning_trap_glass",
    "channel": NS + "block/penning_trap_led",
}


# ======================================================================================
# synchrotron, septa, cryo feed, target station, interlock, creative generator
# ======================================================================================

def accelerator_models():
    """Templates and their children. Ring elements: beam along Z (north/south ends), so the
    blockstate turns axis=x by y=90. Septa, station, interlock: model north = FACING."""
    m = {}
    m["accelerator_ring_element"] = {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#side"},
        "elements": [
            {"from": [0, 0, 1], "to": [16, 16, 15], "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#end"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#end"},
                "east": {"uv": [1, 0, 15, 16], "texture": "#side", "cullface": "east"},
                "west": {"uv": [1, 0, 15, 16], "texture": "#side", "cullface": "west"},
                "up": {"uv": [0, 1, 16, 15], "texture": "#top", "cullface": "up"},
                "down": {"uv": [0, 1, 16, 15], "texture": "#top", "cullface": "down"}}},
            {"from": [4, 4, 0], "to": [12, 12, 1], "faces": {
                "north": {"uv": [4, 4, 12, 12], "texture": "#end", "cullface": "north"},
                "east": {"uv": [0, 4, 1, 12], "texture": "#flange"},
                "west": {"uv": [0, 4, 1, 12], "texture": "#flange"},
                "up": {"uv": [4, 0, 12, 1], "texture": "#flange"},
                "down": {"uv": [4, 0, 12, 1], "texture": "#flange"}}},
            {"from": [4, 4, 15], "to": [12, 12, 16], "faces": {
                "south": {"uv": [4, 4, 12, 12], "texture": "#end", "cullface": "south"},
                "east": {"uv": [15, 4, 16, 12], "texture": "#flange"},
                "west": {"uv": [15, 4, 16, 12], "texture": "#flange"},
                "up": {"uv": [4, 15, 12, 16], "texture": "#flange"},
                "down": {"uv": [4, 15, 12, 16], "texture": "#flange"}}},
        ],
    }
    flange = NS + "block/penning_trap_steel"
    for name in ("ring_dipole_magnet", "superconducting_ring_dipole", "superconducting_rf_cavity"):
        m[name] = {"parent": NS + "block/accelerator_ring_element", "textures": {
            "side": NS + "block/" + name + "_side", "end": NS + "block/" + name + "_end",
            "top": NS + "block/" + name + "_top", "flange": flange}}
    for name in ("injection_septum", "extraction_septum"):
        m[name] = {"parent": "minecraft:block/cube", "textures": {
            "particle": NS + "block/" + name + "_side",
            "north": NS + "block/" + name + "_front", "south": NS + "block/" + name + "_back",
            "east": NS + "block/" + name + "_side", "west": NS + "block/" + name + "_side",
            "up": NS + "block/" + name + "_top", "down": NS + "block/" + name + "_top"}}
    m["target_station"] = {"parent": "minecraft:block/cube", "textures": {
        "particle": NS + "block/target_station_side",
        "north": NS + "block/target_station_front", "south": NS + "block/target_station_back",
        "east": NS + "block/target_station_side", "west": NS + "block/target_station_side",
        "up": NS + "block/target_station_top", "down": NS + "block/target_station_side"}}
    m["accelerator_cryo_feed"] = {
        "parent": "minecraft:block/block",
        "textures": {"particle": NS + "block/accelerator_cryo_feed_side",
                     "front": NS + "block/accelerator_cryo_feed_front",
                     "side": NS + "block/accelerator_cryo_feed_side",
                     "top": NS + "block/accelerator_cryo_feed_top"},
        "elements": [
            {"from": [0, 0, 1], "to": [16, 16, 16], "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "south"},
                "east": {"uv": [0, 0, 15, 16], "texture": "#side", "cullface": "east"},
                "west": {"uv": [1, 0, 16, 16], "texture": "#side", "cullface": "west"},
                "up": {"uv": [0, 1, 16, 16], "texture": "#top", "cullface": "up"},
                "down": {"uv": [0, 1, 16, 16], "texture": "#top", "cullface": "down"}}},
            {"from": [2, 2, 0], "to": [14, 14, 1], "faces": {
                "north": {"uv": [2, 2, 14, 14], "texture": "#front", "cullface": "north"},
                "east": {"uv": [0, 2, 1, 14], "texture": "#side"},
                "west": {"uv": [0, 2, 1, 14], "texture": "#side"},
                "up": {"uv": [2, 0, 14, 1], "texture": "#side"},
                "down": {"uv": [2, 0, 14, 1], "texture": "#side"}}},
        ],
    }
    for name, front in (("beam_interlock", "beam_interlock_front"), ("beam_interlock_secured", "beam_interlock_front_secured")):
        m[name] = {
            "parent": "minecraft:block/block",
            "textures": {"particle": NS + "block/beam_interlock_side", "front": NS + "block/" + front,
                         "side": NS + "block/beam_interlock_side"},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#front", "cullface": "north"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "south"},
                    "east": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "east"},
                    "west": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "west"},
                    "up": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "up"},
                    "down": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "down"}}},
                # The status lamp's lens, proud of the panel and full-bright.
                {"from": [6, 12, -0.5], "to": [10, 14, 0], "shade": False,
                 "neoforge_data": dict(GLOW), "faces": {
                    "north": {"uv": [6, 2, 10, 4], "texture": "#front"},
                    "east": {"uv": [6, 2, 6.5, 4], "texture": "#front"},
                    "west": {"uv": [9.5, 2, 10, 4], "texture": "#front"},
                    "up": {"uv": [6, 2, 10, 2.5], "texture": "#front"},
                    "down": {"uv": [6, 3.5, 10, 4], "texture": "#front"}}},
            ],
        }
    m["creative_rf_generator"] = {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {"particle": NS + "block/creative_rf_generator_frame",
                     "frame": NS + "block/creative_rf_generator_frame",
                     "core": NS + "block/creative_rf_generator_core"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                d: {"uv": [0, 0, 16, 16], "texture": "#frame", "cullface": d} for d in FACES}},
            {"from": [3, 3, 3], "to": [13, 13, 13], "shade": False, "neoforge_data": dict(GLOW), "faces": {
                d: {"uv": [3, 3, 13, 13], "texture": "#core"} for d in FACES}},
        ],
    }
    return m


# ======================================================================================
# annihilation generator and antiproton injector
# ======================================================================================

GENERATOR_TEXTURES = {
    "particle": NS + "block/annihilation_generator_side",
    "front": NS + "block/annihilation_generator_front",
    "side": NS + "block/annihilation_generator_side",
    "back": NS + "block/annihilation_generator_back",
    "window": NS + "block/annihilation_generator_window",
    "top": NS + "block/annihilation_generator_top",
    "terminal": NS + "block/annihilation_generator_terminal",
    "bearing": NS + "block/annihilation_generator_bearing",
    "plinth": NS + "block/penning_trap_plinth",
    "plate": NS + "block/penning_trap_plate",
    "coil": NS + "block/penning_trap_coil",
    "band": NS + "block/penning_trap_band_vial",
    "steel": NS + "block/penning_trap_steel",
    "glass": NS + "block/penning_trap_glass",
    "led": NS + "block/penning_trap_led",
}

# Mode lamps on the generator's front, by AnnihilationModel mode ordinal (ROTATION, ELECTRIC,
# SPLIT), left to right as seen from the front (model north faces the viewer, so x runs right to left).
GENERATOR_MODE_LAMPS = (10.25, 7.25, 4.25)
# The core porthole on the back: the opening, x 4..12 and y 5..12; its floor at z 13.5.
GEN_WINDOW = (4, 5, 12, 12)


def annihilation_generator():
    """1x1, model north = the shaft (FACING). An armoured annihilation chamber (y 4..13) between the
    green field heads of its own small trap, on a hazard-striped plinth. Front: the bearing collar
    round the shaft and three mode lamps above it. Back: a flanged porthole onto the core. East and
    west: the FE terminal plates with a red (+) and a black (-) stud. Top: the vial socket."""
    b = []
    b.append(Box((0, 0, 0), (16, 2, 16), sides("#plinth", top="#plate", bottom="#plate")))
    b.append(Box((1.5, 2, 1.5), (14.5, 3.25, 14.5), sides("#coil")))
    b.append(Box((1.25, 3.25, 1.25), (14.75, 4, 14.75), all_faces("#band"), glow=True))
    # The chamber: its south face is the porthole's floor, framed by four flange pieces.
    b.append(Box((1, 4, 1), (15, 13, 13.5), dict(north="#front", south="#window", east="#side", west="#side",
                                                   up="#steel", down="#steel")))
    x0, y0, x1, y1 = GEN_WINDOW
    b.append(Box((1, y1, 13.5), (15, 13, 15), dict(south="#back", east="#side", west="#side", up="#steel", down="#steel")))
    b.append(Box((1, 4, 13.5), (15, y0, 15), dict(south="#back", east="#side", west="#side", up="#steel", down="#steel")))
    b.append(Box((1, y0, 13.5), (x0, y1, 15), dict(south="#back", west="#side", east="#steel")))
    b.append(Box((x1, y0, 13.5), (15, y1, 15), dict(south="#back", east="#side", west="#steel")))
    b.append(Box((x0, y0, 14.5), (x1, y1, 14.5), {"south": ("#glass", [2, 1, 10, 15]), "north": ("#glass", [10, 1, 2, 15])},
                 alpha=GLASS_ALPHA, cull=False))
    # Front: the bearing collar the half shaft turns in, and the mode lamps.
    b.append(Box((4.5, 4.5, 0.5), (11.5, 11.5, 1), dict(sides("#steel", top="#steel", bottom="#steel", skip=("north", "south")),
                                                         north=("#bearing", [0, 0, 16, 16]))))
    for x in GENERATOR_MODE_LAMPS:
        b.append(Box((x, 11.75, 0.5), (x + 1.5, 12.75, 1), dict(sides("#steel", top="#steel", bottom="#steel", skip=("north", "south")),
                                                                 north=("#led", [0, 0, 16, 16]))))
    # Top head.
    b.append(Box((1.25, 13, 1.25), (14.75, 13.75, 14.75), all_faces("#band"), glow=True))
    b.append(Box((1.5, 13.75, 1.5), (14.5, 15, 14.5), sides("#coil")))
    b.append(Box((0.5, 15, 0.5), (15.5, 16, 15.5), sides("#steel", top="#top", bottom="#plate")))
    # FE terminals: a plate and two studs on each side, + (red) on the viewer's left.
    caps = {"red": ("#terminal", [0, 0, 4, 4]), "black": ("#terminal", [4, 0, 8, 4])}
    for east in (True, False):
        px0, px1 = (15, 15.5) if east else (0.5, 1)
        sx0, sx1 = (15.5, 16) if east else (0, 0.5)
        out = "east" if east else "west"
        b.append(Box((px0, 8.5, 4), (px1, 11.5, 12),
                     dict(sides("#steel", top="#steel", bottom="#steel", skip=("east", "west")), **{out: ("#terminal", [0, 5, 16, 11])})))
        # Seen from the east the left is south; from the west it is north.
        red_z, black_z = ((10, 11.5), (4.5, 6)) if east else ((4.5, 6), (10, 11.5))
        for (z0, z1), colour in ((red_z, "red"), (black_z, "black")):
            b.append(Box((sx0, 9.25, z0), (sx1, 10.75, z1), {f: caps[colour] for f in ("north", "south", "up", "down", out)}))
    return b


INJECTOR_TEXTURES = {
    "particle": NS + "block/antiproton_injector_side",
    "front": NS + "block/antiproton_injector_front",
    "side": NS + "block/antiproton_injector_side",
    "back": NS + "block/antiproton_injector_back",
    "top": NS + "block/antiproton_injector_top",
    "chamber": NS + "block/antiproton_injector_chamber",
    "steel": NS + "block/penning_trap_steel",
    "coil": NS + "block/penning_trap_coil",
    "band": NS + "block/penning_trap_band_small",
    "glass": NS + "block/penning_trap_glass",
    "port": NS + "block/penning_trap_port",
    "brass": NS + "block/penning_trap_brass",
    "led": NS + "block/penning_trap_led",
}

# The injector's ARMED and READY lenses, drawn in the frame's top bar (x ranges, y 13..15, on its
# face at z 0; ARMED on the viewer's left).
INJECTOR_ARMED = (9, 12)
INJECTOR_READY = (3, 6)


def antiproton_injector():
    """1x1 tokamak shell block, model north = its outer face (FACING), south = into the plasma. A 3 px
    titanium frame 2 px proud of a 10x10 recess that holds a miniature Penning trap - cyan field bands
    below and above a glass chamber - with the vial port under it; ARMED and READY lamps on the top
    bar (in the frame's texture, lit by the renderer); the beam nozzle on the back."""
    b = []
    b.append(Box((0, 0, 2), (16, 16, 16), dict(north="#front", south="#back", east="#side", west="#side", up="#top", down="#top")))
    b.append(Box((0, 13, 0), (16, 16, 2), dict(north="#front", east="#side", west="#side", up="#top", down="#front")))
    b.append(Box((0, 0, 0), (16, 3, 2), dict(north="#front", east="#side", west="#side", down="#top", up="#front")))
    b.append(Box((13, 3, 0), (16, 13, 2), dict(north="#front", east="#side", west="#front")))
    b.append(Box((0, 3, 0), (3, 13, 2), dict(north="#front", west="#side", east="#front")))
    # The miniature trap in the recess.
    b.append(front_fitting(5, 3.5, 11, 6, 0.75, 1.25, "#port", "#brass"))
    b.append(Box((4.5, 6.25, 0.75), (11.5, 7, 2), sides("#coil", bottom="#steel")))
    b.append(Box((4.25, 7, 0.5), (11.75, 7.5, 2), all_faces("#band"), glow=True))
    b.append(Box((4.5, 7.5, 1.9), (11.5, 11, 1.9), {"north": ("#chamber", [0, 0, 16, 16])}, cull=False))
    for x in (4.5, 11):
        b.append(Box((x, 7.5, 0.75), (x + 0.5, 11, 2), sides("#steel", skip=("south",))))
    b.append(Box((5, 7.5, 0.75), (11, 11, 0.75), {"north": ("#glass", [2, 1, 10, 15]), "south": ("#glass", [10, 1, 2, 15])},
                 alpha=GLASS_ALPHA, cull=False))
    b.append(Box((4.25, 11, 0.5), (11.75, 11.5, 2), all_faces("#band"), glow=True))
    b.append(Box((4.5, 11.5, 0.75), (11.5, 12.25, 2), sides("#coil", top="#steel")))
    return b


def machine_partials():
    """Lit overlays for the generator and injector renderers: name -> (boxes, textures). In the
    block's own unrotated (north-facing) space; drawn full-bright, white so the vertex colour tints."""
    lit = {"particle": NS + "block/penning_trap_led_lit", "lit": NS + "block/penning_trap_led_lit"}
    out = {}
    for i, x in enumerate(GENERATOR_MODE_LAMPS):
        out["generator_mode_led_%d" % i] = ([Box((x - 0.05, 11.7, 0.45), (x + 1.55, 12.8, 0.5),
                                                  {"north": ("#lit", [0, 0, 16, 16])})], lit)
    x0, y0, x1, y1 = GEN_WINDOW
    out["generator_core"] = ([Box((x0, y0, 13.55), (x1, y1, 13.55), {"south": ("#core", [0, 0, 16, 16])})],
                             {"particle": NS + "block/annihilation_generator_core", "core": NS + "block/annihilation_generator_core"})
    for name, (a, c) in (("injector_armed_led", INJECTOR_ARMED), ("injector_ready_led", INJECTOR_READY)):
        out[name] = ([Box((a, 13, -0.05), (c, 15, -0.05), {"north": ("#lit", [0, 0, 16, 16])})], lit)
    out["injector_nozzle_flash"] = ([Box((2.5, 2.5, 16.05), (13.5, 13.5, 16.05), {"south": ("#flash", [0, 0, 16, 16])})],
                                    {"particle": NS + "block/antiproton_injector_flash", "flash": NS + "block/antiproton_injector_flash"})
    for boxes, _ in out.values():
        for b in boxes:
            b.glow = True
    return out


# ======================================================================================
# isotopes: gamma irradiator, smoke detector, reactor irradiation channel
# ======================================================================================

IRRADIATOR_TEXTURES = {
    "particle": NS + "block/gamma_irradiator_head",
    "head": NS + "block/gamma_irradiator_head",
    "head_plain": NS + "block/gamma_irradiator_head_plain",
    "plate": NS + "block/gamma_irradiator_plate",
    "plate_side": NS + "block/gamma_irradiator_plate_side",
    "plate_bottom": NS + "block/gamma_irradiator_plate_bottom",
    "nozzle": NS + "block/gamma_irradiator_nozzle",
    "shutter": NS + "block/gamma_irradiator_shutter",
    "steel": NS + "block/penning_trap_steel",
    "led": NS + "block/penning_trap_led",
}

# The irradiator's shutter leaves, closed: each covers half of the nozzle's 4x4 aperture (x/z 6..10)
# under the nozzle (y 1); the renderer slides them out along X by up to IRRADIATOR_SHUTTER_TRAVEL.
IRRADIATOR_SHUTTERS = {"west": (5, 8), "east": (8, 11)}
IRRADIATOR_SHUTTER_Y = (0.5, 1)
IRRADIATOR_SHUTTER_TRAVEL = 3
# The status lamps, one on each edge of the mounting plate (y 13..16), centred.
IRRADIATOR_LAMP_Y = (13.75, 15.25)


def _irradiator_lamp(side, depth, tex, glow=False):
    y0, y1 = IRRADIATOR_LAMP_Y
    face = (tex, [0, 0, 16, 16])
    if side == "north":
        return Box((7, y0, -depth), (9, y1, 0), {"north": face}, glow=glow, cull=False)
    if side == "south":
        return Box((7, y0, 16), (9, y1, 16 + depth), {"south": face}, glow=glow, cull=False)
    if side == "west":
        return Box((-depth, y0, 7), (0, y1, 9), {"west": face}, glow=glow, cull=False)
    return Box((16, y0, 7), (16 + depth, y1, 9), {"east": face}, glow=glow, cull=False)


def gamma_irradiator(with_shutters=False):
    """1x1, the same from every side. Hung two blocks above a belt or depot (where Create's belt
    processing looks, like the press): a bolted steel mounting plate (y 13..16) on four corner legs
    with foot pads, the ribbed lead head (x/z 3..13, y 3..13; the radiation label north and south)
    under the plate, and the collimator
    nozzle (x/z 5..11, y 1..3) whose 4x4 aperture the two shutter leaves (partials) close from
    below. ``with_shutters`` adds the closed leaves (the item model). The status lamp lenses stand
    proud of the block, so they come from ``irradiator_lamps`` (not clipped to the cell)."""
    b = []
    b.append(Box((0, 13, 0), (16, 16, 16), sides("#plate_side", top="#plate", bottom="#plate_bottom")))
    for x0, z0 in ((1, 1), (13, 1), (1, 13), (13, 13)):
        b.append(Box((x0, 1, z0), (x0 + 2, 13, z0 + 2), sides("#steel")))
        b.append(Box((x0 - 0.5, 0, z0 - 0.5), (x0 + 2.5, 1, z0 + 2.5), all_faces("#steel")))
    b.append(Box((3, 3, 3), (13, 13, 13), dict(north="#head", south="#head", west="#head_plain", east="#head_plain",
                                                down="#plate_bottom")))
    b.append(Box((5, 1, 5), (11, 3, 11), sides("#nozzle", bottom="#nozzle")))
    if with_shutters:
        for x0, x1 in IRRADIATOR_SHUTTERS.values():
            b.append(Box((x0, IRRADIATOR_SHUTTER_Y[0], 5), (x1, IRRADIATOR_SHUTTER_Y[1], 11), all_faces("#shutter")))
    return b


def irradiator_elements(with_shutters=False):
    els = slice_boxes(gamma_irradiator(with_shutters), (0, 0, 0))
    return els + _whole([_irradiator_lamp(side, 0.25, "#led") for side in ("north", "south", "west", "east")])


def irradiator_partials():
    """The irradiator renderer's pieces, in the block's own space: name -> (boxes, textures).

    * ``shutter_west`` / ``shutter_east``: the two leaves, closed; the renderer translates them by
      -/+ open * IRRADIATOR_SHUTTER_TRAVEL px along X (open 0..1).
    * ``glow``: the aperture under the nozzle (y 0.98, facing down), full-bright Cherenkov blue,
      drawn while the shutter is open, brighter with the source's activity.
    * ``beam``: the gamma cone, the four sides of a 4x4 column from y 0 down to y -16 (one block),
      for an additive render type; the renderer stretches it in Y to reach the belt or depot.
    * ``lamp``: the four status lenses, white so the vertex colour sets their hue."""
    out = {}
    tex = {"particle": NS + "block/gamma_irradiator_shutter", "shutter": NS + "block/gamma_irradiator_shutter"}
    for name, (x0, x1) in IRRADIATOR_SHUTTERS.items():
        out["shutter_" + name] = ([Box((x0, IRRADIATOR_SHUTTER_Y[0], 5), (x1, IRRADIATOR_SHUTTER_Y[1], 11),
                                       {f: ("#shutter", [0, 0, 16, 16] if f in ("up", "down") else [0, 0, 16, 2])
                                        for f in FACES})], tex)
    glow = Box((6, 0.98, 6), (10, 0.98, 10), {"down": ("#glow", [0, 0, 16, 16])}, glow=True)
    out["glow"] = ([glow], {"particle": NS + "block/gamma_irradiator_glow", "glow": NS + "block/gamma_irradiator_glow"})
    beam = Box((6, -16, 6), (10, 0, 10), {f: ("#beam", [0, 0, 16, 16]) for f in ("north", "south", "west", "east")},
               glow=True)
    out["beam"] = ([beam], {"particle": NS + "block/gamma_irradiator_beam", "beam": NS + "block/gamma_irradiator_beam"})
    lamps = [_irradiator_lamp(side, 0.3, "#lit", glow=True) for side in ("north", "south", "west", "east")]
    out["lamp"] = (lamps, {"particle": NS + "block/penning_trap_led_lit", "lit": NS + "block/penning_trap_led_lit"})
    return out


SMOKE_TEXTURES = {
    "particle": NS + "block/smoke_detector_face",
    "face": NS + "block/smoke_detector_face",
    "side": NS + "block/smoke_detector_side",
    "base": NS + "block/smoke_detector_base",
    "led": NS + "block/smoke_detector_led",
}


def smoke_detector(alarm=False):
    """The floor model (the blockstate turns it onto walls and ceilings like a button): a moulded
    disc 10 px across and 2.5 px tall lying on y 0, its corners stepped off, the test button in the
    middle of the face and the status LED beside it. ``alarm`` lights the LED (full-bright)."""
    b = []
    h = 2.5
    b.append(Box((4, 0, 4), (12, h, 12), {"up": "#face", "down": "#base"}))
    b.append(Box((3, 0, 4), (4, h, 12), {"west": "#side", "up": "#face", "down": "#base"}))
    b.append(Box((12, 0, 4), (13, h, 12), {"east": "#side", "up": "#face", "down": "#base"}))
    b.append(Box((4, 0, 3), (12, h, 4), {"north": "#side", "up": "#face", "down": "#base"}))
    b.append(Box((4, 0, 12), (12, h, 13), {"south": "#side", "up": "#face", "down": "#base"}))
    b.append(Box((7, h, 7), (9, h + 0.5, 9), sides("#face", top="#face")))
    b.append(Box((10, h, 10), (11, h + 0.5, 11), sides("#led", top="#led"), glow=alarm))
    return b


CHANNEL_TEXTURES = {
    "particle": NS + "block/reactor_casing",
    "side": NS + "block/reactor_casing",
    "front": NS + "block/reactor_irradiation_channel_front",
}


def irradiation_channel(front, overlay=None):
    """A reactor wall part like the fuel port: a full cube of reactor casing, model north = FACING
    (the face outside the reactor). ``overlay``: a full-bright cutout sheet just in front of it."""
    faces = all_faces("#side")
    faces["north"] = "#front"
    els = slice_boxes([Box((0, 0, 0), (16, 16, 16), faces)], (0, 0, 0))
    textures = dict(CHANNEL_TEXTURES, front=NS + "block/" + front)
    data = block_model(textures, els, translucent=False)
    if overlay:
        textures["overlay"] = NS + "block/" + overlay
        sheet = Box((0, 0, -0.01), (16, 16, -0.01), {"north": ("#overlay", [0, 0, 16, 16])}, glow=True)
        el = _whole([sheet])[0]
        el["faces"]["north"]["cullface"] = "north"
        data["elements"].append(el)
        data["render_type"] = "minecraft:cutout"
    return data


def isotope_models():
    """Block models, item models and partials of the isotope content: relative path -> JSON."""
    m = {}
    m["block/gamma_irradiator.json"] = block_model(IRRADIATOR_TEXTURES, irradiator_elements(), translucent=False)
    m["item/gamma_irradiator.json"] = block_model(IRRADIATOR_TEXTURES,
                                                  transform(irradiator_elements(True), (0, 0, 0), 1, (0, 0, 0)),
                                                  translucent=False)
    for name, (boxes, textures) in sorted(irradiator_partials().items()):
        m["block/gamma_irradiator/partial/%s.json" % name] = block_model(textures, _whole(boxes), translucent=False)
    m["block/smoke_detector.json"] = block_model(SMOKE_TEXTURES, slice_boxes(smoke_detector(), (0, 0, 0)), translucent=False)
    m["block/smoke_detector_alarm.json"] = block_model(dict(SMOKE_TEXTURES, led=NS + "block/smoke_detector_led_alarm"),
                                                       slice_boxes(smoke_detector(True), (0, 0, 0)), translucent=False)
    m["item/smoke_detector.json"] = {
        "parent": NS + "block/smoke_detector",
        "display": {
            "gui": {"rotation": [40, 225, 0], "translation": [0, 3, 0], "scale": [0.95] * 3},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4] * 3},
            "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, -7], "scale": [0.9] * 3},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5] * 3},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.6] * 3},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [0.6] * 3},
        },
    }
    m["block/reactor_irradiation_channel.json"] = irradiation_channel("reactor_irradiation_channel_front")
    m["block/reactor_irradiation_channel_loaded.json"] = irradiation_channel("reactor_irradiation_channel_front_loaded")
    m["block/reactor_irradiation_channel_irradiating.json"] = irradiation_channel(
        "reactor_irradiation_channel_front_loaded", "reactor_irradiation_channel_glow")
    m["block/reactor_irradiation_channel_ready.json"] = irradiation_channel(
        "reactor_irradiation_channel_front_loaded", "reactor_irradiation_channel_ready")
    m["item/reactor_irradiation_channel.json"] = {"parent": NS + "block/reactor_irradiation_channel"}
    return m


# ======================================================================================
# processing machines: gas centrifuge and electrolyzer
# ======================================================================================

# The centrifuge's body, north-facing (front z 0, back z 16), in pixels. A Create-style C frame: a 2 px plinth and a 6 px
# head held by a 4 px gearbox plate across the back; the open front and sides show the rotor spinning between them (front
# posts would hide it from the usual north-west view).
PM_BASE_TOP = 2
PM_HEAD_BOTTOM = 10
PM_BACK = 12
# Rotor: a rounded 7 px column (three crossed boxes) turning about the vertical axis x = z = 8; swept radius 3.81 px.
PM_ROTOR_Y = (PM_BASE_TOP, PM_HEAD_BOTTOM)
# Gauge on the head front (left as seen from the front): dial recess x 9..14, y 10.5..15.5, 0.5 deep; the needle pivots
# on its centre and points up in the model.
PM_DIAL = (9, 10.5, 14, 15.5)
PM_DIAL_DEPTH = 0.5
PM_NEEDLE_PIVOT = (11.5, 13, 0.35)
# Product port recess (right as seen from the front) and the side pockets that hold the input flange and the cascade
# manifold coupling.
PM_PORT = (2, 10.5, 7, 15.5)
# The recess floors sit on half pixels, so they take whole texels from a part of the front texture the model never shows
# (the open chamber).
PM_DIAL_UV = [3, 7, 8, 12]
PM_PORT_UV = [8, 7, 13, 12]
PM_POCKET = (8, 12, 12, 16)   # z0, y0, z1, y1 (open at the top, against the gearbox plate)
PM_POCKET_DEPTH = 2
# Shaft hole in the gearbox plate and the shaft stub in it (4x4 like Create's shaft).
PM_HOLE = (5, 5, 11, 11)
PM_STUB_Z = 13


def _csg(solid, holes, outer, default_carved="#inner"):
    """Boxes of ``solid`` minus ``holes``, each face textured: faces on the solid's own boundary take ``outer[dir]``,
    faces cut by a hole take that hole's ``faces[dir]`` (else ``default_carved``), ``dir`` being the direction the face
    looks (a recess floor facing north is ``north``). ``holes`` are (frm, to, faces).
    The solid is cut on every coordinate of the holes into cells, and cells with equal faces are merged back into boxes,
    so the faces never change texture mid-box."""
    (sx0, sy0, sz0), (sx1, sy1, sz1) = solid
    cuts = []
    for axis, (a, b) in enumerate(((sx0, sx1), (sy0, sy1), (sz0, sz1))):
        vals = {a, b}
        for frm, to, _ in holes:
            for v in (frm[axis], to[axis]):
                if a < v < b:
                    vals.add(v)
        cuts.append(sorted(vals))
    nx, ny, nz = (len(c) - 1 for c in cuts)

    def centre(i, j, k):
        return ((cuts[0][i] + cuts[0][i + 1]) / 2, (cuts[1][j] + cuts[1][j + 1]) / 2, (cuts[2][k] + cuts[2][k + 1]) / 2)

    def hole_at(p):
        for frm, to, faces in holes:
            if all(frm[a] < p[a] < to[a] for a in range(3)):
                return faces
        return None

    solid_cell = {}
    for i in range(nx):
        for j in range(ny):
            for k in range(nz):
                solid_cell[(i, j, k)] = hole_at(centre(i, j, k)) is None
    step = {"west": (-1, 0, 0), "east": (1, 0, 0), "down": (0, -1, 0), "up": (0, 1, 0), "north": (0, 0, -1), "south": (0, 0, 1)}

    def face_tex(i, j, k, d):
        di, dj, dk = step[d]
        n = (i + di, j + dj, k + dk)
        if n in solid_cell:
            if solid_cell[n]:
                return None
            faces = hole_at(centre(*n))
            return faces.get(d, default_carved)
        return outer.get(d)

    sig = {}
    for c, s in solid_cell.items():
        if s:
            sig[c] = {d: face_tex(*c, d) for d in step}
    # boxes: [lo index (i,j,k), hi index exclusive, faces]
    boxes = []
    for (i, j, k) in sorted(sig, key=lambda c: (c[1], c[2], c[0])):
        boxes.append([[i, j, k], [i + 1, j + 1, k + 1], dict(sig[(i, j, k)])])
    pairs = ((0, "west", "east"), (2, "north", "south"), (1, "down", "up"))
    for axis, lo_face, hi_face in pairs:
        merged = True
        while merged:
            merged = False
            index = {tuple(b[0]): b for b in boxes}
            for b in list(boxes):
                if b not in boxes:
                    continue
                nxt = list(b[0])
                nxt[axis] = b[1][axis]
                o = index.get(tuple(nxt))
                if o is None or o is b or o not in boxes:
                    continue
                same_extent = all(o[0][a] == b[0][a] and o[1][a] == b[1][a] for a in range(3) if a != axis)
                if not same_extent or b[2][hi_face] is not None or o[2][lo_face] is not None:
                    continue
                if any(b[2][d] != o[2][d] for d in b[2] if d not in (lo_face, hi_face)):
                    continue
                b[1][axis] = o[1][axis]
                b[2][hi_face] = o[2][hi_face]
                boxes.remove(o)
                index = {tuple(x[0]): x for x in boxes}
                merged = True
    out = []
    for lo, hi, faces in boxes:
        frm = [cuts[a][lo[a]] for a in range(3)]
        to = [cuts[a][hi[a]] for a in range(3)]
        f = {}
        for d, tex in faces.items():
            if tex is None:
                continue
            if d == "east" and not isinstance(tex, tuple) and tex in PM_MIRROR_EAST:
                # The east side shows the side texture mirrored, so the front post and the gearbox plate keep their
                # places (u = z, like the west side).
                f[d] = (tex, [to[2], 16 - to[1], frm[2], 16 - frm[1]])
            else:
                f[d] = tex
        if f:
            out.append(Box(frm, to, f))
    return out


PM_MIRROR_EAST = ("#side",)


def pm_body():
    """The static body, north-facing. Nothing enters the rotor's swept cylinder (radius 3.81 about x = z = 8 from
    y 2 to 10) or the shaft stub's (radius 2*sqrt(2) about x = y = 8 from z 13)."""
    outer = {"north": "#front", "south": "#back", "west": "#side", "east": "#side", "up": "#top", "down": "#bottom"}
    b, h = PM_BASE_TOP, PM_HEAD_BOTTOM
    chamber = {"up": "#chamber", "down": "#chamber", "north": "#wall"}
    dx0, dy0, dx1, dy1 = PM_DIAL
    px0, py0, px1, py1 = PM_PORT
    kz0, ky0, kz1, ky1 = PM_POCKET
    hx0, hy0, hx1, hy1 = PM_HOLE
    holes = [
        # the rotor chamber, open to the north, west and east
        ((-1, b, -1), (17, h, PM_BACK), chamber),
        # gauge and product-port recesses: their floors show the front texture (dial, flange)
        ((dx0, dy0, -1), (dx1, dy1, PM_DIAL_DEPTH), {"north": ("#front", PM_DIAL_UV)}),
        ((px0, py0, -1), (px1, py1, PM_DIAL_DEPTH), {"north": ("#front", PM_PORT_UV)}),
        # side pockets, open at the top: their floors show the side texture (input flange), mirrored on the east
        ((-1, ky0, kz0), (PM_POCKET_DEPTH, 17, kz1), {"west": "#side"}),
        ((16 - PM_POCKET_DEPTH, ky0, kz0), (17, 17, kz1), {"east": "#side"}),
        # shaft hole: brass collar walls
        ((hx0, hy0, PM_STUB_Z), (hx1, hy1, 17), {d: "#collar" for d in ("west", "east", "up", "down")}),
    ]
    return _csg(((0, 0, 0), (16, 16, 16)), holes, outer)


def pm_rotor():
    """The rotor partial: a 7 px column, three crossed boxes (7x3, 5x5, 3x7; swept radius 3.81 px), from y 2 to 10,
    centred on x = z = 8. Its texture is the drum unrolled, one texel per pixel of width: u 0..7 the marked north
    quarter, 7..14 east and west, 9..16 south, so the marking strobes past the windows as it turns; v follows the height
    (v = 16 - y)."""
    y0, y1 = PM_ROTOR_Y
    v = [16 - y1, 16 - y0]

    def uv(u0, u1):
        return ("#rotor", [u0, v[0], u1, v[1]])

    boxes = []
    for half_x, half_z in ((3.5, 1.5), (2.5, 2.5), (1.5, 3.5)):
        n0 = 3.5 - half_x       # where this box's north face starts in the quarter
        e0 = 3.5 - half_z
        boxes.append(Box((8 - half_x, y0, 8 - half_z), (8 + half_x, y1, 8 + half_z), {
            "north": uv(n0, 7 - n0), "south": uv(9 + n0, 16 - n0),
            "west": uv(7 + e0, 14 - e0), "east": uv(7 + e0, 14 - e0)}))
    return boxes


def pm_shaft_stub():
    """A Create shaft piece (x/y 6..10) from inside the gearbox (z 13) to the back face, turning about x = y = 8."""
    return [Box((6, 6, PM_STUB_Z), (10, 10, 16), {
        "north": ("#axis_top", [6, 6, 10, 10]), "south": ("#axis_top", [6, 6, 10, 10]),
        "east": ("#axis", [6, 0, 10, 16 - PM_STUB_Z]), "west": ("#axis", [6, 0, 10, 16 - PM_STUB_Z]),
        "up": ("#axis", [6, 0, 10, 16 - PM_STUB_Z]), "down": ("#axis", [6, 0, 10, 16 - PM_STUB_Z])})]


# Create's shaft texture runs along v; the stub runs along z, so its long faces turn it like Create's shaft_half.
PM_STUB_FACE_ROT = {"north": 180, "east": 270, "west": 90, "up": 180}


def pm_needle():
    """The gauge needle partial, pointing straight up from PM_NEEDLE_PIVOT, just in front of the dial."""
    cx, cy, _ = PM_NEEDLE_PIVOT
    z0, z1 = 0.2, 0.45
    return [Box((cx - 0.25, cy - 0.25, z0), (cx + 0.25, cy + 2.0, z1),
                {"north": ("#needle", [7, 2, 9, 10]), "west": ("#needle", [7, 2, 8, 10]), "east": ("#needle", [8, 2, 9, 10]),
                 "up": ("#needle", [7, 1, 9, 2])}),
            Box((cx - 0.5, cy - 0.5, 0.15), (cx + 0.5, cy + 0.5, z1),
                {"north": ("#needle", [6, 12, 10, 16]), "west": ("#needle", [6, 12, 10, 13]), "east": ("#needle", [6, 12, 10, 13]),
                 "up": ("#needle", [6, 12, 10, 13]), "down": ("#needle", [6, 12, 10, 13])})]


def pm_manifold(west=True):
    """The cascade coupling on one side (west: x 0..2), in the side pocket of the head, which is open at the top: a 3 px
    header stub from the pocket's input flange to the block edge, ending in half of a bolted flange, so the west piece
    of one centrifuge and the east piece of the next meet as one joint, seen from above between the two heads. Static,
    drawn by the blockstate when the neighbour is a centrifuge facing the same way."""
    kz0, ky0, kz1, ky1 = PM_POCKET
    cy, cz = (ky0 + ky1) / 2, (kz0 + kz1) / 2
    pipe = Box((0.75, cy - 1.5, cz - 1.5), (PM_POCKET_DEPTH, cy + 1.5, cz + 1.5), {
        "north": ("#manifold", [0, 0, 1.25, 3]), "south": ("#manifold", [0, 0, 1.25, 3]),
        "up": ("#manifold", [0, 0, 1.25, 3]), "down": ("#manifold", [0, 0, 1.25, 3])})
    collar = Box((0, cy - 1.75, cz - 1.75), (0.75, cy + 1.75, cz + 1.75), {
        "east": ("#manifold", [0, 6, 3.5, 9.5]),
        "north": ("#manifold", [6, 6, 6.75, 9.5]), "south": ("#manifold", [6, 6, 6.75, 9.5]),
        "up": ("#manifold", [8, 6, 8.75, 9.5]), "down": ("#manifold", [8, 6, 8.75, 9.5])})
    boxes = [pipe, collar]
    if west:
        return boxes
    swap = {"west": "east", "east": "west"}
    out = []
    for bx in boxes:
        out.append(Box((16 - bx.to[0], bx.frm[1], bx.frm[2]), (16 - bx.frm[0], bx.to[1], bx.to[2]),
                       {swap.get(k, k): v for k, v in bx.faces.items()}))
    return out


def pm_textures(prefix):
    """Body textures: each machine's own faces; the recess walls and the brass hole collar are shared."""
    t = {name: NS + "block/%s_%s" % (prefix, name) for name in ("front", "side", "back", "top", "bottom", "chamber", "wall")}
    t["inner"] = NS + "block/centrifuge_inner"
    t["collar"] = NS + "block/centrifuge_collar"
    t["particle"] = t["side"]
    return t


PM_ROTOR_TEXTURES = {"particle": NS + "block/centrifuge_rotor", "rotor": NS + "block/centrifuge_rotor"}
PM_STUB_TEXTURES = {"particle": "create:block/axis", "axis": "create:block/axis", "axis_top": "create:block/axis_top"}
PM_NEEDLE_TEXTURES = {"particle": NS + "block/centrifuge_needle", "needle": NS + "block/centrifuge_needle"}
PM_MANIFOLD_TEXTURES = {"particle": NS + "block/centrifuge_manifold", "manifold": NS + "block/centrifuge_manifold"}


def _stub_elements():
    els = _whole(pm_shaft_stub())
    for el in els:
        for face, data in el["faces"].items():
            if face in PM_STUB_FACE_ROT:
                data["rotation"] = PM_STUB_FACE_ROT[face]
    return els


def processing_machines():
    """Block, partial and item models of the centrifuge and its electrolyzer variant: relative path -> JSON."""
    m = {}
    body = slice_boxes(pm_body(), (0, 0, 0))
    m["block/centrifuge/body.json"] = block_model(pm_textures("centrifuge"), body, translucent=False)
    m["block/centrifuge/rotor.json"] = block_model(PM_ROTOR_TEXTURES, _whole(pm_rotor()), translucent=False)
    m["block/centrifuge/shaft_stub.json"] = block_model(PM_STUB_TEXTURES, _stub_elements(), translucent=False)
    m["block/centrifuge/needle.json"] = block_model(PM_NEEDLE_TEXTURES, _whole(pm_needle()), translucent=False)
    m["block/centrifuge/manifold_west.json"] = block_model(PM_MANIFOLD_TEXTURES, _whole(pm_manifold(True)), translucent=False)
    m["block/centrifuge/manifold_east.json"] = block_model(PM_MANIFOLD_TEXTURES, _whole(pm_manifold(False)), translucent=False)
    item_textures = dict(pm_textures("centrifuge"), rotor=PM_ROTOR_TEXTURES["rotor"], needle=PM_NEEDLE_TEXTURES["needle"],
                         axis=PM_STUB_TEXTURES["axis"], axis_top=PM_STUB_TEXTURES["axis_top"])
    item_elements = transform(body, (0, 0, 0), 1, (0, 0, 0)) + _whole(pm_rotor()) + _stub_elements() + _whole(pm_needle())
    m["item/centrifuge.json"] = dict(block_model(item_textures, item_elements, translucent=False),
                                     display=display(0.625, 0.25, 0.5, hand=0.4, third=0.375, lift=0))
    # The electrolyzer: the same geometry, its own textures.
    def el(name):
        return NS + "block/electrolyzer_" + name
    m["block/electrolyzer/body.json"] = {"parent": NS + "block/centrifuge/body", "textures": pm_textures("electrolyzer")}
    m["block/electrolyzer/rotor.json"] = {"parent": NS + "block/centrifuge/rotor",
                                          "textures": {"particle": el("rotor"), "rotor": el("rotor")}}
    m["block/electrolyzer/shaft_stub.json"] = {"parent": NS + "block/centrifuge/shaft_stub"}
    m["block/electrolyzer/needle.json"] = {"parent": NS + "block/centrifuge/needle"}
    m["item/electrolyzer.json"] = {"parent": NS + "item/centrifuge",
                                   "textures": dict(pm_textures("electrolyzer"), rotor=el("rotor"))}
    return m


# ======================================================================================
# output
# ======================================================================================

def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def block_model(textures, elements, translucent=True, parent="minecraft:block/block"):
    data = {"parent": parent}
    if translucent:
        data["render_type"] = "minecraft:translucent"
    data["textures"] = textures
    data["elements"] = elements
    return data


def display(gui_scale=0.625, ground=0.25, fixed=0.5, hand=0.4, third=0.375, lift=1):
    """Item transforms. ``hand`` and ``third`` scale the first- and third-person views; a trap shrunk
    from a multiblock is tall and reads far bigger in the hand than a cube at vanilla's 0.4/0.375.
    ``lift`` raises it in the first-person hand, so a tall trap shows whole above the hotbar."""
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [gui_scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [ground] * 3},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [fixed] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [third] * 3},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [third] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, lift, 0], "scale": [hand] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, lift, 0], "scale": [hand] * 3},
        "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=DEFAULT_OUT)
    args = ap.parse_args()
    blocks = os.path.join(args.out, "models", "block")
    items = os.path.join(args.out, "models", "item")
    written = []

    def put(path, data):
        write(path, data)
        written.append(path)

    # Small trap.
    small = slice_boxes(small_trap(), (0, 0, 0))
    put(os.path.join(blocks, "small_penning_trap.json"), block_model(trap_textures("small"), small))
    put(os.path.join(items, "small_penning_trap.json"), {"parent": NS + "block/small_penning_trap",
                                                          "display": display(0.625, 0.25, 0.5, hand=0.3, third=0.3, lift=3)})

    # Medium trap: two halves; the item is both, scaled to one block.
    med = medium_trap()
    item_elements = []
    for half, y in (("lower", 0), ("upper", 16)):
        els = slice_boxes(med, (0, y, 0))
        put(os.path.join(blocks, "penning_trap", half + ".json"), block_model(trap_textures("medium"), els))
        item_elements += transform(els, (0, y, 0), 0.5, (4, 0, 4))
    put(os.path.join(items, "penning_trap.json"), dict(block_model(trap_textures("medium"), item_elements),
                                                         display=display(0.75, 0.3, 0.6, hand=0.3, third=0.3, lift=3.5)))

    # Large trap: twelve slices; the item is all of them scaled by a third.
    large = large_trap()
    item_elements = []
    s = 1.0 / 3.0
    off = (16 - 32 * s) / 2
    for level, y in (("bottom", 0), ("middle", 16), ("top", 32)):
        for corner, (x, z) in (("nw", (0, 0)), ("ne", (16, 0)), ("sw", (0, 16)), ("se", (16, 16))):
            els = slice_boxes(large, (x, y, z))
            put(os.path.join(blocks, "large_penning_trap", "%s_%s.json" % (level, corner)),
                block_model(trap_textures("large"), els))
            item_elements += transform(els, (x, y, z), s, (off, 0, off))
    put(os.path.join(items, "large_penning_trap.json"), dict(block_model(trap_textures("large"), item_elements),
                                                               display=display(0.75, 0.3, 0.6, hand=0.3, third=0.3, lift=3.5)))

    # Renderer partials.
    tex = {"particle": NS + "block/penning_trap_led_lit", "lit": NS + "block/penning_trap_led_lit"}
    for name, (boxes, origin_y) in sorted(partials().items()):
        put(os.path.join(blocks, "antimatter", "partial", name + ".json"),
            block_model(tex, _whole(boxes, origin_y), translucent=False))

    # Conduit.
    put(os.path.join(blocks, "antimatter_conduit", "core.json"),
        block_model(CONDUIT_TEXTURES, slice_boxes(conduit_core(), (0, 0, 0))))
    put(os.path.join(blocks, "antimatter_conduit", "arm.json"),
        block_model(CONDUIT_TEXTURES, slice_boxes(conduit_arm(), (0, 0, 0))))
    put(os.path.join(items, "antimatter_conduit.json"),
        dict(block_model(CONDUIT_TEXTURES, transform(slice_boxes(conduit_core() + conduit_arm() + mirror_z(conduit_arm()),
                                                                 (0, 0, 0)), (0, 0, 0), 1, (0, 0, 0))),
             display=display(0.8, 0.3, 0.7, hand=0.4, third=0.35, lift=3)))

    # Annihilation generator and antiproton injector: the blocks, their items and the renderers' partials.
    put(os.path.join(blocks, "annihilation_generator.json"),
        block_model(GENERATOR_TEXTURES, slice_boxes(annihilation_generator(), (0, 0, 0))))
    put(os.path.join(items, "annihilation_generator.json"), {"parent": NS + "block/annihilation_generator"})
    put(os.path.join(blocks, "antiproton_injector.json"),
        block_model(INJECTOR_TEXTURES, slice_boxes(antiproton_injector(), (0, 0, 0))))
    put(os.path.join(items, "antiproton_injector.json"), {"parent": NS + "block/antiproton_injector"})
    for name, (boxes, textures) in sorted(machine_partials().items()):
        put(os.path.join(blocks, "antimatter", "partial", name + ".json"),
            block_model(textures, _whole(boxes), translucent=False))

    # Vial item: fill steps and the blinking low-battery warning drawn full-bright on top.
    def vial(tex, warn=False):
        data = {"parent": "minecraft:item/generated", "textures": {"layer0": NS + "item/" + tex}}
        if warn:
            data["loader"] = "neoforge:item_layers"
            data["textures"]["layer1"] = NS + "item/antimatter_vial_warning"
            data["neoforge_data"] = {"layers": {"1": {"block_light": 15, "sky_light": 15}}}
        return data

    for level in (1, 2, 3):
        put(os.path.join(items, "antimatter_vial_%d.json" % level), vial("antimatter_vial_%d" % level))
        put(os.path.join(items, "antimatter_vial_%d_low.json" % level), vial("antimatter_vial_%d" % level, True))
    root = vial("antimatter_vial")
    overrides = []
    for level, fill in ((1, 0.33), (2, 0.66), (3, 1.0)):
        overrides.append({"predicate": {NS + "fill": fill}, "model": NS + "item/antimatter_vial_%d" % level})
    for level, fill in ((1, 0.33), (2, 0.66), (3, 1.0)):
        overrides.append({"predicate": {NS + "fill": fill, NS + "low_battery": 1},
                          "model": NS + "item/antimatter_vial_%d_low" % level})
    root["overrides"] = overrides
    put(os.path.join(items, "antimatter_vial.json"), root)

    # Accelerator and energy blocks.
    for name, data in accelerator_models().items():
        put(os.path.join(blocks, name + ".json"), data)

    # Isotopes: gamma irradiator (and its partials), smoke detector, reactor irradiation channel.
    for rel, data in sorted(isotope_models().items()):
        put(os.path.join(args.out, "models", *rel.split("/")), data)

    # Gas centrifuge and electrolyzer: body, partials, cascade manifold and items.
    for rel, data in sorted(processing_machines().items()):
        put(os.path.join(args.out, "models", *rel.split("/")), data)

    print("wrote %d models -> %s" % (len(written), args.out))


def _whole(boxes, origin_y=0):
    """Boxes emitted unclipped (partials), moved down by ``origin_y``; explicit UVs only."""
    out = []
    for b in boxes:
        b._offset = (0, origin_y, 0)
        lo = [b.frm[0], b.frm[1] - origin_y, b.frm[2]]
        hi = [b.to[0], b.to[1] - origin_y, b.to[2]]
        el = _element(b, lo, hi, None, set(b.faces))
        for face in el["faces"].values():
            face.pop("cullface", None)
        out.append(el)
    return out


if __name__ == "__main__":
    main()

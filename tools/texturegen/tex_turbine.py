"""Textures of the steam turbine and its fusion twin, the plasma turbine: a compact Create-style jet engine.

The engine is a 3x3 shell of thin plates. Each casing face is a dark steel panel with a brass slot; Create's RECTANGLE connected
textures (``*_casing`` 32x + ``*_casing_connected`` 128x, 4x4 tiles: col 0 isolated, 1 joined right, 2 joined both, 3 joined left;
row 0 joined down, 1 both, 2 joined up, 3 isolated) put brass trim along every edge of the shell. The controller's front slice is
the ``*_front_bezel`` sheet (96x, 3x3 tiles seen from the front): a plate round a stepped round opening whose geometry is
``bezel_rows()`` below, shared with ``models/block/turbine/front_bezel.json``, so the brass ring and the dark stator recess are
painted exactly where the model's boxes put them. The fan spinning inside is ``*_fan``. The steam tier is blue-grey steel and
brass; the fusion tier is titanium and violet. Sizes other than 16x16 are listed in ``blocks.SHEET_BLOCKS`` / ``CUTOUT_BLOCKS``
from here. This module is imported at the end of ``blocks.py``.
"""

import math

from blocks import CUTOUT_BLOCKS, SHEET_BLOCKS, block
from fusion_materials import GRAPHITE, TITANIUM
from palette import BRASS, CYAN, DARK, PLASMA
from pixelkit import Ramp, Tex, _hash, fbm, mix, shade

# Blue-grey engine steel, darker than the reactor steel, like the casing in the reference build.
ENGINE = Ramp("#1d2229", "#282e37", "#333a45", "#3f4753", "#4c5562", "#5f6876", "#79838f")
# Light polished steel of the fan and rotor blades.
FAN = Ramp("#50575d", "#6a7278", "#868d92", "#a1a8ab", "#bcc1c2", "#d5d9d8", "#eceeec")
# The fusion tier's trim: violet anodised fittings on titanium.
VIOLET = Ramp("#2b1a3f", "#45285f", "#613a83", "#7f50a6", "#9d6dc4", "#bd94dc", "#dcc0ee")
GLINT = (214, 226, 230, 255)

# body: outer plates; deep: recesses and the inside of the shell; trim: edges, rings and slots.
STEAM = {"body": ENGINE, "deep": ENGINE, "trim": BRASS, "base": 0.5, "seed": 1200}
FUSION = {"body": TITANIUM, "deep": GRAPHITE, "trim": VIOLET, "base": 0.42, "seed": 1300}

SHEET_BLOCKS.update({name: size for prefix in ("turbine", "fusion_turbine") for name, size in (
    (prefix + "_casing", (32, 32)),
    (prefix + "_casing_connected", (128, 128)),
    (prefix + "_casing_inner", (32, 32)),
    (prefix + "_trim", (32, 32)),
    (prefix + "_front_bezel", (96, 96)),
    (prefix + "_fan", (32, 32)),
    (prefix + "_fluid_port_input", (32, 32)),
    (prefix + "_fluid_port_output", (32, 32)),
    (prefix + "_blade", (32, 32)),
    (prefix + "_rotor_core", (32, 32)),
)})
SHEET_BLOCKS.update({
    "turbine_exhaust_port": (32, 32),
    "turbine_output_front": (32, 32),
    "turbine_output_recess": (32, 32),
    "fusion_plasma_turbine_front": (32, 32),
    "fusion_plasma_turbine_recess": (32, 32),
})
CUTOUT_BLOCKS.update({
    "reinforced_turbine_glass": (32, 32),
    "reinforced_turbine_glass_connected": (128, 128),
})


# ======================================================================================
# front bezel geometry (model pixels, relative to the controller block; the opening is centred on (8, 8))
# ======================================================================================

R_OPEN = 19.0
R_RING = 21.5
ROW_BREAKS = (0, 4, 8, 11, 13, 15, 16.5, 18, 19, 20.5, 21.5)


def _half(v):
    return math.ceil(v * 2 - 1e-9) / 2


def bezel_rows():
    """The stepped opening as rows ``(a, b, w_open, w_ring)``: for ``a <= |y - 8| < b`` the opening spans ``|x - 8| < w_open``
    (never smaller than the circle, so the fan clears it) and the brass ring ``w_open <= |x - 8| < w_ring``."""
    rows = []
    for a, b in zip(ROW_BREAKS, ROW_BREAKS[1:]):
        w_open = _half(math.sqrt(R_OPEN ** 2 - a * a)) if a < R_OPEN else 0.0
        w_ring = round(math.sqrt(R_RING ** 2 - a * a) * 2) / 2
        rows.append((a, b, w_open, w_ring))
    return rows


def bezel_class(x, y):
    """'open', 'ring' or 'plate' for a point of the front plate."""
    dx, dy = abs(x - 8), abs(y - 8)
    for a, b, w_open, w_ring in bezel_rows():
        if a <= dy < b:
            return "open" if dx < w_open else "ring" if dx < w_ring else "plate"
    return "plate"


# ======================================================================================
# surfaces
# ======================================================================================

def _ramp_at(ramp, v):
    """A colour between the ramp's entries, ``v`` from 0 (darkest) to 1."""
    v = max(0.0, min(1.0, v)) * (len(ramp) - 1)
    lo = int(v)
    if lo >= len(ramp) - 1:
        return ramp[len(ramp) - 1]
    return mix(ramp[lo], ramp[lo + 1], v - lo)


def _plate(t, x0, y0, x1, y1, ramp, seed, base, amp=0.07, ox=0, oy=0):
    """Soft painted steel. The noise is sampled in tile-local coordinates (``ox``, ``oy``) so every tile of a sheet matches."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x - ox, y - oy, seed, 32, 32, octaves=2, scale=4.0) - 0.5
            t.set(x, y, _ramp_at(ramp, base + n * amp * 2))


def _seams(t, ox, oy, B, size=32):
    """The joint to the next block: lit top-left, shaded bottom-right."""
    t.hline(ox, ox + size - 1, oy, B[4])
    t.vline(ox, oy, oy + size - 1, B[4])
    t.hline(ox, ox + size - 1, oy + size - 1, B[1])
    t.vline(ox + size - 1, oy, oy + size - 1, B[1])


def _bolt(t, x, y, T):
    t.set(x, y, T[6])
    t.set(x + 1, y, T[4])
    t.set(x, y + 1, T[4])
    t.set(x + 1, y + 1, T[1])


def _trim(t, ox, oy, P, left, right, top, bottom, width=4, inner=None, cap=8, seed=0):
    """Brass trim along the unjoined edges of a 32px tile, with a bolted cap where two trimmed edges meet."""
    B, T = P["body"], P["trim"]
    inner = B[0] if inner is None else inner
    # outline, lit edge (set per side below), body, shaded inner edge, then the shadow on the plate
    bands = ((T[0], None, T[3], T[2]) if width == 4 else (T[0], None, T[2])) + (inner,)
    for side, on in (("top", top), ("bottom", bottom), ("left", left), ("right", right)):
        if not on:
            continue
        lit = side in ("top", "left")
        for i in range(32):
            for d, c in enumerate(bands):
                if d == 1:
                    c = T[5] if lit else T[4]
                if 1 <= d < width and _hash(i, d + len(side), P["seed"] + seed) < 0.05:
                    c = T[1]
                x, y = {"top": (i, d), "bottom": (i, 31 - d), "left": (d, i), "right": (31 - d, i)}[side]
                t.set(ox + x, oy + y, c)
    for (cx, cy), on in (((0, 0), top and left), ((32 - cap, 0), top and right),
                         ((0, 32 - cap), bottom and left), ((32 - cap, 32 - cap), bottom and right)):
        if not on:
            continue
        x0, y0 = ox + cx, oy + cy
        t.rect(x0, y0, x0 + cap - 1, y0 + cap - 1, T[3])
        t.hline(x0 + 1, x0 + cap - 2, y0 + 1, T[5])
        t.vline(x0 + 1, y0 + 1, y0 + cap - 2, T[5])
        t.hline(x0 + 1, x0 + cap - 2, y0 + cap - 2, T[2])
        t.vline(x0 + cap - 2, y0 + 1, y0 + cap - 2, T[2])
        t.frame(x0, y0, x0 + cap - 1, y0 + cap - 1, T[0])
        _bolt(t, x0 + cap // 2 - 1, y0 + cap // 2 - 1, T)
        # shadow the cap casts on the plate
        if cx == 0:
            t.vline(x0 + cap, y0 + (1 if cy == 0 else -1), y0 + cap - (0 if cy == 0 else 1), inner)
        else:
            t.vline(x0 - 1, y0 + (1 if cy == 0 else -1), y0 + cap - (0 if cy == 0 else 1), inner)
        if cy == 0:
            t.hline(x0 + (1 if cx == 0 else -1), x0 + cap - (0 if cx == 0 else 1), y0 + cap, inner)
        else:
            t.hline(x0 + (1 if cx == 0 else -1), x0 + cap - (0 if cx == 0 else 1), y0 - 1, inner)


def _slot(t, ox, oy, P):
    """The side panel's brass slot, sunk in a dark recess."""
    B, T = P["body"], P["trim"]
    t.rect(ox + 13, oy + 8, ox + 18, oy + 23, B[0])
    for x, c in zip(range(14, 18), (T[5], T[4], T[3], T[2])):
        t.vline(ox + x, oy + 9, oy + 22, c)
    t.hline(ox + 14, ox + 16, oy + 9, T[6])
    t.hline(ox + 14, ox + 17, oy + 22, T[1])
    for x, y in ((16, 13), (15, 18), (17, 19)):
        t.set(ox + x, oy + y, T[2])
    t.hline(ox + 13, ox + 18, oy + 24, B[4])
    t.vline(ox + 19, oy + 8, oy + 24, B[4])


def _casing_tile(t, ox, oy, P, left, right, top, bottom, slot=True):
    B = P["body"]
    _plate(t, ox, oy, ox + 31, oy + 31, B, P["seed"], P["base"], ox=ox, oy=oy)
    _seams(t, ox, oy, B)
    # a shallow recessed field round the slot
    for y in range(oy + 6, oy + 26):
        for x in range(ox + 6, ox + 26):
            t.set(x, y, shade(t.get(x, y), 0.9))
    t.hline(ox + 6, ox + 25, oy + 6, B[1])
    t.vline(ox + 6, oy + 6, oy + 25, B[1])
    t.hline(ox + 7, ox + 25, oy + 25, B[5])
    t.vline(ox + 25, oy + 7, oy + 25, B[5])
    if slot:
        _slot(t, ox, oy, P)
    _trim(t, ox, oy, P, left, right, top, bottom)


def _ct_sheet(P, tile, seed, cutout=False):
    t = Tex(128, 128, seed)
    for row in range(4):
        for col in range(4):
            tile(t, col * 32, row * 32, P, col in (0, 1), col in (0, 3), row in (0, 3), row in (2, 3))
    return t


def _casing(P):
    t = Tex(32, 32, P["seed"])
    _casing_tile(t, 0, 0, P, True, True, True, True)
    return t


def _inner(P):
    """The shell seen from inside: plain dark plate."""
    B = P["deep"]
    t = Tex(32, 32, P["seed"] + 1)
    _plate(t, 0, 0, 31, 31, B, P["seed"] + 1, 0.2, amp=0.05)
    _seams(t, 0, 0, Ramp(*[shade(c, 0.8) for c in B.c]))
    for y in (10, 21):
        t.hline(1, 30, y, B[0])
        t.hline(1, 30, y + 1, B[2])
    return t


def _trim_tex(P):
    """Plain brass for the ring walls and the bezel's lips; no direction, so any face orientation works."""
    T = P["trim"]
    t = Tex(32, 32, P["seed"] + 2)
    _plate(t, 0, 0, 31, 31, T, P["seed"] + 2, 0.52, amp=0.06)
    for y in range(32):
        for x in range(32):
            if _hash(x, y, P["seed"] + 3) < 0.03:
                t.set(x, y, T[2])
    return t


# ======================================================================================
# glass
# ======================================================================================

def _glass_tile(t, ox, oy, P, left, right, top, bottom):
    """A clear pane: brass frame only where the pane meets the shell's edge, a couple of glints."""
    for i in range(5):
        t.set(ox + 18 + i, oy + 13 - i, GLINT)
    for i in range(2):
        t.set(ox + 21 + i, oy + 15 - i, GLINT)
    t.set(ox + 9, oy + 24, GLINT)
    _trim(t, ox, oy, P, left, right, top, bottom, width=3, inner=P["body"][2], cap=7, seed=7)


@block("reinforced_turbine_glass")
def reinforced_turbine_glass():
    t = Tex(32, 32, 41)
    _glass_tile(t, 0, 0, STEAM, True, True, True, True)
    return t


@block("reinforced_turbine_glass_connected")
def reinforced_turbine_glass_connected():
    return _ct_sheet(STEAM, _glass_tile, 42)


# ======================================================================================
# ports
# ======================================================================================

def _port_recess(t, P):
    """The sunken square (texels 6..25, the formed panel's uv 3..13) a port's fitting sits in."""
    B = P["deep"]
    _plate(t, 6, 6, 25, 25, B, P["seed"] + 5, 0.22, amp=0.05)
    t.hline(6, 25, 6, B[0])
    t.vline(6, 6, 25, B[0])
    t.hline(7, 25, 7, B[1])
    t.vline(7, 7, 25, B[1])
    t.hline(6, 25, 25, B[3])
    t.vline(25, 6, 25, B[3])


def _ellipse(t, cx, cy, rx, ry, fn):
    for y in range(t.h):
        for x in range(t.w):
            e = ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2
            if e <= 1.0:
                t.set(x, y, fn(x, y, e))


def _port(P, accent, out, seed):
    """Brass oval intake pad with a coloured collar and a flow chevron (up = out of the engine)."""
    T = P["trim"]
    t = Tex(32, 32, seed)
    _casing_tile(t, 0, 0, P, True, True, True, True, slot=False)
    _port_recess(t, P)
    _ellipse(t, 16, 16, 9.0, 7.5, lambda x, y, e: T[5 - (y - 8.5) / 15 * 3])
    _ellipse(t, 16, 16, 6.2, 4.7, lambda x, y, e: accent[4 - (y - 11) / 10 * 2])
    _ellipse(t, 16, 16, 4.8, 3.4, lambda x, y, e: DARK[0])
    # outlines
    for x0, y0, rx, ry, c in ((16, 16, 9.0, 7.5, T[0]), (16, 16, 6.2, 4.7, accent[1])):
        for y in range(32):
            for x in range(32):
                inside = ((x + 0.5 - x0) / rx) ** 2 + ((y + 0.5 - y0) / ry) ** 2 <= 1
                edge = any(((x + dx + 0.5 - x0) / rx) ** 2 + ((y + dy + 0.5 - y0) / ry) ** 2 > 1
                           for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if inside and edge:
                    t.set(x, y, c)
    rows = ((14, (15, 16)), (15, (14, 17)), (16, (13, 18))) if out else ((18, (15, 16)), (17, (14, 17)), (16, (13, 18)))
    for y, xs in rows:
        for x in xs:
            t.set(x, y, accent[6])
    for y in ((16, 17) if out else (14, 15)):
        t.hline(15, 16, y, accent[5])
    return t


@block("turbine_exhaust_port")
def turbine_exhaust_port():
    """Spent steam leaves through a louvred grille: lit slats over dark gaps."""
    P = STEAM
    B, T = P["body"], P["trim"]
    t = Tex(32, 32, 99)
    _casing_tile(t, 0, 0, P, True, True, True, True, slot=False)
    _port_recess(t, P)
    for y in range(9, 23):
        k = (y - 9) % 4
        t.hline(9, 22, y, (B[5], B[4], B[2], DARK[0])[k])
    t.frame(8, 8, 23, 23, B[1])
    t.vline(15, 9, 22, B[1])
    t.vline(16, 9, 22, B[4])
    for x, y in ((9, 9), (21, 9), (9, 21), (21, 21)):
        _bolt(t, x, y, T)
    return t


# ======================================================================================
# front bezel and fan
# ======================================================================================

def _bezel(P, seed):
    """The 3x3 front seen from outside. Sheet pixel (sx, sy) is model point x = 32 - sx/2, y = 32 - sy/2."""
    B, T = P["body"], P["trim"]
    t = Tex(96, 96, seed)
    for r in range(3):
        for c in range(3):
            _plate(t, c * 32, r * 32, c * 32 + 31, r * 32 + 31, B, P["seed"], P["base"], ox=c * 32, oy=r * 32)
            _seams(t, c * 32, r * 32, B)
    cls = {}
    for sy in range(96):
        for sx in range(96):
            cls[sx, sy] = bezel_class(32 - (sx + 0.5) / 2, 32 - (sy + 0.5) / 2)
    # brass corner gussets just inside the lips (the outer 4 sheet pixels hide behind them)
    for cx, cy in ((4, 4), (80, 4), (4, 80), (80, 80)):
        t.rect(cx, cy, cx + 11, cy + 11, T[3])
        t.hline(cx + 1, cx + 10, cy + 1, T[5])
        t.vline(cx + 1, cy + 1, cy + 10, T[5])
        t.hline(cx + 1, cx + 10, cy + 10, T[2])
        t.vline(cx + 10, cy + 1, cy + 10, T[2])
        t.frame(cx, cy, cx + 11, cy + 11, T[0])
        _bolt(t, cx + 5, cy + 5, T)
    for (sx, sy), k in cls.items():
        x, y = 32 - (sx + 0.5) / 2, 32 - (sy + 0.5) / 2
        dx, dy = x - 8, y - 8
        d = math.hypot(dx, dy)
        near = [cls.get((sx + a, sy + b)) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        if k == "ring":
            light = (dx + dy) / (math.sqrt(2) * max(d, 1))
            c = T[3.4 + 1.3 * light]
            if "plate" in near:
                c = T[0]
            elif "open" in near:
                c = T[6] if light > -0.2 else T[2]
            t.set(sx, sy, c)
        elif k == "plate" and "ring" in near:
            t.set(sx, sy, B[0])
        elif k == "open":
            # the stator recess behind the fan: dark duct, twelve vanes, a bearing collar round the shaft hole
            D = P["deep"]
            c = _ramp_at(D, 0.1 + 0.05 * (1 - d / R_OPEN))
            ang = math.atan2(dy, dx)
            for i in range(12):
                a = math.radians(15 + 30 * i)
                off = -dx * math.sin(a) + dy * math.cos(a)
                along = dx * math.cos(a) + dy * math.sin(a)
                if 5.5 < along < 18 and abs(off) < 0.75:
                    c = D[3] if off > 0 else D[2]
            if 3.2 < d < 5.2:
                c = D[4] if math.sin(ang + math.pi / 4) > 0 else D[2]
            if d > 17.6:
                c = D[0]
            t.set(sx, sy, c)
    # eight bolts round the ring
    for i in range(8):
        a = math.radians(22.5 + 45 * i)
        x, y = 8 + math.cos(a) * 20.25, 8 + math.sin(a) * 20.25
        sx, sy = int((32 - x) * 2), int((32 - y) * 2)
        if all(cls.get((sx + a2, sy + b2)) == "ring" for a2 in (0, 1) for b2 in (0, 1)):
            _bolt(t, sx, sy, T)
    return t


def _fan(P, seed):
    """The fan's parts on one 32px texture (uv in 16ths):
    blade front (0,0)-(16,3) root left, blade back (0,3)-(16,6), blade edge (0,6)-(16,7),
    hub face (0,8)-(8,16) with the shaft socket in the middle, hub side (8,8)-(16,16)."""
    L, T, B = FAN, P["trim"], P["body"]
    t = Tex(32, 32, seed)
    for x in range(32):
        f = 0.82 + 0.18 * min(1.0, x / 20)
        for y, i in zip(range(6), (6, 5, 5, 4, 4, 2)):
            t.set(x, y, shade(L[i], f))
        for y, i in zip(range(6, 12), (4, 3, 3, 2, 2, 1)):
            t.set(x, y, shade(L[i], f))
        t.hline(x, x, 12, L[3])
        t.hline(x, x, 13, L[2])
    for y in range(12):
        t.set(29, y, L[3] if y < 6 else L[1])
        t.set(30, y, L[2] if y < 6 else L[1])
        t.set(31, y, L[1] if y < 6 else L[0])
    t.rect(0, 14, 31, 15, B[1])
    # hub face: spinner rings round a dark shaft socket
    for y in range(16, 32):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 24)
            c = L[5] if d < 3.5 else L[4] if d < 5.5 else T[3] if d < 6.6 else L[3]
            if (x + 0.5 - 8) + (y + 0.5 - 24) > 3 and c != T[3]:
                c = shade(c, 0.85)
            t.set(x, y, c)
    t.frame(0, 16, 15, 31, L[1])
    t.rect(5, 21, 10, 26, DARK[0])
    t.hline(5, 10, 27, L[6])
    t.vline(11, 21, 27, L[6])
    for i in range(8):
        a = math.radians(45 * i)
        _bolt(t, int(8 + math.cos(a) * 6.2 - 0.5), int(24 + math.sin(a) * 6.2 - 0.5), T) if i % 2 == 0 else None
    # hub side: banded
    for y in range(16, 32):
        for x in range(16, 32):
            t.set(x, y, (L[4], L[3], L[3], L[2], T[3], T[2], L[3], L[2])[(y - 16) // 2])
    return t


# ======================================================================================
# controller (unformed) and rotor
# ======================================================================================

def _output_front(P, seed):
    """A one-block jet: steel frame, bolted brass corners, brass ring round the intake (texels 6..25, notched corners)."""
    B, T = P["body"], P["trim"]
    t = Tex(32, 32, seed)
    _plate(t, 0, 0, 31, 31, B, P["seed"], P["base"], ox=0, oy=0)
    t.frame(0, 0, 31, 31, B[0])
    t.hline(1, 30, 1, B[5])
    t.vline(1, 1, 30, B[5])
    t.hline(1, 30, 30, B[1])
    t.vline(30, 1, 30, B[1])

    def opening(x, y):
        return 6 <= x <= 25 and 6 <= y <= 25 and not ((x < 9 or x > 22) and (y < 9 or y > 22))

    for y in range(32):
        for x in range(32):
            if opening(x, y):
                t.set(x, y, B[0])
                continue
            near = min((max(abs(x - a), abs(y - b)) for a in range(6, 26) for b in range(6, 26) if opening(a, b)))
            if near <= 2:
                lit = (x < 16) + (y < 16)
                t.set(x, y, T[0] if near == 2 else (T[5], T[4], T[2])[2 - lit] if near == 1 else T[3])
    _trim(t, 0, 0, P, False, False, False, False)
    for cx, cy in ((1, 1), (25, 1), (1, 25), (25, 25)):
        t.rect(cx, cy, cx + 5, cy + 5, T[3])
        t.frame(cx, cy, cx + 5, cy + 5, T[0])
        _bolt(t, cx + 2, cy + 2, T)
    return t


def _output_recess(P, seed):
    """The intake floor behind the little fan: stator spokes round the shaft hole."""
    B = P["deep"]
    t = Tex(32, 32, seed)
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            d = math.hypot(dx, dy)
            c = _ramp_at(B, 0.12 + 0.05 * (1 - d / 16))
            for i in range(8):
                a = math.radians(22.5 + 45 * i)
                if 6 < dx * math.cos(a) + dy * math.sin(a) < 15 and abs(-dx * math.sin(a) + dy * math.cos(a)) < 0.8:
                    c = B[3]
            if 4 <= d < 6.5:
                c = B[4] if dx + dy < 0 else B[2]
            t.set(x, y, c)
    t.rect(12, 12, 19, 19, DARK[0])
    return t


def _blade(P, seed):
    """Rotor blade: polished steel with a lit leading edge and diagonal twist bands."""
    L = FAN
    t = Tex(32, 32, seed)
    for y in range(32):
        for x in range(32):
            k = (x + y) % 12
            t.set(x, y, L[(5, 5, 4, 4, 4, 3, 3, 3, 3, 4, 4, 5)[k]])
    t.hline(0, 31, 0, L[6])
    t.hline(0, 31, 1, L[5])
    t.hline(0, 31, 30, L[2])
    t.hline(0, 31, 31, L[1])
    return t


def _rotor_core(P, seed):
    """The rotor shaft: banded steel with trim collars; the ends show a bolted hub."""
    L, T = FAN, P["trim"]
    t = Tex(32, 32, seed)
    for y in range(32):
        for x in range(32):
            t.set(x, y, L[(2, 3, 4, 4, 3, 2, 2, 1)[(x // 2) % 8]])
    for y in (4, 26):
        t.hline(0, 31, y, T[5])
        t.hline(0, 31, y + 1, T[3])
        t.hline(0, 31, y + 2, T[1])
    for y in range(32):
        for x in range(32):
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            if 6.5 <= d < 7.5:
                t.set(x, y, T[1])
            elif d < 6.5:
                t.set(x, y, T[4] if x + y < 30 else T[3])
    t.rect(14, 14, 17, 17, DARK[0])
    return t


# ======================================================================================
# registrations
# ======================================================================================

for _prefix, _P, _front, _seed in (("turbine", STEAM, "turbine_output", 1200),
                                   ("fusion_turbine", FUSION, "fusion_plasma_turbine", 1300)):
    block(_prefix + "_casing")(lambda P=_P: _casing(P))
    block(_prefix + "_casing_connected")(lambda P=_P, s=_seed: _ct_sheet(P, _casing_tile, s + 10))
    block(_prefix + "_casing_inner")(lambda P=_P: _inner(P))
    block(_prefix + "_trim")(lambda P=_P: _trim_tex(P))
    block(_prefix + "_front_bezel")(lambda P=_P, s=_seed: _bezel(P, s + 20))
    block(_prefix + "_fan")(lambda P=_P, s=_seed: _fan(P, s + 30))
    block(_prefix + "_fluid_port_input")(lambda P=_P, s=_seed: _port(P, CYAN, False, s + 40))
    block(_prefix + "_fluid_port_output")(lambda P=_P, s=_seed: _port(P, PLASMA, True, s + 41))
    block(_prefix + "_blade")(lambda P=_P, s=_seed: _blade(P, s + 50))
    block(_prefix + "_rotor_core")(lambda P=_P, s=_seed: _rotor_core(P, s + 51))
    block(_front + "_front")(lambda P=_P, s=_seed: _output_front(P, s + 60))
    block(_front + "_recess")(lambda P=_P, s=_seed: _output_recess(P, s + 61))

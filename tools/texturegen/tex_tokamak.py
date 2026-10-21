"""Textures of the formed tokamak: a faceted octagonal torus in dark vessel steel, brass trims and cream coil bands.

When the tokamak forms, its shell blocks hide and the client draws a continuous skin round the ring from these 32x32 patches
(``block/tokamak/*``). Each patch maps the whole texture onto roughly one block of skin, so the skin textures tile:

* ``skin``: vessel plate, seamless both ways. U (x) runs round the ring (toroidal), V (y) round the octagon (poloidal).
  A patch's own edges carry the panel joint (lit top/left, shaded bottom/right), so abutting patches read as riveted plates.
* ``skin_chamfer``: the same plate with a brass strip along U (rows 11..20) for the four diagonal sides; tiles in U.
* ``skin_band``: a cream toroidal-field coil band running along V, brass edges at x 0..3 and 28..31; tiles in V.
* ``window``: a brass porthole ring centred on (16, 16), outer radius 14.5 px, clear inside radius 10.5 px; the corners are
  ``skin`` plate (same noise and joints). Cutout.
* ``window_glass``: faint tinted glass with two highlight streaks, drawn translucent behind the porthole.

The nozzle, support and central solenoid textures are laid out for their models in ``models/block/tokamak/`` (UVs in model
pixels, two texels each): ``solenoid_ribbed`` puts the rib rows exactly where the model's rib rings sit (every 4 px, at model
y 1..3, 5..7, ...), so ``solenoid_glow`` lights only the gaps between them. This module is imported at the end of ``blocks.py``.
"""

import math

from blocks import CUTOUT_BLOCKS, SHEET_BLOCKS, TRANSLUCENT_BLOCKS, block
from palette import BRASS, COPPER
from pixelkit import Ramp, Tex, _hash, mix
from tex_turbine import ENGINE, _bolt, _plate, _ramp_at

# Off-white enamel of the toroidal field coil cases.
CREAM = Ramp("#4f4c42", "#7d7867", "#a8a28d", "#c9c3ab", "#ddd7bf", "#ece7d3", "#f8f5e8")
# Blue-white of the superconducting windings when the solenoid is energised.
GLOW = Ramp("#5fb8c9", "#7fd0dc", "#a6e4ea", "#c9f1f2", "#e6faf8", "#f6fffd", "#ffffff")
CLEAR = (0, 0, 0, 0)

P = "tokamak/"
SIZE = (32, 32)
SHEET_BLOCKS.update({P + n: SIZE for n in (
    "skin", "skin_chamfer", "skin_band", "nozzle_flange", "nozzle_body",
    "support_pillar", "support_foot", "support_cradle", "solenoid_ribbed", "solenoid_end")})
CUTOUT_BLOCKS.update({P + n: SIZE for n in ("window", "solenoid_glow")})
TRANSLUCENT_BLOCKS.update({P + "window_glass": SIZE})

SEED = 1400
R_OUT, R_IN = 14.5, 10.5


# ======================================================================================
# vessel skin
# ======================================================================================

def _skin_plate(seed=SEED):
    """Dark vessel plate: soft painted steel, panel joint on the patch edges, rivets along the joints."""
    t = Tex(32, 32, seed=seed)
    _plate(t, 0, 0, 31, 31, ENGINE, seed, base=0.5, amp=0.06)
    # A little grime that never touches the joints, so the tiling stays clean.
    for y in range(2, 30):
        for x in range(2, 30):
            if _hash(x, y, seed + 7) < 0.02:
                t.set(x, y, mix(t.get(x, y), ENGINE[1], 0.5))
    t.hline(0, 31, 0, ENGINE[5])
    t.vline(0, 0, 31, ENGINE[5])
    t.hline(0, 31, 31, ENGINE[0])
    t.vline(31, 0, 31, ENGINE[0])
    t.hline(1, 30, 30, ENGINE[2])
    t.vline(30, 1, 30, ENGINE[2])
    return t


def _rivets(t, coords):
    for x, y in coords:
        t.set(x, y, ENGINE[6])
        t.set(x + 1, y, ENGINE[4])
        t.set(x, y + 1, ENGINE[4])
        t.set(x + 1, y + 1, ENGINE[0])


EDGE_RIVETS = [(3, 3), (15, 3), (27, 3), (3, 27), (15, 27), (27, 27), (3, 15), (27, 15)]


@block(P + "skin")
def skin():
    t = _skin_plate()
    _rivets(t, EDGE_RIVETS)
    return t


@block(P + "skin_chamfer")
def skin_chamfer():
    t = _skin_plate(SEED + 1)
    _rivets(t, [(3, 3), (15, 3), (27, 3), (3, 27), (15, 27), (27, 27)])
    # Brass strip along U, bolted every 8 px (period divides 32, so it tiles round the ring).
    for x in range(32):
        n = (_hash(x, 0, SEED + 3) - 0.5) * 0.12
        t.set(x, 11, BRASS[0])
        t.set(x, 12, BRASS[5])
        for y in range(13, 19):
            t.set(x, y, _ramp_at(BRASS, 0.55 + n - (y - 13) * 0.03))
        t.set(x, 19, BRASS[2])
        t.set(x, 20, BRASS[0])
        t.set(x, 21, mix(t.get(x, 21), ENGINE[0], 0.6))
    for x in range(3, 32, 8):
        _bolt(t, x, 15, BRASS)
        t.set(x + 1, 17, BRASS[2])
    return t


@block(P + "skin_band")
def skin_band():
    t = Tex(32, 32, seed=SEED + 2)
    for y in range(32):
        for x in range(4, 28):
            # Lit from the left, a faint cross-band curvature and a little enamel mottling.
            v = 0.74 - (x - 4) * 0.006 + (_hash(x, y, SEED + 2) - 0.5) * 0.05
            t.set(x, y, _ramp_at(CREAM, v))
        # Brass edges, bevelled towards the light.
        for x, c in ((0, BRASS[0]), (1, BRASS[5]), (2, BRASS[3]), (3, BRASS[1]),
                     (28, BRASS[0]), (29, BRASS[4]), (30, BRASS[3]), (31, BRASS[1])):
            t.set(x, y, c)
        t.set(4, y, CREAM[2])
        t.set(27, y, CREAM[3])
    # Case joint once a patch, and the bolts on the brass edges every 8 px.
    t.hline(4, 27, 0, CREAM[6])
    t.hline(4, 27, 31, CREAM[1])
    for y in range(3, 32, 8):
        for x in (1, 29):
            t.set(x, y, BRASS[6])
            t.set(x + 1, y, BRASS[4])
            t.set(x, y + 1, BRASS[4])
            t.set(x + 1, y + 1, BRASS[0])
    return t


# ======================================================================================
# porthole
# ======================================================================================

def _brass_ring(t, cx, cy, r_out, r_in, clear_inside=False):
    """Raised brass ring lit from the top-left: the outer half faces out, the inner half faces the centre."""
    mid = (r_out + r_in) / 2
    for y in range(t.h):
        for x in range(t.w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d < r_in:
                if clear_inside:
                    t.set(x, y, CLEAR)
                continue
            if d > r_out + 1.0:
                continue
            if d > r_out:
                # cast shadow on the plate, bottom-right
                if dx + dy > 0:
                    t.set(x, y, mix(t.get(x, y), ENGINE[0], 0.55))
                continue
            facing = -(dx + dy) / (d * math.sqrt(2))
            s = 1 if d >= mid else -1
            v = 0.55 + 0.38 * s * facing
            if d > r_out - 0.8 or d < r_in + 0.8:
                c = BRASS[0]
            else:
                c = _ramp_at(BRASS, v)
            t.set(x, y, c)


@block(P + "window")
def window():
    t = _skin_plate()
    _rivets(t, [(2, 2), (28, 2), (2, 28), (28, 28)])
    _brass_ring(t, 16, 16, R_OUT, R_IN, clear_inside=True)
    r = (R_OUT + R_IN) / 2
    for i in range(8):
        a = math.radians(22.5 + 45 * i)
        x, y = round(16 + r * math.cos(a) - 1), round(16 + r * math.sin(a) - 1)
        t.set(x, y, BRASS[6])
        t.set(x + 1, y, BRASS[5])
        t.set(x, y + 1, BRASS[4])
        t.set(x + 1, y + 1, BRASS[1])
    return t


@block(P + "window_glass")
def window_glass():
    t = Tex(32, 32, seed=SEED + 4)
    for y in range(32):
        for x in range(32):
            a = 46 + int(_hash(x, y, SEED + 4) * 10)
            t.set(x, y, (178, 214, 220, a))
    # Two highlight streaks across the top-left of the glass.
    for x in range(32):
        for y in range(32):
            s = x + y
            if 12 <= s <= 14 and 4 <= x <= 14:
                t.set(x, y, (236, 248, 248, 150 if s == 13 else 112))
            elif s == 18 and 6 <= x <= 13:
                t.set(x, y, (236, 248, 248, 96))
    return t


# ======================================================================================
# nozzles (ports through the skin)
# ======================================================================================

@block(P + "nozzle_flange")
def nozzle_flange():
    """Square flange face; the model's 2px-thick sides sample the top strip (texels 0..4), where the bolt row sits."""
    t = Tex(32, 32, seed=SEED + 5)
    _plate(t, 0, 0, 31, 31, ENGINE, SEED + 5, base=0.58, amp=0.05)
    t.hline(0, 31, 0, ENGINE[6])
    t.vline(0, 0, 31, ENGINE[6])
    t.hline(0, 31, 1, ENGINE[5])
    t.hline(0, 31, 31, ENGINE[0])
    t.vline(31, 0, 31, ENGINE[0])
    t.hline(1, 30, 30, ENGINE[1])
    t.vline(30, 1, 30, ENGINE[1])
    # Raised collar round the bore.
    _brass_ring(t, 16, 16, 11.5, 8.5)
    t.disc(16, 16, 8.5, ENGINE[1])
    t.disc(16, 16, 6.5, ENGINE[0])
    for c in (3, 9, 15, 21, 27):
        for x, y in ((c, 2), (c, 27), (2, c), (27, c)):
            _bolt(t, x, y, BRASS)
    return t


@block(P + "nozzle_body")
def nozzle_body():
    """Round nozzle seen from the side: columns shaded like a cylinder, rows along the nozzle axis.

    Symmetric about v = 8, so the model's faces read the same whichever way their v runs: brass collars at
    texel rows 8..11 and 20..23 (model v 4..6 and 10..12), steel between."""
    t = Tex(32, 32, seed=SEED + 6)
    for y in range(32):
        for x in range(32):
            cyl = math.cos((x + 0.5) / 32 * math.pi - math.pi * 0.62)
            v = 0.25 + 0.6 * max(0.0, cyl) + (_hash(x, y, SEED + 6) - 0.5) * 0.04
            brass = 8 <= y <= 11 or 20 <= y <= 23
            t.set(x, y, _ramp_at(BRASS if brass else ENGINE, v + (0.05 if brass else 0.12)))
        if y in (8, 20):
            t.hline(0, 31, y, BRASS[5])
        if y in (11, 23):
            t.hline(0, 31, y, BRASS[1])
        if y in (12, 24):
            t.hline(0, 31, y, ENGINE[0])
    for y in (15, 16):
        for x in range(2, 32, 6):
            t.set(x, y, ENGINE[0] if y == 16 else ENGINE[5])
    return t


# ======================================================================================
# support stand
# ======================================================================================

@block(P + "support_pillar")
def support_pillar():
    """I-beam column, tiles vertically. u 0..4 (texels 0..7): flange edges; u 4..12 (8..23): flange face;
    u 12..16 (24..31): the web, with a lightening hole once per block."""
    t = Tex(32, 32, seed=SEED + 7)
    _plate(t, 0, 0, 31, 31, ENGINE, SEED + 7, base=0.48, amp=0.05)
    # flange edges
    for y in range(32):
        for x, c in ((0, ENGINE[6]), (1, ENGINE[5]), (2, ENGINE[4]), (5, ENGINE[3]), (6, ENGINE[2]), (7, ENGINE[0])):
            t.set(x, y, c)
    # flange face: lit left edge, shaded right edge, rivet pairs
    for y in range(32):
        t.set(8, y, ENGINE[5])
        t.set(9, y, ENGINE[4])
        t.set(22, y, ENGINE[2])
        t.set(23, y, ENGINE[0])
    for y in (6, 14, 22, 30):
        _rivets(t, [(11, y - 1), (18, y - 1)])
    # brass splice band once a block
    for y, c in ((0, BRASS[5]), (1, BRASS[3]), (2, BRASS[2]), (3, BRASS[0])):
        t.hline(8, 23, y, c)
    _bolt(t, 12, 1, BRASS)
    _bolt(t, 18, 1, BRASS)
    # web: darker, in the shadow of the flanges, with an oval hole
    for y in range(32):
        for x in range(24, 32):
            t.set(x, y, mix(t.get(x, y), ENGINE[0], 0.35))
        t.set(24, y, ENGINE[0])
        t.set(31, y, ENGINE[1])
    for y in range(10, 22):
        for x in range(25, 31):
            if ((x - 27.5) / 2.6) ** 2 + ((y - 15.5) / 5.6) ** 2 <= 1:
                t.set(x, y, ENGINE[0])
    t.vline(26, 11, 20, ENGINE[2])
    return t


@block(P + "support_foot")
def support_foot():
    """Base plate seen from above; its 2px sides sample the top strip (texels 0..4)."""
    t = Tex(32, 32, seed=SEED + 8)
    _plate(t, 0, 0, 31, 31, ENGINE, SEED + 8, base=0.46, amp=0.06)
    t.hline(0, 31, 0, ENGINE[6])
    t.vline(0, 0, 31, ENGINE[6])
    t.hline(0, 31, 1, ENGINE[4])
    t.hline(0, 31, 2, ENGINE[3])
    t.hline(0, 31, 3, ENGINE[1])
    t.hline(0, 31, 31, ENGINE[0])
    t.vline(31, 0, 31, ENGINE[0])
    t.vline(30, 1, 30, ENGINE[1])
    t.hline(1, 30, 30, ENGINE[1])
    # Anchor bolts: brass nuts on washers near each corner.
    for cx, cy in ((6, 7), (26, 7), (6, 26), (26, 26)):
        t.disc(cx, cy, 2.6, ENGINE[1])
        t.disc(cx - 0.3, cy - 0.3, 1.8, BRASS[3])
        t.set(cx - 2, cy - 2, BRASS[6])
        t.set(cx - 1, cy - 2, BRASS[5])
        t.set(cx - 2, cy - 1, BRASS[5])
        t.set(cx, cy, BRASS[1])
    return t


@block(P + "support_cradle")
def support_cradle():
    """Saddle plate: steel with a brass rim (outer 4 texels), a rubber bearing pad in the middle.
    The plate sides and the diagonal braces sample the brass top strip (texels 0..3)."""
    t = Tex(32, 32, seed=SEED + 9)
    _plate(t, 0, 0, 31, 31, ENGINE, SEED + 9, base=0.5, amp=0.05)
    for i, (lit, dark) in enumerate(((BRASS[6], BRASS[0]), (BRASS[5], BRASS[1]), (BRASS[4], BRASS[2]), (BRASS[1], BRASS[1]))):
        t.hline(i, 31 - i, i, lit if i < 3 else dark)
        t.vline(i, i, 31 - i, lit if i < 3 else dark)
        t.hline(i, 31 - i, 31 - i, dark)
        t.vline(31 - i, i, 31 - i, dark)
    # Bearing pad: dark rubber with ribs across.
    t.rect(9, 9, 22, 22, ENGINE[0])
    for y in range(10, 22, 3):
        t.hline(10, 21, y, ENGINE[2])
    for x, y in ((5, 5), (25, 5), (5, 25), (25, 25)):
        _rivets(t, [(x, y)])
    return t


# ======================================================================================
# central solenoid
# ======================================================================================

def _rib_row(y):
    """Texel row -> position inside a rib (0..3) or None for a winding gap. Ribs sit at model y 1..3, 5..7, ...
    i.e. texel rows 2..5, 10..13, 18..21, 26..29."""
    k = (y - 2) % 8
    return k if k < 4 else None


@block(P + "solenoid_ribbed")
def solenoid_ribbed():
    t = Tex(32, 32, seed=SEED + 10)
    for y in range(32):
        k = _rib_row(y)
        for x in range(32):
            edge = 0.08 if x < 2 else -0.1 if x > 29 else 0.0
            if k is None:
                # copper windings, one turn per texel row
                c = COPPER[4 if y % 2 == 0 else 2]
                if _hash(x, y, SEED + 10) < 0.06:
                    c = COPPER[5]
            else:
                c = _ramp_at(ENGINE, (0.95, 0.7, 0.55, 0.2)[k] + edge)
            t.set(x, y, c)
        if k == 2:
            for x in range(3, 32, 8):
                t.set(x, y, BRASS[5])
                t.set(x + 1, y, BRASS[2])
    return t


@block(P + "solenoid_end")
def solenoid_end():
    t = Tex(32, 32, seed=SEED + 11)
    _plate(t, 0, 0, 31, 31, ENGINE, SEED + 11, base=0.52, amp=0.05)
    t.hline(0, 31, 0, ENGINE[6])
    t.vline(0, 0, 31, ENGINE[6])
    t.hline(0, 31, 31, ENGINE[0])
    t.vline(31, 0, 31, ENGINE[0])
    t.hline(1, 30, 1, ENGINE[5])
    t.vline(1, 1, 30, ENGINE[5])
    t.hline(1, 30, 30, ENGINE[1])
    t.vline(30, 1, 30, ENGINE[1])
    _brass_ring(t, 16, 16, 12.5, 9.5)
    for i in range(8):
        a = math.radians(45 * i)
        x, y = round(16 + 11 * math.cos(a) - 1), round(16 + 11 * math.sin(a) - 1)
        _rivets(t, [(x, y)])
    for r in (9.5, 8.0, 6.5, 5.0):
        t.ring(16, 16, r, r - 0.9, COPPER[4] if r in (9.5, 6.5) else COPPER[2])
    t.disc(16, 16, 3.6, ENGINE[0])
    t.ring(16, 16, 3.6, 2.8, ENGINE[2])
    return t


@block(P + "solenoid_glow")
def solenoid_glow():
    """Emissive overlay: only the winding gaps of ``solenoid_ribbed`` are lit, everything else is clear."""
    t = Tex(32, 32, seed=SEED + 12)
    for y in range(32):
        if _rib_row(y) is not None:
            continue
        g = (y - 6) % 8  # 0..3 inside the gap
        for x in range(32):
            base = 5 if g in (1, 2) else 3
            if _hash(x, y, SEED + 12) < 0.12:
                base -= 1
            t.set(x, y, GLOW[base])
    return t

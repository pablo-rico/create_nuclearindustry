"""Textures of the launch base and the silo fuel port.

A loose launch base is a reinforced concrete pad (32x); once a controller forms a 3x3 of them each draws its ninth of one launch
table from the ``launch_base_formed`` sheet (96x96), whose top face is mapped by part in
``models/block/launch_base/formed_<part>.json``. The table is laid out like a real pad seen from above: the flame opening over the
trench (a W deflector under a grate) where the rocket stands, the bolted launch ring round its foot, the deluge moat that floods
the deck at ignition draining to the four corner sumps, the blast apron, the trench outlets on the east and west faces, the
four hold-down clamps' castings and a painted safety border. Sizes other than 16x16 are listed in ``blocks.SHEET_BLOCKS`` from
here. This module is imported at the end of ``blocks.py``.
"""

import math

from blocks import SHEET_BLOCKS, block
from palette import BRASS, CONCRETE, DANGER, DARK, MIMIC_BLUE, STEEL, WARN
from pixelkit import Ramp, Tex, _hash, fbm, mix, shade

from tex_launch import BLACK_PAINT, YELLOW
from tex_missile import _silo_concrete

SHEET_BLOCKS.update({
    "launch_base_top": (32, 32),
    "launch_base_side": (32, 32),
    "launch_base_formed": (96, 96),
    "launch_base_formed_side": (32, 32),
    "launch_base_formed_outlet": (32, 32),
})

# Soot and heat colours of the flame pit.
SOOT = Ramp("#120f0e", "#1c1715", "#29201b", "#3a2b22", "#4f3a2b", "#6a4c35", "#8a6544")
HEAT = Ramp("#2a1712", "#4a2416", "#6e3418", "#93481d", "#b46228", "#cf8240", "#e6a865")
# Water standing in the deluge channels.
WATER = Ramp("#14232c", "#1b3340", "#244555", "#30596a", "#46737f", "#6c97a0", "#a9c9c8")


def _ramp_at(ramp, v):
    """A colour between the ramp's entries, ``v`` from 0 (darkest) to 1."""
    v = max(0.0, min(1.0, v)) * (len(ramp) - 1)
    lo = int(v)
    if lo >= len(ramp) - 1:
        return ramp[len(ramp) - 1]
    return mix(ramp[lo], ramp[lo + 1], v - lo)


# ======================================================================================
# surfaces
# ======================================================================================

def _concrete(t, x0, y0, x1, y1, seed, base=0.5, amp=0.16, cells=None):
    """Poured concrete: soft blotches, fine grain, a few pores and pale aggregate."""
    cells = cells or max(4, t.w // 6)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, t.w, t.h, octaves=3, scale=cells) - 0.5
            g = _hash(x, y, seed + 7) - 0.5
            c = _ramp_at(CONCRETE, base + n * amp * 2 + g * 0.05)
            h = _hash(x, y, seed + 13)
            if h < 0.025:
                c = shade(c, 0.78)
            elif h > 0.985:
                c = mix(c, CONCRETE[6], 0.45)
            t.set(x, y, c)


def _slab(t, x0, y0, x1, y1, seed, base=0.5):
    """A cast slab between sealed joints: lit top-left edge, shaded bottom-right edge, dark joint beyond."""
    _concrete(t, x0, y0, x1, y1, seed, base)
    for x in range(x0, x1 + 1):
        t.set(x, y0, mix(t.get(x, y0), CONCRETE[6], 0.35))
        t.set(x, y1, shade(t.get(x, y1), 0.8))
    for y in range(y0, y1 + 1):
        t.set(x0, y, mix(t.get(x0, y), CONCRETE[6], 0.3))
        t.set(x1, y, shade(t.get(x1, y), 0.82))


def _steel(t, x0, y0, x1, y1, seed, ramp=STEEL, base=0.55, amp=0.1, axis="h", rim=True):
    """Brushed plate, lit from the top-left, with a dark rim."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, t.w, t.h, octaves=2, scale=max(4, t.w // 4)) - 0.5
            s = _hash(y if axis == "h" else x, 0, seed + 3) - 0.5
            t.set(x, y, _ramp_at(ramp, base + n * amp * 2 + s * 0.05))
    if rim:
        for x in range(x0, x1 + 1):
            t.set(x, y0, mix(t.get(x, y0), ramp[6], 0.45))
            t.set(x, y1, ramp[1])
        for y in range(y0, y1 + 1):
            t.set(x0, y, mix(t.get(x0, y), ramp[6], 0.35))
            t.set(x1, y, ramp[1])


def _bolt(t, x, y, ramp=STEEL):
    """A 2x2 hex head in 32x scale: lit corner, body, shadow corner and a cast shadow on the deck."""
    t.set(x, y, ramp[6])
    t.set(x + 1, y, ramp[4])
    t.set(x, y + 1, ramp[4])
    t.set(x + 1, y + 1, ramp[2])
    t.set(x + 2, y + 1, shade(t.get(x + 2, y + 1), 0.72))
    t.set(x + 1, y + 2, shade(t.get(x + 1, y + 2), 0.72))
    t.set(x + 2, y + 2, shade(t.get(x + 2, y + 2), 0.78))


def _hazard(t, x0, y0, x1, y1, seed, period=8, worn=0.68):
    """Yellow-black deck paint on world-aligned diagonals, scuffed where traffic wears it through to the concrete."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c = YELLOW if ((x + y) % period) < period // 2 else BLACK_PAINT
            c = mix(c, t.get(x, y), 0.12)
            n = fbm(x, y, seed, t.w, t.h, octaves=2, scale=max(4, t.w // 4))
            if n > worn:
                c = mix(c, t.get(x, y), 0.7)
            elif _hash(x, y, seed) < 0.04:
                c = mix(c, t.get(x, y), 0.5)
            t.set(x, y, c)


def _grate(t, x0, y0, x1, y1, seed, pitch=2, vertical=True):
    """A drain grating: steel frame round dark slots, a glint of water at the bottom of each."""
    t.rect(x0, y0, x1, y1, STEEL[1])
    for y in range(y0 + 1, y1):
        for x in range(x0 + 1, x1):
            k = (x - x0) if vertical else (y - y0)
            if k % pitch == 0:
                t.set(x, y, STEEL[4] if (y == y0 + 1 if vertical else x == x0 + 1) else STEEL[3])
            else:
                t.set(x, y, WATER[1] if _hash(x, y, seed) > 0.25 else WATER[3])
    t.hline(x0, x1, y0, STEEL[5])
    t.vline(x0, y0, y1, STEEL[4])
    t.hline(x0, x1, y1, STEEL[0])
    t.vline(x1, y0, y1, STEEL[0])


# ======================================================================================
# the loose pad: a heavy slab framed in steel, tiling as a floor
# ======================================================================================

def _pad_top(seed=940):
    t = Tex(32, 32, seed)
    # Four cast slabs per pad, the joints sealed dark, inside a steel edge angle.
    for i, (sx, sy) in enumerate(((2, 2), (16, 2), (2, 16), (16, 16))):
        _slab(t, sx, sy, sx + 13, sy + 13, seed + i, base=0.5 + (_hash(i, 0, seed) - 0.5) * 0.08)
    t.hline(2, 29, 15, CONCRETE[0])
    t.vline(15, 2, 29, CONCRETE[0])
    # The steel edge angle all round, its outer arris bright on the lit sides.
    for i in range(32):
        t.set(i, 0, STEEL[5])
        t.set(0, i, STEEL[5])
        t.set(i, 31, STEEL[1])
        t.set(31, i, STEEL[1])
        t.set(i, 1, STEEL[3]) if 0 < i < 31 else None
        t.set(1, i, STEEL[3]) if 0 < i < 31 else None
        t.set(i, 30, STEEL[2]) if 0 < i < 31 else None
        t.set(30, i, STEEL[2]) if 0 < i < 31 else None
    t.set(31, 0, STEEL[3])
    t.set(0, 31, STEEL[3])
    for x, y in ((3, 3), (27, 3), (3, 27), (27, 27)):
        _bolt(t, x, y)
    # The drain where the four slabs fall: a round cast-iron cover with its slots, in a dished collar.
    for y in range(10, 22):
        for x in range(10, 22):
            r = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            if r < 5.6:
                t.set(x, y, shade(t.get(x, y), 0.86 if (x + y) < 32 else 0.94))
            if r < 4.2:
                t.set(x, y, STEEL[2])
            if r < 3.4:
                lit = (x + y) < 31
                slot = (x % 2 == 0) and abs(y + 0.5 - 16) < 2.6
                t.set(x, y, WATER[1] if slot else (DARK[4] if lit else DARK[3]))
            if 3.4 <= r < 4.2:
                t.set(x, y, STEEL[4] if (x + y) < 31 else STEEL[1])
    # Hairline cracks and rust bleeding from the edge bolts.
    for x, y in ((6, 9), (7, 10), (7, 11), (8, 12), (23, 21), (24, 22), (24, 23)):
        t.set(x, y, shade(t.get(x, y), 0.8))
    for x, y in ((4, 6), (28, 6), (4, 30), (28, 30)):
        t.set(x, y, mix(t.get(x, y), HEAT[3], 0.35))
    return t


@block("launch_base_top")
def launch_base_top():
    """The loose pad: four cast slabs framed by a bolted steel edge angle round a central drain; tiles into a floor."""
    return _pad_top()


def _side(t, seed, formed=False):
    """The slab's edge: the deck's steel nosing, the poured face with its form-tie holes, and a plinth course."""
    _concrete(t, 0, 0, 31, 31, seed, base=0.48)
    # Formwork lifts.
    for y in (16,):
        for x in range(32):
            t.set(x, y, shade(t.get(x, y), 0.86))
            t.set(x, y + 1, mix(t.get(x, y + 1), CONCRETE[6], 0.15))
    # Steel nosing.
    _steel(t, 0, 0, 31, 4, seed + 1, axis="h")
    t.hline(0, 31, 0, STEEL[6])
    t.hline(0, 31, 5, DARK[1])
    t.hline(0, 31, 6, shade(t.get(0, 6), 0.7))
    for x in range(32):
        t.set(x, 6, shade(t.get(x, 6), 0.75))
        t.set(x, 7, shade(t.get(x, 7), 0.88))
    for x in (3, 27) if not formed else (3, 15, 27):
        _bolt(t, x, 2)
    # Form-tie holes.
    for x, y in ((7, 11), (24, 11), (7, 22), (24, 22)) if not formed else ((8, 11), (24, 11), (8, 22), (24, 22)):
        t.set(x, y, CONCRETE[0])
        t.set(x + 1, y, CONCRETE[1])
        t.set(x, y + 1, CONCRETE[2])
        t.set(x + 1, y + 1, CONCRETE[5])
    # Plinth course at the foot, chamfered.
    for x in range(32):
        t.set(x, 27, mix(t.get(x, 27), CONCRETE[6], 0.25))
        t.set(x, 28, shade(t.get(x, 28), 0.95))
        t.set(x, 31, shade(t.get(x, 31), 0.7))
    t.hline(0, 31, 26, shade(CONCRETE[2], 1.0))
    # Weather streaks running down from the nosing.
    for x in (5, 12, 19, 29):
        for y in range(8, 8 + 3 + int(_hash(x, 1, seed) * 8)):
            t.set(x, y, shade(t.get(x, y), 0.9))
    return t


@block("launch_base_side")
def launch_base_side():
    """The loose pad's edge: steel nosing over the poured slab."""
    t = Tex(32, 32, 942)
    return _side(t, 942)


# ======================================================================================
# the formed launch table: one 96x96 sheet, a third per block
# ======================================================================================

C = 48.0  # the table's centre, in sheet texels
R_OPEN = 17.5  # the flame opening (the rocket's square foot is 24 texels across: corners at 17)
R_RING_OUT = 22.5
MOAT_IN, MOAT_OUT = 25.0, 28.0


def _oct(dx, dy):
    """Octagonal 'radius': a square with chamfered corners."""
    return max(abs(dx), abs(dy), (abs(dx) + abs(dy)) / 1.42)


def _rot(x, y, q):
    """Turn a sheet point a quarter q times clockwise about the centre (north to east)."""
    for _ in range(q % 4):
        x, y = 95 - y, x
    return x, y


def _rot_rect(x0, y0, x1, y1, q):
    a, b = _rot(x0, y0, q), _rot(x1, y1, q)
    return min(a[0], b[0]), min(a[1], b[1]), max(a[0], b[0]), max(a[1], b[1])


def _pit(t):
    """The flame opening: through the grate, the trench's W deflector (its crest north-south, splitting the flame east and
    west), black with soot in the valleys, heat-scaled where the crests and the outer walls catch the light."""
    for y in range(96):
        for x in range(96):
            dx, dy = x + 0.5 - C, y + 0.5 - C
            r = math.hypot(dx, dy)
            if r >= R_OPEN:
                continue
            d = abs(dx)
            h = abs(d - 8.0) / 8.0  # W profile: 1 on the crest and at the walls, 0 in the valleys
            # Which way the face leans: west-facing faces (east of a valley's floor going up, west of the crest) take the light.
            west_facing = (dx < 0 and d > 8.0) or (dx > 0 and d < 8.0)
            v = 0.08 + 0.5 * h * (1.0 if west_facing else 0.45)
            n = fbm(x, y, 970, 96, 96, octaves=2, scale=24) - 0.5
            c = _ramp_at(SOOT, v + n * 0.18)
            if west_facing and h > 0.55:
                c = mix(c, HEAT[4], 0.35 * (h - 0.55) / 0.45)
            # Depth: the rim's shadow falls in on the north-west.
            if r > R_OPEN - 4 and (dx + dy) < 0:
                c = shade(c, 0.5 + 0.12 * (R_OPEN - r))
            t.set(x, y, c)
    # The grate: three heavy east-west bearers, lit on top, their shadows on the deflector.
    for y in range(96):
        for x in range(96):
            dx, dy = x + 0.5 - C, y + 0.5 - C
            if math.hypot(dx, dy) >= R_OPEN - 0.2:
                continue
            k = (y - 37) % 8
            if k == 0:
                t.set(x, y, STEEL[4])
            elif k == 1:
                t.set(x, y, mix(DARK[3], HEAT[3], 0.3))
            elif k == 2:
                t.set(x, y, shade(t.get(x, y), 0.45))


def _ring(t):
    """The launch ring: a bolted steel collar round the opening, lit as a raised ring (outer rim catches the light on the
    north-west, inner rim on the south-east)."""
    for y in range(96):
        for x in range(96):
            dx, dy = x + 0.5 - C, y + 0.5 - C
            r = math.hypot(dx, dy)
            if not (R_OPEN <= r < R_RING_OUT + 0.6):
                continue
            ux, uy = dx / r, dy / r
            lit = -(ux + uy) / math.sqrt(2)  # outward normal against light from the north-west
            n = fbm(x, y, 972, 96, 96, octaves=2, scale=24) - 0.5
            c = _ramp_at(STEEL, 0.58 + n * 0.12)
            if r >= R_RING_OUT:
                c = STEEL[0]
            elif r >= R_RING_OUT - 1:
                c = _ramp_at(STEEL, 0.55 + 0.4 * lit)
            elif r < R_OPEN + 1:
                c = _ramp_at(STEEL, 0.5 - 0.42 * lit)
            elif r < R_OPEN + 2:
                c = mix(c, DARK[1], 0.35)
            t.set(x, y, c)
    # A groove splitting the collar's two rings, and the bolt circle on the outer one.
    for y in range(96):
        for x in range(96):
            r = math.hypot(x + 0.5 - C, y + 0.5 - C)
            if abs(r - 99) < 0.5:
                t.set(x, y, DARK[1])
    for i in range(16):
        a = math.radians(i * 22.5 + 11.25)
        bx = int(round(C + math.cos(a) * 20.2 - 1))
        by = int(round(C + math.sin(a) * 20.2 - 1))
        t.set(bx, by, STEEL[6])
        t.set(bx + 1, by, STEEL[4])
        t.set(bx, by + 1, STEEL[4])
        t.set(bx + 1, by + 1, STEEL[1])


def _moat(t):
    """The deluge moat: an octagonal channel round the ring that floods the deck at ignition, and its four diagonal drains
    to the corner sumps. Recessed: its north-west lip in shadow, its south-east lip lit."""
    for y in range(96):
        for x in range(96):
            dx, dy = x + 0.5 - C, y + 0.5 - C
            o = _oct(dx, dy)
            diag = abs(abs(dx) - abs(dy)) < 2.2 and MOAT_OUT - 1 <= o and max(abs(dx), abs(dy)) < 38
            if not (MOAT_IN <= o < MOAT_OUT or diag):
                continue
            w = fbm(x, y, 973, 96, 96, octaves=2, scale=16)
            c = _ramp_at(WATER, 0.25 + 0.35 * w)
            if _hash(x, y, 974) > 0.94:
                c = WATER[5]
            t.set(x, y, c)
    # Lips: shade the texel on the inside of the channel's north-west edge, light the south-east one.
    src = {(x, y): t.get(x, y) for y in range(96) for x in range(96)}

    def wet(x, y):
        return 0 <= x < 96 and 0 <= y < 96 and src[(x, y)][2] > src[(x, y)][0] + 8 and src[(x, y)][2] < 230

    for y in range(96):
        for x in range(96):
            if wet(x, y):
                if not wet(x - 1, y) or not wet(x, y - 1):
                    t.set(x, y, WATER[0])
            else:
                if wet(x - 1, y) or wet(x, y - 1):
                    t.set(x, y, mix(src[(x, y)], CONCRETE[6], 0.4))
                elif wet(x + 1, y) or wet(x, y + 1):
                    t.set(x, y, shade(src[(x, y)], 0.7))


def _apron(t):
    """The blast apron between the ring and the moat: heavy steel plates, four of them meeting on the diagonals."""
    for y in range(96):
        for x in range(96):
            dx, dy = x + 0.5 - C, y + 0.5 - C
            o = _oct(dx, dy)
            if o >= MOAT_IN:
                continue
            n = fbm(x, y, 975, 96, 96, octaves=2, scale=24) - 0.5
            s = _hash(y if abs(dy) > abs(dx) else x, 1, 976) - 0.5
            c = _ramp_at(DARK, 0.6 + n * 0.14 + s * 0.05)
            if abs(abs(dx) - abs(dy)) < 0.75:
                c = DARK[1]
            elif abs(abs(dx) - abs(dy)) < 1.75 and (dx + dy < 0) == (abs(dx) > abs(dy)):
                c = mix(c, DARK[6], 0.3)
            t.set(x, y, c)
    # Stud bolts along each plate's outer edge.
    for q in range(4):
        for bx in (40, 46, 52):
            px, py = _rot(bx, 23, q)
            t.set(px, py, DARK[6])
            t.set(px + (1 if q % 2 == 0 else 0), py + (0 if q % 2 == 0 else 1), DARK[2])


def _clamp_base(t, q):
    """A hold-down clamp's cast base: a bolted plate under the hinge lugs with the slot its counterweight drops into, inside a
    yellow line marking the arm's swing. Drawn for the north clamp and turned by ``q`` quarters."""
    x0, y0, x1, y1 = _rot_rect(38, 6, 57, 16, q)
    # Swing-zone line.
    for x in range(x0 - 2, x1 + 3):
        for y in (y0 - 2, y1 + 2):
            t.set(x, y, mix(YELLOW, t.get(x, y), 0.2))
    for y in range(y0 - 2, y1 + 3):
        for x in (x0 - 2, x1 + 2):
            t.set(x, y, mix(YELLOW, t.get(x, y), 0.2))
    _steel(t, x0, y0, x1, y1, 980 + q, ramp=STEEL, base=0.45, amp=0.08)
    t.hline(x0 + 1, x1, y1 + 1, shade(t.get(x0 + 1, y1 + 1), 0.6))
    t.vline(x1 + 1, y0 + 1, y1 + 1, shade(t.get(x1 + 1, y0 + 1), 0.6))
    # Counterweight slot (z 2.5..5 of the edge block) and the hinge barrel's trough.
    sx0, sy0, sx1, sy1 = _rot_rect(42, 7, 53, 10, q)
    t.rect(sx0, sy0, sx1, sy1, DARK[0])
    t.hline(sx0, sx1, sy1, DARK[2])
    t.vline(sx1, sy0, sy1, DARK[2])
    hx0, hy0, hx1, hy1 = _rot_rect(42, 12, 53, 13, q)
    t.rect(hx0, hy0, hx1, hy1, STEEL[1])
    for bx, by in ((39, 7), (55, 7), (39, 13), (55, 13)):
        px, py = _rot(bx, by, q)
        px, py = min(px, _rot(bx + 1, by + 1, q)[0]), min(py, _rot(bx + 1, by + 1, q)[1])
        _bolt(t, px, py)


def _border(t):
    """The table's edge: a steel nosing and a band of worn yellow-black paint inside it."""
    _hazard(t, 2, 2, 93, 5, 976)
    _hazard(t, 2, 90, 93, 93, 977)
    _hazard(t, 2, 6, 5, 89, 978)
    _hazard(t, 90, 6, 93, 89, 979)
    for i in range(96):
        t.set(i, 0, STEEL[5])
        t.set(0, i, STEEL[5])
        t.set(i, 1, STEEL[3])
        t.set(1, i, STEEL[3])
        t.set(i, 95, STEEL[1])
        t.set(95, i, STEEL[1])
        t.set(i, 94, STEEL[2])
        t.set(94, i, STEEL[2])
    for i in range(2, 94):
        t.set(i, 6, shade(t.get(i, 6), 0.8)) if 6 <= i <= 89 else None
        t.set(6, i, shade(t.get(6, i), 0.8)) if 6 <= i <= 89 else None


@block("launch_base_formed")
def launch_base_formed():
    """The formed launch table's top, 96x96 for nine blocks of 32: see the module notes for the layout."""
    t = Tex(96, 96, 960)
    for sy in range(6):
        for sx in range(6):
            _slab(t, sx * 16, sy * 16, sx * 16 + 15, sy * 16 + 15, 961 + sy * 6 + sx,
                  base=0.5 + (_hash(sx, sy, 962) - 0.5) * 0.07)
    for k in range(16, 96, 16):
        t.hline(0, 95, k - 1, CONCRETE[1])
        t.vline(k - 1, 0, 95, CONCRETE[1])
    _apron(t)
    _moat(t)
    # Corner sumps where the diagonal drains end.
    for cx, cy in ((8, 8), (80, 8), (8, 80), (80, 80)):
        _grate(t, cx, cy, cx + 7, cy + 7, 981 + cx + cy, pitch=2, vertical=cx < 48)
    _ring(t)
    _pit(t)
    for q in range(4):
        _clamp_base(t, q)
    _border(t)
    return t


@block("launch_base_formed_side")
def launch_base_formed_side():
    """The launch table's edge, the same on every block so it runs on along a side."""
    t = Tex(32, 32, 944)
    return _side(t, 944, formed=True)


@block("launch_base_formed_outlet")
def launch_base_formed_outlet():
    """The flame trench's outlet on the east and west faces of the table: a scorched mouth behind heavy louvres."""
    t = Tex(32, 32, 945)
    _side(t, 945, formed=True)
    x0, x1, y0, y1 = 4, 27, 9, 29
    t.rect(x0 - 2, y0 - 1, x1 + 2, y1, CONCRETE[1])
    for y in range(y0, y1):
        for x in range(x0, x1 + 1):
            depth = (y - y0) / (y1 - y0)
            t.set(x, y, _ramp_at(SOOT, 0.12 + 0.25 * depth))
    for y in range(y0 + 1, y1, 3):
        t.hline(x0, x1, y, DARK[3])
        t.hline(x0, x1, y + 1, DARK[1])
    # Scorch licking up the face above the mouth.
    for y in range(6, y0 - 1):
        for x in range(x0 - 2, x1 + 3):
            a = 0.6 * (y - 5) / (y0 - 6) * (0.6 + 0.4 * fbm(x, y, 946, 32, 32, octaves=2, scale=8))
            t.set(x, y, mix(t.get(x, y), SOOT[2], a))
    t.hline(x0 - 2, x1 + 2, y1, CONCRETE[5])
    return t


# ======================================================================================
# the hold-down clamps' hardware (16x atlas shared by the clamp partial and the hinge lugs on the formed edge blocks)
# ======================================================================================

@block("launch_base_clamp")
def launch_base_clamp():
    """Atlas, by region: arm girder side (0,0)-(14,3) with its lightening holes, girder top (0,4)-(14,8) riveted, bolted
    steel for the head and lugs (0,8)-(6,13), the brass jaw pad with its grip grooves (6,8)-(11,15), the yellow counterweight
    (11,8)-(16,13), the hinge barrel (0,13)-(6,16) and brass pin caps (11,13)-(16,16)."""
    t = Tex(seed=950)
    _steel(t, 0, 0, 15, 15, 950, base=0.5, rim=False)
    # Girder side: flanges top and bottom, web with lightening holes.
    t.hline(0, 13, 0, STEEL[6])
    t.hline(0, 13, 1, STEEL[3])
    t.hline(0, 13, 2, STEEL[1])
    for x in (2, 6, 10):
        t.set(x, 1, DARK[0])
        t.set(x + 1, 1, DARK[1])
    t.vline(14, 0, 3, STEEL[2])
    t.vline(15, 0, 3, STEEL[2])
    t.hline(0, 15, 3, STEEL[2])
    # Girder top: riveted cover plate.
    t.rect(0, 4, 13, 7, STEEL[4])
    t.hline(0, 13, 4, STEEL[5])
    t.hline(0, 13, 7, STEEL[1])
    for x in range(1, 13, 3):
        t.set(x, 5, STEEL[6])
        t.set(x + 1, 6, STEEL[2])
    # Head and lug steel.
    t.rect(0, 8, 5, 12, STEEL[3])
    t.hline(0, 5, 8, STEEL[5])
    t.vline(0, 8, 12, STEEL[4])
    t.hline(0, 5, 12, STEEL[1])
    t.vline(5, 8, 12, STEEL[1])
    t.set(1, 9, STEEL[6])
    t.set(4, 9, STEEL[6])
    t.set(1, 11, STEEL[5])
    t.set(4, 11, STEEL[5])
    t.set(2, 10, STEEL[2])
    # Brass jaw pad with grip grooves.
    t.rect(6, 8, 10, 14, BRASS[3])
    for y in range(9, 14, 2):
        t.hline(6, 10, y, BRASS[1])
        t.hline(7, 10, y - 1, BRASS[4])
    t.vline(6, 8, 14, BRASS[5])
    t.vline(10, 8, 14, BRASS[1])
    t.hline(6, 10, 14, BRASS[0])
    # Counterweight in safety yellow, bevelled.
    t.rect(11, 8, 15, 12, WARN[4])
    t.hline(11, 15, 8, WARN[6])
    t.vline(11, 8, 12, WARN[5])
    t.hline(11, 15, 12, WARN[1])
    t.vline(15, 8, 12, WARN[2])
    t.set(13, 10, WARN[2])
    # Hinge barrel: dark steel lit on its upper curve.
    for x in range(0, 6):
        t.set(x, 13, DARK[5])
        t.set(x, 14, DARK[3])
        t.set(x, 15, DARK[1])
    for x in (1, 4):
        t.set(x, 14, DARK[6])
    # Pin caps.
    t.rect(11, 13, 15, 15, BRASS[3])
    t.hline(11, 15, 13, BRASS[5])
    t.hline(11, 15, 15, BRASS[1])
    t.set(13, 14, BRASS[6])
    return t


# ======================================================================================
# the silo fuel port
# ======================================================================================

FROST = Ramp("#5c7894", "#7f9bb4", "#a3bccf", "#c3d6e2", "#dce8ef", "#eef5f8", "#ffffff")


@block("silo_fuel_port")
def silo_fuel_port():
    """The fuelling face: a recessed steel panel set in the silo concrete, split between the fuel coupling (left, a red band
    under it) and the liquid oxygen coupling (right, blue band, frost creeping over the steel round it). The couplings, valve
    handwheels and gauges are model elements standing proud of this face."""
    t = Tex(seed=911)
    _silo_concrete(t, 911)
    t.frame(0, 0, 15, 15, DARK[0])
    t.hline(1, 14, 1, STEEL[4])
    t.vline(1, 1, 14, STEEL[4])
    t.hline(1, 14, 14, STEEL[1])
    t.vline(14, 1, 14, STEEL[1])
    # Recessed panel: shadowed top-left inner edge, lit bottom-right.
    _steel(t, 2, 2, 13, 13, 912, base=0.48, amp=0.08, rim=False)
    t.hline(2, 13, 2, STEEL[0])
    t.vline(2, 2, 13, STEEL[0])
    t.hline(3, 13, 3, STEEL[2])
    t.vline(3, 3, 13, STEEL[2])
    t.hline(3, 13, 13, STEEL[5])
    t.vline(13, 3, 13, STEEL[5])
    # Divider between the two halves.
    t.vline(7, 4, 12, STEEL[1])
    t.vline(8, 4, 12, STEEL[4])
    # Colour bands under each coupling.
    t.hline(3, 6, 12, DANGER[3])
    t.hline(4, 6, 12, DANGER[4])
    t.hline(9, 12, 12, MIMIC_BLUE[3])
    t.hline(9, 11, 12, MIMIC_BLUE[4])
    # Frost bloom on the oxygen side.
    for y in range(4, 13):
        for x in range(9, 13):
            n = fbm(x, y, 913, 16, 16, octaves=2, scale=8)
            if n > 0.55 and y > 6:
                t.set(x, y, mix(t.get(x, y), FROST[5], 0.55 * (n - 0.4)))
    for x, y in ((4, 4), (11, 4)):
        t.set(x, y, STEEL[6])
        t.set(x + 1, y + 1, STEEL[1])
    return t


@block("silo_fuel_port_parts")
def silo_fuel_port_parts():
    """Atlas for the port's couplings, by region: fuel pipe (0,0)-(8,4) and oxygen pipe frosted (8,0)-(16,4), the two
    couplings' bores (0,4)-(4,8) and (4,4)-(8,8), a gauge dial (8,4)-(12,8), a bolted flange (12,4)-(16,8), the handwheels'
    red (0,8)-(8,10) and blue (8,8)-(16,10) paint, and plain steel below."""
    t = Tex(seed=914)
    _steel(t, 0, 0, 15, 15, 914, base=0.5, rim=False)
    for x in range(8):
        for y, k in ((0, 4), (1, 3), (2, 2), (3, 1)):
            t.set(x, y, mix(DANGER[k], (200, 40, 32, 255), 0.35))
    for x in range(8, 16):
        for y, k in ((0, 5), (1, 4), (2, 3), (3, 1)):
            c = MIMIC_BLUE[k]
            if _hash(x, y, 915) > 0.55:
                c = mix(c, FROST[5], 0.6)
            t.set(x, y, c)
    fuel_red = Ramp(*[mix(c, (200, 40, 32, 255), 0.35) for c in DANGER.c])
    for ox, ramp in ((0, fuel_red), (4, MIMIC_BLUE)):
        t.rect(ox, 4, ox + 3, 7, ramp[4])
        t.frame(ox, 4, ox + 3, 7, ramp[2])
        t.rect(ox + 1, 5, ox + 2, 6, DARK[0])
        t.set(ox + 2, 6, DARK[2])
    for x, y in ((4, 4), (7, 7), (4, 7)):
        t.set(x, y, FROST[6])
    # Gauge (10,10)-(16,16): white dial in a dark bezel, red sector at the top right, needle swung up to it.
    DIAL = [
        ".####.",
        "#wwwr#",
        "#wwnr#",
        "#wnww#",
        "#wwww#",
        ".####.",
    ]
    pal = {"#": DARK[1], "w": (226, 226, 214, 255), "r": DANGER[3], "n": DARK[0]}
    t.stamp(DIAL, pal, 10, 10)
    for x, y in ((10, 10), (15, 10), (10, 15), (15, 15)):
        t.set(x, y, STEEL[2])
    t.rect(8, 4, 11, 7, STEEL[3])
    # Flange: steel ring with four bolts.
    t.rect(12, 4, 15, 7, STEEL[3])
    t.frame(12, 4, 15, 7, STEEL[1])
    for x, y in ((12, 4), (15, 4), (12, 7), (15, 7)):
        t.set(x, y, STEEL[6])
    for x in range(8):
        t.set(x, 8, fuel_red[4])
        t.set(x, 9, fuel_red[2])
    for x in range(8, 16):
        t.set(x, 8, MIMIC_BLUE[5])
        t.set(x, 9, MIMIC_BLUE[2])
    return t

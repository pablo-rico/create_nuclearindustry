"""Textures of the missile area: the silo, the rocket sections, the radar, its scope and the holographic command table.

Register block textures with ``@block``, items with ``@item`` and screens with ``@gui``; this module is imported at the end of
``blocks.py``.
"""

import math

from blocks import block  # noqa: F401
from common import bolt, casing, flange
from gui import gui
from items import item, mask_from, sculpt
from palette import BRASS, CONCRETE, COPPER, CYAN, DANGER, DARK, IN_ACCENT, RADIUM, STEEL, WARN, WHITE
from pixelkit import Ramp, Tex, bevel, brushed, fbm, glow, hazard, item_outline, mix, outline, shade


# ======================================================================================
# the silo: poured concrete lined with steel, and the port propellant is pumped in by
# ======================================================================================

def _silo_concrete(t, seed):
    """Smooth poured concrete with the faint marks of its formwork and a few pores."""
    for x in range(16):
        for y in range(16):
            n = fbm(x, y, seed, 16, 16, octaves=3, scale=5.0)
            t.set(x, y, CONCRETE.t(0.42 + 0.28 * n))
    for y in (5, 10):
        for x in range(16):
            t.set(x, y, shade(t.get(x, y), 0.9))
    for x, y in ((3, 2), (12, 7), (6, 12), (14, 13), (9, 3)):
        t.set(x, y, CONCRETE[1])


@block("silo_wall")
def silo_wall():
    t = Tex(seed=910)
    _silo_concrete(t, 910)
    # The steel liner's edge: a band round the slab, bolted at the corners.
    outline(t, 0, 0, 15, 15, DARK)
    t.hline(1, 14, 1, STEEL[4])
    t.vline(1, 1, 14, STEEL[4])
    t.hline(1, 14, 14, STEEL[1])
    t.vline(14, 1, 14, STEEL[1])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(t, x, y, STEEL)
    return t


# ======================================================================================
# guidance: the star tracker, a small brass-bound telescope on a gimballed steel base
# ======================================================================================

STAR_TRACKER = [
    "................",
    ".........####...",
    "........######..",
    ".......#######..",
    "......#######...",
    ".....#######....",
    "....#######.....",
    "...######.......",
    "..######........",
    "...####.........",
    "...#####........",
    "..########......",
    ".##########.....",
    ".##########.....",
    "..########......",
    "................",
]


# ======================================================================================
# the silo controller and the rocket's sections: a painted hull, dark engine metal, a
# concrete-and-steel console
# ======================================================================================

HULL = Ramp("#6f7473", "#8c918e", "#a7aba6", "#c0c3bc", "#d6d8d0", "#e7e8e0", "#f5f5ee")


def _hull(t, seed):
    """White-painted rocket skin: faint streaks down it, a seam either side."""
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, HULL.t(0.62 + 0.18 * n))
    for x in (0, 15):
        t.vline(x, 0, 15, HULL[2])
    for x in (4, 11):
        t.vline(x, 0, 15, HULL[3])


@block("rocket_tank_hull")
def rocket_tank_hull():
    """A propellant tank: painted skin, a welded belt with rivets and a black and white roll pattern."""
    t = Tex(seed=930)
    _hull(t, 930)
    t.hline(0, 15, 7, HULL[2])
    t.hline(0, 15, 8, HULL[5])
    for x in range(1, 16, 3):
        t.set(x, 7, HULL[1])
    for y in range(1, 4):
        for x in range(11, 14):
            t.set(x, y, DARK[1])
    for y in range(12, 15):
        for x in range(2, 5):
            t.set(x, y, DARK[1])
    return t


@block("rocket_engine_hull")
def rocket_engine_hull():
    """The engine bay: bare steel going blue and bronze with heat towards the nozzle."""
    t = Tex(seed=931)
    brushed(t, STEEL, base=3.0, amp=0.7, seed=931, axis="v")
    for y in range(9, 16):
        f = (y - 9) / 6
        for x in range(16):
            t.set(x, y, mix(t.get(x, y), BRASS[2] if (x + y) % 5 else COPPER[1], 0.25 + 0.35 * f))
    t.hline(0, 15, 0, DARK[1])
    t.hline(0, 15, 1, STEEL[5])
    for x in (2, 7, 12):
        bolt(t, x, 3, STEEL)
    return t


@block("rocket_engine_nozzle")
def rocket_engine_nozzle():
    """Nozzle and fins' metal: dark, sooty, scorched."""
    t = Tex(seed=932)
    brushed(t, DARK, base=1.8, amp=0.6, seed=932)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 932, 16, 16, octaves=2, scale=3.0)
            if n > 0.62:
                t.set(x, y, shade(t.get(x, y), 0.7))
    outline(t, 0, 0, 15, 15, DARK[0])
    return t


@block("rocket_ring")
def rocket_ring():
    """The joint rings between sections: dark steel bolted all round."""
    t = Tex(seed=933)
    brushed(t, DARK, base=2.6, amp=0.5, seed=933)
    t.hline(0, 15, 0, DARK[5])
    t.hline(0, 15, 15, DARK[0])
    for x in range(1, 16, 4):
        bolt(t, x, 6, DARK)
    return t


@block("rocket_guidance_hull")
def rocket_guidance_hull():
    """The guidance bay: painted skin with an access hatch and a small star window."""
    t = Tex(seed=934)
    _hull(t, 934)
    outline(t, 3, 2, 12, 13, HULL[1])
    t.hline(4, 11, 3, HULL[6])
    for x, y in ((4, 4), (11, 4), (4, 12), (11, 12)):
        t.set(x, y, STEEL[2])
    t.rect(6, 6, 9, 9, DARK[0])
    t.set(7, 7, CYAN[5])
    t.set(8, 8, CYAN[3])
    return t


@block("rocket_guidance_band")
def rocket_guidance_band():
    """The avionics band round the guidance bay: dark panels and status lights."""
    t = Tex(seed=935)
    brushed(t, DARK, base=2.2, amp=0.4, seed=935)
    t.hline(0, 15, 4, DARK[5])
    t.hline(0, 15, 11, DARK[0])
    for i, x in enumerate(range(1, 16, 3)):
        t.set(x, 7, (CYAN[5], WARN[4], CYAN[4], DANGER[4], CYAN[5], WARN[3])[i % 6])
        t.set(x, 8, DARK[0])
    return t


@block("rocket_warhead_cone")
def rocket_warhead_cone():
    """The re-entry body: olive-grey ablative skin, a red band and a yellow stencil line."""
    t = Tex(seed=936)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 936, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, mix(CONCRETE.t(0.45 + 0.2 * n), RADIUM[1], 0.25))
    t.hline(0, 15, 12, DANGER[2])
    t.hline(0, 15, 13, DANGER[3])
    t.hline(0, 15, 14, DANGER[1])
    t.hline(2, 9, 9, WARN[4])
    return t


@item("star_tracker")
def star_tracker():
    t = Tex(16, 16, 900)
    pts = mask_from(STAR_TRACKER)
    base = {(x, y) for (x, y) in pts if y >= 10}
    tube = pts - base
    sculpt(t, base, DARK, base=3.8, seed=900, grain=0.4)
    sculpt(t, tube, BRASS, base=3.6, seed=901, grain=0.5)
    # Bands round the tube, and the gimbal pin it turns on.
    for x, y in ((5, 6), (6, 5), (7, 4), (9, 3), (10, 2)):
        t.set(x, y, BRASS[1])
    t.set(4, 9, STEEL[6])
    t.set(5, 10, STEEL[5])
    # The objective lens, looking up and out, with a star caught in it.
    for x, y in ((10, 1), (11, 1), (12, 1), (12, 2), (13, 2), (13, 3)):
        t.set(x, y, CYAN[4])
    t.set(12, 1, CYAN[6])
    t.set(11, 2, WHITE)
    # Status lamps on the base.
    t.set(3, 12, RADIUM[6])
    t.set(8, 12, WARN[5])
    glow(t, 11.5, 2.0, 3.5, CYAN[6], 0.3)
    item_outline(t, DARK[0])
    return t


# ======================================================================================
# the radar in the world: an olive housing with a turntable, the dish, and the scope
# ======================================================================================

def _olive_casing(seed):
    t = Tex(seed=seed)
    brushed(t, OLIVE, base=3.3, amp=0.6, seed=seed)
    bevel(t, 0, 0, 15, 15, OLIVE, hi=OLIVE[5], lo=OLIVE[1])
    outline(t, 0, 0, 15, 15, OLIVE[0])
    return t


@block("missile_radar_side")
def missile_radar_side():
    t = _olive_casing(970)
    # Louvres for the transmitter's cooling, and a lamp that shows it is fed.
    for y in (3, 5, 7):
        t.hline(3, 12, y, OLIVE[0])
        t.hline(3, 12, y + 1, OLIVE[5])
    t.rect(3, 10, 8, 13, DARK[1])
    outline(t, 3, 10, 8, 13, OLIVE[0])
    t.set(5, 11, RADIUM[6])
    t.set(6, 11, RADIUM[4])
    t.set(5, 12, RADIUM[3])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, OLIVE)
    return t


@block("missile_radar_top")
def missile_radar_top():
    t = _olive_casing(971)
    # The turntable the dish's mast stands on: a steel ring on a bearing race.
    t.disc(8, 8, 6.6, STEEL[2])
    t.ring(8, 8, 6.6, 5.6, STEEL[5])
    t.ring(8, 8, 4.4, 3.6, STEEL[1])
    t.disc(8, 8, 3.0, DARK[2])
    for a in range(0, 360, 45):
        r = math.radians(a)
        t.set(int(8 + math.cos(r) * 5.0), int(8 + math.sin(r) * 5.0), STEEL[6])
    return t


@block("missile_radar_bottom")
def missile_radar_bottom():
    t = _olive_casing(972)
    t.disc(8, 8, 3.2, DARK[1])
    t.ring(8, 8, 3.2, 2.4, STEEL[4])
    return t


@block("radar_dish_face")
def radar_dish_face():
    """The reflector seen from the front: pale grey panels, ribs radiating from the feed, the rim darker."""
    t = Tex(seed=973)
    light = Ramp("#5e6766", "#7b8584", "#98a2a0", "#b3bcb9", "#c9d1cd", "#dce3de", "#eef2ee")
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2) ** 0.5
            n = fbm(x, y, 973, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, light.t(0.75 - d / 16 + 0.15 * n))
    for a in range(0, 360, 30):
        r = math.radians(a)
        for k in range(2, 8):
            t.set(int(8 + math.cos(r) * k), int(8 + math.sin(r) * k), light[2])
    t.ring(8, 8, 4.6, 3.9, light[3])
    t.disc(8, 8, 1.4, DARK[2])
    return t


@block("radar_dish_back")
def radar_dish_back():
    """Behind the reflector: steel trusses on a grey skin."""
    t = Tex(seed=974)
    brushed(t, STEEL, base=2.8, amp=0.5, seed=974)
    for i in range(16):
        t.set(i, i, STEEL[5])
        t.set(15 - i, i, STEEL[5])
    t.hline(0, 15, 7, STEEL[1])
    t.vline(7, 0, 15, STEEL[1])
    outline(t, 0, 0, 15, 15, STEEL[0])
    return t


@block("radar_scope_front")
def radar_scope_front():
    """The scope's face when nothing is drawn on it: a round phosphor tube dark in an olive bezel."""
    t = _olive_casing(975)
    t.disc(8, 8, 6.9, STEEL[3])
    t.ring(8, 8, 6.9, 6.0, STEEL[5])
    t.disc(8, 8, 5.9, PHOSPHOR[2])
    t.disc(8, 8, 3.0, PHOSPHOR[3])
    t.set(6, 5, PHOSPHOR[5])
    return t


@block("radar_scope_side")
def radar_scope_side():
    t = _olive_casing(976)
    for y in range(3, 13, 2):
        t.hline(4, 11, y, OLIVE[1])
    return t


# ======================================================================================
# the holographic command table: a dark console whose top is a projection glass
# ======================================================================================

HOLO = Ramp("#020a0c", "#05161a", "#0a242a", "#10363e", "#1a4e58", "#2a7480", "#5ad8e8")


@block("holo_table_glass")
def holo_table_glass():
    """Projection glass: dark blue-black, a faint cyan lattice that runs on across the pieces without a seam."""
    t = Tex(seed=980)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 980, 16, 16, octaves=2, scale=3.0)
            t.set(x, y, mix(HOLO[1], HOLO[2], 0.35 + 0.35 * n))
    for i in (0, 8):
        for k in range(16):
            t.set(i, k, mix(t.get(i, k), HOLO[4], 0.28))
            t.set(k, i, mix(t.get(k, i), HOLO[4], 0.28))
    return t


@block("holo_table_rim")
def holo_table_rim():
    """The frame round the glass: dark steel with a brass lip along it."""
    t = Tex(seed=982)
    brushed(t, DARK, base=3.2, amp=0.6, seed=982)
    t.hline(0, 15, 0, DARK[5])
    t.hline(0, 15, 1, BRASS[4])
    t.hline(0, 15, 2, BRASS[2])
    t.hline(0, 15, 13, BRASS[3])
    t.hline(0, 15, 14, BRASS[1])
    t.hline(0, 15, 15, DARK[0])
    for x in (2, 13):
        t.set(x, 7, DARK[6])
        t.set(x, 8, DARK[1])
    return t


@block("holo_table_casing")
def holo_table_casing():
    """The console's panels, legs and stretchers: dark industrial casing with a cyan status slot."""
    t = Tex(seed=981)
    casing(t, DARK, seed=981, base=2.6, amp=0.6)
    t.rect(6, 7, 9, 8, HOLO[3])
    outline(t, 5, 6, 10, 9, DARK[0])
    t.set(7, 7, HOLO[6])
    t.set(8, 7, HOLO[5])
    return t


@block("holo_table_lens")
def holo_table_lens():
    """The projector's emitter: rings of cyan glass round a bright core."""
    t = Tex(seed=983)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2:
                c = HOLO[6]
            elif d < 4:
                c = mix(HOLO[5], HOLO[6], 0.4 if int(d * 2) % 2 else 0.0)
            elif d < 6:
                c = mix(HOLO[3], HOLO[5], 0.5 if int(d * 2) % 2 else 0.0)
            else:
                c = BRASS[2] if d < 7.2 else DARK[1]
            t.set(x, y, c)
    return t


# ======================================================================================
# the radar screen: an olive console round a phosphor scope (RadarLayout.java holds the same numbers)
# ======================================================================================

# Olive-drab enamel of a military console, and the phosphor scope's greens.
OLIVE = Ramp("#14180f", "#1e2418", "#2a3221", "#38412c", "#48533a", "#5d694b", "#7a8664")
PHOSPHOR = Ramp("#020803", "#041207", "#071d0c", "#0b2a12", "#113a19", "#1a4d22", "#256230")
RADAR_W, RADAR_H = 384, 256
R_TITLE = (6, 4, 372, 13)
R_PPI = (6, 20, 212, 212)
PPI_CX, PPI_CY, PPI_RADIUS, BEZEL = 112, 126, 99, 105
R_ZOOM_OUT = (6, 236, 15, 15)
R_ZOOM_IN = (23, 236, 15, 15)
R_RANGE = (40, 236, 64, 15)
R_CENTRE = (106, 236, 30, 15)
R_ZONE = (138, 236, 80, 15)
R_TABLE = (224, 20, 154, 92)
TABLE_HEADER, TABLE_ROW, TABLE_ROWS = 11, 10, 8
R_DETAIL = (224, 116, 154, 62)
R_PROFILE = (224, 182, 154, 50)
R_CODE = (224, 236, 44, 15)
R_FILTER = (270, 236, 50, 15)
R_ACK = (322, 236, 56, 15)


def _box(r):
    x, y, w, h = r
    return x, y, x + w - 1, y + h - 1


def _glass(t, r, ramp=PHOSPHOR, seed=0):
    """A recessed phosphor window: dark glass with faint grain, sunk into the enamel."""
    x0, y0, x1, y1 = _box(r)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, RADAR_W, RADAR_H, octaves=3, scale=2.0)
            t.set(x, y, mix(ramp[1], ramp[3], 0.35 + 0.25 * n))
    t.hline(x0, x1, y0, OLIVE[0])
    t.vline(x0, y0, y1, OLIVE[0])
    t.hline(x0 + 1, x1, y0 + 1, PHOSPHOR[0])
    t.vline(x0 + 1, y0 + 1, y1, PHOSPHOR[0])
    t.hline(x0, x1, y1, OLIVE[5])
    t.vline(x1, y0, y1, OLIVE[5])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[1])


def _button(t, r, face=OLIVE, glyph=None):
    """A raised push button; `glyph` paints its legend."""
    x0, y0, x1, y1 = _box(r)
    t.rect(x0, y0, x1, y1, face[3])
    for y in range(y0, y1 + 1):
        t.hline(x0, x1, y, mix(face[4], face[2], (y - y0) / max(1, y1 - y0)))
    bevel(t, x0, y0, x1, y1, face, hi=face[6], lo=face[1])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[0])
    if glyph:
        glyph(t, (x0 + x1) // 2, (y0 + y1) // 2)


def _minus(t, cx, cy):
    t.hline(cx - 3, cx + 3, cy, RADIUM[6])


def _plus(t, cx, cy):
    t.hline(cx - 3, cx + 3, cy, RADIUM[6])
    t.vline(cx, cy - 3, cy + 3, RADIUM[6])


def _crosshair(t, cx, cy):
    t.ring(cx + 0.5, cy + 0.5, 4.2, 3.2, RADIUM[6])
    t.set(cx, cy, RADIUM[6])
    for d in (-6, 6):
        t.set(cx + d, cy, RADIUM[5])
        t.set(cx, cy + d, RADIUM[5])


def _radar_panel():
    t = Tex(RADAR_W, RADAR_H, 960)
    brushed(t, OLIVE, base=3.3, amp=0.55, seed=960, scale=11.0)
    # A raised outer frame and the heavy screws of a field console.
    bevel(t, 0, 0, RADAR_W - 1, RADAR_H - 1, OLIVE, hi=OLIVE[6], lo=OLIVE[0])
    bevel(t, 1, 1, RADAR_W - 2, RADAR_H - 2, OLIVE, hi=OLIVE[5], lo=OLIVE[1])
    outline(t, 0, 0, RADAR_W - 1, RADAR_H - 1, OLIVE[0])
    for x, y in ((2, 2), (RADAR_W - 4, 2), (2, RADAR_H - 4), (RADAR_W - 4, RADAR_H - 4), (220, 2), (220, RADAR_H - 4)):
        bolt(t, x, y, OLIVE)
    # Title strip, with a lamp socket at its right end.
    x0, y0, x1, y1 = _box(R_TITLE)
    t.rect(x0, y0, x1, y1, OLIVE[1])
    for x in range(x0, x1 + 1, 4):
        t.set(x, y1, OLIVE[2])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[0])
    t.hline(x0 - 1, x1 + 1, y1 + 2, OLIVE[5])
    t.disc(x1 - 6 + 0.5, y0 + 6 + 0.5, 4.0, OLIVE[0])
    t.disc(x1 - 6 + 0.5, y0 + 6 + 0.5, 3.0, mix(DARK[1], RADIUM[1], 0.4))
    # The scope: a sunk square holding a steel bezel and the round phosphor glass.
    x0, y0, x1, y1 = _box(R_PPI)
    t.rect(x0, y0, x1, y1, OLIVE[1])
    outline(t, x0, y0, x1, y1, OLIVE[0])
    t.hline(x0, x1, y1, OLIVE[4])
    t.vline(x1, y0, y1, OLIVE[4])
    for y in range(y0 + 1, y1):
        for x in range(x0 + 1, x1):
            d = ((x + 0.5 - PPI_CX) ** 2 + (y + 0.5 - PPI_CY) ** 2) ** 0.5
            if d <= PPI_RADIUS:
                # Phosphor glass, a shade brighter in the middle as a lit tube is.
                n = fbm(x, y, 961, RADAR_W, RADAR_H, octaves=3, scale=2.0)
                glow_ = max(0.0, 1.0 - d / PPI_RADIUS)
                t.set(x, y, mix(PHOSPHOR[0], PHOSPHOR[4], 0.15 + 0.55 * glow_ * glow_ + 0.12 * n))
            elif d <= BEZEL + 0.5:
                # Steel bezel lit from the top left: bright where its face looks that way.
                ang = math.atan2(y + 0.5 - PPI_CY, x + 0.5 - PPI_CX)
                light = -math.cos(ang + math.pi / 4)
                ramp = STEEL if d > PPI_RADIUS + 1.5 else DARK
                k = 3.0 + 1.6 * light * (1 if d > (PPI_RADIUS + BEZEL) / 2 else -0.6)
                t.set(x, y, ramp[int(round(max(1, min(6, k))))])
            elif d <= BEZEL + 1.5:
                t.set(x, y, OLIVE[0])
    # Bearing ticks round the bezel: every 10°, longer and brighter every 30°.
    for deg in range(0, 360, 10):
        a = math.radians(deg)
        major = deg % 30 == 0
        r0 = PPI_RADIUS + 1.2
        r1 = PPI_RADIUS + (4.6 if major else 2.8)
        for k in range(8):
            r = r0 + (r1 - r0) * k / 7
            t.set(int(PPI_CX + math.sin(a) * r), int(PPI_CY - math.cos(a) * r), RADIUM[6] if major else STEEL[6])
    for x, y in ((x0 + 2, y0 + 2), (x1 - 3, y0 + 2), (x0 + 2, y1 - 3), (x1 - 3, y1 - 3)):
        bolt(t, x, y, OLIVE)
    # Under the scope: zoom, range readout, recentre and the zone switch.
    _button(t, R_ZOOM_OUT, glyph=_minus)
    _button(t, R_ZOOM_IN, glyph=_plus)
    _glass(t, R_RANGE, seed=962)
    _button(t, R_CENTRE, glyph=_crosshair)
    _button(t, R_ZONE)
    zx0, zy0, zx1, zy1 = _box(R_ZONE)
    t.ring(zx0 + 8.5, (zy0 + zy1) / 2 + 0.5, 4.3, 3.3, WARN[5])
    # The track table: a header band and faint rules between rows.
    _glass(t, R_TABLE, seed=963)
    x0, y0, x1, y1 = _box(R_TABLE)
    t.rect(x0 + 2, y0 + 2, x1 - 1, y0 + TABLE_HEADER - 1, PHOSPHOR[4])
    t.hline(x0 + 2, x1 - 1, y0 + TABLE_HEADER, PHOSPHOR[6])
    for row in range(1, TABLE_ROWS):
        y = y0 + TABLE_HEADER + row * TABLE_ROW
        for x in range(x0 + 3, x1 - 1, 2):
            t.set(x, y, PHOSPHOR[4])
    # Detail panel, and the side view with its grid.
    _glass(t, R_DETAIL, seed=964)
    _glass(t, R_PROFILE, seed=965)
    x0, y0, x1, y1 = _box(R_PROFILE)
    for gx in range(x0 + 2, x1, 15):
        for y in range(y0 + 2, y1, 2):
            t.set(gx, y, PHOSPHOR[4])
    for gy in range(y1 - 4, y0 + 1, -10):
        for x in range(x0 + 2, x1, 2):
            t.set(x, gy, PHOSPHOR[4])
    # Network code window, filter switch and the alarm acknowledge button in its guard.
    _glass(t, R_CODE, seed=966)
    _button(t, R_FILTER)
    x0, y0, x1, y1 = _box(R_ACK)
    hazard(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, WARN[5], DARK[0], period=6, seed=967)
    _button(t, (R_ACK[0] + 2, R_ACK[1] + 2, R_ACK[2] - 4, R_ACK[3] - 4), face=Ramp(*DANGER[:]))
    return t


@gui("radar_screen")
def radar_screen():
    return _radar_panel()


@gui("radar_scope_mask")
def radar_scope_mask():
    """The scope's square of the panel with its glass cut out: drawn over the picture, it rounds it off under the bezel."""
    panel = _radar_panel()
    x, y, w, h = R_PPI
    t = Tex(w, h, 968)
    for j in range(h):
        for i in range(w):
            d = ((x + i + 0.5 - PPI_CX) ** 2 + (y + j + 0.5 - PPI_CY) ** 2) ** 0.5
            if d > PPI_RADIUS:
                t.set(i, j, panel.get(x + i, y + j))
    return t

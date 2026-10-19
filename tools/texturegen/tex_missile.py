"""Textures of the missile area: the silo, the rocket sections, the radar, its scope and the holographic command table.

Register block textures with ``@block``, items with ``@item`` and screens with ``@gui``; this module is imported at the end of
``blocks.py``.
"""

from blocks import block  # noqa: F401
from common import bolt, flange
from items import item, mask_from, sculpt
from palette import BRASS, CONCRETE, CYAN, DARK, IN_ACCENT, RADIUM, STEEL, WARN, WHITE
from pixelkit import Tex, bevel, fbm, glow, hazard, item_outline, mix, outline, shade


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


@block("silo_fuel_port")
def silo_fuel_port():
    t = Tex(seed=911)
    _silo_concrete(t, 911)
    outline(t, 0, 0, 15, 15, DARK)
    # A steel plate set in the wall, a flanged inlet and the yellow-black of a fuelling point.
    t.rect(2, 2, 13, 13, STEEL[3])
    bevel(t, 2, 2, 13, 13, STEEL)
    outline(t, 2, 2, 13, 13, STEEL[0])
    hazard(t, 3, 3, 12, 4, WARN[5], DARK[0], period=4, seed=911)
    hazard(t, 3, 11, 12, 12, WARN[5], DARK[0], period=4, seed=912)
    flange(t, STEEL, IN_ACCENT, cx=8.0, cy=8.0, r=3.6)
    t.set(8, 8, IN_ACCENT[5])
    t.set(7, 7, IN_ACCENT[6])
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

"""Textures of the missile area: the silo, the rocket sections, the radar, its scope and the holographic command table.

Register block textures with ``@block``, items with ``@item`` and screens with ``@gui``; this module is imported at the end of
``blocks.py``.
"""

from blocks import block  # noqa: F401
from items import item, mask_from, sculpt
from palette import BRASS, CYAN, DARK, RADIUM, STEEL, WARN, WHITE
from pixelkit import Tex, glow, item_outline


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

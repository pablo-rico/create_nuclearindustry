"""Textures of the launch base and the silo fuel port.

A loose launch base is a concrete pad; once a controller forms a 3x3 of them each draws its ninth of one launch table from the
``launch_base_formed`` sheet (96x96, listed in ``blocks.SHEET_BLOCKS``), whose top face is mapped by part in
``models/block/launch_base/formed_<part>.json``. Register block textures with ``@block``; this module is imported at the end of
``blocks.py``.
"""

from blocks import block  # noqa: F401
from common import bolt, flange
from palette import CONCRETE, DARK, IN_ACCENT, STEEL, WARN
from pixelkit import Tex, bevel, fbm, hazard, mix, outline, shade

from tex_launch import BLACK_PAINT, YELLOW
from tex_missile import _silo_concrete


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
# the launch platform: a steel deck over the flame trench, hazard-painted walkways at the seams
# ======================================================================================

@block("launch_base_top")
def launch_base_top():
    """One of the pad's nine plates. The hazard band round the edge runs on into the next plate (its stripes are laid on
    world-aligned diagonals that repeat every 8 pixels), so a 3x3 pad reads as nine deflector grates between walkways."""
    t = Tex(seed=940)
    _silo_concrete(t, 940)
    # Walkway edge: 1 px of worn yellow-black that meets the neighbour's, round a concrete margin.
    for y in range(16):
        for x in range(16):
            if x < 1 or x > 14 or y < 1 or y > 14:
                c = YELLOW if ((x + y) % 8) < 4 else BLACK_PAINT
                n = fbm(x, y, 941, 16, 16, octaves=2, scale=4.0)
                if n > 0.66:
                    c = mix(c, CONCRETE[3], 0.55)
                t.set(x, y, c)
    # The deck's steel frame, bolted at its corners, round the deflector grate.
    t.rect(2, 2, 13, 13, STEEL[3])
    bevel(t, 2, 2, 13, 13, STEEL, hi=STEEL[5], lo=STEEL[1])
    t.frame(3, 3, 12, 12, STEEL[4])
    t.hline(4, 11, 12, STEEL[2])
    t.vline(12, 4, 12, STEEL[2])
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        bolt(t, x, y, STEEL)
    # The grate: slots down into the trench, scorched at the bottom of each.
    t.rect(5, 5, 10, 10, DARK[0])
    for x in range(5, 11):
        if x % 2 == 1:
            for y in range(5, 11):
                t.set(x, y, STEEL[4] if y == 5 else (STEEL[3] if y < 9 else STEEL[2]))
            t.set(x, 10, DARK[2])
        else:
            t.set(x, 5, DARK[1])
            t.set(x, 9, mix(DARK[0], (120, 64, 36, 255), 0.45))
            t.set(x, 10, mix(DARK[1], (150, 80, 40, 255), 0.35))
    t.hline(5, 10, 4, DARK[1])
    t.vline(4, 4, 10, DARK[1])
    t.hline(4, 11, 11, STEEL[5])
    t.vline(11, 5, 11, STEEL[5])
    return t


@block("launch_base_side")
def launch_base_side():
    """The pad's edge: a poured slab under the steel deck's cap band, its bolts and a drain slot."""
    t = Tex(seed=942)
    _silo_concrete(t, 942)
    # Cap band: the deck's edge plate.
    t.rect(0, 0, 15, 3, STEEL[3])
    t.hline(0, 15, 0, STEEL[5])
    t.hline(0, 15, 1, STEEL[4])
    t.hline(0, 15, 3, STEEL[1])
    t.hline(0, 15, 4, DARK[1])
    for x in (2, 12):
        bolt(t, x, 1, STEEL)
    # A hazard line just under the cap, and the slab's weathering beneath.
    for x in range(16):
        t.set(x, 5, YELLOW if (x % 4) < 2 else BLACK_PAINT)
    for x in range(16):
        t.set(x, 6, shade(t.get(x, 6), 0.82))
    t.rect(6, 10, 9, 11, DARK[1])
    t.hline(6, 9, 10, DARK[0])
    t.hline(6, 9, 12, CONCRETE[5])
    for x, y in ((7, 12), (8, 13), (7, 14)):
        t.set(x, y, shade(t.get(x, y), 0.78))
    t.hline(0, 15, 15, CONCRETE[1])
    return t


@block("launch_base_formed")
def launch_base_formed():
    """The formed table's top, nine plates of 32 pixels: for now the loose pad's plate, doubled and tiled."""
    plate = launch_base_top()
    t = Tex(96, 96, 943)
    for y in range(96):
        for x in range(96):
            t.set(x, y, plate.get((x % 32) // 2, (y % 32) // 2))
    return t

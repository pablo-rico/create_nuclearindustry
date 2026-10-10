"""Textures for spent fuel racks and dry casks.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.
"""

import math

from blocks import block  # noqa: F401
from common import bolt
from gui import gui  # noqa: F401
from items import item  # noqa: F401
from palette import BRASS, CONCRETE, COPPER, DARK, LEAD, MIMIC_BLUE, STEEL, URANIUM, WARN
from pixelkit import Tex, bevel, brushed, outline, rivet, shade, speckle, vents

# The rack is a 3x3 grid of square cells: borated steel walls on these columns and rows.
WALLS = (0, 5, 10, 15)
CELLS = ((1, 4), (6, 9), (11, 14))


def _rack_grid(t, seed):
    """Steel cell walls with dark, recessed cells between them."""
    brushed(t, STEEL, base=3.0, amp=0.6, seed=seed)
    for x0, x1 in CELLS:
        for y0, y1 in CELLS:
            t.rect(x0, y0, x1, y1, DARK[0])
            t.hline(x0, x1, y0, DARK[1])
            t.vline(x0, y0, y1, DARK[1])
    # Lit from the top-left: the first walls catch the light, the far ones are in shade.
    for w in WALLS:
        t.vline(w, 0, 15, STEEL[5] if w in (0, 5) else STEEL[3])
        t.hline(0, 15, w, STEEL[5] if w in (0, 5) else STEEL[3])
    outline(t, 0, 0, 15, 15, STEEL[1])
    return t


def _assembly_heads(t, nozzle, gap):
    """The top nozzle of an assembly in every cell: a square head with its handling hole."""
    for x0, x1 in CELLS:
        for y0, y1 in CELLS:
            t.rect(x0, y0, x1, y1, gap)
            t.rect(x0 + 1, y0, x1, y1 - 1, nozzle[3])
            t.hline(x0 + 1, x1, y0, nozzle[5])
            t.vline(x0 + 1, y0, y1 - 1, nozzle[4])
            t.vline(x1, y0, y1 - 1, nozzle[2])
            t.set(x0 + 2, y0 + 1, nozzle[0])
            t.set(x0 + 3, y0 + 2, nozzle[1])


@block("spent_fuel_rack_top")
def spent_fuel_rack_top():
    return _rack_grid(Tex(seed=301), 301)


@block("spent_fuel_rack_top_loaded")
def spent_fuel_rack_top_loaded():
    t = _rack_grid(Tex(seed=302), 302)
    _assembly_heads(t, LEAD, DARK[0])
    return t


@block("spent_fuel_rack_top_glowing")
def spent_fuel_rack_top_glowing():
    # Cherenkov light: the water between fresh assemblies glows blue.
    t = _rack_grid(Tex(seed=303), 303)
    _assembly_heads(t, LEAD, MIMIC_BLUE[5])
    for x0, x1 in CELLS:
        for y0, y1 in CELLS:
            t.set(x0, y1, MIMIC_BLUE[6])
            t.set(x1, y1, MIMIC_BLUE[4])
    for w in (5, 10):
        for i in range(1, 15):
            t.blend(w, i, MIMIC_BLUE[4], 0.35)
            t.blend(i, w, MIMIC_BLUE[4], 0.35)
    return t


@block("spent_fuel_rack_side")
def spent_fuel_rack_side():
    # Open-sided cells: the long assemblies show through slots in the boron steel.
    t = Tex(seed=304)
    brushed(t, STEEL, base=3.0, amp=0.6, seed=304, axis="v")
    for x0, x1 in CELLS:
        t.rect(x0, 2, x1, 13, DARK[0])
        t.rect(x0 + 1, 2, x1 - 1, 13, LEAD[2])
        t.vline(x0 + 1, 2, 13, LEAD[4])
        t.vline(x1 - 1, 2, 13, LEAD[1])
        for y in (4, 8, 12):
            t.hline(x0 + 1, x1 - 1, y, LEAD[5])
    for y in (0, 1, 14, 15):
        t.hline(0, 15, y, STEEL[4] if y in (1, 14) else STEEL[2])
    for x in WALLS:
        t.vline(x, 0, 15, STEEL[5] if x < 8 else STEEL[3])
    # Boron plate markings.
    for x0, _ in CELLS:
        t.set(x0 + 1, 1, URANIUM[4])
    outline(t, 0, 0, 15, 15, STEEL[1])
    return t


@block("spent_fuel_rack_bottom")
def spent_fuel_rack_bottom():
    t = Tex(seed=305)
    brushed(t, STEEL, base=2.8, amp=0.7, seed=305)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    # Flow holes under each cell.
    for x0, x1 in CELLS:
        for y0, y1 in CELLS:
            cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
            t.set(cx, cy, DARK[0])
            t.set(cx + 1, cy, DARK[1])
            t.set(cx, cy + 1, DARK[1])
            t.set(cx + 1, cy + 1, STEEL[4])
    return t


@block("pool_cooler_side")
def pool_cooler_side():
    # Copper heat exchanger fins in a steel frame.
    t = Tex(seed=306)
    brushed(t, STEEL, base=3.0, amp=0.6, seed=306)
    t.rect(2, 2, 13, 13, DARK[1])
    for y in range(3, 13, 2):
        for x in range(3, 13):
            f = 0.85 + 0.3 * math.sin(math.pi * (x - 2.5) / 11)
            t.set(x, y, shade(COPPER[4], f))
            t.set(x, y + 1, shade(COPPER[1], f))
    bevel(t, 1, 1, 14, 14, STEEL, hi=STEEL[1], lo=STEEL[5])
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    return t


@block("pool_cooler_top")
def pool_cooler_top():
    # Plate with the drive shaft socket in the middle, like Create's machines.
    t = Tex(seed=307)
    brushed(t, STEEL, base=3.2, amp=0.6, seed=307)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    t.disc(8, 8, 4.6, BRASS[2])
    t.ring(8, 8, 4.6, 3.6, BRASS[4])
    t.disc(8, 8, 2.6, DARK[0])
    t.rect(7, 6, 8, 9, DARK[2])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, STEEL)
    return t


@block("pool_cooler_bottom")
def pool_cooler_bottom():
    # Intake grille drawing pool water up through the exchanger.
    t = Tex(seed=308)
    brushed(t, STEEL, base=3.0, amp=0.6, seed=308)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    t.rect(3, 3, 12, 12, DARK[0])
    vents(t, 3, 3, 12, 12, DARK, step=1, dark=DARK[0], lite=STEEL[2])
    outline(t, 2, 2, 13, 13, STEEL[1])
    return t


def _concrete(t, seed):
    brushed(t, CONCRETE, base=3.3, amp=0.7, seed=seed, axis="v", scale=3.0)
    speckle(t, 0, 0, 15, 15, CONCRETE[2], chance=0.06, seed=seed)
    speckle(t, 0, 0, 15, 15, CONCRETE[5], chance=0.04, seed=seed + 1)


@block("dry_cask_side")
def dry_cask_side():
    # Concrete overpack, rounded by shading, with steel bands and air vents top and bottom.
    t = Tex(seed=309)
    _concrete(t, 309)
    for x in range(16):
        f = 0.74 + 0.5 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    for y in (0, 15):
        for x in range(16):
            t.set(x, y, shade(STEEL[3], 0.8 + 0.3 * math.sin(math.pi * (x + 0.5) / 16)))
    for y0 in (2, 12):
        for x0 in (3, 7, 11):
            t.rect(x0, y0, x0 + 1, y0 + 1, DARK[0])
            t.set(x0, y0, DARK[1])
    # A yellow-and-black hazard band round the middle.
    for x in range(16):
        f = 0.8 + 0.3 * math.sin(math.pi * (x + 0.5) / 16)
        for y in (7, 8):
            t.set(x, y, shade(WARN[4] if (x + y) % 4 < 2 else DARK[1], f))
        t.set(x, 6, shade(STEEL[2], f))
        t.set(x, 9, shade(STEEL[1], f))
    return t


@block("dry_cask_top")
def dry_cask_top():
    # Bolted steel lid in its concrete collar.
    t = Tex(seed=310)
    _concrete(t, 310)
    t.disc(8, 8, 6.4, STEEL[3])
    t.ring(8, 8, 6.4, 5.6, STEEL[1])
    t.disc(8, 8, 4.0, STEEL[4])
    t.ring(8, 8, 4.0, 3.4, STEEL[2])
    for i in range(8):
        a = i * math.pi / 4
        rivet(t, int(8 + 4.9 * math.cos(a)), int(8 + 4.9 * math.sin(a)), STEEL)
    t.rect(7, 7, 8, 8, DARK[1])
    outline(t, 0, 0, 15, 15, CONCRETE[1])
    return t

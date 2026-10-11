"""Textures for the renewable fuel cycle: rock fines, heavy mineral sand, the sea-water adsorbent,
decontamination sludge, the thorium fuel and its uranium-233, and the irradiation source block.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.
"""

import math
import random

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from gui import gui  # noqa: F401
from items import (GRANULES, PILE, _fuel_bundle, _ingot, _powder, item, mask_from, rect_pts,
                   sculpt, solid)
from palette import (CARBONATE, DARK, FLUORITE, LEAD, RADIUM, STEEL, STONE, UO2, URANIUM, WARN, WHITE)
from pixelkit import Ramp, Tex, bevel, brushed, glow, item_outline, speckle

# ------------------------------------------------------------------ colours

GRANITE = Ramp("#3f2a26", "#5e3d35", "#7d5245", "#9a6a58", "#b4836e", "#cc9e88", "#e2bca7")
TUFF = Ramp("#33362f", "#4a4e44", "#61665a", "#787d6f", "#8f9584", "#a8ad9b", "#c2c6b5")
DEEPSLATE = Ramp("#1c1d22", "#2a2b32", "#383a43", "#484a54", "#5a5c66", "#6e717b", "#878a93")
# Monazite (red-brown) and zircon in a dark sand.
SANDS = Ramp("#2d2620", "#43382c", "#5b4b39", "#746049", "#8f775b", "#ab9171", "#c8ae8c")
MONAZITE = Ramp("#4b2117", "#6e301e", "#924428", "#b15b35", "#c97a4e", "#dd9c70", "#efc19d")
# Thorium dioxide is a white ceramic powder.
THORIA = CARBONATE.tinted(LEAD[4], 0.25)
# Protactinium-233 makes freshly irradiated thorium amber.
PROTACTINIUM = Ramp("#4c3613", "#6f4f1b", "#966d25", "#ba8c32", "#d6ab4a", "#e9c875", "#f7e4ac")
FIBRE = Ramp("#4a4537", "#6a6450", "#8c856b", "#ada685", "#c8c2a2", "#e0dbbf", "#f3f0dc")
SLUDGE = Ramp("#1a1f14", "#262e1b", "#344023", "#45542c", "#586a37", "#6e8244", "#889c55")
# Uranium-233 oxide: dark, with the hard glow of its uranium-232 daughters.
U233 = UO2.tinted(URANIUM[2], 0.35)

# ------------------------------------------------------------------ rock and sand


def _flecks(t, shape, seed, flecks):
    """Scatter grains over a silhouette's interior: ``flecks`` is a list of (colour, chance)."""
    rng = random.Random(seed)
    pts = mask_from(shape)
    for (x, y) in sorted(pts):
        if (x, y - 1) not in pts or (x, y + 1) not in pts:
            continue
        roll = rng.random()
        for colour, chance in flecks:
            if roll < chance:
                t.set(x, y, colour)
                break
            roll -= chance
    return t


@item("granite_fines")
def granite_fines():
    # Pink feldspar grit with glints of quartz and the odd dark mica flake.
    t = _powder(911, GRANITE, grain=1.3, sparkle=WHITE, sparkle_chance=0.10, shape=GRANULES)
    for (x, y) in ((5, 9), (9, 7), (11, 10)):
        t.set(x, y, DARK[1])
    return t


@item("tuff_fines")
def tuff_fines():
    # Grey-green volcanic ash with pale pumice flecks.
    return _powder(912, TUFF, grain=1.4, sparkle=TUFF[6], sparkle_chance=0.16, shape=GRANULES)


@item("deepslate_fines")
def deepslate_fines():
    # Blue-black shale grit with violet fluorite specks.
    t = _powder(913, DEEPSLATE, grain=1.3, shape=GRANULES)
    return _flecks(t, GRANULES, 913, [(FLUORITE[5], 0.08), (FLUORITE[3], 0.05)])


@item("heavy_mineral_sand")
def heavy_mineral_sand():
    # Dark placer sand speckled with red-brown monazite and glassy zircon.
    t = _powder(914, SANDS, grain=1.5, shape=PILE)
    return _flecks(t, PILE, 914, [(MONAZITE[4], 0.16), (MONAZITE[2], 0.08), (STONE[6], 0.07)])


# ------------------------------------------------------------------ sea water

SKEIN = [
    "................",
    "................",
    "................",
    "....######......",
    "...########.....",
    "..###....###....",
    "..##......##....",
    "..##......###...",
    "..###......##...",
    "...###.....##...",
    "....###...###...",
    ".....########...",
    "......######....",
    "................",
    "................",
    "................",
]


def _skein(seed, ramp, fleck=None):
    """A loop of fibre: strands lit across the loop, the odd fleck of what it has picked up."""
    t = Tex(16, 16, seed)
    pts = mask_from(SKEIN)
    sculpt(t, pts, ramp, base=3.9, seed=seed, grain=0.4)
    for (x, y) in sorted(pts):
        if (x + 2 * y) % 4 == 0:
            t.set(x, y, ramp[5])
        elif (x + 2 * y) % 4 == 2:
            t.set(x, y, ramp[2])
    if fleck is not None:
        for (x, y) in ((3, 6), (11, 8), (6, 11), (9, 4)):
            if (x, y) in pts:
                t.set(x, y, fleck)
    item_outline(t, ramp[0])
    return t


@item("amidoxime_adsorbent")
def amidoxime_adsorbent():
    return _skein(915, FIBRE)


@item("loaded_adsorbent")
def loaded_adsorbent():
    return _skein(916, FIBRE.tinted(URANIUM[4], 0.45), fleck=RADIUM[5])


# ------------------------------------------------------------------ decontamination

BLOB = [
    "................",
    "................",
    "................",
    "................",
    "........##......",
    "......#####.....",
    ".....#######....",
    "....#########...",
    "...##########...",
    "..############..",
    ".##############.",
    ".##############.",
    "..####..#######.",
    "...##....####...",
    "................",
    "................",
]


@item("radioactive_sludge")
def radioactive_sludge():
    # A wet heap of dark sludge with a sheen and a sickly glow.
    t = solid(BLOB, SLUDGE, seed=917, base=3.4, grain=1.0, outline=False)
    pts = mask_from(BLOB)
    for (x, y) in ((5, 10), (8, 12), (11, 9), (12, 11), (7, 8), (3, 11)):
        if (x, y) in pts:
            t.set(x, y, RADIUM[5])
    for (x, y) in ((6, 7), (7, 7), (9, 9)):
        t.set(x, y, SLUDGE[6])
    glow(t, 8, 10, 6.0, RADIUM[6], 0.16)
    item_outline(t, SLUDGE[0])
    return t


# ------------------------------------------------------------------ thorium and uranium-233


@item("thorium_dioxide")
def thorium_dioxide():
    return _powder(918, THORIA, grain=0.8, sparkle=WHITE, sparkle_chance=0.08)


@item("irradiated_thorium")
def irradiated_thorium():
    # Thoria turned amber by protactinium-233, with a faint glow.
    t = _powder(919, PROTACTINIUM, grain=1.0, sparkle=RADIUM[6], sparkle_chance=0.10)
    glow(t, 8, 10, 6.0, RADIUM[6], 0.14)
    return t


@item("irradiated_depleted_uranium")
def irradiated_depleted_uranium():
    # A dull depleted uranium ingot flecked where neutrons have bred plutonium.
    t = _ingot(920, URANIUM.tinted(LEAD[4], 0.55), mark=[(9, 5), (7, 7), (5, 9), (8, 9)], mark_glow=RADIUM)
    return t


@item("uranium_233_dioxide")
def uranium_233_dioxide():
    t = _powder(921, U233, grain=0.9)
    _flecks(t, PILE, 921, [(RADIUM[6], 0.06), (RADIUM[4], 0.06)])
    glow(t, 8, 10, 6.0, RADIUM[6], 0.18)
    return t


@item("thorium_fuel_pellet")
def thorium_fuel_pellet():
    # A sintered thoria cylinder, like the uranium pellet but white, with its seed showing.
    t = Tex(16, 16, 922)
    pts = rect_pts(5, 3, 10, 12)
    sculpt(t, pts, THORIA, base=3.8, seed=922, grain=0.35)
    for y in range(3, 13):
        t.set(5, y, THORIA[5])
        t.set(6, y, THORIA[4])
        t.set(10, y, THORIA[1])
    t.hline(5, 10, 3, THORIA[6])
    t.hline(5, 10, 4, THORIA[4])
    t.hline(5, 10, 12, THORIA[1])
    for y in (6, 9):
        t.hline(6, 9, y, THORIA[2])
    for (x, y) in ((7, 5), (8, 8), (7, 10)):
        t.set(x, y, UO2[3])
    t.set(8, 7, RADIUM[5])
    item_outline(t, THORIA[0])
    return t


@item("thorium_reactor_fuel")
def thorium_reactor_fuel():
    return _fuel_bundle(923, THORIA, STEEL, glow_ramp=RADIUM)


@item("incomplete_thorium_reactor_fuel")
def incomplete_thorium_reactor_fuel():
    t = _fuel_bundle(926, THORIA, STEEL)
    # Only the left half of the cladding is filled so far.
    for x in range(8, 13):
        for y in range(3, 12):
            t.set(x, y, DARK[2] if x % 2 else DARK[1])
    for y in range(3, 12):
        t.set(12, y, DARK[0])
    return t


@item("depleted_thorium_reactor_fuel")
def depleted_thorium_reactor_fuel():
    dull = THORIA.tinted(PROTACTINIUM[2], 0.5)
    return _fuel_bundle(924, dull, STEEL.tinted(DARK[3], 0.4), glow_ramp=None, spent=True)


@item("thorium_spent_fragments")
def thorium_spent_fragments():
    # Sheared cladding mixed with amber-stained thoria.
    t = _powder(925, THORIA.tinted(PROTACTINIUM[3], 0.45), grain=1.3, sparkle=RADIUM[4], sparkle_chance=0.06)
    for (x, y) in ((4, 9), (5, 9), (9, 7), (10, 7), (11, 7), (7, 11), (8, 11)):
        t.set(x, y, STEEL[5])
    return t


# ------------------------------------------------------------------ the irradiation source


@block("irradiation_source_frame")
def irradiation_source_frame():
    # Dark steel bars; the rails (top and bottom rows) carry yellow-and-black hazard bands.
    t = Tex(seed=930)
    brushed(t, DARK, base=3.2, amp=0.8, seed=930, axis="v")
    for y in (0, 1, 14, 15):
        for x in range(16):
            t.set(x, y, WARN[4] if ((x + y) % 4) < 2 else DARK[0])
    for x in (0, 15):
        for y in range(2, 14):
            t.set(x, y, DARK[1] if x else DARK[5])
    bevel(t, 0, 0, 15, 15, DARK, hi=DARK[5], lo=DARK[0], corner=False)
    return t


@block("irradiation_source_capsule")
def irradiation_source_capsule():
    # A lead capsule with a glowing window band and yellow warning collars.
    t = Tex(seed=931)
    brushed(t, LEAD, base=3.4, amp=0.7, seed=931, axis="v")
    for x in range(16):
        t.set(x, 3, WARN[4])
        t.set(x, 12, WARN[4])
        t.set(x, 4, WARN[2])
        t.set(x, 13, LEAD[1])
    for y in range(6, 10):
        for x in range(4, 12):
            d = math.hypot(x + 0.5 - 8, (y + 0.5 - 8) * 1.6)
            t.set(x, y, RADIUM[6] if d < 1.6 else RADIUM[5] if d < 2.8 else RADIUM[3])
    t.hline(4, 11, 5, LEAD[1])
    t.hline(4, 11, 10, LEAD[5])
    speckle(t, 0, 0, 15, 2, LEAD[2], chance=0.15, seed=932)
    return t

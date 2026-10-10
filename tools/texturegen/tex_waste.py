"""Textures for waste classes, treatment machines and the repository.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.
"""

import math

from blocks import block  # noqa: F401
from common import bar_readout, bolt, casing, panel_screen, trefoil
from gui import gui  # noqa: F401
from items import GRANULES, item, mask_from, solid  # noqa: F401
from palette import CONCRETE, CRYO, DARK, PLASMA, RADIUM, STEEL, WARN
from pixelkit import Ramp, Tex, brushed, fbm, glow, hazard, item_outline, outline, rivet, shade, speckle

# Pale swelling clay, a little green like real sodium bentonite.
BENTONITE = Ramp("#5d5a4c", "#797663", "#948f79", "#aaa58c", "#bfba9f", "#d3cfb6", "#e6e3cf")
# Stainless steel of the canisters: lighter and cooler than structural steel.
STAINLESS = CRYO.tinted(STEEL[4], 0.45)
# Freshly separated fission products: hot calcine with a radioactive glow.
CALCINE = Ramp("#24191a", "#3a2620", "#553326", "#73412b", "#9b552f", "#cf7a3a", "#f5b862")
# Yellow polyethylene of low-level waste bags.
BAG = Ramp("#4a3c1b", "#6f5a27", "#977c35", "#bb9e44", "#d6bd5d", "#e9d684", "#f7ecb7")


def _drum_side(seed, ramp, band=None, label=None):
    """A steel drum seen from the side: rounded shading, rolling hoops, an optional painted band."""
    t = Tex(seed=seed)
    brushed(t, ramp, base=3.5, amp=0.6, seed=seed, axis="v")
    for x in range(16):
        f = 0.74 + 0.52 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    for y in (0, 15):
        t.hline(0, 15, y, shade(DARK[3], 0.8))
    for y in (1, 14):
        t.hline(0, 15, y, DARK[3])
    if band is not None:
        for y in range(6, 11):
            for x in range(16):
                f = 0.74 + 0.52 * math.sin(math.pi * (x + 0.5) / 16)
                t.set(x, y, shade(band[4 if y in (6, 10) else 3], f))
        trefoil(t, 8, 8.5, 2.6, DARK[0])
    if label is not None:
        label(t)
    return t


def _drum_top(seed, ramp, fill, fill_seed):
    """A drum lid: rim, a ring of hoop and the content showing through the open centre."""
    t = Tex(seed=seed)
    brushed(t, ramp, base=3.3, amp=0.5, seed=seed)
    t.ring(8, 8, 8.0, 6.9, DARK[3])
    t.ring(8, 8, 6.9, 6.2, ramp[5])
    for y in range(16):
        for x in range(16):
            if math.hypot(x + 0.5 - 8, y + 0.5 - 8) < 6.0:
                n = fbm(x, y, fill_seed, 16, 16, 3, 3.0)
                t.set(x, y, fill[int(round(2 + n * 3))])
    return t


# ======================================================================================
# waste classes
# ======================================================================================

BAG_ART = [
    "................",
    ".......##.......",
    "......####......",
    ".......##.......",
    "......####......",
    ".....######.....",
    "....########....",
    "...##########...",
    "..############..",
    "..############..",
    ".##############.",
    ".##############.",
    ".##############.",
    "..############..",
    "...##########...",
    "................",
]


@item("low_level_waste")
def low_level_waste():
    # A tied yellow bag of contaminated rags and filters, with the trefoil printed on it.
    t = solid(BAG_ART, BAG, seed=301, base=3.6, grain=0.8, outline=False)
    pts = mask_from(BAG_ART)
    for (x, y) in sorted(pts):
        if y >= 7 and (x + 2 * y) % 7 == 0:
            t.set(x, y, BAG[2])
    trefoil(t, 8, 11, 3.0, DARK[0])
    for (x, y) in list(sorted(pts)):
        if y < 5:
            t.set(x, y, BAG[1] if y % 2 else BAG[3])
    item_outline(t, BAG[0])
    return t


@item("high_level_waste")
def high_level_waste():
    # Calcined fission products: dark, crumbly granules glowing hot through their cracks,
    # with green radioactive glints.
    t = solid(GRANULES, CALCINE, seed=302, base=2.6, grain=1.1, outline=False)
    pts = mask_from(GRANULES)
    for (x, y) in sorted(pts):
        n = fbm(x, y, 302, 16, 16, 3, 3.0)
        if n > 0.57:
            t.set(x, y, PLASMA[5] if n > 0.63 else PLASMA[3])
        elif (x * 7 + y * 13) % 11 == 0:
            t.set(x, y, RADIUM[6])
    item_outline(t, CALCINE[0])
    return t


# ======================================================================================
# packages
# ======================================================================================

@block("compacted_waste_drum_side")
def compacted_waste_drum_side():
    # A grey drum crushed flat by the compactor: buckled ribs all round, a blue LLW band.
    blue = Ramp("#1e2a3b", "#2b3d56", "#3b5373", "#4e6b8f", "#6787aa", "#87a5c3", "#b0c7db")

    def buckles(t):
        for y in (3, 4, 11, 12):
            for x in range(16):
                wobble = 1 if (x // 3 + y) % 2 else 0
                t.set(x, y, shade(t.get(x, y), 0.80 + 0.12 * wobble))
    return _drum_side(303, STEEL, band=blue, label=buckles)


@block("compacted_waste_drum_top")
def compacted_waste_drum_top():
    t = _drum_top(304, STEEL, STEEL, 314)
    # crushed drums pressed together: dark creases across the lid
    for i in range(-5, 6):
        t.set(8 + i, 8 + (i * i) // 9 - 2, DARK[2])
        t.set(8 + i // 2, 8 + i, DARK[3])
    return t


@block("cemented_waste_drum_side")
def cemented_waste_drum_side():
    # A painted drum full of grout: warm grey paint, an orange ILW band.
    return _drum_side(305, CONCRETE, band=WARN)


@block("cemented_waste_drum_top")
def cemented_waste_drum_top():
    t = _drum_top(306, CONCRETE, CONCRETE, 316)
    speckle(t, 4, 4, 11, 11, CONCRETE[1], chance=0.10, seed=326)
    speckle(t, 4, 4, 11, 11, CONCRETE[6], chance=0.06, seed=336)
    return t


@block("vitrified_waste_canister_side")
def vitrified_waste_canister_side():
    # A tall stainless canister: welded seams, a lifting collar and a red HLW band.
    t = Tex(seed=307)
    brushed(t, STAINLESS, base=3.8, amp=0.5, seed=307, axis="v")
    for x in range(16):
        f = 0.70 + 0.60 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    t.hline(0, 15, 0, STAINLESS[1])
    t.hline(0, 15, 1, STAINLESS[5])
    t.hline(0, 15, 15, STAINLESS[0])
    for y in (5, 6):
        for x in range(16):
            f = 0.74 + 0.52 * math.sin(math.pi * (x + 0.5) / 16)
            t.set(x, y, shade(PLASMA[1] if y == 5 else PLASMA[2], f * 1.1))
    # vertical weld bead
    for y in range(2, 15):
        if y not in (5, 6):
            t.set(11, y, STAINLESS[6] if y % 2 else STAINLESS[3])
    trefoil(t, 6, 10.5, 2.4, DARK[1])
    return t


@block("vitrified_waste_canister_top")
def vitrified_waste_canister_top():
    t = Tex(seed=308)
    brushed(t, STAINLESS, base=3.6, amp=0.5, seed=308)
    t.ring(8, 8, 8.0, 7.0, STAINLESS[1])
    t.ring(8, 8, 7.0, 6.3, STAINLESS[6])
    # welded plug and lifting pintle
    t.ring(8, 8, 4.0, 3.2, STAINLESS[2])
    t.disc(8, 8, 2.2, STAINLESS[5])
    t.disc(7.5, 7.5, 1.0, STAINLESS[6])
    for a in range(0, 360, 45):
        x = 8 + 5.2 * math.cos(math.radians(a))
        y = 8 + 5.2 * math.sin(math.radians(a))
        t.set(int(x), int(y), STAINLESS[3])
    return t


# ======================================================================================
# repository
# ======================================================================================

@block("bentonite")
def bentonite():
    # Pale, waxy swelling clay with fine shrinkage cracks.
    t = Tex(seed=309)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 309, 16, 16, 3, 4.0)
            t.set(x, y, BENTONITE[int(round(2.6 + (n - 0.5) * 2.4))])
    for (x0, y0, x1, y1) in ((2, 3, 6, 5), (9, 10, 13, 9), (4, 12, 6, 14), (11, 2, 12, 5)):
        t.line(x0, y0, x1, y1, BENTONITE[1])
    speckle(t, 0, 0, 15, 15, BENTONITE[6], chance=0.05, seed=319)
    return t


@block("repository_inspector_side")
def repository_inspector_side():
    return casing(Tex(seed=310), STEEL, seed=310)


@block("repository_inspector_top")
def repository_inspector_top():
    t = casing(Tex(seed=311), STEEL, seed=311, rivets=False)
    t.disc(7.5, 7.5, 2.4, DARK[1])
    t.disc(7.5, 7.5, 1.4, RADIUM[4])
    return t


@block("repository_inspector_front")
def repository_inspector_front():
    # A survey panel: a green bar graph per barrier above a trefoil status lamp.
    t = Tex(seed=312)
    brushed(t, STEEL, base=3.2, amp=0.6, seed=312)
    outline(t, 0, 0, 15, 15, STEEL[0])
    panel_screen(t, 2, 2, 13, 9, STEEL, accent=RADIUM, lit=True, seed=312)
    bar_readout(t, 3, 3, 12, 9, RADIUM, [5, 3, 6, 4, 2], warn=4)
    t.rect(5, 11, 10, 14, WARN[4])
    outline(t, 5, 11, 10, 14, DARK[0])
    trefoil(t, 8, 13, 1.8, DARK[0])
    for x, y in ((1, 12), (13, 12)):
        rivet(t, x, y, STEEL)
    return t


# ======================================================================================
# vitrification melter
# ======================================================================================

@block("vitrification_melter_side")
def vitrification_melter_side():
    # Refractory-lined steel body with a peep window onto the molten glass and a hazard band.
    t = casing(Tex(seed=313), DARK, seed=313, rivets=False, seam=False)
    t.rect(5, 4, 10, 8, DARK[0])
    for y in range(5, 8):
        for x in range(6, 10):
            t.set(x, y, PLASMA[4 + (x + y) % 3])
    t.set(6, 5, PLASMA[6])
    hazard(t, 3, 11, 12, 12, WARN[4], DARK[0], period=4, seed=313)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, STEEL)
    return t


@block("vitrification_melter_top")
def vitrification_melter_top():
    # The crucible mouth: molten borosilicate glass glowing inside a thick rim.
    t = Tex(seed=314)
    brushed(t, DARK, base=3.0, amp=0.6, seed=314)
    outline(t, 0, 0, 15, 15, DARK[0])
    t.disc(7.5, 7.5, 6.4, STEEL[1])
    t.disc(7.5, 7.5, 5.4, DARK[0])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d < 4.6:
                n = fbm(x, y, 344, 16, 16, 2, 3.0)
                c = PLASMA[int(round(3 + n * 3))] if d > 2 else RADIUM[5 + int(n > 0.5)]
                t.set(x, y, c)
    return t


@block("vitrification_melter_bottom")
def vitrification_melter_bottom():
    # Hot plate sitting over the burner: a grid of heat slots.
    t = Tex(seed=315)
    brushed(t, DARK, base=3.0, amp=0.6, seed=315)
    outline(t, 0, 0, 15, 15, DARK[0])
    for y in range(3, 13, 3):
        t.hline(3, 12, y, DARK[0])
        t.hline(3, 12, y + 1, PLASMA[1])
    return t

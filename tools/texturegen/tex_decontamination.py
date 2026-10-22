"""Textures for ground decontamination: the sprinkler, the irradiated sunflower and its biomass.

Register block textures with ``@block`` and item textures with ``@item``; this module is imported
at the end of ``blocks.py``. The sunflower's four textures are cutout plants with the layout of
vanilla's ``sunflower_*``: the top only shows rows 8-15 and the head is a disc centred on x 8.
"""

import math

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from common import bolt
from items import item, mask_from, sculpt
from palette import (BRASS, CARBONATE, CONCRETE, COPPER, DANGER, DARK, MIMIC_BLUE, RADIUM, STEEL,
                     UO2, URANIUM, WARN)
from pixelkit import Tex, bevel, brushed, fbm, hazard, item_outline, mix, outline, rivet, shade

for _name in ("irradiated_sunflower_bottom", "irradiated_sunflower_top",
              "irradiated_sunflower_front", "irradiated_sunflower_back"):
    CUTOUT_BLOCKS[_name] = (16, 16)

# Clean feed water and the murky, faintly green water collected in the tank.
WATER = MIMIC_BLUE
DIRTY_WATER = MIMIC_BLUE.tinted(RADIUM[3], 0.40)
# Wilted plant tissue: olive drained towards grey.
WILT = URANIUM.tinted(CONCRETE[3], 0.30)
# Dead, browned leaf.
DEAD = WARN.tinted(UO2[4], 0.55)
# Sickly sunflower petals: a greyed yellow.
PETAL = WARN.tinted(CONCRETE[4], 0.18)


# ======================================================================================
# decontamination sprinkler
# ======================================================================================

@block("decontamination_sprinkler_side")
def decontamination_sprinkler_side():
    # The body is 12px tall, so the model shows rows 4-15: a hazard band under the top lip, a sight
    # glass on the contaminated-water tank and a small pressure gauge. Rows 0-3 are never shown.
    t = Tex(seed=560)
    brushed(t, STEEL, base=3.1, amp=0.75, seed=560)
    outline(t, 0, 0, 15, 15, STEEL[0])
    t.hline(0, 15, 4, STEEL[5])
    t.hline(1, 14, 5, STEEL[1])
    hazard(t, 1, 6, 14, 7, WARN[4], DARK[0], period=4, seed=560)
    t.hline(1, 14, 8, STEEL[1])
    t.hline(1, 14, 9, STEEL[5])
    bevel(t, 0, 4, 15, 15, STEEL, hi=STEEL[5], lo=STEEL[0])
    outline(t, 0, 4, 15, 15, STEEL[0])
    # sight glass: murky water rising to two-thirds, a bright meniscus and scale ticks
    t.rect(2, 10, 6, 14, DARK[0])
    t.rect(3, 11, 5, 13, DARK[1])
    for y in range(11, 14):
        for x in range(3, 6):
            if y >= 12:
                t.set(x, y, DIRTY_WATER[2 + (x + y) % 2])
    t.hline(3, 5, 12, WATER[5])
    t.set(3, 13, RADIUM[4])
    t.set(6, 11, STEEL[6])
    t.set(6, 13, STEEL[6])
    # pressure gauge with a brass rim
    t.disc(10.5, 12.0, 2.6, BRASS[2])
    t.ring(10.5, 12.0, 2.6, 2.0, BRASS[4])
    t.disc(10.5, 12.0, 1.7, CARBONATE[5])
    t.set(10, 12, DARK[0])
    t.set(11, 11, DANGER[3])
    t.set(9, 12, CARBONATE[3])
    # a blue feed-water tag by the gauge
    t.vline(14, 10, 13, WATER[4])
    t.set(14, 10, WATER[6])
    rivet(t, 1, 14, STEEL)
    rivet(t, 13, 14, STEEL)
    return t


@block("decontamination_sprinkler_top")
def decontamination_sprinkler_top():
    # The tank lid seen past the spray head: hazard corners outside the arms' sweep, a ring of
    # drain slots catching the runoff and a brass flange round the neck.
    t = Tex(seed=561)
    brushed(t, STEEL, base=3.2, amp=0.7, seed=561)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d > 7.6 and (x + y) % 4 < 2:
                t.set(x, y, WARN[4])
            elif d > 7.6:
                t.set(x, y, DARK[0])
    t.ring(8, 8, 7.6, 6.9, STEEL[1])
    t.ring(8, 8, 6.9, 6.3, STEEL[5])
    # drain ring: dark slots with a glint of water
    for i, (x, y) in enumerate(((7, 2), (12, 4), (13, 7), (12, 11), (8, 13), (3, 11), (2, 8), (3, 4))):
        t.set(x, y, DARK[0])
        t.set(x + (1 if i in (0, 4) else 0), y + (0 if i in (0, 4) else 1), WATER[3] if i % 2 else DARK[1])
    t.ring(8, 8, 4.0, 3.3, BRASS[2])
    t.disc(8, 8, 3.3, BRASS[4])
    t.ring(8, 8, 3.3, 2.7, BRASS[5])
    t.disc(8, 8, 1.9, DARK[1])
    t.rect(7, 7, 8, 8, WATER[2])
    t.set(7, 7, WATER[5])
    outline(t, 0, 0, 15, 15, STEEL[0])
    return t


@block("decontamination_sprinkler_bottom")
def decontamination_sprinkler_bottom():
    # Plain plate round the drive shaft socket, like the scrubber, with a drain plug.
    t = Tex(seed=562)
    brushed(t, STEEL, base=2.8, amp=0.7, seed=562)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    t.disc(8, 8, 3.6, DARK[0])
    t.ring(8, 8, 3.6, 2.8, BRASS[4])
    t.ring(8, 8, 2.8, 2.2, BRASS[2])
    t.rect(6, 6, 9, 9, DARK[2])
    t.hline(6, 9, 6, DARK[4])
    t.rect(12, 12, 13, 13, BRASS[3])
    t.set(12, 12, BRASS[5])
    t.set(13, 13, BRASS[1])
    for x, y in ((1, 1), (13, 1), (1, 13)):
        bolt(t, x, y, STEEL)
    return t


@block("decontamination_sprinkler_head")
def decontamination_sprinkler_head():
    # Atlas for the spinning head (decontamination_sprinkler/head.json picks every face by UV):
    #   rows 0-3      brass spray arm, lit from the top, with copper weld bands;
    #   x 5-10 y 5-10 the hub's cap: a brass swivel with a blue water seal and a bright index mark;
    #   x 0-3 y 5-10  copper nozzle body;  x 12-15 y 5-10 the nozzle tip with its blue bore;
    #   rows 12-15    the hub's band, bolts alternating with spray holes so the spin reads.
    t = Tex(seed=563)
    brushed(t, BRASS, base=3.2, amp=0.6, seed=563)
    for x in range(16):
        t.set(x, 0, BRASS[6])
        t.set(x, 1, BRASS[5])
        t.set(x, 2, BRASS[3])
        t.set(x, 3, BRASS[1])
    for x in (4, 9, 14):
        for y, c in enumerate((COPPER[5], COPPER[4], COPPER[3], COPPER[1])):
            t.set(x, y, c)
    t.hline(0, 15, 4, DARK[0])
    t.hline(0, 15, 11, DARK[0])
    # hub cap
    t.rect(5, 5, 10, 10, BRASS[3])
    bevel(t, 5, 5, 10, 10, BRASS)
    t.rect(7, 7, 8, 8, WATER[3])
    t.set(7, 7, WATER[5])
    t.set(8, 8, WATER[1])
    t.hline(9, 9, 7, BRASS[6])
    t.set(9, 8, BRASS[6])
    # nozzle body
    for y in range(5, 11):
        t.set(0, y, COPPER[5])
        t.set(1, y, COPPER[4])
        t.set(2, y, COPPER[3])
        t.set(3, y, COPPER[1])
    t.hline(0, 3, 8, COPPER[0])
    # nozzle tip
    t.rect(12, 5, 15, 10, COPPER[3])
    bevel(t, 12, 5, 15, 10, COPPER)
    t.rect(13, 7, 14, 8, DARK[0])
    t.set(13, 7, WATER[4])
    t.set(14, 8, WATER[2])
    # hub band
    for x in range(16):
        t.set(x, 12, BRASS[5])
        t.set(x, 13, BRASS[3])
        t.set(x, 14, BRASS[2])
        t.set(x, 15, BRASS[1])
    for x in range(0, 16, 4):
        bolt(t, x, 12, BRASS)
        t.set(x + 2, 13, DARK[0])
        t.set(x + 2, 14, WATER[3])
    return t


# ======================================================================================
# irradiated sunflower
# ======================================================================================

def _paint(art, colours, seed=0):
    """Paint string art with a ``char -> colour`` map; '.' stays clear (cutout)."""
    t = Tex(seed=seed)
    for y, row in enumerate(art):
        assert len(row) == 16, row
        for x, ch in enumerate(row):
            if ch != ".":
                t.set(x, y, colours[ch])
    return t


# s/S lit and shaded stem, l leaf, v vein, e leaf edge, d browned tip
PLANT = {
    "s": WILT[4], "S": WILT[2], "l": WILT[3], "v": WILT[5], "e": WILT[1], "d": DEAD[2],
}

# The leaves leave the stem and hang down, as a thirsty plant's do.
SUNFLOWER_BOTTOM = [
    ".......sS.......",
    ".......sS.......",
    ".......sS.......",
    ".......sSee.....",
    ".......sSlle....",
    ".......sS.vlle..",
    "..eee..sS..lvle.",
    ".ellle.sS...lld.",
    "elvlleesS....dd.",
    "el...llsS.......",
    "e......sS.......",
    ".......sSee.....",
    "......esSlle....",
    "....ellsS.vld...",
    "....dd.sS..dd...",
    "......ssSS......",
]

# Only rows 8-15 are shown: the stem bending up into the head, with one limp leaf.
SUNFLOWER_TOP = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "........sS......",
    ".......sS.......",
    ".......sSee.....",
    ".......sSlle....",
    ".......sS.vle...",
    ".......sS..ld...",
    "......esS...d...",
    ".......sS.......",
]

# The wilted head, its upper petals dropped and its lower ones hanging long. P/p lit and shaded
# petal, q the seam between petals, g a radioactive-green petal tip, o the seed head's rim,
# D/d dark and lighter seeds, r a green fleck.
SUNFLOWER_HEAD = [
    "................",
    "................",
    "................",
    "................",
    "....g.qPPq.g....",
    "...gPPooooPPg...",
    "..gPPoDdrDoPpg..",
    "..gpPodrDdoPpg..",
    "..gqpoDdDroqpg..",
    "..gppodrdDopqg..",
    "...gpqooooqpg...",
    "...gPpPqqPpPg...",
    "....pP.pP.Pp....",
    "....gp.Pp.pg....",
    ".....g.gg.g.....",
    "................",
]


@block("irradiated_sunflower_bottom")
def irradiated_sunflower_bottom():
    # A greyed stem whose leaves hang limp, their tips browned and curling.
    t = _paint(SUNFLOWER_BOTTOM, PLANT)
    t.set(13, 6, RADIUM[5])
    return t


@block("irradiated_sunflower_top")
def irradiated_sunflower_top():
    return _paint(SUNFLOWER_TOP, PLANT)


@block("irradiated_sunflower_front")
def irradiated_sunflower_front():
    # Drooping, greyish-yellow petals going radioactive green at the tips round a dark seed head
    # flecked with green.
    return _paint(SUNFLOWER_HEAD, {
        "P": PETAL[4], "p": PETAL[3], "q": PETAL[2], "g": mix(PETAL[3], RADIUM[4], 0.65),
        "o": DEAD[1], "D": UO2[1], "d": UO2[3], "r": RADIUM[5],
    })


@block("irradiated_sunflower_back")
def irradiated_sunflower_back():
    # The back of the same head, mirrored because the model shows it from the other side: a
    # grey-green calyx with dark sepal seams over the limp petal backs.
    back = mix(PETAL[3], WILT[3], 0.45)
    t = _paint([row[::-1] for row in SUNFLOWER_HEAD], {
        "P": back, "p": shade(back, 0.88), "q": shade(back, 0.74), "g": mix(back, RADIUM[3], 0.5),
        "o": WILT[3], "D": WILT[2], "d": WILT[4], "r": WILT[1],
    })
    return t


# ======================================================================================
# irradiated biomass
# ======================================================================================

BIOMASS_ART = [
    "................",
    "................",
    "..........#.....",
    "....#....##.....",
    "....##..##......",
    ".....######.....",
    "....########....",
    "...##########...",
    "..############..",
    ".#############..",
    ".##############.",
    "..#############.",
    "...###########..",
    "....##.....##...",
    "................",
    "................",
]

# browned stems tangled through the clump
BIOMASS_STEMS = (((4, 3), (8, 9)), ((10, 2), (7, 9)), ((2, 10), (13, 8)))


@item("irradiated_biomass")
def irradiated_biomass():
    # A clump of wilted, cut plant matter: limp grey-green leaves and browned stems, glinting green.
    t = Tex(seed=574)
    pts = mask_from(BIOMASS_ART)
    sculpt(t, pts, WILT, base=3.5, seed=574, grain=0.6)
    for (x, y) in sorted(pts):
        n = fbm(x, y, 584, 16, 16, 3, 3.0)
        if n > 0.64:
            t.set(x, y, DEAD[3])
    for (x0, y0), (x1, y1) in BIOMASS_STEMS:
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(steps + 1):
            x = round(x0 + (x1 - x0) * i / steps)
            y = round(y0 + (y1 - y0) * i / steps)
            if (x, y) in pts:
                t.set(x, y, DEAD[1] if (x + y) % 3 else DEAD[2])
    for (x, y) in ((6, 7), (11, 9), (4, 10), (9, 11), (8, 5)):
        t.set(x, y, RADIUM[6])
        t.set(x + 1, y + 1, RADIUM[3])
    item_outline(t, WILT[0])
    return t

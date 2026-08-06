"""Item texture definitions."""

import math

from common import trefoil
from palette import (BLACK, BRASS, CONCRETE, COPPER, CRYO, CYAN, DANGER, DARK, LEAD,
                     LITHIUM, PLASMA, RADIUM, STEEL, TRITIUM, URANIUM, WARN, WHITE)
from pixelkit import (Ramp, Tex, fbm, glow, item_outline, mix, shade, speckle)

ITEMS = {}


def item(name):
    def deco(fn):
        ITEMS[name] = fn
        return fn
    return deco


# --------------------------------------------------------------------------------------
# shading engine: turn a silhouette into a lit, ramped solid
# --------------------------------------------------------------------------------------

def mask_from(art):
    return {(x, y) for y, row in enumerate(art) for x, ch in enumerate(row) if ch == "#"}


def sculpt(t, pts, ramp, base=3.7, seed=0, grain=0.7, light=(-1, -1)):
    """Light a silhouette from the top-left with rim highlights and contact shadow."""
    for (x, y) in sorted(pts):
        v = base
        v += (0.5 - (x + y) / (t.w + t.h)) * 1.7
        if (x, y - 1) not in pts:
            v += 1.25
        if (x - 1, y) not in pts:
            v += 0.55
        if (x, y + 1) not in pts:
            v -= 1.35
        if (x + 1, y) not in pts:
            v -= 0.85
        v += (fbm(x, y, seed, t.w, t.h, 2, 6.0) - 0.5) * grain * 2
        t.set(x, y, ramp[int(round(max(1, min(len(ramp) - 1, v))))])
    return t


def solid(art, ramp, seed=0, base=3.7, grain=0.7, outline=True):
    t = Tex(16, 16, seed)
    pts = mask_from(art)
    sculpt(t, pts, ramp, base=base, seed=seed, grain=grain)
    if outline:
        item_outline(t, ramp[0])
    return t


def capsule_pts(x0, y0, x1, y1, r):
    """Points within `r` of the segment (x0,y0)-(x1,y1)."""
    pts = set()
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            dx, dy = x1 - x0, y1 - y0
            ln = dx * dx + dy * dy
            tt = 0 if ln == 0 else max(0, min(1, ((px - x0) * dx + (py - y0) * dy) / ln))
            cx, cy = x0 + dx * tt, y0 + dy * tt
            if math.hypot(px - cx, py - cy) <= r:
                pts.add((x, y))
    return pts


def rect_pts(x0, y0, x1, y1):
    return {(x, y) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1)}


# --------------------------------------------------------------------------------------
# silhouettes
# --------------------------------------------------------------------------------------

CHUNK = [
    "................",
    "................",
    "................",
    "......####......",
    ".....#######....",
    "....#########...",
    "...##########...",
    "..###########...",
    "..###########...",
    "..##########....",
    "...#########....",
    "....#######.....",
    ".....#####......",
    "......###.......",
    "................",
    "................",
]

# Classic diagonal ingot silhouette, shared by every metal in the mod.
INGOT = [
    "................",
    "................",
    "................",
    "................",
    "........####....",
    ".......######...",
    "......#######...",
    ".....#######....",
    "....#######.....",
    "...#######......",
    "..#######.......",
    "..######........",
    "...####.........",
    "................",
    "................",
    "................",
]

PILE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......####......",
    ".....######.....",
    "....########....",
    "...##########...",
    "..############..",
    ".##############.",
    ".##############.",
    "..############..",
    "................",
    "................",
    "................",
]

CANISTER = [
    "................",
    "......####......",
    ".....######.....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    ".....######.....",
    "......####......",
    "................",
]

RODS = [
    "................",
    "...##########...",
    "...##########...",
    "...#.#.##.#.#...",
    "...#.#.##.#.#...",
    "...#.#.##.#.#...",
    "...#.#.##.#.#...",
    "...#.#.##.#.#...",
    "...#.#.##.#.#...",
    "...#.#.##.#.#...",
    "...#.#.##.#.#...",
    "...#.#.##.#.#...",
    "...##########...",
    "...##########...",
    "................",
    "................",
]

ASSEMBLY = [
    "................",
    "................",
    "..############..",
    "..############..",
    "..#.#.#.#.#.##..",
    "..############..",
    "..#.#.#.#.#.##..",
    "..############..",
    "..#.#.#.#.#.##..",
    "..############..",
    "..#.#.#.#.#.##..",
    "..############..",
    "..############..",
    "................",
    "................",
    "................",
]

PELLET = [
    "................",
    "................",
    "................",
    "................",
    "......####......",
    ".....######.....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    ".....######.....",
    "......####......",
    "................",
    "................",
    "................",
    "................",
]

NOSE = [
    "................",
    "................",
    ".......##.......",
    "......####......",
    "......####......",
    ".....######.....",
    ".....######.....",
    "....########....",
    "....########....",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    "..############..",
    "................",
    "................",
]

FIN = [
    "................",
    "................",
    "................",
    "............##..",
    "...........###..",
    "..........####..",
    ".........#####..",
    "........######..",
    ".......#######..",
    "......########..",
    ".....#########..",
    "....##########..",
    "....##########..",
    "................",
    "................",
    "................",
]

DESIGNATOR = [
    "................",
    "................",
    "..#.............",
    "..#....#####....",
    "..#...#######...",
    "..#############.",
    "..#############.",
    "..#############.",
    "..#############.",
    "..####...######.",
    "..###.....#####.",
    "..####...######.",
    "..#############.",
    "...###########..",
    "................",
    "................",
]


# ======================================================================================
# ore / metal items
# ======================================================================================

@item("raw_uranium")
def raw_uranium():
    rock = Ramp("#101410", "#20281f", "#333d31", "#485442", "#5e6c57", "#77866e", "#94a48a")
    t = solid(CHUNK, rock, seed=201, base=3.6, outline=False)
    pts = mask_from(CHUNK)
    # uranium veins
    for (x, y) in sorted(pts):
        n = fbm(x, y, 77, 16, 16, 3, 4.5)
        if n > 0.68:
            t.set(x, y, URANIUM[4 if n < 0.76 else 5])
        elif n > 0.64:
            t.set(x, y, URANIUM[2])
    for (x, y) in ((5, 6), (9, 8), (7, 11)):
        if (x, y) in pts:
            t.set(x, y, RADIUM[6])
    glow(t, 7, 8, 6.5, RADIUM[6], 0.08)
    item_outline(t, rock[0])
    return t


def _ingot(seed, ramp, mark=None, mark_glow=None):
    t = Tex(16, 16, seed)
    pts = mask_from(INGOT)
    sculpt(t, pts, ramp, base=4.0, seed=seed, grain=0.45)
    # polished top facet
    for (x, y) in sorted(pts):
        if (x - 1, y - 1) not in pts and (x, y - 1) in pts:
            t.set(x, y, ramp[6])
    if mark:
        for (x, y) in mark:
            if (x, y) in pts:
                t.set(x, y, mark_glow[6])
                if (x + 1, y) in pts:
                    t.set(x + 1, y, mark_glow[4])
    if mark_glow and mark:
        glow(t, 8, 8, 8.0, mark_glow[6], 0.18)
    item_outline(t, ramp[0])
    return t


@item("uranium_235")
def uranium_235():
    return _ingot(202, URANIUM,
                  mark=[(8, 6), (6, 8), (4, 10)], mark_glow=RADIUM)


@item("uranium_238")
def uranium_238():
    return _ingot(203, URANIUM.tinted(LEAD[4], 0.55))


@item("borax_salt")
def borax_salt():
    salt = Ramp("#2b2c26", "#585a4e", "#8a8d7c", "#b4b7a4", "#d5d8c6", "#eef0e4", "#ffffff")
    t = solid(PILE, salt, seed=204, base=4.0, grain=1.1, outline=False)
    pts = mask_from(PILE)
    # crystal facets
    for (x, y) in sorted(pts):
        if (x + y) % 5 == 0 and (x, y - 1) in pts:
            t.set(x, y, salt[6])
    speckle(t, 2, 8, 13, 12, salt[2], chance=0.10, seed=204)
    item_outline(t, salt[0])
    return t


@item("boron")
def boron():
    bor = Ramp("#08090b", "#141619", "#212429", "#2f343b", "#40464f", "#535a65", "#6d7581")
    t = solid(PILE, bor, seed=205, base=3.5, grain=1.2, outline=False)
    pts = mask_from(PILE)
    for (x, y) in sorted(pts):
        if fbm(x, y, 99, 16, 16, 2, 7.0) > 0.68:
            t.set(x, y, bor[6])
    item_outline(t, bor[0])
    return t


@item("beryllium")
def beryllium():
    ber = Ramp("#0e1210", "#1c2420", "#2e3a33", "#445448", "#5d7062", "#7c9080", "#a5b8a8")
    return _ingot(206, ber)


@item("lithium")
def lithium():
    return _ingot(207, LITHIUM)


@item("lithium_6")
def lithium_6():
    return _ingot(208, LITHIUM.tinted(CYAN[4], 0.35),
                  mark=[(9, 6), (7, 8), (5, 10)], mark_glow=CYAN)


@item("superconductor_ingot")
def superconductor_ingot():
    return _ingot(209, CRYO.tinted(WHITE, 0.20),
                  mark=[(9, 5), (7, 7), (5, 9), (4, 11)], mark_glow=CYAN)


# ======================================================================================
# fuel
# ======================================================================================

def _fuel_bundle(seed, rod_ramp, cap_ramp, glow_ramp=None, spent=False):
    t = Tex(16, 16, seed)
    pts = mask_from(RODS)
    sculpt(t, pts, cap_ramp, base=3.6, seed=seed, grain=0.5)
    # rods: light / dark column pairs read as cylinders
    for x in range(3, 13):
        c = rod_ramp[5] if x % 2 == 1 else rod_ramp[2]
        for y in range(3, 12):
            t.set(x, y, c)
    for y in range(3, 12):
        t.set(12, y, rod_ramp[1])
    # top and bottom end caps
    for y in (1, 2, 12, 13):
        for x in range(3, 13):
            v = 5 if y in (1, 12) else 3
            t.set(x, y, cap_ramp[v])
    for x in range(3, 13):
        t.set(x, 1, cap_ramp[6] if x % 2 == 0 else cap_ramp[5])
        t.set(x, 13, cap_ramp[1])
    if glow_ramp:
        for x in range(3, 13, 2):
            for y in range(4, 11, 3):
                t.set(x, y, glow_ramp[6])
        glow(t, 8, 7, 8.0, glow_ramp[6], 0.20)
    if spent:
        speckle(t, 3, 3, 12, 11, DARK[1], chance=0.18, seed=seed)
    item_outline(t, cap_ramp[0])
    return t


@item("uranium_reactor_fuel")
def uranium_reactor_fuel():
    return _fuel_bundle(210, URANIUM, STEEL, glow_ramp=RADIUM)


@item("decayed_uranium_reactor_fuel")
def decayed_uranium_reactor_fuel():
    dull = URANIUM.tinted(LEAD[3], 0.6)
    return _fuel_bundle(211, dull, STEEL.tinted(DARK[3], 0.4), glow_ramp=None, spent=True)


@item("lithium_6_breeder_assembly")
def lithium_6_breeder_assembly():
    li6 = LITHIUM.tinted(CYAN[4], 0.30)
    t = Tex(16, 16, 212)
    pts = mask_from(ASSEMBLY)
    sculpt(t, pts, STEEL, base=3.8, seed=212, grain=0.5)
    # breeder lattice cells
    for y in range(4, 12, 2):
        for x in range(3, 13, 2):
            t.set(x, y, li6[5])
            t.set(x + 1, y, li6[2])
    for x in range(2, 14):
        t.set(x, 2, STEEL[6])
        t.set(x, 12, STEEL[1])
    glow(t, 8, 7, 7.5, CYAN[6], 0.14)
    item_outline(t, STEEL[0])
    return t


def _cell(seed, gas, label_ramp):
    t = Tex(16, 16, seed)
    pts = mask_from(CANISTER)
    sculpt(t, pts, STEEL, base=3.9, seed=seed, grain=0.45)
    # viewing window with pressurised gas
    for y in range(4, 12):
        for x in range(5, 11):
            v = 3 + int((fbm(x, y, seed + 5, 16, 16, 2, 5.0) - 0.5) * 4)
            t.set(x, y, gas[max(2, min(6, v + 2))])
    for y in range(4, 12):
        t.set(5, y, gas[2])
        t.set(10, y, gas[1])
    t.set(6, 5, WHITE)
    t.set(7, 5, gas[6])
    # collars
    for y in (2, 3, 12, 13):
        for x in range(4, 12):
            t.set(x, y, STEEL[5 if y in (2, 12) else 3])
    for x in range(4, 12):
        t.set(x, 1 if x in range(6, 10) else 2, STEEL[6])
    glow(t, 8, 8, 7.0, gas[6], 0.25)
    item_outline(t, STEEL[0])
    return t


@item("deuterium_cell")
def deuterium_cell():
    return _cell(213, CYAN, CYAN)


@item("tritium_cell")
def tritium_cell():
    return _cell(214, TRITIUM, TRITIUM)


@item("dt_fuel_pellet")
def dt_fuel_pellet():
    t = Tex(16, 16, 215)
    pts = mask_from(PELLET)
    sculpt(t, pts, CRYO, base=3.6, seed=215, grain=0.4)
    t.disc(8, 8, 2.6, PLASMA[4])
    t.disc(8, 8, 1.6, PLASMA[6])
    t.disc(8, 8, 0.8, WHITE)
    t.ring(8, 8, 3.6, 2.9, CRYO[5])
    glow(t, 8, 8, 6.5, PLASMA[6], 0.35)
    item_outline(t, CRYO[0])
    return t


@item("spent_dt_pellet")
def spent_dt_pellet():
    ash = Ramp("#0a0a0b", "#171719", "#242427", "#323236", "#424348", "#54555b", "#6a6b72")
    t = Tex(16, 16, 216)
    pts = mask_from(PELLET)
    sculpt(t, pts, ash, base=3.4, seed=216, grain=0.9)
    t.disc(8, 8, 2.4, ash[1])
    speckle(t, 5, 5, 10, 10, ash[0], chance=0.3, seed=216)
    t.set(7, 6, PLASMA[2])
    item_outline(t, ash[0])
    return t


# ======================================================================================
# ordnance items
# ======================================================================================

@item("missile_body")
def missile_body():
    olive = STEEL.tinted(WARN[2], 0.30)
    t = Tex(16, 16, 217)
    pts = rect_pts(4, 1, 11, 14)
    sculpt(t, pts, olive, base=4.0, seed=217, grain=0.5)
    for y in range(1, 15):
        t.set(4, y, olive[6])
        t.set(5, y, olive[5])
        t.set(10, y, olive[2])
        t.set(11, y, olive[1])
    for y in (4, 11):
        for x in range(4, 12):
            t.set(x, y, WARN[5] if (x + y) % 4 < 2 else DARK[1])
    t.hline(4, 11, 3, DARK[0])
    t.hline(4, 11, 5, DARK[0])
    t.hline(4, 11, 10, DARK[0])
    t.hline(4, 11, 12, DARK[0])
    t.hline(4, 11, 1, olive[6])
    t.hline(4, 11, 14, olive[1])
    item_outline(t, olive[0])
    return t


@item("missile_top")
def missile_top():
    t = solid(NOSE, STEEL, seed=218, base=4.0, grain=0.4, outline=False)
    for y in range(2, 8):
        for x in range(16):
            if t.im.getpixel((x, y))[3]:
                t.set(x, y, DANGER[4 if (x + y) % 3 else 5])
    t.set(7, 3, WHITE)
    item_outline(t, STEEL[0])
    return t


@item("missile_fin")
def missile_fin():
    t = solid(FIN, STEEL, seed=219, base=3.6, grain=0.5, outline=False)
    pts = mask_from(FIN)
    for (x, y) in sorted(pts):
        if x >= 12:
            t.set(x, y, STEEL[2])
    for y in range(3, 13):
        if (12, y) in pts:
            t.set(12, y, STEEL[6])
    item_outline(t, STEEL[0])
    return t


@item("target_designator")
def target_designator():
    t = Tex(16, 16, 220)
    pts = mask_from(DESIGNATOR)
    sculpt(t, pts, DARK, base=4.0, seed=220, grain=0.5)
    # antenna
    for y in range(2, 13):
        t.set(2, y, STEEL[5])
    t.set(2, 2, DANGER[6])
    # optics
    t.disc(8.5, 9.5, 2.4, CYAN[2])
    t.ring(8.5, 9.5, 2.4, 1.7, STEEL[4])
    t.disc(8.5, 9.5, 1.4, CYAN[5])
    t.set(8, 9, CYAN[6])
    # grip / buttons
    for x, y in ((11, 4), (13, 4)):
        t.set(x, y, RADIUM[6])
        t.set(x, y + 1, RADIUM[3])
    for x in range(5, 13):
        t.set(x, 6, DARK[5])
    glow(t, 8.5, 9.5, 5.0, CYAN[6], 0.28)
    item_outline(t, DARK[0])
    return t

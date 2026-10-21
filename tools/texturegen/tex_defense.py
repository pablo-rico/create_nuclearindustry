"""Textures of missile defence: the interceptor battery, its rounds, the kill vehicle and the battery's screen.

The battery is of the radar's olive military family (``tex_missile``); its screen takes every coordinate from
``missile_layouts.py`` (``BATTERY_*``, mirroring ``BatteryLayout``) and never hard-codes one.

The battery's block model spans its whole 2x2 footprint from the master block, so its faces are mapped one block at a time:
``interceptor_battery_side`` is one block's width of a side with the corner post on its left and the centre seam on its right,
``interceptor_battery_deck`` the north-west quarter of the deck seen from above (outer edges top and left, the turret well's
coaming bottom right). The model mirrors both for the other quarters, so the battery looks the same from every side.
"""

import math

import missile_layouts as L
from blocks import block
from common import bolt
from gui import gui
from items import item
from palette import BRASS, COPPER, DARK, RADIUM, STEEL, WARN, WHITE
from pixelkit import Ramp, Tex, bevel, brushed, fbm, item_outline, mix, outline, shade

from tex_launch import (LAMP_AMBER, LAMP_GREEN, LAMP_RED, _brackets, _button, _groove, _lamp, _louvres, _olive_panel,
                        _phosphor, _slot, _stripes, _sunk)
from tex_missile import FOIL, GRAPHITE, HULL, OLIVE, PHOSPHOR, _box

# Ceramic of the interceptor's radome, and the kill vehicle's seeker glass.
RADOME = Ramp("#4a4a44", "#6d6c63", "#8f8d82", "#aeab9d", "#c8c5b5", "#dddacb", "#f0eee2")
SEEKER = Ramp("#03080c", "#071822", "#0d2a3a", "#164257", "#24647c", "#4fa6bf", "#c8f4ff")


def _olive_plate(t, seed, base=3.4, amp=0.55):
    brushed(t, OLIVE, base=base, amp=amp, seed=seed)


# ======================================================================================
# the battery's base: an olive plinth over its whole footprint, a turret well in the middle of the deck
# ======================================================================================

@block("interceptor_battery_side")
def interceptor_battery_side():
    """One block's width of a side: deck lip with hazard trim (rows 0..6), the seam, then the plinth (rows 7..15).

    Column 0 is the corner post, column 15 the seam the mirrored half meets in the middle of the side."""
    t = Tex(seed=1100)
    _olive_plate(t, 1100)
    # Deck lip: a lit edge, the hazard trim, its shadow.
    t.hline(0, 15, 0, OLIVE[6])
    _stripes(t, 0, 1, 15, 2, period=4)
    t.hline(0, 15, 3, OLIVE[1])
    for x in (4, 11):
        bolt(t, x, 4, OLIVE)
    # A small data plate on the lip.
    t.rect(7, 4, 9, 5, mix(HULL[3], OLIVE[4], 0.4))
    t.hline(7, 9, 6, OLIVE[1])
    # Seam between the deck and the plinth.
    t.hline(0, 15, 7, OLIVE[0])
    t.hline(0, 15, 8, OLIVE[5])
    # Plinth: cooling louvres for the slew drive.
    for y in (10, 12):
        t.hline(4, 12, y, OLIVE[0])
        t.hline(4, 12, y + 1, OLIVE[4])
    t.vline(3, 9, 13, OLIVE[1])
    t.vline(13, 9, 13, OLIVE[5])
    # The foot: dark steel skirting.
    t.hline(0, 15, 14, DARK[3])
    t.hline(0, 15, 15, DARK[0])
    # Corner post and centre seam.
    t.vline(0, 0, 13, OLIVE[6])
    t.vline(1, 3, 13, OLIVE[4])
    t.vline(15, 3, 13, OLIVE[1])
    t.vline(14, 3, 13, mix(OLIVE[3], OLIVE[2], 0.5))
    t.set(0, 1, WARN[5])
    t.set(0, 2, DARK[0])
    return t


@block("interceptor_battery_deck")
def interceptor_battery_deck():
    """The north-west quarter of the deck from above: hazard trim along the outer edges (top, left), tread plate, and the
    steel coaming of the turret well round its corner (from pixel 7, bottom right)."""
    t = Tex(seed=1101)
    _olive_plate(t, 1101, base=3.5, amp=0.45)
    # Tread: raised pips in a staggered grid.
    for y in range(4, 16, 3):
        for x in range(4 + (y // 3) % 2, 16, 3):
            t.set(x, y, OLIVE[5])
            t.set(x + 1, y + 1, OLIVE[1])
    # Outer edges: a lit lip, then the hazard band, then its shadow.
    t.hline(0, 15, 0, OLIVE[6])
    t.vline(0, 0, 15, OLIVE[6])
    for y in range(1, 3):
        for x in range(1, 16):
            t.set(x, y, WARN[5] if ((x + y) % 4) < 2 else DARK[0])
    for x in range(1, 3):
        for y in range(1, 16):
            t.set(x, y, WARN[5] if ((x + y) % 4) < 2 else DARK[0])
    t.hline(3, 15, 3, OLIVE[1])
    t.vline(3, 3, 15, OLIVE[1])
    # The well's coaming: a steel race round the hole, bolted every three pixels.
    for k in range(5, 16):
        t.set(k, 5, STEEL[5])
        t.set(k, 6, STEEL[3])
        t.set(5, k, STEEL[5])
        t.set(6, k, STEEL[3])
    t.set(5, 5, STEEL[6])
    for k in range(8, 16, 3):
        t.set(k, 5, STEEL[6])
        t.set(k, 6, STEEL[1])
        t.set(5, k, STEEL[6])
        t.set(6, k, STEEL[1])
    t.hline(4, 15, 4, OLIVE[2])
    t.vline(4, 4, 15, OLIVE[2])
    # Under the coaming the well itself (hidden by the hole, painted dark in case a face reaches it).
    t.rect(7, 7, 15, 15, DARK[1])
    return t


@block("interceptor_battery_well")
def interceptor_battery_well():
    """The turret well's walls and floor, and the plinth's underside: dark steel with a floor grating."""
    t = Tex(seed=1102)
    brushed(t, DARK, base=1.8, amp=0.4, seed=1102)
    for y in range(1, 16, 4):
        t.hline(0, 15, y, DARK[0])
        t.hline(0, 15, y + 1, DARK[3])
    for x in range(3, 16, 4):
        t.vline(x, 0, 15, shade(DARK[1], 0.85))
    return t


# ======================================================================================
# the turret: a steel turntable on the well's race and an olive yoke holding the canister's trunnions
# ======================================================================================

@block("interceptor_battery_turret")
def interceptor_battery_turret():
    """The yoke's armour: olive plate a shade lighter than the base, bevelled, bolted at the corners."""
    t = Tex(seed=1103)
    _olive_plate(t, 1103, base=3.9, amp=0.5)
    bevel(t, 0, 0, 15, 15, OLIVE, hi=OLIVE[6], lo=OLIVE[1])
    outline(t, 0, 0, 15, 15, OLIVE[0])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(t, x, y, OLIVE)
    # A stencilled white bar, the marking of a rotating part.
    t.hline(4, 11, 7, mix(HULL[4], OLIVE[4], 0.3))
    t.hline(4, 11, 8, mix(HULL[2], OLIVE[3], 0.4))
    return t


@block("interceptor_battery_ring")
def interceptor_battery_ring():
    """The turntable and bearing caps: bare steel, a bolt circle and the slew gear's teeth along the foot."""
    t = Tex(seed=1104)
    brushed(t, STEEL, base=3.4, amp=0.6, seed=1104)
    t.hline(0, 15, 0, STEEL[6])
    for x in range(1, 16, 4):
        bolt(t, x, 4, STEEL)
    t.hline(0, 15, 9, STEEL[1])
    for x in range(16):
        t.set(x, 12, DARK[1] if x % 2 else STEEL[4])
        t.set(x, 13, DARK[0] if x % 2 else STEEL[2])
    t.hline(0, 15, 15, DARK[0])
    return t


# ======================================================================================
# the canister: four tubes in an olive box, their mouths in a recessed bulkhead
# ======================================================================================

@block("interceptor_battery_canister")
def interceptor_battery_canister():
    """The canister's skin; rows run along it (its up and down faces are turned to match): rivet lines at both edges, a
    yellow band that marks a live launcher and a pale stencil plate."""
    t = Tex(seed=1105)
    _olive_plate(t, 1105, base=3.6, amp=0.45)
    t.hline(0, 15, 0, OLIVE[6])
    t.hline(0, 15, 15, OLIVE[0])
    for x in range(1, 16, 3):
        t.set(x, 1, OLIVE[5])
        t.set(x, 14, OLIVE[1])
    t.hline(0, 15, 2, OLIVE[2])
    t.hline(0, 15, 13, OLIVE[2])
    t.hline(0, 15, 5, WARN[4])
    t.hline(0, 15, 6, WARN[2])
    # A stencilled data plate and the lifting points' white ticks.
    t.rect(9, 8, 13, 10, mix(HULL[3], OLIVE[4], 0.35))
    t.hline(10, 12, 9, OLIVE[3])
    t.set(13, 10, OLIVE[2])
    for x in (2, 3):
        t.set(x, 9, HULL[4])
        t.set(x, 10, HULL[2])
    return t


@block("interceptor_battery_canister_front")
def interceptor_battery_canister_front():
    """The bulkhead the tubes open from, at the back of the canister's mouth: four dark bores centred on (4, 4), (12, 4),
    (4, 12) and (12, 12), each in a steel rim."""
    t = Tex(seed=1106)
    brushed(t, OLIVE, base=2.2, amp=0.4, seed=1106)
    for cx, cy in ((4, 4), (12, 4), (4, 12), (12, 12)):
        t.disc(cx, cy, 3.0, STEEL[2])
        t.ring(cx, cy, 3.0, 2.4, STEEL[4])
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d < 2.3:
                    t.set(x, y, DARK[0] if d < 1.2 else mix(DARK[0], DARK[2], (d - 1.2) / 1.1))
        t.set(cx - 2, cy - 2, STEEL[6])
    return t


@block("interceptor_battery_canister_back")
def interceptor_battery_canister_back():
    """The canister's rear: the umbilical connector the battery fires the rounds through, and hazard corners."""
    t = Tex(seed=1107)
    _olive_plate(t, 1107, base=3.3, amp=0.45)
    bevel(t, 0, 0, 15, 15, OLIVE, hi=OLIVE[5], lo=OLIVE[1])
    for (x0, y0) in ((1, 1), (12, 1), (1, 12), (12, 12)):
        for y in range(y0, y0 + 3):
            for x in range(x0, x0 + 3):
                t.set(x, y, WARN[5] if (x - y) % 4 < 2 else DARK[0])
    t.disc(8, 8, 4.2, STEEL[2])
    t.ring(8, 8, 4.2, 3.4, STEEL[5])
    t.disc(8, 8, 2.6, DARK[1])
    for px, py in ((7, 7), (9, 7), (7, 9), (9, 9), (8, 8)):
        t.set(px, py, BRASS[4])
    t.set(5, 5, STEEL[6])
    return t


@block("interceptor_battery_tube_cover")
def interceptor_battery_tube_cover():
    """A tube's frangible cover, centred (the model maps its middle 7 px): pale composite scored in a cross so the round
    breaks through it, a red ring round the edge."""
    t = Tex(seed=1108)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1108, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, HULL.t(0.55 + 0.2 * n))
    t.frame(4, 4, 11, 11, LAMP_RED[2])
    t.frame(5, 5, 10, 10, LAMP_RED[3])
    for k in range(6, 10):
        t.set(k, k, HULL[1])
        t.set(15 - k, k, HULL[1])
    t.set(6, 6, HULL[6])
    return t


# ======================================================================================
# the interceptor round: white body, ceramic radome, dark fins
# ======================================================================================

@block("interceptor_round_body")
def interceptor_round_body():
    """The round's body, rows top to bottom along it: the radome joint, painted body with a roll mark, the yellow live band,
    and the dark motor case at the tail."""
    t = Tex(seed=1110)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1110, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, HULL.t(0.62 + 0.18 * n))
    t.hline(0, 15, 0, DARK[2])
    t.hline(0, 15, 1, DARK[4])
    for y in range(3, 7):
        for x in range(16):
            if (x // 2) % 4 == 0:
                t.set(x, y, DARK[1])
    t.hline(0, 15, 9, WARN[5])
    t.hline(0, 15, 10, WARN[3])
    t.hline(0, 15, 13, DARK[3])
    t.hline(0, 15, 14, DARK[2])
    t.hline(0, 15, 15, DARK[1])
    return t


@block("interceptor_round_nose")
def interceptor_round_nose():
    """The radome: off-white ceramic, darker to its tip (top rows)."""
    t = Tex(seed=1111)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1111, 16, 16, octaves=2, scale=5.0)
            t.set(x, y, RADOME.t(0.35 + 0.5 * y / 15 + 0.12 * n))
    t.hline(0, 15, 15, DARK[3])
    return t


# ======================================================================================
# the kill vehicle: the interceptor rocket's top section, a seeker and divert thrusters instead of a warhead
# ======================================================================================

@block("rocket_kill_vehicle_hull")
def rocket_kill_vehicle_hull():
    """Mapped one pixel a pixel up the kill vehicle (v = 16 - y). Rows 0-7 (y 8..16): the seeker's housing, black graphite
    composite with faint panel lines. Rows 8-15 (y 0..8): the divert propulsion stage wrapped in crinkled gold insulation
    foil. No markings: this one kills by hitting."""
    t = Tex(seed=1120)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1120, 16, 16, octaves=2, scale=4.0)
            if y < 8:
                t.set(x, y, GRAPHITE.t(0.5 + 0.18 * (n - 0.5) + 0.06 * (8 - x) / 8))
            else:
                crinkle = fbm(x, y, 1121, 16, 16, octaves=3, scale=8.0)
                t.set(x, y, FOIL.t(max(0.0, min(1.0, 0.6 + 0.32 * (crinkle - 0.5) + 0.08 * (8 - x) / 8))))
    for x in range(16):
        t.set(x, 3, GRAPHITE[2])
        t.set(x, 7, GRAPHITE[1])
        t.set(x, 8, FOIL[2])
    return t


@block("rocket_kill_vehicle_seeker")
def rocket_kill_vehicle_seeker():
    """The seeker's dome: dark sapphire glass with a cyan sheen and a white glint up and left."""
    t = Tex(seed=1122)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 6, y + 0.5 - 6)
            n = fbm(x, y, 1122, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, SEEKER.t(max(0.0, min(1.0, 0.62 - d / 16 + 0.12 * n))))
    for x, y in ((4, 4), (5, 4), (4, 5)):
        t.set(x, y, SEEKER[6])
    t.set(5, 5, WHITE)
    t.hline(0, 15, 15, SEEKER[0])
    return t


# ======================================================================================
# items: the interceptor round and its part-built form
# ======================================================================================

# The round lying from its tail (bottom left) to its radome (top right), three pixels a row across: lit top-left edge (W),
# body (w), shaded edge (g); the yellow live band (Y y), the radome (N n) and its dark tip (t), the motor case (M m k), the
# fins (F lit, f in shadow) and the nozzle (E).
INTERCEPTOR = [
    "................",
    "..............t.",
    ".............Nn.",
    "...........NNnn.",
    "..........NNNn..",
    ".........kkkk...",
    "........Wwwg....",
    ".......Wwwg.....",
    "......YYYy......",
    ".....Wwwg.......",
    "...FWwwg........",
    ".FFMmmk.........",
    "FfMmmkf.........",
    "..EEff..........",
    "..Eff...........",
    "................",
]

# The same round on the assembly line: the rear skinned and primed, the front an open frame (s steel rails, G guidance
# board, C copper wiring) with a loose wire where the radome will go; no fins, no live band yet.
INCOMPLETE_INTERCEPTOR = [
    "................",
    "................",
    ".............c..",
    "............C...",
    "..........sGCs..",
    ".........sCGs...",
    "........sGCs....",
    ".......sCGs.....",
    "......Wwwg......",
    ".....Wwwg.......",
    "....Wwwg........",
    "...Mmmk.........",
    "..Mmmk..........",
    "..EE............",
    "..E.............",
    "................",
]


def _paint(art, colours, seed):
    t = Tex(16, 16, seed)
    t.stamp(art, colours)
    item_outline(t, DARK[0])
    return t


@item("interceptor")
def interceptor():
    return _paint(INTERCEPTOR, {
        "W": HULL[6], "w": HULL[4], "g": HULL[2], "Y": WARN[5], "y": WARN[3], "M": DARK[5], "m": DARK[4], "k": DARK[3],
        "N": RADOME[5],
        "n": RADOME[3], "t": DARK[2], "F": DARK[5], "f": DARK[3], "E": DARK[1]}, 1130)


@item("incomplete_interceptor")
def incomplete_interceptor():
    return _paint(INCOMPLETE_INTERCEPTOR, {
        "W": HULL[5], "w": HULL[3], "g": HULL[1], "M": DARK[5], "m": DARK[4], "k": DARK[3], "E": DARK[1],
        "s": STEEL[4], "G": RADIUM[3], "C": COPPER[4], "c": COPPER[5]}, 1134)


# ======================================================================================
# the battery's screen: an olive console like the launch controller's (BatteryLayout.java holds the same numbers)
# ======================================================================================

def _glyph_down(t, cx, cy, c):
    for k in range(4):
        t.hline(cx - 3 + k, cx + 3 - k, cy - 1 + k, c)


def _glyph_up(t, cx, cy, c):
    for k in range(4):
        t.hline(cx - 3 + k, cx + 3 - k, cy + 2 - k, c)


def _glyph_here(t, cx, cy, c):
    t.ring(cx + 0.5, cy + 0.5, 3.6, 2.6, c)
    t.set(cx, cy, c)
    for d in (-5, 5):
        t.set(cx + d, cy, c)
        t.set(cx, cy + d, c)


def _scope(t):
    """The schematic: a dark phosphor scope seen from above with range rings, bearing ticks and a north mark. The screen
    draws the battery's footprint, the turret's heading and the bearing it is turning to over it."""
    x0, y0, x1, y1 = _box(L.BATTERY_SCHEMATIC)
    _phosphor(t, L.BATTERY_SCHEMATIC, 1141, lo=0.22, hi=0.26)
    cx = (x0 + x1 + 1) / 2
    cy = (y0 + y1 + 1) / 2
    radius = min(x1 - x0, y1 - y0) / 2 - 5
    # Rings at a third, two thirds and the edge; the outer one solid, the others dotted.
    for frac, solid in ((1 / 3, False), (2 / 3, False), (1.0, True)):
        r = radius * frac
        steps = int(2 * math.pi * r * 2)
        for k in range(steps):
            ang = 2 * math.pi * k / steps
            if not solid and (k // 3) % 2:
                continue
            t.set(int(cx + math.cos(ang) * r), int(cy + math.sin(ang) * r), PHOSPHOR[6] if solid else PHOSPHOR[5])
    # Cross hairs, dotted.
    for k in range(int(cx - radius) + 2, int(cx + radius) - 1, 2):
        t.set(k, int(cy), PHOSPHOR[4])
    for k in range(int(cy - radius) + 2, int(cy + radius) - 1, 2):
        t.set(int(cx), k, PHOSPHOR[4])
    # Bearing ticks outside the outer ring every 30 degrees, longer at the cardinal points.
    for deg in range(0, 360, 30):
        a = math.radians(deg)
        r1 = radius + (4 if deg % 90 == 0 else 2.5)
        for k in range(6):
            r = radius + 1 + (r1 - radius - 1) * k / 5
            t.set(int(cx + math.sin(a) * r), int(cy - math.cos(a) * r), RADIUM[6] if deg % 90 == 0 else PHOSPHOR[6])
    # North: a filled pointer above the outer ring.
    ny = int(cy - radius) - 6
    for k in range(4):
        t.hline(int(cx) - k, int(cx) + k, ny + k, RADIUM[6])
    _brackets(t, L.BATTERY_SCHEMATIC, PHOSPHOR[5], arm=4)


@gui("interceptor_battery_screen")
def interceptor_battery_screen():
    t = _olive_panel(L.BATTERY_WIDTH, L.BATTERY_HEIGHT, 1140)
    # Title strip, a short hazard block at its right end.
    x0, y0, x1, y1 = _box(L.BATTERY_TITLE)
    t.rect(x0, y0, x1, y1, OLIVE[1])
    for x in range(x0, x1 + 1, 4):
        t.set(x, y1, OLIVE[2])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[0])
    t.hline(x0 - 1, x1 + 1, y1 + 2, OLIVE[5])
    _stripes(t, x1 - 10, y0 + 1, x1 - 1, y1 - 1, period=4)
    outline(t, x1 - 11, y0, x1, y1, OLIVE[0])
    # The schematic, and the groove between it and the controls.
    _scope(t)
    sx0, sy0, sx1, sy1 = _box(L.BATTERY_SCHEMATIC)
    _groove(t, sx1 + 2, sy0, sx1 + 2, sy1)
    # Tubes: a sunk rack holding the four slots, a cross rail between them.
    _sunk(t, L.BATTERY_TUBES, fill=OLIVE[2])
    tx0, ty0, tx1, ty1 = _box(L.BATTERY_TUBES)
    brushed(t, OLIVE, tx0, ty0, tx1, ty1, base=2.4, amp=0.35, seed=1142, scale=13.0)
    for i in range(4):
        _slot(t, L.BATTERY_SLOT_X[i], L.BATTERY_SLOT_Y[i])
    # Tube status: a phosphor list, a row per tube with a faint rule between them.
    _phosphor(t, L.BATTERY_TUBE_STATUS, 1143)
    ux0, uy0, ux1, uy1 = _box(L.BATTERY_TUBE_STATUS)
    row = L.BATTERY_TUBE_STATUS[3] // 4
    for k in range(1, 4):
        for x in range(ux0 + 3, ux1 - 1, 2):
            t.set(x, uy0 + k * row, PHOSPHOR[4])
    # Status, radar code and its lamp.
    _phosphor(t, L.BATTERY_STATUS, 1144)
    _brackets(t, L.BATTERY_STATUS, PHOSPHOR[5], arm=3, inset=2)
    _phosphor(t, L.BATTERY_CODE, 1145)
    lx, ly, lw, lh = L.BATTERY_CODE_LAMP
    _sunk(t, L.BATTERY_CODE_LAMP, fill=OLIVE[1])
    _lamp(t, lx + 1, ly + 1, lw - 2, LAMP_GREEN, lit=False)
    # Automatic mode and doctrine keys.
    for r in (L.BATTERY_AUTO, L.BATTERY_DOCTRINE):
        _sunk(t, r)
        _button(t, *r, 0)
    ax0, ay0, ax1, ay1 = _box(L.BATTERY_AUTO)
    t.rect(ax0 + 2, ay0 + 3, ax0 + 3, ay1 - 3, LAMP_AMBER[3])
    # Defended zone: down, radius readout, up, and the key that centres it on the battery.
    for r, glyph in ((L.BATTERY_ZONE_DOWN, _glyph_down), (L.BATTERY_ZONE_UP, _glyph_up), (L.BATTERY_ZONE_HERE, _glyph_here)):
        _sunk(t, r)
        _button(t, *r, 0)
        glyph(t, r[0] + r[2] // 2, r[1] + r[3] // 2, RADIUM[6])
    _phosphor(t, L.BATTERY_ZONE, 1146)
    # Inventory, on a sunk plate, with louvres either side.
    _sunk(t, L.BATTERY_INVENTORY, fill=OLIVE[2])
    for r in range(3):
        for c in range(9):
            _slot(t, L.BATTERY_INV_X + c * 18, L.BATTERY_INV_Y + r * 18)
    for c in range(9):
        _slot(t, L.BATTERY_INV_X + c * 18, L.BATTERY_HOTBAR_Y)
    ix0, iy0, ix1, iy1 = _box(L.BATTERY_INVENTORY)
    _louvres(t, sx0, iy0 + 2, ix0 - 5, iy1 - 2)
    right = L.BATTERY_STATUS[0] + L.BATTERY_STATUS[2] - 1
    _louvres(t, ix1 + 5, iy0 + 2, right, iy1 - 2)
    return t

"""Textures of satellites: the rocket's payload fairing, the ground station, the two satellites (items and the mini models the
holographic table and the sky draw) and the part-built satellite.

The fairing is the white of the rocket's hull (``tex_missile.HULL``), with a blue band and a seam down the plane it splits on;
its faces are mapped one pixel a pixel from the block model (``v = 16 - y``), so a row of the side textures is a height in the
block. The ground station is concrete and steel with Create's brass on what turns; its dish and the satellites' parts are drawn at
the same one-to-one scale by the partial models under ``models/block/ground_station/`` and ``models/block/satellite/``.

Imported at the end of ``blocks.py``.
"""

import math

from blocks import block
from items import item
from palette import BRASS, CONCRETE, COPPER, CYAN, DANGER, DARK, LITHIUM, MIMIC_BLUE, STEEL, WARN
from pixelkit import Ramp, Tex, bevel, brushed, fbm, item_outline, outline, shade

import missile_layouts as L
from gui import gui
from tex_launch import LAMP_GREEN, _brackets, _groove, _lamp, _louvres, _olive_panel, _phosphor, _stripes, _sunk
from tex_missile import HULL, OLIVE, PHOSPHOR, _box

# Gold multi-layer insulation, crinkled; and the deep blue of solar cells.
FOIL = Ramp("#5a3a0c", "#8a5c14", "#b8861f", "#d9a933", "#efc759", "#fbe08c", "#fff4c8")
SOLAR = Ramp("#0b1024", "#131c3d", "#1d2b5c", "#2a3f80", "#3c58a6", "#5d7fcb", "#a9c2f0")
# The infrared telescope's germanium lens: dark, with a warm sheen.
LENS = Ramp("#0d0708", "#1f1012", "#3a1a1c", "#5c2a26", "#8c4a32", "#c47f48", "#f2c48a")


# ======================================================================================
# the payload fairing: the rocket's nose when it carries a satellite
# ======================================================================================

def _fairing_skin(t, seed):
    """Glossy white paint, a shade cleaner than the rest of the hull, lit a little more on the left."""
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, HULL.t(0.7 + 0.16 * n - 0.04 * x / 15))


def _fairing_common(t):
    """What every side shows (rows are heights, v = 16 - y): a rivet line where the cylinder meets the ogive (row 6, y 9.5)
    and a darker lip at the shell's foot (row 13, y 2..3), just above the separation ring."""
    for x in range(16):
        t.set(x, 13, shade(t.get(x, 13), 0.9))
        t.set(x, 6, shade(t.get(x, 6), 0.94))
        if x % 2 == 0:
            t.set(x, 7, HULL[2])


@block("rocket_satellite_fairing")
def rocket_satellite_fairing():
    """The fairing's east and west sides, away from its seam: clean glossy white."""
    t = Tex(seed=2400)
    _fairing_skin(t, 2400)
    _fairing_common(t)
    return t


@block("rocket_satellite_fairing_side")
def rocket_satellite_fairing_side():
    """The north and south sides, which the seam runs down: a groove between columns 7 and 8 (the two halves meet at x = 8),
    with the separation bolts either side of it."""
    t = Tex(seed=2401)
    _fairing_skin(t, 2401)
    _fairing_common(t)
    for y in range(0, 14):
        t.set(7, y, HULL[1])
        t.set(8, y, HULL[3])
    for y in (2, 5, 8, 11):
        t.set(6, y, STEEL[2])
        t.set(9, y, STEEL[2])
    return t


@block("rocket_satellite_fairing_top")
def rocket_satellite_fairing_top():
    """The ogive's steps seen from above (u = x, v = z): white, lit from the north-west, the seam along z at x = 8 and the nose
    cap in the middle."""
    t = Tex(seed=2402)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2402, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, HULL.t(0.74 + 0.14 * n - 0.1 * (x + y) / 30))
    for y in range(16):
        t.set(7, y, HULL[1])
        t.set(8, y, HULL[3])
    t.rect(7, 7, 8, 8, STEEL[3])
    t.set(7, 7, STEEL[5])
    return t


@block("rocket_satellite_fairing_inner")
def rocket_satellite_fairing_inner():
    """Inside a half, where it parts from the other: a quilted acoustic blanket, off-white, sewn in squares."""
    t = Tex(seed=2403)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2403, 16, 16, octaves=2, scale=6.0)
            lx, ly = x % 4, y % 4
            v = 0.58 + 0.16 * n + (0.08 if (lx, ly) in ((1, 1), (1, 2), (2, 1)) else 0)
            t.set(x, y, LITHIUM.t(v))
            if lx == 3 or ly == 3:
                t.set(x, y, LITHIUM[2])
    return t


# ======================================================================================
# the ground station: a concrete plinth, a steel azimuth housing on a brass race, a white dish on a yoke
# ======================================================================================

@block("ground_station_plinth")
def ground_station_plinth():
    """The plinth's sides, rows 12..15 (it is four pixels high): a steel lip, then concrete with a strip of hazard paint. The rows
    above are the same concrete, for the particle."""
    t = Tex(seed=2410)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2410, 16, 16, octaves=3, scale=5.0)
            t.set(x, y, CONCRETE.t(0.45 + 0.25 * n))
    t.hline(0, 15, 12, STEEL[5])
    for x in range(16):
        t.set(x, 13, WARN[4] if (x // 2) % 2 == 0 else DARK[1])
    t.hline(0, 15, 15, CONCRETE[1])
    for x in (0, 15):
        t.set(x, 14, CONCRETE[2])
    return t


@block("ground_station_deck")
def ground_station_deck():
    """The plinth from above: concrete inside a steel edge, an anchor bolt at each corner."""
    t = Tex(seed=2411)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2411, 16, 16, octaves=3, scale=5.0)
            t.set(x, y, CONCRETE.t(0.5 + 0.25 * n))
    outline(t, 0, 0, 15, 15, STEEL[2])
    t.hline(1, 14, 1, STEEL[5])
    t.vline(1, 1, 14, STEEL[5])
    t.hline(1, 14, 14, CONCRETE[2])
    t.vline(14, 1, 14, CONCRETE[2])
    # Expansion joints between the slabs.
    for i in range(2, 14):
        t.set(i, 8, shade(t.get(i, 8), 0.86))
        t.set(8, i, shade(t.get(8, i), 0.86))
    return t


@block("ground_station_underside")
def ground_station_underside():
    """The plinth from below, round the hole the shaft comes up through."""
    t = Tex(seed=2412)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2412, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, CONCRETE.t(0.32 + 0.2 * n))
    outline(t, 4, 4, 11, 11, DARK[1])
    return t


@block("ground_station_housing")
def ground_station_housing():
    """The azimuth housing's sides (rows 8..11, four pixels high): ribbed steel, a brass data plate in the middle. Its top
    (u = x, v = z) is the same steel round a dark centre, under the turntable."""
    t = Tex(seed=2413)
    brushed(t, STEEL, base=3.0, amp=0.6, seed=2413, axis="v")
    for x in range(0, 16, 3):
        t.vline(x, 0, 15, STEEL[2])
        t.vline(x + 1, 0, 15, STEEL[4])
    t.hline(0, 15, 8, STEEL[5])
    t.hline(0, 15, 11, STEEL[1])
    t.rect(6, 9, 9, 10, BRASS[3])
    t.hline(6, 9, 9, BRASS[5])
    t.set(9, 10, BRASS[1])
    return t


@block("ground_station_brass")
def ground_station_brass():
    """Brass for what turns or bears: the azimuth race, the yoke's bearing caps, the anchor bolts. Lit edge on top, bolts
    round the middle."""
    t = Tex(seed=2414)
    brushed(t, BRASS, base=3.3, amp=0.6, seed=2414)
    t.hline(0, 15, 0, BRASS[6])
    t.hline(0, 15, 15, BRASS[1])
    for x in range(1, 16, 4):
        t.set(x, 7, BRASS[6])
        t.set(x + 1, 8, BRASS[1])
    return t


@block("ground_station_lamp")
def ground_station_lamp():
    """The status lamp's lens: green glass, a hot spot up and left."""
    t = Tex(seed=2415)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 6, y + 0.5 - 6)
            t.set(x, y, LAMP_GREEN.t(max(0.0, min(1.0, 0.85 - d / 14))))
    t.rect(4, 4, 5, 5, LAMP_GREEN[6])
    outline(t, 0, 0, 15, 15, LAMP_GREEN[1])
    return t


@block("ground_station_mount")
def ground_station_mount():
    """The yoke and its arms: white-painted steel, a dark edge top and bottom, a few rivets."""
    t = Tex(seed=2416)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2416, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, HULL.t(0.55 + 0.2 * n))
    outline(t, 0, 0, 15, 15, HULL[1])
    t.hline(1, 14, 1, HULL[6])
    t.vline(1, 1, 14, HULL[5])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.set(x, y, STEEL[2])
    return t


@block("ground_station_turntable")
def ground_station_turntable():
    """The turntable from above (u = x, v = z, its disc 3..12): dark steel, a brass rim. Its sides take rows 0..1: the rim."""
    t = Tex(seed=2417)
    brushed(t, DARK, base=2.6, amp=0.5, seed=2417)
    t.ring(8, 8, 5.2, 4.2, BRASS[3])
    t.ring(8, 8, 4.2, 3.6, BRASS[1])
    t.hline(0, 15, 0, BRASS[4])
    t.hline(0, 15, 1, BRASS[2])
    for x, y in ((8, 4), (4, 8), (11, 8), (8, 11)):
        t.set(x, y, BRASS[6])
    return t


def _dish_bowl(t, seed, cx=8.0, cy=8.0, r=6.0):
    """A white dish face inside radius ``r``: shaded like a bowl (dark towards the top-left rim, light at the bottom right) with
    a ring of panel joints."""
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            n = fbm(x, y, seed, 16, 16, octaves=2, scale=5.0)
            if d > r:
                t.set(x, y, HULL[2])
                continue
            v = 0.72 + 0.14 * n + 0.12 * (dx + dy) / max(1.0, r) * (d / r)
            t.set(x, y, HULL.t(max(0.0, min(1.0, v))))
    t.ring(cx, cy, r, r - 0.8, HULL[3])
    t.ring(cx, cy, r * 0.55, r * 0.55 - 0.6, HULL[4])


@block("ground_station_dish_face")
def ground_station_dish_face():
    """The dish's reflector, seen face on (its disc fills 2..13): white panels, a rim and a dark vertex where the feed looks."""
    t = Tex(seed=2418)
    _dish_bowl(t, 2418)
    t.rect(7, 7, 8, 8, HULL[1])
    t.set(7, 7, DARK[2])
    return t


@block("ground_station_dish_back")
def ground_station_dish_back():
    """The back of the dish and its edges: grey steel with radial ribs to the hub."""
    t = Tex(seed=2419)
    brushed(t, STEEL, base=3.6, amp=0.5, seed=2419)
    for i in range(16):
        t.set(i, 8, STEEL[2])
        t.set(8, i, STEEL[2])
        t.set(i, i, STEEL[2])
        t.set(15 - i, i, STEEL[2])
    t.rect(6, 6, 9, 9, DARK[2])
    bevel(t, 6, 6, 9, 9, DARK)
    return t


@block("ground_station_feed")
def ground_station_feed():
    """The feed horn and its struts: copper with a dark mouth in the middle (6..9), which looks back into the dish."""
    t = Tex(seed=2420)
    brushed(t, COPPER, base=3.4, amp=0.6, seed=2420)
    t.rect(6, 6, 9, 9, DARK[0])
    t.rect(7, 7, 8, 8, DARK[2])
    t.hline(0, 15, 0, COPPER[6])
    t.hline(0, 15, 15, COPPER[1])
    return t


# ======================================================================================
# the satellites' mini models: foil, cells, a white radiator and the infrared telescope
# ======================================================================================

@block("satellite_foil")
def satellite_foil():
    """Gold multi-layer insulation, crinkled into soft facets."""
    t = Tex(seed=2430)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2430, 16, 16, octaves=3, scale=4.0)
            m = fbm(x + 7, y + 3, 2431, 16, 16, octaves=1, scale=8.0)
            t.set(x, y, FOIL.t(max(0.0, min(1.0, 0.3 + 0.6 * n + (0.15 if m > 0.72 else 0)))))
    return t


@block("satellite_solar")
def satellite_solar():
    """Solar cells: deep blue squares on a silver grid, a frame round the edge."""
    t = Tex(seed=2432)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2432, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, SOLAR.t(0.35 + 0.25 * n + 0.15 * (1 - (x + y) / 30)))
    for i in range(0, 16, 4):
        t.hline(0, 15, i, STEEL[3])
        t.vline(i, 0, 15, STEEL[3])
    outline(t, 0, 0, 15, 15, STEEL[5])
    return t


@block("satellite_radiator")
def satellite_radiator():
    """The bus's white radiator panels, with a few dark joints and a pair of thruster ports."""
    t = Tex(seed=2433)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2433, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, HULL.t(0.66 + 0.2 * n))
    for i in (5, 10):
        t.vline(i, 0, 15, HULL[2])
    t.hline(0, 15, 0, HULL[6])
    outline(t, 0, 0, 15, 15, HULL[1])
    t.rect(2, 12, 3, 13, DARK[1])
    t.rect(12, 12, 13, 13, DARK[1])
    return t


@block("satellite_telescope")
def satellite_telescope():
    """The early-warning satellite's infrared telescope: a dark tube; seen end on (its middle, 4..11), the germanium lens."""
    t = Tex(seed=2434)
    brushed(t, DARK, base=2.4, amp=0.5, seed=2434, axis="v")
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d <= 3.6:
                t.set(x, y, LENS.t(max(0.0, min(1.0, 0.7 - d / 6))))
            elif d <= 4.4:
                t.set(x, y, DARK[0])
    t.set(6, 6, LENS[6])
    t.hline(0, 15, 0, DARK[5])
    return t


# ======================================================================================
# items: the two satellites and the bare bus
# ======================================================================================

def _wing(t, x0, y0, x1, y1):
    """A solar wing: blue cells between silver frame rails."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t.set(x, y, SOLAR[4] if (x + y) % 2 == 0 else SOLAR[3])
    t.hline(x0, x1, y0, STEEL[6])
    t.hline(x0, x1, y1, STEEL[2])
    for x in range(x0 + 2, x1, 3):
        t.vline(x, y0 + 1, y1 - 1, SOLAR[1])


def _foil_body(t, x0, y0, x1, y1, seed):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, 16, 16, octaves=2, scale=5.0)
            v = 0.45 + 0.4 * n + 0.2 * (0.5 - (x - x0) / max(1, x1 - x0))
            t.set(x, y, FOIL.t(max(0.15, min(1.0, v))))
    t.vline(x0, y0, y1, FOIL[6])
    t.vline(x1, y0, y1, FOIL[1])


@item("early_warning_satellite")
def early_warning_satellite():
    """A gold bus between two solar wings, with the big infrared telescope under it looking down, its lens glinting."""
    t = Tex(16, 16, 2440)
    _wing(t, 0, 3, 3, 7)
    _wing(t, 12, 3, 15, 7)
    t.hline(4, 4, 5, STEEL[4])
    t.hline(11, 11, 5, STEEL[2])
    _foil_body(t, 5, 2, 10, 8, 2440)
    # An antenna mast on top.
    t.vline(8, 0, 1, STEEL[5])
    # The telescope: a broad dark tube down from the bus, its sunshade flared at the end.
    for y in range(9, 14):
        t.hline(6, 9, y, DARK[3])
        t.set(6, y, DARK[5])
        t.set(9, y, DARK[1])
    t.hline(5, 10, 14, DARK[2])
    t.hline(6, 9, 14, LENS[2])
    t.set(7, 14, LENS[6])
    t.hline(6, 9, 9, DARK[1])
    item_outline(t, DARK[0])
    return t


def _bowl(t, x0, x1, y, depth, down=False):
    """A dish seen from the side: a white bowl ``depth`` rows deep opening up (or down)."""
    w = x1 - x0
    for i in range(depth):
        inset = round(w * 0.5 * (i / depth) ** 1.6) if depth > 1 else 0
        yy = y - i if down else y + i
        t.hline(x0 + inset, x1 - inset, yy, HULL[5] if i == 0 else HULL[3])
    t.set(x0, y, HULL[6])
    t.set(x1, y, HULL[2])


@item("comms_satellite")
def comms_satellite():
    """A white box bus between long solar wings, a dish on a boom above and another below, a stub antenna."""
    t = Tex(16, 16, 2441)
    _wing(t, 0, 6, 3, 9)
    _wing(t, 12, 6, 15, 9)
    t.hline(4, 4, 7, STEEL[4])
    t.hline(11, 11, 7, STEEL[2])
    for y in range(4, 12):
        for x in range(5, 11):
            n = fbm(x, y, 2441, 16, 16, octaves=2, scale=5.0)
            t.set(x, y, HULL.t(0.62 + 0.25 * n - 0.03 * (x - 5)))
    t.vline(5, 4, 11, HULL[6])
    t.vline(10, 4, 11, HULL[2])
    t.hline(5, 10, 8, FOIL[3])
    t.hline(5, 10, 9, FOIL[2])
    # The upper dish, opening up and to the left on a short boom.
    t.set(7, 3, STEEL[4])
    _bowl(t, 3, 9, 2, 2, down=True)
    t.set(6, 0, COPPER[4])
    # The lower dish, opening down.
    t.set(9, 12, STEEL[3])
    _bowl(t, 6, 12, 13, 2)
    t.set(9, 15, COPPER[3])
    item_outline(t, DARK[0])
    return t


@item("incomplete_satellite")
def incomplete_satellite():
    """The bare bus: a steel frame braced corner to corner, brass fittings, a loose bundle of wiring and nothing else yet."""
    t = Tex(16, 16, 2442)
    x0, y0, x1, y1 = 4, 3, 11, 12
    for x in range(x0, x1 + 1):
        t.set(x, y0, STEEL[5])
        t.set(x, y1, STEEL[2])
    for y in range(y0, y1 + 1):
        t.set(x0, y, STEEL[5])
        t.set(x1, y, STEEL[2])
    t.hline(x0, x1, 7, STEEL[3])
    t.line(x0 + 1, y0 + 1, x1 - 1, 6, STEEL[3])
    t.line(x0 + 1, 11, x1 - 1, 8, STEEL[3])
    for x, y in ((x0, y0), (x1, y0), (x0, y1), (x1, y1), (x0, 7), (x1, 7)):
        t.set(x, y, BRASS[4])
    # A loose harness hanging out of the frame, and a single mount plate where the payload will go.
    t.vline(8, 9, 11, COPPER[4])
    t.set(9, 11, DANGER[3])
    t.hline(6, 9, 2, DARK[3])
    t.hline(6, 9, 1, DARK[5])
    item_outline(t, DARK[0])
    return t


# ======================================================================================
# the ground station's screen: an olive console like the battery's (StationLayout.java holds the same numbers)
# ======================================================================================

def _sky(t):
    """The sky plot: a phosphor window looking up, the zenith at the centre and the horizon the outer ring; rings every 30
    degrees of elevation, bearing ticks and north at the top. The screen draws the network's satellites over it."""
    _phosphor(t, L.STATION_SKY, 1151, lo=0.22, hi=0.26)
    cx, cy, radius = L.STATION_SKY_CX + 0.5, L.STATION_SKY_CY + 0.5, L.STATION_SKY_RADIUS
    for frac, solid in ((1 / 3, False), (2 / 3, False), (1.0, True)):
        r = radius * frac
        steps = int(2 * math.pi * r * 2)
        for k in range(steps):
            ang = 2 * math.pi * k / steps
            if not solid and (k // 3) % 2:
                continue
            t.set(int(cx + math.cos(ang) * r), int(cy + math.sin(ang) * r), PHOSPHOR[6] if solid else PHOSPHOR[5])
    for k in range(int(cx - radius) + 2, int(cx + radius) - 1, 2):
        t.set(k, int(cy), PHOSPHOR[4])
    for k in range(int(cy - radius) + 2, int(cy + radius) - 1, 2):
        t.set(int(cx), k, PHOSPHOR[4])
    for deg in range(0, 360, 45):
        a = math.radians(deg)
        for k in range(3):
            r = radius - 1 - k
            t.set(int(cx + math.sin(a) * r), int(cy - math.cos(a) * r), RADIUM_TICK if deg % 90 == 0 else PHOSPHOR[6])
    # The zenith: a small cross.
    t.set(int(cx), int(cy), RADIUM_TICK)
    _brackets(t, L.STATION_SKY, PHOSPHOR[5], arm=4)


RADIUM_TICK = (231, 249, 193, 255)


@gui("ground_station_screen")
def ground_station_screen():
    t = _olive_panel(L.STATION_WIDTH, L.STATION_HEIGHT, 1150)
    # Title strip, a short hazard block at its right end: the dish turns.
    x0, y0, x1, y1 = _box(L.STATION_TITLE)
    t.rect(x0, y0, x1, y1, OLIVE[1])
    for x in range(x0, x1 + 1, 4):
        t.set(x, y1, OLIVE[2])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[0])
    t.hline(x0 - 1, x1 + 1, y1 + 2, OLIVE[5])
    _stripes(t, x1 - 10, y0 + 1, x1 - 1, y1 - 1, period=4)
    outline(t, x1 - 11, y0, x1, y1, OLIVE[0])
    _sky(t)
    sx0, sy0, sx1, sy1 = _box(L.STATION_SKY)
    _groove(t, sx1 + 2, sy0, sx1 + 2, sy1)
    # Link code, its lamp and the dish's speed.
    _phosphor(t, L.STATION_CODE, 1152)
    lx, ly, lw, lh = L.STATION_CODE_LAMP
    _sunk(t, L.STATION_CODE_LAMP, fill=OLIVE[1])
    _lamp(t, lx + 1, ly + 1, lw - 2, LAMP_GREEN, lit=False)
    _phosphor(t, L.STATION_RPM, 1153)
    rx0, ry0, rx1, ry1 = _box(L.STATION_RPM)
    # A dial's arc along the foot of the speed readout.
    for x in range(rx0 + 3, rx1 - 2, 2):
        t.set(x, ry1 - 2, PHOSPHOR[4])
    # Status and the network, with a faint rule between the network's two lines.
    _phosphor(t, L.STATION_STATUS, 1154)
    _brackets(t, L.STATION_STATUS, PHOSPHOR[5], arm=3, inset=2)
    _phosphor(t, L.STATION_NETWORK, 1155)
    nx0, ny0, nx1, ny1 = _box(L.STATION_NETWORK)
    for x in range(nx0 + 3, nx1 - 1, 2):
        t.set(x, (ny0 + ny1) // 2, PHOSPHOR[4])
    return t

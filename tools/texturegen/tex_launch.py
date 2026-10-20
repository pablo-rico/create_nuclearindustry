"""Textures of the launch platform, the Tactical Linker and the launch controller's and command table's screens.

The launch controller is an olive field console with phosphor-green displays, of a family with the radar screen; the command
table's console is the in-world table's dark steel with its brass lip and cyan projection glass. Screen sheets take every
coordinate from ``missile_layouts.py`` (which mirrors ``SiloLayout``/``HoloLayout``) and never hard-code one: the numbers
below are only offsets inside a region. Words are left to the screens; only small glyphs are painted.
"""

import math

import missile_layouts as L
from blocks import block  # noqa: F401
from common import bolt
from gui import gui
from items import item, mask_from, sculpt
from palette import BRASS, CONCRETE, DARK, RADIUM, STEEL, WARN
from pixelkit import Ramp, Tex, bevel, brushed, fbm, hazard, item_outline, mix, outline, shade

from tex_missile import HOLO, OLIVE, PHOSPHOR, _box, _silo_concrete


# ======================================================================================
# shared colour: lamp glass by controller state, the hazard paint, a neutral hull for the cells
# ======================================================================================

LAMP_RED = Ramp("#2e0705", "#5a0f0a", "#8e1a12", "#c42a1c", "#ff4a3a", "#ff8f80", "#ffd6cf")
LAMP_AMBER = Ramp("#2e1e04", "#5c3c08", "#94620e", "#cf8c18", "#ffb347", "#ffd28c", "#fff0d2")
LAMP_GREEN = Ramp("#06240c", "#0f4a1c", "#1b7a30", "#33ad4c", "#7dff9a", "#b4ffc4", "#eaffef")
LAMP_ORANGE = Ramp("#2e1204", "#5c2408", "#963e0e", "#d45e16", "#ff8a3a", "#ffb47e", "#ffe2cc")
LAMP_HOT = Ramp("#3c0303", "#740808", "#b60e0e", "#f01c1c", "#ff5050", "#ffa0a0", "#ffffff")
LAMP_GREY = Ramp("#1c1e1c", "#303330", "#4a4e4a", "#666a66", "#858a85", "#a6aaa6", "#c8ccc8")
# Controller states in order: incomplete, unprepared, ready, armed, countdown, launched.
STATE_LAMPS = (LAMP_RED, LAMP_AMBER, LAMP_GREEN, LAMP_ORANGE, LAMP_HOT, LAMP_GREY)

# Neutral light metal for the diagram's cells, so the screen can tint them by stage.
CELL = Ramp("#3a3c3c", "#7c7f7e", "#a3a6a4", "#c0c3c0", "#d8dad6", "#e9eae6", "#fafaf6")

YELLOW = WARN[5]
BLACK_PAINT = DARK[0]
CLEAR = (0, 0, 0, 0)


def _rgba(c, a):
    return c[0], c[1], c[2], int(a)


def _vgrad(t, x0, y0, x1, y1, top, bottom):
    for y in range(y0, y1 + 1):
        t.hline(x0, x1, y, mix(top, bottom, (y - y0) / max(1, y1 - y0)))


def _stripes(t, x0, y0, x1, y1, period=6, a=YELLOW, b=BLACK_PAINT, wear=0, seed=0):
    """Yellow-black chevron paint, the stripes running down to the right; `wear` scuffs a few pixels."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c = a if ((x - y) % period) < period // 2 else b
            if wear and fbm(x, y, seed, 64, 64, octaves=2, scale=8.0) > 1 - wear:
                c = shade(c, 0.72)
            t.set(x, y, c)


def _lamp(t, x, y, size, ramp, lit=True):
    """A round lamp lens in a dark bezel, lit from the top left; the corners stay transparent."""
    c = size / 2
    r_out = c
    r_glass = c - (1.15 if size >= 9 else 0.95)
    for j in range(size):
        for i in range(size):
            dx, dy = i + 0.5 - c, j + 0.5 - c
            d = math.hypot(dx, dy)
            if d > r_out + 0.05:
                continue
            if d > r_glass:
                # Bezel: bright where it faces the light, dark where it faces away.
                k = (dx + dy) / max(0.01, d)
                t.set(x + i, y + j, DARK[1] if k > 0.35 else (DARK[4] if k < -0.35 else DARK[2]))
                continue
            # Glass: a hot centre a little up and left, falling off to the rim.
            hd = math.hypot(dx + 0.35 * c * 0.4, dy + 0.35 * c * 0.4) / r_glass
            v = 5.2 - 3.4 * hd if lit else 2.4 - 1.4 * hd
            t.set(x + i, y + j, ramp[int(round(max(1, min(6, v))))])
    s = max(1, size // 4)
    t.set(x + s + (1 if size >= 9 else 0), y + s + (1 if size >= 9 else 0), ramp[6] if lit else ramp[3])


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


# ======================================================================================
# the Tactical Linker: a ruggedised olive field handset with a red link lamp and a whip antenna
# ======================================================================================

TACTICAL_BODY = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "....#######.....",
    "...#########....",
    "...#########....",
    "...#########....",
    "...#########....",
    "...#########....",
    "...#########....",
    "...#########....",
    "...#########....",
    "....#######.....",
    "................",
]


@item("tactical_linker")
def tactical_linker():
    t = Tex(16, 16, 943)
    body = mask_from(TACTICAL_BODY)
    sculpt(t, body, OLIVE, base=4.1, seed=943, grain=0.45)
    # Right flank in shadow: the handset is a box, not a slab.
    for y in range(6, 14):
        t.set(11, y, OLIVE[2])
    # Whip antenna on a rubber boot, its tip a bead.
    t.rect(9, 4, 10, 4, DARK[1])
    t.set(10, 3, DARK[2])
    for y in range(0, 3):
        t.set(10, y, STEEL[5] if y else STEEL[6])
    t.set(11, 1, STEEL[2])
    t.set(11, 2, STEEL[2])
    # The link lamp: a red dome on the top left, caught by the light.
    t.set(5, 3, LAMP_RED[4])
    t.set(4, 4, LAMP_RED[4])
    t.set(5, 4, LAMP_RED[6])
    t.set(6, 4, LAMP_RED[2])
    t.hline(4, 6, 5, OLIVE[1])
    # Phosphor display and its bezel.
    t.rect(4, 7, 10, 9, OLIVE[0])
    t.rect(5, 7, 9, 8, PHOSPHOR[4])
    t.set(5, 7, RADIUM[5])
    t.set(6, 8, RADIUM[4])
    t.set(8, 7, RADIUM[3])
    # Keypad: two rows of buttons, and a yellow stencil stripe under the screen.
    for x in (4, 6, 8):
        t.set(x, 11, OLIVE[6])
        t.set(x + 1, 11, OLIVE[1])
        t.set(x, 13, OLIVE[6])
        t.set(x + 1, 13, OLIVE[1])
    t.set(10, 11, LAMP_AMBER[4])
    t.hline(4, 10, 10, mix(OLIVE[3], WARN[4], 0.45))
    item_outline(t, OLIVE[0])
    return t


# ======================================================================================
# launch controller: shared pieces of an olive console
# ======================================================================================

def _olive_panel(w, h, seed):
    t = Tex(w, h, seed)
    brushed(t, OLIVE, base=3.3, amp=0.55, seed=seed, scale=11.0)
    bevel(t, 0, 0, w - 1, h - 1, OLIVE, hi=OLIVE[6], lo=OLIVE[0])
    bevel(t, 1, 1, w - 2, h - 2, OLIVE, hi=OLIVE[5], lo=OLIVE[1])
    outline(t, 0, 0, w - 1, h - 1, OLIVE[0])
    for x, y in ((2, 2), (w - 4, 2), (2, h - 4), (w - 4, h - 4)):
        bolt(t, x, y, OLIVE)
    return t


def _groove(t, x0, y0, x1, y1):
    """A 1+1 px engraved line on the enamel, vertical or horizontal."""
    if x0 == x1:
        t.vline(x0, y0, y1, OLIVE[1])
        t.vline(x0 + 1, y0, y1, OLIVE[5])
    else:
        t.hline(x0, x1, y0, OLIVE[1])
        t.hline(x0, x1, y0 + 1, OLIVE[5])


def _sunk(t, r, fill=None, out=1):
    """A plate sunk into the enamel: shadowed top-left lip, lit bottom-right lip, `out` px outside `r`."""
    x0, y0, x1, y1 = _box(r)
    if fill is not None:
        t.rect(x0, y0, x1, y1, fill)
    a, b = x0 - out, y0 - out
    c, d = x1 + out, y1 + out
    t.hline(a, c, b, OLIVE[0])
    t.vline(a, b, d, OLIVE[0])
    t.hline(a + 1, c, d, OLIVE[5])
    t.vline(c, b + 1, d, OLIVE[5])


def _phosphor(t, r, seed, ramp=PHOSPHOR, lo=0.30, hi=0.30):
    """A phosphor display window: dark glass with faint grain and the lip of its bezel."""
    x0, y0, x1, y1 = _box(r)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, 336, 256, octaves=3, scale=2.0)
            t.set(x, y, mix(ramp[1], ramp[3], lo + hi * n))
    # Glass lip: two dark pixels top-left, a lit pixel bottom-right, and a bezel outside.
    t.hline(x0, x1, y0, OLIVE[0])
    t.vline(x0, y0, y1, OLIVE[0])
    t.hline(x0 + 1, x1, y0 + 1, ramp[0])
    t.vline(x0 + 1, y0 + 1, y1, ramp[0])
    t.hline(x0, x1, y1, OLIVE[5])
    t.vline(x1, y0, y1, OLIVE[5])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[1])


def _brackets(t, r, c, arm=4, inset=3):
    """Corner crop marks inside a display, the reticle of a military readout."""
    x0, y0, x1, y1 = _box(r)
    x0, y0, x1, y1 = x0 + inset, y0 + inset, x1 - inset + 1, y1 - inset + 1
    for (x, y, sx, sy) in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
        for k in range(arm):
            t.set(x + sx * k, y, c)
            t.set(x, y + sy * k, c)


def _slot(t, x, y):
    """An 18 px slot frame round the 16 px item area at (x, y)."""
    t.rect(x - 1, y - 1, x + 16, y + 16, mix(OLIVE[1], OLIVE[2], 0.5))
    t.hline(x - 1, x + 16, y - 1, OLIVE[0])
    t.vline(x - 1, y - 1, y + 16, OLIVE[0])
    t.hline(x, x + 16, y + 16, OLIVE[5])
    t.vline(x + 16, y, y + 16, OLIVE[5])
    t.set(x - 1, y + 16, OLIVE[3])
    t.set(x + 16, y - 1, OLIVE[3])


def _screw(t, x, y):
    """A slotted screw head, 3x3."""
    t.set(x, y, OLIVE[6])
    t.set(x + 1, y, OLIVE[5])
    t.set(x + 2, y, OLIVE[3])
    t.set(x, y + 1, OLIVE[5])
    t.set(x + 1, y + 1, OLIVE[1])
    t.set(x + 2, y + 1, OLIVE[2])
    t.set(x, y + 2, OLIVE[3])
    t.set(x + 1, y + 2, OLIVE[2])
    t.set(x + 2, y + 2, OLIVE[0])


def _louvres(t, x0, y0, x1, y1):
    """Cooling louvres pressed into the enamel."""
    _sunk(t, (x0, y0, x1 - x0 + 1, y1 - y0 + 1), fill=OLIVE[2])
    for y in range(y0 + 2, y1 - 1, 3):
        t.hline(x0 + 2, x1 - 2, y, OLIVE[0])
        t.hline(x0 + 2, x1 - 2, y + 1, OLIVE[4])


# -- sprites, painted the same on the sheets and in the widget sheet ---------------------

def _tab(t, x, y, w, h, active):
    if active:
        _vgrad(t, x, y, x + w - 1, y + h - 1, OLIVE[5], OLIVE[3])
        bevel(t, x, y, x + w - 1, y + h - 1, OLIVE, hi=OLIVE[6], lo=OLIVE[1])
        # A lit phosphor bar along the foot and the tab's indicator.
        t.hline(x + 2, x + w - 3, y + h - 2, RADIUM[5])
        t.hline(x + 3, x + w - 4, y + h - 3, mix(OLIVE[4], RADIUM[4], 0.35))
        t.rect(x + 3, y + 5, x + 4, y + 6, RADIUM[6])
        t.set(x + 3, y + 7, RADIUM[3])
        t.set(x + 4, y + 7, RADIUM[3])
    else:
        _vgrad(t, x, y, x + w - 1, y + h - 1, OLIVE[3], OLIVE[2])
        bevel(t, x, y, x + w - 1, y + h - 1, OLIVE, hi=OLIVE[4], lo=OLIVE[1])
        t.hline(x + 2, x + w - 3, y + h - 2, OLIVE[1])
        t.rect(x + 3, y + 5, x + 4, y + 6, PHOSPHOR[4])
        t.set(x + 3, y + 7, OLIVE[1])
        t.set(x + 4, y + 7, OLIVE[1])
    outline(t, x, y, x + w - 1, y + h - 1, OLIVE[0])


def _button(t, x, y, w, h, state):
    """Launch console push button: 0 normal, 1 hover, 2 disabled, 3 armed (lit red with hazard ends)."""
    x1, y1 = x + w - 1, y + h - 1
    if state == 3:
        _vgrad(t, x, y, x1, y1, LAMP_RED[4], LAMP_RED[2])
        bevel(t, x, y, x1, y1, LAMP_RED, hi=LAMP_RED[6], lo=LAMP_RED[1])
        t.hline(x + 2, x1 - 2, y + 2, LAMP_RED[5])
        for side in (x + 2, x1 - 9):
            _stripes(t, side, y + 2, side + 7, y1 - 2, period=4, a=LAMP_AMBER[4], b=LAMP_RED[0])
            outline(t, side - 1, y + 1, side + 8, y1 - 1, LAMP_RED[1])
    elif state == 2:
        t.rect(x, y, x1, y1, OLIVE[2])
        for yy in range(y + 1, y1):
            for xx in range(x + 1, x1):
                if (xx + yy) % 4 == 0:
                    t.set(xx, yy, mix(OLIVE[2], OLIVE[1], 0.5))
        bevel(t, x, y, x1, y1, OLIVE, hi=OLIVE[3], lo=OLIVE[1])
    else:
        top, bot = (OLIVE[5], OLIVE[3]) if state == 1 else (OLIVE[4], OLIVE[2])
        _vgrad(t, x, y, x1, y1, top, bot)
        bevel(t, x, y, x1, y1, OLIVE, hi=OLIVE[6], lo=OLIVE[1])
        if h >= 14 and w >= 24:
            _screw(t, x + 3, y + h // 2 - 1)
            _screw(t, x1 - 5, y + h // 2 - 1)
        if state == 1:
            outline(t, x + 1, y + 1, x1 - 1, y1 - 1, RADIUM[4])
            t.hline(x + 2, x1 - 2, y + 2, mix(OLIVE[5], RADIUM[5], 0.3))
    outline(t, x, y, x1, y1, OLIVE[0])


def _toggle(t, x, y, w, h, on):
    """A latching switch: a slide at the left end, its lamp beside it; the label goes to the right of x+22."""
    x1, y1 = x + w - 1, y + h - 1
    top, bot = (OLIVE[5], OLIVE[3]) if on else (OLIVE[3], OLIVE[2])
    _vgrad(t, x, y, x1, y1, top, bot)
    bevel(t, x, y, x1, y1, OLIVE, hi=OLIVE[6] if on else OLIVE[4], lo=OLIVE[1])
    outline(t, x, y, x1, y1, OLIVE[0])
    # The slide's track, lit when latched on.
    tx0, ty0, tx1, ty1 = x + 3, y + 4, x + 14, y + h - 5
    t.rect(tx0, ty0, tx1, ty1, LAMP_AMBER[2] if on else OLIVE[0])
    t.hline(tx0, tx1, ty0, LAMP_AMBER[1] if on else DARK[0])
    t.hline(tx0, tx1, ty1 + 1, OLIVE[5])
    # The knob, steel, at the right end when on.
    kx = tx1 - 4 if on else tx0
    t.rect(kx, ty0 - 1, kx + 4, ty1 + 1, STEEL[4])
    t.vline(kx, ty0 - 1, ty1 + 1, STEEL[6])
    t.hline(kx, kx + 4, ty0 - 1, STEEL[6])
    t.vline(kx + 4, ty0 - 1, ty1 + 1, STEEL[1])
    t.hline(kx, kx + 4, ty1 + 1, STEEL[1])
    t.vline(kx + 2, ty0 + 1, ty1 - 1, STEEL[2])
    # The indicator lamp.
    _lamp(t, x + 16, y + h // 2 - 3, 6, LAMP_AMBER, lit=on)


# ======================================================================================
# launch controller sheets
# ======================================================================================

def _keyhole(t, cx, cy):
    """The arming key switch at the right end of the title."""
    t.disc(cx + 0.5, cy + 0.5, 4.4, OLIVE[0])
    t.disc(cx + 0.5, cy + 0.5, 3.6, STEEL[3])
    t.ring(cx + 0.5, cy + 0.5, 3.6, 2.8, STEEL[5])
    t.set(cx + 2, cy + 2, STEEL[1])
    t.set(cx + 1, cy + 2, STEEL[2])
    t.set(cx + 2, cy + 1, STEEL[2])
    t.vline(cx, cy - 2, cy + 1, BRASS[1])
    t.set(cx, cy - 1, DARK[0])
    t.set(cx, cy, DARK[0])
    t.set(cx, cy + 1, DARK[0])
    t.set(cx, cy - 2, DARK[0])


def _silo_frame(active):
    """Everything both tabs share: panel, title, tabs, status window and the rocket's cutaway."""
    t = _olive_panel(L.SILO_WIDTH, L.SILO_HEIGHT, 990)
    # Title strip with the arming key at its right end.
    x0, y0, x1, y1 = _box(L.SILO_TITLE)
    t.rect(x0, y0, x1, y1, OLIVE[1])
    for x in range(x0, x1 + 1, 4):
        t.set(x, y1, OLIVE[2])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[0])
    t.hline(x0 - 1, x1 + 1, y1 + 2, OLIVE[5])
    _stripes(t, x1 - 22, y0 + 1, x1 - 13, y1 - 1, period=4)
    outline(t, x1 - 23, y0, x1 - 12, y1, OLIVE[0])
    _keyhole(t, x1 - 6, (y0 + y1) // 2)
    # Tabs, painted as the screen draws them, in wells.
    for r, is_active in ((L.SILO_TAB_ROCKET, active == "rocket"), (L.SILO_TAB_LAUNCH, active == "launch")):
        _sunk(t, r)
        _tab(t, r[0], r[1], r[2], r[3], is_active)
    # Status: phosphor strip with the state lamp's socket at its left (lamp at STATUS.x+2, STATUS.y+2).
    _phosphor(t, L.SILO_STATUS, 991)
    sx, sy = L.SILO_STATUS[0], L.SILO_STATUS[1]
    t.rect(sx + 1, sy + 1, sx + 12, sy + 12, OLIVE[0])
    t.frame(sx + 1, sy + 1, sx + 12, sy + 12, OLIVE[2])
    t.vline(sx + 13, sy + 2, sy + L.SILO_STATUS[3] - 2, PHOSPHOR[0])
    # The cutaway: a dark phosphor window, a height scale up its left and the column's bay.
    _phosphor(t, L.SILO_DIAGRAM, 992, lo=0.22, hi=0.28)
    dx0, dy0, dx1, dy1 = _box(L.SILO_DIAGRAM)
    base = L.silo_base_y()
    cx0 = L.silo_cell_x()
    cx1 = cx0 + L.SILO_CELL_W - 1
    top = L.silo_cell_y(L.SILO_CELLS - 1)
    # Faint vertical grid every 9 px, dotted.
    for gx in range(dx0 + 3, dx1, 9):
        for y in range(dy0 + 3, base, 3):
            t.set(gx, y, PHOSPHOR[3])
    # Height ticks: one per cell, longer every five, a dotted rule across at each.
    tick_x = L.SILO_BRACKET_X + L.SILO_BRACKET_W + 1
    for k in range(L.SILO_CELLS + 1):
        y = base - k * L.SILO_CELL_H
        major = k % 5 == 0
        t.hline(tick_x, tick_x + (5 if major else 2), y, PHOSPHOR[6] if major else PHOSPHOR[5])
        for x in range(tick_x + 7, dx1 - 1, 4 if major else 6):
            t.set(x, y, PHOSPHOR[4] if major else PHOSPHOR[3])
    t.vline(tick_x, top, base, PHOSPHOR[5])
    # The column's bay: dashed sides and a centre line the rocket stands on.
    for y in range(top, base):
        if (y // 2) % 2 == 0:
            t.set(cx0 - 2, y, PHOSPHOR[5])
            t.set(cx1 + 2, y, PHOSPHOR[5])
        if y % 6 < 3:
            t.set(L.SILO_COL_CX, y, PHOSPHOR[4])
    # Ground under the base: a hatched band.
    gy0, gy1 = base + L.SILO_BASE_H, dy1 - 1
    for y in range(gy0, gy1 + 1):
        for x in range(dx0 + 2, dx1):
            t.set(x, y, PHOSPHOR[4] if (x + y) % 4 == 0 else PHOSPHOR[2])
    t.hline(dx0 + 2, dx1 - 1, gy0, PHOSPHOR[5])
    _brackets(t, L.SILO_DIAGRAM, PHOSPHOR[5], arm=4)
    # The engraved line between the cutaway and the tab's side.
    _groove(t, dx1 + 2, dy0, dx1 + 2, dy1)
    return t


@gui("launch_controller_rocket")
def launch_controller_rocket():
    t = _silo_frame("rocket")
    # Stage table: header band, three row bands, faint column rules.
    _phosphor(t, L.SILO_STAGES, 993)
    x0, y0, x1, y1 = _box(L.SILO_STAGES)
    hy = y0 + L.SILO_STAGE_HEADER - 1
    t.rect(x0 + 2, y0 + 2, x1 - 1, hy - 1, PHOSPHOR[5])
    t.hline(x0 + 2, x1 - 1, y0 + 2, PHOSPHOR[6])
    t.hline(x0 + 2, x1 - 1, hy, RADIUM[3])
    for row in range(3):
        ry = y0 + L.SILO_STAGE_HEADER + row * L.SILO_STAGE_ROW
        if row % 2 == 1:
            for y in range(ry, min(y1, ry + L.SILO_STAGE_ROW)):
                for x in range(x0 + 2, x1):
                    t.set(x, y, mix(t.get(x, y), PHOSPHOR[4], 0.35))
        if row:
            for x in range(x0 + 3, x1 - 1, 2):
                t.set(x, ry, PHOSPHOR[4])
    for c in (L.SILO_C_TANKS, L.SILO_C_TWR, L.SILO_C_DV, L.SILO_C_BURN, L.SILO_C_REACH):
        cx = x0 + c - 3
        t.vline(cx, y0 + 2, hy - 1, PHOSPHOR[3])
        for y in range(hy + 2, y1, 2):
            t.set(cx, y, PHOSPHOR[4])
    # Section panel: title strip over a sunk rack holding the slots, and the stats readout.
    sx0, sy0, sx1, sy1 = _box(L.SILO_SECTION)
    _sunk(t, L.SILO_SECTION, fill=OLIVE[2])
    brushed(t, OLIVE, sx0, sy0 + L.SILO_SECTION_TITLE, sx1, sy1, base=2.4, amp=0.35, seed=994, scale=13.0)
    ty1 = sy0 + L.SILO_SECTION_TITLE - 1
    _vgrad(t, sx0, sy0, sx1, ty1 - 1, OLIVE[1], OLIVE[0])
    t.hline(sx0, sx1, ty1, OLIVE[4])
    for x in range(sx1 - 30, sx1 - 2, 4):
        t.rect(x, sy0 + 4, x + 1, sy0 + 6, OLIVE[2])
    # Group rails under the slots: charge, then secondary and tamper (and the bomb's own slot above them).
    xs, ys = L.SILO_SLOT_X, L.SILO_SLOT_Y
    groups = ((0, 1, 2), (3, 4, 5, 6), (7, 8), (9,))
    for g in groups:
        gx0 = min(xs[i] for i in g) - 1
        gx1 = max(xs[i] for i in g) + 16
        gy = max(ys[i] for i in g) + 18
        t.hline(gx0, gx1, gy, OLIVE[1])
        t.hline(gx0, gx1, gy + 1, OLIVE[4])
        t.vline(gx0, gy - 1, gy, OLIVE[1])
        t.vline(gx1, gy - 1, gy, OLIVE[1])
    for i in range(len(xs)):
        _slot(t, xs[i], ys[i])
    _phosphor(t, L.SILO_SECTION_STATS, 995)
    _brackets(t, L.SILO_SECTION_STATS, PHOSPHOR[5], arm=3, inset=3)
    # Advice: a single line of glass.
    _phosphor(t, L.SILO_ADVICE, 996)
    # Inventory, on a sunk plate, with louvres either side.
    _sunk(t, L.SILO_INVENTORY, fill=OLIVE[2])
    for row in range(3):
        for col in range(9):
            _slot(t, L.SILO_INV_X + col * 18, L.SILO_INV_Y + row * 18)
    for col in range(9):
        _slot(t, L.SILO_INV_X + col * 18, L.SILO_HOTBAR_Y)
    ix0, iy0, ix1, iy1 = _box(L.SILO_INVENTORY)
    left = L.SILO_ADVICE[0]
    right = L.SILO_ADVICE[0] + L.SILO_ADVICE[2] - 1
    _louvres(t, left + 1, iy0 + 2, ix0 - 5, iy1 - 2)
    _louvres(t, ix1 + 5, iy0 + 2, right - 1, iy1 - 2)
    return t


DUST = Ramp("#3a0000", "#6e0000", "#a00000", "#d00000", "#ff1a1a", "#ff6a5a", "#ffb0a0")
WOOD = Ramp("#2e1f10", "#4a321a", "#6a4a28", "#8a6438", "#a87e4a", "#c49a62", "#dcb884")
COBBLE = Ramp("#232323", "#3c3c3c", "#545454", "#6a6a6a", "#828282", "#9a9a9a", "#b6b6b6")

LEVER = [
    "..........##..",
    ".........###..",
    ".........##...",
    "........###...",
    "........##....",
    ".......###....",
    ".......##.....",
    "......###.....",
    "......##......",
    ".....###......",
    ".....##.......",
    "..##########..",
    ".############.",
    ".############.",
    ".############.",
    "..##########..",
]


def _lever(t, x, y):
    """A thrown lever on its cobblestone base, 14x16 from (x, y), outlined so it reads on the glass."""
    pts = mask_from(LEVER)
    handle = {(i, j) for (i, j) in pts if j < 11}
    base = pts - handle
    for (i, j) in handle:
        lit = (i - 1, j) not in handle
        t.set(x + i, y + j, WOOD[5] if lit else ((WOOD[2]) if (i + 1, j) not in handle else WOOD[3]))
    t.set(x + 10, y, WOOD[6])
    t.set(x + 11, y, WOOD[4])
    for (i, j) in base:
        n = fbm(i, j, 1004, 16, 16, octaves=2, scale=8.0)
        v = 3.4 + 2.2 * (n - 0.5) + (1.2 if j == 11 else 0) - (1.2 if j == 15 else 0)
        t.set(x + i, y + j, COBBLE[int(round(max(1, min(6, v))))])
    t.set(x + 5, y + 12, COBBLE[1])
    t.set(x + 6, y + 12, COBBLE[1])
    t.set(x + 7, y + 12, COBBLE[2])
    for i in range(-1, 15):
        for j in range(-1, 17):
            if (i, j) in pts:
                continue
            if any((i + di, j + dj) in pts for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                t.set(x + i, y + j, PHOSPHOR[0])


def _dust(t, cx, cy):
    """A dot of lit redstone dust where its line ends, with short trails."""
    for k in (-3, -2, 2, 3):
        t.set(cx, cy + k, DUST[3])
    t.set(cx + 2, cy, DUST[3])
    t.set(cx + 3, cy, DUST[2])
    t.rect(cx - 1, cy - 1, cx + 1, cy + 1, DUST[4])
    t.set(cx, cy, DUST[6])
    t.set(cx - 1, cy - 1, DUST[5])
    for px, py in ((cx - 2, cy - 2), (cx + 2, cy + 2), (cx + 2, cy - 2), (cx - 2, cy + 2)):
        t.set(px, py, DUST[2])


@gui("launch_controller_launch")
def launch_controller_launch():
    t = _silo_frame("launch")
    # Target box: a sunk plate holding three coordinate fields, the SET button and the airburst switch.
    tx0, ty0, tx1, ty1 = _box(L.SILO_TARGET)
    _sunk(t, L.SILO_TARGET, fill=OLIVE[2])
    brushed(t, OLIVE, tx0, ty0, tx1, ty1, base=2.5, amp=0.35, seed=997, scale=13.0)
    _vgrad(t, tx0, ty0, tx1, ty0 + 10, OLIVE[1], OLIVE[2])
    t.hline(tx0, tx1, ty0 + 11, OLIVE[4])
    # A crosshair stencil in the target title's right corner.
    ccx, ccy = tx1 - 8, ty0 + 5
    t.ring(ccx + 0.5, ccy + 0.5, 4.0, 3.0, LAMP_RED[3])
    t.hline(ccx - 5, ccx - 3, ccy, LAMP_RED[3])
    t.hline(ccx + 3, ccx + 5, ccy, LAMP_RED[3])
    t.vline(ccx, ccy - 5, ccy - 3, LAMP_RED[3])
    t.vline(ccx, ccy + 3, ccy + 5, LAMP_RED[3])
    t.set(ccx, ccy, LAMP_RED[4])
    for r in (L.SILO_BOX_X, L.SILO_BOX_Y, L.SILO_BOX_Z):
        _phosphor(t, r, 998)
    _sunk(t, L.SILO_SET, out=2)
    _button(t, *L.SILO_SET, 0)
    _sunk(t, L.SILO_AIRBURST, out=2)
    _toggle(t, *L.SILO_AIRBURST, False)
    # The readout.
    _phosphor(t, L.SILO_READOUT, 999)
    _brackets(t, L.SILO_READOUT, PHOSPHOR[5], arm=5)
    # The countdown: a deep black display in a heavy bezel, for big red figures.
    cx0, cy0, cx1, cy1 = _box(L.SILO_COUNTDOWN)
    t.rect(cx0 - 1, cy0 - 1, cx1 + 1, cy1 + 1, OLIVE[0])
    for y in range(cy0 + 2, cy1 - 1):
        for x in range(cx0 + 2, cx1 - 1):
            n = fbm(x, y, 1000, 336, 256, octaves=2, scale=3.0)
            c = mix((14, 6, 5, 255), (34, 12, 9, 255), 0.3 + 0.4 * n)
            t.set(x, y, shade(c, 0.85) if y % 2 else c)
    bevel(t, cx0, cy0, cx1, cy1, STEEL, hi=STEEL[1], lo=STEEL[5])
    bevel(t, cx0 + 1, cy0 + 1, cx1 - 1, cy1 - 1, DARK, hi=DARK[0], lo=DARK[3])
    for x, y in ((cx0 + 2, cy0 + 2), (cx1 - 3, cy0 + 2), (cx0 + 2, cy1 - 3), (cx1 - 3, cy1 - 3)):
        t.set(x, y, (52, 18, 14, 255))
    # Arm and disarm wells; the arm one ringed with hazard paint.
    ax0, ay0, ax1, ay1 = _box(L.SILO_ARM)
    _stripes(t, ax0 - 3, ay0 - 3, ax1 + 3, ay1 + 3, period=6, wear=0.2, seed=1001)
    t.hline(ax0 - 3, ax1 + 3, ay1 + 3, shade(YELLOW, 0.6))
    t.vline(ax1 + 3, ay0 - 3, ay1 + 3, shade(YELLOW, 0.6))
    _button(t, *L.SILO_ARM, 0)
    _sunk(t, L.SILO_DISARM, out=1)
    _button(t, *L.SILO_DISARM, 0)
    # Redstone: a display whose left third shows a lever wired by dust to the launch, the text going to its right.
    rx0, ry0, rx1, ry1 = _box(L.SILO_REDSTONE)
    _phosphor(t, L.SILO_REDSTONE, 1002)
    third = rx0 + L.SILO_REDSTONE[2] // 3
    for x in range(rx0 + 2, third):
        for y in range(ry0 + 2, ry1):
            t.set(x, y, mix(t.get(x, y), PHOSPHOR[0], 0.45))
    t.vline(third, ry0 + 2, ry1 - 1, PHOSPHOR[5])
    t.vline(third + 1, ry0 + 2, ry1 - 1, PHOSPHOR[0])
    midy = (ry0 + ry1) // 2
    _lever(t, rx0 + 6, midy - 9)
    # The dust line from the lever's foot to a dot, then an arrow on to the text.
    for x in range(rx0 + 22, rx0 + 44):
        t.set(x, midy + 5, DUST[3] if x % 3 else DUST[4])
        t.set(x, midy + 6, DUST[1])
    _dust(t, rx0 + 48, midy + 5)
    ax = third - 14
    t.hline(ax, ax + 8, midy + 5, RADIUM[5])
    for k in range(1, 4):
        t.vline(ax + 8 - k, midy + 5 - k, midy + 5 + k, RADIUM[5] if k < 3 else RADIUM[3])
    _brackets(t, L.SILO_REDSTONE, PHOSPHOR[5], arm=3)
    return t


# ======================================================================================
# launch controller widgets
# ======================================================================================

def _cell(t, ox, oy, section, state):
    """One section of the cutaway, CELL_W x CELL_H: 0 engine, 1 tank, 2 guidance, 3 warhead."""
    w, h = L.SILO_CELL_W, L.SILO_CELL_H
    lo, hi = 1, w - 2  # body columns (the outermost columns stay clear for the selection outline)
    ghost = state == L.SILO_CELL_GHOST

    def cyl(x, x0, x1):
        """Shade of a cylinder lit from the left at column x between x0 and x1."""
        u = (x - x0) / max(1, x1 - x0)
        return 5.4 - 3.6 * u + (0.6 if u < 0.2 else 0.0)

    def body(x0, x1, y0, y1):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                t.set(ox + x, oy + y, CELL[int(round(max(1, min(6, cyl(x, x0, x1)))))])
        t.vline(ox + x0, oy + y0, oy + y1, CELL[0])
        t.vline(ox + x1, oy + y0, oy + y1, CELL[0])

    if ghost:
        # Only a dashed outline of the section's shape, half clear.
        dash = _rgba(CELL[6], 200)
        if section == 3:
            for y in range(h):
                half = 1 + (hi - lo) * (y + 1) / (2 * h)
                for x in (int(round(w / 2 - half)), int(round(w / 2 + half - 1))):
                    if y % 3 != 2:
                        t.set(ox + x, oy + y, dash)
        else:
            for y in range(h):
                if y % 3 != 2:
                    t.set(ox + lo, oy + y, dash)
                    t.set(ox + hi, oy + y, dash)
            for x in range(lo, hi + 1):
                if x % 3 != 2:
                    t.set(ox + x, oy, dash)
                    t.set(ox + x, oy + h - 1, dash)
        return

    if section == 0:
        # Engine: thrust frame on top, the bell flaring out below with its cooling lines.
        body(lo, hi, 0, 3)
        t.hline(ox + lo, ox + hi, oy, CELL[1])
        t.hline(ox + lo + 1, ox + hi - 1, oy + 3, CELL[2])
        for x in (lo + 3, hi - 3):
            t.set(ox + x, oy + 1, CELL[6])
            t.set(ox + x, oy + 2, CELL[1])
        for y in range(4, h):
            k = (y - 4) / (h - 5)
            half = 2.5 + 4.5 * k * k + 1.0 * k
            x0 = int(round(w / 2 - half))
            x1 = int(round(w / 2 + half)) - 1
            for x in range(x0, x1 + 1):
                v = cyl(x, x0, x1) - 0.8
                if (x - x0) % 3 == 1 and 0 < x - x0 < x1 - x0:
                    v -= 0.9
                t.set(ox + x, oy + y, CELL[int(round(max(1, min(6, v))))])
            t.set(ox + x0, oy + y, CELL[0])
            t.set(ox + x1, oy + y, CELL[0])
        t.hline(ox + 6, ox + w - 7, oy + 4, CELL[1])
        t.hline(ox + int(w / 2 - 7), ox + int(w / 2 + 7) - 1, oy + h - 1, CELL[1])
    elif section == 1:
        # Tank: weld bands top and bottom, a seam of rivets down the shadow side.
        body(lo, hi, 0, h - 1)
        for y in (1, h - 2):
            for x in range(lo + 1, hi):
                t.set(ox + x, oy + y, CELL[int(round(max(1, min(6, cyl(x, lo, hi) - 1.3))))])
        t.hline(ox + lo + 1, ox + hi - 1, oy + 2, CELL[6])
        for y in range(3, h - 3, 2):
            t.set(ox + hi - 3, oy + y, CELL[2])
        t.hline(ox + lo, ox + hi, oy, CELL[1])
    elif section == 2:
        # Guidance: painted skin with a black and white roll band and a star window.
        body(lo, hi, 0, h - 1)
        t.hline(ox + lo, ox + hi, oy, CELL[1])
        for x in range(lo + 1, hi):
            v = cyl(x, lo, hi)
            t.set(ox + x, oy + 3, CELL[int(round(max(1, min(6, v - 2.6))))])
            t.set(ox + x, oy + 4, CELL[int(round(max(1, min(6, v - 3.4))))])
            t.set(ox + x, oy + 5, CELL[int(round(max(1, min(6, v - 3.4))))])
            t.set(ox + x, oy + 6, CELL[int(round(max(1, min(6, v + 0.6))))])
        for x in range(lo + 2, hi - 1, 3):
            t.set(ox + x, oy + 4, CELL[3] if x < hi - 5 else CELL[2])
        t.set(ox + 5, oy + 8, CELL[0])
        t.set(ox + 6, oy + 8, CELL[1])
    else:
        # Warhead: the nose cone, its point at the top row and its base the body's full width.
        for y in range(h):
            half = 0.6 + (hi - lo + 1) / 2 * ((y + 1) / h) ** 0.75
            x0 = int(round(w / 2 - half))
            x1 = int(round(w / 2 + half)) - 1
            x0, x1 = max(lo, x0), min(hi, x1)
            for x in range(x0, x1 + 1):
                t.set(ox + x, oy + y, CELL[int(round(max(1, min(6, cyl(x, x0, x1) + 0.3))))])
            t.set(ox + x0, oy + y, CELL[0])
            t.set(ox + x1, oy + y, CELL[0])
        # A dark band near the base: the re-entry body's stripe.
        for x in range(lo + 1, hi):
            t.set(ox + x, oy + h - 3, CELL[int(round(max(1, min(6, cyl(x, lo, hi) - 2.2))))])
        t.set(ox + w // 2 - 1, oy, CELL[6])

    if state == L.SILO_CELL_SELECTED:
        # A bright frame round the whole cell, broken at the corners like a reticle.
        sel = (255, 255, 236, 255)
        for x in range(w):
            if 1 <= x <= w - 2:
                t.set(ox + x, oy, sel)
                t.set(ox + x, oy + h - 1, sel)
        for y in range(1, h - 1):
            t.set(ox, oy + y, sel)
            t.set(ox + w - 1, oy + y, sel)
    elif state == L.SILO_CELL_ERROR:
        # Red wash over what is drawn and a red frame.
        for y in range(h):
            for x in range(w):
                p = t.get(ox + x, oy + y)
                if p[3]:
                    t.set(ox + x, oy + y, mix(p, LAMP_RED[3], 0.5))
        for x in range(w):
            if (x + 1) % 4:
                t.set(ox + x, oy, LAMP_RED[4])
                t.set(ox + x, oy + h - 1, LAMP_RED[4])
        for y in range(h):
            t.set(ox, oy + y, LAMP_RED[4])
            t.set(ox + w - 1, oy + y, LAMP_RED[4])


def _base_platform(t, ox, oy):
    """The launch pad seen from the side: hazard-striped deck over concrete, a flame trench under the column."""
    w, h = L.SILO_BASE_W, L.SILO_BASE_H
    for y in range(h):
        for x in range(w):
            n = fbm(x, y, 1010, 64, 16, octaves=2, scale=6.0)
            t.set(ox + x, oy + y, CONCRETE.t(0.4 + 0.25 * n))
    _stripes(t, ox, oy, ox + w - 1, oy + 1, period=6)
    t.hline(ox, ox + w - 1, oy + 2, STEEL[5])
    t.hline(ox, ox + w - 1, oy + 3, STEEL[2])
    # The trench under the column, scorched.
    c0 = w // 2 - L.SILO_CELL_W // 2
    c1 = c0 + L.SILO_CELL_W - 1
    t.rect(ox + c0 + 2, oy + 2, ox + c1 - 2, oy + h - 2, DARK[0])
    for x in range(c0 + 3, c1 - 2, 2):
        t.vline(ox + x, oy + 2, oy + 3, STEEL[3])
    t.hline(ox + c0 + 2, ox + c1 - 2, oy + h - 2, (90, 52, 30, 255))
    t.vline(ox + c0 + 1, oy + 2, oy + h - 1, STEEL[1])
    t.vline(ox + c1 - 1, oy + 2, oy + h - 1, STEEL[5])
    for x in (3, w - 5):
        bolt(t, ox + x, oy + 4, STEEL)
    t.hline(ox, ox + w - 1, oy + h - 1, CONCRETE[1])
    t.vline(ox, oy, oy + h - 1, DARK[0])
    t.vline(ox + w - 1, oy, oy + h - 1, DARK[0])


def _base_silo(t, ox, oy):
    """The silo's floor: a concrete slab between the stubs of its walls, a duct under the column."""
    w, h = L.SILO_BASE_W, L.SILO_BASE_H
    for y in range(h):
        for x in range(w):
            n = fbm(x, y, 1011, 64, 16, octaves=2, scale=6.0)
            t.set(ox + x, oy + y, CONCRETE.t(0.36 + 0.25 * n))
    t.hline(ox, ox + w - 1, oy + 3, CONCRETE[5])
    for x in range(w):
        for y in range(0, 3):
            t.set(ox + x, oy + y, CLEAR)
    # Wall stubs, steel-lined on their inner faces.
    for x0, x1, liner in ((0, 5, 5), (w - 6, w - 1, w - 6)):
        for y in range(h):
            for x in range(x0, x1 + 1):
                n = fbm(x, y, 1012, 64, 16, octaves=2, scale=6.0)
                t.set(ox + x, oy + y, CONCRETE.t(0.5 + 0.25 * n))
        t.vline(ox + liner, oy, oy + h - 1, STEEL[4])
        t.hline(ox + x0, ox + x1, oy, CONCRETE[5])
    # Flame duct.
    c0 = w // 2 - L.SILO_CELL_W // 2
    c1 = c0 + L.SILO_CELL_W - 1
    t.rect(ox + c0 + 3, oy + 4, ox + c1 - 3, oy + h - 2, DARK[0])
    t.hline(ox + c0 + 3, ox + c1 - 3, oy + h - 2, (90, 52, 30, 255))
    t.hline(ox + c0 + 2, ox + c1 - 2, oy + 3, STEEL[4])
    t.hline(ox, ox + w - 1, oy + h - 1, CONCRETE[1])
    t.vline(ox, oy, oy + h - 1, DARK[0])
    t.vline(ox + w - 1, oy, oy + h - 1, DARK[0])


GHOST_HI = (222, 224, 214, 150)
GHOST_MID = (190, 192, 184, 110)
GHOST_LO = (150, 152, 146, 80)


def _ghost_fill(t, ox, oy, pts):
    """A translucent grey silhouette, lit from the top left like an item."""
    for (x, y) in pts:
        top = (x, y - 1) not in pts or (x - 1, y) not in pts
        bot = (x, y + 1) not in pts or (x + 1, y) not in pts
        t.set(ox + x, oy + y, GHOST_HI if top else (GHOST_LO if bot else GHOST_MID))


CRYSTAL = [
    "................",
    ".......H........",
    "......HHM.......",
    ".....HHHMM......",
    "....HHHHMML.....",
    "....HHHMMMLL....",
    "....HHHMMMLL....",
    "....HHHMMMLL....",
    "....HHHMMMLL....",
    "....HHHMMMLL....",
    "....HHHMMMLL....",
    "....HHHMMMLL....",
    ".....HHMMML.....",
    "......HMML......",
    ".......ML.......",
    "................",
]

BLOCK = [
    "................",
    "................",
    ".......HH.......",
    ".....HHHHHH.....",
    "...HHHHHHHHHH...",
    "..MHHHHHHHHHHL..",
    "..MMMHHHHHHLLL..",
    "..MMMMMHHLLLLL..",
    "..MMMMMMLLLLLL..",
    "..MMMMMMLLLLLL..",
    "..MMMMMMLLLLLL..",
    "..MMMMMMLLLLLL..",
    "..MMMMMMLLLLLL..",
    "....MMMMLLLL....",
    "......MMLL......",
    "................",
]


def _ghost_art(art):
    """A ghost icon from string art whose letters are its faces: H lit, M half, L in shadow."""
    pts, marks = set(), []
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch in "HML":
                pts.add((x, y))
                marks.append(((x, y), {"H": GHOST_HI, "M": GHOST_MID, "L": GHOST_LO}[ch]))
    return pts, marks


def _ghosts(t, ox, oy):
    def disc_pts(cx, cy, r):
        return {(x, y) for y in range(16) for x in range(16) if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r}

    icons = []
    # Core: a sphere with the seam of its two hemispheres.
    core = disc_pts(8, 8, 6.0)
    icons.append((core, [((x, 8), GHOST_LO) for x in range(3, 13)] + [((6, 5), GHOST_HI), ((5, 6), GHOST_HI)]))
    # Radiological: the trefoil, one blade down and two up.
    tre = set()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            d = math.hypot(dx, dy)
            th = math.degrees(math.atan2(dy, dx))
            blade = any(abs((th - c + 180) % 360 - 180) <= 30 for c in (90, 210, 330))
            if d <= 1.7 or (2.7 <= d <= 7.3 and blade):
                tre.add((x, y))
    icons.append((tre, []))
    # TNT: three sticks bound with a band, a fuse curling up.
    tnt = set()
    for sx in (3, 7, 11):
        tnt |= {(x, y) for x in range(sx, sx + 3) for y in range(5, 15)}
    tnt |= {(8, 4), (8, 3), (9, 2), (10, 1)}
    band = [((x, y), GHOST_LO) for x in range(3, 14) for y in (8, 9) if (x, y) in tnt]
    icons.append((tnt, band))
    # Lithium deuteride: a cut crystal, three faces lit left to right.
    icons.append(_ghost_art(CRYSTAL))
    # Tamper: a heavy block seen from above a corner.
    icons.append(_ghost_art(BLOCK))
    # Bomb: a finned casing lying across, nose to the left.
    bomb = set()
    for y in range(16):
        for x in range(16):
            if x <= 11 and ((x + 0.5 - 6.5) / 5.8) ** 2 + ((y + 0.5 - 8) / 3.4) ** 2 <= 1:
                bomb.add((x, y))
    for x in range(11, 15):
        k = x - 11
        bomb |= {(x, y) for y in range(7 - k, 9 + k)}
    bomb |= {(14, y) for y in range(3, 13)}
    marks = [((8, y), GHOST_LO) for y in range(5, 11)] + [((x, 8), GHOST_LO) for x in range(12, 14)]
    icons.append((bomb, marks))
    # Propellant cartridge: a canister with a valve cap.
    cart = {(x, y) for x in range(4, 12) for y in range(4, 15)}
    cart -= {(4, 4), (11, 4), (4, 14), (11, 14)}
    cart |= {(x, y) for x in range(6, 10) for y in range(1, 4)}
    rings = [((x, y), GHOST_LO) for x in range(4, 12) for y in (6, 12) if (x, y) in cart]
    icons.append((cart, rings))
    # Star tracker: a lens ring with a four-pointed star.
    lens = {(x, y) for y in range(16) for x in range(16) if 4.6 <= math.hypot(x + 0.5 - 8, y + 0.5 - 8) <= 6.6}
    star = {(8, y) for y in range(3, 13)} | {(x, 8) for x in range(3, 13)} | {(7, 7), (9, 7), (7, 9), (9, 9)}
    star |= {(7, 8), (9, 8), (8, 7), (8, 9)}
    icons.append((lens | star, [((8, 8), GHOST_HI)]))
    for i, (pts, marks) in enumerate(icons):
        x0 = ox + i * 16
        _ghost_fill(t, x0, oy, pts)
        for (x, y), c in marks:
            if (x, y) in pts:
                t.set(x0 + x, oy + y, c)


@gui("launch_controller_widgets")
def launch_controller_widgets():
    s = L.SILO_WIDGETS_SIZE
    t = Tex(s, s, 1020)
    cx, cy, _, _ = L.SILO_SPRITE_CELLS
    for section in range(4):
        for state in (L.SILO_CELL_NORMAL, L.SILO_CELL_SELECTED, L.SILO_CELL_ERROR, L.SILO_CELL_GHOST):
            _cell(t, cx + state * L.SILO_CELL_W, cy + section * L.SILO_CELL_H, section, state)
    _base_platform(t, L.SILO_SPRITE_BASE_PLATFORM[0], L.SILO_SPRITE_BASE_PLATFORM[1])
    _base_silo(t, L.SILO_SPRITE_BASE_SILO[0], L.SILO_SPRITE_BASE_SILO[1])
    _ghosts(t, L.SILO_SPRITE_GHOSTS[0], L.SILO_SPRITE_GHOSTS[1])
    _tab(t, *L.SILO_SPRITE_TAB_IDLE, False)
    _tab(t, *L.SILO_SPRITE_TAB_ACTIVE, True)
    bx, by, bw, bh = L.SILO_SPRITE_BUTTONS
    for i in range(4):
        _button(t, bx, by + i * (bh // 4), bw, bh // 4, i)
    gx, gy, gw, gh = L.SILO_SPRITE_TOGGLES
    for i in range(2):
        _toggle(t, gx, gy + i * (gh // 2), gw, gh // 2, i == 1)
    lx, ly, lw, lh = L.SILO_SPRITE_LAMPS
    for i, ramp in enumerate(STATE_LAMPS):
        _lamp(t, lx + i * lh, ly, lh, ramp)
    return t


# ======================================================================================
# command table: the in-world table's dark steel and brass round cyan projection glass
# ======================================================================================

def _holo_panel(w, h, seed):
    t = Tex(w, h, seed)
    brushed(t, DARK, base=2.7, amp=0.5, seed=seed, scale=11.0)
    bevel(t, 0, 0, w - 1, h - 1, DARK, hi=DARK[5], lo=DARK[0])
    t.hline(1, w - 2, 1, BRASS[4])
    t.hline(1, w - 2, 2, BRASS[2])
    t.hline(1, w - 2, h - 3, BRASS[3])
    t.hline(1, w - 2, h - 2, BRASS[1])
    t.vline(1, 2, h - 3, BRASS[3])
    t.vline(w - 2, 2, h - 3, BRASS[1])
    outline(t, 0, 0, w - 1, h - 1, DARK[0])
    return t


def _holo_glass(t, r, seed, header=0):
    """Projection glass: blue-black with a faint lattice, sunk behind a brass lip."""
    x0, y0, x1, y1 = _box(r)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, 400, 256, octaves=3, scale=2.0)
            t.set(x, y, mix(HOLO[1], HOLO[2], 0.3 + 0.35 * n))
    t.hline(x0, x1, y0, HOLO[0])
    t.vline(x0, y0, y1, HOLO[0])
    t.hline(x0, x1, y1, HOLO[3])
    t.vline(x1, y0, y1, HOLO[3])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, DARK[0])
    t.hline(x0 - 1, x1 + 1, y1 + 2, DARK[4])


def _holo_sunk(t, r, out=1):
    x0, y0, x1, y1 = _box(r)
    a, b, c, d = x0 - out, y0 - out, x1 + out, y1 + out
    t.hline(a, c, b, DARK[0])
    t.vline(a, b, d, DARK[0])
    t.hline(a + 1, c, d, DARK[5])
    t.vline(c, b + 1, d, DARK[5])


def _holo_button(t, x, y, w, h, state):
    """Command table key: 0 normal, 1 hover, 2 disabled, 3 danger (a red key in a hazard-striped guard)."""
    x1, y1 = x + w - 1, y + h - 1
    if state == 3:
        # The guard: a hazard-painted collar 3 px wide round a sunk red key, hinged at the top.
        _stripes(t, x, y, x1, y1, period=4)
        t.hline(x, x1, y, shade(YELLOW, 1.0))
        t.hline(x, x1, y1, shade(YELLOW, 0.55))
        t.vline(x1, y, y1, shade(YELLOW, 0.55))
        t.vline(x, y, y1, YELLOW)
        bx0, by0, bx1, by1 = x + 4, y + 4, x1 - 4, y1 - 4
        _vgrad(t, bx0, by0, bx1, by1, LAMP_RED[4], LAMP_RED[3])
        t.hline(bx0, bx1, by0, LAMP_RED[6])
        t.hline(bx0, bx1, by0 + 1, LAMP_RED[5])
        t.hline(bx0, bx1, by1, LAMP_RED[1])
        t.frame(bx0 - 1, by0 - 1, bx1 + 1, by1 + 1, DARK[0])
        for hx in (x + 6, x1 - 7):
            t.hline(hx, hx + 1, y + 1, STEEL[6])
            t.hline(hx, hx + 1, y + 2, STEEL[3])
        return
    if state == 2:
        t.rect(x, y, x1, y1, DARK[1])
        bevel(t, x, y, x1, y1, DARK, hi=DARK[2], lo=DARK[0])
        for yy in range(y + 2, y1 - 1, 2):
            t.hline(x + 2, x1 - 2, yy, mix(DARK[1], DARK[0], 0.4))
    else:
        top, bot = (DARK[5], DARK[3]) if state == 1 else (DARK[4], DARK[2])
        _vgrad(t, x, y, x1, y1, top, bot)
        bevel(t, x, y, x1, y1, DARK, hi=DARK[6], lo=DARK[1])
        # Brass lip along the foot, cyan when hovered.
        t.hline(x + 1, x1 - 1, y1 - 1, HOLO[6] if state == 1 else BRASS[2])
        if state == 1:
            t.hline(x + 1, x1 - 1, y + 1, mix(DARK[6], HOLO[6], 0.4))
    outline(t, x, y, x1, y1, DARK[0])


def _holo_tab(t, x, y, w, h, active):
    x1, y1 = x + w - 1, y + h - 1
    if active:
        _vgrad(t, x, y, x1, y1, DARK[4], DARK[3])
        bevel(t, x, y, x1, y1, DARK, hi=DARK[6], lo=DARK[1])
        t.hline(x + 1, x1 - 1, y + 1, BRASS[4])
        t.hline(x + 2, x1 - 2, y1 - 1, HOLO[6])
        t.hline(x + 3, x1 - 3, y1 - 2, HOLO[4])
    else:
        _vgrad(t, x, y, x1, y1, DARK[2], DARK[1])
        bevel(t, x, y, x1, y1, DARK, hi=DARK[3], lo=DARK[0])
        t.hline(x + 2, x1 - 2, y1 - 1, HOLO[3])
    outline(t, x, y, x1, y1, DARK[0])


def _glyph_minus(t, cx, cy, c):
    t.hline(cx - 3, cx + 3, cy, c)


def _glyph_plus(t, cx, cy, c):
    t.hline(cx - 3, cx + 3, cy, c)
    t.vline(cx, cy - 3, cy + 3, c)


def _check(t, x, y, ticked):
    t.rect(x, y, x + 7, y + 7, HOLO[0])
    t.frame(x, y, x + 7, y + 7, HOLO[4])
    t.hline(x + 1, x + 6, y + 1, HOLO[2])
    t.set(x, y, HOLO[3])
    t.set(x + 7, y + 7, HOLO[3])
    if ticked:
        for px, py in ((1, 3), (2, 4), (3, 5), (4, 4), (5, 3), (6, 2)):
            t.set(x + px, y + py, HOLO[6])
            t.set(x + px, y + py + 1, HOLO[5])
        t.set(x + 3, y + 5, (210, 255, 255, 255))


def _unlink(t, x, y, hover):
    c = LAMP_RED[4] if hover else HOLO[5]
    d = LAMP_RED[2] if hover else HOLO[3]
    if hover:
        t.rect(x, y, x + 7, y + 7, _rgba(LAMP_RED[1], 200))
    for k in range(1, 7):
        t.set(x + k, y + k, c)
        t.set(x + 7 - k, y + k, c)
        t.set(x + k + 1, y + k, d) if k < 6 else None
    t.set(x + 1, y + 7 - 1, c)


@gui("holo_table_screen")
def holo_table_screen():
    t = _holo_panel(L.HOLO_WIDTH, L.HOLO_HEIGHT, 1030)
    # Title strip: dark glass with a brass rule and the projector's status slot at its right.
    x0, y0, x1, y1 = _box(L.HOLO_TITLE)
    t.rect(x0, y0, x1, y1, DARK[0])
    for x in range(x0, x1 + 1):
        t.set(x, y1, HOLO[2] if x % 3 else HOLO[3])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, DARK[1])
    t.hline(x0 - 1, x1 + 1, y1 + 2, BRASS[2])
    # The projector's status lights at the right end: power, link, uplink.
    for i, c in enumerate((HOLO[6], LAMP_AMBER[4], LAMP_GREEN[4])):
        lx = x1 - 8 - i * 7
        t.rect(lx - 1, y0 + 3, lx + 4, y0 + 8, DARK[1])
        t.rect(lx, y0 + 4, lx + 3, y0 + 7, shade(c, 0.55))
        t.hline(lx, lx + 2, y0 + 4, c)
        t.set(lx, y0 + 5, c)
    # The map's bezel: brass rim round dark steel, a graduated scale all round, and the dark map window.
    fx0, fy0, fx1, fy1 = _box(L.HOLO_MAP_FRAME)
    mx0, my0, mx1, my1 = _box(L.HOLO_MAP)
    t.rect(fx0, fy0, fx1, fy1, DARK[2])
    bevel(t, fx0, fy0, fx1, fy1, BRASS, hi=BRASS[5], lo=BRASS[1])
    outline(t, fx0 - 1, fy0 - 1, fx1 + 1, fy1 + 1, DARK[0])
    bevel(t, fx0 + 1, fy0 + 1, fx1 - 1, fy1 - 1, DARK, hi=DARK[4], lo=DARK[0])
    span = L.HOLO_MAP[2]
    for k in range(0, 21):
        off = k * span // 20
        major = k % 5 == 0
        ln = 3 if major else 1
        c = BRASS[5] if major else BRASS[3]
        t.vline(mx0 + off - (1 if k == 20 else 0), fy0 + 2, fy0 + 2 + ln, c)
        t.vline(mx0 + off - (1 if k == 20 else 0), fy1 - 2 - ln, fy1 - 2, c)
        t.hline(fx0 + 2, fx0 + 2 + ln, my0 + off - (1 if k == 20 else 0), c)
        t.hline(fx1 - 2 - ln, fx1 - 2, my0 + off - (1 if k == 20 else 0), c)
    for x, y in ((fx0 + 1, fy0 + 1), (fx1 - 2, fy0 + 1), (fx0 + 1, fy1 - 2), (fx1 - 2, fy1 - 2)):
        bolt(t, x, y, BRASS)
    # The map glass itself, with range rings and a cross the drawn map normally hides.
    for y in range(my0, my1 + 1):
        for x in range(mx0, mx1 + 1):
            n = fbm(x, y, 1031, 400, 256, octaves=3, scale=2.0)
            t.set(x, y, mix(HOLO[0], HOLO[2], 0.25 + 0.3 * n))
    mcx, mcy = (mx0 + mx1 + 1) / 2, (my0 + my1 + 1) / 2
    for rr in (span * 0.25, span * 0.5 - 2):
        for a in range(0, 1440):
            ang = math.radians(a / 4)
            px, py = int(mcx + math.cos(ang) * rr), int(mcy + math.sin(ang) * rr)
            if (a // 12) % 2 == 0:
                t.set(px, py, HOLO[4])
    for k in range(mx0 + 2, mx1 - 1, 3):
        t.set(k, int(mcy), HOLO[3])
        t.set(int(mcx), k - mx0 + my0, HOLO[3])
    outline(t, mx0 - 1, my0 - 1, mx1 + 1, my1 + 1, DARK[0])
    t.hline(mx0, mx1, my0, HOLO[0])
    t.vline(mx0, my0, my1, HOLO[0])
    # Keys under the map.
    for r, glyph in ((L.HOLO_ZOOM_OUT, _glyph_minus), (L.HOLO_ZOOM_IN, _glyph_plus)):
        _holo_button(t, *r, 0)
        glyph(t, r[0] + r[2] // 2, r[1] + r[3] // 2 - 1, HOLO[6])
    for r in (L.HOLO_AUTO, L.HOLO_DESIGNATE, L.HOLO_AIRBURST):
        _holo_button(t, *r, 0)
    # Tabs.
    for r in (L.HOLO_TAB_SILOS, L.HOLO_TAB_RADARS, L.HOLO_TAB_TRACKS):
        _holo_sunk(t, r)
        _holo_tab(t, *r, False)
    # The list: header band, sixteen faint alternating rows and column rules.
    _holo_glass(t, L.HOLO_LIST, 1032)
    lx0, ly0, lx1, ly1 = _box(L.HOLO_LIST)
    hy1 = ly0 + L.HOLO_LIST_HEADER - 1
    _vgrad(t, lx0 + 1, ly0 + 1, lx1 - 1, hy1 - 1, HOLO[4], HOLO[3])
    t.hline(lx0 + 1, lx1 - 1, hy1, HOLO[6])
    for row in range(L.HOLO_LIST_ROWS):
        ry = ly0 + L.HOLO_LIST_HEADER + row * L.HOLO_LIST_ROW
        if row % 2 == 1:
            for y in range(ry, min(ly1, ry + L.HOLO_LIST_ROW)):
                for x in range(lx0 + 1, lx1):
                    t.set(x, y, mix(t.get(x, y), HOLO[3], 0.4))
    for c in (L.HOLO_COL_NAME, L.HOLO_COL_STATE, L.HOLO_COL_INFO, L.HOLO_COL_DELAY, L.HOLO_COL_UNLINK):
        cx = lx0 + c - 2
        t.vline(cx, ly0 + 2, hy1 - 1, HOLO[2])
        for y in range(hy1 + 2, ly1, 2):
            t.set(cx, y, HOLO[3])
    # Detail strip.
    _holo_glass(t, L.HOLO_DETAIL, 1033)
    dx0, dy0, dx1, dy1 = _box(L.HOLO_DETAIL)
    t.vline(dx0 + 1, dy0 + 2, dy1 - 2, HOLO[5])
    # Salvo row: mode key, spacing -/+ and its readout.
    _holo_button(t, *L.HOLO_SALVO_MODE, 0)
    for r, glyph in ((L.HOLO_SPACING_DOWN, _glyph_minus), (L.HOLO_SPACING_UP, _glyph_plus)):
        _holo_button(t, *r, 0)
        glyph(t, r[0] + r[2] // 2, r[1] + r[3] // 2 - 1, HOLO[6])
    _holo_glass(t, L.HOLO_SPACING, 1034)
    # Command keys; arm, disarm and salvo plain, abort a guarded red switch.
    for r in (L.HOLO_ARM, L.HOLO_DISARM, L.HOLO_SALVO):
        _holo_button(t, *r, 0)
    ax0, ay0, ax1, ay1 = _box(L.HOLO_ARM)
    t.rect(ax0 + 2, ay0 + 2, ax0 + 3, ay1 - 3, LAMP_AMBER[3])
    _holo_button(t, *L.HOLO_ABORT, 3)
    # Divider between the map and the console side.
    gx = L.HOLO_MAP_FRAME[0] + L.HOLO_MAP_FRAME[2] + 2
    t.vline(gx, L.HOLO_MAP_FRAME[1], L.HOLO_HEIGHT - 6, DARK[0])
    t.vline(gx + 1, L.HOLO_MAP_FRAME[1], L.HOLO_HEIGHT - 6, DARK[4])
    return t


@gui("holo_table_widgets")
def holo_table_widgets():
    t = Tex(L.HOLO_WIDGETS_W, L.HOLO_WIDGETS_H, 1040)
    lx, ly, lw, lh = L.HOLO_SPRITE_LAMPS
    for i, ramp in enumerate(STATE_LAMPS):
        _lamp(t, lx + i * 8, ly, lh, ramp)
    cx, cy, cw, ch = L.HOLO_SPRITE_CHECKS
    _check(t, cx, cy, False)
    _check(t, cx + ch, cy, True)
    ux, uy, uw, uh = L.HOLO_SPRITE_UNLINK
    _unlink(t, ux, uy, False)
    _unlink(t, ux + uh, uy, True)
    bx, by, bw, bh = L.HOLO_SPRITE_BUTTONS
    for i in range(4):
        _holo_button(t, bx, by + i * 16, bw, 15, i)
    tx, ty, tw, th = L.HOLO_SPRITE_TABS
    _holo_tab(t, tx, ty, tw, 14, False)
    _holo_tab(t, tx, ty + 16, tw, 14, True)
    return t

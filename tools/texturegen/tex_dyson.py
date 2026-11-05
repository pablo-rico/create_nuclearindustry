"""Textures of the Dyson swarm: the EM rail ejector, the ray receiver and the swarm terminal, the solar sail items and the
two screens.

The block models and partials are written by ``tools/modelgen/generate.py`` (section "Dyson swarm") and mapped one texel a
pixel: a side's row r is the height 15 - r in the block (v = 16 - y), a top's texel (u, v) is (x, z). Where a model gives
explicit UVs (rails, coils, the terminal's screen, the receiver's collector) the regions are listed by the texture. The
house materials are Create's: andesite casing (``STEEL``), brass on what turns, copper on what carries power. The coils and
the collector are near-white because their renderers draw them full-bright and tint them.

The screens are painted from ``dyson_layouts.py``, which mirrors ``dyson/gui/EjectorLayout.java`` and ``TerminalLayout.java``;
their text is drawn by code in warm colours (0xFFE6A0, 0xB89A5A), so every well under text is dark.

Imported at the end of ``blocks.py``.
"""

import math
import random

import dyson_layouts as L
from blocks import block
from common import bolt
from gui import gui
from items import item
from palette import BRASS, COPPER, DANGER, DARK, RADIUM, STEEL, WARN, WHITE
from pixelkit import Ramp, Tex, bevel, brushed, fbm, item_outline, mix, outline, shade
from tex_orbit import FOIL

# Near-white copper for the coils (drawn lit and tinted), near-white for the collector.
PALE = COPPER.tinted(WHITE, 0.62)
GLOW = Ramp("#d9cbb2", "#e6dac4", "#efe6d4", "#f6f0e3", "#fbf8ef", "#fefcf7", "#ffffff")
# Gold photovoltaic film and the dish's mirror (warmer and paler than the satellites' foil).
GOLD = FOIL
MIRROR = Ramp("#6e4a16", "#9a6c22", "#c4952f", "#e0b748", "#f0d070", "#fbe6a2", "#fff7da")
# Deep space for the terminal's screens, and the dark warm ink of the screens' wells.
SPACE = Ramp("#04050b", "#080b16", "#0e1324", "#161d36", "#22304e", "#3a4c74", "#7a8cba")
INK = Ramp("#0c0805", "#140e08", "#1d150c", "#281d11", "#352717", "#47351f", "#5e4829")
SUN = Ramp("#6a3a0c", "#a8661a", "#e09a32", "#ffc552", "#ffe08a", "#fff2c4", "#ffffff")


def _box(r):
    """(x, y, w, h) -> inclusive corners."""
    x, y, w, h = r
    return x, y, x + w - 1, y + h - 1


def _noise_fill(t, ramp, x0, y0, x1, y1, seed, lo, hi, scale=4.0):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, 16, 16, octaves=2, scale=scale)
            t.set(x, y, ramp.t(max(0.0, min(1.0, lo + (hi - lo) * n))))


# ======================================================================================
# shared materials
# ======================================================================================

@block("dyson/brass")
def brass():
    """Brass for what turns or bears: races, caps, axles, struts, the sled. Brushed, a lit edge on top, bolts round the
    middle."""
    t = Tex(seed=3100)
    brushed(t, BRASS, base=3.4, amp=0.6, seed=3100)
    t.hline(0, 15, 0, BRASS[6])
    t.hline(0, 15, 15, BRASS[1])
    for x in range(2, 16, 5):
        t.set(x, 7, BRASS[6])
        t.set(x + 1, 8, BRASS[1])
    return t


@block("dyson/frame")
def frame():
    """Dark steel for the frames: the yoke's arms, the breech's spine, the receiver's fork and hub, the terminal's foot."""
    t = Tex(seed=3101)
    brushed(t, DARK, base=3.0, amp=0.6, seed=3101, axis="v")
    outline(t, 0, 0, 15, 15, DARK[0])
    t.hline(1, 14, 1, DARK[5])
    t.vline(1, 1, 14, DARK[4])
    t.hline(1, 14, 14, DARK[1])
    t.vline(14, 1, 14, DARK[1])
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        bolt(t, x, y, STEEL)
    return t


# ======================================================================================
# EM rail ejector: the plinth (block), turntable, rails, coils and sled (partials)
# ======================================================================================

@block("dyson/ejector_casing")
def ejector_casing():
    """The plinth's sides, rows 4..15 (12 px high): a brass band at the top (y 10..12), andesite casing with the stator's
    copper windings showing through a slot, a dark foot. Rows 0..3 (only the particle shows them) carry on the casing."""
    t = Tex(seed=3110)
    brushed(t, STEEL, base=3.4, amp=0.7, seed=3110)
    outline(t, 0, 0, 15, 3, STEEL[1])
    t.hline(1, 14, 1, STEEL[5])
    # Brass band with its bolt heads, and the shadow under it.
    t.hline(0, 15, 4, BRASS[5])
    t.hline(0, 15, 5, BRASS[3])
    for x in (1, 7, 13):
        t.set(x, 4, BRASS[6])
        t.set(x + 1, 5, BRASS[1])
    t.hline(0, 15, 6, DARK[1])
    # Casing edges.
    t.vline(0, 7, 13, STEEL[1])
    t.vline(1, 7, 13, STEEL[5])
    t.vline(14, 7, 13, STEEL[2])
    t.vline(15, 7, 13, STEEL[1])
    t.hline(1, 14, 7, STEEL[5])
    t.hline(1, 14, 13, STEEL[2])
    # The window onto the windings.
    t.rect(3, 8, 12, 12, DARK[0])
    for y in range(9, 12):
        for x in range(4, 12):
            c = COPPER[5] if x % 2 == 0 else COPPER[3]
            if y == 9:
                c = COPPER[6] if x % 2 == 0 else COPPER[4]
            if y == 11:
                c = shade(c, 0.8)
            t.set(x, y, c)
    t.hline(4, 11, 12, STEEL[5])
    t.vline(12, 9, 12, STEEL[5])
    for x in (2, 13):
        t.set(x, 8, STEEL[6])
        t.set(x, 12, STEEL[1])
    # Foot.
    t.hline(0, 15, 14, DARK[4])
    t.hline(0, 15, 15, DARK[1])
    for x in range(1, 16, 3):
        t.set(x, 15, DARK[0])
    return t


@block("dyson/ejector_top")
def ejector_top():
    """From above (u = x, v = z): the rim the turntable runs on (1.5 px), and inside it the floor of the well the breech
    swings down into: dark grating round a brass hub over the shaft, ringed by the stator's copper."""
    t = Tex(seed=3111)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 3111, 16, 16, octaves=2, scale=4.0)
            c = DARK.t(0.16 + 0.14 * n)
            if (x + y) % 3 == 0 or (x - y) % 3 == 0:
                c = shade(c, 0.78)
            t.set(x, y, c)
    t.ring(8, 8, 5.9, 4.7, COPPER[3])
    t.ring(8, 8, 5.9, 5.4, COPPER[5])
    t.ring(8, 8, 4.7, 4.3, DARK[0])
    t.disc(8, 8, 2.7, BRASS[3])
    t.ring(8, 8, 2.7, 2.1, BRASS[5])
    t.disc(8, 8, 1.1, DARK[1])
    for x, y in ((7, 5), (10, 7), (8, 10), (5, 8)):
        t.set(x, y, BRASS[6])
    # The rim: lit from the top-left.
    outline(t, 0, 0, 15, 15, STEEL[1])
    t.hline(1, 14, 1, STEEL[5])
    t.vline(1, 1, 14, STEEL[5])
    t.hline(1, 14, 14, STEEL[3])
    t.vline(14, 1, 14, STEEL[3])
    for x in (4, 8, 12):
        for p in ((x, 0), (0, x), (x, 15), (15, x)):
            t.set(*p, STEEL[4])
    return t


@block("dyson/ejector_bottom")
def ejector_bottom():
    """The plinth from below, round the hole (5..11) the shaft comes up through."""
    t = Tex(seed=3112)
    _noise_fill(t, DARK, 0, 0, 15, 15, 3112, 0.25, 0.5)
    outline(t, 0, 0, 15, 15, DARK[1])
    outline(t, 4, 4, 11, 11, DARK[0])
    t.ring(8, 8, 2.6, 1.8, BRASS[2])
    return t


@block("dyson/ejector_pit")
def ejector_pit():
    """The well's walls (rows 4..8): the stator, copper windings between dark steel teeth, a steel lip at the top."""
    t = Tex(seed=3113)
    for y in range(16):
        for x in range(16):
            k = x % 4
            if k == 0:
                c = DARK[1]
            else:
                c = (COPPER[4], COPPER[3], COPPER[2])[k - 1]
                if y % 4 == 1:
                    c = shade(c, 1.12)
            t.set(x, y, c)
    for y in (0, 4, 8, 12):
        t.hline(0, 15, y, STEEL[4])
    return t


@block("dyson/ejector_rail")
def ejector_rail():
    """The launch rails, by rows: 0..2 a bar's top, 2..4 its underside, 4..7 its outer side (dark steel, a copper bus
    line, brass clamps every 4 px), 7..10 its inner face (the copper conductor, laminated), 10..14 the keel's guide track,
    14..15 the keel's front. u 14..16 of rows 4..7: a bar's muzzle end."""
    t = Tex(seed=3114)
    for x in range(16):
        clamp = x % 4 == 1
        # Bar top and underside.
        t.set(x, 0, DARK[5] if not clamp else BRASS[5])
        t.set(x, 1, DARK[3] if not clamp else BRASS[3])
        t.set(x, 2, DARK[2])
        t.set(x, 3, DARK[1])
        # Outer side.
        t.set(x, 4, DARK[4])
        t.set(x, 5, COPPER[4] if not clamp else BRASS[5])
        t.set(x, 6, DARK[2])
        if clamp:
            t.set(x, 4, BRASS[6])
            t.set(x, 6, BRASS[2])
        # Conductor face: laminations every 2 px.
        lam = x % 2 == 0
        t.set(x, 7, COPPER[6] if lam else COPPER[5])
        t.set(x, 8, COPPER[5] if lam else COPPER[4])
        t.set(x, 9, COPPER[3] if lam else COPPER[2])
        # Keel track: dark edges, a polished guideway with index marks.
        t.set(x, 10, DARK[1])
        t.set(x, 11, STEEL[5] if x % 4 else STEEL[3])
        t.set(x, 12, STEEL[4] if x % 4 else STEEL[2])
        t.set(x, 13, DARK[1])
        t.set(x, 14, DARK[3])
        t.set(x, 15, DARK[1])
    # Muzzle ends.
    t.rect(14, 4, 15, 6, BRASS[4])
    t.set(14, 4, BRASS[6])
    t.set(15, 6, BRASS[1])
    t.set(14, 5, DARK[1])
    return t


@block("dyson/ejector_breech")
def ejector_breech():
    """The breech plate behind the sled (front and back u 4..12, v 1..5.5; its edges v 0..1 and u 0..1): steel with a strip
    of hazard paint, a copper capacitor terminal in the middle and bolts at the corners."""
    t = Tex(seed=3115)
    brushed(t, STEEL, base=3.2, amp=0.5, seed=3115)
    t.hline(0, 15, 0, STEEL[5])
    for x in range(16):
        t.set(x, 1, WARN[4] if (x // 2) % 2 == 0 else DARK[1])
    t.hline(0, 15, 2, STEEL[1])
    t.rect(6, 3, 9, 5, COPPER[3])
    t.hline(6, 9, 3, COPPER[5])
    t.set(9, 5, COPPER[1])
    t.set(7, 4, DARK[1])
    for x in (4, 11):
        t.set(x, 3, STEEL[6])
        t.set(x, 5, STEEL[1])
    t.vline(0, 1, 5, STEEL[2])
    return t


@block("dyson/ejector_coil")
def ejector_coil():
    """The coil rings, near-white copper: rows 0..8 have the windings running across a horizontal bar (columns), rows
    8..16 across an upright one (rows)."""
    t = Tex(seed=3116)
    for y in range(16):
        for x in range(16):
            k = (x if y < 8 else y) % 3
            c = (PALE[5], PALE[4], PALE[3])[k]
            if (x if y < 8 else y) % 9 == 8:
                c = PALE[2]
            t.set(x, y, c)
    return t


@block("dyson/sail_folded")
def sail_folded():
    """The folded sail on the sled: gold photovoltaic film in tight pleats, a crease across every 4 px."""
    t = Tex(seed=3117)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 3117, 16, 16, octaves=2, scale=6.0)
            v = (0.78 if x % 2 == 0 else 0.5) + 0.18 * (n - 0.5)
            if y % 4 == 3:
                v -= 0.25
            t.set(x, y, GOLD.t(max(0.0, min(1.0, v))))
    return t


# ======================================================================================
# ray receiver: the plinth (block), mount, dish and focus (partials)
# ======================================================================================

@block("dyson/receiver_side")
def receiver_side():
    """The plinth's sides, rows 6..15 (10 px high): a lit lip at the deck (row 6), a brass body with a recessed andesite
    panel and louvres, an andesite foot (rows 14..15). Rows 0..5 (the particle) carry on the brass."""
    t = Tex(seed=3120)
    brushed(t, BRASS, base=3.4, amp=0.6, seed=3120)
    t.hline(0, 15, 0, BRASS[6])
    t.hline(0, 15, 6, BRASS[6])
    t.hline(0, 15, 7, BRASS[2])
    # The recessed panel (the terminals sit on it, 5..11 wide, rows 8.5..12.5).
    t.rect(3, 8, 12, 12, STEEL[3])
    for y in range(8, 13):
        for x in range(3, 13):
            t.set(x, y, STEEL[3] if (x + y) % 5 else STEEL[2])
    t.hline(3, 12, 8, STEEL[1])
    t.vline(3, 8, 12, STEEL[1])
    t.hline(3, 12, 12, STEEL[5])
    t.vline(12, 8, 12, STEEL[5])
    t.hline(0, 15, 13, BRASS[1])
    for x in range(16):
        t.set(x, 14, STEEL[4] if x % 4 else STEEL[2])
        t.set(x, 15, STEEL[1])
    return t


@block("dyson/receiver_top")
def receiver_top():
    """The deck from above: brass plate round the turntable's seat (3..13), anchor bolts at the corners."""
    t = Tex(seed=3121)
    brushed(t, BRASS, base=3.2, amp=0.6, seed=3121)
    outline(t, 0, 0, 15, 15, BRASS[1])
    t.hline(1, 14, 1, BRASS[6])
    t.vline(1, 1, 14, BRASS[5])
    t.rect(3, 3, 12, 12, DARK[2])
    outline(t, 2, 2, 13, 13, BRASS[0])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, STEEL)
    return t


@block("dyson/receiver_bottom")
def receiver_bottom():
    t = Tex(seed=3122)
    _noise_fill(t, STEEL, 0, 0, 15, 15, 3122, 0.2, 0.45)
    outline(t, 0, 0, 15, 15, STEEL[1])
    return t


@block("dyson/receiver_terminal")
def receiver_terminal():
    """The FE terminals: a copper pad (u 5..11, v 8.5..12.5 on every side) in a dark insulator, the studs' heads at u 0..1.5,
    v 0..1.5."""
    t = Tex(seed=3123)
    brushed(t, COPPER, base=3.6, amp=0.6, seed=3123)
    outline(t, 5, 8, 10, 12, DARK[1])
    t.hline(6, 9, 9, COPPER[6])
    t.vline(6, 9, 11, COPPER[5])
    t.hline(6, 9, 11, COPPER[2])
    t.set(5, 8, DANGER[3])
    t.set(10, 8, DARK[3])
    # Stud heads.
    t.rect(0, 0, 1, 1, COPPER[5])
    t.set(0, 0, COPPER[6])
    t.set(1, 1, COPPER[2])
    return t


@block("dyson/turntable")
def turntable():
    """The receiver's turntable: from above (u = x, v = z, its plate 2.5..13.5) dark steel round a brass race; its sides
    take rows 0..1.5, the brass rim."""
    t = Tex(seed=3124)
    brushed(t, DARK, base=2.6, amp=0.5, seed=3124)
    t.ring(8, 8, 5.4, 4.4, BRASS[3])
    t.ring(8, 8, 4.4, 3.8, BRASS[1])
    t.hline(0, 15, 0, BRASS[5])
    t.hline(0, 15, 1, BRASS[2])
    for x, y in ((8, 3), (3, 8), (12, 8), (8, 12)):
        t.set(x, y, BRASS[6])
    outline(t, 3, 3, 12, 12, DARK[0])
    return t


@block("dyson/receiver_mirror")
def receiver_mirror():
    """The dish's mirror: polished gold, a broad sheen from the top-left, faint seams every 4 px; rows 14..16 are the darker
    gold of the terraces' risers."""
    t = Tex(seed=3125)
    for y in range(14):
        for x in range(16):
            n = fbm(x, y, 3125, 16, 16, octaves=2, scale=4.0)
            band = math.sin((x - y) * 0.45) * 0.08
            v = 0.62 + 0.18 * (1 - (x + y) / 28) + 0.1 * (n - 0.5) + band
            c = MIRROR.t(max(0.0, min(1.0, v)))
            if x % 4 == 3 or y % 4 == 3:
                c = shade(c, 0.9)
            t.set(x, y, c)
    for x in range(16):
        t.set(x, 14, MIRROR[3] if x % 2 else MIRROR[2])
        t.set(x, 15, MIRROR[1])
    return t


@block("dyson/receiver_dish_back")
def receiver_dish_back():
    """The back of the dish and its edges: andesite-grey steel with ribs and bolts."""
    t = Tex(seed=3126)
    brushed(t, STEEL, base=3.3, amp=0.55, seed=3126)
    for i in (0, 8):
        t.hline(0, 15, i, STEEL[2])
        t.vline(i, 0, 15, STEEL[2])
        t.hline(0, 15, i + 1, STEEL[5])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.set(x, y, STEEL[6])
        t.set(x + 1, y + 1, STEEL[1])
    return t


@block("dyson/receiver_focus")
def receiver_focus():
    """The collector at the focus, near-white: every face shows the middle (u, v 4.5..11.5, at twice the density), a soft
    warm core inside a pale rim."""
    t = Tex(seed=3127)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            t.set(x, y, GLOW.t(max(0.0, min(1.0, 1.0 - d / 7.0))))
    t.ring(8, 8, 3.6, 3.0, GLOW[2])
    return t


# ======================================================================================
# swarm terminal (block model)
# ======================================================================================

def _andesite(t, x0, y0, x1, y1, seed):
    """A recessed andesite panel in the brass, shadowed top-left."""
    brushed(t, STEEL, x0, y0, x1, y1, base=3.1, amp=0.5, seed=seed)
    t.hline(x0, x1, y0, STEEL[1])
    t.vline(x0, y0, y1, STEEL[1])
    t.hline(x0 + 1, x1, y1, STEEL[5])
    t.vline(x1, y0 + 1, y1, STEEL[5])


def _louvres(t, x0, y0, x1, y1):
    for y in range(y0, y1 + 1, 2):
        t.hline(x0, x1, y, STEEL[1])
        if y + 1 <= y1:
            t.hline(x0, x1, y + 1, STEEL[4])


def _brass_plate(t, seed):
    brushed(t, BRASS, base=3.3, amp=0.55, seed=seed, axis="v")


@block("dyson/terminal_side")
def terminal_side():
    """The console's sides: the desk (z 1..7, up to y 7.5) and the cabinet (z 7..16, up to y 10), rows 6..14. A brass
    frame, lit along the top, round andesite panels with louvres; a brass post where desk and cabinet meet (u 7..9)."""
    t = Tex(seed=3130)
    _brass_plate(t, 3130)
    t.hline(0, 15, 6, BRASS[6])
    t.hline(0, 15, 13, BRASS[1])
    t.vline(7, 0, 15, BRASS[5])
    t.vline(8, 0, 15, BRASS[2])
    _andesite(t, 1, 9, 6, 12, 3136)
    _andesite(t, 10, 8, 14, 12, 3137)
    _louvres(t, 11, 9, 13, 11)
    t.set(2, 10, BRASS[5])
    t.set(5, 10, BRASS[5])
    outline(t, 0, 0, 15, 5, BRASS[1])
    return t


@block("dyson/terminal_front")
def terminal_front():
    """The desk's front (u 1..15, rows 8.5..14): a brass lip, an andesite panel with two little brass-rimmed dials either
    side of a dark nameplate, a dark kick plate."""
    t = Tex(seed=3131)
    _brass_plate(t, 3131)
    t.hline(0, 15, 8, BRASS[6])
    t.hline(0, 15, 9, BRASS[3])
    _andesite(t, 2, 10, 13, 13, 3138)
    for cx in (4, 11):
        t.rect(cx - 1, 11, cx, 12, INK[5])
        t.set(cx - 1, 11, GLOW[3])
        t.set(cx, 12, BRASS[2])
        t.set(cx - 2, 11, BRASS[4])
        t.set(cx + 1, 12, BRASS[2])
    t.rect(6, 11, 9, 12, INK[2])
    t.hline(6, 9, 11, INK[4])
    t.set(7, 12, SUN[4])
    t.hline(0, 15, 14, DARK[3])
    t.hline(0, 15, 15, DARK[1])
    outline(t, 0, 0, 15, 7, BRASS[1])
    return t


@block("dyson/terminal_desk")
def terminal_desk():
    """The desk's top (u = x 1..15, v = z 1..5 in front of the screen): a brass rim round a dark key bed with a row of
    lamps in the swarm's four orbit colours and keys."""
    t = Tex(seed=3132)
    _brass_plate(t, 3132)
    t.rect(2, 2, 13, 6, INK[3])
    t.hline(2, 13, 1, BRASS[1])
    t.hline(2, 13, 7, BRASS[5])
    for i, c in enumerate(((255, 200, 87, 255), (127, 216, 255, 255), (255, 143, 184, 255), (168, 255, 138, 255))):
        t.set(5 + i * 2, 3, c)
    for x in range(3, 13, 2):
        t.set(x, 5, STEEL[5])
        t.set(x, 6, STEEL[2])
    t.hline(0, 15, 0, BRASS[6])
    return t


@block("dyson/terminal_top")
def terminal_top():
    """Tops of the cabinet and the electronics box: an andesite grille in a brass rim."""
    t = Tex(seed=3133)
    _brass_plate(t, 3133)
    outline(t, 0, 0, 15, 15, BRASS[1])
    t.hline(1, 14, 1, BRASS[6])
    _andesite(t, 3, 3, 12, 12, 3139)
    _louvres(t, 4, 4, 11, 11)
    return t


@block("dyson/terminal_back")
def terminal_back():
    """The cabinet's back and the electronics box: andesite in a brass frame, louvres and a copper cable gland."""
    t = Tex(seed=3134)
    _brass_plate(t, 3134)
    outline(t, 0, 0, 15, 15, BRASS[1])
    t.hline(1, 14, 1, BRASS[6])
    _andesite(t, 2, 3, 13, 13, 3142)
    _louvres(t, 4, 4, 11, 8)
    t.disc(8, 11.5, 1.7, DARK[1])
    t.ring(8, 11.5, 2.2, 1.6, COPPER[4])
    return t


# The monitor's picture, 10x6 inside its bezel: the sun with the swarm's ring round it, seen edge-on (the far half
# dim behind the sun, the near half bright across it, a sail catching the light).
SCREEN_ART = [
    "....hh....",
    ".aahSShaa.",
    "a.hSSSSh.a",
    ".AAAAAwAA.",
    "...hSSh...",
    "....hh....",
]


@block("dyson/terminal_screen")
def terminal_screen():
    """The monitor's face (u 2..14, v 4..12): a brass bezel round dark glass showing the sun and its ring of sails. The rest
    of the texture is the bezel's brass."""
    t = Tex(seed=3135)
    _brass_plate(t, 3135)
    x0, y0, x1, y1 = 2, 4, 13, 11
    t.rect(x0, y0, x1, y1, BRASS[4])
    t.hline(x0, x1, y0, BRASS[6])
    t.vline(x0, y0, y1, BRASS[5])
    t.hline(x0, x1, y1, BRASS[1])
    t.vline(x1, y0, y1, BRASS[2])
    for y in range(y0 + 1, y1):
        for x in range(x0 + 1, x1):
            t.set(x, y, SPACE[2] if (y - y0) % 2 else SPACE[1])
    t.stamp(SCREEN_ART, {"a": mix(SPACE[1], SUN[2], 0.55), "A": SUN[3], "w": WHITE, "h": mix(SPACE[1], SUN[2], 0.7),
                         "S": SUN[5]}, x0 + 1, y0 + 1)
    t.set(x0 + 5, y0 + 2, SUN[6])
    return t


# ======================================================================================
# items
# ======================================================================================

def _hex_pts(cx, cy, rx, ry):
    """A flat-topped hexagon, ``rx`` across and ``ry`` up from its centre."""
    pts = set()
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x + 0.5 - cx), abs(y + 0.5 - cy)
            if dy <= ry and dx <= rx - (dy / ry) * rx * 0.5:
                pts.add((x, y))
    return pts


SAIL_HEX = _hex_pts(8, 8, 7.6, 6.6)
# The hexagon's corners, where the spars run from the hub.
SAIL_CORNERS = ((4, 2), (11, 2), (15, 8), (11, 13), (4, 13), (0, 8))


def _sail_film(t, pts, seed):
    """Gold photovoltaic film lit from the top-left: a lattice of cells and a broad sheen."""
    for (x, y) in pts:
        n = fbm(x, y, seed, 16, 16, octaves=2, scale=5.0)
        v = 0.58 + 0.24 * (1 - (x + y) / 26) + 0.12 * (n - 0.5)
        if 6 <= x + y <= 8:
            v += 0.14
        if (x + y) % 4 == 0 or (x - y) % 4 == 0:
            v -= 0.2
        t.set(x, y, GOLD.t(max(0.1, min(1.0, v))))


def _sail_spars(t, pts, ramp, lit):
    for x1, y1 in SAIL_CORNERS:
        t.line(7.5, 7.5, x1, y1, ramp[lit])


def _sail_frame(t, pts):
    """The brass edge: every pixel on the hexagon's border, lit top-left."""
    for (x, y) in pts:
        if (x, y - 1) not in pts or (x - 1, y) not in pts:
            t.set(x, y, BRASS[6])
        elif (x, y + 1) not in pts or (x + 1, y) not in pts:
            t.set(x, y, BRASS[2])


def _outline_outside(t, pts, color):
    """The item's dark edge round the hexagon only, not round the holes inside it."""
    for y in range(16):
        for x in range(16):
            if (x, y) in pts:
                continue
            if any((x + dx, y + dy) in pts for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                t.set(x, y, color)


@item("solar_sail")
def solar_sail():
    """A solar sail: a hexagon of gold photovoltaic film, a lattice of cells, bright spars from the hub to its corners and
    a brass edge."""
    t = Tex(16, 16, 3140)
    _sail_film(t, SAIL_HEX, 3140)
    _sail_spars(t, SAIL_HEX, GOLD, 5)
    _sail_frame(t, SAIL_HEX)
    t.rect(7, 7, 8, 8, GOLD[6])
    t.set(7, 7, WHITE)
    _outline_outside(t, SAIL_HEX, GOLD[0])
    return t


@item("incomplete_solar_sail")
def incomplete_solar_sail():
    """Part-made: the brass hexagon and its spars, film laid on the upper three of its six sectors only."""
    t = Tex(16, 16, 3141)
    film = {(x, y) for (x, y) in SAIL_HEX if y < 8}
    _sail_film(t, film, 3141)
    _sail_spars(t, SAIL_HEX, BRASS, 4)
    _sail_frame(t, SAIL_HEX)
    t.rect(7, 7, 8, 8, BRASS[5])
    t.set(8, 8, BRASS[2])
    _outline_outside(t, SAIL_HEX, BRASS[0])
    return t


# ======================================================================================
# screens
# ======================================================================================

def _panel(w, h, seed):
    """Create-style screen plate: andesite inside a brass frame, bolts at the corners."""
    t = Tex(w, h, seed)
    brushed(t, STEEL, base=3.3, amp=0.45, seed=seed, scale=11.0)
    outline(t, 0, 0, w - 1, h - 1, DARK[0])
    bevel(t, 1, 1, w - 2, h - 2, BRASS, hi=BRASS[6], lo=BRASS[1])
    bevel(t, 2, 2, w - 3, h - 3, BRASS, hi=BRASS[4], lo=BRASS[2])
    outline(t, 3, 3, w - 4, h - 4, STEEL[1])
    t.hline(4, w - 5, 4, STEEL[5])
    t.vline(4, 4, h - 5, STEEL[5])
    for x, y in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
        bolt(t, x, y, BRASS)
    return t


def _lip(t, x0, y0, x1, y1):
    """The edge of a well sunk into the plate, one pixel outside (x0, y0)-(x1, y1): shadow top-left, light bottom-right."""
    t.hline(x0 - 1, x1 + 1, y0 - 1, DARK[0])
    t.vline(x0 - 1, y0 - 1, y1 + 1, DARK[0])
    t.hline(x0, x1 + 1, y1 + 1, STEEL[5])
    t.vline(x1 + 1, y0, y1 + 1, STEEL[5])


def _well(t, r, seed, fill=None):
    """A dark warm well for text, sunk into the plate."""
    x0, y0, x1, y1 = _box(r)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, 256, 256, octaves=2, scale=8.0)
            t.set(x, y, fill or mix(INK[1], INK[2], 0.3 + 0.5 * n))
    t.hline(x0, x1, y0, INK[0])
    t.vline(x0, y0, y1, INK[0])
    _lip(t, x0, y0, x1, y1)


def _slot(t, x, y):
    """An 18 px slot frame round the 16 px item area at (x, y)."""
    t.rect(x - 1, y - 1, x + 16, y + 16, mix(STEEL[1], STEEL[2], 0.4))
    t.hline(x - 1, x + 16, y - 1, DARK[0])
    t.vline(x - 1, y - 1, y + 16, DARK[0])
    t.hline(x, x + 16, y + 16, STEEL[5])
    t.vline(x + 16, y, y + 16, STEEL[5])
    t.set(x - 1, y + 16, STEEL[3])
    t.set(x + 16, y - 1, STEEL[3])


def _sail_ghost(t, x, y):
    """A faint diamond in a magazine slot: what goes there."""
    c = mix(STEEL[1], STEEL[3], 0.55)
    for k in range(5):
        t.set(x + 7 - k, y + 3 + k, c)
        t.set(x + 8 + k, y + 3 + k, c)
        t.set(x + 7 - k, y + 12 - k, c)
        t.set(x + 8 + k, y + 12 - k, c)


def _title(t, r, seed):
    """The title strip: a dark well with a little sun at its right end."""
    _well(t, r, seed)
    x0, y0, x1, y1 = _box(r)
    cx, cy = x1 - 6, (y0 + y1) / 2 + 0.5
    t.disc(cx + 0.5, cy, 2.4, SUN[3])
    t.disc(cx + 0.5, cy, 1.4, SUN[5])
    for k in range(-5, -2):
        t.set(cx + k, int(cy), INK[5])
    for k in range(4, 7):
        t.set(cx + k, int(cy), INK[5])


def _gauge(t, r, full):
    """The ejector's charge gauge, inside its rect: a dark trough (or, full, a glowing column from white-hot at the foot to
    gold at the top) with a tick every 4 px."""
    x0, y0, x1, y1 = _box(r)
    t.rect(x0, y0, x1, y1, DARK[0])
    for y in range(y0 + 1, y1):
        f = (y1 - y) / max(1, (y1 - y0))
        for x in range(x0 + 1, x1):
            if full:
                edge = x in (x0 + 1, x1 - 1)
                c = SUN.t(max(0.0, min(1.0, 0.95 - 0.55 * f - (0.15 if edge else 0))))
            else:
                c = INK[2] if (x + y) % 2 else INK[1]
            t.set(x, y, c)
    for y in range(y1 - 4, y0, -4):
        t.hline(x0 + 1, x0 + 3, y, INK[0] if full else INK[5])
        t.hline(x1 - 3, x1 - 1, y, INK[0] if full else INK[5])


def _orbit_button(t, x, y, w, h, state):
    """An orbit selector, 0 open (dark bronze, raised), 1 picked (lit: gold rim, amber lamps), 2 closed (near-black,
    hatched). The label is drawn in the middle, so the face stays dark."""
    x1, y1 = x + w - 1, y + h - 1
    if state == 2:
        t.rect(x, y, x1, y1, INK[1])
        for yy in range(y + 1, y1):
            for xx in range(x + 1, x1):
                if (xx + yy) % 4 == 0:
                    t.set(xx, yy, INK[3])
        bevel(t, x, y, x1, y1, INK, hi=INK[4], lo=INK[0])
        outline(t, x, y, x1, y1, DARK[0])
        return
    lit = state == 1
    for yy in range(y, y1 + 1):
        f = (yy - y) / max(1, h - 1)
        t.hline(x, x1, yy, mix(INK[6] if lit else mix(INK[5], BRASS[1], 0.5), INK[3], f))
    if lit:
        bevel(t, x, y, x1, y1, SUN, hi=SUN[5], lo=SUN[1])
        outline(t, x + 1, y + 1, x1 - 1, y1 - 1, SUN[2])
        for lx in (x + 3, x1 - 4):
            t.rect(lx, y + 5, lx + 1, y + 8, SUN[5])
            t.set(lx, y + 5, SUN[6])
    else:
        bevel(t, x, y, x1, y1, BRASS, hi=BRASS[5], lo=BRASS[1])
        for lx in (x + 3, x1 - 4):
            t.rect(lx, y + 5, lx + 1, y + 8, INK[1])
            t.set(lx + 1, y + 8, BRASS[3])
    outline(t, x, y, x1, y1, DARK[0])


@gui("em_rail_ejector_screen")
def em_rail_ejector_screen():
    t = Tex(L.EJECTOR_SHEET, L.EJECTOR_SHEET, 3150)
    panel = _panel(L.EJECTOR_WIDTH, L.EJECTOR_HEIGHT, 3150)
    t.im.paste(panel.im, (0, 0))
    _title(t, L.EJECTOR_TITLE, 3151)
    # The magazine: 2 rows of 8 slots, a ghost of a sail in each, inside a sunk tray.
    sx0, sy0, sx1, sy1 = _box(L.EJECTOR_SAILS)
    _lip(t, sx0, sy0, sx1, sy1)
    cols, rows = L.EJECTOR_SAILS[2] // 18, L.EJECTOR_SAILS[3] // 18
    for r in range(rows):
        for c in range(cols):
            x, y = L.EJECTOR_SAIL_X + 18 * c, L.EJECTOR_SAIL_Y + 18 * r
            _slot(t, x, y)
            _sail_ghost(t, x, y)
    # Charge gauge, empty; the screen draws the full sprite over it from the foot.
    gx0, gy0, gx1, gy1 = _box(L.EJECTOR_CHARGE)
    _lip(t, gx0, gy0, gx1, gy1)
    _gauge(t, L.EJECTOR_CHARGE, full=False)
    # Orbit buttons: sockets under each (the sprite is drawn over), showing the open state.
    for r in (L.EJECTOR_ORBIT_0, L.EJECTOR_ORBIT_1, L.EJECTOR_ORBIT_2, L.EJECTOR_ORBIT_3):
        x0, y0, x1, y1 = _box(r)
        _lip(t, x0, y0, x1, y1)
        _orbit_button(t, r[0], r[1], r[2], r[3], 0)
    # Status: two lines of text, a faint rule between them.
    _well(t, L.EJECTOR_STATUS, 3152)
    x0, y0, x1, y1 = _box(L.EJECTOR_STATUS)
    for x in range(x0 + 3, x1 - 2, 2):
        t.set(x, y0 + 11, INK[4])
    # Player inventory: three rows and the hotbar, a groove between them.
    ix0, iy0, ix1, iy1 = _box(L.EJECTOR_INVENTORY)
    for r in range(3):
        for c in range(9):
            _slot(t, L.EJECTOR_INV_X + 18 * c, L.EJECTOR_INV_Y + 18 * r)
    for c in range(9):
        _slot(t, L.EJECTOR_INV_X + 18 * c, L.EJECTOR_HOTBAR_Y)
    gy = (L.EJECTOR_INV_Y + 18 * 3 + L.EJECTOR_HOTBAR_Y) // 2 - 1
    t.hline(ix0 + 2, ix1 - 2, gy, STEEL[1])
    t.hline(ix0 + 2, ix1 - 2, gy + 1, STEEL[5])
    # Sprites: the orbit button in its three states, and the full gauge.
    ox, oy, ow, oh = L.EJECTOR_SPRITE_ORBIT
    bh = oh // 3
    for state in range(3):
        _orbit_button(t, ox, oy + state * bh, ow, bh, state)
    _gauge(t, L.EJECTOR_SPRITE_CHARGE, full=True)
    return t


def _sky(t):
    """The swarm terminal's sky plot: a brass porthole round dark space with a speckle of stars, faint rings every 10
    degrees from the sun (PIXELS_PER_DEGREE = 58 / 40) and a faint cross. The sun and the orbits are drawn by code."""
    x0, y0, x1, y1 = _box(L.TERMINAL_SKY)
    t.rect(x0, y0, x1, y1, INK[1])
    _lip(t, x0, y0, x1, y1)
    cx, cy, radius = L.TERMINAL_SKY_CX + 0.5, L.TERMINAL_SKY_CY + 0.5, L.TERMINAL_SKY_RADIUS
    rng = random.Random(3160)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= radius - 0.5:
                n = fbm(x, y, 3161, 256, 256, octaves=3, scale=6.0)
                t.set(x, y, mix(SPACE[1], SPACE[2], 0.25 + 0.6 * n + 0.2 * (1 - d / radius)))
            elif d <= radius + 1.2:
                t.set(x, y, BRASS[5] if (x + 0.5 - cx) + (y + 0.5 - cy) < 0 else BRASS[2])
            elif d <= radius + 1.9:
                t.set(x, y, BRASS[0])
    for _ in range(140):
        a, r = rng.uniform(0, math.tau), radius * math.sqrt(rng.random()) * 0.97
        x, y = int(cx + math.cos(a) * r), int(cy + math.sin(a) * r)
        b = rng.random()
        t.set(x, y, SPACE[6] if b > 0.85 else SPACE[5] if b > 0.5 else SPACE[4])
    ppd = 58 / 40
    for deg in (10, 20, 30, 40):
        r = deg * ppd
        steps = int(math.tau * r * 1.5)
        for k in range(steps):
            if (k // 2) % 2:
                continue
            ang = math.tau * k / steps
            t.set(int(cx + math.cos(ang) * r), int(cy + math.sin(ang) * r), SPACE[4] if deg < 40 else SPACE[5])
    for k in range(int(cx - radius) + 3, int(cx + radius) - 2, 3):
        t.set(k, int(cy), SPACE[3])
    for k in range(int(cy - radius) + 3, int(cy + radius) - 2, 3):
        t.set(int(cx), k, SPACE[3])
    # Bearing ticks on the brass ring, north at the top.
    for deg in range(0, 360, 30):
        a = math.radians(deg)
        for k in (0, 1):
            r = radius - 1 - k
            t.set(int(cx + math.sin(a) * r), int(cy - math.cos(a) * r), BRASS[5] if deg % 90 == 0 else SPACE[5])


def _tab(t, x, y, w, h, picked):
    x1, y1 = x + w - 1, y + h - 1
    for yy in range(y, y1 + 1):
        f = (yy - y) / max(1, h - 1)
        t.hline(x, x1, yy, mix(INK[5] if picked else INK[3], INK[3] if picked else INK[1], f))
    if picked:
        bevel(t, x, y, x1, y1, BRASS, hi=BRASS[6], lo=BRASS[2])
        t.hline(x + 2, x1 - 2, y1 - 1, SUN[4])
    else:
        bevel(t, x, y, x1, y1, BRASS, hi=BRASS[3], lo=BRASS[0])
        t.hline(x + 2, x1 - 2, y1 - 1, INK[4])
    outline(t, x, y, x1, y1, DARK[0])


def _toggle(t, x, y, w, h, open_):
    """The orbit's open/closed switch: a dark plate, a lamp at the left end (green open, dark red closed)."""
    x1, y1 = x + w - 1, y + h - 1
    for yy in range(y, y1 + 1):
        f = (yy - y) / max(1, h - 1)
        t.hline(x, x1, yy, mix(INK[4], INK[2], f))
    bevel(t, x, y, x1, y1, BRASS, hi=BRASS[5] if open_ else BRASS[3], lo=BRASS[1])
    outline(t, x, y, x1, y1, DARK[0])
    # Two lamps: green at the left end lit while open, red at the right end lit while closed.
    ly = y + (h - 6) // 2
    for lx, lamp, lit in ((x + 3, RADIUM, open_), (x1 - 8, DANGER, not open_)):
        t.rect(lx, ly, lx + 5, ly + 5, DARK[0])
        t.rect(lx + 1, ly + 1, lx + 4, ly + 4, lamp[5] if lit else lamp[1])
        t.set(lx + 1, ly + 1, lamp[6] if lit else lamp[2])


def _slider(t, x, y, w, h):
    """A slider's track: a dark plate with a groove along the middle and ticks every tenth."""
    x1, y1 = x + w - 1, y + h - 1
    t.rect(x, y, x1, y1, INK[2])
    bevel(t, x, y, x1, y1, INK, hi=INK[0], lo=INK[5])
    outline(t, x, y, x1, y1, DARK[0])
    my = y + h // 2
    t.hline(x + 3, x1 - 3, my - 1, INK[0])
    t.hline(x + 3, x1 - 3, my, INK[4])
    for i in range(11):
        tx = x + 3 + round(i * (w - 7) / 10)
        t.set(tx, y + 2, INK[5])
        t.set(tx, y1 - 2, INK[5])


def _knob(t, x, y, w, h):
    x1, y1 = x + w - 1, y + h - 1
    t.rect(x, y, x1, y1, BRASS[4])
    bevel(t, x, y, x1, y1, BRASS, hi=BRASS[6], lo=BRASS[1])
    outline(t, x, y, x1, y1, DARK[0])
    for yy in range(y + 4, y1 - 3, 2):
        t.hline(x + 2, x1 - 2, yy, BRASS[2])


@gui("swarm_terminal_screen")
def swarm_terminal_screen():
    t = Tex(L.TERMINAL_SHEET, L.TERMINAL_SHEET, 3155)
    panel = _panel(L.TERMINAL_WIDTH, L.TERMINAL_HEIGHT, 3155)
    t.im.paste(panel.im, (0, 0))
    _title(t, L.TERMINAL_TITLE, 3156)
    _sky(t)
    for r in (L.TERMINAL_TAB_0, L.TERMINAL_TAB_1, L.TERMINAL_TAB_2, L.TERMINAL_TAB_3):
        x0, y0, x1, y1 = _box(r)
        _lip(t, x0, y0, x1, y1)
        _tab(t, r[0], r[1], r[2], r[3], False)
    # The orbit's panel: a darker sunk plate holding the switch, the three sliders and the orbit's figures.
    px0, py0, px1, py1 = _box(L.TERMINAL_PANEL)
    for y in range(py0, py1 + 1):
        for x in range(px0, px1 + 1):
            t.set(x, y, shade(t.get(x, y), 0.72))
    _lip(t, px0, py0, px1, py1)
    _toggle(t, *L.TERMINAL_ENABLE, False)
    for r in (L.TERMINAL_RADIUS, L.TERMINAL_INCLINATION, L.TERMINAL_NODE):
        _slider(t, *r)
    _well(t, L.TERMINAL_ORBIT_STATS, 3157)
    sx0, sy0, sx1, sy1 = _box(L.TERMINAL_ORBIT_STATS)
    for x in range(sx0 + 3, sx1 - 2, 2):
        t.set(x, sy0 + 13, INK[4])
    # The whole swarm: a strip of two lines.
    _well(t, L.TERMINAL_SWARM, 3158)
    wx0, wy0, wx1, wy1 = _box(L.TERMINAL_SWARM)
    for x in range(wx0 + 3, wx1 - 2, 2):
        t.set(x, wy0 + 15, INK[4])
    # Sprites.
    x, y, w, h = L.TERMINAL_SPRITE_TAB
    _tab(t, x, y, w, h // 2, False)
    _tab(t, x, y + h // 2, w, h // 2, True)
    x, y, w, h = L.TERMINAL_SPRITE_TOGGLE
    _toggle(t, x, y, w, h // 2, False)
    _toggle(t, x, y + h // 2, w, h // 2, True)
    _slider(t, *L.TERMINAL_SPRITE_SLIDER)
    _knob(t, *L.TERMINAL_SPRITE_KNOB)
    return t

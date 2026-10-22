"""Textures of the gas centrifuge and its variant, the electrolyzer: a Create-style C frame round a spinning rotor.

Both share one model (``tools/modelgen/generate.py``, ``processing_machines``), built facing north: a 2 px plinth
(y 0..2) and a 6 px head (y 10..16) held by a 4 px gearbox plate across the back (z 12..16); the front and both sides
are open, so the rotor is seen turning between them. The body faces use position UVs (u = 16 - x on the north face,
u = z on both sides, the east side mirrored), so each face texture is a map of that face:

* ``front`` (north): the head band (rows 0..5) with the rims of the gauge recess (u 2..6) and the product-port recess
  (u 9..13); the plinth (rows 14..15). Rows 6..13 are open in the model, so the recess floors take their texels from
  there: the dial at u 3..7, v 7..11 (the needle partial pivots on its centre pixel, (5, 9)), the port at u 8..12.
* ``side`` (u = z): the head band with the floor of the pocket (open at the top) that holds the input flange and the
  cascade manifold (u 8..11, rows 0..3), the plinth, and the gearbox plate's flank (u 12..15, full height).
* ``back`` (south, u = x): the gearbox plate, a brass collar round the shaft hole (u 5..10, v 5..10).
* ``top`` (u = x, v = z): the feed port on the head, the plate behind it; ``bottom``: the tails port.
* ``chamber``: the deck under the rotor and the head's underside, a bearing ring round (8, 8); ``wall``: the gearbox
  plate's inner face behind the rotor (u = 16 - x, rows 6..13 show).
* ``rotor``: the drum unrolled for the rotor's explicit UVs (u 0..7 the marked north quarter, 7..14 east and west,
  9..16 south; rows 6..13 = y 10..2).
* Shared by both machines: ``inner`` (recess walls), ``collar`` (the shaft hole's walls), ``needle`` (needle u 7..8,
  v 0..11; hub u 6..9, v 12..15) and ``manifold`` (the cascade coupling: pipe u 0..1, v 0..2; brass flange face
  u 0..3, v 6..9; its rims u 6 and u 8).

The electrolyzer swaps the steel for a copper cell casing with dark insulating bands, a sight glass of bubbling
electrolyte down each flank, a square item hatch underneath and a copper-wound armature for a rotor. This module is
imported at the end of ``blocks.py``.
"""

import math

from blocks import block
from common import bolt
from palette import BRASS, COPPER, CYAN, DANGER, DARK, PLASMA, RADIUM, STEEL, WARN
from pixelkit import Ramp, Tex, brushed, shade

# Off-white enamel of the dial face.
ENAMEL = Ramp("#5a574d", "#858071", "#aaa593", "#c8c3ae", "#dcd8c4", "#ece9d8", "#faf8ee")
# Cream insulating tape on the armature's marked coil.
TAPE = Ramp("#5f594a", "#857d68", "#ab9f86", "#c8bc9f", "#ddd1b4", "#ece3c8", "#f8f1dd")

# body: head and plinth; plate: the gearbox plate; trim: bolts, rims and the bearing ring; band: insulating bands.
CENTRIFUGE = {"body": STEEL, "plate": DARK, "trim": BRASS, "band": None, "seed": 1500}
ELECTROLYZER = {"body": COPPER, "plate": DARK, "trim": COPPER, "band": DARK, "seed": 1520}


# ======================================================================================
# helpers
# ======================================================================================

def _member(t, x0, y0, x1, y1, R, seed, base=3.6, outline="", axis="h"):
    """A raised frame member: brushed fill, lit top/left edge, shaded bottom/right edge. ``outline`` names the edges
    ('tlbr') that are the block's own border, drawn darker so neighbouring blocks show a joint. Copper is brushed
    more softly than steel, or its grain reads as wood."""
    brushed(t, R, x0, y0, x1, y1, base=base, amp=0.3 if R is COPPER else 0.55, seed=seed, axis=axis)
    t.hline(x0, x1, y0, R[5])
    t.vline(x0, y0, y1, R[5])
    t.hline(x0, x1, y1, R[1])
    t.vline(x1, y0, y1, R[1])
    t.set(x1, y0, R[3])
    t.set(x0, y1, R[3])
    for side in outline:
        if side == "t":
            t.hline(x0, x1, y0, R[2])
        elif side == "l":
            t.vline(x0, y0, y1, R[2])
        elif side == "b":
            t.hline(x0, x1, y1, R[0])
        elif side == "r":
            t.vline(x1, y0, y1, R[0])


def _band(t, x0, y0, x1, y1, R):
    """An insulating band: a dark ribbed strip, lit along its top."""
    for x in range(x0, x1 + 1):
        t.set(x, y0, R[4])
        for y in range(y0 + 1, y1):
            t.set(x, y, R[2] if (x + y) % 2 else R[1])
        t.set(x, y1, R[0])


def _bezel(t, x0, y0, x1, y1, R):
    """The rim round a recess: shaded above and left (the recess is sunk in), lit below and right."""
    t.hline(x0 - 1, x1 + 1, y0 - 1, R[1])
    t.vline(x0 - 1, y0 - 1, y1 + 1, R[1])
    t.hline(x0 - 1, x1 + 1, y1 + 1, R[5])
    t.vline(x1 + 1, y0 - 1, y1 + 1, R[5])


def _flange5(t, x0, y0, A):
    """A 5x5 round output port seen face-on: a lit ring of the accent ramp round a dark bore."""
    ring = [(1, 0), (2, 0), (3, 0), (0, 1), (4, 1), (0, 2), (4, 2), (0, 3), (4, 3), (1, 4), (2, 4), (3, 4)]
    for x, y in ring:
        t.set(x0 + x, y0 + y, A[5] if x + y <= 3 else A[2])
    for x, y in ((0, 0), (4, 0), (0, 4), (4, 4)):
        t.set(x0 + x, y0 + y, DARK[1])
    t.rect(x0 + 1, y0 + 1, x0 + 3, y0 + 3, A[3])
    t.set(x0 + 1, y0 + 1, A[4])
    t.set(x0 + 2, y0 + 2, DARK[0])
    t.set(x0 + 3, y0 + 2, A[1])
    t.set(x0 + 2, y0 + 3, A[6])
    t.set(x0 + 3, y0 + 3, A[2])


def _dial(t, x0, y0):
    """The 5x5 gauge dial: green, amber and red ticks round the top arc the needle sweeps (pivot (x0 + 2, y0 + 2))."""
    for y in range(5):
        for x in range(5):
            t.set(x0 + x, y0 + y, ENAMEL[6] if y < 3 else ENAMEL[4])
    for x, y in ((0, 0), (4, 0), (0, 4), (4, 4)):
        t.set(x0 + x, y0 + y, DARK[1])
    t.set(x0 + 0, y0 + 2, RADIUM[3])
    t.set(x0 + 0, y0 + 1, RADIUM[4])
    t.set(x0 + 1, y0 + 0, WARN[4])
    t.set(x0 + 2, y0 + 0, WARN[3])
    t.set(x0 + 3, y0 + 0, DANGER[4])
    t.set(x0 + 4, y0 + 1, DANGER[3])
    t.set(x0 + 4, y0 + 2, DANGER[2])
    t.set(x0 + 2, y0 + 2, DARK[2])
    t.set(x0 + 2, y0 + 4, ENAMEL[2])


def _sight_glass(t, x, y0, y1, seed):
    """A 2 px sight glass of electrolyte with rising bubbles, in a dark frame."""
    t.rect(x - 1, y0 - 1, x + 2, y1 + 1, DARK[0])
    for y in range(y0, y1 + 1):
        t.set(x, y, CYAN[3])
        t.set(x + 1, y, CYAN[2])
    for y in range(y0, y1 + 1):
        if (y + seed) % 3 == 0:
            t.set(x + (y // 3) % 2, y, CYAN[6])
    t.hline(x, x + 1, y0, CYAN[1])


def _port_disc(t, cx, cy, r, A, trim, out):
    """A round bolted flange seen from above or below: brass ring, accent throat, dark bore, four bolts."""
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            lit = dx + dy < -0.5
            if d <= r:
                t.set(x, y, trim[4] if lit else trim[2])
            if d <= r - 1.0:
                t.set(x, y, trim[1])
            if d <= r - 1.5:
                t.set(x, y, A[4] if lit else A[3])
            if d <= r - 2.5:
                t.set(x, y, DARK[1])
            if d <= r - 3.4:
                t.set(x, y, DARK[0])
    for a in (45, 135, 225, 315):
        bx = int(math.floor(cx + math.cos(math.radians(a)) * (r - 0.5)))
        by = int(math.floor(cy + math.sin(math.radians(a)) * (r - 0.5)))
        t.set(bx, by, trim[6])
    # a bright pip in the bore: on the rim for inputs, in the middle for outputs
    t.set(int(cx) - 1, int(cy) - 1, A[6] if not out else A[2])
    t.set(int(cx), int(cy), A[6] if out else DARK[0])


# ======================================================================================
# body faces
# ======================================================================================

def _front(P):
    B, s = P["body"], P["seed"]
    t = Tex(seed=s)
    brushed(t, DARK, base=1.2, amp=0.4, seed=s)
    _member(t, 0, 0, 15, 5, B, s + 1, outline="tlr")
    _member(t, 0, 14, 15, 15, B, s + 4, outline="lbr")
    if P["band"]:
        _band(t, 0, 4, 15, 5, P["band"])
        _band(t, 0, 14, 15, 15, P["band"])
    # rims of the gauge (u 2..6) and product-port (u 9..13) recesses; rows 0 and 5 show as half-pixel lips
    _bezel(t, 2, 1, 6, 4, BRASS)
    _bezel(t, 9, 1, 13, 4, DARK)
    bolt(t, 7, 2, B)
    t.hline(7, 8, 4, B[1])
    # the recess floors, painted where the open chamber hides them (the model maps them here)
    _dial(t, 3, 7)
    _flange5(t, 8, 7, PLASMA)
    return t


def _side(P):
    B, G, T, s = P["body"], P["plate"], P["trim"], P["seed"]
    t = Tex(seed=s + 10)
    brushed(t, DARK, base=1.2, amp=0.4, seed=s + 10)
    _member(t, 0, 0, 11, 5, B, s + 11, outline="tl")
    _member(t, 0, 14, 11, 15, B, s + 13, outline="lb")
    # gearbox plate flank: ribbed, bolted, full height
    _member(t, 12, 0, 15, 15, G, s + 14, base=3.4, outline="tbr", axis="v")
    if P["band"]:
        _band(t, 0, 4, 11, 5, P["band"])
        _band(t, 0, 14, 11, 15, P["band"])
        _sight_glass(t, 13, 3, 12, s)
    else:
        for y in (4, 7, 10):
            t.hline(13, 14, y, G[1])
            t.hline(13, 14, y + 1, G[5])
    bolt(t, 13, 1, T)
    bolt(t, 13, 13, T)
    bolt(t, 1, 1, B)
    if P["band"]:
        # the cell's electrode terminals: anode (red) and cathode (cyan) studs on an insulating plate
        t.rect(3, 1, 6, 3, DARK[1])
        t.hline(3, 6, 3, DARK[3])
        for x, A in ((3, DANGER), (5, CYAN)):
            t.set(x, 1, A[5])
            t.set(x + 1, 1, A[3])
            t.set(x, 2, A[3])
            t.set(x + 1, 2, A[1])
    else:
        # a louvred vent
        for y in (1, 3):
            t.hline(3, 6, y, B[1])
            t.hline(3, 6, y + 1, B[5])
    # the pocket for the input flange and the cascade manifold (u 8..11, rows 0..3; open at the top, closed behind by
    # the gearbox plate): its floor, a steel plate with the cyan flange (the manifold pipe covers u 8.5..11.5)
    _member(t, 8, 0, 11, 3, B, s + 15, base=2.6)
    for x, y in ((9, 0), (10, 0), (8, 1), (9, 1), (10, 1), (11, 1), (8, 2), (9, 2), (10, 2), (11, 2), (9, 3), (10, 3)):
        t.set(x, y, CYAN[5] if (x < 10 and y < 2) else CYAN[3])
    t.set(9, 1, CYAN[6])
    t.set(10, 2, DARK[0])
    t.vline(7, 0, 4, B[1])
    t.hline(8, 11, 4, B[5])
    return t


def _back(P):
    G, T, s = P["plate"], P["trim"], P["seed"]
    t = Tex(seed=s + 20)
    _member(t, 0, 0, 15, 15, G, s + 21, base=3.4, outline="tlbr")
    t.rect(2, 2, 13, 13, G[2])
    for y in range(2, 14, 3):
        t.hline(2, 13, y, G[3])
    # brass collar round the shaft hole (the hole is u 5..10, v 5..10)
    for y in range(3, 13):
        for x in range(3, 13):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d <= 5.2:
                t.set(x, y, BRASS[4] if x + y < 15 else BRASS[2])
            if d <= 4.4:
                t.set(x, y, BRASS[3] if x + y < 15 else BRASS[1])
    # hole floor: the bevel gear's teeth round the shaft
    t.rect(5, 5, 10, 10, DARK[0])
    for i in range(5, 11):
        if i % 2:
            t.set(i, 5, BRASS[2])
            t.set(i, 10, BRASS[1])
            t.set(5, i, BRASS[2])
            t.set(10, i, BRASS[1])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, T)
    if P["band"]:
        _band(t, 0, 0, 15, 1, P["band"])
        _band(t, 0, 14, 15, 15, P["band"])
    return t


def _top(P):
    B, G, s = P["body"], P["plate"], P["seed"]
    t = Tex(seed=s + 30)
    _member(t, 0, 0, 15, 11, B, s + 31, outline="tlr")
    _member(t, 0, 12, 15, 15, G, s + 32, base=3.4, outline="lbr")
    for x in range(2, 14, 3):
        t.vline(x, 13, 14, G[1])
    if P["band"]:
        t.hline(0, 15, 11, P["band"][0])
    bolt(t, 1, 1, B)
    bolt(t, 13, 1, B)
    # rims of the side pockets (x 0..2 and 14..16, z 8..12), open at the top
    for x0, x1, rim in ((0, 2, 2), (13, 15, 13)):
        t.hline(x0, x1, 7, B[1])
        t.vline(rim, 8, 11, B[1] if rim == 2 else B[5])
    _port_disc(t, 8.0, 6.0, 4.6, CYAN, BRASS, out=False)
    return t


def _bottom(P):
    B, G, s = P["body"], P["plate"], P["seed"]
    t = Tex(seed=s + 40)
    _member(t, 0, 0, 15, 15, G, s + 41, base=2.8, outline="tlbr")
    t.rect(2, 2, 13, 13, G[2])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, B)
    if P is CENTRIFUGE:
        _port_disc(t, 8.0, 8.0, 4.6, PLASMA, BRASS, out=True)
        return t
    # the electrolyzer's products are items: a square hatch
    t.rect(4, 4, 11, 11, G[4])
    t.hline(4, 11, 4, G[6])
    t.vline(4, 4, 11, G[6])
    t.hline(4, 11, 11, G[0])
    t.vline(11, 4, 11, G[0])
    t.rect(6, 6, 9, 9, DARK[1])
    t.hline(6, 9, 6, PLASMA[3])
    t.vline(6, 6, 9, PLASMA[3])
    t.hline(6, 9, 9, PLASMA[1])
    t.set(7, 8, PLASMA[6])
    t.set(8, 8, PLASMA[5])
    return t


def _chamber(P):
    """The deck under the rotor (and the head's underside): a bolted plate with a bearing ring round the axis."""
    T, s = P["trim"], P["seed"]
    t = Tex(seed=s + 50)
    brushed(t, DARK, base=2.6, amp=0.5, seed=s + 50)
    t.hline(0, 15, 0, DARK[5])
    t.hline(0, 15, 15, DARK[5])
    for x in (0, 15):
        t.vline(x, 1, 14, DARK[4])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, DARK)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d < 3.6:
                t.set(x, y, DARK[0])
            elif d <= 4.4:
                t.set(x, y, T[5] if (x + y) < 15 else T[3])
            elif d <= 5.1:
                t.set(x, y, T[2])
            elif d <= 5.7:
                t.set(x, y, DARK[1])
    return t


def _wall(P):
    """The gearbox plate's inner face behind the rotor: a ribbed cover with bolts down its edges."""
    G, T, s = P["plate"], P["trim"], P["seed"]
    t = Tex(seed=s + 70)
    brushed(t, G, base=1.8, amp=0.5, seed=s + 70, axis="v")
    for x in range(1, 16, 3):
        t.vline(x, 6, 13, G[3])
        t.vline(x + 1, 6, 13, G[1])
    t.hline(0, 15, 6, G[0])
    t.hline(0, 15, 13, G[4])
    for y in (7, 11):
        bolt(t, 0, y, T)
        bolt(t, 14, y, T)
    return t


# ======================================================================================
# rotors
# ======================================================================================

def _drum_shade(t, x0, x1, ramp, base):
    """One quarter of the drum (u x0..x1, 7 wide): darker shoulders, a polished middle with a highlight left of centre."""
    prof = (-1.4, -0.4, 0.4, 0.9, 0.5, -0.2, -1.2)
    for i, x in enumerate(range(x0, x1)):
        for y in range(16):
            t.set(x, y, ramp[int(round(base + prof[i]))])


def _rotor_centrifuge():
    t = Tex(seed=CENTRIFUGE["seed"] + 60)
    _drum_shade(t, 0, 7, STEEL, 5.0)
    _drum_shade(t, 7, 14, STEEL, 5.1)
    _drum_shade(t, 9, 16, STEEL, 4.8)
    # north quarter: a bold hazard stripe that strobes past the opening as the rotor turns
    for y in range(16):
        t.set(1, y, DARK[1])
        t.set(2, y, WARN[4])
        t.set(3, y, WARN[5])
        t.set(4, y, WARN[3])
        t.set(5, y, DARK[1])
    # south quarter: a dark band
    for y in range(16):
        t.set(13, y, DARK[2])
        t.set(14, y, DARK[3])
    # a groove round the drum, then the brass hub collars (row 6 = y 9..10, row 13 = y 2..3)
    for x in range(16):
        t.set(x, 9, shade(t.get(x, 9), 0.7))
        t.set(x, 10, shade(t.get(x, 10), 1.08))
    for y, ramp_i in ((6, 5), (13, 3)):
        for x in range(16):
            t.set(x, y, BRASS[ramp_i])
        for x in range(1, 16, 4):
            t.set(x, y, BRASS[ramp_i - 3])
    for x in range(16):
        t.set(x, 7, shade(t.get(x, 7), 0.8))
        t.set(x, 12, shade(t.get(x, 12), 0.85))
    return t


def _rotor_electrolyzer():
    t = Tex(seed=ELECTROLYZER["seed"] + 60)
    # armature: copper windings in horizontal turns, dark slots between the coil groups, a brass commutator below
    for x0, base in ((0, 4.6), (7, 4.4), (9, 4.2)):
        _drum_shade(t, x0, x0 + 7, COPPER, base)
    for y in range(16):
        if y % 2:
            for x in range(16):
                t.set(x, y, shade(t.get(x, y), 0.8))
    for x in (0, 6, 7, 13):
        for y in range(16):
            t.set(x, y, DARK[1])
    # the marked coil: wrapped in cream insulating tape
    for y in range(16):
        t.set(2, y, TAPE[6] if y % 2 == 0 else TAPE[5])
        t.set(3, y, TAPE[5] if y % 2 == 0 else TAPE[4])
        t.set(4, y, TAPE[3])
    for y, ramp_i in ((6, 5), (13, 3)):
        for x in range(16):
            t.set(x, y, BRASS[ramp_i] if x % 2 else BRASS[ramp_i - 2])
    return t


# ======================================================================================
# shared parts
# ======================================================================================

def _inner():
    t = Tex(seed=1592)
    brushed(t, DARK, base=2.0, amp=0.5, seed=1592, axis="v")
    return t


def _collar():
    t = Tex(seed=1593)
    brushed(t, BRASS, base=3.0, amp=0.6, seed=1593)
    for x in range(0, 16, 2):
        t.vline(x, 0, 15, BRASS[1])
    return t


def _needle():
    t = Tex(seed=1590)
    t.rect(0, 0, 15, 15, DARK[1])
    for y in range(0, 12):
        t.set(7, y, DANGER[5])
        t.set(8, y, DANGER[3])
    t.set(7, 1, DANGER[6])
    t.rect(6, 12, 9, 15, BRASS[3])
    t.hline(6, 9, 12, BRASS[5])
    t.set(7, 13, BRASS[6])
    return t


def _manifold():
    t = Tex(seed=1591)
    t.rect(0, 0, 15, 15, STEEL[3])
    # pipe (v 0..2 across the pipe, u along it): rolled steel with a cyan service band next to the flange
    for x in range(16):
        for y, c in enumerate((STEEL[5], STEEL[4], STEEL[2])):
            t.set(x, y, c)
    t.vline(1, 0, 2, CYAN[4])
    t.set(1, 2, CYAN[2])
    # flange face (u 0..3, v 6..9): a brass ring with four dark bolts
    t.rect(0, 6, 3, 9, BRASS[4])
    t.hline(0, 3, 6, BRASS[5])
    t.vline(0, 6, 9, BRASS[5])
    t.hline(0, 3, 9, BRASS[2])
    t.vline(3, 6, 9, BRASS[2])
    for x, y in ((0, 6), (3, 6), (0, 9), (3, 9)):
        t.set(x, y, BRASS[0])
    # rims: the flange's edge seen from the side (u 6) and from above (u 8)
    t.vline(6, 6, 9, BRASS[3])
    t.vline(8, 6, 9, BRASS[5])
    return t


# ======================================================================================
# registry
# ======================================================================================

for _name, _P in (("centrifuge", CENTRIFUGE), ("electrolyzer", ELECTROLYZER)):
    for _part, _fn in (("front", _front), ("side", _side), ("back", _back), ("top", _top), ("bottom", _bottom),
                       ("chamber", _chamber), ("wall", _wall)):
        block("%s_%s" % (_name, _part))(lambda P=_P, fn=_fn: fn(P))

block("centrifuge_rotor")(_rotor_centrifuge)
block("electrolyzer_rotor")(_rotor_electrolyzer)
block("centrifuge_inner")(_inner)
block("centrifuge_collar")(_collar)
block("centrifuge_needle")(_needle)
block("centrifuge_manifold")(_manifold)

"""Textures of the conventional warhead charges and of what a blast does to the senses: plastic explosive, the thermobaric
charge, the cluster bomblet, and the flash-blinded and deafened effect icons.

This module is imported at the end of ``blocks.py``. The cluster bomblet's texture doubles as the UV sheet of its 3D item
model (``models/item/cluster_bomblet.json``): the body runs along x 4..11, rows 5..10 (lit top, shaded bottom), the yellow band
at x 9..10, the nose at x 12..13 and the fuze tip at x 14, the tail cap at x 3 and the ribbon at x 0..2. Keep those regions
where they are if the art changes.
"""

import math

from armor import EFFECTS
from items import item, sculpt
from palette import CARBONATE, DANGER, DARK, LEAD, STEEL, WARN, WHITE
from pixelkit import Ramp, Tex, item_outline

# Olive drab, as munitions are painted; the charge's film a touch glossier and greener.
OLIVE_DRAB = Ramp("#1c2116", "#283020", "#36402b", "#465237", "#586646", "#6f7d58", "#8f9c74")
FILM = Ramp("#1e2418", "#2b3322", "#3a442d", "#4b583b", "#606e4c", "#7c8a63", "#a2ad85")
# Plastic explosive itself: an off-white putty.
PUTTY = Ramp("#5e5848", "#7f7863", "#a29a81", "#bdb59b", "#d2cbb2", "#e2dcc6", "#f1edde")
LABEL = Ramp("#4d4a3c", "#6b6753", "#8d886f", "#aea88c", "#c9c3a6", "#dcd7bd", "#ece8d4")


@item("plastic_explosive")
def plastic_explosive():
    """A block of plastic explosive: an oblong bar wrapped in glossy olive film with a printed label round it, its cut end
    showing the off-white putty."""
    t = Tex(16, 16, 3600)
    front = {(x, y) for x in range(1, 12) for y in range(6, 13)}
    top = {(x + d, 6 - d) for x in range(1, 12) for d in range(1, 4)}
    end = {(11 + d, y - d) for y in range(6, 13) for d in range(1, 4)}
    sculpt(t, front, FILM, base=3.2, seed=3600, grain=0.35)
    sculpt(t, top, FILM, base=4.6, seed=3601, grain=0.3)
    sculpt(t, end, PUTTY, base=3.6, seed=3602, grain=0.6)
    # The film folded over the cut end's rim.
    for d in range(1, 4):
        t.set(11 + d, 12 - d, FILM[2])
        t.set(11 + d, 6 - d, FILM[4])
    for y in range(5, 12):
        t.set(12, y, FILM[3] if y < 11 else FILM[2])
    # Gloss on the film's top, catching the light in a streak.
    for x, y in ((5, 4), (6, 4), (7, 4), (9, 3), (10, 3)):
        t.set(x, y, FILM[6])
    # The printed label: a pale band with lines of dark lettering.
    for x in range(1, 12):
        t.set(x, 8, LABEL[5])
        t.set(x, 9, LABEL[4])
        t.set(x, 10, LABEL[3])
    for x in (2, 3, 4, 6, 7, 9):
        t.set(x, 9, DARK[1])
    for x in (2, 3, 5, 6, 7, 8):
        t.set(x, 10, LABEL[1])
    # A crease in the film across the front.
    t.set(3, 12, FILM[1])
    t.set(4, 11, FILM[1])
    item_outline(t, FILM[0])
    return t


# The canister's columns, left to right: how far up the steel ramp each is lit (a cylinder lit from the upper left).
_CAN = (3, 5, 6, 5, 4, 3, 2, 1)


@item("thermobaric_charge")
def thermobaric_charge():
    """A thermobaric charge: a steel canister of fuel and metal powder, rolled beads at top and bottom, a yellow and black
    hazard band round its waist and the burster's fuze standing up out of its lid."""
    t = Tex(16, 16, 3610)
    for i, k in enumerate(_CAN):
        x = 4 + i
        for y in range(3, 15):
            t.set(x, y, STEEL[k])
        # The lid's rim, the rolled bead under it and the one at the foot.
        t.set(x, 2, STEEL[min(6, k + 1)])
        t.set(x, 3, STEEL[max(1, k - 2)])
        t.set(x, 4, STEEL[min(6, k + 1)])
        t.set(x, 12, STEEL[max(1, k - 2)])
        t.set(x, 13, STEEL[min(6, k + 1)])
        t.set(x, 14, STEEL[max(1, k - 1)])
        # The hazard band: diagonal stripes, the yellow shaded round the can.
        for y in (8, 9, 10):
            t.set(x, y, DARK[1] if (x + y) % 4 < 2 else WARN[min(6, k + 1)])
    # The lid, seen a little from above.
    for x, k in zip(range(5, 11), (5, 6, 6, 5, 4, 3)):
        t.set(x, 1, STEEL[k])
    # The fuze: a short dark boss in the middle of the lid.
    t.set(7, 0, DARK[4])
    t.set(8, 0, DARK[3])
    t.set(7, 1, DARK[3])
    t.set(8, 1, DARK[1])
    # A stencilled fill port and marking.
    t.set(9, 6, DARK[1])
    t.set(10, 6, STEEL[3])
    for x in (5, 6, 7):
        t.set(x, 6, STEEL[6] if x != 6 else STEEL[4])
    item_outline(t, STEEL[0])
    return t


@item("cluster_bomblet")
def cluster_bomblet():
    """A cluster bomblet lying on its side, nose to the right: an olive drab cylinder with a yellow band, a steel nose with
    its fuze, and the fabric ribbon that steadies it as it falls trailing from its tail. Also its 3D model's UV sheet."""
    t = Tex(16, 16, 3620)
    # Body, rows 5..10: shaded as a cylinder lit from above.
    rows = {5: 6, 6: 5, 7: 4, 8: 3, 9: 2, 10: 1}
    for y, k in rows.items():
        for x in range(4, 12):
            c = OLIVE_DRAB[k]
            if (x * 5 + y * 3) % 7 == 0 and 2 <= k <= 5:
                c = OLIVE_DRAB[k - 1]
            t.set(x, y, c)
        # The yellow band near the nose.
        t.set(9, y, WARN[min(6, k)])
        t.set(10, y, WARN[max(1, k - 1)])
    # Nose: a steel cone with the fuze at its tip.
    for y, k in ((6, 6), (7, 5), (8, 3), (9, 2)):
        t.set(12, y, STEEL[k])
        t.set(13, y, STEEL[max(1, k - 1)])
    t.set(14, 7, DARK[4])
    t.set(14, 8, DARK[2])
    # Tail cap, where the ribbon is tied on.
    for y, k in ((6, 4), (7, 3), (8, 2), (9, 1)):
        t.set(3, y, DARK[k])
    # The ribbon, off-white fabric streaming back and fluttering.
    for x, y, k in ((2, 7, 5), (2, 8, 4), (1, 6, 6), (1, 7, 4), (0, 8, 5), (0, 9, 3), (1, 9, 2), (2, 6, 3)):
        t.set(x, y, CARBONATE[k])
    item_outline(t, OLIVE_DRAB[0])
    return t


# ======================================================================================
# effect icons, 18x18, in the style of armor.py's
# ======================================================================================

_EYE = [
    "...KKKKKK...",
    ".KKWWWWWWKK.",
    "KWWWWGGGWWWK",
    "KWWWGPPGWWWK",
    "KWWWGGGGWWWK",
    ".KKWWWWWWKK.",
    "...KKKKKK...",
]


def flash_blinded():
    """An eye, its pupil shrunk to a point, in front of a starburst."""
    t = Tex(18, 18, 3630)
    t.disc(9, 9, 8.5, DARK[1])
    t.disc(9, 9, 7.5, DARK[3])
    for i in range(8):
        a = math.pi * i / 4.0
        length = 7.4 if i % 2 == 0 else 6.2
        for s in range(40):
            r = s * length / 40.0
            t.set(int(9.0 + math.cos(a) * r), int(9.0 + math.sin(a) * r), WARN[6] if r < 5.0 else WARN[4])
    colours = {"K": DARK[0], "W": WHITE, "G": LEAD[4], "P": DARK[0]}
    for y, row in enumerate(_EYE):
        for x, ch in enumerate(row):
            if ch in colours:
                t.set(3 + x, 6 + y, colours[ch])
    t.set(8, 8, WHITE)
    return t


_EAR = [
    "..HHHH..",
    ".HhhhhH.",
    "Hh....hH",
    "H..hh..H",
    "H.h..h.H",
    "...h.h.H",
    "....h.hH",
    "...h..H.",
    "...h.hH.",
    "..H.hH..",
    "..HHH...",
]


def deafened():
    """An ear, the sound waves beside it struck through."""
    t = Tex(18, 18, 3631)
    t.disc(9, 9, 8.5, DARK[1])
    t.disc(9, 9, 7.5, LEAD[2])
    colours = {"H": CARBONATE[5], "h": CARBONATE[3]}
    for y, row in enumerate(_EAR):
        for x, ch in enumerate(row):
            if ch in colours:
                t.set(3 + x, 3 + y, colours[ch])
    # Sound waves, struck through.
    for r, c in ((1.8, LEAD[6]), (3.6, LEAD[5]), (5.4, LEAD[5])):
        for s in range(30):
            a = -0.75 + 1.5 * s / 29.0
            t.set(int(9.5 + math.cos(a) * r), int(9.0 + math.sin(a) * r), c)
    for i in range(9):
        t.set(15 - i, 4 + i, DANGER[5])
        t.set(15 - i, 5 + i, DANGER[2])
    return t


EFFECTS["flash_blinded"] = flash_blinded
EFFECTS["deafened"] = deafened

"""Armor layer and mob effect textures.

Armor layers follow the vanilla 64x32 humanoid UV layout: layer 1 is drawn for helmet,
chestplate and boots, layer 2 for leggings.
"""

from palette import DARK, LEAD, RADIUM, URANIUM, WARN
from pixelkit import Tex, fbm
from common import trefoil

ARMOR = {}
EFFECTS = {}

# (x0, y0, x1, y1) inclusive boxes of the humanoid unwrap
HEAD = (0, 0, 31, 15)
HAT = (32, 0, 63, 15)
BODY = (16, 16, 39, 31)
ARMS = (40, 16, 55, 31)
LEGS = (0, 16, 15, 31)


def _rubber(t, box, seed):
    x0, y0, x1, y1 = box
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            # leave the unused corners of each unwrap transparent
            n = fbm(x, y, seed, 64, 32, 2, 5.0)
            t.set(x, y, WARN[3 if n < 0.45 else 4 if n < 0.75 else 5])


def _seams(t, box):
    x0, y0, x1, y1 = box
    for x in range(x0, x1 + 1, 4):
        for y in range(y0, y1 + 1):
            if (y - y0) % 4 == 3:
                t.set(x, y, WARN[1])


def _clear_unused(t, box, face):
    """Blank the 8x4-ish corner cells a cuboid unwrap does not use."""
    x0, y0, w, d, h = face
    # top-left and top-right corners of a cuboid unwrap
    for y in range(y0, y0 + d):
        for x in range(x0, x0 + d):
            t.im.putpixel((x, y), (0, 0, 0, 0))
        for x in range(x0 + d + 2 * w, x0 + 2 * d + 2 * w):
            t.im.putpixel((x, y), (0, 0, 0, 0))


def _layer(seed, legs_only=False):
    t = Tex(64, 32, seed)
    if legs_only:
        _rubber(t, BODY, seed)
        _rubber(t, LEGS, seed + 1)
        _clear_unused(t, BODY, (16, 16, 8, 4, 12))
        _clear_unused(t, LEGS, (0, 16, 4, 4, 12))
        # belt
        for x in range(20, 36):
            t.set(x, 30, DARK[2])
            t.set(x, 31, DARK[1])
        return t
    # The hat overlay is left empty so it does not cover the visor.
    for box in (HEAD, BODY, ARMS, LEGS):
        _rubber(t, box, seed)
        _seams(t, box)
    _clear_unused(t, HEAD, (0, 0, 8, 8, 8))
    _clear_unused(t, BODY, (16, 16, 8, 4, 12))
    _clear_unused(t, ARMS, (40, 16, 4, 4, 12))
    _clear_unused(t, LEGS, (0, 16, 4, 4, 12))
    # visor on the face, respirator filters below it
    t.rect(9, 9, 14, 12, DARK[1])
    t.rect(10, 10, 13, 11, (90, 140, 150, 255))
    t.set(10, 10, (200, 230, 230, 255))
    t.rect(10, 14, 13, 15, DARK[3])
    # trefoil badge on the chest
    badge = Tex(8, 8, seed)
    trefoil(badge, 4, 4, 3.6, DARK[0])
    for y in range(8):
        for x in range(8):
            if badge.im.getpixel((x, y))[3]:
                t.set(20 + x, 21 + y, DARK[0])
    # boots: dark soles at the bottom of the legs
    for x in range(0, 16):
        for y in (29, 30, 31):
            t.set(x, y, DARK[1 if y == 31 else 2])
    # gloves
    for x in range(40, 56):
        for y in (29, 30, 31):
            t.set(x, y, DARK[2])
    return t


def _plates(t, box, seed):
    """Dull depleted-uranium plate in 4x4 panels with dark seams and a faint green oxide."""
    x0, y0, x1, y1 = box
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, 64, 32, 2, 4.0)
            t.set(x, y, LEAD[2 if n < 0.35 else 3 if n < 0.7 else 4])
            if (x - x0) % 4 == 0 or (y - y0) % 4 == 0:
                t.set(x, y, LEAD[1])
            elif n > 0.85:
                t.set(x, y, URANIUM[1])


def _du_layer(seed, legs_only=False):
    t = Tex(64, 32, seed)
    if legs_only:
        _plates(t, BODY, seed)
        _plates(t, LEGS, seed + 1)
        _clear_unused(t, BODY, (16, 16, 8, 4, 12))
        _clear_unused(t, LEGS, (0, 16, 4, 4, 12))
        for x in range(20, 36):
            t.set(x, 30, LEAD[0])
            t.set(x, 31, DARK[0])
        return t
    for box in (HEAD, BODY, ARMS, LEGS):
        _plates(t, box, seed)
    _clear_unused(t, HEAD, (0, 0, 8, 8, 8))
    _clear_unused(t, BODY, (16, 16, 8, 4, 12))
    _clear_unused(t, ARMS, (40, 16, 4, 4, 12))
    _clear_unused(t, LEGS, (0, 16, 4, 4, 12))
    # narrow eye slit on the face
    t.rect(9, 11, 14, 11, DARK[0])
    # yellow trefoil stencil on the chest
    badge = Tex(8, 8, seed)
    trefoil(badge, 4, 4, 3.6, WARN[5])
    for y in range(8):
        for x in range(8):
            if badge.im.getpixel((x, y))[3]:
                t.set(20 + x, 21 + y, WARN[5])
    for x in range(0, 16):
        for y in (30, 31):
            t.set(x, y, DARK[1])
    return t


ARMOR["hazmat_layer_1"] = lambda: _layer(450)
ARMOR["hazmat_layer_2"] = lambda: _layer(451, legs_only=True)
ARMOR["depleted_uranium_layer_1"] = lambda: _du_layer(460)
ARMOR["depleted_uranium_layer_2"] = lambda: _du_layer(461, legs_only=True)


def radiation_sickness():
    t = Tex(18, 18, 452)
    t.disc(9, 9, 8.5, DARK[1])
    t.disc(9, 9, 7.5, RADIUM[4])
    trefoil(t, 9, 9, 6.6, DARK[0])
    return t


EFFECTS["radiation_sickness"] = radiation_sickness

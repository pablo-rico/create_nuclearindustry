"""Textures for the debug energy blocks: the creative RF generator.

Register block textures with ``@block``; this module is imported at the end of ``blocks.py``.

Creative-only blocks borrow Create's creative look - a magenta and black checker - so nobody takes
them for survival machinery. The generator is a checkered cage (a cutout texture) round a
full-bright energy core drawn by its own element.
"""

import math

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from pixelkit import Ramp, Tex, fbm, mix

CLEAR = (0, 0, 0, 0)
MAGENTA = Ramp("#2a0a2e", "#4d1352", "#7a1f82", "#a62db0", "#cf45d6", "#ec7cf0", "#ffc4ff")
VOID = Ramp("#050307", "#0e0912", "#17101c", "#221828", "#2e2236", "#3b2d44", "#4a3a55")

CUTOUT_BLOCKS["creative_rf_generator_frame"] = (16, 16)


@block("creative_rf_generator_frame")
def creative_frame():
    """A 3 px checkered frame with corner terminals; the 10x10 window in the middle is clear."""
    t = Tex(seed=1500)
    t.rect(0, 0, 15, 15, CLEAR)
    for y in range(16):
        for x in range(16):
            if 3 <= x <= 12 and 3 <= y <= 12:
                continue
            on = ((x // 2) + (y // 2)) % 2 == 0
            t.set(x, y, MAGENTA[4] if on else VOID[2])
    for i in range(16):
        t.set(i, 0, MAGENTA[6])
        t.set(0, i, MAGENTA[6])
        t.set(i, 15, MAGENTA[1])
        t.set(15, i, MAGENTA[1])
    # Inner lip of the window.
    for i in range(3, 13):
        t.set(i, 2, VOID[0])
        t.set(2, i, VOID[0])
        t.set(i, 13, MAGENTA[5])
        t.set(13, i, MAGENTA[5])
    # Copper-gold FE terminals in the corners.
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        t.rect(x, y, x + 1, y + 1, (232, 189, 108, 255))
        t.set(x + 1, y + 1, (134, 82, 53, 255))
    return t


@block("creative_rf_generator_core")
def creative_core():
    """The energy core: white-hot centre, magenta plasma and forking arcs, shown full-bright."""
    t = Tex(seed=1501)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8.0
            n = fbm(x, y, 1501, 16, 16, 3, 4.0)
            v = max(0.0, 1.0 - d) * 0.9 + (n - 0.5) * 0.5
            t.set(x, y, mix(MAGENTA[3], (255, 255, 255, 255), max(0.0, min(1.0, v))))
    # Arcs: four jagged lines from the core to the edges.
    for a0 in (0.4, 2.0, 3.6, 5.1):
        x, y = 8.0, 8.0
        for i in range(9):
            a = a0 + math.sin(i * 1.7 + a0) * 0.5
            x += math.cos(a)
            y += math.sin(a)
            t.set(int(x), int(y), MAGENTA[6])
    t.rect(7, 7, 8, 8, (255, 255, 255, 255))
    return t

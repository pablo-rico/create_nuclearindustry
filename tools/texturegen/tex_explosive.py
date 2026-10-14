"""Textures of what a nuclear detonation leaves: trinitite and charred logs.

Register block textures with ``@block``; this module is imported at the end of ``blocks.py``.
"""

from blocks import block  # noqa: F401
from pixelkit import Ramp, Tex, fbm, mix

# Green glass fused from sand, dark streaks and bubbles; charred wood, black with grey ash and embers.
TRINITITE = Ramp("#151a12", "#1f281b", "#2b3825", "#3a4a31", "#4f6141", "#6e8059", "#98a87c")
CHAR = Ramp("#0d0b0a", "#171311", "#221c18", "#2e2620", "#3d342c", "#55493e", "#706356")
EMBER = (0xc8, 0x5a, 0x24, 255)


@block("trinitite")
def trinitite():
    t = Tex(seed=210)
    for x in range(16):
        for y in range(16):
            n = fbm(x, y, 210, 16, 16, octaves=3, scale=4.0)
            t.set(x, y, TRINITITE.t(0.1 + 0.55 * n))
    # Glassy streaks of melt, and gas bubbles caught in it.
    for x, y in ((2, 3), (3, 3), (4, 4), (9, 10), (10, 10), (11, 11), (12, 11), (6, 13), (7, 13)):
        t.set(x, y, TRINITITE[5])
    for x, y in ((5, 7), (12, 4), (3, 11), (13, 14), (9, 2)):
        t.set(x, y, TRINITITE[0])
        t.set(x + 1, y, TRINITITE[1])
    for x, y in ((6, 6), (13, 3), (10, 9)):
        t.set(x, y, TRINITITE[6])
    return t


@block("charred_log")
def charred_log():
    t = Tex(seed=211)
    for x in range(16):
        for y in range(16):
            n = fbm(x, y, 211, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, CHAR.t(0.1 + 0.45 * n))
    # Cracked into blocks, as burnt wood splits along and across the grain.
    for x in (2, 6, 11, 14):
        for y in range(16):
            t.set(x, y, CHAR[0])
    for x0, x1, y in ((3, 5, 4), (7, 10, 9), (12, 13, 2), (0, 1, 12), (7, 10, 14)):
        t.hline(x0, x1, y, CHAR[0])
    for x, y in ((4, 2), (9, 6), (13, 11), (1, 8)):
        t.set(x, y, CHAR[5])
    for x, y in ((8, 11), (4, 13)):
        t.set(x, y, mix(CHAR[2], EMBER, 0.5))
    return t


@block("charred_log_top")
def charred_log_top():
    t = Tex(seed=212)
    t.rect(0, 0, 15, 15, CHAR[1])
    for r, c in ((7, CHAR[2]), (5, CHAR[3]), (3, CHAR[2]), (1, CHAR[4])):
        for x in range(16):
            for y in range(16):
                if abs(((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 - r) < 0.6:
                    t.set(x, y, c)
    t.set(7, 7, CHAR[5])
    t.set(8, 8, mix(CHAR[3], EMBER, 0.4))
    for x in range(16):
        t.set(x, 0, CHAR[0])
        t.set(x, 15, CHAR[0])
        t.set(0, x, CHAR[0])
        t.set(15, x, CHAR[0])
    return t

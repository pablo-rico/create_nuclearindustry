"""Textures of nuclear weapons and what their detonations leave: the thermonuclear bomb and its cannon
shell, the parts warheads are assembled from, trinitite and charred logs.

Register block textures with ``@block`` and item textures with ``@item``; this module is imported at the
end of ``blocks.py``.
"""

import math

from blocks import block  # noqa: F401
from common import trefoil
from items import _ingot, capsule_pts, item, mask_from, rect_pts, sculpt
from palette import BRASS, COPPER, CYAN, DANGER, DARK, LEAD, LITHIUM, PLASMA, RADIUM, STEEL, TRITIUM, URANIUM, WARN, WHITE
from pixelkit import Ramp, Tex, bevel, brushed, fbm, glow, hazard, item_outline, mix, outline, rivet, shade

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


# ======================================================================================
# the thermonuclear bomb: blued gunmetal, a yellow and black band, a radiation case
# ======================================================================================

GUNMETAL = Ramp("#15181c", "#20252b", "#2c333a", "#3b434b", "#4d565e", "#636d74", "#808a90")
# Composition B, the buff explosive of the lenses, and its slow inner cone.
EXPLOSIVE = Ramp("#4a3a22", "#6a5532", "#8a7044", "#a68a58", "#bfa36e", "#d4bb88", "#e8d5a8")
# Lithium deuteride: a pressed grey-white salt with a blue cast.
DEUTERIDE = LITHIUM.tinted(CYAN[5], 0.25)


@block("thermonuclear_bomb_body")
def thermonuclear_bomb_body():
    t = Tex(seed=230)
    brushed(t, GUNMETAL, base=3.2, amp=0.7, seed=230)
    for x in range(16):
        f = 0.75 + 0.5 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    # The waist between primary and secondary, banded yellow and black.
    hazard(t, 0, 7, 15, 9, WARN[5], DARK[0], period=6, seed=230)
    t.hline(0, 15, 6, DARK[0])
    t.hline(0, 15, 10, DARK[0])
    for y in (2, 13):
        t.hline(0, 15, y, GUNMETAL[1])
        t.hline(0, 15, y + 1, GUNMETAL[5])
    for x in (2, 13):
        rivet(t, x, 4, GUNMETAL)
        rivet(t, x, 11, GUNMETAL)
    return t


@block("thermonuclear_bomb_panel")
def thermonuclear_bomb_panel():
    t = Tex(seed=231)
    brushed(t, GUNMETAL, base=3.2, amp=0.6, seed=231)
    bevel(t, 0, 0, 15, 15, GUNMETAL)
    outline(t, 0, 0, 15, 15, GUNMETAL)
    t.rect(2, 2, 13, 13, WARN[4])
    bevel(t, 2, 2, 13, 13, WARN)
    outline(t, 2, 2, 13, 13, DARK)
    trefoil(t, 8, 8, 5.4, DARK[0])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(t, x, y, GUNMETAL)
    return t


@block("thermonuclear_bomb_top")
def thermonuclear_bomb_top():
    t = Tex(seed=232)
    brushed(t, GUNMETAL, base=3.2, amp=0.6, seed=232)
    bevel(t, 0, 0, 15, 15, GUNMETAL)
    outline(t, 0, 0, 15, 15, GUNMETAL)
    t.disc(8, 8, 6.0, GUNMETAL[4])
    t.ring(8, 8, 6.0, 5.2, GUNMETAL[6])
    t.ring(8, 8, 6.3, 5.9, DARK[0])
    # The arming plug: a yellow ring round a black one round the red arming light.
    t.disc(8, 8, 4.6, WARN[4])
    t.ring(8, 8, 4.6, 3.9, WARN[6])
    t.disc(8, 8, 3.0, DARK[0])
    t.disc(8, 8, 1.6, DANGER[4])
    t.set(7, 7, DANGER[6])
    for a in range(0, 360, 45):
        r = math.radians(a)
        t.set(round(8 + math.cos(r) * 5.4 - 0.5), round(8 + math.sin(r) * 5.4 - 0.5), GUNMETAL[6])
    glow(t, 8, 8, 3.0, DANGER[6], 0.2)
    return t


@block("projectile/thermonuclear_cannon_shell_side")
def thermonuclear_shell_side():
    t = Tex(seed=233)
    brushed(t, GUNMETAL, base=3.4, amp=0.6, seed=233, axis="v")
    for x in range(16):
        f = 0.7 + 0.6 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    # brass driving bands round a yellow and black band
    for y in (3, 11):
        t.hline(0, 15, y, BRASS[2])
        t.hline(0, 15, y + 1, BRASS[5])
    hazard(t, 0, 6, 15, 8, WARN[5], DARK[1], period=6, seed=233)
    t.hline(0, 15, 6, DARK[0])
    t.hline(0, 15, 8, DARK[0])
    return t


@block("projectile/thermonuclear_cannon_shell_top")
def thermonuclear_shell_top():
    t = Tex(seed=234)
    brushed(t, GUNMETAL, base=3.0, amp=0.5, seed=234)
    t.disc(8, 8, 7.0, GUNMETAL[4])
    t.ring(8, 8, 7.0, 6.0, GUNMETAL[6])
    t.ring(8, 8, 7.4, 6.9, GUNMETAL[0])
    t.disc(8, 8, 4.4, WARN[4])
    t.ring(8, 8, 4.4, 3.6, WARN[6])
    t.disc(8, 8, 2.6, DARK[0])
    t.disc(8, 8, 1.2, DANGER[5])
    glow(t, 6, 6, 5.0, GUNMETAL[6], 0.15)
    return t


@block("projectile/thermonuclear_cannon_shell_bottom")
def thermonuclear_shell_bottom():
    t = Tex(seed=235)
    brushed(t, GUNMETAL, base=2.6, amp=0.5, seed=235)
    t.disc(8, 8, 7.0, GUNMETAL[3])
    t.ring(8, 8, 7.0, 6.0, GUNMETAL[5])
    t.ring(8, 8, 7.4, 6.9, GUNMETAL[0])
    t.disc(8, 8, 4.0, DARK[1])
    for a in range(0, 360, 45):
        r = math.radians(a)
        t.line(8 + math.cos(r) * 1.4, 8 + math.sin(r) * 1.4, 8 + math.cos(r) * 3.6, 8 + math.sin(r) * 3.6, DARK[4])
    t.disc(8, 8, 1.2, CYAN[5])
    return t


# ======================================================================================
# warhead parts
# ======================================================================================

@item("lithium_deuteride")
def lithium_deuteride():
    # A pressed ingot of the salt, chalky rather than metallic.
    t = _ingot(240, DEUTERIDE)
    for (x, y) in ((5, 8), (9, 7), (11, 9), (7, 10)):
        if t.im.getpixel((x, y))[3]:
            t.set(x, y, DEUTERIDE[3])
    return t


LENS = [
    "................",
    "................",
    "......####......",
    "....########....",
    "...##########...",
    "..############..",
    "..############..",
    "..############..",
    "...##########...",
    "...##########...",
    "....########....",
    ".....######.....",
    "......####......",
    "................",
    "................",
    "................",
]


def _lens(seed, fill=1.0):
    """An explosive lens: a buff block of fast explosive round the dark cone of the slow one."""
    t = Tex(16, 16, seed)
    pts = mask_from(LENS)
    sculpt(t, pts, EXPLOSIVE, base=3.8, seed=seed, grain=0.6)
    for (x, y) in sorted(pts):
        # The slow explosive's cone, pointing down at the core.
        if abs(x + 0.5 - 8) <= (y - 3) * 0.45 and y >= 4 and y <= 10:
            t.set(x, y, mix(t.get(x, y), DARK[1], 0.55))
        if y > 2 + 9 * fill:
            t.set(x, y, LEAD[1] if (x + y) % 2 else LEAD[2])
    # A detonator well at the top, its wire trailing off.
    t.set(7, 3, COPPER[4])
    t.set(8, 3, COPPER[2])
    t.set(8, 2, DANGER[4])
    item_outline(t, EXPLOSIVE[0])
    return t


@item("explosive_lens")
def explosive_lens():
    return _lens(241)


@item("incomplete_explosive_lens")
def incomplete_explosive_lens():
    # Only part of the charge poured into the sheet mould.
    return _lens(242, fill=0.45)


def _sphere(t, cx, cy, r, ramp, light=(-2.0, -2.0)):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                lit = 1.0 - math.hypot(x + 0.5 - (cx + light[0]), y + 0.5 - (cy + light[1])) / (r * 1.6)
                t.set(x, y, ramp[max(1, min(6, int(round(1 + lit * 5.5))))])


@item("neutron_initiator")
def neutron_initiator():
    # A gold-plated beryllium ball, grooved inside, with its plutonium source glowing in the seam.
    t = Tex(16, 16, 243)
    _sphere(t, 8, 8, 4.6, BRASS)
    t.ring(8, 8, 4.8, 4.0, BRASS[1])
    for a in range(0, 360, 60):
        r = math.radians(a)
        t.line(8 + math.cos(r) * 1.4, 8 + math.sin(r) * 1.4, 8 + math.cos(r) * 3.6, 8 + math.sin(r) * 3.6, BRASS[2])
    t.disc(8, 8, 1.1, PLASMA[5])
    glow(t, 8, 8, 6.0, PLASMA[5], 0.25)
    item_outline(t, BRASS[0])
    return t


def _firing_set(seed, built=1.0):
    """A steel box of capacitors and a krytron tube, its leads running out to the detonators."""
    t = Tex(16, 16, seed)
    pts = rect_pts(2, 4, 13, 12)
    sculpt(t, pts, STEEL, base=3.8, seed=seed, grain=0.4)
    bevel(t, 2, 4, 13, 12, STEEL)
    t.rect(3, 5, 12, 11, DARK[1])
    parts = [((4, 6), (5, 9), COPPER), ((6, 6), (7, 9), COPPER), ((9, 6), (11, 7), DARK), ((9, 9), (11, 10), DANGER)]
    for i, ((x0, y0), (x1, y1), ramp) in enumerate(parts):
        if i < len(parts) * built:
            t.rect(x0, y0, x1, y1, ramp[3])
            t.hline(x0, x1, y0, ramp[5])
    if built >= 1.0:
        # the krytron's glow
        t.set(10, 6, CYAN[6])
        glow(t, 10, 7, 3.0, CYAN[6], 0.3)
    # leads
    for x in (4, 7, 10, 12):
        t.set(x, 3, COPPER[4])
        t.set(x, 2, COPPER[2] if x % 2 else DANGER[4])
    item_outline(t, STEEL[0])
    return t


@item("firing_set")
def firing_set():
    return _firing_set(244)


@item("incomplete_firing_set")
def incomplete_firing_set():
    return _firing_set(245, built=0.5)


def _implosion(seed, lenses=1.0):
    """A sphere tiled with lenses round a beryllium tamper, a hole left for the core."""
    t = Tex(16, 16, seed)
    _sphere(t, 8, 8, 6.4, EXPLOSIVE)
    # The seams between lenses, a football's pattern.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d > 6.4:
                continue
            a = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            if abs(d - 3.6) < 0.45 or (d > 3.6 and abs(math.sin(a * 2.5)) < 0.18):
                t.set(x, y, EXPLOSIVE[1])
            elif lenses < 1.0 and (a + math.pi) / (2 * math.pi) > lenses:
                t.set(x, y, LEAD[2] if (x + y) % 2 else LEAD[3])
    # The beryllium tamper in the middle and the hole for the core.
    t.disc(8, 8, 2.6, LITHIUM[5])
    t.disc(8, 8, 1.4, DARK[0])
    t.ring(8, 8, 6.6, 6.0, EXPLOSIVE[0])
    item_outline(t, EXPLOSIVE[0])
    return t


@item("implosion_assembly")
def implosion_assembly():
    return _implosion(246)


@item("incomplete_implosion_assembly")
def incomplete_implosion_assembly():
    return _implosion(247, lenses=0.5)


def _casing(seed, ramp, band, length, fins=True, open_end=False):
    """A bomb casing lying on its side: a rounded body, its tail fins, a coloured band."""
    t = Tex(16, 16, seed)
    pts = capsule_pts(2.5, 8, 2.5 + length, 8, 4.2)
    if fins:
        pts |= {(x, y) for x in range(12, 15) for y in range(3, 14) if abs(y - 8) >= x - 11}
    sculpt(t, pts, ramp, base=3.8, seed=seed, grain=0.45)
    for y in range(16):
        if (7, y) in pts:
            t.set(7, y, band[4] if y % 2 else DARK[1])
            t.set(8, y, band[5] if y % 2 else DARK[0])
    if open_end:
        t.disc(3.5, 8, 2.6, DARK[0])
        t.ring(3.5, 8, 2.8, 2.2, ramp[2])
    item_outline(t, ramp[0])
    return t


OLIVE = STEEL.tinted(WARN[2], 0.35)


@item("bomb_casing")
def bomb_casing():
    # The empty olive drab casing, open at the nose for the implosion assembly.
    return _casing(248, OLIVE, WARN, 8, open_end=True)


@item("thermonuclear_casing")
def thermonuclear_casing():
    # A longer gunmetal radiation case, the waist between primary and secondary banded, lined with uranium.
    t = _casing(249, GUNMETAL, WARN, 9, open_end=True)
    t.set(3, 8, URANIUM[3])
    t.set(4, 8, URANIUM[2])
    return t


@item("incomplete_nuclear_bomb")
def incomplete_nuclear_bomb():
    t = _casing(250, OLIVE, WARN, 8, open_end=True)
    # The implosion assembly showing through the open nose.
    t.disc(3.5, 8, 2.2, EXPLOSIVE[4])
    t.disc(3.5, 8, 0.9, LITHIUM[5])
    return t


@item("incomplete_thermonuclear_bomb")
def incomplete_thermonuclear_bomb():
    t = _casing(251, GUNMETAL, WARN, 9, open_end=True)
    t.disc(3.5, 8, 2.2, EXPLOSIVE[4])
    t.disc(3.5, 8, 0.9, TRITIUM[5])
    return t

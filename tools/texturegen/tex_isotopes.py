"""Textures for isotopes and medicine: cobalt, the sealed sources, the technetium generator and the
instruments that use them (gamma irradiator, smoke detector, reactor irradiation channel).

Register block textures with ``@block`` and item textures with ``@item``; this module is imported
at the end of ``blocks.py``.

One colour language runs through the set: every gamma source glows the same Cherenkov blue
(``CHERENKOV``) behind lead, cobalt metal is a cool blue-silver, strontium is a warm crimson,
americium a pinkish gold foil and technetium a medical teal. Sealed sources wear the yellow and
black of a radiation label so they never read as plain ingredients.

The block models (``models/block/gamma_irradiator.json``, ``smoke_detector*.json`` and
``reactor_irradiation_channel*.json``, written by ``tools/modelgen``) sample these textures by
block coordinates, so each face texture puts its detail where its element sits:

* gamma irradiator: the lead head spans x/z 3..13 and y 3..13 (side rows 3..12); the mounting
  plate y 13..16 (rows 0..2); the corner legs y 0..13 at columns 1..3 and 13..15; the collimator
  nozzle's underside x/z 5..11 with a 4x4 aperture at 6..10.
* smoke detector: a disc 10 px across (3..13) and 3 px tall lying on the floor of the block (its
  side rows 13..16).
* irradiation channel: a full cube; the front carries the pneumatic sample tube, the rest is the
  reactor casing.
"""

import math

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from common import bolt
from items import (PILE, _ingot, _powder, _rtg_pellet, capsule_pts, item, mask_from,  # noqa: F401
                   rect_pts, sculpt)
from palette import BRASS, CARBONATE, DANGER, DARK, GLASS_OFF, LEAD, RADIUM, STEEL, WARN, WHITE
from pixelkit import Ramp, Tex, bevel, brushed, fbm, item_outline, mix, outline
from reactor_materials import STEEL as REACTOR_STEEL, steel_panel

CLEAR = (0, 0, 0, 0)
BLACK = (0, 0, 0, 255)

# Cobalt: a cool, slightly blue silver.
COBALT = Ramp("#232a38", "#384357", "#525f78", "#6f7e98", "#8f9db5", "#b3bfd2", "#dbe3ee")
# The glow of every gamma source (and of water round one): Cherenkov blue.
CHERENKOV = Ramp("#0b1a3a", "#11306e", "#1d4fb0", "#2f74e0", "#4f9bff", "#8cc4ff", "#d6ecff")
# Strontium-90: a warm crimson (strontium's flame colour) over a cream ceramic.
STRONTIUM = Ramp("#3e1620", "#5e1f2c", "#86293a", "#ad3a4a", "#d0566a", "#ec8a98", "#fbc9cf")
SR_CERAMIC = Ramp("#5a5246", "#7b7163", "#9d9282", "#bdb3a1", "#d8cfbd", "#ece5d5", "#fbf8ee")
# Americium oxide foil: a pinkish gold.
AMERICIUM = Ramp("#4f3226", "#77503a", "#a0704c", "#c4925e", "#ddb179", "#f0d29e", "#fbeccc")
# Technetium-99m pertechnetate: medical teal.
TECHNETIUM = Ramp("#0e3434", "#155050", "#1f7470", "#2e9a92", "#4cc2b4", "#86e0d2", "#ccf6ee")
# White enamel and moulded plastic (medical kit, smoke detector).
ENAMEL = CARBONATE
# Radiation-label yellow and the black of its trefoil.
LABEL = WARN
INK = Ramp("#0d0f10", "#17191b", "#222528", "#2d3134", "#3a3f42", "#474d50", "#585f62")
# A medical green cross.
MEDIC = Ramp("#123d22", "#1b5a31", "#247a40", "#2f9a50", "#45b866", "#79d590", "#bdf0c8")


def _window(t, x0, y0, x1, y1, ramp=CHERENKOV):
    """A glowing window: bright core, darker rim, one white glint top-left."""
    t.rect(x0, y0, x1, y1, ramp[3])
    if x1 - x0 >= 2 and y1 - y0 >= 2:
        t.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, ramp[5])
    t.hline(x0, x1, y1, ramp[2])
    t.vline(x1, y0, y1, ramp[2])
    t.set(x0, y0, ramp[6])


# A radiation trefoil on a 9x9 grid: two blades raised to the sides, one down, the dot between them
# (an analytic trefoil sampled 6x6 per pixel, then tidied). ``common.trefoil`` reads as a face at
# this size.
TREFOIL9 = [
    ".##...##.",
    "###...###",
    "###.#.###",
    "##.###.##",
    "....#....",
    ".........",
    "...###...",
    "..#####..",
    "..#####..",
]


# The same symbol on a 7x6 grid, for labels.
TREFOIL7 = [
    ".#...#.",
    "###.###",
    "##.#.##",
    ".......",
    "..###..",
    "..###..",
]


def _trefoil9(t, x0, y0, ink):
    t.stamp(TREFOIL9, {"#": ink}, x0, y0)


def _trefoil7(t, x0, y0, ink):
    t.stamp(TREFOIL7, {"#": ink}, x0, y0)


# The label's trefoil on an 8x8 grid, turned so one blade points up: with two blades on top, any
# trefoil this small reads as a face.
TREFOIL8_UP = [
    ".######.",
    "..####..",
    "...##...",
    "........",
    "...##...",
    "##....##",
    "###..###",
    ".##..##.",
]


def _trefoil8(t, x0, y0, ink):
    t.stamp(TREFOIL8_UP, {"#": ink}, x0, y0)


# ======================================================================================
# metals and isotopes (items)
# ======================================================================================

@item("cobalt_nugget")
def cobalt_nugget():
    """Three small rounded beads of cobalt, the way Create draws its nuggets."""
    t = Tex(16, 16, 1700)
    pts = set()
    beads = ((5.5, 7.5, 2.6), (10.5, 6.5, 2.3), (8.5, 11.5, 2.5))
    for cx, cy, r in beads:
        for y in range(16):
            for x in range(16):
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r:
                    pts.add((x, y))
    sculpt(t, pts, COBALT, base=3.7, seed=1700, grain=0.3)
    # Each bead: a dark lower-right crescent where it meets the next, a two-pixel highlight.
    for cx, cy, r in beads:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if r - 0.9 < d <= r and (x + 0.5 - cx) + (y + 0.5 - cy) > 0.6:
                    t.set(x, y, COBALT[1])
        hx, hy = int(cx - 1), int(cy - 1)
        t.set(hx, hy, COBALT[6])
        t.set(hx + 1, hy, COBALT[5])
    item_outline(t, COBALT[0])
    return t


@item("cobalt_ingot")
def cobalt_ingot():
    return _ingot(1701, COBALT)


@item("cobalt_60")
def cobalt_60():
    """A sealed Co-60 source in its lead pig: a domed lead flask with a slot window that glows
    Cherenkov blue, a yellow-and-black radiation band round its foot and a steel lifting knob."""
    t = Tex(16, 16, 1702)
    body = rect_pts(4, 4, 11, 13) | rect_pts(5, 3, 10, 3)
    sculpt(t, body, LEAD, base=3.4, seed=1702, grain=0.45)
    # Steel cap and knob.
    t.rect(5, 2, 10, 3, STEEL[4])
    t.hline(5, 10, 2, STEEL[6])
    t.hline(5, 10, 3, STEEL[3])
    t.rect(7, 0, 8, 1, STEEL[5])
    t.set(7, 0, STEEL[6])
    # Window onto the source capsule.
    t.rect(5, 5, 10, 10, LEAD[0])
    _window(t, 6, 6, 9, 9)
    t.rect(7, 7, 8, 8, CHERENKOV[6])
    t.set(7, 7, WHITE)
    # Radiation band at the foot.
    for x in range(4, 12):
        t.set(x, 11, LABEL[5] if (x // 2) % 2 == 0 else INK[1])
        t.set(x, 12, LABEL[3] if (x // 2) % 2 == 0 else INK[0])
    t.hline(4, 11, 13, LEAD[1])
    item_outline(t, LEAD[0])
    return t


@item("strontium_90")
def strontium_90():
    """Strontium-90 from the PUREX line: a heap of cream fluoride flecked with crimson."""
    t = _powder(1703, SR_CERAMIC, grain=0.9)
    pts = mask_from(PILE)
    for k, (x, y) in enumerate(sorted(pts)):
        if (x * 7 + y * 13) % 11 == 0 and (x, y - 1) in pts:
            t.set(x, y, STRONTIUM[4 if k % 2 else 3])
    t.set(8, 6, STRONTIUM[6])
    return t


@item("americium_241")
def americium_241():
    """An americium-241 foil source, the button that goes into a smoke detector: a pinkish-gold foil
    pressed into a steel cup, seen from above at an angle, with a raised ring round its active spot."""
    t = Tex(16, 16, 1704)
    cx, cy = 7.5, 7.0
    cup = set()
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - cx) / 6.4, (y + 0.5 - cy) / 4.6
            if dx * dx + dy * dy <= 1.0:
                cup.add((x, y))
                # The cup's rim has depth: repeat it 2 px lower.
                cup.add((x, y + 1))
                cup.add((x, y + 2))
    sculpt(t, cup, STEEL, base=3.6, seed=1704, grain=0.3)
    for x in range(3, 13):
        t.set(x, 10, STEEL[5])
    # The foil: a smaller disc, lit top-left, its bottom-right edge in shadow under the cup's lip.
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - cx) / 3.9, (y + 0.5 - cy) / 2.7
            d = math.sqrt(dx * dx + dy * dy)
            if d > 1.0:
                continue
            side = dx + dy
            v = 4.6 - side * 1.1
            if d > 0.75:
                v -= 0.8
            t.set(x, y, AMERICIUM[int(round(max(2, min(6, v))))])
    # The steel lip casts a dark line round the foil's lower right.
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - cx) / 4.5, (y + 0.5 - cy) / 3.3
            d = math.sqrt(dx * dx + dy * dy)
            if 0.88 < d <= 1.05 and dx + dy > 0.2:
                t.set(x, y, STEEL[1])
    t.set(6, 5, WHITE)
    item_outline(t, STEEL[0])
    return t


@item("technetium_generator")
def technetium_generator():
    """The technetium cow: a lead-shielded alumina column in a white housing. Two needles on top
    (saline in, eluate out), a carrying handle, the teal technetium band and a radiation label."""
    t = Tex(16, 16, 1706)
    body = rect_pts(3, 5, 12, 14) | rect_pts(4, 4, 11, 4)
    sculpt(t, body, ENAMEL, base=3.9, seed=1706, grain=0.25)
    # Lead shield showing at the foot.
    t.rect(3, 13, 12, 14, LEAD[3])
    t.hline(3, 12, 13, LEAD[4])
    t.hline(3, 12, 14, LEAD[2])
    # Teal band and its label.
    t.rect(3, 8, 12, 9, TECHNETIUM[4])
    t.hline(3, 12, 8, TECHNETIUM[5])
    t.hline(3, 12, 9, TECHNETIUM[3])
    # Radiation label: yellow square with a black dot (a trefoil at this size).
    t.rect(5, 10, 8, 12, LABEL[4])
    t.set(6, 11, INK[0])
    t.set(7, 11, INK[1])
    # Top: steel cap, two needle ports and the handle.
    t.rect(5, 3, 10, 4, STEEL[4])
    t.hline(5, 10, 3, STEEL[6])
    for x in (6, 9):
        t.vline(x, 1, 2, STEEL[6])
        t.set(x, 0, TECHNETIUM[5] if x == 9 else CHERENKOV[5])
    t.vline(3, 2, 4, DARK[3])
    t.vline(12, 2, 4, DARK[2])
    t.hline(3, 12, 1, DARK[4])
    t.set(3, 1, DARK[5])
    item_outline(t, ENAMEL[0])
    return t


@item("technetium_99m")
def technetium_99m():
    """A shielded syringe of technetium-99m: teal pertechnetate behind a tungsten shield's window,
    plunger to the lower left, needle to the upper right."""
    t = Tex(16, 16, 1707)
    barrel = capsule_pts(4.0, 12.0, 10.5, 5.5, 2.3)
    sculpt(t, barrel, LEAD, base=3.6, seed=1707, grain=0.3)
    # The window: teal liquid along the barrel's axis.
    for i in range(1, 7):
        x, y = 4 + i, 12 - i
        t.set(x, y, TECHNETIUM[5])
        t.set(x - 1, y, TECHNETIUM[3])
        t.set(x, y + 1, TECHNETIUM[3])
    t.set(6, 9, TECHNETIUM[6])
    t.set(7, 8, WHITE)
    # Hub and needle.
    t.set(11, 4, STEEL[5])
    t.set(12, 4, STEEL[4])
    t.set(11, 3, STEEL[4])
    for i in range(3):
        t.set(12 + i, 3 - i, STEEL[6] if i < 2 else STEEL[5])
    # Finger flange and plunger.
    for x, y in ((1, 12), (2, 11), (3, 14), (4, 13)):
        t.set(x, y, STEEL[4])
    t.set(2, 13, STEEL[3])
    t.set(1, 14, STEEL[5])
    t.set(0, 15, STEEL[6])
    item_outline(t, LEAD[0])
    return t


@item("strontium_rtg_pellet")
def strontium_rtg_pellet():
    """The strontium titanate heat source: the RTG capsule with a crimson core; it glows less than
    the plutonium pellet because it runs cooler for longer."""
    return _rtg_pellet(1708, STRONTIUM, STRONTIUM[5])


# ======================================================================================
# instruments (items)
# ======================================================================================

@item("radiography_camera")
def radiography_camera():
    """An industrial gamma radiography camera: a yellow shielded body on a carrying handle, the
    black trefoil on its side, the collimator port (blue when the source is in) on the right and
    the drive-cable socket on the left."""
    t = Tex(16, 16, 1709)
    body = rect_pts(1, 4, 13, 14) | rect_pts(2, 3, 12, 3)
    sculpt(t, body, LABEL, base=4.0, seed=1709, grain=0.2)
    # Handle.
    t.hline(4, 10, 0, DARK[4])
    t.set(4, 0, DARK[5])
    t.vline(3, 1, 2, DARK[3])
    t.vline(11, 1, 2, DARK[2])
    t.hline(4, 10, 1, DARK[1])
    # Trefoil.
    _trefoil9(t, 3, 5, INK[1])
    # Collimator port.
    t.rect(14, 7, 15, 11, STEEL[4])
    t.vline(14, 7, 11, STEEL[5])
    t.vline(15, 7, 11, STEEL[2])
    t.rect(15, 8, 15, 10, CHERENKOV[4])
    t.set(15, 8, CHERENKOV[6])
    # Drive-cable socket on the left.
    t.rect(0, 8, 0, 10, DARK[3])
    t.set(0, 8, DARK[5])
    # Base skid.
    t.hline(1, 13, 15, DARK[2])
    t.set(1, 15, DARK[3])
    item_outline(t, LABEL[0])
    return t


@item("medical_isotope_kit")
def medical_isotope_kit():
    """A white medical case with a green cross, the technetium teal band and a small radiation
    label, brass latches and a dark handle."""
    t = Tex(16, 16, 1710)
    body = rect_pts(1, 5, 14, 13)
    sculpt(t, body, ENAMEL, base=4.1, seed=1710, grain=0.2)
    # Lid seam and teal band.
    t.hline(1, 14, 7, ENAMEL[2])
    t.hline(1, 14, 13, TECHNETIUM[4])
    # Green cross.
    t.rect(4, 9, 9, 10, MEDIC[4])
    t.rect(6, 7, 7, 12, MEDIC[4])
    t.hline(4, 5, 9, MEDIC[5])
    t.vline(6, 7, 8, MEDIC[5])
    t.hline(8, 9, 10, MEDIC[2])
    t.vline(7, 11, 12, MEDIC[2])
    # Radiation label.
    t.rect(11, 8, 13, 10, LABEL[4])
    t.set(12, 9, INK[0])
    # Latches.
    for x in (3, 12):
        t.rect(x, 6, x, 7, BRASS[5])
        t.set(x, 7, BRASS[2])
    # Handle.
    t.hline(5, 10, 2, DARK[4])
    t.vline(5, 3, 4, DARK[3])
    t.vline(10, 3, 4, DARK[2])
    t.hline(6, 9, 3, DARK[1])
    item_outline(t, ENAMEL[0])
    return t


# ======================================================================================
# gamma irradiator (block)
# ======================================================================================
#
# The model is a lead head hung from a steel mounting plate on four corner legs, a collimator
# nozzle under it whose aperture a two-leaf shutter closes; it works over the belt or depot two
# blocks below it, like Create's press. Side textures run v = 16 - y.

LEAD_HEAD = LEAD.tinted(DARK[2], 0.25)


@block("gamma_irradiator_head")
def gamma_irradiator_head():
    """The lead head's sides: cast ribs round a yellow radiation label with a black trefoil that
    fills the face (the head's sides are rows 3..12, columns 3..12)."""
    t = Tex(seed=1720)
    brushed(t, LEAD_HEAD, base=3.2, amp=0.55, seed=1720, axis="v", scale=6.0)
    for x in range(0, 16, 3):
        t.vline(x, 0, 15, LEAD_HEAD[4])
        t.vline(x + 1, 0, 15, LEAD_HEAD[2])
    # The lit top edge of the head (row 3).
    t.hline(0, 15, 3, LEAD_HEAD[5])
    # Riveted radiation label filling the head's face (columns 3..12, rows 3..12).
    t.rect(3, 3, 12, 12, LABEL[4])
    bevel(t, 3, 3, 12, 12, LABEL, hi=LABEL[6], lo=LABEL[2])
    _trefoil8(t, 4, 4, INK[1])
    # Shaded foot of the head (row 12) under the label.
    t.hline(0, 15, 12, LEAD_HEAD[1])
    return t


@block("gamma_irradiator_head_plain")
def gamma_irradiator_head_plain():
    """The head's other two sides: cast ribs and a small brass maker's plate (rows 3..12)."""
    t = Tex(seed=1720)
    brushed(t, LEAD_HEAD, base=3.2, amp=0.55, seed=1720, axis="v", scale=6.0)
    for x in range(0, 16, 3):
        t.vline(x, 0, 15, LEAD_HEAD[4])
        t.vline(x + 1, 0, 15, LEAD_HEAD[2])
    t.hline(0, 15, 3, LEAD_HEAD[5])
    t.hline(0, 15, 12, LEAD_HEAD[1])
    t.rect(6, 6, 10, 8, BRASS[4])
    bevel(t, 6, 6, 10, 8, BRASS, hi=BRASS[6], lo=BRASS[1])
    t.hline(7, 9, 7, BRASS[2])
    return t


@block("gamma_irradiator_head_top")
def gamma_irradiator_head_top():
    """The top of the lead head, seen between the legs under the plate: plain cast lead."""
    t = Tex(seed=1721)
    brushed(t, LEAD_HEAD, base=3.0, amp=0.5, seed=1721, axis="h", scale=5.0)
    outline(t, 3, 3, 12, 12, LEAD_HEAD[1])
    return t


@block("gamma_irradiator_plate")
def gamma_irradiator_plate():
    """The mounting plate's top: bolted steel, a lifting eye in the middle and a blue Co-60 stencil."""
    t = Tex(seed=1722)
    brushed(t, STEEL, base=3.5, amp=0.55, seed=1722, axis="h", scale=6.0)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(t, x, y, STEEL)
    # Lifting eye.
    t.ring(8, 8, 3.0, 1.8, STEEL[5])
    t.ring(8, 8, 1.8, 0.0, STEEL[1])
    t.set(6, 6, STEEL[6])
    # Source-type stencil: a short blue line (Co-60) under the eye.
    t.hline(6, 9, 12, CHERENKOV[3])
    return t


@block("gamma_irradiator_plate_side")
def gamma_irradiator_plate_side():
    """The mounting plate's edge (rows 0..2): bolted steel with a thin yellow-and-black stripe."""
    t = Tex(seed=1723)
    brushed(t, STEEL, base=3.4, amp=0.5, seed=1723, axis="h", scale=6.0)
    t.hline(0, 15, 0, STEEL[6])
    for x in range(16):
        t.set(x, 1, LABEL[4] if (x // 2) % 2 == 0 else INK[1])
    t.hline(0, 15, 2, STEEL[1])
    for x in (2, 12):
        t.set(x, 1, STEEL[6])
        t.set(x + 1, 1, STEEL[2])
    return t


@block("gamma_irradiator_plate_bottom")
def gamma_irradiator_plate_bottom():
    """The underside of the mounting plate, round the head: dark steel."""
    t = Tex(seed=1724)
    brushed(t, DARK, base=3.0, amp=0.4, seed=1724, axis="h", scale=6.0)
    outline(t, 0, 0, 15, 15, DARK[0])
    return t


@block("gamma_irradiator_nozzle")
def gamma_irradiator_nozzle():
    """The collimator nozzle: its sides (rows 13..14, a steel collar) and its underside (columns and
    rows 5..10), a dark tungsten face with the 4x4 aperture (6..9) the shutter closes."""
    t = Tex(seed=1725)
    brushed(t, STEEL, base=3.0, amp=0.45, seed=1725, axis="h", scale=6.0)
    t.hline(0, 15, 13, STEEL[5])
    t.hline(0, 15, 14, STEEL[2])
    t.rect(5, 5, 10, 10, DARK[2])
    bevel(t, 5, 5, 10, 10, DARK, hi=DARK[4], lo=DARK[1])
    t.rect(6, 6, 9, 9, BLACK)
    t.frame(6, 6, 9, 9, CHERENKOV[0])
    return t


@block("gamma_irradiator_shutter")
def gamma_irradiator_shutter():
    """A shutter leaf: polished tungsten with grip lines across it and a lead edge."""
    t = Tex(seed=1726)
    brushed(t, DARK.tinted(STEEL[4], 0.35), base=3.6, amp=0.4, seed=1726, axis="v", scale=6.0)
    for x in range(1, 16, 3):
        t.vline(x, 0, 15, DARK[1])
    outline(t, 0, 0, 15, 15, DARK[0])
    return t


@block("gamma_irradiator_glow")
def gamma_irradiator_glow():
    """The open aperture, drawn full-bright: Cherenkov blue round a white-hot middle. Near-uniform
    so any slice the partial samples reads as the glow."""
    t = Tex(seed=1727)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8.0
            n = fbm(x, y, 1727, 16, 16, 2, 4.0)
            v = 1.0 - d * 0.75 + (n - 0.5) * 0.25
            t.set(x, y, CHERENKOV.t(max(0.35, min(1.0, v))))
    return t


@block("gamma_irradiator_beam")
def gamma_irradiator_beam():
    """The gamma cone the renderer draws additively under an open shutter: black-backed vertical
    streaks of blue, brightest in the middle columns, fading downwards (v grows down the beam)."""
    t = Tex(seed=1728)
    for y in range(16):
        for x in range(16):
            across = 1.0 - abs(x + 0.5 - 8) / 8.0
            down = 1.0 - y / 20.0
            streak = fbm(x, 0, 1728, 16, 16, 2, 8.0)
            v = across * down * (0.55 + streak * 0.6)
            t.set(x, y, mix(BLACK, CHERENKOV[5], max(0.0, min(1.0, v))))
    return t


# ======================================================================================
# smoke detector (block)
# ======================================================================================

SMOKE_PLASTIC = ENAMEL.tinted((230, 226, 214, 255), 0.25)


@block("smoke_detector_face")
def smoke_detector_face():
    """The detector's face (its top on the floor model): moulded off-white plastic with a ring of
    vent slots round the test button (the disc spans 3..12, the button 6..9)."""
    t = Tex(seed=1730)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1730, 16, 16, 2, 5.0)
            t.set(x, y, SMOKE_PLASTIC[5 if n > 0.42 else 4])
    # Moulded rim of the disc, lit top-left.
    t.ring(8, 8, 5.2, 4.4, SMOKE_PLASTIC[3])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 4.4 <= d <= 5.2 and x + y < 14:
                t.set(x, y, SMOKE_PLASTIC[6])
    # Eight vent slots round the button.
    for a in range(8):
        ang = a * math.pi / 4 + math.pi / 8
        for r in (2.7, 3.5):
            x, y = 8 + math.cos(ang) * r, 8 + math.sin(ang) * r
            t.set(int(x), int(y), SMOKE_PLASTIC[0])
    # Test button.
    t.rect(7, 7, 8, 8, SMOKE_PLASTIC[6])
    t.set(8, 8, SMOKE_PLASTIC[3])
    t.set(7, 7, WHITE)
    return t


@block("smoke_detector_side")
def smoke_detector_side():
    """The detector's rim (rows 13..15 on the floor model): plastic with dark smoke-inlet slots."""
    t = Tex(seed=1731)
    for y in range(16):
        for x in range(16):
            t.set(x, y, SMOKE_PLASTIC[4 if y % 4 else 5])
    for x in range(1, 16, 3):
        t.vline(x, 13, 14, SMOKE_PLASTIC[0])
        t.vline(x + 1, 13, 14, SMOKE_PLASTIC[2])
    t.hline(0, 15, 12, SMOKE_PLASTIC[6])
    t.hline(0, 15, 15, SMOKE_PLASTIC[2])
    return t


@block("smoke_detector_base")
def smoke_detector_base():
    """The mounting plate against the wall or ceiling: grey plastic with two screw heads."""
    t = Tex(seed=1732)
    for y in range(16):
        for x in range(16):
            t.set(x, y, SMOKE_PLASTIC[2 if (x + y) % 5 else 3])
    for x, y in ((5, 7), (10, 7)):
        t.set(x, y, STEEL[5])
        t.set(x + 1, y + 1, STEEL[1])
    return t


@block("smoke_detector_led")
def smoke_detector_led():
    """The status LED when quiet: a dim green lens."""
    t = Tex(seed=1733)
    t.rect(0, 0, 15, 15, mix(GLASS_OFF[2], RADIUM[2], 0.5))
    t.disc(7.5, 7.5, 5.0, mix(GLASS_OFF[3], RADIUM[3], 0.6))
    t.rect(4, 4, 6, 5, RADIUM[5])
    return t


@block("smoke_detector_led_alarm")
def smoke_detector_led_alarm():
    """The status LED in alarm, drawn full-bright: red."""
    t = Tex(seed=1734)
    t.rect(0, 0, 15, 15, DANGER[4])
    t.disc(7.5, 7.5, 6.0, DANGER[5])
    t.disc(7.5, 7.5, 3.5, DANGER[6])
    t.rect(4, 4, 6, 5, WHITE)
    return t


# ======================================================================================
# reactor irradiation channel (block)
# ======================================================================================
#
# A reactor wall part like the fuel port: a full cube of reactor casing whose front carries a
# pneumatic sample tube ("rabbit" system) with a flanged mouth, a status lamp below it and a violet
# stencil band (irradiation). Overlays are cutout and drawn full-bright over the front.

IRR_ACCENT = Ramp("#2c2045", "#41306a", "#5a468f", "#7562b0", "#9481cc", "#b4a6e0", "#d6cdf0")
TUBE_C = (8.0, 7.0)
LAMP = (7, 13, 8, 13)  # x0, y0, x1, y1 of the status lamp

CUTOUT_BLOCKS["reactor_irradiation_channel_glow"] = (16, 16)
CUTOUT_BLOCKS["reactor_irradiation_channel_ready"] = (16, 16)


def _channel_front(loaded):
    t = steel_panel(seed=1740)
    s = REACTOR_STEEL
    cx, cy = TUBE_C
    # Square weld plate, bolted flange and the tube mouth.
    t.rect(3, 2, 12, 11, s[2])
    bevel(t, 3, 2, 12, 11, s, hi=s[5], lo=s[0])
    t.disc(cx, cy, 4.2, s[4])
    t.ring(cx, cy, 4.2, 3.5, s[5])
    t.ring(cx, cy, 4.4, 4.1, s[0])
    for x, y in ((4, 3), (11, 3), (4, 10), (11, 10)):
        t.set(x, y, s[6])
    t.disc(cx, cy, 2.6, s[1])
    t.disc(cx, cy, 2.0, BLACK)
    if loaded:
        # The sample capsule ("rabbit") seated in the tube: its aluminium end cap.
        t.disc(cx, cy, 1.7, ENAMEL[3])
        t.set(7, 6, WHITE)
        t.set(8, 7, ENAMEL[1])
        t.set(7, 7, ENAMEL[2])
    # Status lamp (off) and the violet stencil band.
    t.rect(LAMP[0] - 1, LAMP[1] - 1, LAMP[2] + 1, LAMP[3] + 1, s[0])
    t.rect(LAMP[0], LAMP[1], LAMP[2], LAMP[3], GLASS_OFF[2])
    t.set(LAMP[0], LAMP[1], GLASS_OFF[5])
    t.hline(3, 5, 13, IRR_ACCENT[4])
    t.hline(10, 12, 13, IRR_ACCENT[4])
    t.hline(3, 5, 14, IRR_ACCENT[2])
    t.hline(10, 12, 14, IRR_ACCENT[2])
    return t


@block("reactor_irradiation_channel_front")
def reactor_irradiation_channel_front():
    """The channel's face when empty: a dark tube mouth."""
    return _channel_front(False)


@block("reactor_irradiation_channel_front_loaded")
def reactor_irradiation_channel_front_loaded():
    """The channel's face with a sample capsule seated in the tube."""
    return _channel_front(True)


@block("reactor_irradiation_channel_glow")
def reactor_irradiation_channel_glow():
    """Overlay while irradiating (cutout, full-bright): the Cherenkov glow round the capsule in the
    tube and a blue status lamp."""
    t = Tex(seed=1741)
    t.rect(0, 0, 15, 15, CLEAR)
    cx, cy = TUBE_C
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if 1.6 < d <= 2.6:
                t.set(x, y, CHERENKOV[5] if d < 2.1 else CHERENKOV[3])
    t.rect(LAMP[0], LAMP[1], LAMP[2], LAMP[3], CHERENKOV[5])
    t.set(LAMP[0], LAMP[1], CHERENKOV[6])
    return t


@block("reactor_irradiation_channel_ready")
def reactor_irradiation_channel_ready():
    """Overlay when the sample has reached its fluence (cutout, full-bright): a green status lamp."""
    t = Tex(seed=1742)
    t.rect(0, 0, 15, 15, CLEAR)
    t.rect(LAMP[0], LAMP[1], LAMP[2], LAMP[3], RADIUM[5])
    t.set(LAMP[0], LAMP[1], RADIUM[6])
    return t

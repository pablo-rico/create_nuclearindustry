"""Textures for the chart recorder, annunciator panel and controller module.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.
"""

from blocks import block  # noqa: F401
from common import bolt
from gui import _plate, _recess, gui  # noqa: F401
from items import item, mask_from, sculpt  # noqa: F401
from palette import BLACK, CONSOLE, DANGER, DARK, GLASS_OFF, MIMIC_BLUE, RADIUM, STEEL, WARN, WHITE
from pixelkit import Ramp, Tex, bevel, brushed, item_outline, mix, outline

# Chart paper and its grid, shared by the block window, the item and the roll screen.
PAPER = Ramp("#8f8770", "#b3aa8e", "#cfc6a6", "#ddd5b7", "#e8e1c6", "#f0eacf", "#f8f3df")
GRID = (0xb4, 0xc7, 0xaa, 255)
INK = (DANGER[3], MIMIC_BLUE[3], RADIUM[2])


def _housing(seed):
    # The dark instrument housing of the control monitor, so the instruments read as one family.
    t = Tex(seed=seed)
    brushed(t, DARK, base=3.2, amp=0.5, seed=seed)
    bevel(t, 0, 0, 15, 15, DARK)
    outline(t, 0, 0, 15, 15, DARK[0])
    return t


def _well(t, x0, y0, x1, y1):
    """A sunken opening: dark rim on the top-left, light on the bottom-right."""
    t.hline(x0 - 1, x1 + 1, y0 - 1, DARK[0])
    t.vline(x0 - 1, y0 - 1, y1 + 1, DARK[0])
    t.hline(x0 - 1, x1 + 1, y1 + 1, DARK[3])
    t.vline(x1 + 1, y0 - 1, y1 + 1, DARK[3])


# ======================================================================================
# chart recorder
# ======================================================================================

@block("chart_recorder_front")
def chart_recorder_front():
    # Paper window at x 2-13, y 2-11 (the renderer draws the live chart there, pixels 2-14 x 4-14
    # from the bottom-left); a paper-tray gauge slot at y 13-14.
    t = _housing(160)
    t.rect(2, 2, 13, 11, PAPER[3])
    for x in range(2, 14):
        for y in range(2, 12):
            if x % 4 == 1 or y % 3 == 0:
                t.set(x, y, mix(PAPER[3], GRID, 0.6))
    _well(t, 2, 2, 13, 11)
    t.hline(2, 13, 12, DARK[1])
    t.rect(2, 13, 13, 14, BLACK)
    _well(t, 2, 13, 13, 14)
    for x, y in ((0, 0), (14, 0)):
        bolt(t, x, y, DARK)
    return t


@block("chart_recorder_top")
def chart_recorder_top():
    # The paper roll seen from above through a slot, between the side bolts.
    t = _housing(161)
    t.rect(3, 5, 12, 10, PAPER[2])
    for x in range(3, 13):
        t.set(x, 5, PAPER[5])
        t.set(x, 10, PAPER[1])
    _well(t, 3, 5, 12, 10)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, DARK)
    return t


@item("chart_roll")
def chart_roll():
    # A curled strip of chart paper with a red and a blue trace.
    t = Tex(16, 16, 162)
    art = [
        "................",
        "................",
        "..##############",
        ".###############",
        ".###############",
        ".###############",
        ".###############",
        ".###############",
        ".###############",
        ".###############",
        ".###############",
        ".###############",
        "..##############",
        "................",
        "................",
        "................",
    ]
    pts = mask_from(art)
    sculpt(t, pts, PAPER, base=4.6, seed=162, grain=0.25)
    # The rolled-up end on the left.
    for y in range(2, 13):
        t.set(1, y, PAPER[2] if 2 < y < 12 else PAPER[1])
        t.set(2, y, PAPER[5])
        t.set(3, y, PAPER[1])
    for x in range(5, 15, 3):
        for y in range(3, 12):
            t.blend(x, y, GRID, 0.6)
    red = [9, 8, 8, 6, 5, 6, 7, 6, 5, 5]
    blue = [5, 5, 6, 8, 9, 9, 8, 9, 10, 10]
    for i, (ry, by) in enumerate(zip(red, blue)):
        t.set(5 + i, ry, INK[0])
        t.set(5 + i, by, INK[1])
    item_outline(t, PAPER[0])
    return t


@gui("chart_recorder")
def chart_recorder_gui():
    # 200x124: title strip, a window with three pen rows (swatch well at x 10-19, buttons from
    # x 24, rows at y 22/46/70) and the paper speed button at y 96.
    w, h = 200, 124
    t = _plate(w, h, 410)
    _recess(t, 6, 4, w - 7, 16)
    _recess(t, 6, 18, w - 7, h - 6)
    for row in range(3):
        y = 22 + row * 24 + 4
        t.rect(10, y, 19, y + 11, DARK[0])
        t.rect(11, y + 1, 18, y + 10, DARK[2])
    t.hline(10, w - 11, 91, DARK[3])
    return t


@gui("chart_roll")
def chart_roll_gui():
    # 256x168: a sheet of chart paper (grid area 12-244 x 20-128) on a clipboard, legend below.
    w, h = 256, 168
    t = Tex(w, h, 411)
    brushed(t, PAPER, base=4.8, amp=0.25, seed=411, scale=11.0)
    bevel(t, 0, 0, w - 1, h - 1, PAPER, hi=PAPER[6], lo=PAPER[1])
    outline(t, 0, 0, w - 1, h - 1, PAPER[0])
    t.rect(12, 20, 244, 128, PAPER[5])
    for x in range(12, 245):
        for y in range(20, 129):
            minor_x = (x - 12) % 10 == 0
            minor_y = (y - 20) % 9 == 0
            if (x - 12) % 50 == 0 or (y - 20) % 27 == 0:
                t.set(x, y, mix(PAPER[5], GRID, 0.95))
            elif minor_x or minor_y:
                t.set(x, y, mix(PAPER[5], GRID, 0.45))
    outline(t, 11, 19, 245, 129, PAPER[1])
    # Sprocket holes along the top edge of the strip, and a fold under the grid.
    for x in range(16, 244, 12):
        t.rect(x, 15, x + 2, 16, PAPER[1])
    t.hline(12, 244, 131, PAPER[2])
    return t


# ======================================================================================
# annunciator panel
# ======================================================================================

@block("annunciator_panel_front")
def annunciator_panel_front():
    # Six windows in two columns (x 1-6, 9-14) and three rows (y 1-3, 5-7, 9-11), dark until the
    # renderer lights them, over an ACK (x 2-6) and a RESET (x 9-13) button at y 13-14.
    t = _housing(170)
    for row in range(3):
        y0 = 1 + row * 4
        for x0 in (1, 9):
            t.rect(x0, y0, x0 + 5, y0 + 2, GLASS_OFF[1])
            t.hline(x0, x0 + 5, y0, GLASS_OFF[3])
            t.set(x0, y0 + 1, GLASS_OFF[2])
            _well(t, x0, y0, x0 + 5, y0 + 2)
    t.rect(2, 13, 6, 14, DARK[0])
    t.hline(2, 6, 13, DARK[3])
    _well(t, 2, 13, 6, 14)
    t.rect(9, 13, 13, 14, DANGER[1])
    t.hline(9, 13, 13, DANGER[3])
    _well(t, 9, 13, 13, 14)
    t.set(7, 15, DARK[1])
    t.set(8, 15, DARK[1])
    return t


@gui("annunciator_panel")
def annunciator_panel_gui():
    # 200x142: title strip, window tabs at y 20, label box at y 44, metric row at y 62, the
    # link and state lines at y 88-108 and the unlink button at y 114.
    w, h = 200, 142
    t = _plate(w, h, 412)
    _recess(t, 6, 4, w - 7, 16)
    _recess(t, 6, 18, w - 7, h - 6)
    t.hline(10, w - 11, 40, DARK[3])
    t.hline(10, w - 11, 85, DARK[3])
    for x in range(10, w - 10, 6):
        t.set(x, h - 4, WARN[3] if (x // 6) % 2 else DARK[0])
    return t


# ======================================================================================
# rod controller module
# ======================================================================================

@block("desk_controller")
def desk_controller():
    # Partial atlas. [0,0,10,4] readout face (green LCD with a bar and digits) | [0,4,10,8] readout
    # top and back | [0,8,4,12] readout sides | [10,0,14,2.5] setpoint scale | [10,2.5,14,3] scale
    # edge | [10,6,12,7.25] knob side | [12,6,14,8] knob top | [14,6] pointer. Unused texels stay opaque.
    t = Tex(seed=180)
    t.rect(0, 0, 15, 15, DARK[1])
    lcd = Ramp("#0f1f14", "#173323", "#21503a", "#3b7a52", "#5fa66d", "#9fd88e", "#d7f5c0")
    t.rect(0, 0, 9, 3, lcd[1])
    outline(t, 0, 0, 9, 3, DARK[0])
    for x in (2, 4, 6):
        t.vline(x, 1, 2, lcd[5])
    t.set(3, 1, lcd[5])
    t.set(7, 2, lcd[4])
    t.hline(1, 8, 3, lcd[2])
    t.rect(0, 4, 9, 7, CONSOLE[1])
    t.hline(0, 9, 4, CONSOLE[3])
    t.rect(0, 8, 3, 11, CONSOLE[2])
    t.hline(0, 3, 8, CONSOLE[4])
    # Setpoint scale: ticks round the knob, red past three quarters.
    t.rect(10, 0, 13, 2, DARK[0])
    for x, y in ((10, 2), (10, 1), (11, 0), (12, 0), (13, 1)):
        t.set(x, y, STEEL[6])
    t.set(13, 2, DANGER[4])
    t.hline(10, 13, 3, DARK[2])
    # Knob: knurled side, grey top with a white index.
    for x in range(10, 12):
        t.vline(x, 6, 7, DARK[3] if x % 2 == 0 else DARK[1])
    t.rect(12, 6, 13, 7, STEEL[3])
    t.set(12, 6, STEEL[5])
    t.set(14, 6, WHITE)
    t.set(14, 7, STEEL[6])
    return t

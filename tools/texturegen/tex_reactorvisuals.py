"""Textures for the visible and audible fission reactor: the casing viewport and the fuel channels.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.

The fuel channel (``blockstates/uranium_rod.json``) is a multipart of hand-written models:

* ``uranium_rod_sheath``: zirconium posts and grid plates, always drawn (cutout). The plates use
  ``uranium_rod_plate`` (a spacer grid with holes) and the open sides ``uranium_rod_cage`` (spacer
  bands with clear windows between them); the posts sample ``uranium_rod_sheath``.
* ``uranium_rod_pellets_<fresh|half|spent>``: four fuel pins, 4px wide, sampled at u 0..4, 4..8,
  8..12 or 12..16, so the pin shading repeats every 4px. Darker as the fuel burns.
* ``uranium_rod_water`` (opaque here) and ``uranium_rod_cherenkov`` (streaks with clear gaps): the
  models draw them translucent with a vertex alpha (``neoforge_data.color``), faint enough that the
  green fuel shows through; the Cherenkov glow at full block and sky light.
"""

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from gui import gui  # noqa: F401
from items import item  # noqa: F401
from palette import CYAN, DARK, LEAD, MIMIC_BLUE, URANIUM, WARN, WHITE
from pixelkit import Tex, bevel, fbm, mix, rivet
from reactor_materials import STEEL as REACTOR_STEEL

CLEAR = (0, 0, 0, 0)
# Zirconium alloy cladding: a light, slightly warm grey.
ZIRCALOY = [(88, 92, 92, 255), (112, 117, 116, 255), (138, 143, 140, 255), (162, 167, 162, 255),
            (184, 189, 182, 255), (206, 210, 202, 255), (228, 231, 222, 255)]

# The viewport is a window; the sheath's plates and cage have open holes.
CUTOUT_BLOCKS["reactor_viewport"] = (16, 16)
CUTOUT_BLOCKS["reactor_viewport_connected"] = (64, 64)
CUTOUT_BLOCKS["uranium_rod_plate"] = (16, 16)
CUTOUT_BLOCKS["uranium_rod_cage"] = (16, 16)
CUTOUT_BLOCKS["uranium_rod_cherenkov"] = (16, 16)


# ======================================================================================
# viewport
# ======================================================================================

def _viewport_tile(t, ox, oy, left, right, top, bottom):
    """One 16x16 pane at (ox, oy): the rolled-steel frame and the lead glass's amber retaining
    ring only on the sides that meet something other than another viewport, so a wall of them
    reads as one big window. Glints use tile-local spots, the same on every tile."""
    steel = REACTOR_STEEL
    amber_hi = mix(WARN[5], LEAD[5], 0.45)
    amber_lo = mix(WARN[2], LEAD[2], 0.55)
    for y in range(16):
        for x in range(16):
            t.set(ox + x, oy + y, CLEAR)
    # Frame: two pixels of steel, lit top-left, then a dark seam and the amber ring.
    for i in range(16):
        if top:
            t.set(ox + i, oy, steel[5])
            t.set(ox + i, oy + 1, steel[4])
            t.set(ox + i, oy + 2, steel[0])
            t.set(ox + i, oy + 3, amber_hi)
        if bottom:
            t.set(ox + i, oy + 15, steel[1])
            t.set(ox + i, oy + 14, steel[2])
            t.set(ox + i, oy + 13, steel[0])
            t.set(ox + i, oy + 12, amber_lo)
        if left:
            t.set(ox, oy + i, steel[5])
            t.set(ox + 1, oy + i, steel[4])
            t.set(ox + 2, oy + i, steel[0])
            t.set(ox + 3, oy + i, amber_hi)
        if right:
            t.set(ox + 15, oy + i, steel[1])
            t.set(ox + 14, oy + i, steel[2])
            t.set(ox + 13, oy + i, steel[0])
            t.set(ox + 12, oy + i, amber_lo)
    # A rivet holds each corner where two framed sides meet.
    for cx, cy, on in ((1, 1, top and left), (14, 1, top and right), (1, 14, bottom and left), (14, 14, bottom and right)):
        if on:
            rivet(t, ox + cx - (1 if cx > 8 else 0), oy + cy - (1 if cy > 8 else 0), steel)
    # One small glint on the glass, opaque so cutout rendering keeps it.
    t.set(ox + 6, oy + 5, WHITE)
    t.set(ox + 5, oy + 6, amber_hi)


@block("reactor_viewport")
def reactor_viewport():
    """A lead-glass pane in a rolled-steel frame, matching the reactor casing (the item and a lone viewport)."""
    t = Tex(seed=901)
    _viewport_tile(t, 0, 0, True, True, True, True)
    return t


@block("reactor_viewport_connected")
def reactor_viewport_connected():
    """Create's RECTANGLE layout: 4x4 tiles, index = col + 4*row.
    col 0 isolated, 1 joined right, 2 joined both, 3 joined left;
    row 0 joined down, 1 joined both, 2 joined up, 3 isolated."""
    t = Tex(64, 64, 903)
    for row in range(4):
        for col in range(4):
            _viewport_tile(t, col * 16, row * 16,
                           left=col in (0, 1), right=col in (0, 3),
                           top=row in (0, 3), bottom=row in (2, 3))
    return t


# ======================================================================================
# fuel channel
# ======================================================================================

@block("uranium_rod_sheath")
def uranium_rod_sheath():
    """The zirconium posts at the channel's corners: brushed vertically, with spacer collars."""
    t = Tex(seed=902)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y * 0.25, 902, 16, 16, 2, 5.0)
            t.set(x, y, ZIRCALOY[3 if n < 0.55 else 4])
    for x in range(16):
        # every column carries the same collars, so any 1px post looks alike
        for y in (0, 7, 15):
            t.set(x, y, ZIRCALOY[1])
        t.set(x, 1, ZIRCALOY[6])
        t.set(x, 8, ZIRCALOY[5])
        t.set(x, 14, ZIRCALOY[2])
    return t


@block("uranium_rod_plate")
def uranium_rod_plate():
    """The top and bottom spacer grids: a frame and a cross, open where the four pins pass."""
    t = Tex(seed=903)
    t.rect(0, 0, 15, 15, ZIRCALOY[3])
    bevel(t, 0, 0, 15, 15, ZIRCALOY, hi=ZIRCALOY[5], lo=ZIRCALOY[1])
    t.hline(1, 14, 7, ZIRCALOY[4])
    t.hline(1, 14, 8, ZIRCALOY[2])
    t.vline(7, 1, 14, ZIRCALOY[4])
    t.vline(8, 1, 14, ZIRCALOY[2])
    # holes round the pins (pins sit at 2..5 and 10..13)
    for x0 in (2, 10):
        for y0 in (2, 10):
            t.rect(x0, y0, x0 + 3, y0 + 3, CLEAR)
    return t


@block("uranium_rod_cage")
def uranium_rod_cage():
    """An open side of the channel: spacer bands at the ends and the middle, clear between."""
    t = Tex(seed=904)
    for x in range(16):
        for y, c in ((0, ZIRCALOY[2]), (1, ZIRCALOY[5]), (7, ZIRCALOY[4]), (8, ZIRCALOY[2]),
                     (14, ZIRCALOY[4]), (15, ZIRCALOY[1])):
            t.set(x, y, c)
    for y in range(2, 14):
        if y in (7, 8):
            continue
        for x in range(16):
            t.set(x, y, CLEAR)
    # spring tabs on the middle band
    for x in (3, 11):
        t.set(x, 7, ZIRCALOY[6])
        t.set(x, 8, ZIRCALOY[1])
    return t


def _pellets(seed, ceramic, cladding):
    """Four pins' worth of stacked fuel pellets seen through split cladding.

    ``ceramic`` is the pellets' ramp (light to dark, 4 tones), ``cladding`` the tube's.
    The shading repeats every 4px so any pin-wide window of the texture looks the same."""
    t = Tex(seed=seed)
    for y in range(16):
        # a pellet every 3px, with a dark chamfer gap between
        row = y % 3
        for x in range(16):
            col = x % 4
            if col == 0:
                c = cladding[2]
            elif col == 3:
                c = cladding[0]
            elif row == 2:
                c = ceramic[3]
            else:
                n = fbm(x, y, seed, 16, 16, 2, 4.0)
                c = ceramic[0] if (col == 1 and row == 0) else ceramic[1 if n < 0.6 else 2]
            t.set(x, y, c)
    return t


@block("uranium_rod_pellets_fresh")
def uranium_rod_pellets_fresh():
    # Fresh oxide: the bright uranium green of the mod's fuel.
    ceramic = [URANIUM[5], URANIUM[4], URANIUM[3], URANIUM[0]]
    return _pellets(905, ceramic, ZIRCALOY[3:])


@block("uranium_rod_pellets_half")
def uranium_rod_pellets_half():
    # Half burnt: olive, the fission products dulling it.
    ceramic = [mix(URANIUM[4], DARK[4], 0.35), mix(URANIUM[3], DARK[3], 0.45), mix(URANIUM[2], DARK[3], 0.5), DARK[1]]
    return _pellets(906, ceramic, ZIRCALOY[2:])


@block("uranium_rod_pellets_spent")
def uranium_rod_pellets_spent():
    # Nearly spent: a dark brown-grey ceramic in tarnished cladding.
    brown = (92, 70, 54, 255)
    ceramic = [mix(brown, DARK[5], 0.3), mix(brown, DARK[2], 0.35), mix(brown, DARK[1], 0.55), DARK[0]]
    return _pellets(907, ceramic, ZIRCALOY[1:])


@block("uranium_rod_water")
def uranium_rod_water():
    """Coolant in the channel: drawn translucent by its model, so only its tone and ripples matter."""
    t = Tex(seed=908)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 908, 16, 16, 3, 5.0)
            t.set(x, y, MIMIC_BLUE[5] if n < 0.45 else MIMIC_BLUE[6] if n < 0.7 else CYAN[6])
    # rising bubbles
    for x, y in ((3, 12), (4, 9), (3, 6), (11, 13), (12, 10), (11, 4), (7, 2)):
        t.set(x, y, CYAN[6])
    return t


@block("uranium_rod_cherenkov")
def uranium_rod_cherenkov():
    """The Cherenkov glow round running fuel: electric blue streaks rising along the pins, clear
    between them so the green fuel shows through. Drawn translucent and at full light by its
    models; brighter models only raise its alpha."""
    t = Tex(seed=909)
    for y in range(16):
        for x in range(16):
            t.set(x, y, CLEAR)
    # One streak by each pin's edge (pins repeat every 4px), broken up as it rises.
    for x in (0, 3, 4, 7, 8, 11, 12, 15):
        for y in range(16):
            n = fbm(x * 0.5, y * 0.3, 909, 16, 16, 2, 4.0)
            if n > 0.38:
                t.set(x, y, mix(MIMIC_BLUE[6], WHITE, 0.35) if n > 0.62 else MIMIC_BLUE[5])
    return t

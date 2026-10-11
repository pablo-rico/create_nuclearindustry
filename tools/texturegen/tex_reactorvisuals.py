"""Textures for the visible and audible fission reactor: the casing viewport and the fuel channels.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.

The fuel channel (``blockstates/uranium_rod.json``) is a multipart of hand-written models:

* ``uranium_rod_sheath``: zirconium posts and grid plates, always drawn (cutout). The plates use
  ``uranium_rod_plate`` (a spacer grid with holes) and the open sides ``uranium_rod_cage`` (spacer
  bands with clear windows between them); the posts sample ``uranium_rod_sheath``.
* ``uranium_rod_pellets_<fresh|half|spent>``: four fuel pins, 4px wide, sampled at u 0..4, 4..8,
  8..12 or 12..16, so the pin shading repeats every 4px. Darker as the fuel burns.
* ``uranium_rod_water`` and ``uranium_rod_cherenkov``: opaque here; the models draw them translucent
  with a vertex alpha (``neoforge_data.color``), the Cherenkov glow at full block and sky light.
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
CUTOUT_BLOCKS["uranium_rod_plate"] = (16, 16)
CUTOUT_BLOCKS["uranium_rod_cage"] = (16, 16)


# ======================================================================================
# viewport
# ======================================================================================

@block("reactor_viewport")
def reactor_viewport():
    """A thick rolled-steel frame bolted round a lead-glass pane, matching the reactor casing."""
    steel = REACTOR_STEEL
    t = Tex(seed=901)
    bevel(t, 0, 0, 15, 15, steel, hi=steel[5], lo=steel[1])
    bevel(t, 1, 1, 14, 14, steel, hi=steel[4], lo=steel[2], corner=False)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(t, x, y, steel)
    t.frame(2, 2, 13, 13, steel[0])
    # Lead glass is faintly amber: its retaining ring is, lit top-left, shadowed bottom-right.
    amber_hi = mix(WARN[5], LEAD[5], 0.45)
    amber_lo = mix(WARN[2], LEAD[2], 0.55)
    t.hline(3, 12, 3, amber_hi)
    t.vline(3, 3, 12, amber_hi)
    t.hline(3, 12, 12, amber_lo)
    t.vline(12, 3, 12, amber_lo)
    t.rect(4, 4, 11, 11, CLEAR)
    # Two glints on the glass, opaque so cutout rendering keeps them.
    t.set(5, 5, WHITE)
    t.set(6, 4, amber_hi)
    t.set(4, 6, amber_hi)
    t.set(9, 10, amber_hi)
    t.set(10, 9, amber_hi)
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
    ceramic = [URANIUM[6], URANIUM[5], URANIUM[4], URANIUM[1]]
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
            t.set(x, y, MIMIC_BLUE[3] if n < 0.45 else MIMIC_BLUE[4] if n < 0.7 else CYAN[4])
    # rising bubbles
    for x, y in ((3, 12), (4, 9), (3, 6), (11, 13), (12, 10), (11, 4), (7, 2)):
        t.set(x, y, CYAN[6])
    return t


@block("uranium_rod_cherenkov")
def uranium_rod_cherenkov():
    """The Cherenkov glow round running fuel: bright electric blue, streaked upwards.
    Drawn translucent and at full light by its models; brighter models only raise its alpha."""
    t = Tex(seed=909)
    for y in range(16):
        for x in range(16):
            n = fbm(x * 0.5, y * 0.15, 909, 16, 16, 2, 4.0)
            t.set(x, y, MIMIC_BLUE[6] if n > 0.62 else MIMIC_BLUE[5] if n > 0.4 else MIMIC_BLUE[4])
    for x in (2, 6, 9, 13):
        for y in range(16):
            if (y + x) % 5 != 0:
                t.set(x, y, mix(MIMIC_BLUE[6], WHITE, 0.45))
    return t

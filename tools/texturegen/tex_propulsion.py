"""Textures of the nuclear engines for Create Aeronautics ships.

Register block textures with ``@block``; this module is imported at the end of ``blocks.py``.

The nuclear thermal engine is steel plating with brass flanges and copper exchanger tubes; its
nozzle is heat-tinted steel, straw to blue towards the mouth. The antimatter engine is dark titanium
with copper windings and a violet field glow, the colour of the antimatter elsewhere in the mod. The
controllers' operator panel is on the face away from the exhaust (``*_front``), the face towards the
segments is a flange (``*_back``). The nozzles' mouths are the open faces the jet leaves from: dark
inside when cold, glowing when lit.
"""

import math

from blocks import block
from common import casing
from fusion_materials import TITANIUM
from palette import BRASS, COPPER, DANGER, DARK, STEEL, WARN
from pixelkit import Ramp, Tex, bevel, brushed, fbm, glow, hazard, outline, shade

# Antimatter violet, as the medium trap's band.
VIOLET = Ramp("#2e1f45", "#46306a", "#614a8f", "#7e68b0", "#9b5cff", "#c2a2ff", "#eadcff")
# Steel after the heat: straw, bronze, purple and blue, the colours a hot nozzle takes.
HEAT = [(176, 150, 96, 255), (150, 104, 62, 255), (112, 74, 108, 255), (70, 88, 140, 255), (58, 70, 96, 255)]
FLAME = Ramp("#5b2a10", "#8a3d12", "#c25a12", "#e8842a", "#ffb04a", "#ffd88a", "#fff6d8")


# ======================================================================================
# nuclear thermal engine
# ======================================================================================

@block("nuclear_thermal_engine_side")
def thermal_side():
    """Riveted steel plating crossed by a brass coolant manifold."""
    t = casing(Tex(seed=1900), STEEL, seed=1900, panel=(4, 4, 11, 11))
    t.rect(3, 6, 12, 9, BRASS[3])
    t.hline(3, 12, 6, BRASS[5])
    t.hline(3, 12, 9, BRASS[1])
    for x in (4, 8, 11):
        t.set(x, 7, BRASS[6])
    return t


@block("nuclear_thermal_engine_front")
def thermal_front():
    """The operator panel: a throttle dial and a nozzle-temperature dial over a hazard band."""
    t = casing(Tex(seed=1901), STEEL, seed=1901, rivets=True, seam=False)
    t.rect(3, 3, 12, 9, DARK[1])
    outline(t, 3, 3, 12, 9, DARK[0])
    for cx, needle, c in ((5.5, (4, 4), WARN[5]), (10.5, (12, 4), DANGER[5])):
        t.ring(cx, 6.5, 2.6, 1.9, STEEL[5])
        t.disc(cx, 6.5, 1.8, DARK[3])
        t.line(int(cx), 6, needle[0] if needle[0] < 12 else 11, needle[1] + 1, c)
    hazard(t, 3, 11, 12, 12, WARN[4], DARK[1], period=4, seed=1901)
    t.set(4, 10, (90, 230, 120, 255))
    t.set(11, 10, DANGER[5])
    return t


@block("nuclear_thermal_engine_back")
def thermal_back():
    """The flange the exchanger bolts on to: a ring of bolts round the hot-gas duct."""
    t = Tex(seed=1902)
    brushed(t, STEEL, base=3.4, amp=0.6, seed=1902)
    outline(t, 0, 0, 15, 15, STEEL[1])
    bevel(t, 1, 1, 14, 14, STEEL, hi=STEEL[5], lo=STEEL[1])
    t.disc(7.5, 7.5, 5.5, BRASS[3])
    t.ring(7.5, 7.5, 5.5, 4.8, BRASS[5])
    t.disc(7.5, 7.5, 3.4, DARK[1])
    t.ring(7.5, 7.5, 3.4, 2.8, DARK[3])
    for a in range(8):
        x = 7.5 + math.cos(a * math.pi / 4) * 4.2
        y = 7.5 + math.sin(a * math.pi / 4) * 4.2
        t.set(int(x), int(y), BRASS[6])
    return t


@block("thermal_engine_exchanger_side")
def exchanger_side():
    """A heat exchanger section along the engine's axis (up the texture): steel shell with copper
    tubes showing between its ribs."""
    t = Tex(seed=1903)
    brushed(t, STEEL, base=3.2, amp=0.6, seed=1903, axis="v")
    for x in (2, 6, 9, 13):
        for y in range(16):
            n = (fbm(x, y, 1903, 16, 16, 2, 5.0) - 0.5) * 0.8
            t.set(x, y, COPPER[int(round(4.2 + n))])
            t.set(x + 1, y, COPPER[2])
    for y in (0, 15):
        t.hline(0, 15, y, STEEL[1])
    for y in (4, 11):
        t.hline(0, 15, y, STEEL[5])
        t.hline(0, 15, y + 1, STEEL[2])
    t.vline(0, 0, 15, STEEL[1])
    t.vline(15, 0, 15, STEEL[5])
    return t


@block("thermal_engine_exchanger_end")
def exchanger_end():
    """The tube sheet: a grid of tube ends inside a brass rim."""
    t = Tex(seed=1904)
    brushed(t, STEEL, base=3.0, amp=0.5, seed=1904)
    t.ring(7.5, 7.5, 7.6, 6.6, BRASS[4])
    t.ring(7.5, 7.5, 6.6, 6.0, BRASS[2])
    for y in range(3, 13, 3):
        for x in range(3, 13, 3):
            if math.hypot(x + 0.5 - 7.5, y + 0.5 - 7.5) < 5.6:
                t.rect(x, y, x + 1, y + 1, COPPER[2])
                t.set(x, y, COPPER[5])
    return t


@block("thermal_rocket_nozzle_side")
def thermal_nozzle_side():
    """The bell's outside, heat-tinted from straw near the throat (top) to blue at the lip."""
    t = Tex(seed=1905)
    for y in range(16):
        band = HEAT[min(len(HEAT) - 1, y * len(HEAT) // 16)]
        for x in range(16):
            n = fbm(x, y, 1905, 16, 16, 2, 5.0) - 0.5
            t.set(x, y, shade(band, 1.0 + n * 0.25))
    for x in (0, 8):
        t.vline(x, 0, 15, shade(HEAT[2], 0.7))
    t.hline(0, 15, 15, STEEL[1])
    t.hline(0, 15, 0, STEEL[5])
    return t


def _mouth(t, lit, ramp_hot, rim):
    # The plate round the bell fills the corners: block textures are opaque.
    brushed(t, rim, base=2.4, amp=0.5, seed=t.seed if hasattr(t, "seed") else 0)
    outline(t, 0, 0, 15, 15, rim[0])
    t.disc(7.5, 7.5, 7.6, rim[2])
    t.ring(7.5, 7.5, 7.6, 6.9, rim[4])
    t.ring(7.5, 7.5, 6.9, 6.3, rim[1])
    if lit:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - 7.5, y + 0.5 - 7.5)
                if d < 6.3:
                    t.set(x, y, ramp_hot[max(0, min(6, int(6.6 - d)))])
    else:
        t.disc(7.5, 7.5, 6.3, DARK[0])
        t.ring(7.5, 7.5, 4.2, 3.6, DARK[2])
        t.disc(7.5, 7.5, 1.8, DARK[1])
    return t


@block("thermal_rocket_nozzle_mouth")
def thermal_mouth():
    """The open bell seen from behind: dark throat inside a heat-blued lip."""
    return _mouth(Tex(seed=1906), False, FLAME, STEEL)


@block("thermal_rocket_nozzle_mouth_lit")
def thermal_mouth_lit():
    """Firing: white-hot at the throat, orange towards the lip."""
    return _mouth(Tex(seed=1907), True, FLAME, STEEL)


@block("thermal_rocket_nozzle_plate")
def thermal_nozzle_plate():
    """The mounting plate the bell hangs from, with its bolts."""
    t = casing(Tex(seed=1908), STEEL, seed=1908, seam=False)
    t.disc(7.5, 7.5, 3.5, HEAT[0])
    t.ring(7.5, 7.5, 3.5, 2.8, STEEL[2])
    return t


# ======================================================================================
# antimatter engine
# ======================================================================================

@block("antimatter_engine_side")
def antimatter_side():
    """Dark titanium plating with a violet field seam."""
    t = casing(Tex(seed=1910), TITANIUM, seed=1910, panel=(4, 4, 11, 11))
    t.hline(3, 12, 7, VIOLET[4])
    t.hline(3, 12, 8, VIOLET[2])
    for x in (4, 7, 10):
        t.set(x, 7, VIOLET[6])
    return t


@block("antimatter_engine_front")
def antimatter_front():
    """The operator panel: a viewport on the annihilation chamber's violet glow and a quench lamp."""
    t = casing(Tex(seed=1911), TITANIUM, seed=1911, seam=False)
    t.rect(4, 3, 11, 9, DARK[0])
    outline(t, 3, 2, 12, 10, TITANIUM[1])
    glow(t, 7.5, 6.0, 3.6, VIOLET[4], strength=0.9)
    t.disc(7.5, 6.0, 1.2, VIOLET[6])
    hazard(t, 3, 12, 12, 12, VIOLET[3], DARK[1], period=4, seed=1911)
    t.set(4, 11, DANGER[5])
    t.set(11, 11, (90, 230, 120, 255))
    return t


@block("antimatter_engine_back")
def antimatter_back():
    """The flange towards the coils: the beam port inside a ring of field couplings."""
    t = Tex(seed=1912)
    brushed(t, TITANIUM, base=3.0, amp=0.5, seed=1912)
    outline(t, 0, 0, 15, 15, TITANIUM[1])
    bevel(t, 1, 1, 14, 14, TITANIUM, hi=TITANIUM[5], lo=TITANIUM[1])
    t.disc(7.5, 7.5, 5.4, COPPER[2])
    t.ring(7.5, 7.5, 5.4, 4.6, COPPER[5])
    t.disc(7.5, 7.5, 3.2, DARK[0])
    glow(t, 7.5, 7.5, 2.6, VIOLET[4], strength=0.8)
    return t


@block("magnetic_coil_side")
def coil_side():
    """A magnetic coil along the engine's axis (up the texture): copper windings, one turn on each
    two rows, between titanium bands with a violet field line."""
    t = Tex(seed=1913)
    for y in range(16):
        for x in range(16):
            n = (fbm(x, y, 1913, 16, 16, 2, 6.0) - 0.5) * 1.0
            t.set(x, y, COPPER[int(round((4.6 if x % 2 == 0 else 2.4) + n))])
    for y in (0, 1, 14, 15):
        t.hline(0, 15, y, TITANIUM[3] if y in (1, 14) else TITANIUM[1])
    t.hline(0, 15, 7, VIOLET[4])
    t.hline(0, 15, 8, VIOLET[2])
    return t


@block("magnetic_coil_end")
def coil_end():
    """The coil seen end-on: the turns as rings round the beam bore."""
    t = Tex(seed=1914)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 7.5, y + 0.5 - 7.5)
            t.set(x, y, COPPER[5] if int(d) % 2 == 0 else COPPER[2])
    t.ring(7.5, 7.5, 8.5, 7.2, TITANIUM[2])
    t.disc(7.5, 7.5, 2.4, DARK[0])
    t.ring(7.5, 7.5, 2.4, 1.8, VIOLET[4])
    return t


@block("magnetic_nozzle_side")
def magnetic_nozzle_side():
    """The magnetic nozzle's field rings round a titanium bell."""
    t = Tex(seed=1915)
    brushed(t, TITANIUM, base=2.8, amp=0.5, seed=1915, axis="v")
    for y in (2, 7, 12):
        t.hline(0, 15, y, COPPER[4])
        t.hline(0, 15, y + 1, COPPER[2])
    t.hline(0, 15, 15, TITANIUM[0])
    t.hline(0, 15, 0, TITANIUM[5])
    return t


@block("magnetic_nozzle_mouth")
def magnetic_mouth():
    """The open field funnel seen from behind, dark when the engine is cold."""
    return _mouth(Tex(seed=1916), False, VIOLET, TITANIUM)


@block("magnetic_nozzle_mouth_lit")
def magnetic_mouth_lit():
    """Firing: the pion stream, white at the core and violet at the edge."""
    return _mouth(Tex(seed=1917), True, VIOLET, TITANIUM)


@block("magnetic_nozzle_plate")
def magnetic_nozzle_plate():
    """The plate the nozzle mounts on."""
    t = casing(Tex(seed=1918), TITANIUM, seed=1918, seam=False)
    t.disc(7.5, 7.5, 3.0, DARK[0])
    t.ring(7.5, 7.5, 3.0, 2.4, VIOLET[3])
    return t

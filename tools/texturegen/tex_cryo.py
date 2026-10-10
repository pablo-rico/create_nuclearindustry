"""Textures for the cryogenic plant, the tokamak's vacuum pumps and cryogenic ports.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.

The plant reads as a cold box: pale insulated panels on a steel frame, frost on the seams and
blue-white accents. The tokamak parts keep the titanium fusion language of ``fusion_materials``.
"""

import math

from blocks import block  # noqa: F401
from common import bolt, casing, port_face
from fusion_materials import COPPER as FCOPPER, GRAPHITE, INLET, OUTLET, TITANIUM, panel, recess
from gui import gui  # noqa: F401
from items import item  # noqa: F401
from palette import BRASS, CRYO, CYAN, DARK, MIMIC_BLUE, PLASMA, STEEL, WARN, WHITE
from pixelkit import Tex, bevel, brushed, fbm, glow, mix, outline, vents

# Frost: near-white with a blue cast, laid over cold metal.
FROST = (226, 240, 246, 255)
FROST_DIM = (186, 210, 222, 255)


def _frost(t, seed, amount=0.12, rows=None):
    """Scattered frost crystals, denser along the given rows (seams collect it first)."""
    rng = t.rng
    for y in range(16):
        for x in range(16):
            bias = 2.2 if rows and y in rows else 1.0
            if fbm(x, y, seed, 16, 16, 2, 3.0) > 1.0 - amount * bias and rng.random() < 0.7:
                t.set(x, y, mix(t.get(x, y), FROST, 0.65))


# ======================================================================================
# cryogenic plant: an insulated cold box
# ======================================================================================

def _cold_panel(seed):
    """Insulated plant casing: a steel frame around a pale vacuum-jacketed panel."""
    t = Tex(seed=seed)
    casing(t, STEEL, seed=seed, seam=False)
    # The jacket: a pale insulated panel with a vertical quilting of welds.
    for y in range(3, 13):
        for x in range(3, 13):
            n = fbm(x, y, seed, 16, 16, 2, 5.0) - 0.5
            t.set(x, y, CRYO[max(2, min(6, int(round(4.4 + n * 1.4))))])
    for x in (5, 10):
        t.vline(x, 3, 12, CRYO[2])
        t.vline(x + 1, 3, 12, CRYO[5])
    outline(t, 3, 3, 12, 12, CRYO[1])
    _frost(t, seed + 7, 0.08, rows=(12, 13))
    return t


@block("cryoplant_casing")
def cryoplant_casing():
    return _cold_panel(900)


@block("cryoplant_casing_top")
def cryoplant_casing_top():
    t = Tex(seed=901)
    brushed(t, STEEL, base=3.2, amp=0.6, seed=901)
    # A manway flange on the cold box lid, bolted down, with frost on its rim.
    t.disc(8, 8, 6.2, CRYO[3])
    t.ring(8, 8, 6.2, 5.2, STEEL[5])
    t.ring(8, 8, 6.6, 6.2, STEEL[1])
    for a in range(0, 360, 45):
        r = math.radians(a)
        t.set(round(8 + math.cos(r) * 5.7 - 0.5), round(8 + math.sin(r) * 5.7 - 0.5), STEEL[6])
    t.disc(8, 8, 3.6, CRYO[5])
    t.ring(8, 8, 3.6, 2.8, CRYO[2])
    t.disc(8, 8, 1.2, STEEL[2])
    outline(t, 0, 0, 15, 15, STEEL[1])
    bevel(t, 1, 1, 14, 14, STEEL, hi=STEEL[5], lo=STEEL[0])
    _frost(t, 902, 0.1)
    return t


def _controller(running):
    t = _cold_panel(903)
    # Instrument panel: a cold-end temperature readout and a compressor speed dial.
    t.rect(3, 3, 12, 12, STEEL[2])
    bevel(t, 3, 3, 12, 12, STEEL)
    outline(t, 2, 2, 13, 13, STEEL[0])
    screen = CYAN if running else DARK
    t.rect(4, 4, 11, 7, DARK[0])
    t.rect(5, 5, 10, 6, screen[2] if running else DARK[1])
    if running:
        # "4.5 K": three lit digit blocks and a K
        for x in (5, 7, 9):
            t.set(x, 5, CYAN[6])
            t.set(x, 6, CYAN[5])
        t.set(10, 5, WHITE)
    t.disc(6.5, 10.0, 1.9, DARK[1])
    t.ring(6.5, 10.0, 1.9, 1.3, BRASS[4])
    t.line(6, 10, 7 if running else 5, 9, WARN[6] if running else WARN[2])
    # Status lamp: cyan when running, dark otherwise.
    t.rect(9, 9, 10, 10, CYAN[6] if running else DARK[2])
    if running:
        t.set(9, 9, WHITE)
        glow(t, 10, 10, 2.5, CYAN[5], 0.25)
    return t


@block("cryoplant_controller")
def cryoplant_controller():
    return _controller(False)


@block("cryoplant_controller_running")
def cryoplant_controller_running():
    return _controller(True)


@block("cryoplant_port_helium_in")
def cryoplant_port_helium_in():
    t = port_face(Tex(seed=910), CRYO, PLASMA, seed=910)
    _frost(t, 910, 0.06)
    return t


@block("cryoplant_port_liquid_helium_out")
def cryoplant_port_liquid_helium_out():
    t = port_face(Tex(seed=911), CRYO, CYAN, seed=911, out=True)
    _frost(t, 911, 0.12)
    return t


@block("cryoplant_port_nitrogen_in")
def cryoplant_port_nitrogen_in():
    t = port_face(Tex(seed=912), CRYO, MIMIC_BLUE, seed=912, square=True)
    _frost(t, 912, 0.08)
    return t


@block("cryoplant_port_liquid_air_out")
def cryoplant_port_liquid_air_out():
    t = port_face(Tex(seed=913), CRYO, WARN, seed=913, out=True, square=True)
    # intake louvres along the bottom edge
    for x in range(3, 13, 2):
        t.set(x, 13, DARK[0])
        t.set(x, 14, CRYO[1])
    _frost(t, 913, 0.06)
    return t


# -------------------------------------------------------------------- helium compressor

@block("cryoplant_compressor_front")
def cryoplant_compressor_front():
    """Bolted frame around the 8x8 impeller opening (the opening itself is never drawn)."""
    t = Tex(seed=920)
    brushed(t, STEEL, base=3.4, amp=0.6, seed=920)
    t.rect(4, 4, 11, 11, DARK[0])
    t.frame(3, 3, 12, 12, STEEL[1])
    outline(t, 0, 0, 15, 15, STEEL[1])
    bevel(t, 1, 1, 14, 14, STEEL, hi=STEEL[5], lo=STEEL[0])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, STEEL)
    # cyan rating band across the top: a cold machine
    t.hline(5, 10, 1, CYAN[4])
    t.hline(5, 10, 2, CYAN[2])
    return t


@block("cryoplant_compressor_side")
def cryoplant_compressor_side():
    t = Tex(seed=921)
    casing(t, STEEL, seed=921, seam=False)
    # cooling fins of the compressor housing
    vents(t, 3, 4, 12, 11, STEEL, step=2)
    t.hline(3, 12, 3, CRYO[4])
    t.hline(3, 12, 12, CRYO[2])
    _frost(t, 921, 0.05)
    return t


@block("cryoplant_compressor_recess")
def cryoplant_compressor_recess():
    """The impeller housing seen through the opening: a dark volute with a radial grille."""
    t = Tex(seed=922)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            t.set(x, y, DARK[1] if d < 4.6 else DARK[2])
    for a in range(0, 360, 30):
        r = math.radians(a)
        for s in (2.5, 3.5):
            t.set(round(8 + math.cos(r) * s - 0.5), round(8 + math.sin(r) * s - 0.5), DARK[0])
    t.ring(8, 8, 4.6, 3.9, STEEL[1])
    return t


@block("cryoplant_compressor_back")
def cryoplant_compressor_back():
    return _cold_panel(923)


@block("cryoplant_compressor_impeller")
def cryoplant_compressor_impeller():
    t = Tex(seed=924)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 924, 16, 16, 2, 4.0)
            t.set(x, y, BRASS[3 if n < 0.5 else 4])
    for y in (0, 15):
        t.hline(0, 15, y, BRASS[1])
    t.vline(0, 0, 15, BRASS[5])
    return t


@block("cryoplant_compressor_impeller_hub")
def cryoplant_compressor_impeller_hub():
    t = Tex(seed=925)
    brushed(t, STEEL, base=3.6, amp=0.5, seed=925)
    t.disc(8, 8, 4, STEEL[5])
    t.disc(8, 8, 2, STEEL[2])
    outline(t, 0, 0, 15, 15, STEEL[1])
    return t


# ======================================================================================
# tokamak shell parts, in the fusion hardware language
# ======================================================================================

def _cryo_port(output):
    t = panel()
    recess(t)
    # an insulated bayonet coupling: frosty ring for liquid in, warm ring for gas out
    ring = (170, 214, 232, 255) if not output else OUTLET
    t.disc(8, 8, 3.4, GRAPHITE[3])
    t.ring(8, 8, 3.4, 2.6, ring)
    t.disc(8, 8, 1.6, GRAPHITE[0])
    t.set(6, 6, TITANIUM[5])
    color = INLET if not output else OUTLET
    t.hline(6, 9, 12, color)
    t.set(8 if output else 7, 11 if output else 13, color)
    if not output:
        # frost creeping out of the liquid helium line
        for x, y in ((4, 4), (11, 5), (5, 11), (12, 10), (3, 8)):
            t.set(x, y, FROST)
        t.set(10, 3, FROST_DIM)
        t.set(2, 12, FROST_DIM)
    return t


@block("fusion_cryo_port_input")
def fusion_cryo_port_input():
    return _cryo_port(False)


@block("fusion_cryo_port_output")
def fusion_cryo_port_output():
    return _cryo_port(True)


@block("fusion_vacuum_pump_front")
def fusion_vacuum_pump_front():
    """Titanium frame around the 10x10 rotor opening (the opening itself is never drawn)."""
    t = panel()
    t.rect(3, 3, 12, 12, GRAPHITE[0])
    t.frame(2, 2, 13, 13, GRAPHITE[1])
    for x, y in ((0, 0), (14, 0), (0, 14), (14, 14)):
        t.set(x, y, TITANIUM[5])
        t.set(x + 1, y + 1, TITANIUM[1])
    t.hline(5, 10, 0, INLET)
    return t


@block("fusion_vacuum_pump_side")
def fusion_vacuum_pump_side():
    t = panel()
    recess(t, (3, 3, 12, 12))
    # the pump's cooling jacket: copper coolant turns
    for y in range(4, 12, 2):
        t.hline(4, 11, y, FCOPPER[4])
        t.hline(4, 11, y + 1, FCOPPER[1])
    return t


@block("fusion_vacuum_pump_recess")
def fusion_vacuum_pump_recess():
    """The stator behind the rotor: concentric rings of fixed blades."""
    t = Tex(seed=930)
    for y in range(16):
        for x in range(16):
            t.set(x, y, GRAPHITE[1])
    for r in (5.2, 3.6, 2.0):
        t.ring(8, 8, r, r - 0.6, GRAPHITE[3])
    t.disc(8, 8, 1.0, GRAPHITE[0])
    return t


@block("fusion_vacuum_pump_back")
def fusion_vacuum_pump_back():
    t = panel()
    recess(t, (5, 5, 10, 10))
    t.disc(8, 8, 2, GRAPHITE[3])
    t.set(7, 7, TITANIUM[5])
    return t


@block("fusion_vacuum_pump_rotor")
def fusion_vacuum_pump_rotor():
    t = Tex(seed=931)
    for y in range(16):
        for x in range(16):
            t.set(x, y, TITANIUM[(5, 4, 3, 4)[(x + y) % 4]])
    return t


@block("fusion_vacuum_pump_rotor_hub")
def fusion_vacuum_pump_rotor_hub():
    t = Tex(seed=932)
    for y in range(16):
        for x in range(16):
            t.set(x, y, FCOPPER[3])
    t.disc(8, 8, 4, FCOPPER[5])
    t.disc(8, 8, 2, GRAPHITE[1])
    return t

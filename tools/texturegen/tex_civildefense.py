"""Textures for sirens, the seismic detector, blast doors and shelters.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.
"""

from blocks import block  # noqa: F401
from gui import gui  # noqa: F401
from items import item  # noqa: F401

from common import bolt, casing, panel_screen
from palette import BRASS, CARBONATE, DANGER, DARK, LEAD, RADIUM, STEEL, URANIUM, WARN
from pixelkit import Tex, bevel, brushed, hazard, inset, mix, outline, rivet, shade, speckle


# ======================================================================================
# air raid sirens
# ======================================================================================

# Sirens are painted signal red over steel, weathered at the edges.
SIREN_RED = DANGER.tinted(STEEL[2], 0.15)


@block("air_raid_siren_housing")
def air_raid_siren_housing():
    # Rotor housing (the model uses the middle, x 3-12 and y 4-11): red sheet with the ports the
    # rotor chops the air through, between steel rims.
    t = Tex(seed=500)
    brushed(t, SIREN_RED, base=3.2, amp=0.6, seed=500, axis="v")
    for y in (3, 4):
        t.hline(0, 15, y, STEEL[2] if y == 3 else STEEL[4])
    for y in (11, 12):
        t.hline(0, 15, y, STEEL[4] if y == 11 else STEEL[1])
    for x in range(4, 13, 2):
        t.vline(x, 5, 10, DARK[0])
        t.set(x, 5, DARK[1])
        t.vline(x + 1, 6, 10, SIREN_RED[5])
    t.vline(3, 5, 10, SIREN_RED[2])
    for x in (3, 12):
        rivet(t, x, 4, STEEL)
    outline(t, 0, 0, 15, 15, SIREN_RED[0])
    return t


@block("air_raid_siren_cap")
def air_raid_siren_cap():
    # The rain cap and finial: plain red sheet, lit from the top-left.
    t = Tex(seed=501)
    brushed(t, SIREN_RED, base=3.4, amp=0.5, seed=501)
    bevel(t, 1, 1, 14, 14, SIREN_RED, hi=SIREN_RED[5], lo=SIREN_RED[1])
    outline(t, 0, 0, 15, 15, SIREN_RED[0])
    t.disc(7.5, 7.5, 2.2, SIREN_RED[2])
    t.disc(7.0, 7.0, 1.0, SIREN_RED[5])
    return t


@block("air_raid_siren_base")
def air_raid_siren_base():
    # The mounting pole: ribbed galvanised steel.
    t = Tex(seed=502)
    brushed(t, STEEL, base=3.0, amp=0.7, seed=502, axis="v")
    for x in (5, 9):
        t.vline(x, 0, 15, STEEL[1])
        t.vline(x + 1, 0, 15, STEEL[5])
    outline(t, 0, 0, 15, 15, STEEL[0])
    return t


@block("hand_crank_siren_base")
def hand_crank_siren_base():
    # The gearbox under a hand-crank siren: brass teeth along its sides (rows 12-15 show on the
    # model) and the shaft socket on its bottom face.
    t = Tex(seed=503)
    brushed(t, STEEL, base=3.0, amp=0.6, seed=503)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    for x in range(1, 15):
        t.set(x, 13, BRASS[4] if x % 2 else BRASS[2])
        t.set(x, 14, BRASS[2] if x % 2 else BRASS[1])
    t.disc(7.5, 7.5, 3.2, DARK[0])
    t.ring(7.5, 7.5, 3.2, 2.4, BRASS[4])
    t.rect(6, 6, 9, 9, DARK[2])
    return t


# ======================================================================================
# seismic detector
# ======================================================================================

@block("seismic_detector_side")
def seismic_detector_side():
    # Dark instrument case with a small screen showing the last trace, and three status lamps.
    t = Tex(seed=510)
    brushed(t, DARK, base=3.0, amp=0.5, seed=510)
    bevel(t, 0, 0, 15, 15, DARK)
    outline(t, 0, 0, 15, 15, DARK[0])
    panel_screen(t, 3, 3, 12, 9, DARK)
    t.hline(3, 12, 6, mix(DARK[1], RADIUM[2], 0.5))
    points = ((3, 6), (5, 6), (6, 4), (7, 8), (8, 5), (9, 7), (10, 6), (12, 6))
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        t.line(x0, y0, x1, y1, RADIUM[5])
    for x, c in ((4, RADIUM[5]), (7, WARN[5]), (10, DANGER[4])):
        t.rect(x, 12, x + 1, 13, c)
    for x, y in ((1, 1), (13, 1)):
        bolt(t, x, y, DARK)
    return t


@block("seismic_detector_top")
def seismic_detector_top():
    # The recording drum: chart paper between two rollers, inked with a quake.
    t = Tex(seed=511)
    brushed(t, DARK, base=2.6, amp=0.4, seed=511)
    outline(t, 0, 0, 15, 15, DARK[0])
    for y in range(3, 13):
        for x in range(1, 15):
            t.set(x, y, CARBONATE[5] if (x + y) % 5 else CARBONATE[4])
    for x in range(1, 15):
        t.set(x, 2, BRASS[3])
        t.set(x, 13, BRASS[2])
    for x in (4, 8, 12):
        t.vline(x, 3, 12, CARBONATE[3])
    points = ((1, 8), (4, 8), (5, 6), (6, 10), (7, 4), (8, 11), (9, 6), (10, 9), (11, 8), (14, 8))
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        t.line(x0, y0, x1, y1, DARK[0])
    t.rect(11, 3, 12, 5, DANGER[3])
    t.set(11, 3, DANGER[5])
    return t


# ======================================================================================
# blast doors
# ======================================================================================

def _frame(seed):
    # Heavy dark plate, the house casing in the trim metal, around which the leaves sit.
    t = Tex(seed=seed)
    casing(t, DARK, seed=seed, base=3.0, amp=0.7)
    return t


@block("blast_door_frame")
def blast_door_frame():
    t = _frame(520)
    # Hazard chevrons on the inner plate warn of the moving door.
    hazard(t, 5, 5, 10, 10, WARN[4], DARK[0], period=4, seed=520)
    outline(t, 4, 4, 11, 11, DARK[0])
    return t


@block("blast_door_controller")
def blast_door_controller():
    # The frame with the drive's status lamps and a position gauge.
    t = _frame(521)
    t.rect(4, 4, 11, 11, DARK[0])
    inset(t, 4, 4, 11, 11, DARK)
    t.rect(5, 5, 10, 6, DARK[1])
    t.rect(5, 5, 8, 6, RADIUM[4])
    t.set(5, 5, RADIUM[6])
    t.rect(5, 9, 6, 10, RADIUM[5])
    t.rect(9, 9, 10, 10, DANGER[3])
    t.set(5, 9, RADIUM[6])
    return t


@block("blast_door_controller_shaft")
def blast_door_controller_shaft():
    # Where the drive shaft goes through the wall: a brass-bushed socket in the frame.
    t = _frame(522)
    t.rect(4, 4, 11, 11, DARK[1])
    t.disc(7.5, 7.5, 3.6, DARK[0])
    t.ring(7.5, 7.5, 3.6, 2.6, BRASS[4])
    t.ring(7.5, 7.5, 2.6, 2.0, BRASS[2])
    t.rect(6, 6, 9, 9, DARK[2])
    t.hline(6, 9, 6, DARK[4])
    return t


@block("steel_blast_door")
def steel_blast_door():
    # A thick welded plate: seams at the block edges, two riveted stiffening ribs and a band of
    # hazard stripes, so a whole door reads as one striped slab.
    t = Tex(seed=530)
    brushed(t, STEEL, base=3.1, amp=0.7, seed=530, axis="v")
    for y, (hi, lo) in ((2, (STEEL[5], STEEL[1])), (12, (STEEL[5], STEEL[1]))):
        t.hline(0, 15, y, hi)
        t.hline(0, 15, y + 1, lo)
        for x in (2, 7, 12):
            t.set(x, y, STEEL[6])
            t.set(x + 1, y + 1, STEEL[0])
    hazard(t, 0, 6, 15, 9, WARN[4], DARK[1], period=6, seed=530)
    t.hline(0, 15, 5, STEEL[1])
    t.hline(0, 15, 10, STEEL[5])
    outline(t, 0, 0, 15, 15, STEEL[1])
    return t


@block("lead_blast_door")
def lead_blast_door():
    # Lead sheets lapped over a steel core: dull grey with white oxide bloom, riveted at the laps.
    t = Tex(seed=531)
    brushed(t, LEAD, base=2.8, amp=0.5, seed=531)
    speckle(t, 1, 1, 14, 14, mix(LEAD[5], CARBONATE[6], 0.3), chance=0.05, seed=531)
    for y in (5, 10):
        t.hline(0, 15, y, LEAD[0])
        t.hline(0, 15, y + 1, LEAD[4])
        for x in (1, 5, 9, 13):
            rivet(t, x, y + 2, LEAD)
    for x in range(16):
        t.set(x, 0, shade(t.get(x, 0), 0.85))
    t.vline(7, 0, 4, LEAD[1])
    t.vline(11, 7, 9, LEAD[1])
    t.vline(4, 12, 15, LEAD[1])
    outline(t, 0, 0, 15, 15, LEAD[0])
    return t


# ======================================================================================
# shelter filtration unit
# ======================================================================================

# Civil defence olive drab over steel.
SHELTER_OLIVE = URANIUM.tinted(STEEL[2], 0.55)


@block("shelter_filtration_unit_side")
def shelter_filtration_unit_side():
    # Olive casing around the intake grille: louvres over the pale filter mat, and a white
    # stencil band above them.
    t = Tex(seed=540)
    casing(t, SHELTER_OLIVE, seed=540, base=3.2, amp=0.6, seam=False)
    t.rect(4, 5, 11, 12, DARK[0])
    for y in range(6, 12, 2):
        t.hline(4, 11, y, CARBONATE[3])
        t.hline(5, 10, y + 1, DARK[1])
        t.set(4, y, CARBONATE[4])
    inset(t, 3, 4, 12, 13, SHELTER_OLIVE)
    t.hline(4, 11, 3, CARBONATE[5])
    for x in (5, 7, 9):
        t.set(x, 3, SHELTER_OLIVE[1])
    return t


@block("shelter_filtration_unit_end")
def shelter_filtration_unit_end():
    # The blower's shaft end: a brass-bushed socket in the olive casing.
    t = Tex(seed=541)
    casing(t, SHELTER_OLIVE, seed=541, base=3.2, amp=0.6, seam=False)
    t.rect(4, 4, 11, 11, SHELTER_OLIVE[1])
    t.disc(7.5, 7.5, 3.6, DARK[0])
    t.ring(7.5, 7.5, 3.6, 2.6, BRASS[4])
    t.ring(7.5, 7.5, 2.6, 2.0, BRASS[2])
    t.rect(6, 6, 9, 9, DARK[2])
    t.hline(6, 9, 6, DARK[4])
    return t

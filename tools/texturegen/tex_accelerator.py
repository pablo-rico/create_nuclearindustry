"""Textures for the particle accelerator: controller, ion source, beamline, machines and beam dump.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.

The accelerator's look is a physics lab's: steel beam pipe with bolted flanges, copper RF
cavities, magnets painted by job (red quadrupoles, blue bending magnets), and yellow-and-black
high-voltage bands on the machines. Beamline blocks are ``cube_column`` models whose axis runs
along the beam: each ``*_side`` texture is laid out with the beam running top to bottom, each
``*_end`` texture is what the beam sees head-on. The shaft-driven machines share
``models/block/accelerator_machine.json`` (front recessed 2px round the shaft bearing, like the
tokamak's heaters).
"""

import math

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from common import bolt, casing, trefoil
from fusion_materials import COPPER, GRAPHITE, INLET
from gui import _plate, _recess, _trough, gui  # noqa: F401
from items import item  # noqa: F401
from palette import BRASS, CYAN, DARK, DANGER, MIMIC_BLUE, STEEL, WARN
from pixelkit import Ramp, Tex, bevel, brushed, hazard, outline, rivet, vents

CLEAR = (0, 0, 0, 0)
MAGNET_RED = Ramp("#4a1e1e", "#6b2a27", "#8c3a33", "#ab4c3f", "#c6624f", "#dc8068", "#eeaa90")
MAGNET_BLUE = MIMIC_BLUE
BEAM = CYAN

# The beam pipe is a tube in an empty block: everything round it is clear, and the window in
# its wall shows the beam.
CUTOUT_BLOCKS["beam_pipe_side"] = (16, 16)
CUTOUT_BLOCKS["beam_pipe_end"] = (16, 16)


# ======================================================================================
# shared pieces
# ======================================================================================

def _flange_end(t, ramp=STEEL, bore=2.6, r=6.2):
    """A bolted vacuum flange round the beam bore, as the beam sees it."""
    t.disc(7.5, 7.5, r, ramp[4])
    t.ring(7.5, 7.5, r, r - 1.0, ramp[5])
    t.ring(7.5, 7.5, r + 0.3, r - 0.2, ramp[1])
    t.disc(7.5, 7.5, bore + 1.2, ramp[2])
    t.ring(7.5, 7.5, bore + 1.2, bore + 0.4, ramp[1])
    t.disc(7.5, 7.5, bore, DARK[0])
    t.set(6, 6, BEAM[3])
    for x, y in ((7, 2), (12, 7), (7, 12), (2, 7)):
        t.set(x, y, ramp[6])
        t.set(x + 1, y + 1, ramp[1])


def _yoke(ramp, seed):
    """A painted magnet yoke: a heavy frame of laminations."""
    t = Tex(seed=seed)
    brushed(t, ramp, base=3.2, amp=0.5, seed=seed, axis="h", scale=7.0)
    for y in range(1, 15, 3):
        t.hline(1, 14, y, ramp[2])
    outline(t, 0, 0, 15, 15, ramp[0])
    bevel(t, 1, 1, 14, 14, ramp, hi=ramp[5], lo=ramp[1])
    return t


def _shaft_front(accent, ramp=STEEL):
    """A recessed bearing plate behind a 3px proud frame: where the shaft goes in."""
    t = Tex(seed=730)
    brushed(t, ramp, base=3.4, amp=0.5, seed=730)
    bevel(t, 0, 0, 15, 15, ramp, hi=ramp[5], lo=ramp[1])
    bevel(t, 1, 1, 14, 14, ramp, hi=ramp[4], lo=ramp[2], corner=False)
    t.rect(3, 3, 12, 12, DARK[2])
    t.hline(3, 12, 3, DARK[0])
    t.vline(3, 3, 12, DARK[0])
    t.hline(4, 12, 4, DARK[1])
    t.vline(4, 4, 12, DARK[1])
    t.ring(8, 8, 3.6, 2.4, BRASS[4])
    t.set(6, 5, BRASS[6])
    t.set(5, 6, BRASS[6])
    t.set(10, 10, BRASS[2])
    t.rect(6, 6, 9, 9, DARK[0])
    t.set(12, 1, accent)
    t.set(12, 0, ramp[2])
    for x, y in ((1, 1), (13, 13)):
        rivet(t, x, y, ramp)
    return t


def _machine_side(seed, ramp=STEEL):
    t = Tex(seed=seed)
    casing(t, ramp, seed=seed, seam=False)
    return t


# ======================================================================================
# controller
# ======================================================================================

@block("particle_accelerator_controller_side")
def controller_side():
    t = _machine_side(741)
    vents(t, 4, 4, 11, 11, STEEL, step=2)
    return t


@block("particle_accelerator_controller_top")
def controller_top():
    t = _machine_side(742)
    t.rect(4, 4, 11, 11, STEEL[2])
    bevel(t, 4, 4, 11, 11, STEEL, hi=STEEL[1], lo=STEEL[5])
    for x, y in ((5, 5), (9, 5), (5, 9), (9, 9)):
        bolt(t, x, y, STEEL)
    return t


@block("particle_accelerator_controller_bottom")
def controller_bottom():
    return _machine_side(743)


@block("particle_accelerator_controller_beam")
def controller_beam():
    """The face towards the ion source: a cable bundle feeding the source."""
    t = _machine_side(744)
    t.rect(4, 4, 11, 11, DARK[1])
    bevel(t, 4, 4, 11, 11, STEEL, hi=STEEL[1], lo=STEEL[5])
    for i, c in enumerate((WARN[4], BEAM[4], DANGER[4], WARN[4])):
        t.vline(5 + i * 2, 5, 10, c)
        t.set(5 + i * 2, 5, STEEL[6])
    return t


def _controller_front(lit):
    t = Tex(seed=745)
    brushed(t, STEEL, base=3.4, amp=0.5, seed=745)
    outline(t, 0, 0, 15, 15, STEEL[0])
    bevel(t, 1, 1, 14, 14, STEEL, hi=STEEL[5], lo=STEEL[1])
    # Screen: the beamline drawn as a trace, lit while the accelerator runs.
    t.rect(2, 2, 13, 8, DARK[1] if not lit else BEAM[1])
    bevel(t, 2, 2, 13, 8, STEEL, hi=STEEL[1], lo=STEEL[5])
    trace = BEAM[5] if lit else DARK[3]
    t.hline(3, 9, 5, trace)
    t.vline(9, 3, 5, trace)
    t.hline(9, 12, 3, trace)
    if lit:
        t.set(3, 5, BEAM[6])
        t.set(12, 3, WARN[6])
        for x in range(4, 12, 2):
            t.blend(x, 7, BEAM[4], 0.6)
    # Buttons and the key switch.
    t.rect(3, 10, 4, 11, BEAM[4] if lit else DARK[3])
    t.rect(6, 10, 7, 11, DANGER[4])
    t.rect(9, 10, 12, 13, STEEL[2])
    t.set(10, 11, BRASS[5])
    t.set(11, 12, BRASS[2])
    hazard(t, 2, 13, 7, 13, WARN[4], DARK[0], period=4, seed=745)
    return t


@block("particle_accelerator_controller_front")
def controller_front():
    return _controller_front(False)


@block("particle_accelerator_controller_front_on")
def controller_front_on():
    return _controller_front(True)


# ======================================================================================
# ion source
# ======================================================================================

@block("particle_accelerator_ion_source_front")
def ion_source_front():
    return _shaft_front(BEAM[5], STEEL)


@block("particle_accelerator_ion_source_side")
def ion_source_side():
    """Copper discharge chamber with the beam's flange in the middle and the hydrogen inlet."""
    t = Tex(seed=750)
    brushed(t, COPPER, base=3.3, amp=0.6, seed=750)
    outline(t, 0, 0, 15, 15, COPPER[0])
    bevel(t, 1, 1, 14, 14, COPPER, hi=COPPER[5], lo=COPPER[1])
    _flange_end(t, STEEL, bore=2.0, r=4.6)
    # Hydrogen inlet: cyan like every input.
    t.rect(12, 1, 14, 2, INLET)
    t.set(13, 0, STEEL[2])
    hazard(t, 1, 14, 14, 14, WARN[4], DARK[0], period=4, seed=750)
    return t


@block("particle_accelerator_ion_source_back")
def ion_source_back():
    t = _machine_side(751, COPPER)
    t.rect(5, 5, 10, 10, STEEL[3])
    bevel(t, 5, 5, 10, 10, STEEL)
    trefoil(t, 8, 8, 2.6, WARN[5])
    return t


# ======================================================================================
# beamline
# ======================================================================================

@block("beam_pipe_side")
def beam_pipe_side():
    """A steel tube running top to bottom, a slot window down its middle, flanges at its ends."""
    t = Tex(seed=760)
    t.rect(0, 0, 15, 15, CLEAR)
    for y in range(16):
        t.set(4, y, STEEL[2])
        t.set(5, y, STEEL[4])
        t.set(10, y, STEEL[3])
        t.set(11, y, STEEL[1])
    for y in (0, 15):
        t.hline(3, 12, y, STEEL[5] if y == 0 else STEEL[2])
    # The window: glass frames round a clear slot that shows the beam.
    for y in range(2, 14):
        t.set(6, y, STEEL[5] if y in (2, 13) else CLEAR)
        t.set(7, y, CLEAR)
        t.set(8, y, CLEAR)
        t.set(9, y, STEEL[5] if y in (2, 13) else CLEAR)
    t.hline(6, 9, 2, STEEL[5])
    t.hline(6, 9, 13, STEEL[2])
    t.hline(6, 9, 1, STEEL[3])
    t.hline(6, 9, 14, STEEL[3])
    return t


@block("beam_pipe_end")
def beam_pipe_end():
    t = Tex(seed=761)
    t.rect(0, 0, 15, 15, CLEAR)
    t.disc(7.5, 7.5, 4.6, STEEL[4])
    t.ring(7.5, 7.5, 4.6, 3.6, STEEL[5])
    t.ring(7.5, 7.5, 4.9, 4.3, STEEL[1])
    t.disc(7.5, 7.5, 2.5, CLEAR)
    for x, y in ((7, 3), (11, 7), (7, 11), (3, 7)):
        t.set(x, y, STEEL[6])
    return t


@block("beam_rf_cavity_side")
def rf_cavity_side():
    """A stack of copper pillbox cells with a waveguide coupler on its flank."""
    t = Tex(seed=770)
    brushed(t, COPPER, base=3.4, amp=0.5, seed=770, axis="v")
    for y in (0, 5, 10, 15):
        t.hline(0, 15, y, COPPER[1])
        if y + 1 <= 15:
            t.hline(0, 15, y + 1, COPPER[5])
    for y in range(16):
        t.set(0, y, COPPER[2])
        t.set(15, y, COPPER[1])
    # Waveguide coupler: a steel rectangle bolted on.
    t.rect(5, 6, 10, 9, STEEL[3])
    bevel(t, 5, 6, 10, 9, STEEL)
    t.rect(6, 7, 9, 8, DARK[1])
    t.set(7, 7, BRASS[5])
    return t


@block("beam_rf_cavity_end")
def rf_cavity_end():
    t = Tex(seed=771)
    brushed(t, COPPER, base=3.2, amp=0.5, seed=771)
    outline(t, 0, 0, 15, 15, COPPER[0])
    bevel(t, 1, 1, 14, 14, COPPER, hi=COPPER[5], lo=COPPER[1])
    _flange_end(t, STEEL, bore=2.0, r=5.0)
    return t


@block("beam_quadrupole_side")
def quadrupole_side():
    """Red yoke with the copper coils' windings showing between its plates."""
    t = _yoke(MAGNET_RED, 780)
    for y in (3, 4, 11, 12):
        t.hline(2, 13, y, COPPER[4] if y in (3, 11) else COPPER[2])
    t.rect(6, 6, 9, 9, MAGNET_RED[1])
    t.set(6, 6, MAGNET_RED[5])
    for x, y in ((2, 7), (12, 7)):
        bolt(t, x, y, STEEL)
    return t


@block("beam_quadrupole_end")
def quadrupole_end():
    """The quadrupole's classic cross-section: four pole tips and their coils round the bore."""
    t = _yoke(MAGNET_RED, 781)
    t.rect(2, 2, 13, 13, MAGNET_RED[2])
    # Coils on the four poles, along the diagonals.
    for x, y in ((3, 3), (10, 3), (3, 10), (10, 10)):
        t.rect(x, y, x + 2, y + 2, COPPER[4])
        t.set(x, y, COPPER[6])
        t.set(x + 2, y + 2, COPPER[1])
    # Pole tips pointing at the bore.
    for x, y in ((5, 5), (9, 5), (5, 9), (9, 9)):
        t.rect(x, y, x + 1, y + 1, MAGNET_RED[4])
    t.disc(7.5, 7.5, 1.8, DARK[0])
    t.set(7, 7, BEAM[3])
    return t


@block("beam_position_monitor_side")
def position_monitor_side():
    """A short steel spool with four gilded pickup buttons and its signal cable."""
    t = Tex(seed=790)
    brushed(t, STEEL, base=3.3, amp=0.5, seed=790, axis="v")
    outline(t, 0, 0, 15, 15, STEEL[0])
    t.hline(0, 15, 1, STEEL[5])
    t.hline(0, 15, 14, STEEL[1])
    for x, y in ((4, 5), (10, 5), (4, 10), (10, 10)):
        t.disc(x + 0.5, y + 0.5, 1.3, BRASS[4])
        t.set(x, y, BRASS[6])
    t.vline(7, 3, 12, DARK[2])
    t.vline(8, 3, 12, BEAM[3])
    return t


@block("beam_position_monitor_end")
def position_monitor_end():
    t = Tex(seed=791)
    brushed(t, STEEL, base=3.0, amp=0.5, seed=791)
    outline(t, 0, 0, 15, 15, STEEL[0])
    _flange_end(t, STEEL)
    for x, y in ((7, 4), (10, 7), (7, 10), (4, 7)):
        t.set(x, y, BRASS[5])
    return t


def _bend_face(seed, port, arrow=False):
    t = _yoke(MAGNET_BLUE, seed)
    for y in (3, 12):
        t.hline(2, 13, y, COPPER[4])
    if port:
        _flange_end(t, STEEL, bore=2.0, r=4.0)
    if arrow:
        # A white arrow round the yoke: which way the beam turns.
        for x in range(4, 12):
            t.set(x, 8, (230, 236, 240, 255))
        for i in range(3):
            t.set(11 - i, 8 - i, (230, 236, 240, 255))
            t.set(11 - i, 8 + i, (230, 236, 240, 255))
    return t


@block("beam_bending_magnet_front")
def bend_front():
    t = _bend_face(800, True)
    # The exit: an orange output mark like every outlet.
    t.hline(6, 9, 13, (179, 130, 79, 255))
    return t


@block("beam_bending_magnet_side")
def bend_side():
    return _bend_face(801, True)


@block("beam_bending_magnet_back")
def bend_back():
    return _bend_face(802, False)


@block("beam_bending_magnet_top")
def bend_top():
    t = _bend_face(803, False, arrow=True)
    return t


# ======================================================================================
# machines beside the beamline
# ======================================================================================

@block("particle_accelerator_klystron_front")
def klystron_front():
    return _shaft_front(WARN[5], STEEL)


@block("particle_accelerator_klystron_side")
def klystron_side():
    """The copper tube with its cooling fins, under a high-voltage band."""
    t = _machine_side(811)
    t.rect(4, 2, 11, 13, COPPER[3])
    for y in range(3, 13, 2):
        t.hline(3, 12, y, COPPER[5])
        t.hline(3, 12, y + 1, COPPER[1])
    hazard(t, 1, 1, 14, 1, WARN[4], DARK[0], period=4, seed=811)
    hazard(t, 1, 14, 14, 14, WARN[4], DARK[0], period=4, seed=812)
    return t


@block("particle_accelerator_klystron_back")
def klystron_back():
    """The waveguide flange the radio-frequency power leaves through."""
    t = _machine_side(812)
    t.rect(3, 5, 12, 10, COPPER[4])
    bevel(t, 3, 5, 12, 10, COPPER)
    t.rect(5, 7, 10, 8, DARK[0])
    for x, y in ((3, 5), (11, 5), (3, 9), (11, 9)):
        t.set(x, y, STEEL[6])
    return t


@block("particle_accelerator_magnet_power_supply_front")
def supply_front():
    return _shaft_front(MAGNET_RED[5], STEEL)


@block("particle_accelerator_magnet_power_supply_side")
def supply_side():
    """A cabinet with an ammeter and vents."""
    t = _machine_side(821)
    t.rect(4, 3, 11, 7, DARK[1])
    bevel(t, 4, 3, 11, 7, STEEL, hi=STEEL[1], lo=STEEL[5])
    t.hline(5, 10, 6, WARN[3])
    t.line(7, 6, 10, 4, WARN[6])
    vents(t, 4, 9, 11, 12, STEEL, step=1)
    return t


@block("particle_accelerator_magnet_power_supply_back")
def supply_back():
    """Copper busbars out to the magnets."""
    t = _machine_side(822)
    for x in (5, 9):
        t.rect(x, 2, x + 1, 13, COPPER[4])
        t.vline(x, 2, 13, COPPER[6])
        t.set(x, 13, COPPER[1])
    t.rect(4, 7, 12, 8, MAGNET_RED[3])
    return t


@block("particle_accelerator_vacuum_pump_front")
def pump_front():
    return _shaft_front(BEAM[5], STEEL)


@block("particle_accelerator_vacuum_pump_side")
def pump_side():
    """The pump's round body with its rotor fins seen through the cowling."""
    t = _machine_side(831)
    t.disc(7.5, 7.5, 5.0, STEEL[2])
    t.ring(7.5, 7.5, 5.0, 4.2, STEEL[5])
    for i in range(8):
        a = i * math.pi / 4
        t.line(8, 8, round(7.5 + 3.6 * math.cos(a)), round(7.5 + 3.6 * math.sin(a)), STEEL[4])
    t.disc(7.5, 7.5, 1.2, DARK[0])
    return t


@block("particle_accelerator_vacuum_pump_back")
def pump_back():
    t = _machine_side(832)
    _flange_end(t, STEEL, bore=2.4, r=5.0)
    return t


# ======================================================================================
# beam dump
# ======================================================================================

@block("beam_dump_side")
def dump_side():
    """Stacked graphite blocks in a steel frame, under a radiation mark."""
    t = Tex(seed=840)
    brushed(t, GRAPHITE, base=2.6, amp=0.6, seed=840)
    for y in (4, 8, 12):
        t.hline(1, 14, y, GRAPHITE[0])
    for y, x in ((1, 8), (5, 4), (5, 11), (9, 8), (13, 4), (13, 11)):
        t.vline(x, y, y + 2, GRAPHITE[0])
    outline(t, 0, 0, 15, 15, STEEL[1])
    bevel(t, 1, 1, 14, 14, STEEL, hi=STEEL[4], lo=STEEL[1])
    t.rect(5, 5, 10, 10, WARN[4])
    trefoil(t, 8, 8, 2.8, DARK[0])
    return t


@block("beam_dump_top")
def dump_top():
    t = Tex(seed=841)
    casing(t, STEEL, seed=841, seam=False)
    _flange_end(t, STEEL, bore=2.0, r=4.4)
    return t


@block("beam_dump_bottom")
def dump_bottom():
    return casing(Tex(seed=842), STEEL, seed=842, seam=False)


# ======================================================================================
# control screen
# ======================================================================================

@gui("accelerator_control")
def accelerator_control():
    """AcceleratorScreen: 256x196. Coordinates are the screen's constants."""
    w, h = 256, 196
    t = _plate(w, h, 850)
    hazard(t, w - 60, 5, w - 10, 15, WARN[4], DARK[0], period=6, seed=851)
    # Phase panel: two lines of text from y=23.
    _recess(t, 8, 19, w - 9, 42)
    # Readouts: eight rows from y=47 every 11 px; gauges 70x5 at x=172 on all but the power row.
    _recess(t, 8, 44, w - 9, 134)
    for row in (0, 1, 3, 4, 5, 6, 7):
        _trough(t, 172, 47 + row * 11, 70, 5)
    # Setpoints: value windows between the - and + buttons.
    for x0 in (12, 136):
        _recess(t, x0 + 16, 171, x0 + 96, 184)
    return t


# ======================================================================================
# synchrotron ring, septa, cryogenics, target station and personnel interlock
# ======================================================================================
# These blocks have hand-written models (``models/block/accelerator_ring_element.json`` and the
# septum/station models). In the ring element the beam runs along the model's Z axis, so each
# ``*_side`` texture has the beam running left to right, each ``*_top`` top to bottom, and each
# ``*_end`` is what the beam sees head-on; its middle 8x8 is cut out for the flange stub.

WHITE_MARK = (230, 236, 240, 255)
SC_JACKET = Ramp("#3a4c50", "#55707a", "#7896a0", "#9fb8bf", "#c4d6da", "#dfe9ea", "#f2f7f6")
NIOBIUM = Ramp("#3b3f46", "#565b64", "#737984", "#9097a1", "#adb3bc", "#c9ced5", "#e3e7ec")


def _end_flange(t, ramp=STEEL):
    """The beam's flange in the middle 8x8 of an end face."""
    t.rect(4, 4, 11, 11, ramp[3])
    bevel(t, 4, 4, 11, 11, ramp, hi=ramp[5], lo=ramp[1])
    t.disc(7.5, 7.5, 2.6, ramp[2])
    t.ring(7.5, 7.5, 2.6, 2.0, ramp[1])
    t.disc(7.5, 7.5, 1.7, DARK[0])
    t.set(7, 7, BEAM[3])
    for x, y in ((5, 5), (10, 5), (5, 10), (10, 10)):
        t.set(x, y, ramp[6])


@block("ring_dipole_magnet_side")
def ring_dipole_side():
    """The C-yoke's flank: blue laminations, the copper coils bulging out above and below the gap."""
    t = _yoke(MAGNET_BLUE, 1400)
    for y in (2, 3, 4, 11, 12, 13):
        t.hline(0, 15, y, COPPER[5] if y in (2, 11) else COPPER[3] if y in (3, 12) else COPPER[1])
    t.hline(0, 15, 7, DARK[1])
    t.hline(0, 15, 8, DARK[2])
    for x in (2, 13):
        bolt(t, x, 5, STEEL)
        bolt(t, x, 9, STEEL)
    return t


@block("ring_dipole_magnet_top")
def ring_dipole_top():
    """From above: the yoke's back plate with a white arrow along the beam."""
    t = _yoke(MAGNET_BLUE, 1401)
    for x in (2, 13):
        t.vline(x, 1, 14, COPPER[4])
    for y in range(3, 13):
        t.set(8, y, WHITE_MARK)
    for i in range(3):
        t.set(8 - i, 3 + i, WHITE_MARK)
        t.set(8 + i, 3 + i, WHITE_MARK)
    return t


@block("ring_dipole_magnet_end")
def ring_dipole_end():
    """The classic C cross-section: yoke round three sides, coils above and below the pole gap."""
    t = _yoke(MAGNET_BLUE, 1402)
    t.rect(2, 2, 13, 13, MAGNET_BLUE[1])
    for y0 in (2, 11):
        t.rect(3, y0, 12, y0 + 2, COPPER[3])
        t.hline(3, 12, y0, COPPER[5])
        t.hline(3, 12, y0 + 2, COPPER[1])
    t.rect(13, 5, 15, 10, DARK[1])
    _end_flange(t)
    return t


def _sc_jacket(seed, axis="h"):
    """A superconducting magnet's cryostat: a pale vacuum jacket with a blue stripe."""
    t = Tex(seed=seed)
    brushed(t, SC_JACKET, base=4.2, amp=0.5, seed=seed, axis=axis, scale=6.0)
    outline(t, 0, 0, 15, 15, SC_JACKET[1])
    bevel(t, 1, 1, 14, 14, SC_JACKET, hi=SC_JACKET[6], lo=SC_JACKET[2])
    return t


@block("superconducting_ring_dipole_side")
def sc_dipole_side():
    t = _sc_jacket(1410)
    t.hline(1, 14, 6, MAGNET_BLUE[3])
    t.hline(1, 14, 7, MAGNET_BLUE[4])
    t.hline(1, 14, 8, MAGNET_BLUE[2])
    for x in (4, 11):
        t.vline(x, 2, 13, SC_JACKET[2])
        t.vline(x + 1, 2, 13, SC_JACKET[5])
    # Frost where the cold mass touches the jacket supports.
    for x, y in ((2, 3), (13, 12), (3, 12), (12, 3)):
        t.set(x, y, (226, 240, 246, 255))
    return t


@block("superconducting_ring_dipole_top")
def sc_dipole_top():
    """The helium inlet turret on top of the cryostat, beside the beam arrow."""
    t = _sc_jacket(1411, axis="v")
    t.disc(4.5, 4.5, 2.6, SC_JACKET[3])
    t.ring(4.5, 4.5, 2.6, 1.8, INLET)
    t.disc(4.5, 4.5, 1.2, DARK[1])
    for y in range(4, 14):
        t.set(10, y, WHITE_MARK)
    for i in range(3):
        t.set(10 - i, 4 + i, WHITE_MARK)
        t.set(10 + i, 4 + i, WHITE_MARK)
    return t


@block("superconducting_ring_dipole_end")
def sc_dipole_end():
    """Round cryostat vessel round the collared coil and the bore."""
    t = _sc_jacket(1412)
    t.disc(7.5, 7.5, 6.6, SC_JACKET[3])
    t.ring(7.5, 7.5, 6.6, 5.8, SC_JACKET[6])
    t.ring(7.5, 7.5, 5.8, 4.9, MAGNET_BLUE[3])
    _end_flange(t)
    return t


@block("superconducting_rf_cavity_side")
def sc_cavity_side():
    """The cryomodule's flank with a window onto the niobium cells strung along the beam."""
    t = _sc_jacket(1420)
    t.rect(2, 4, 13, 11, DARK[0])
    for cx in (3.5, 7.5, 11.5):
        for y in range(5, 11):
            for x in range(2, 14):
                d = abs(x + 0.5 - cx) / 2.0 + abs(y + 0.5 - 8) / 3.2
                if d < 1.0:
                    t.set(x, y, NIOBIUM[int(round(5.6 - d * 3 - (y - 5) * 0.3))])
    t.hline(2, 13, 8, NIOBIUM[1])
    outline(t, 1, 3, 14, 12, SC_JACKET[1])
    t.hline(2, 13, 13, MAGNET_RED[4])
    return t


@block("superconducting_rf_cavity_top")
def sc_cavity_top():
    """The power coupler and the helium chimney on top of the cryomodule."""
    t = _sc_jacket(1421, axis="v")
    t.rect(3, 5, 7, 10, COPPER[4])
    bevel(t, 3, 5, 7, 10, COPPER)
    t.rect(4, 6, 6, 9, DARK[1])
    t.disc(11.5, 7.5, 2.4, SC_JACKET[3])
    t.ring(11.5, 7.5, 2.4, 1.6, INLET)
    t.disc(11.5, 7.5, 1.0, DARK[1])
    return t


@block("superconducting_rf_cavity_end")
def sc_cavity_end():
    t = _sc_jacket(1422)
    t.disc(7.5, 7.5, 6.2, NIOBIUM[3])
    t.ring(7.5, 7.5, 6.2, 5.4, NIOBIUM[5])
    _end_flange(t)
    return t


def _septum_body(seed, accent):
    t = _yoke(MAGNET_BLUE, seed)
    t.hline(1, 14, 13, accent)
    return t


def _septum_front(seed, accent, out):
    """Two bores side by side, split by the thin copper septum blade: the big one is the ring's
    circulating beam, the small one the line joining (injection) or leaving (extraction) it."""
    t = _septum_body(seed, accent)
    t.rect(2, 3, 13, 12, MAGNET_BLUE[1])
    t.disc(5.5, 7.5, 3.2, STEEL[3])
    t.ring(5.5, 7.5, 3.2, 2.5, STEEL[5])
    t.disc(5.5, 7.5, 1.8, DARK[0])
    t.set(5, 7, BEAM[3])
    t.vline(9, 3, 12, COPPER[5])
    t.vline(10, 3, 12, COPPER[2])
    t.disc(12.0, 7.5, 1.6, STEEL[3])
    t.disc(12.0, 7.5, 0.9, DARK[0])
    t.set(12, 7, accent)
    # Port mark: the mod's in/out arrow, below the bores.
    if out:
        t.hline(4, 9, 14, accent)
        t.set(10, 14, accent)
    else:
        t.hline(6, 11, 14, accent)
        t.set(5, 14, accent)
    return t


def _septum_top(seed, accent, merge):
    """Seen from above: the two beam paths meeting at the blade (injection) or parting (extraction)."""
    t = _septum_body(seed, accent)
    for y in range(2, 14):
        t.set(6, y, WHITE_MARK)
    for i in range(6):
        y = 2 + i if merge else 13 - i
        t.set(11 - i // 2, y, accent)
    t.vline(8, 2, 13, COPPER[4])
    return t


def _septum_side(seed, accent):
    t = _septum_body(seed, accent)
    for y in (3, 4, 10, 11):
        t.hline(2, 13, y, COPPER[4] if y in (3, 10) else COPPER[2])
    hazard(t, 1, 1, 14, 1, WARN[4], DARK[0], period=4, seed=seed)
    for x, y in ((2, 6), (12, 6)):
        bolt(t, x, y, STEEL)
    return t


def _septum_back(seed, accent):
    t = _septum_body(seed, accent)
    _flange_end(t, STEEL, bore=2.0, r=4.2)
    return t


@block("injection_septum_front")
def injection_front():
    return _septum_front(1430, INLET, False)


@block("injection_septum_top")
def injection_top():
    return _septum_top(1431, INLET, True)


@block("injection_septum_side")
def injection_side():
    return _septum_side(1432, INLET)


@block("injection_septum_back")
def injection_back():
    return _septum_back(1433, INLET)


OUTLET_MARK = (179, 130, 79, 255)


@block("extraction_septum_front")
def extraction_front():
    return _septum_front(1440, OUTLET_MARK, True)


@block("extraction_septum_top")
def extraction_top():
    return _septum_top(1441, OUTLET_MARK, False)


@block("extraction_septum_side")
def extraction_side():
    return _septum_side(1442, OUTLET_MARK)


@block("extraction_septum_back")
def extraction_back():
    return _septum_back(1443, OUTLET_MARK)


@block("accelerator_cryo_feed_front")
def cryo_feed_front():
    """The bayonet coupling of the helium transfer line, frosted, with its cyan input ring."""
    t = _sc_jacket(1450)
    t.disc(7.5, 7.5, 5.4, SC_JACKET[3])
    t.ring(7.5, 7.5, 5.4, 4.6, SC_JACKET[6])
    t.ring(7.5, 7.5, 4.2, 3.4, INLET)
    t.disc(7.5, 7.5, 2.4, DARK[1])
    t.disc(7.5, 7.5, 1.2, (226, 240, 246, 255))
    for x, y in ((7, 2), (12, 7), (7, 12), (2, 7)):
        t.set(x, y, STEEL[6])
    return t


@block("accelerator_cryo_feed_side")
def cryo_feed_side():
    """The valve box's flank: a pressure gauge and the vacuum-jacketed line running down it."""
    t = _sc_jacket(1451)
    t.disc(5.5, 5.5, 3.0, STEEL[2])
    t.disc(5.5, 5.5, 2.3, (232, 226, 205, 255))
    t.line(5, 6, 7, 4, DANGER[3])
    t.rect(10, 1, 12, 14, SC_JACKET[5])
    t.vline(10, 1, 14, SC_JACKET[6])
    t.vline(12, 1, 14, SC_JACKET[2])
    for y in (4, 9):
        t.hline(9, 13, y, STEEL[3])
    t.hline(2, 8, 11, MAGNET_BLUE[4])
    t.hline(2, 8, 12, MAGNET_BLUE[2])
    return t


@block("accelerator_cryo_feed_top")
def cryo_feed_top():
    """Two valve handwheels: supply (cyan) and return (orange)."""
    t = _sc_jacket(1452)
    for cx, c in ((4.5, INLET), (11.5, OUTLET_MARK)):
        t.ring(cx, 7.5, 3.0, 2.2, c)
        t.line(int(cx) - 2, 7, int(cx) + 2, 7, c)
        t.line(int(cx), 5, int(cx), 9, c)
        t.set(int(cx), 7, STEEL[6])
    return t


LEAD_BRICK = Ramp("#2c3236", "#3f474c", "#555f64", "#6c777b", "#869095", "#a1aaad", "#bfc6c7")
ANTI_OUT = (107, 255, 138, 255)


@block("target_station_front")
def target_front():
    """The beam's way in: a flange set in the lead shield, framed by hazard tape."""
    t = Tex(seed=1460)
    brushed(t, LEAD_BRICK, base=3.0, amp=0.5, seed=1460)
    hazard(t, 0, 0, 15, 1, WARN[4], DARK[0], period=4, seed=1460)
    hazard(t, 0, 14, 15, 15, WARN[4], DARK[0], period=4, seed=1461)
    _flange_end(t, STEEL, bore=2.2, r=5.0)
    t.ring(7.5, 7.5, 2.9, 2.2, INLET)
    return t


@block("target_station_back")
def target_back():
    """The antiproton collector: a magnetic horn's mouth with the green antimatter port ring."""
    t = Tex(seed=1462)
    brushed(t, LEAD_BRICK, base=3.0, amp=0.5, seed=1462)
    outline(t, 0, 0, 15, 15, LEAD_BRICK[0])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d <= 6.4:
                t.set(x, y, COPPER[5] if int(d * 0.75) % 2 == 0 else COPPER[2])
    t.ring(7.5, 7.5, 2.4, 1.7, ANTI_OUT)
    t.disc(7.5, 7.5, 1.6, DARK[0])
    return t


@block("target_station_side")
def target_side():
    """Stacked lead bricks under a radiation mark: the target makes a lot of secondaries."""
    t = Tex(seed=1463)
    brushed(t, LEAD_BRICK, base=3.2, amp=0.6, seed=1463)
    for y in (0, 4, 8, 12):
        t.hline(0, 15, y, LEAD_BRICK[0])
        t.hline(0, 15, y + 1, LEAD_BRICK[5])
    for y, x in ((1, 7), (5, 3), (5, 11), (9, 7), (13, 3), (13, 11)):
        t.vline(x, y, y + 2, LEAD_BRICK[0])
    t.rect(5, 5, 10, 10, WARN[4])
    outline(t, 5, 5, 10, 10, WARN[1])
    trefoil(t, 8, 8, 2.6, DARK[0])
    return t


@block("target_station_top")
def target_top():
    """The target hatch: a bolted brass plug with a lifting handle over the target slot."""
    t = Tex(seed=1464)
    brushed(t, LEAD_BRICK, base=3.0, amp=0.5, seed=1464)
    outline(t, 0, 0, 15, 15, LEAD_BRICK[0])
    t.rect(3, 3, 12, 12, BRASS[3])
    bevel(t, 3, 3, 12, 12, BRASS, hi=BRASS[6], lo=BRASS[1])
    t.rect(5, 7, 10, 8, BRASS[1])
    t.hline(5, 10, 7, BRASS[5])
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        t.set(x + 1, y + 1, STEEL[6])
    return t


INTERLOCK = Ramp("#4a4220", "#6b5f2a", "#8f7f37", "#b39f44", "#cfba59", "#e3d07a", "#f2e6a8")


def _interlock_front(secured):
    """The search-and-secure panel: key switch, SEARCH (amber) and SECURE (green) buttons and the
    status lamp - red while the area is open, green once secured. Lamp at [6..9, 2..3]."""
    t = Tex(seed=1470)
    brushed(t, INTERLOCK, base=3.6, amp=0.5, seed=1470)
    outline(t, 0, 0, 15, 15, INTERLOCK[0])
    bevel(t, 1, 1, 14, 14, INTERLOCK, hi=INTERLOCK[6], lo=INTERLOCK[1])
    lamp = RADIUM_LAMP if secured else DANGER
    t.rect(5, 1, 10, 4, DARK[0])
    t.rect(6, 2, 9, 3, lamp[5] if secured else lamp[4])
    t.set(6, 2, lamp[6])
    # The key switch, turned to "secure" once the search is done.
    t.disc(7.5, 7.5, 2.0, STEEL[3])
    t.ring(7.5, 7.5, 2.0, 1.4, STEEL[5])
    if secured:
        t.hline(6, 9, 7, BRASS[5])
    else:
        t.vline(7, 6, 9, BRASS[5])
    # Buttons.
    t.rect(3, 11, 6, 13, DARK[0])
    t.rect(4, 11, 5, 12, WARN[4])
    t.rect(9, 11, 12, 13, DARK[0])
    t.rect(10, 11, 11, 12, RADIUM_LAMP[4] if not secured else RADIUM_LAMP[6])
    hazard(t, 1, 14, 14, 14, WARN[4], DARK[0], period=4, seed=1470)
    return t


RADIUM_LAMP = Ramp("#1f3d25", "#2f5c35", "#3f7f46", "#58a85c", "#77cf74", "#a6ec9c", "#d9ffd2")


@block("beam_interlock_front")
def interlock_front():
    return _interlock_front(False)


@block("beam_interlock_front_secured")
def interlock_front_secured():
    return _interlock_front(True)


@block("beam_interlock_side")
def interlock_side():
    """The cabinet's flank: plain painted steel with the conduit for the door switches."""
    t = Tex(seed=1472)
    brushed(t, INTERLOCK, base=3.4, amp=0.5, seed=1472)
    outline(t, 0, 0, 15, 15, INTERLOCK[0])
    bevel(t, 1, 1, 14, 14, INTERLOCK, hi=INTERLOCK[5], lo=INTERLOCK[1])
    t.rect(6, 2, 9, 13, STEEL[3])
    t.vline(6, 2, 13, STEEL[5])
    t.vline(9, 2, 13, STEEL[1])
    for y in (4, 11):
        t.hline(5, 10, y, STEEL[2])
    return t

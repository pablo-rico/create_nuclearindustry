"""Textures for the tokamak's neutral beam injectors, radio-frequency antennas and viewports.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.

The heaters' models (``models/block/fusion_neutral_beam_injector.json`` and
``fusion_rf_antenna.json``) face north: a 3px frame stands 2px proud of a 10x10 recess where the
drive shaft comes out, so each ``*_front`` texture is laid out as seen from the front - frame in
the outer 3px, recess floor in the middle with the shaft bearing at 6..9. The ``*_back`` faces look
into the plasma chamber, the sides carry the hardware that says what the block is.
"""

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from fusion_materials import COPPER, GRAPHITE, INLET, TITANIUM, panel
from gui import gui  # noqa: F401
from items import item  # noqa: F401
from palette import CYAN, PLASMA, WARN, WHITE
from pixelkit import Tex, bevel

CLEAR = (0, 0, 0, 0)
QUARTZ = [(214, 226, 226, 255), (236, 244, 242, 255), (170, 190, 192, 255)]

# The viewport is a window: clear glass in a titanium frame.
CUTOUT_BLOCKS["fusion_viewport"] = (16, 16)


# ======================================================================================
# shared heater frame
# ======================================================================================

def _heater_front(accent):
    """Frame bolted round a recessed bearing plate; ``accent`` colours the frame's lamp."""
    t = panel()
    # The outer 3px are the proud frame: lit top-left, shadowed bottom-right.
    bevel(t, 0, 0, 15, 15, TITANIUM, hi=TITANIUM[5], lo=TITANIUM[1])
    bevel(t, 1, 1, 14, 14, TITANIUM, hi=TITANIUM[4], lo=TITANIUM[2], corner=False)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        t.set(x, y, TITANIUM[6])
        t.set(x + 1, y + 1, TITANIUM[1])
    # Recess floor, in shadow under the frame's top and left edges.
    t.rect(3, 3, 12, 12, GRAPHITE[2])
    t.hline(3, 12, 3, GRAPHITE[0])
    t.vline(3, 3, 12, GRAPHITE[0])
    t.hline(4, 12, 4, GRAPHITE[1])
    t.vline(4, 4, 12, GRAPHITE[1])
    # Shaft bearing: a titanium collar round a dark bore.
    t.ring(8, 8, 3.6, 2.4, TITANIUM[4])
    t.set(6, 5, TITANIUM[6])
    t.set(5, 6, TITANIUM[6])
    t.set(10, 10, TITANIUM[2])
    t.rect(6, 6, 9, 9, GRAPHITE[0])
    # Status lamp on the top bar.
    t.set(12, 1, accent)
    t.set(12, 0, TITANIUM[2])
    return t


# ======================================================================================
# neutral beam injector
# ======================================================================================

@block("fusion_neutral_beam_injector_front")
def nbi_front():
    t = _heater_front(PLASMA[5])
    # High voltage: hazard stripes along the bottom bar.
    for x in range(1, 15):
        t.set(x, 14, WARN[4] if (x // 2) % 2 == 0 else GRAPHITE[0])
    t.hline(1, 14, 13, TITANIUM[2])
    # Deuterium feed on the top bar, cyan like every input.
    t.rect(3, 1, 4, 1, INLET)
    t.set(3, 0, TITANIUM[2])
    # Beam arrow in the recess, pointing in.
    for i, y in enumerate((5, 6)):
        t.set(11 - i, y, PLASMA[4])
    t.set(11, 6, PLASMA[3])
    return t


@block("fusion_neutral_beam_injector_side")
def nbi_side():
    t = panel()
    # Beam line running front to back: duct, flanges, then the ion source box at the back end.
    for y, c in ((6, GRAPHITE[1]), (7, TITANIUM[5]), (8, TITANIUM[4]), (9, TITANIUM[2]), (10, GRAPHITE[1])):
        t.hline(0, 9, y, c)
    for x in (2, 7):
        t.vline(x, 5, 11, TITANIUM[6])
        t.vline(x + 1, 5, 11, TITANIUM[1])
    # Ion source: a boxy housing on ceramic insulators.
    t.rect(10, 2, 15, 13, TITANIUM[3])
    bevel(t, 10, 2, 15, 13, TITANIUM, hi=TITANIUM[5], lo=TITANIUM[0])
    for y in (4, 7, 10):
        t.hline(11, 14, y, QUARTZ[1])
        t.hline(11, 14, y + 1, QUARTZ[2])
    t.set(12, 12, PLASMA[5])
    return t


@block("fusion_neutral_beam_injector_back")
def nbi_back():
    t = Tex(seed=402)
    t.rect(0, 0, 15, 15, GRAPHITE[1])
    bevel(t, 0, 0, 15, 15, GRAPHITE, hi=GRAPHITE[3], lo=GRAPHITE[0])
    # Accelerator grid: rows of apertures with the beam glowing through.
    t.rect(3, 3, 12, 12, GRAPHITE[0])
    for y in range(4, 12, 2):
        for x in range(4, 12, 2):
            t.set(x, y, CYAN[5] if (x + y) % 4 == 0 else CYAN[4])
            t.set(x + 1, y + 1, GRAPHITE[2])
    t.frame(2, 2, 13, 13, TITANIUM[2])
    return t


# ======================================================================================
# radio-frequency antenna
# ======================================================================================

@block("fusion_rf_antenna_front")
def rf_front():
    t = _heater_front(CYAN[5])
    # Coaxial feed: copper rings round the bearing collar.
    t.ring(8, 8, 4.6, 3.7, COPPER[4])
    t.set(4, 7, COPPER[6])
    t.set(7, 4, COPPER[6])
    t.set(11, 9, COPPER[1])
    t.set(9, 11, COPPER[1])
    # Tuning stubs on the bottom bar.
    for x in (4, 7, 10):
        t.rect(x, 13, x + 1, 14, COPPER[3])
        t.set(x, 13, COPPER[5])
    # Radio waves on the top bar.
    for x in (2, 4, 6):
        t.set(x, 1, COPPER[5])
        t.set(x + 1, 2, COPPER[2])
    return t


@block("fusion_rf_antenna_side")
def rf_side():
    t = panel()
    # The current straps, seen through slots in the casing.
    t.rect(2, 2, 13, 13, GRAPHITE[1])
    bevel(t, 2, 2, 13, 13, TITANIUM, hi=TITANIUM[1], lo=TITANIUM[5])
    for x in range(3, 13, 3):
        t.vline(x, 3, 12, COPPER[4])
        t.vline(x + 1, 3, 12, COPPER[2])
    # Feeder bar across the straps.
    t.hline(3, 12, 7, COPPER[5])
    t.hline(3, 12, 8, COPPER[1])
    return t


@block("fusion_rf_antenna_back")
def rf_back():
    t = Tex(seed=412)
    t.rect(0, 0, 15, 15, GRAPHITE[1])
    bevel(t, 0, 0, 15, 15, GRAPHITE, hi=GRAPHITE[3], lo=GRAPHITE[0])
    # Faraday screen: tilted copper bars over the straps.
    for y in range(2, 14, 2):
        t.hline(2, 13, y, COPPER[4])
        t.hline(2, 13, y + 1, COPPER[1])
    for x in (2, 13):
        t.vline(x, 2, 13, TITANIUM[3])
    return t


# ======================================================================================
# viewport
# ======================================================================================

@block("fusion_viewport")
def viewport():
    t = panel()
    # A thick titanium frame bolted round a fused-quartz pane.
    bevel(t, 0, 0, 15, 15, TITANIUM, hi=TITANIUM[5], lo=TITANIUM[1])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        t.set(x, y, TITANIUM[6])
        t.set(x + 1, y + 1, TITANIUM[1])
    t.frame(2, 2, 13, 13, TITANIUM[1])
    # Quartz retaining ring: lit top-left, shadowed bottom-right.
    t.hline(3, 12, 3, QUARTZ[1])
    t.vline(3, 3, 12, QUARTZ[1])
    t.hline(3, 12, 12, QUARTZ[2])
    t.vline(12, 3, 12, QUARTZ[2])
    t.rect(4, 4, 11, 11, CLEAR)
    # A glint on the glass.
    t.set(5, 5, WHITE)
    t.set(6, 4, QUARTZ[0])
    t.set(4, 6, QUARTZ[0])
    t.set(10, 11, QUARTZ[2])
    return t


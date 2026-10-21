"""Textures of the launch controller: a Cold War launch-control rack cabinet on its generator plinth.

The controller is drawn like the equipment racks of a Titan II or Minuteman launch control centre: grey-green enamel steel,
bolted side panels, a front of rack units (a green phosphor scope, a lamp bar, two meters, two key switches for the two-man
rule and guarded toggles), standing on a dark plinth that houses the motor-generator whose flywheel shows through its grille.

The big faces are painted where they sit on the block (the models use the default, position-derived UVs), so
``silo_controller_front`` is the north face as the operator sees it, ``silo_controller_side`` both sides, and so on. The small
fittings take their faces from the ``silo_controller_parts`` atlas by explicit UV; ``PARTS`` names its regions for the models in
``models/block/silo_controller/`` (body, item and the ``SiloPartials`` flywheel and lamp lenses). Register block textures with
``@block``; this module is imported at the end of ``blocks.py``.
"""

from blocks import block  # noqa: F401
from palette import BRASS, CONSOLE, DARK, RADIUM, STEEL
from pixelkit import Ramp, Tex, fbm, mix, shade

# Painted enamel picks up a little warm grime; steel trim stays cool.
ENAMEL = CONSOLE
LAMP_RED = Ramp("#3a1a19", "#5e2421", "#8c2c25", "#c23a2c", "#ea5a3f", "#ff8d68", "#ffd2b8")
LAMP_AMBER = Ramp("#3d2a12", "#62411a", "#93611f", "#cf8a26", "#f4b23a", "#ffd56e", "#fff1c0")
LAMP_GREEN = Ramp("#14301d", "#1f4a29", "#2f7037", "#4ea447", "#7fd85e", "#b8f58c", "#e8ffd2")
PHOSPHOR = RADIUM
# Safety red of the switch guards and the flywheel's balance mark.
GUARD_RED = Ramp("#3e1210", "#6a1c17", "#8f251d", "#b33326", "#d04a35", "#e8705a", "#f6a58f")

# Atlas regions of ``silo_controller_parts`` as (u0, v0, u1, v1) texels, for the models' explicit UVs.
PARTS = {
    "screen": (0, 0, 4, 3),
    "bezel": (4, 0, 8, 4),
    "dial": (8, 0, 12, 4),
    "chrome": (12, 0, 14, 2),
    "brass": (14, 0, 16, 2),
    "red": (12, 2, 14, 4),
    "black": (14, 2, 16, 4),
    "housing": (0, 4, 6, 6),
    "lens_green": (6, 4, 8, 6), "lens_amber": (8, 4, 10, 6), "lens_red": (10, 4, 12, 6),
    "lit_green": (6, 6, 8, 8), "lit_amber": (8, 6, 10, 8), "lit_red": (10, 6, 12, 8),
    "toggle_base": (0, 6, 2, 8),
    "conduit": (12, 4, 16, 8),
    "enamel": (0, 8, 8, 12),
    "junction": (8, 8, 16, 12),
    "trim": (0, 12, 16, 13),
}


def _enamel(t, x0, y0, x1, y1, seed, base=3.3, ramp=ENAMEL):
    """Grey-green stoved enamel: almost flat, a faint mottle so large faces are not dead."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            # Three tones between two ramp steps, so a big face is mottled but never blotchy.
            v = base + (fbm(x, y, seed, 16, 16, octaves=2, scale=4.0) - 0.5) * 0.9
            k = int(v)
            t.set(x, y, mix(ramp[k], ramp[k + 1], round((v - k) * 2) / 2))


def _screw(t, x, y, ramp=STEEL):
    t.set(x, y, ramp[5])


# ======================================================================================
# the cabinet's faces, painted where they sit on the block
# ======================================================================================

@block("silo_controller_front")
def silo_controller_front():
    """The north face as the operator sees it (u = 16 - x, v = 16 - y): two rack rails with their square holes either side
    of three panel units (scope and lamps, meters and keys, guarded switches), with a soft shadow under each fitting."""
    t = Tex(seed=960)
    t.rect(0, 0, 15, 15, DARK[1])
    _enamel(t, 1, 0, 14, 11, 960)
    # Three rack units, y 10..15 (rows 1..5), 7..10 (6..8) and 4..7 (9..11): lit from above, a groove between.
    for v0, v1 in ((1, 5), (6, 8), (9, 11)):
        t.hline(2, 13, v0, ENAMEL[5])
        t.hline(2, 13, v1, ENAMEL[2])
        t.vline(2, v0, v1, ENAMEL[4])
        t.vline(13, v0, v1, ENAMEL[2])
        for x in (3, 12):
            _screw(t, x, v0 + 1 if v1 - v0 > 2 else v0)
    for v in (5, 8):
        t.hline(2, 13, v, ENAMEL[1])
    t.hline(1, 14, 0, ENAMEL[1])
    t.hline(1, 14, 11, ENAMEL[1])
    # Fittings throw a pixel of shadow on the enamel below them (scope, lamp bar, meters).
    for x in range(3, 8):
        t.set(x, 5, shade(t.get(x, 5), 0.8))
    for x in range(8, 12):
        t.set(x, 9, shade(t.get(x, 9), 0.85))
    # Rack rails (x 14..15 and 1..2): bright steel uprights pierced with square cage-nut holes.
    for u in (1, 14):
        t.vline(u, 1, 11, STEEL[3])
        t.set(u, 1, STEEL[4])
        t.set(u, 11, STEEL[2])
    # Plinth rows (y 0..4): the dark generator housing behind the grille.
    for y in range(12, 16):
        t.hline(0, 15, y, DARK[1] if y < 15 else DARK[0])
    return t


@block("silo_controller_side")
def silo_controller_side():
    """Both sides (symmetric front to back): a bolted, removable side panel with a pair of louvres at the top and the
    enamel lip of the cabinet frame around it."""
    t = Tex(seed=961)
    t.rect(0, 0, 15, 15, DARK[1])
    _enamel(t, 1, 0, 14, 11, 961, base=3.0)
    # The cabinet frame: a lip at the front and back edges and under the top cap.
    t.vline(1, 0, 11, ENAMEL[2])
    t.vline(14, 0, 11, ENAMEL[2])
    t.hline(1, 14, 0, ENAMEL[1])
    t.hline(1, 14, 11, ENAMEL[1])
    # The side panel, slightly proud: lit edge above and to the left, shadowed below and to the right.
    _enamel(t, 3, 2, 12, 10, 962, base=3.6)
    t.hline(2, 13, 1, ENAMEL[5])
    t.vline(2, 1, 10, ENAMEL[5])
    t.hline(2, 13, 10, ENAMEL[1])
    t.vline(13, 1, 10, ENAMEL[1])
    t.set(13, 1, ENAMEL[3])
    t.set(2, 10, ENAMEL[3])
    for x, y in ((3, 2), (12, 2), (3, 9), (12, 9)):
        t.set(x, y, ENAMEL[6])
        t.set(x + 1 if x < 12 else x - 1, y, ENAMEL[2])
    # Louvres: a dark slot under a lit lip, twice.
    for v in (3, 5):
        t.hline(5, 10, v, ENAMEL[0])
        t.hline(5, 10, v + 1, ENAMEL[5])
        t.set(4, v, ENAMEL[2])
        t.set(11, v, ENAMEL[2])
    for y in range(12, 16):
        t.hline(0, 15, y, DARK[1] if y < 15 else DARK[0])
    return t


@block("silo_controller_back")
def silo_controller_back():
    """The rear door (u = x, v = 16 - y): hinged on the x 14 side, a louvre at the top for the generator's air, bolts."""
    t = Tex(seed=963)
    t.rect(0, 0, 15, 15, DARK[1])
    _enamel(t, 1, 0, 14, 11, 963, base=3.0)
    t.hline(1, 14, 0, ENAMEL[1])
    t.hline(1, 14, 11, ENAMEL[1])
    # The door leaf.
    _enamel(t, 2, 1, 13, 10, 964, base=3.5)
    t.hline(2, 13, 1, ENAMEL[5])
    t.vline(2, 1, 10, ENAMEL[5])
    t.hline(2, 13, 10, ENAMEL[1])
    t.vline(13, 1, 10, ENAMEL[1])
    # Louvre block above the junction box.
    for v in (2, 4):
        t.hline(4, 11, v, ENAMEL[0])
        t.hline(4, 11, v + 1, ENAMEL[5])
    # A stiffener pressed into the leaf, and its screws.
    for x, y in ((3, 2), (12, 2), (3, 9), (12, 9)):
        _screw(t, x, y)
    for y in range(12, 16):
        t.hline(0, 15, y, DARK[1] if y < 15 else DARK[0])
    return t


@block("silo_controller_top")
def silo_controller_top():
    """The top cap (u = x, v = z): an enamel lid screwed down at its corners round the generator's exhaust grille."""
    t = Tex(seed=965)
    _enamel(t, 0, 0, 15, 15, 965, base=3.6)
    t.frame(0, 0, 15, 15, ENAMEL[1])
    t.hline(1, 14, 1, ENAMEL[5])
    t.vline(1, 1, 14, ENAMEL[5])
    t.hline(1, 14, 14, ENAMEL[2])
    t.vline(14, 1, 14, ENAMEL[2])
    # Exhaust grille: a bolted square frame round a ring of slots.
    t.rect(4, 4, 11, 11, DARK[2])
    t.frame(4, 4, 11, 11, STEEL[3])
    t.hline(4, 11, 4, STEEL[5])
    t.vline(4, 4, 11, STEEL[5])
    t.hline(5, 11, 11, STEEL[1])
    t.vline(11, 5, 11, STEEL[1])
    for v in (6, 8):
        t.hline(5, 10, v, DARK[0])
        t.hline(5, 10, v + 1, DARK[3])
    t.hline(5, 10, 5, DARK[3])
    t.hline(5, 10, 10, DARK[1])
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        t.set(x, y, STEEL[6])
    # The lid's fixing screws.
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t.set(x, y, ENAMEL[6])
        t.set(x + 1, y + 1, ENAMEL[2])
    return t


@block("silo_controller_plinth")
def silo_controller_plinth():
    """The generator plinth and its base plate: near-black frame steel, a little worn on the edges."""
    t = Tex(seed=966)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 966, 16, 16, octaves=2, scale=4.0) - 0.5
            t.set(x, y, DARK[int(round(2.0 + n * 1.2))])
    # Rails of the grille frame (y 3..4 and 1..2 on the sides) and the base plate's edge (y 0..1).
    t.hline(0, 15, 12, DARK[4])
    t.hline(0, 15, 13, DARK[2])
    t.hline(0, 15, 14, DARK[3])
    t.hline(0, 15, 15, DARK[1])
    for x in (1, 14):
        t.set(x, 15, STEEL[4])
    return t


@block("silo_controller_flywheel")
def silo_controller_flywheel():
    """The motor-generator's flywheel: a spoked steel disc seen from above (x, z 4..12) and its rim, with a bright balance
    weight and dark lightening slots so the spin shows through the grille."""
    t = Tex(seed=967)
    t.rect(0, 0, 15, 15, STEEL[2])
    # Disc (top and bottom faces, positional over x/z 4..12).
    cx = cy = 8.0
    for y in range(4, 12):
        for x in range(4, 12):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            c = STEEL[3]
            if d > 3.2:
                c = STEEL[4]
            elif d > 2.3:
                c = DARK[1] if (x + y) % 2 == 0 else STEEL[2]
            elif d > 1.3:
                c = STEEL[3]
            else:
                c = BRASS[3]
            t.set(x, y, c)
    t.set(5, 5, STEEL[5])
    t.set(10, 10, STEEL[1])
    # Rim band (side faces, rows 12..14): steel with alternating slots.
    for x in range(16):
        for y in (12, 13, 14):
            c = STEEL[6] if y == 12 else STEEL[5] if y == 13 else STEEL[3]
            if x % 4 in (1, 2) and y == 13:
                c = DARK[1]
            t.set(x, y, c)
    # The balance weight's face (rows 0..2): painted red so a turn reads at a glance.
    for x in range(16):
        for y in (0, 1, 2):
            t.set(x, y, t.get(x, 12 + y))
    t.rect(2, 0, 5, 2, GUARD_RED[3])
    t.hline(2, 5, 0, GUARD_RED[5])
    t.hline(2, 5, 2, GUARD_RED[1])
    return t


# ======================================================================================
# the fittings atlas
# ======================================================================================

def _lens(t, x, y, ramp, lit):
    """A 2x2 domed lens: a glint at the top left, the body, a darker lower rim."""
    if lit:
        t.set(x, y, ramp[6])
        t.set(x + 1, y, ramp[5])
        t.set(x, y + 1, ramp[5])
        t.set(x + 1, y + 1, ramp[4])
    else:
        t.set(x, y, mix(ramp[3], STEEL[5], 0.45))
        t.set(x + 1, y, ramp[1])
        t.set(x, y + 1, ramp[1])
        t.set(x + 1, y + 1, ramp[0])


@block("silo_controller_parts")
def silo_controller_parts():
    t = Tex(seed=968)
    t.rect(0, 0, 15, 15, DARK[2])
    # Scope: a trace with a pulse on near-black green glass.
    glass = shade(PHOSPHOR[0], 0.6)
    t.rect(0, 0, 3, 2, glass)
    t.stamp(["..6.",
             "45.4",
             "...."], {"4": PHOSPHOR[4], "5": PHOSPHOR[5], "6": PHOSPHOR[6]}, 0, 0)
    t.set(0, 0, PHOSPHOR[1])
    # Bezel steel: the scope's frame and hood, dark and lit from above.
    t.rect(4, 0, 7, 3, DARK[3])
    t.hline(4, 7, 0, DARK[5])
    t.hline(4, 7, 3, DARK[1])
    # Meter face: cream, a darker scale arc along the top ending in a red band.
    face = (230, 226, 204, 255)
    t.rect(8, 0, 11, 3, face)
    t.hline(8, 10, 0, mix(face, DARK[2], 0.35))
    t.set(11, 0, GUARD_RED[4])
    t.set(8, 3, mix(face, DARK[2], 0.12))
    t.set(11, 3, mix(face, DARK[2], 0.12))
    # Chrome, brass, red guard and black knob swatches, each lit from the top left.
    for (x, y), ramp in (((12, 0), STEEL), ((14, 0), BRASS), ((12, 2), GUARD_RED), ((14, 2), DARK)):
        t.set(x, y, ramp[6] if ramp is not DARK else ramp[4])
        t.set(x + 1, y, ramp[5] if ramp is not DARK else ramp[3])
        t.set(x, y + 1, ramp[4] if ramp is not DARK else ramp[2])
        t.set(x + 1, y + 1, ramp[2] if ramp is not DARK else ramp[1])
    # Lamp bar: a black housing with a lit top edge.
    t.rect(0, 4, 5, 5, DARK[1])
    t.hline(0, 5, 4, DARK[3])
    # Lenses, dark then lit.
    for i, ramp in enumerate((LAMP_GREEN, LAMP_AMBER, LAMP_RED)):
        _lens(t, 6 + 2 * i, 4, ramp, False)
        _lens(t, 6 + 2 * i, 6, ramp, True)
    # Toggle base: a black plate with the switch's chrome nut.
    t.rect(0, 6, 1, 7, DARK[1])
    t.set(0, 6, DARK[3])
    t.set(1, 7, STEEL[4])
    # Conduit steel: galvanised, shaded across its width.
    for x, c in zip(range(12, 16), (STEEL[5], STEEL[4], STEEL[3], STEEL[1])):
        t.vline(x, 4, 7, c)
    t.hline(12, 15, 7, STEEL[2])
    # Enamel swatch and the junction box's lid (a bolted cover).
    _enamel(t, 0, 8, 7, 11, 969, base=3.4)
    _enamel(t, 8, 8, 15, 11, 970, base=2.6, ramp=STEEL)
    t.hline(8, 15, 8, STEEL[4])
    t.vline(8, 8, 11, STEEL[4])
    t.hline(8, 15, 11, STEEL[1])
    t.vline(15, 8, 11, STEEL[1])
    for x, y in ((9, 9), (14, 9)):
        _screw(t, x, y)
    # Trim strip for the cap's edges.
    t.hline(0, 15, 12, ENAMEL[2])
    t.rect(0, 13, 15, 15, DARK[1])
    return t

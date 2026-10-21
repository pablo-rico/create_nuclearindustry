"""Textures of the missile area: the silo, the rocket sections, the radar, its scope and the holographic command table.

Register block textures with ``@block``, items with ``@item`` and screens with ``@gui``; this module is imported at the end of
``blocks.py``.
"""

import math

from blocks import SHEET_BLOCKS, block  # noqa: F401
from common import bolt, casing, flange
from gui import gui
from items import item, mask_from, sculpt
from palette import BRASS, CONCRETE, COPPER, CYAN, DANGER, DARK, IN_ACCENT, MIMIC_BLUE, RADIUM, STEEL, WARN, WHITE
from pixelkit import Ramp, Tex, bevel, brushed, fbm, glow, hazard, item_outline, mix, outline, shade


# ======================================================================================
# the silo: poured concrete lined with steel, and the port propellant is pumped in by
# ======================================================================================

def _silo_concrete(t, seed):
    """Smooth poured concrete with the faint marks of its formwork and a few pores."""
    for x in range(16):
        for y in range(16):
            n = fbm(x, y, seed, 16, 16, octaves=3, scale=5.0)
            t.set(x, y, CONCRETE.t(0.42 + 0.28 * n))
    for y in (5, 10):
        for x in range(16):
            t.set(x, y, shade(t.get(x, y), 0.9))
    for x, y in ((3, 2), (12, 7), (6, 12), (14, 13), (9, 3)):
        t.set(x, y, CONCRETE[1])


@block("silo_wall")
def silo_wall():
    t = Tex(seed=910)
    _silo_concrete(t, 910)
    # The steel liner's edge: a band round the slab, bolted at the corners.
    outline(t, 0, 0, 15, 15, DARK)
    t.hline(1, 14, 1, STEEL[4])
    t.vline(1, 1, 14, STEEL[4])
    t.hline(1, 14, 14, STEEL[1])
    t.vline(14, 1, 14, STEEL[1])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(t, x, y, STEEL)
    return t


# ======================================================================================
# guidance: the star tracker, a small brass-bound telescope on a gimballed steel base
# ======================================================================================

STAR_TRACKER = [
    "................",
    ".........####...",
    "........######..",
    ".......#######..",
    "......#######...",
    ".....#######....",
    "....#######.....",
    "...######.......",
    "..######........",
    "...####.........",
    "...#####........",
    "..########......",
    ".##########.....",
    ".##########.....",
    "..########......",
    "................",
]


# ======================================================================================
# the rocket's sections: one off-white fuselage twelve pixels across, stacked from engine
# to payload. Cold War livery: no lettering, no roll marks; detail is geometry plus the
# panel seams, welds, corrugations and bolt heads drawn here.
#
# Side faces are mapped one pixel a pixel from the block (u from the position across the
# face, v = 16 - y), so every row of a body texture is a height in the block and stacked
# sections line up. The 32x32 engine textures put two texels in each pixel.
# ======================================================================================

# The hull ramp other modules share (the fairing, the battery); the sections use PAINT below.
HULL = Ramp("#6f7473", "#8c918e", "#a7aba6", "#c0c3bc", "#d6d8d0", "#e7e8e0", "#f5f5ee")

# The sections' paint, a warm off-white; bare aluminium for corrugated and structural skin; the solid motor case's grey
# steel; LOX frost; the nozzles' tube wall; the re-entry vehicles' heat shields.
PAINT = Ramp("#4b4d49", "#6d6f69", "#8f918a", "#afb1a8", "#c9cac0", "#dcdbd1", "#ecebe2")
ALU = Ramp("#363c40", "#4f575c", "#6b7378", "#899094", "#a6acad", "#c1c6c5", "#dadddb")
CASE = Ramp("#292e2c", "#3a413e", "#4e5652", "#636c67", "#7a837d", "#929b94", "#adb5ad")
FROST = Ramp("#6a7f93", "#8ea2b4", "#b0c2d0", "#cbd8e1", "#dfe7ed", "#edf2f5", "#fafcfd")
TUBE = Ramp("#17191c", "#24282c", "#33393e", "#454c51", "#596166", "#70797d", "#8d969a")
HEAT = Ramp("#2a1d1b", "#47302a", "#6a4632", "#8e5f3c", "#b07c48", "#c99a5c", "#e2be7e")
HEAT_BLUE = Ramp("#1c2230", "#2a3346", "#3a4762", "#4f5e7e", "#6a7a98", "#8b99b2", "#b0bccd")
RV_OLIVE = Ramp("#25291c", "#363d29", "#4a5237", "#5f6847", "#757f59", "#8d986f", "#a9b38b")
ABLATIVE = Ramp("#261b15", "#3b2a20", "#523b2c", "#6a4d39", "#836148", "#9d795b", "#b99573")
CARBON = Ramp("#0f1113", "#181b1e", "#23272a", "#30353a", "#41474b", "#565d62", "#71797d")
RV_LEAD = Ramp("#26292b", "#363b3d", "#4a5052", "#5f6668", "#767d7e", "#8e9594", "#a9afac")
GRAPHITE = Ramp("#141618", "#1e2124", "#2a2e32", "#373c41", "#474d52", "#5b6267", "#767d82")
# The burnt-orange of a nozzle's injector seen down its throat.
PLASMA_DIM = (90, 52, 34, 255)
FOIL = Ramp("#4f350c", "#7a5414", "#a3741f", "#c69332", "#ddb04f", "#efcd79", "#fbe7ad")

SHEET_BLOCKS.update({name: (32, 32) for name in (
    "rocket_engine_skirt", "rocket_engine_bell", "rocket_engine_exit", "rocket_engine_heatshield", "rocket_engine_parts",
    "rocket_interstage")})


def _skin(t, seed, ramp, base=0.66, amp=0.14, scale=4.0, x0=0, y0=0, x1=None, y1=None):
    """Painted skin with a faint mottle; seamless, since the noise wraps."""
    x1 = t.w - 1 if x1 is None else x1
    y1 = t.h - 1 if y1 is None else y1
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, t.w, t.h, octaves=2, scale=scale)
            t.set(x, y, ramp.t(max(0.0, min(1.0, base + amp * (n - 0.5) * 2))))


def _dim(t, x, y, f):
    t.set(x, y, shade(t.get(x, y), f))


@block("rocket_skin")
def rocket_skin():
    """The tanks' painted skin: faint stringers either side of each flat's middle and a girth weld at the top of each barrel
    section (row 0), riveted under it."""
    t = Tex(seed=940)
    _skin(t, 940, PAINT)
    for y in range(16):
        for x, f in ((5, 0.965), (6, 1.02), (9, 0.965), (10, 1.02), (2, 0.95), (13, 1.03)):
            _dim(t, x, y, f)
    for x in range(16):
        _dim(t, x, 0, 0.9)
        _dim(t, x, 1, 1.03)
        if x % 3 == 1:
            _dim(t, x, 2, 0.92)
    return t


@block("rocket_skin_top")
def rocket_skin_top():
    """A bulkhead seen from above or below: the dome's light falls off to its rim, a soft weld ring and a central fitting."""
    t = Tex(seed=941)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            n = fbm(x, y, 941, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, PAINT.t(max(0.0, min(1.0, 0.86 - d * 0.03 + 0.08 * (n - 0.5) - 0.015 * (x + y) / 4))))
    t.ring(8, 8, 5.4, 4.7, PAINT[3])
    t.rect(7, 7, 8, 8, STEEL[3])
    t.set(7, 7, STEEL[5])
    t.set(8, 8, STEEL[1])
    return t


@block("rocket_ring")
def rocket_ring():
    """The section rings, two rows a style (a ring two pixels tall shows one pair): rows 0-1 the joint ring between sections,
    bare steel with its bolt heads; 2-3 a solid motor's clevis joint, case grey with pin heads; 4-5 the heavy thrust ring;
    6-7 a white-painted stiffener; 8-15 plain steel for the faces nobody sees."""
    t = Tex(seed=942)
    styles = ((STEEL, 0.66, 0.38, 4, 1), (CASE, 0.6, 0.3, 3, 0), (DARK, 0.62, 0.34, 2, 1), (PAINT, 0.74, 0.5, 0, 0))
    for i, (ramp, hi, lo, step, off) in enumerate(styles):
        for x in range(16):
            n = fbm(x, i, 942, 16, 16, octaves=2, scale=4.0)
            t.set(x, 2 * i, ramp.t(hi + 0.1 * (n - 0.5)))
            t.set(x, 2 * i + 1, ramp.t(lo + 0.1 * (n - 0.5)))
        if step:
            for x in range(off, 16, step):
                t.set(x, 2 * i, ramp[6])
                t.set(x, 2 * i + 1, ramp[1])
    brushed(t, STEEL, 0, 8, 15, 15, base=3.2, amp=0.5, seed=943)
    return t


@block("rocket_deck")
def rocket_deck():
    """The top of a joint ring, where a narrower section stands on it: a steel separation deck, a bolt circle near its rim and
    a radial web out from the middle."""
    t = Tex(seed=944)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 944, 16, 16, octaves=2, scale=5.0)
            t.set(x, y, STEEL.t(0.6 + 0.12 * (n - 0.5) - 0.02 * (x + y) / 6))
    for i in range(2, 14):
        _dim(t, i, i, 0.9)
        _dim(t, i, 15 - i, 0.9)
    t.ring(8, 8, 3.2, 2.4, STEEL[2])
    for a in range(0, 360, 30):
        bx, by = round(8 + 6.0 * math.cos(math.radians(a)) - 0.5), round(8 + 6.0 * math.sin(math.radians(a)) - 0.5)
        t.set(bx, by, STEEL[6])
    t.frame(0, 0, 15, 15, STEEL[2])
    return t


@block("rocket_solid_case")
def rocket_solid_case():
    """A solid motor's steel case: smooth grey, a faint lighter band where it is lit and a line of tie-down bolts low on each
    segment (row 12)."""
    t = Tex(seed=945)
    _skin(t, 945, CASE, base=0.64, amp=0.12, scale=3.0)
    for y in range(16):
        for x, f in ((3, 1.05), (4, 1.06), (5, 1.04), (12, 0.94), (13, 0.92)):
            _dim(t, x, y, f)
    for x in range(1, 16, 3):
        t.set(x, 12, CASE[6])
        t.set(x, 13, CASE[1])
    return t


def _frost(t, seed, y0=0):
    for y in range(y0, 16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, octaves=3, scale=6.0)
            t.set(x, y, FROST.t(max(0.0, min(1.0, 0.62 + 0.42 * (n - 0.5)))))
    rng = t.rng
    for y in range(y0, 16):
        for x in range(16):
            r = rng.random()
            if r < 0.07:
                t.set(x, y, FROST[6])
            elif r < 0.1:
                t.set(x, y, FROST[2])


@block("rocket_frost")
def rocket_frost():
    """Frost on a cryogenic tank's wall below its liquid level: white-blue rime with a few glints; seamless."""
    t = Tex(seed=946)
    _frost(t, 946)
    return t


@block("rocket_frost_edge")
def rocket_frost_edge():
    """The frost's ragged top edge (rows 0-3, painted skin where the rime has not reached), frost below."""
    t = Tex(seed=947)
    _frost(t, 947)
    for x in range(16):
        h = int(fbm(x, 0, 947, 16, 16, octaves=2, scale=8.0) * 4.2)
        for y in range(h):
            t.set(x, y, PAINT.t(0.66 + 0.06 * fbm(x, y, 940, 16, 16)))
        if h < 4:
            t.set(x, h, FROST[3])
    return t


@block("rocket_fittings")
def rocket_fittings():
    """The small hardware, four columns a part: u 0-3 an aluminium feed line lit from the left with a flange every eight
    rows; 4-7 the cable tunnel's ribbed cover; 8-11 dark steel brackets and braces; 12-15 four colour patches four rows each:
    the fuel valve's red, the oxidiser's blue, brass, black rubber."""
    t = Tex(seed=948)
    for y in range(16):
        for x, k in zip(range(4), (1, 5, 4, 2)):
            t.set(x, y, ALU[k])
        for x in range(4, 8):
            t.set(x, y, PAINT[5] if y % 4 else PAINT[3])
        t.set(4, y, shade(t.get(4, y), 0.93))
        t.set(7, y, shade(t.get(7, y), 0.9))
    for y in (0, 8):
        for x in range(4):
            t.set(x, y, ALU[6] if x == 1 else ALU[3])
    brushed(t, DARK, 8, 0, 11, 15, base=3.0, amp=0.5, seed=949, axis="v")
    for x in range(8, 12):
        t.set(x, 0, DARK[5])
    for i, ramp in enumerate((DANGER, MIMIC_BLUE, BRASS, DARK)):
        for y in range(4 * i, 4 * i + 4):
            for x in range(12, 16):
                t.set(x, y, ramp[4 if (x + y) % 3 else 3] if ramp is not DARK else DARK[1])
        t.set(12, 4 * i, ramp[6])
    return t


@block("rocket_guidance")
def rocket_guidance():
    """The guidance and control section: painted skin, an access door on each flat (u 5-10, rows 3-12) with its four
    fasteners, and a rivet line round each end frame."""
    t = Tex(seed=950)
    _skin(t, 950, PAINT, base=0.7, amp=0.1)
    x0, y0, x1, y1 = 5, 3, 10, 12
    t.hline(x0, x1, y0, PAINT[3])
    t.vline(x0, y0, y1, PAINT[3])
    t.hline(x0 + 1, x1, y1, PAINT[6])
    t.vline(x1, y0 + 1, y1, PAINT[6])
    for x, y in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
        t.set(x, y, PAINT[2])
    for x in range(0, 16, 2):
        t.set(x, 1, PAINT[4])
        t.set(x, 14, PAINT[4])
    return t


@block("rocket_guidance_band")
def rocket_guidance_band():
    """The avionics bay's radiator band: dark louvres, a slat every other row, broken every four columns."""
    t = Tex(seed=951)
    brushed(t, DARK, base=2.4, amp=0.4, seed=951)
    for y in range(16):
        for x in range(16):
            if x % 4 == 3:
                t.set(x, y, DARK[1])
            elif y % 2 == 0:
                t.set(x, y, DARK[4] if x % 4 else DARK[5])
            else:
                t.set(x, y, DARK[1])
    return t


def _rv(seed, ramp, tip, base=0.6, amp=0.16, scale=4.0):
    """A re-entry vehicle's heat shield: rows 0-1 are its nose tip (``tip`` ramp), the rest the shield, darker towards the
    nose where it would char."""
    t = Tex(seed=seed)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, octaves=2, scale=scale)
            t.set(x, y, ramp.t(max(0.0, min(1.0, base + amp * (n - 0.5) * 2 - 0.12 * (15 - y) / 15 + 0.04 * (8 - x) / 8))))
    for x in range(16):
        t.set(x, 0, tip[5 if x < 8 else 4])
        t.set(x, 1, tip[3 if x < 8 else 2])
    return t


@block("rocket_rv_conventional")
def rocket_rv_conventional():
    """The conventional warhead's blunt body: olive-drab painted steel, two panel lines."""
    t = _rv(952, RV_OLIVE, STEEL)
    for x in range(2, 16):
        t.set(x, 6, RV_OLIVE[2])
        t.set(x, 11, RV_OLIVE[2])
        t.set(x, 12, RV_OLIVE[5])
    for x in range(3, 16, 4):
        t.set(x, 9, RV_OLIVE[1])
    return t


@block("rocket_rv_fission")
def rocket_rv_fission():
    """The fission RV: brown phenolic ablator, finely mottled, with a graphite nose."""
    t = _rv(953, ABLATIVE, DARK, scale=8.0)
    for y in range(2, 16):
        for x in range(16):
            if t.rng.random() < 0.08:
                t.set(x, y, ABLATIVE[1])
    return t


@block("rocket_rv_thermonuclear")
def rocket_rv_thermonuclear():
    """The thermonuclear RV: black carbon-phenolic with a faint woven sheen and a bright metal nose tip."""
    t = _rv(954, CARBON, STEEL, base=0.5, amp=0.1)
    for y in range(2, 16):
        for x in range(16):
            if (x + 2 * y) % 5 == 0:
                _dim(t, x, y, 1.15)
    return t


@block("rocket_rv_dirty")
def rocket_rv_dirty():
    """The dirty bomb's casing: heavy lead-grey cast with weld seams and a ring of bolts."""
    t = _rv(955, RV_LEAD, RV_LEAD, base=0.58, amp=0.12)
    for y in range(2, 16):
        _dim(t, 7, y, 0.88)
        _dim(t, 8, y, 1.08)
    for x in range(1, 16, 3):
        t.set(x, 4, RV_LEAD[6])
        t.set(x, 5, RV_LEAD[1])
    return t


@block("rocket_rv_platform")
def rocket_rv_platform():
    """The payload's mounting platform and its fittings: machined steel, bolt holes and a darker bore."""
    t = Tex(seed=956)
    brushed(t, STEEL, base=3.6, amp=0.5, seed=956)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12), (7, 2), (2, 7), (12, 7), (7, 12)):
        t.set(x, y, STEEL[1])
        t.set(x + 1, y + 1, STEEL[6])
    t.rect(6, 6, 9, 9, DARK[2])
    t.rect(7, 7, 8, 8, DARK[0])
    return t


# -- the engines: 32x32 ------------------------------------------------------------------

@block("rocket_engine_skirt")
def rocket_engine_skirt():
    """The thrust section's skin: corrugated aluminium (a crest every pixel), a stiffening ring every six pixels, lit from the
    left."""
    t = Tex(32, 32, 960)
    for y in range(32):
        for x in range(32):
            n = fbm(x, y, 960, 32, 32, octaves=2, scale=4.0)
            crest = (x % 2 == 0)
            k = (0.7 if crest else 0.5) + 0.1 * (n - 0.5) - 0.05 * x / 31
            t.set(x, y, ALU.t(max(0.0, min(1.0, k))))
    for y0 in (0, 12, 24):
        for x in range(32):
            t.set(x, y0, ALU[2])
            t.set(x, y0 + 1, ALU[6] if x % 6 else ALU[3])
            t.set(x, y0 + 2, ALU[4])
    return t


@block("rocket_engine_bell")
def rocket_engine_bell():
    """A regeneratively cooled nozzle's tube wall, throat (row 0) to exit (row 31): brazed tubes two texels a tube, dark steel
    going heat-blue then bronze towards the lip, a hat band every eight rows."""
    t = Tex(32, 32, 961)
    for y in range(32):
        f = y / 31
        for x in range(32):
            n = fbm(x, y, 961, 32, 32, octaves=2, scale=6.0)
            v = (0.6 if x % 2 == 0 else 0.34) + 0.12 * (n - 0.5)
            c = TUBE.t(max(0.0, min(1.0, v)))
            if f > 0.35:
                c = mix(c, HEAT_BLUE.t(v), min(1.0, (f - 0.35) * 2.2) * 0.7)
            if f > 0.7:
                c = mix(c, HEAT.t(v + 0.1), (f - 0.7) * 2.4)
            t.set(x, y, c)
    for y in (7, 15, 23):
        for x in range(32):
            t.set(x, y, TUBE[5])
            t.set(x, y + 1, TUBE[1])
    for x in range(32):
        t.set(x, 30, HEAT[6])
        t.set(x, 31, HEAT[2])
    return t


@block("rocket_engine_exit")
def rocket_engine_exit():
    """A nozzle seen from below: the bronze lip, the sooty wall going darker to the throat, a dull glow of the injector deep
    inside."""
    t = Tex(32, 32, 962)
    for y in range(32):
        for x in range(32):
            # Square-ish distance, since the bells are square slabs.
            d = max(abs(x + 0.5 - 16), abs(y + 0.5 - 16)) * 0.55 + math.hypot(x + 0.5 - 16, y + 0.5 - 16) * 0.45
            n = fbm(x, y, 962, 32, 32, octaves=2, scale=6.0)
            if d > 14.5:
                c = HEAT[5] if (x + y) % 2 else HEAT[4]
            elif d > 13.5:
                c = HEAT[1]
            else:
                c = TUBE.t(max(0.0, min(1.0, 0.1 + d / 13.5 * 0.5 + 0.1 * (n - 0.5))))
                if (int(d) % 3 == 0):
                    c = shade(c, 0.85)
            t.set(x, y, c)
    t.disc(16, 16, 3.2, HEAT[0])
    t.disc(16, 16, 2.0, PLASMA_DIM)
    t.disc(16, 16, 1.0, HEAT[3])
    return t



@block("rocket_engine_heatshield")
def rocket_engine_heatshield():
    """The base heat shield seen from below: quilted dark panels, stitched in squares of four pixels, sooted."""
    t = Tex(32, 32, 963)
    for y in range(32):
        for x in range(32):
            n = fbm(x, y, 963, 32, 32, octaves=3, scale=5.0)
            lx, ly = x % 8, y % 8
            v = 0.45 + 0.2 * (n - 0.5) + (0.12 if lx in (2, 3) and ly in (2, 3) else 0) - (0.08 if lx > 4 and ly > 4 else 0)
            t.set(x, y, mix(DARK.t(v), HEAT[2], 0.18))
            if lx == 7 or ly == 7:
                t.set(x, y, DARK[0])
    return t


@block("rocket_engine_parts")
def rocket_engine_parts():
    """The engine's hardware, a quarter each: top left the engine fairings' riveted aluminium; top right the hold-down posts'
    forged dark steel with bolts; bottom left the turbopump's brass castings; bottom right the chambers' and feed lines'
    steel."""
    t = Tex(32, 32, 964)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 964, 32, 32, octaves=2, scale=4.0)
            t.set(x, y, ALU.t(0.66 + 0.12 * (n - 0.5) - 0.12 * x / 15))
    for y in range(1, 16, 4):
        for x in range(1, 16, 2):
            t.set(x, y, ALU[6])
    for x in range(16):
        t.set(x, 0, ALU[3])
    brushed(t, DARK, 16, 0, 31, 15, base=3.2, amp=0.5, seed=965, axis="v")
    for x in range(16, 32):
        t.set(x, 0, DARK[6])
        t.set(x, 15, DARK[0])
    for x, y in ((18, 3), (28, 3), (18, 11), (28, 11), (23, 7)):
        bolt(t, x, y, DARK)
    brushed(t, BRASS, 0, 16, 15, 31, base=3.4, amp=0.7, seed=966)
    for y in (19, 24, 29):
        for x in range(16):
            t.set(x, y, BRASS[1])
            t.set(x, y - 1, BRASS[5])
    brushed(t, STEEL, 16, 16, 31, 31, base=3.0, amp=0.6, seed=967, axis="v")
    for y in (20, 27):
        for x in range(16, 32):
            t.set(x, y, STEEL[6])
            t.set(x, y + 1, STEEL[1])
    return t


@block("rocket_interstage")
def rocket_interstage():
    """The interstage's frame members: white-painted aluminium extrusions with a rivet line down their middle and a darker
    edge, lit from the left."""
    t = Tex(32, 32, 965)
    for y in range(32):
        for x in range(32):
            n = fbm(x, y, 965, 32, 32, octaves=2, scale=4.0)
            t.set(x, y, PAINT.t(0.64 + 0.1 * (n - 0.5) - 0.08 * (x % 8) / 7))
    for y in range(32):
        for x in range(0, 32, 8):
            t.set(x, y, PAINT[6])
            t.set(x + 7, y, PAINT[2])
        if y % 3 == 0:
            for x in range(3, 32, 8):
                t.set(x, y, PAINT[3])
    for x in range(32):
        if x % 8 not in (0, 7):
            t.set(x, 0, PAINT[6])
            t.set(x, 31, PAINT[2])
    return t


@block("rocket_engine_nozzle")
def rocket_engine_nozzle():
    """Dark, sooty, scorched metal: the interceptor round's fins and nozzle (``models/entity/interceptor_round.json``)."""
    t = Tex(seed=932)
    brushed(t, DARK, base=1.8, amp=0.6, seed=932)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 932, 16, 16, octaves=2, scale=3.0)
            if n > 0.62:
                t.set(x, y, shade(t.get(x, y), 0.7))
    outline(t, 0, 0, 15, 15, DARK[0])
    return t



@item("star_tracker")
def star_tracker():
    t = Tex(16, 16, 900)
    pts = mask_from(STAR_TRACKER)
    base = {(x, y) for (x, y) in pts if y >= 10}
    tube = pts - base
    sculpt(t, base, DARK, base=3.8, seed=900, grain=0.4)
    sculpt(t, tube, BRASS, base=3.6, seed=901, grain=0.5)
    # Bands round the tube, and the gimbal pin it turns on.
    for x, y in ((5, 6), (6, 5), (7, 4), (9, 3), (10, 2)):
        t.set(x, y, BRASS[1])
    t.set(4, 9, STEEL[6])
    t.set(5, 10, STEEL[5])
    # The objective lens, looking up and out, with a star caught in it.
    for x, y in ((10, 1), (11, 1), (12, 1), (12, 2), (13, 2), (13, 3)):
        t.set(x, y, CYAN[4])
    t.set(12, 1, CYAN[6])
    t.set(11, 2, WHITE)
    # Status lamps on the base.
    t.set(3, 12, RADIUM[6])
    t.set(8, 12, WARN[5])
    glow(t, 11.5, 2.0, 3.5, CYAN[6], 0.3)
    item_outline(t, DARK[0])
    return t


# ======================================================================================
# the radar in the world: an olive housing with a turntable, the dish, and the scope
# ======================================================================================

def _olive_casing(seed):
    t = Tex(seed=seed)
    brushed(t, OLIVE, base=3.3, amp=0.6, seed=seed)
    bevel(t, 0, 0, 15, 15, OLIVE, hi=OLIVE[5], lo=OLIVE[1])
    outline(t, 0, 0, 15, 15, OLIVE[0])
    return t


@block("missile_radar_side")
def missile_radar_side():
    t = _olive_casing(970)
    # Louvres for the transmitter's cooling, and a lamp that shows it is fed.
    for y in (3, 5, 7):
        t.hline(3, 12, y, OLIVE[0])
        t.hline(3, 12, y + 1, OLIVE[5])
    t.rect(3, 10, 8, 13, DARK[1])
    outline(t, 3, 10, 8, 13, OLIVE[0])
    t.set(5, 11, RADIUM[6])
    t.set(6, 11, RADIUM[4])
    t.set(5, 12, RADIUM[3])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, OLIVE)
    return t


@block("missile_radar_top")
def missile_radar_top():
    t = _olive_casing(971)
    # The turntable the dish's mast stands on: a steel ring on a bearing race.
    t.disc(8, 8, 6.6, STEEL[2])
    t.ring(8, 8, 6.6, 5.6, STEEL[5])
    t.ring(8, 8, 4.4, 3.6, STEEL[1])
    t.disc(8, 8, 3.0, DARK[2])
    for a in range(0, 360, 45):
        r = math.radians(a)
        t.set(int(8 + math.cos(r) * 5.0), int(8 + math.sin(r) * 5.0), STEEL[6])
    return t


@block("missile_radar_bottom")
def missile_radar_bottom():
    t = _olive_casing(972)
    t.disc(8, 8, 3.2, DARK[1])
    t.ring(8, 8, 3.2, 2.4, STEEL[4])
    return t


@block("radar_dish_face")
def radar_dish_face():
    """The reflector seen from the front: pale grey panels, ribs radiating from the feed, the rim darker."""
    t = Tex(seed=973)
    light = Ramp("#5e6766", "#7b8584", "#98a2a0", "#b3bcb9", "#c9d1cd", "#dce3de", "#eef2ee")
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2) ** 0.5
            n = fbm(x, y, 973, 16, 16, octaves=2, scale=4.0)
            t.set(x, y, light.t(0.75 - d / 16 + 0.15 * n))
    for a in range(0, 360, 30):
        r = math.radians(a)
        for k in range(2, 8):
            t.set(int(8 + math.cos(r) * k), int(8 + math.sin(r) * k), light[2])
    t.ring(8, 8, 4.6, 3.9, light[3])
    t.disc(8, 8, 1.4, DARK[2])
    return t


@block("radar_dish_back")
def radar_dish_back():
    """Behind the reflector: steel trusses on a grey skin."""
    t = Tex(seed=974)
    brushed(t, STEEL, base=2.8, amp=0.5, seed=974)
    for i in range(16):
        t.set(i, i, STEEL[5])
        t.set(15 - i, i, STEEL[5])
    t.hline(0, 15, 7, STEEL[1])
    t.vline(7, 0, 15, STEEL[1])
    outline(t, 0, 0, 15, 15, STEEL[0])
    return t


@block("radar_scope_front")
def radar_scope_front():
    """The scope's face when nothing is drawn on it: a round phosphor tube dark in an olive bezel."""
    t = _olive_casing(975)
    t.disc(8, 8, 6.9, STEEL[3])
    t.ring(8, 8, 6.9, 6.0, STEEL[5])
    t.disc(8, 8, 5.9, PHOSPHOR[2])
    t.disc(8, 8, 3.0, PHOSPHOR[3])
    t.set(6, 5, PHOSPHOR[5])
    return t


@block("radar_scope_side")
def radar_scope_side():
    t = _olive_casing(976)
    for y in range(3, 13, 2):
        t.hline(4, 11, y, OLIVE[1])
    return t


# ======================================================================================
# the holographic command table: a dark console whose top is a projection glass
# ======================================================================================

HOLO = Ramp("#020a0c", "#05161a", "#0a242a", "#10363e", "#1a4e58", "#2a7480", "#5ad8e8")


@block("holo_table_glass")
def holo_table_glass():
    """Projection glass: dark blue-black, a faint cyan lattice that runs on across the pieces without a seam."""
    t = Tex(seed=980)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 980, 16, 16, octaves=2, scale=3.0)
            t.set(x, y, mix(HOLO[1], HOLO[2], 0.35 + 0.35 * n))
    for i in (0, 8):
        for k in range(16):
            t.set(i, k, mix(t.get(i, k), HOLO[4], 0.28))
            t.set(k, i, mix(t.get(k, i), HOLO[4], 0.28))
    return t


@block("holo_table_rim")
def holo_table_rim():
    """The frame round the glass: dark steel with a brass lip along it."""
    t = Tex(seed=982)
    brushed(t, DARK, base=3.2, amp=0.6, seed=982)
    t.hline(0, 15, 0, DARK[5])
    t.hline(0, 15, 1, BRASS[4])
    t.hline(0, 15, 2, BRASS[2])
    t.hline(0, 15, 13, BRASS[3])
    t.hline(0, 15, 14, BRASS[1])
    t.hline(0, 15, 15, DARK[0])
    for x in (2, 13):
        t.set(x, 7, DARK[6])
        t.set(x, 8, DARK[1])
    return t


@block("holo_table_casing")
def holo_table_casing():
    """The console's panels, legs and stretchers: dark industrial casing with a cyan status slot."""
    t = Tex(seed=981)
    casing(t, DARK, seed=981, base=2.6, amp=0.6)
    t.rect(6, 7, 9, 8, HOLO[3])
    outline(t, 5, 6, 10, 9, DARK[0])
    t.set(7, 7, HOLO[6])
    t.set(8, 7, HOLO[5])
    return t


@block("holo_table_lens")
def holo_table_lens():
    """The projector's emitter: rings of cyan glass round a bright core."""
    t = Tex(seed=983)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2:
                c = HOLO[6]
            elif d < 4:
                c = mix(HOLO[5], HOLO[6], 0.4 if int(d * 2) % 2 else 0.0)
            elif d < 6:
                c = mix(HOLO[3], HOLO[5], 0.5 if int(d * 2) % 2 else 0.0)
            else:
                c = BRASS[2] if d < 7.2 else DARK[1]
            t.set(x, y, c)
    return t


# ======================================================================================
# the radar screen: an olive console round a phosphor scope (RadarLayout.java holds the same numbers)
# ======================================================================================

# Olive-drab enamel of a military console, and the phosphor scope's greens.
OLIVE = Ramp("#14180f", "#1e2418", "#2a3221", "#38412c", "#48533a", "#5d694b", "#7a8664")
PHOSPHOR = Ramp("#020803", "#041207", "#071d0c", "#0b2a12", "#113a19", "#1a4d22", "#256230")
RADAR_W, RADAR_H = 384, 256
R_TITLE = (6, 4, 372, 13)
R_PPI = (6, 20, 212, 212)
PPI_CX, PPI_CY, PPI_RADIUS, BEZEL = 112, 126, 99, 105
R_ZOOM_OUT = (6, 236, 15, 15)
R_ZOOM_IN = (23, 236, 15, 15)
R_RANGE = (40, 236, 64, 15)
R_CENTRE = (106, 236, 30, 15)
R_ZONE = (138, 236, 80, 15)
R_TABLE = (224, 20, 154, 92)
TABLE_HEADER, TABLE_ROW, TABLE_ROWS = 11, 10, 8
R_DETAIL = (224, 116, 154, 62)
R_PROFILE = (224, 182, 154, 50)
R_CODE = (224, 236, 44, 15)
R_FILTER = (270, 236, 50, 15)
R_ACK = (322, 236, 56, 15)


def _box(r):
    x, y, w, h = r
    return x, y, x + w - 1, y + h - 1


def _glass(t, r, ramp=PHOSPHOR, seed=0):
    """A recessed phosphor window: dark glass with faint grain, sunk into the enamel."""
    x0, y0, x1, y1 = _box(r)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            n = fbm(x, y, seed, RADAR_W, RADAR_H, octaves=3, scale=2.0)
            t.set(x, y, mix(ramp[1], ramp[3], 0.35 + 0.25 * n))
    t.hline(x0, x1, y0, OLIVE[0])
    t.vline(x0, y0, y1, OLIVE[0])
    t.hline(x0 + 1, x1, y0 + 1, PHOSPHOR[0])
    t.vline(x0 + 1, y0 + 1, y1, PHOSPHOR[0])
    t.hline(x0, x1, y1, OLIVE[5])
    t.vline(x1, y0, y1, OLIVE[5])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[1])


def _button(t, r, face=OLIVE, glyph=None):
    """A raised push button; `glyph` paints its legend."""
    x0, y0, x1, y1 = _box(r)
    t.rect(x0, y0, x1, y1, face[3])
    for y in range(y0, y1 + 1):
        t.hline(x0, x1, y, mix(face[4], face[2], (y - y0) / max(1, y1 - y0)))
    bevel(t, x0, y0, x1, y1, face, hi=face[6], lo=face[1])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[0])
    if glyph:
        glyph(t, (x0 + x1) // 2, (y0 + y1) // 2)


def _minus(t, cx, cy):
    t.hline(cx - 3, cx + 3, cy, RADIUM[6])


def _plus(t, cx, cy):
    t.hline(cx - 3, cx + 3, cy, RADIUM[6])
    t.vline(cx, cy - 3, cy + 3, RADIUM[6])


def _crosshair(t, cx, cy):
    t.ring(cx + 0.5, cy + 0.5, 4.2, 3.2, RADIUM[6])
    t.set(cx, cy, RADIUM[6])
    for d in (-6, 6):
        t.set(cx + d, cy, RADIUM[5])
        t.set(cx, cy + d, RADIUM[5])


def _radar_panel():
    t = Tex(RADAR_W, RADAR_H, 960)
    brushed(t, OLIVE, base=3.3, amp=0.55, seed=960, scale=11.0)
    # A raised outer frame and the heavy screws of a field console.
    bevel(t, 0, 0, RADAR_W - 1, RADAR_H - 1, OLIVE, hi=OLIVE[6], lo=OLIVE[0])
    bevel(t, 1, 1, RADAR_W - 2, RADAR_H - 2, OLIVE, hi=OLIVE[5], lo=OLIVE[1])
    outline(t, 0, 0, RADAR_W - 1, RADAR_H - 1, OLIVE[0])
    for x, y in ((2, 2), (RADAR_W - 4, 2), (2, RADAR_H - 4), (RADAR_W - 4, RADAR_H - 4), (220, 2), (220, RADAR_H - 4)):
        bolt(t, x, y, OLIVE)
    # Title strip, with a lamp socket at its right end.
    x0, y0, x1, y1 = _box(R_TITLE)
    t.rect(x0, y0, x1, y1, OLIVE[1])
    for x in range(x0, x1 + 1, 4):
        t.set(x, y1, OLIVE[2])
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OLIVE[0])
    t.hline(x0 - 1, x1 + 1, y1 + 2, OLIVE[5])
    t.disc(x1 - 6 + 0.5, y0 + 6 + 0.5, 4.0, OLIVE[0])
    t.disc(x1 - 6 + 0.5, y0 + 6 + 0.5, 3.0, mix(DARK[1], RADIUM[1], 0.4))
    # The scope: a sunk square holding a steel bezel and the round phosphor glass.
    x0, y0, x1, y1 = _box(R_PPI)
    t.rect(x0, y0, x1, y1, OLIVE[1])
    outline(t, x0, y0, x1, y1, OLIVE[0])
    t.hline(x0, x1, y1, OLIVE[4])
    t.vline(x1, y0, y1, OLIVE[4])
    for y in range(y0 + 1, y1):
        for x in range(x0 + 1, x1):
            d = ((x + 0.5 - PPI_CX) ** 2 + (y + 0.5 - PPI_CY) ** 2) ** 0.5
            if d <= PPI_RADIUS:
                # Phosphor glass, a shade brighter in the middle as a lit tube is.
                n = fbm(x, y, 961, RADAR_W, RADAR_H, octaves=3, scale=2.0)
                glow_ = max(0.0, 1.0 - d / PPI_RADIUS)
                t.set(x, y, mix(PHOSPHOR[0], PHOSPHOR[4], 0.15 + 0.55 * glow_ * glow_ + 0.12 * n))
            elif d <= BEZEL + 0.5:
                # Steel bezel lit from the top left: bright where its face looks that way.
                ang = math.atan2(y + 0.5 - PPI_CY, x + 0.5 - PPI_CX)
                light = -math.cos(ang + math.pi / 4)
                ramp = STEEL if d > PPI_RADIUS + 1.5 else DARK
                k = 3.0 + 1.6 * light * (1 if d > (PPI_RADIUS + BEZEL) / 2 else -0.6)
                t.set(x, y, ramp[int(round(max(1, min(6, k))))])
            elif d <= BEZEL + 1.5:
                t.set(x, y, OLIVE[0])
    # Bearing ticks round the bezel: every 10°, longer and brighter every 30°.
    for deg in range(0, 360, 10):
        a = math.radians(deg)
        major = deg % 30 == 0
        r0 = PPI_RADIUS + 1.2
        r1 = PPI_RADIUS + (4.6 if major else 2.8)
        for k in range(8):
            r = r0 + (r1 - r0) * k / 7
            t.set(int(PPI_CX + math.sin(a) * r), int(PPI_CY - math.cos(a) * r), RADIUM[6] if major else STEEL[6])
    for x, y in ((x0 + 2, y0 + 2), (x1 - 3, y0 + 2), (x0 + 2, y1 - 3), (x1 - 3, y1 - 3)):
        bolt(t, x, y, OLIVE)
    # Under the scope: zoom, range readout, recentre and the zone switch.
    _button(t, R_ZOOM_OUT, glyph=_minus)
    _button(t, R_ZOOM_IN, glyph=_plus)
    _glass(t, R_RANGE, seed=962)
    _button(t, R_CENTRE, glyph=_crosshair)
    _button(t, R_ZONE)
    zx0, zy0, zx1, zy1 = _box(R_ZONE)
    t.ring(zx0 + 8.5, (zy0 + zy1) / 2 + 0.5, 4.3, 3.3, WARN[5])
    # The track table: a header band and faint rules between rows.
    _glass(t, R_TABLE, seed=963)
    x0, y0, x1, y1 = _box(R_TABLE)
    t.rect(x0 + 2, y0 + 2, x1 - 1, y0 + TABLE_HEADER - 1, PHOSPHOR[4])
    t.hline(x0 + 2, x1 - 1, y0 + TABLE_HEADER, PHOSPHOR[6])
    for row in range(1, TABLE_ROWS):
        y = y0 + TABLE_HEADER + row * TABLE_ROW
        for x in range(x0 + 3, x1 - 1, 2):
            t.set(x, y, PHOSPHOR[4])
    # Detail panel, and the side view with its grid.
    _glass(t, R_DETAIL, seed=964)
    _glass(t, R_PROFILE, seed=965)
    x0, y0, x1, y1 = _box(R_PROFILE)
    for gx in range(x0 + 2, x1, 15):
        for y in range(y0 + 2, y1, 2):
            t.set(gx, y, PHOSPHOR[4])
    for gy in range(y1 - 4, y0 + 1, -10):
        for x in range(x0 + 2, x1, 2):
            t.set(x, gy, PHOSPHOR[4])
    # Network code window, filter switch and the alarm acknowledge button in its guard.
    _glass(t, R_CODE, seed=966)
    _button(t, R_FILTER)
    x0, y0, x1, y1 = _box(R_ACK)
    hazard(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, WARN[5], DARK[0], period=6, seed=967)
    _button(t, (R_ACK[0] + 2, R_ACK[1] + 2, R_ACK[2] - 4, R_ACK[3] - 4), face=Ramp(*DANGER[:]))
    return t


@gui("radar_screen")
def radar_screen():
    return _radar_panel()


@gui("radar_scope_mask")
def radar_scope_mask():
    """The scope's square of the panel with its glass cut out: drawn over the picture, it rounds it off under the bezel."""
    panel = _radar_panel()
    x, y, w, h = R_PPI
    t = Tex(w, h, 968)
    for j in range(h):
        for i in range(w):
            d = ((x + i + 0.5 - PPI_CX) ** 2 + (y + j + 0.5 - PPI_CY) ** 2) ** 0.5
            if d > PPI_RADIUS:
                t.set(i, j, panel.get(x + i, y + j))
    return t

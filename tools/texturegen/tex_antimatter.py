"""Textures for antimatter containment: Penning traps, the magnetic conduit and the vial.

Register block textures with ``@block`` and item textures with ``@item``; this module is imported
at the end of ``blocks.py``.

Every trap is a glass cylinder held between two magnet heads. The heads share one material set
(steel caps, exposed copper windings, a glowing field band) and only the band changes colour with
the tier: cyan for the small trap, violet for the medium one, amber for the large one and green for
the portable vial. The models in ``models/block/penning_trap*`` and ``antimatter_conduit`` sample
these textures by their block coordinates, so the windings, ribs and bands are periodic in the
direction their elements are thin: any 1-2 px slice still shows a lit edge and a shadow.

Glass is drawn opaque here; the models make it see-through with a vertex alpha
(``neoforge_data.color``), the way the Cherenkov glow of the uranium rod does. The cloud textures
are black-backed so the renderer can draw them additively without any alpha channel.
"""

import math

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from common import bolt
from fusion_materials import GRAPHITE, TITANIUM, panel
from items import item  # noqa: F401
from palette import BRASS, COPPER, DANGER, DARK, GLASS_OFF, STEEL, WARN, WHITE
from pixelkit import Ramp, Tex, bevel, brushed, fbm, hazard, item_outline, mix, outline, rivet

BLACK = (0, 0, 0, 255)


def _tier_ramp(base):
    """A dark -> light ramp round one bright accent colour: index 4 is the colour itself."""
    c = tuple(int(base[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
    black, white = (8, 10, 14, 255), (255, 255, 255, 255)
    return Ramp(mix(black, c, 0.22), mix(black, c, 0.42), mix(black, c, 0.62), mix(black, c, 0.82), c,
                mix(c, white, 0.45), mix(c, white, 0.8))


# Tier colours (the plan's table): the band, the status of the cloud and the item accents.
SMALL = _tier_ramp("#3FD8E8")
MEDIUM = _tier_ramp("#9B5CFF")
LARGE = _tier_ramp("#FFB23F")
VIAL = _tier_ramp("#6BFF8A")

# Clear borosilicate glass: a faint blue-white, its reflections near white.
GLASS = Ramp("#5d7680", "#87a3ad", "#a9c3cc", "#c4d9df", "#d8e8ec", "#ebf4f6", "#ffffff")


# ======================================================================================
# magnet heads
# ======================================================================================

@block("penning_trap_steel")
def trap_steel():
    """Plain brushed steel for the caps' sides and the tie rods: no frame, so it tiles."""
    t = Tex(seed=1300)
    brushed(t, STEEL, base=3.6, amp=0.55, seed=1300, axis="h", scale=6.0)
    for y in (0, 8):
        t.hline(0, 15, y, STEEL[5])
    for y in (7, 15):
        t.hline(0, 15, y, STEEL[2])
    return t


@block("penning_trap_plate")
def trap_plate():
    """The heads' end plates, seen from above or below: a bevelled plate with four bolts."""
    t = Tex(seed=1301)
    brushed(t, STEEL, base=3.5, amp=0.5, seed=1301, scale=5.0)
    outline(t, 0, 0, 15, 15, STEEL[1])
    bevel(t, 1, 1, 14, 14, STEEL, hi=STEEL[5], lo=STEEL[1])
    # The pole piece's machined ring round the glass seat.
    t.ring(7.5, 7.5, 5.6, 4.8, STEEL[2])
    t.ring(7.5, 7.5, 4.8, 4.1, STEEL[5])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(t, x, y, STEEL)
    return t


@block("penning_trap_pole")
def trap_pole():
    """The pole face the glass stands on, seen through the glass from the front and from above:
    polished steel round a bright brass end-cap electrode. Kept light all over - a dark centre
    read as an opaque disc floating in the middle of the empty cylinder, worst at night."""
    t = Tex(seed=1302)
    brushed(t, STEEL, base=4.6, amp=0.4, seed=1302, scale=7.0)
    t.ring(7.5, 7.5, 6.0, 5.4, STEEL[3])
    t.ring(7.5, 7.5, 5.4, 4.9, STEEL[6])
    t.disc(7.5, 7.5, 2.4, BRASS[4])
    t.ring(7.5, 7.5, 2.4, 1.8, BRASS[3])
    t.set(6, 6, BRASS[6])
    t.set(7, 6, BRASS[5])
    t.set(8, 8, BRASS[3])
    return t


@block("penning_trap_coil")
def trap_coil():
    """Exposed copper windings. Every row is one turn on a 2 px period - lit crown, shaded flank -
    with a sparse glint along each crown, so even a 1 px slice of a coil reads as wire."""
    t = Tex(seed=1303)
    for y in range(16):
        for x in range(16):
            n = (fbm(x, y, 1303, 16, 16, 2, 6.0) - 0.5) * 1.0
            t.set(x, y, COPPER[int(round((4.6 if y % 2 == 0 else 2.4) + n))])
            if y % 2 == 0 and (x * 5 + y * 3) % 11 == 0:
                t.set(x, y, COPPER[6])
    return t


@block("penning_trap_copper")
def trap_copper():
    """The coils' end faces: the turns seen edge-on as fine concentric lines."""
    t = Tex(seed=1304)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            t.set(x, y, COPPER[5] if int(d) % 2 == 0 else COPPER[2])
    return t


def _band(ramp, seed):
    """The field band: a strip of light in the tier's colour. Bright core rows on a 2 px period
    with sparse white glints, flat enough to read as emissive at any thickness."""
    t = Tex(seed=seed)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 2, 4.0)
            t.set(x, y, ramp[6] if y % 2 == 0 and n > 0.62 else ramp[5] if y % 2 == 0 else ramp[4])
    return t


@block("penning_trap_band_small")
def band_small():
    return _band(SMALL, 1310)


@block("penning_trap_band_medium")
def band_medium():
    return _band(MEDIUM, 1311)


@block("penning_trap_band_large")
def band_large():
    return _band(LARGE, 1312)


@block("penning_trap_band_vial")
def band_vial():
    return _band(VIAL, 1313)


# ======================================================================================
# glass
# ======================================================================================

@block("penning_trap_glass")
def trap_glass():
    """The cylinder's wall, unrolled: column u = angle round the trap. The octagonal models give
    each panel a 2 px column, so the bright reflection lands on one panel and a faint one on the
    opposite side. Darker rims at top and bottom where it seats in the pole pieces."""
    t = Tex(seed=1320)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1320, 16, 16, 2, 4.0) - 0.5
            t.set(x, y, GLASS[int(round(3.3 + n * 1.2))])
    # Each panel gets a 2 px column: a slightly darker first column marks the facet edges.
    for y in range(1, 15):
        for x in range(0, 16, 2):
            t.set(x, y, GLASS[2] if fbm(x, y, 1321, 16, 16, 2, 4.0) < 0.6 else GLASS[3])
    for y in range(1, 15):
        t.set(2, y, GLASS[6])
        t.set(3, y, GLASS[5])
        t.set(10, y, GLASS[5])
        t.set(13, y, GLASS[2])
        t.set(14, y, GLASS[2])
    for x in range(16):
        t.set(x, 0, GLASS[1])
        t.set(x, 15, GLASS[0])
        t.set(x, 1, GLASS[2])
        t.set(x, 14, GLASS[1])
    return t


# ======================================================================================
# fittings: vial port, field gauge, LEDs, brass clamp, cable entry, fins, plinth
# ======================================================================================

@block("penning_trap_port")
def trap_port():
    """The vial port on the front: a brass collar round a dark socket with a green-lit rim,
    and two arrows - in above the socket, out below it - because it fills and drains."""
    t = Tex(seed=1330)
    brushed(t, BRASS, base=3.4, amp=0.5, seed=1330)
    outline(t, 0, 0, 15, 15, BRASS[0])
    bevel(t, 1, 1, 14, 14, BRASS, hi=BRASS[6], lo=BRASS[1])
    t.rect(4, 3, 11, 12, DARK[0])
    outline(t, 4, 3, 11, 12, VIAL[3])
    t.hline(5, 10, 4, VIAL[1])
    t.rect(6, 5, 9, 10, DARK[1])
    t.vline(6, 5, 10, DARK[2])
    t.set(7, 6, VIAL[5])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        rivet(t, x, y, BRASS)
    return t


@block("penning_trap_gauge")
def trap_gauge():
    """The analogue field gauge's face. The renderer draws the needle (field strength 0..1) over
    it, pivoting on the centre (8, 8) and sweeping clockwise from 225 degrees (empty field, lower
    left) to -45 degrees (full field, lower right). The arc is red where the field cannot hold
    the cloud, green at the working field and amber over the rated field."""
    t = Tex(seed=1331)
    t.rect(0, 0, 15, 15, BRASS[2])
    t.disc(8, 8, 7.9, BRASS[4])
    t.ring(8, 8, 7.9, 7.1, BRASS[5])
    t.disc(8, 8, 7.0, (232, 226, 205, 255))
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, 8 - (y + 0.5)
            r = math.hypot(dx, dy)
            if not 4.6 <= r <= 6.2:
                continue
            a = math.degrees(math.atan2(dy, dx))
            sweep = (225 - a) % 360
            if sweep > 270:
                continue
            f = sweep / 270
            t.set(x, y, DANGER[4] if f < 0.25 else VIAL[2] if f < 0.8 else WARN[4])
    for i in range(6):
        a = math.radians(225 - 54 * i)
        t.set(int(8 + 3.9 * math.cos(a)), int(8 - 3.9 * math.sin(a)), DARK[1])
    t.rect(7, 7, 8, 8, DARK[0])
    t.set(7, 7, STEEL[5])
    t.hline(6, 9, 12, DARK[3])
    return t


@block("penning_trap_led")
def trap_led():
    """An LED lens when off: dark glass with one glint. Lit LEDs are drawn over it."""
    t = Tex(seed=1332)
    t.rect(0, 0, 15, 15, GLASS_OFF[1])
    t.disc(7.5, 7.5, 6.2, GLASS_OFF[2])
    t.disc(7.5, 7.5, 4.0, GLASS_OFF[3])
    t.rect(4, 4, 6, 5, GLASS_OFF[6])
    outline(t, 0, 0, 15, 15, DARK[0])
    return t


@block("penning_trap_led_lit")
def trap_led_lit():
    """A lit LED, near white so the renderer's vertex colour sets its hue."""
    t = Tex(seed=1333)
    t.rect(0, 0, 15, 15, (196, 196, 196, 255))
    t.disc(7.5, 7.5, 6.5, (228, 228, 228, 255))
    t.disc(7.5, 7.5, 4.2, (255, 255, 255, 255))
    return t


@block("penning_trap_brass")
def trap_brass():
    """The medium trap's ring clamp and gauge housing: turned brass, a groove every 4 px."""
    t = Tex(seed=1334)
    brushed(t, BRASS, base=3.8, amp=0.5, seed=1334, axis="h", scale=7.0)
    for y in (0, 4, 8, 12):
        t.hline(0, 15, y, BRASS[5])
        t.hline(0, 15, y + 3, BRASS[2])
    return t


@block("penning_trap_fins")
def trap_fins():
    """The large heads' cooling fins: vertical ribs on a 2 px period."""
    t = Tex(seed=1335)
    for y in range(16):
        for x in range(16):
            n = (fbm(x, y, 1335, 16, 16, 2, 5.0) - 0.5) * 1.0
            t.set(x, y, STEEL[int(round((5 if x % 2 == 0 else 2) + n))])
    t.hline(0, 15, 0, STEEL[6])
    t.hline(0, 15, 15, STEEL[1])
    return t


@block("penning_trap_cable")
def trap_cable():
    """The large trap's cable entry: a bolted gland plate with three insulated feeders."""
    t = Tex(seed=1336)
    brushed(t, DARK, base=3.3, amp=0.5, seed=1336)
    outline(t, 0, 0, 15, 15, DARK[0])
    bevel(t, 1, 1, 14, 14, DARK, hi=DARK[5], lo=DARK[1])
    for cx, c in ((4.5, DANGER), (8.0, Ramp(*[(30 + 14 * i, 30 + 14 * i, 34 + 14 * i, 255) for i in range(7)])),
                  (11.5, WARN)):
        t.disc(cx, 8, 2.0, c[3])
        t.ring(cx, 8, 2.0, 1.3, c[5])
        t.set(int(cx) - 1, 7, c[6])
        t.set(int(cx), 9, c[1])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        rivet(t, x, y, DARK)
    return t


@block("penning_trap_plinth")
def trap_plinth():
    """The large trap's plinth. Its 2 px skirt samples the bottom rows, so the yellow-and-black
    band sits there, under a lit edge; the rest is plain steel."""
    t = Tex(seed=1337)
    brushed(t, STEEL, base=3.0, amp=0.5, seed=1337)
    hazard(t, 0, 14, 15, 15, WARN[4], DARK[0], period=4, seed=1337)
    t.hline(0, 15, 13, STEEL[5])
    t.hline(0, 15, 0, STEEL[5])
    return t


# ======================================================================================
# antimatter cloud (drawn additively by the trap renderer; black is transparent)
# ======================================================================================

@block("antimatter_cloud")
def cloud():
    """A soft greyscale glow, white core to black edge. The renderer tints it with the tier colour."""
    t = Tex(seed=1340)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8.0
            v = max(0.0, 1.0 - d) ** 1.8
            g = int(round(255 * v))
            t.set(x, y, (g, g, g, 255))
    return t


@block("antimatter_cloud_wisp")
def cloud_wisp():
    """Two spiral arms for the slower rotating layer of the cloud."""
    t = Tex(seed=1341)
    t.rect(0, 0, 15, 15, BLACK)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy) / 8.0
            if r > 1.0:
                continue
            a = math.atan2(dy, dx)
            arm = 0.5 + 0.5 * math.cos(2 * a - r * 7.0)
            v = arm ** 3 * (1.0 - r) ** 1.2 * 1.6
            g = int(round(255 * min(1.0, v)))
            t.set(x, y, (g, g, g, 255))
    return t


# ======================================================================================
# magnetic conduit
# ======================================================================================

@block("antimatter_conduit_ring")
def conduit_ring():
    """The conduit's focusing rings: copper windings under a steel strap. The rings are 1 px
    thick, so any 1 px slice must read the same: noise-free copper, one strap every 4 px."""
    t = Tex(seed=1350)
    t.rect(0, 0, 15, 15, COPPER[4])
    for i in range(0, 16, 4):
        t.vline(i, 0, 15, STEEL[3])
        t.hline(0, 15, i, STEEL[3])
    for i in range(2, 16, 4):
        t.vline(i, 0, 15, COPPER[5])
        t.hline(0, 15, i, COPPER[5])
    return t


@block("antimatter_conduit_node")
def conduit_node():
    """The coupler at each conduit joint. Its faces sample the middle 6x6 (5..10): a bevelled steel
    frame round copper windings and a dark lens lit green at its rim."""
    t = Tex(seed=1351)
    brushed(t, STEEL, base=3.5, amp=0.5, seed=1351)
    t.rect(5, 5, 10, 10, STEEL[3])
    bevel(t, 5, 5, 10, 10, STEEL, hi=STEEL[6], lo=STEEL[1])
    t.rect(6, 6, 9, 9, COPPER[4])
    t.hline(6, 9, 6, COPPER[6])
    t.vline(9, 6, 9, COPPER[2])
    t.rect(7, 7, 8, 8, DARK[0])
    t.set(7, 7, VIAL[4])
    t.set(8, 8, VIAL[2])
    outline(t, 0, 0, 15, 15, STEEL[1])
    return t


# ======================================================================================
# vial (item)
# ======================================================================================

def _vial(level):
    """The portable vial: a glass ampoule between two green micro-heads, the antimatter a glowing
    speck held in its middle that grows with the fill (0 = empty, 3 = full)."""
    t = Tex(16, 16, 1360 + level)
    # Heads: steel caps with the green field band.
    for y0, band_y in ((1, 3), (12, 12)):
        for y in range(y0, y0 + 3):
            for x in range(4, 12):
                t.set(x, y, STEEL[5 if y == y0 else 3 if x < 10 else 2])
        t.hline(4, 11, band_y, VIAL[5])
        t.set(4, band_y, VIAL[3])
        t.set(11, band_y, VIAL[3])
    t.hline(6, 9, 0, STEEL[4])
    t.set(7, 0, STEEL[6])
    t.hline(6, 9, 15, STEEL[2])
    # Glass.
    for y in range(4, 12):
        for x in range(5, 11):
            t.set(x, y, GLASS[3])
        t.set(5, y, GLASS[5])
        t.set(6, y, GLASS[6] if 5 <= y <= 9 else GLASS[5])
        t.set(10, y, GLASS[1])
    # The cloud: a glowing speck held in the middle, larger as the vial fills.
    r = (0.0, 1.3, 2.0, 2.8)[level]
    for y in range(4, 12):
        for x in range(6, 10):
            d = math.hypot(x + 0.5 - 8, (y + 0.5 - 8) * 0.8)
            if d < r * 0.45:
                t.set(x, y, WHITE if level == 3 else VIAL[6])
            elif d < r * 0.8:
                t.set(x, y, VIAL[5])
            elif d < r:
                t.set(x, y, VIAL[3])
    item_outline(t, STEEL[0])
    return t


@item("antimatter_vial")
def vial_empty():
    return _vial(0)


@item("antimatter_vial_1")
def vial_1():
    return _vial(1)


@item("antimatter_vial_2")
def vial_2():
    return _vial(2)


@item("antimatter_vial_3")
def vial_3():
    return _vial(3)


@item("antimatter_vial_warning")
def vial_warning():
    """Overlay drawn (full-bright) over a filled vial while its battery is low and the warning
    blinks: the bands and the cap's LED turn red."""
    t = Tex(16, 16, 1365)
    for band_y in (3, 12):
        t.hline(4, 11, band_y, DANGER[5])
    t.rect(7, 1, 8, 1, DANGER[6])
    return t


# ======================================================================================
# annihilation generator
# ======================================================================================
#
# ``models/block/annihilation_generator.json`` faces north (the shaft) and samples these by block
# coordinates: the armoured chamber spans y 4..13 (texture rows 3..12) between the green field
# heads, so the face textures put their lit and shaded edges on rows 3 and 12 and columns 1 and 14.
# Front: the shaft's bearing collar (its own texture) and three mode lamps on row 3. Back: the core
# porthole, an 8x7 opening (columns 4..11, rows 4..10) onto the chamber. Sides: the FE terminals.

GEN_ARMOUR = DARK


def _armour(seed, rows=(3, 12)):
    """Heavy plate: brushed dark steel with a lit top-left edge, a shaded bottom-right one and a
    welded seam two pixels in."""
    t = Tex(seed=seed)
    brushed(t, GEN_ARMOUR, base=3.6, amp=0.5, seed=seed, scale=6.0)
    top, bottom = rows
    t.hline(0, 15, top, GEN_ARMOUR[6])
    t.hline(0, 15, bottom, GEN_ARMOUR[1])
    t.vline(1, top, bottom, GEN_ARMOUR[5])
    t.vline(14, top, bottom, GEN_ARMOUR[1])
    t.vline(0, top, bottom, GEN_ARMOUR[2])
    t.vline(15, top, bottom, GEN_ARMOUR[0])
    t.hline(2, 13, top + 1, GEN_ARMOUR[2])
    t.hline(2, 13, bottom - 1, GEN_ARMOUR[4])
    return t


@block("annihilation_generator_front")
def gen_front():
    """The shaft side: armour with a bolt in each corner and the three mode lamps' bezels on row 3
    (lit by the renderer). The bearing collar is a separate box over the middle."""
    t = _armour(1400)
    for x, y in ((2, 5), (12, 5), (2, 10), (12, 10)):
        bolt(t, x, y, GEN_ARMOUR)
    # A dark rebate under the lamps so they read as one control strip.
    t.hline(3, 12, 4, GEN_ARMOUR[0])
    # Hazard edge along the bottom of the armour, above the field head.
    for x in range(1, 15):
        t.set(x, 12, WARN[4] if (x // 2) % 2 == 0 else DARK[0])
    return t


@block("annihilation_generator_side")
def gen_side():
    """East and west: armour with louvres under the terminal plate and the annihilation mark -
    a ring with a bar through it - between them."""
    t = _armour(1401)
    # Louvres (rows 8..10, under the terminals on rows 5..7).
    for y in (8, 10):
        t.hline(3, 12, y, GEN_ARMOUR[0])
        t.hline(3, 12, y - 1, GEN_ARMOUR[5])
    for x, y in ((2, 4), (12, 4)):
        bolt(t, x, y, GEN_ARMOUR)
    t.set(2, 11, GEN_ARMOUR[6])
    t.set(13, 11, GEN_ARMOUR[6])
    return t


@block("annihilation_generator_back")
def gen_back():
    """The porthole flange round the core window (opening at columns 4..11, rows 4..10): a
    machined steel ring with eight bolts, on armour."""
    t = _armour(1402)
    t.rect(3, 3, 12, 11, STEEL[3])
    bevel(t, 3, 3, 12, 11, STEEL, hi=STEEL[6], lo=STEEL[1])
    # The flange's inner lip, shaded on the top-left where it falls into the recess.
    t.hline(4, 11, 4, STEEL[1])
    t.vline(4, 4, 10, STEEL[1])
    for x, y in ((3, 3), (7, 3), (11, 3), (3, 7), (12, 7), (3, 11), (7, 11), (11, 11)):
        t.set(x, y, STEEL[6])
    t.set(12, 3, STEEL[5])
    t.set(12, 11, STEEL[2])
    return t


@block("annihilation_generator_window")
def gen_window():
    """Inside the porthole, unlit: the chamber's dark wall with its own small Penning trap - pole
    pieces above and below a ring electrode, and a reflection of the glass. Sampled at columns
    4..11, rows 4..10; the renderer covers it with the glowing core while the generator burns."""
    t = Tex(seed=1403)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1403, 16, 16, 2, 4.0) - 0.5
            t.set(x, y, DARK[int(round(1.4 + n))])
    # Pole pieces at the top and bottom of the opening.
    t.hline(5, 10, 4, STEEL[3])
    t.hline(6, 9, 5, STEEL[2])
    t.hline(5, 10, 10, STEEL[2])
    t.hline(6, 9, 9, STEEL[1])
    # Ring electrode round the middle, copper.
    t.ring(7.5, 7.5, 2.6, 1.8, COPPER[2])
    t.set(6, 6, COPPER[4])
    t.set(7, 7, VIAL[1])
    # The glass's reflection: one bright diagonal glint.
    t.set(10, 5, GLASS[4])
    t.set(9, 6, GLASS[2])
    return t


@block("annihilation_generator_core")
def gen_core():
    """The burning core, drawn full-bright over the window by the renderer and tinted by it (output
    and mode): white-hot in the middle, swirling outwards, never black at the edge so the pane reads
    as glowing glass rather than a hole. Greyscale; mapped whole onto the 8x7 opening."""
    t = Tex(seed=1404)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy) / 8.0
            a = math.atan2(dy, dx)
            swirl = 0.5 + 0.5 * math.cos(3 * a - r * 6.5)
            v = 0.32 + 0.68 * max(0.0, 1.0 - r) ** 1.4 + 0.14 * swirl * (1.0 - min(1.0, r))
            n = fbm(x, y, 1404, 16, 16, 2, 4.0) - 0.5
            g = int(round(255 * max(0.22, min(1.0, v + n * 0.12))))
            t.set(x, y, (g, g, g, 255))
    t.rect(7, 7, 8, 8, WHITE)
    return t


@block("annihilation_generator_top")
def gen_top():
    """The top plate, from above (north at the top): the vial port's brass socket in the north half,
    lit green round its rim, and a plain bevelled panel in the south half where the mode's value
    box sits."""
    t = Tex(seed=1405)
    brushed(t, STEEL, base=3.4, amp=0.5, seed=1405, scale=5.0)
    outline(t, 0, 0, 15, 15, STEEL[1])
    bevel(t, 1, 1, 14, 14, STEEL, hi=STEEL[5], lo=STEEL[1])
    # Vial socket.
    t.disc(8, 4.5, 3.2, BRASS[3])
    t.ring(8, 4.5, 3.2, 2.5, BRASS[5])
    t.disc(8, 4.5, 2.0, DARK[0])
    t.ring(8, 4.5, 2.0, 1.4, VIAL[3])
    t.set(7, 3, VIAL[5])
    # Value-box panel.
    t.rect(4, 8, 11, 13, STEEL[2])
    bevel(t, 4, 8, 11, 13, STEEL, hi=STEEL[1], lo=STEEL[5])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(t, x, y, STEEL)
    return t


@block("annihilation_generator_terminal")
def gen_terminal():
    """The FE terminals. Rows 5..10 are the plate (mapped onto an 8x3 face): a copper bus bar on a
    black insulating board with a red (+) end and a black (-) end. Rows 0..3 hold the studs' caps:
    red at columns 0..3, black at 4..7."""
    t = Tex(seed=1406)
    t.rect(0, 0, 15, 15, DARK[0])
    # Stud caps.
    t.rect(0, 0, 3, 3, DANGER[4])
    t.hline(0, 3, 0, DANGER[6])
    t.vline(3, 0, 3, DANGER[1])
    t.rect(4, 0, 7, 3, DARK[1])
    t.hline(4, 7, 0, DARK[4])
    t.vline(7, 0, 3, DARK[0])
    # Board.
    t.rect(0, 5, 15, 10, DARK[1])
    t.hline(0, 15, 5, DARK[3])
    t.hline(0, 15, 10, DARK[0])
    # Copper bus between the two ends.
    t.hline(3, 12, 7, COPPER[5])
    t.hline(3, 12, 8, COPPER[2])
    # + on the red end (south / left as seen from the east), - on the black end.
    t.rect(1, 6, 2, 9, DANGER[3])
    t.rect(13, 6, 14, 9, DARK[3])
    return t


@block("annihilation_generator_bearing")
def gen_bearing():
    """The bearing collar the shaft leaves through, mapped whole onto a 7x7 boss: a turned steel
    ring with four bolts round a dark bore the half shaft covers."""
    t = Tex(seed=1407)
    t.rect(0, 0, 15, 15, STEEL[3])
    t.disc(8, 8, 7.8, STEEL[4])
    t.ring(8, 8, 7.8, 6.8, STEEL[6])
    t.ring(8, 8, 6.0, 5.2, STEEL[2])
    t.disc(8, 8, 4.6, DARK[0])
    t.ring(8, 8, 4.6, 3.9, DARK[2])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(t, x, y, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[1])
    return t


# ======================================================================================
# antiproton injector (tokamak shell)
# ======================================================================================
#
# ``models/block/antiproton_injector.json`` faces north, its outer face, like the other heaters:
# a 3 px titanium frame stands 2 px proud of a 10x10 recess holding a miniature Penning trap (a
# cyan field band below and above a glass chamber), the vial port under it and the ARMED / READY
# lamps on the frame's top bar. The south face looks into the plasma chamber: the beam nozzle.

ANTIPROTON = SMALL


@block("antiproton_injector_front")
def inj_front():
    """The frame (outer 3 px) and the recess floor round the miniature trap. Two lamp lenses on the
    top bar (columns 4..6 ARMED, 10..12 READY, rows 1..2) and a hazard bar along the bottom."""
    t = panel()
    bevel(t, 0, 0, 15, 15, TITANIUM, hi=TITANIUM[5], lo=TITANIUM[1])
    bevel(t, 1, 1, 14, 14, TITANIUM, hi=TITANIUM[4], lo=TITANIUM[2], corner=False)
    t.rect(3, 3, 12, 12, GRAPHITE[1])
    t.hline(3, 12, 3, GRAPHITE[0])
    t.vline(3, 3, 12, GRAPHITE[0])
    t.vline(12, 4, 12, GRAPHITE[2])
    # Lamp bezels with their dark lenses (columns 4..6 and 10..12, rows 1..2), lit by the renderer.
    for x in (3, 9):
        t.rect(x, 0, x + 4, 2, TITANIUM[1])
        t.hline(x, x + 4, 0, TITANIUM[2])
        t.rect(x + 1, 1, x + 3, 2, GLASS_OFF[2])
        t.set(x + 1, 1, GLASS_OFF[5])
    # Hazard bar: antimatter on board.
    for x in range(1, 15):
        t.set(x, 14, WARN[4] if (x // 2) % 2 == 0 else GRAPHITE[0])
    t.hline(1, 14, 13, TITANIUM[2])
    return t


@block("antiproton_injector_side")
def inj_side():
    """The beam line through the shell, front (right as seen from the east) to back: the trap's
    feed, two cyan focusing rings and the decelerator's ceramic stack."""
    t = panel()
    for y, c in ((6, GRAPHITE[1]), (7, TITANIUM[5]), (8, TITANIUM[4]), (9, TITANIUM[2]), (10, GRAPHITE[1])):
        t.hline(2, 13, y, c)
    for x in (4, 7):
        t.vline(x, 5, 11, ANTIPROTON[4])
        t.vline(x + 1, 5, 11, ANTIPROTON[2])
    # Decelerator: ceramic insulators stacked on the beam line.
    for x in (10, 12):
        t.rect(x, 4, x + 1, 12, (214, 226, 226, 255))
        t.vline(x + 1, 4, 12, (170, 190, 192, 255))
    return t


@block("antiproton_injector_top")
def inj_top():
    """Top and bottom: the shell's plain insulated panel with one cyan-capped trap feedthrough."""
    t = panel()
    t.disc(8, 4.5, 1.6, TITANIUM[1])
    t.ring(8, 4.5, 1.6, 0.9, ANTIPROTON[3])
    return t


@block("antiproton_injector_back")
def inj_back():
    """The face into the plasma: a graphite tile with the beam nozzle - concentric copper electrodes
    stepping down to a cyan-lit aperture. The renderer flashes over it when it fires."""
    t = Tex(seed=1410)
    t.rect(0, 0, 15, 15, GRAPHITE[1])
    bevel(t, 0, 0, 15, 15, GRAPHITE, hi=GRAPHITE[3], lo=GRAPHITE[0])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d <= 5.6:
                t.set(x, y, COPPER[5] if int(d) % 2 == 0 else COPPER[1])
    t.disc(8, 8, 1.8, DARK[0])
    t.ring(8, 8, 1.8, 1.1, ANTIPROTON[3])
    t.frame(1, 1, 14, 14, GRAPHITE[2])
    return t


@block("antiproton_injector_chamber")
def inj_chamber():
    """Behind the mini trap's glass: a dark chamber with a ring electrode, seen whole (mapped onto
    the 7x3.5 window)."""
    t = Tex(seed=1411)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 1411, 16, 16, 2, 4.0) - 0.5
            t.set(x, y, DARK[int(round(1.2 + n))])
    for x in range(16):
        t.set(x, 0, STEEL[2])
        t.set(x, 15, STEEL[1])
    t.ring(8, 8, 4.0, 3.0, COPPER[2])
    t.set(5, 5, COPPER[4])
    t.vline(13, 3, 9, GLASS[3])
    return t


@block("antiproton_injector_flash")
def inj_flash():
    """The nozzle's shot, full-bright and tinted by the renderer: a hard white centre and rays."""
    t = Tex(seed=1412)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy) / 8.0
            a = math.atan2(dy, dx)
            rays = max(0.0, math.cos(4 * a)) ** 6 * 0.35
            v = max(0.0, 1.0 - r) ** 1.6 + rays * (1.0 - min(1.0, r))
            g = int(round(255 * max(0.18, min(1.0, v))))
            t.set(x, y, (g, g, g, 255))
    return t

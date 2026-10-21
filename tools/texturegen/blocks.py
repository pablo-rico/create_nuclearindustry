"""Block texture definitions."""

import math
from fusion_materials import FUSION_BLOCKS
from reactor_materials import steel_panel, steel_port, mineral, STEEL as REACTOR_STEEL

from common import (bar_readout, bezel, bolt, cracks, flange, ore,
                    panel_screen, port_face, trefoil)
from palette import (BLACK, BRASS, CONCRETE, COPPER, CRYO, CYAN, DANGER, DARK, IN_ACCENT,
                     LEAD, OUT_ACCENT, PLASMA, RADIUM, STEEL, STONE, TRITIUM, URANIUM,
                     WARN, WHITE)
from pixelkit import (Tex, bevel, brushed, fbm, glow, hazard, inset, item_outline, mix,
                      outline, rivet, scanlines, shade, speckle)

BLOCKS = {}


def block(name):
    def deco(fn):
        BLOCKS[name] = fn
        return fn
    return deco


# ======================================================================================
# ores
# ======================================================================================

@block("uranium_ore")
def uranium_ore():
    return mineral("deepslate", URANIUM)


@block("thorium_ore")
def thorium_ore():
    return mineral("stone", CRYO, offset=2)


@block("borax_ore")
def borax_ore():
    pale = URANIUM.tinted(WHITE, 0.0)
    from pixelkit import Ramp
    borax = Ramp("#2a2b25", "#4d4f43", "#7b7d6c", "#a8aa94", "#cdcfba", "#e7e8dc", "#ffffff")
    return mineral("stone", borax, offset=1)


@block("spodumene_ore")
def spodumene_ore():
    from palette import SPODUMENE
    return mineral("stone", SPODUMENE, offset=3)


# ======================================================================================
# fission tier - casings and structure
# ======================================================================================

@block("reactor_casing")
def reactor_casing():
    return steel_panel()


@block("brocken_reactor_casting")
def brocken_reactor_casting():
    STEEL = REACTOR_STEEL
    t = reactor_casing()
    t.seed = 12
    # soot staining + fractures
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 44, 16, 16, 3, 4.0)
            if n < 0.40:
                t.blend(x, y, DARK[1], min(0.5, (0.40 - n) * 1.8))
    cracks(t, STEEL, seed=91, count=3)
    # buckled plate corner
    for i in range(4):
        t.set(11 + i // 2, 4 + i, STEEL[6])
        t.set(10 + i // 2, 4 + i, STEEL[0])
    t.blend(6, 10, PLASMA[4], 0.30)
    t.blend(7, 10, PLASMA[3], 0.20)
    return t


@block("reactor_opening")
def reactor_opening():
    STEEL = REACTOR_STEEL
    t = Tex(seed=13)
    steel_panel(t)
    # recessed hatch showing the core
    t.rect(3, 3, 12, 12, DARK[1])
    inset(t, 3, 3, 12, 12, STEEL)
    outline(t, 2, 2, 13, 13, STEEL[0])
    for y in range(4, 12, 2):
        t.hline(4, 11, y, DARK[0])
        t.hline(4, 11, y + 1, URANIUM[2])
    glow(t, 8, 8, 5.5, RADIUM[5], 0.4)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        rivet(t, x, y, STEEL)
    return t


@block("control_rod")
def control_rod():
    LEAD = REACTOR_STEEL
    t = Tex(seed=14)
    brushed(t, DARK, base=2.5, amp=0.6, seed=14, axis="v")
    # four boron-carbide rods
    for i, x in enumerate((1, 5, 9, 13)):
        t.rect(x, 0, x + 2, 15, LEAD[3])
        t.vline(x, 0, 15, LEAD[5])
        t.vline(x + 1, 0, 15, LEAD[4])
        t.vline(x + 2, 0, 15, LEAD[1])
    # binding collars
    for y in (2, 8, 13):
        for x in range(16):
            t.set(x, y, LEAD[6] if (x % 4) != 3 else LEAD[2])
            t.set(x, y + 1, LEAD[1])
        t.set(1, y, LEAD[4])
        t.set(14, y, LEAD[4])
    speckle(t, 0, 0, 15, 15, LEAD[6], chance=0.03, seed=14)
    return t


@block("uranium_block")
def uranium_block():
    t = Tex(seed=15)
    brushed(t, URANIUM, base=3.4, amp=0.9, seed=15)
    # cast metal block: chamfered edge, corner marks, faint activity speckle
    outline(t, 0, 0, 15, 15, URANIUM[1])
    bevel(t, 1, 1, 14, 14, URANIUM)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        t.set(x, y, URANIUM[6])
        t.set(x + 1, y, URANIUM[5])
        t.set(x, y + 1, URANIUM[4])
        t.set(x + 1, y + 1, URANIUM[1])
    speckle(t, 2, 2, 13, 13, RADIUM[6], chance=0.04, seed=15)
    glow(t, 8, 8, 9, RADIUM[6], 0.10)
    return t


@block("heat_exchanger_top")
def heat_exchanger_top():
    STEEL = REACTOR_STEEL
    t = Tex(seed=16)
    steel_panel(t)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, STEEL)
    # four pipe bores matching the model's tube positions, kept clear of the
    # horizontal strips the model samples for its side faces (rows 0-3, 8-9, 13-14)
    for cx, cy in ((5.0, 5.0), (11.0, 5.0), (5.0, 11.0), (11.0, 11.0)):
        t.disc(cx, cy, 1.9, STEEL[2])
        t.ring(cx, cy, 1.9, 1.3, COPPER[3])
        t.disc(cx, cy, 1.2, DARK[0])
        t.blend(cx - 1.0, cy - 1.0, COPPER[5], 0.45)
    t.hline(1, 14, 8, STEEL[1])
    t.hline(1, 14, 9, STEEL[5])
    return t


@block("heat_exchanger_pipes")
def heat_exchanger_pipes():
    t = Tex(seed=17)
    # Vertical copper tube wall. The model samples arbitrary 2px-wide columns for the
    # tube sides, so the cylindrical shading has a 2px period: light column, dark column.
    for x in range(16):
        for y in range(16):
            v = 4.6 if x % 2 == 0 else 2.4
            v += (fbm(x, y, 17, 16, 16, 2, 8.0) - 0.5) * 0.7
            t.set(x, y, COPPER[int(round(v))])
    # welded collars, kept 1px so the vertical read survives
    for y in (3, 9, 14):
        for x in range(16):
            t.set(x, y, COPPER[6] if x % 2 == 0 else COPPER[3])
            t.set(x, (y + 1) % 16, COPPER[1])
    # tube cross-sections used by the model's up/down faces
    for ox, oy in ((2, 4), (13, 9)):
        t.rect(ox, oy, ox + 1, oy + 1, COPPER[3])
        t.set(ox, oy, COPPER[5])
        t.set(ox + 1, oy + 1, COPPER[1])
    return t


@block("reactor_temperature_sensor")
def reactor_temperature_sensor():
    STEEL = REACTOR_STEEL
    t = Tex(seed=18)
    steel_panel(t)
    t.disc(8, 8, 5.2, STEEL[5])
    t.ring(8, 8, 5.2, 4.4, STEEL[2])
    t.ring(8, 8, 5.5, 5.0, STEEL[0])
    t.disc(8, 8, 4.2, DARK[2])
    # dial arc: cool -> hot
    for a in range(200, 341, 10):
        r = math.radians(a)
        x = 8 + math.cos(r) * 3.3 - 0.5
        y = 8 + math.sin(r) * 3.3 - 0.5
        c = CYAN[5] if a < 260 else (WARN[5] if a < 305 else DANGER[5])
        t.set(round(x), round(y), c)
    t.line(8, 8, 10.5, 5.5, DANGER[6])
    t.set(8, 8, STEEL[6])
    glow(t, 10, 6, 3.0, DANGER[6], 0.22)
    return t


@block("reactor_redstone_port")
def reactor_redstone_port():
    return _reactor_redstone_port(False)


@block("reactor_redstone_port_scram")
def reactor_redstone_port_scram():
    return _reactor_redstone_port(True)


def _reactor_redstone_port(scram):
    STEEL = REACTOR_STEEL
    t = Tex(seed=24 if scram else 23)
    steel_panel(t)
    # Recessed socket with a redstone lamp; the SCRAM variant gets a hazard-striped bezel.
    t.rect(4, 4, 11, 11, DARK[1])
    bevel(t, 4, 4, 11, 11, STEEL)
    if scram:
        for i in range(4, 12):
            for j in (4, 11):
                t.set(i, j, WARN[5] if (i // 2) % 2 == 0 else DARK[0])
                t.set(j, i, WARN[5] if (i // 2) % 2 == 0 else DARK[0])
    t.rect(6, 6, 9, 9, DANGER[1])
    t.rect(7, 7, 8, 8, DANGER[5] if scram else DANGER[3])
    t.set(7, 7, DANGER[6])
    glow(t, 7.5, 7.5, 3.0, DANGER[6], 0.3 if scram else 0.15)
    # comparator-style output pins along the bottom edge
    for x in (5, 8, 10):
        t.set(x, 13, STEEL[6])
        t.set(x, 14, STEEL[1])
    return t


@block("reactor_controller_off")
def reactor_controller_off():
    return _reactor_controller(False)


@block("reactor_controller_on")
def reactor_controller_on():
    return _reactor_controller(True)


def _reactor_controller(on):
    STEEL = REACTOR_STEEL
    t = Tex(seed=19)
    steel_panel(t)
    # Analogue instrument with a brass bezel and separate activity window.
    t.rect(3, 3, 9, 9, BRASS[1])
    bevel(t, 3, 3, 9, 9, STEEL)
    t.rect(4, 4, 8, 8, CRYO[5] if on else STEEL[3])
    for x, y in ((4, 6), (5, 4), (7, 4), (8, 6)):
        t.set(x, y, DARK[2])
    t.line(6, 7, 8 if on else 4, 5 if on else 7, DANGER[3])
    t.set(6, 7, BRASS[1])
    t.rect(11, 3, 12, 9, DARK[0])
    for y in (4, 6, 8):
        t.set(11, y, RADIUM[5] if on else RADIUM[1])
    t.rect(3, 11, 6, 12, DARK[1])
    t.hline(3, 6, 11, STEEL[5])
    for x, accent in ((9, WARN), (12, DANGER)):
        t.set(x, 11, accent[5] if on else accent[2])
        t.set(x, 12, accent[1])
    return t


# ------------------------------------------------------------------ fission ports

@block("reactor_fluid_port_input")
def reactor_fluid_port_input():
    return steel_port(IN_ACCENT)


@block("reactor_fluid_port_output")
def reactor_fluid_port_output():
    return steel_port(OUT_ACCENT, output=True)


@block("reactor_fuel_port_input")
def reactor_fuel_port_input():
    return steel_port(URANIUM, fuel=True)


@block("reactor_fuel_port_output")
def reactor_fuel_port_output():
    return steel_port(LEAD, output=True, fuel=True)


# ======================================================================================
# centrifuge
# ======================================================================================

@block("centrifuge_metal")
def centrifuge_metal():
    t = Tex(seed=31)
    brushed(t, STEEL, base=4.0, amp=0.7, seed=31, axis="v")
    # polished drum: vertical cylindrical shading
    for x in range(16):
        f = 0.68 + 0.62 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    # segmented drum: welded seams top and bottom, specular band on the left third
    for y in (0, 4, 11, 15):
        for x in range(16):
            t.set(x, y, shade(t.get(x, y), 0.62 if y in (0, 11) else 1.3))
    for y in range(1, 15):
        if y not in (4, 11):
            t.blend(4, y, WHITE, 0.16)
    for y in (2, 13):
        for x in (2, 8, 13):
            t.set(x, y, STEEL[6])
            t.set(x, y + 1, STEEL[1])
    return t


@block("centrifuge_dark_metal")
def centrifuge_dark_metal():
    t = Tex(seed=32)
    brushed(t, DARK, base=3.2, amp=0.8, seed=32)
    bevel(t, 0, 0, 15, 15, DARK)
    outline(t, 0, 0, 15, 15, DARK)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        rivet(t, x, y, DARK)
    return t


@block("centrifuge_panel")
def centrifuge_panel():
    t = Tex(seed=33)
    brushed(t, DARK, base=3.4, amp=0.6, seed=33)
    bevel(t, 0, 0, 15, 15, DARK)
    outline(t, 0, 0, 15, 15, DARK)
    panel_screen(t, 2, 2, 9, 8, DARK, RADIUM, lit=True, seed=5)
    bar_readout(t, 3, 3, 9, 8, RADIUM, (2, 3, 5, 4, 6), warn=4)
    # side indicator LEDs
    for i, y in enumerate((3, 6)):
        t.rect(11, y, 12, y + 1, (RADIUM, WARN)[i][5])
        t.set(11, y, (RADIUM, WARN)[i][6])
        t.set(12, y + 1, DARK[0])
    # dial
    t.disc(11.5, 11.5, 2.4, DARK[5])
    t.ring(11.5, 11.5, 2.4, 1.8, DARK[2])
    t.line(11, 11, 12.5, 9.8, WARN[6])
    t.hline(2, 13, 10, DARK[1])
    glow(t, 6, 5, 6.0, RADIUM[6], 0.14)
    return t


@block("centrifuge_input")
def centrifuge_input():
    t = Tex(seed=34)
    brushed(t, STEEL, base=3.0, amp=0.6, seed=34)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL)
    t.rect(3, 3, 12, 12, DARK[1])
    inset(t, 3, 3, 12, 12, STEEL)
    for y in range(4, 12, 2):
        t.hline(4, 11, y, DARK[0])
        t.hline(4, 11, y + 1, CYAN[3])
    glow(t, 8, 8, 5.5, CYAN[6], 0.3)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(t, x, y, STEEL)
    return t


@block("centrifuge_output")
def centrifuge_output():
    t = Tex(seed=35)
    brushed(t, STEEL, base=3.0, amp=0.6, seed=35)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL)
    t.disc(8, 8, 5.6, STEEL[4])
    t.ring(8, 8, 5.6, 4.8, STEEL[6])
    t.ring(8, 8, 5.9, 5.4, STEEL[0])
    t.disc(8, 8, 4.2, DARK[1])
    t.disc(8, 8, 3.0, PLASMA[3])
    t.disc(8, 8, 1.8, PLASMA[5])
    glow(t, 8, 8, 6.0, PLASMA[6], 0.28)
    return t


@block("centrifuge_shaft")
def centrifuge_shaft():
    t = Tex(seed=36)
    for x in range(16):
        f = 0.7 + 0.6 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            v = 3.0 + (fbm(x, y, 36, 16, 16, 2, 6.0) - 0.5) * 1.0
            t.set(x, y, shade(BRASS[int(round(v))], f))
    for y in (1, 5, 9, 13):
        t.hline(0, 15, y, BRASS[1])
        t.hline(0, 15, y + 1, BRASS[6])
    return t


# Textures drawn with transparent pixels; validate.py checks these against their own sizes (the turbine glass, from tex_turbine).
CUTOUT_BLOCKS = {}

# Opaque textures larger than 16x16 (connected-texture sheets and the 32x launch-site textures); validate.py
# checks their size and that every pixel is opaque.
SHEET_BLOCKS = {
    "launch_base_formed": (96, 96),
}


# ======================================================================================
# waste storage
# ======================================================================================

@block("radioactive_waste_barrel_side")
def radioactive_waste_barrel_side():
    t = Tex(seed=39)
    brushed(t, WARN, base=3.6, amp=0.6, seed=39, axis="v")
    for x in range(16):
        f = 0.72 + 0.56 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    # rolling hoops top and bottom
    for y in (0, 1, 14, 15):
        for x in range(16):
            t.set(x, y, shade(DARK[3], 1.0 if y in (1, 14) else 0.8))
    # black trefoil on the drum
    trefoil(t, 8, 8, 5.6, DARK[0])
    return t


@block("radioactive_waste_barrel_top")
def radioactive_waste_barrel_top():
    t = Tex(seed=40)
    brushed(t, WARN, base=3.4, amp=0.5, seed=40)
    t.ring(8, 8, 8.0, 6.9, DARK[3])
    t.ring(8, 8, 6.9, 6.2, WARN[5])
    # bung and vent
    t.disc(11, 5, 1.6, DARK[2])
    t.set(10, 4, DARK[5])
    t.disc(5, 11, 1.1, DARK[2])
    return t


# ======================================================================================
# electrolyzer (same body as the centrifuge, copper cell and electrode panel)
# ======================================================================================

@block("electrolyzer_metal")
def electrolyzer_metal():
    t = Tex(seed=37)
    brushed(t, COPPER, base=3.8, amp=0.7, seed=37, axis="v")
    for x in range(16):
        f = 0.70 + 0.58 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    # banded electrolytic cell with insulating rings
    for y in (0, 5, 10, 15):
        for x in range(16):
            t.set(x, y, shade(DARK[2], 0.9 if y in (0, 15) else 1.1))
    for y in range(1, 15):
        if y not in (5, 10):
            t.blend(4, y, WHITE, 0.14)
    for y in (2, 7, 12):
        for x in (3, 12):
            t.set(x, y, BRASS[5])
            t.set(x, y + 1, BRASS[1])
    return t


@block("electrolyzer_panel")
def electrolyzer_panel():
    t = Tex(seed=38)
    brushed(t, DARK, base=3.4, amp=0.6, seed=38)
    bevel(t, 0, 0, 15, 15, DARK)
    outline(t, 0, 0, 15, 15, DARK)
    # sight glass with two electrodes and rising bubbles
    t.rect(2, 2, 13, 11, CYAN[1])
    inset(t, 2, 2, 13, 11, DARK)
    for y in range(3, 11):
        for x in range(3, 13):
            t.set(x, y, CYAN[2] if (x + y) % 5 else CYAN[3])
    for y in range(3, 11):
        t.set(5, y, COPPER[5])
        t.set(6, y, COPPER[2])
        t.set(10, y, LEAD[5])
        t.set(11, y, LEAD[2])
    for x, y in ((7, 9), (8, 7), (7, 5), (9, 4), (4, 8), (12, 6)):
        t.set(x, y, CYAN[6])
    glow(t, 8, 7, 6.0, CYAN[6], 0.18)
    # polarity marks
    t.rect(3, 13, 4, 13, DANGER[5])
    t.rect(11, 13, 12, 13, CYAN[5])
    t.set(4, 12, DANGER[5])
    t.set(4, 14, DANGER[5])
    return t


# ======================================================================================
# fusion tier
# ======================================================================================

BLOCKS.update(FUSION_BLOCKS)


# ======================================================================================
# ordnance
# ======================================================================================

@block("nuclear_bomb_body")
def nuclear_bomb_body():
    t = Tex(seed=60)
    olive = STEEL.tinted(WARN[2], 0.35)
    brushed(t, olive, base=3.0, amp=0.7, seed=60)
    for x in range(16):
        f = 0.75 + 0.5 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    # hazard band
    hazard(t, 0, 6, 15, 9, WARN[5], DARK[0], period=6, seed=60)
    t.hline(0, 15, 5, DARK[0])
    t.hline(0, 15, 10, DARK[0])
    for y in (2, 13):
        t.hline(0, 15, y, olive[1])
        t.hline(0, 15, y + 1, olive[5])
    for x in (2, 13):
        rivet(t, x, 3, olive)
        rivet(t, x, 11, olive)
    return t


@block("nuclear_bomb_panel")
def nuclear_bomb_panel():
    t = Tex(seed=61)
    olive = STEEL.tinted(WARN[2], 0.35)
    brushed(t, olive, base=3.0, amp=0.6, seed=61)
    bevel(t, 0, 0, 15, 15, olive)
    outline(t, 0, 0, 15, 15, olive)
    t.rect(2, 2, 13, 13, WARN[4])
    bevel(t, 2, 2, 13, 13, WARN)
    outline(t, 2, 2, 13, 13, DARK)
    trefoil(t, 8, 8, 5.4, DARK[0])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(t, x, y, olive)
    return t


@block("nuclear_bomb_top")
def nuclear_bomb_top():
    t = Tex(seed=62)
    olive = STEEL.tinted(WARN[2], 0.35)
    brushed(t, olive, base=3.0, amp=0.6, seed=62)
    bevel(t, 0, 0, 15, 15, olive)
    outline(t, 0, 0, 15, 15, olive)
    t.disc(8, 8, 6.0, olive[4])
    t.ring(8, 8, 6.0, 5.2, olive[6])
    t.ring(8, 8, 6.3, 5.9, DARK[0])
    t.disc(8, 8, 4.6, DARK[2])
    t.disc(8, 8, 2.0, DANGER[4])
    t.set(7, 7, DANGER[6])
    for a in range(0, 360, 60):
        r = math.radians(a)
        t.set(round(8 + math.cos(r) * 5.4 - 0.5), round(8 + math.sin(r) * 5.4 - 0.5),
              olive[6])
    glow(t, 8, 8, 4.5, DANGER[6], 0.25)
    return t


@block("projectile/nuclear_cannon_shell_side")
def shell_side():
    t = Tex(seed=63)
    brushed(t, BRASS, base=3.4, amp=0.6, seed=63, axis="v")
    for x in range(16):
        f = 0.7 + 0.6 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    # driving bands
    for y in (3, 11):
        t.hline(0, 15, y, BRASS[1])
        t.hline(0, 15, y + 1, BRASS[6])
    hazard(t, 0, 6, 15, 8, WARN[5], DARK[1], period=6, seed=63)
    t.hline(0, 15, 6, DARK[0])
    t.hline(0, 15, 8, DARK[0])
    return t


@block("projectile/depleted_uranium_ap_shot_side")
def du_shot_side():
    t = Tex(seed=66)
    brushed(t, LEAD, base=3.0, amp=0.6, seed=66, axis="v")
    for x in range(16):
        f = 0.7 + 0.6 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    # bright hardened penetrator cap on top, steel driving band, olive base
    for y in range(0, 4):
        for x in range(16):
            t.set(x, y, shade(STEEL[5 if y < 2 else 4], 0.7 + 0.6 * math.sin(math.pi * (x + 0.5) / 16)))
    t.hline(0, 15, 4, STEEL[1])
    for y in (10, 11):
        t.hline(0, 15, y, COPPER[3 if y == 10 else 1])
    t.hline(0, 15, 12, URANIUM[1])
    return t


@block("projectile/depleted_uranium_ap_shot_top")
def du_shot_top():
    t = Tex(seed=67)
    brushed(t, STEEL, base=3.6, amp=0.4, seed=67)
    t.disc(8, 8, 7.0, STEEL[5])
    t.ring(8, 8, 7.4, 6.9, STEEL[0])
    t.disc(8, 8, 3.0, STEEL[6])
    glow(t, 7, 7, 4.0, WHITE, 0.2)
    return t


@block("projectile/depleted_uranium_ap_shot_bottom")
def du_shot_bottom():
    t = Tex(seed=68)
    brushed(t, LEAD, base=2.4, amp=0.5, seed=68)
    t.disc(8, 8, 7.0, LEAD[2])
    t.ring(8, 8, 7.4, 6.9, LEAD[0])
    t.disc(8, 8, 3.0, URANIUM[1])
    return t


@block("projectile/nuclear_cannon_shell_top")
def shell_top():
    t = Tex(seed=64)
    brushed(t, BRASS, base=3.0, amp=0.5, seed=64)
    t.disc(8, 8, 7.0, BRASS[4])
    t.ring(8, 8, 7.0, 6.0, BRASS[6])
    t.ring(8, 8, 7.4, 6.9, BRASS[0])
    t.disc(8, 8, 4.4, DANGER[3])
    t.ring(8, 8, 4.4, 3.6, DANGER[5])
    t.disc(8, 8, 2.2, WARN[5])
    trefoil(t, 8, 8, 2.0, DARK[0])
    glow(t, 6, 6, 5.0, BRASS[6], 0.15)
    return t


@block("projectile/nuclear_cannon_shell_bottom")
def shell_bottom():
    t = Tex(seed=65)
    brushed(t, BRASS, base=2.6, amp=0.5, seed=65)
    t.disc(8, 8, 7.0, BRASS[3])
    t.ring(8, 8, 7.0, 6.0, BRASS[5])
    t.ring(8, 8, 7.4, 6.9, BRASS[0])
    t.disc(8, 8, 4.0, DARK[2])
    for a in range(0, 360, 45):
        r = math.radians(a)
        t.line(8 + math.cos(r) * 1.4, 8 + math.sin(r) * 1.4,
               8 + math.cos(r) * 3.6, 8 + math.sin(r) * 3.6, DARK[4])
    t.disc(8, 8, 1.2, PLASMA[4])
    return t


# ======================================================================================
# launch pad
# ======================================================================================

@block("launch_pad_base")
def launch_pad_base():
    t = Tex(seed=70)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 70, 16, 16, 3, 5.0)
            t.set(x, y, CONCRETE[int(round(1.6 + n * 3.6))])
    hazard(t, 0, 0, 15, 1, WARN[5], DARK[1], period=6, seed=70)
    hazard(t, 0, 14, 15, 15, WARN[5], DARK[1], period=6, seed=71)
    t.hline(0, 15, 2, DARK[0])
    t.hline(0, 15, 13, DARK[0])
    t.hline(0, 15, 3, CONCRETE[5])
    speckle(t, 0, 3, 15, 12, CONCRETE[1], chance=0.06, seed=70)
    speckle(t, 0, 3, 15, 12, CONCRETE[6], chance=0.04, seed=71)
    return t


@block("launch_pad_panel")
def launch_pad_panel():
    t = Tex(seed=71)
    brushed(t, STEEL, base=3.0, amp=0.6, seed=71)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL)
    panel_screen(t, 2, 2, 13, 8, STEEL, WARN, lit=True, seed=9)
    # countdown digits
    for x in (3, 6, 9, 11):
        t.rect(x, 4, x + 1, 7, DARK[0])
        t.rect(x, 4, x + 1, 4, WARN[6])
        t.rect(x, 7, x + 1, 7, WARN[6])
    for i, x in enumerate((3, 7, 11)):
        c = (RADIUM, WARN, DANGER)[i]
        t.rect(x, 11, x + 1, 12, c[5])
        t.set(x, 11, c[6])
    t.hline(1, 14, 10, STEEL[1])
    return t


@block("launch_pad_rail")
def launch_pad_rail():
    t = Tex(seed=72)
    brushed(t, STEEL, base=2.4, amp=0.6, seed=72, axis="v")
    for x in (2, 12):
        t.rect(x, 0, x + 2, 15, STEEL[4])
        t.vline(x, 0, 15, STEEL[6])
        t.vline(x + 2, 0, 15, STEEL[1])
    for y in range(1, 16, 4):
        t.hline(0, 15, y, STEEL[1])
        t.hline(0, 15, y + 1, STEEL[5])
    for x in (2, 12):
        t.rect(x, 0, x + 2, 15, None)
    # redraw rails on top of sleepers
    for x in (2, 12):
        for y in range(16):
            t.set(x, y, STEEL[6])
            t.set(x + 1, y, STEEL[4])
            t.set(x + 2, y, STEEL[1])
    return t


# ======================================================================================
# cooling tower
# ======================================================================================

def _tower_concrete(t, seed):
    # Poured concrete in horizontal lifts, weathered with vertical rain streaks.
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 3, 5.0)
            t.set(x, y, CONCRETE[int(round(2.0 + n * 3.0))])
    for y in (0, 8):
        t.hline(0, 15, y, CONCRETE[1])
        t.hline(0, 15, y + 1, CONCRETE[5])
    for x in (3, 9, 13):
        for y in range(2, 16):
            if fbm(x, y, seed + 5, 16, 16, 2, 3.0) > 0.45:
                t.set(x, y, CONCRETE[2])
    speckle(t, 0, 0, 15, 15, CONCRETE[1], chance=0.05, seed=seed)
    return t


@block("cooling_tower_casing")
def cooling_tower_casing():
    return _tower_concrete(Tex(seed=90), 90)


@block("cooling_tower_controller")
def cooling_tower_controller():
    t = _tower_concrete(Tex(seed=91), 91)
    # Steel instrument plate with a load dial and a water-level column.
    t.rect(2, 2, 13, 13, STEEL[3])
    bevel(t, 2, 2, 13, 13, STEEL)
    outline(t, 2, 2, 13, 13, STEEL[0])
    t.disc(6.5, 7.5, 3.4, DARK[2])
    t.ring(6.5, 7.5, 3.4, 2.8, STEEL[5])
    for a in range(200, 341, 20):
        r = math.radians(a)
        t.set(round(6.5 + math.cos(r) * 2.2 - 0.5), round(7.5 + math.sin(r) * 2.2 - 0.5), CYAN[5])
    t.line(6, 7, 8, 5, WARN[6])
    t.rect(11, 4, 12, 11, DARK[0])
    t.rect(11, 8, 12, 11, CYAN[4])
    t.set(11, 8, CYAN[6])
    return t


@block("cooling_tower_fluid_port_input")
def cooling_tower_fluid_port_input():
    return port_face(Tex(seed=92), CONCRETE, IN_ACCENT, seed=92)


@block("cooling_tower_fluid_port_output")
def cooling_tower_fluid_port_output():
    return port_face(Tex(seed=93), CONCRETE, OUT_ACCENT, seed=93, out=True)


@block("cooling_tower_fan")
def cooling_tower_fan():
    t = _tower_concrete(Tex(seed=94), 94)
    # Louvred intake around a steel hub.
    t.rect(1, 1, 14, 14, DARK[1])
    bevel(t, 1, 1, 14, 14, STEEL)
    for y in range(3, 13, 2):
        t.hline(2, 13, y, STEEL[4])
        t.hline(2, 13, y + 1, DARK[0])
    t.disc(7.5, 7.5, 2.4, STEEL[3])
    t.ring(7.5, 7.5, 2.4, 1.6, STEEL[6])
    t.disc(7.5, 7.5, 0.9, DARK[0])
    return t



@block("depleted_uranium_block")
def depleted_uranium_block():
    # Dense grey metal blocks stacked in a 2x2 bond, faint green oxide in the joints.
    t = Tex(seed=95)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 95, 16, 16, 3, 4.0)
            t.set(x, y, LEAD[int(round(2.2 + n * 2.4))])
    for i in (0, 8):
        t.hline(0, 15, i, LEAD[0])
        t.vline(i, 0, 15, LEAD[0])
        t.hline(0, 15, i + 1, LEAD[5])
        t.vline(i + 1, 0, 15, LEAD[5])
    speckle(t, 0, 0, 15, 15, URANIUM[1], chance=0.05, seed=95)
    for cx, cy in ((4, 4), (12, 4), (4, 12), (12, 12)):
        t.set(cx, cy, LEAD[6])
    return t


# ======================================================================================
# RTG
# ======================================================================================

@block("rtg_side")
def rtg_side():
    # Radiator fins over a dark core, with a yellow trefoil plate.
    t = Tex(seed=96)
    for y in range(16):
        for x in range(16):
            t.set(x, y, STEEL[2] if x % 3 == 0 else STEEL[4] if x % 3 == 1 else STEEL[5])
    t.hline(0, 15, 0, STEEL[1])
    t.hline(0, 15, 15, STEEL[0])
    t.rect(5, 5, 10, 10, WARN[5])
    outline(t, 5, 5, 10, 10, DARK[0])
    trefoil(t, 7.5, 7.5, 2.2, DARK[0])
    return t


@block("rtg_front")
def rtg_front():
    t = Tex(seed=97)
    brushed(t, STEEL, base=3.2, amp=0.5, seed=97)
    bevel(t, 0, 0, 15, 15, STEEL)
    t.disc(7.5, 7.5, 3.6, DARK[0])
    t.ring(7.5, 7.5, 4.4, 3.6, BRASS[4])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, STEEL)
    return t


@block("rtg_back")
def rtg_back():
    t = Tex(seed=98)
    brushed(t, STEEL, base=2.8, amp=0.5, seed=98)
    bevel(t, 0, 0, 15, 15, STEEL)
    hazard(t, 2, 6, 13, 9, WARN[5], DARK[1], period=4, seed=98)
    return t


@block("radiation_scrubber_side")
def radiation_scrubber_side():
    # Steel housing around a glowing green filter cartridge behind a grille.
    t = Tex(seed=96)
    brushed(t, STEEL, base=3.0, amp=0.8, seed=96)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    t.rect(3, 3, 12, 12, DARK[0])
    t.rect(4, 4, 11, 11, RADIUM[2])
    for x in range(4, 12, 2):
        t.vline(x, 4, 11, RADIUM[4])
    for y in (4, 7, 10):
        t.hline(4, 11, y, DARK[1])
    t.set(7, 6, RADIUM[6])
    t.set(8, 9, RADIUM[6])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        rivet(t, x, y, STEEL)
    return t


@block("radiation_scrubber_top")
def radiation_scrubber_top():
    # Intake grate with the trefoil warning.
    t = Tex(seed=97)
    brushed(t, STEEL, base=3.2, amp=0.7, seed=97)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    t.disc(7.5, 7.5, 6.2, DARK[1])
    t.disc(7.5, 7.5, 5.4, WARN[4])
    trefoil(t, 7.5, 7.5, 4.8, DARK[0])
    return t


@block("radiation_scrubber_bottom")
def radiation_scrubber_bottom():
    # Plain plate around the drive shaft socket.
    t = Tex(seed=98)
    brushed(t, STEEL, base=2.8, amp=0.7, seed=98)
    bevel(t, 0, 0, 15, 15, STEEL)
    outline(t, 0, 0, 15, 15, STEEL[0])
    t.disc(7.5, 7.5, 3.2, DARK[0])
    t.ring(7.5, 7.5, 3.2, 2.4, STEEL[5])
    t.rect(6, 6, 9, 9, DARK[2])
    return t


# ======================================================================================
# fission tier - moderator and reflector
# ======================================================================================

GRAPHITE = LEAD.tinted(DARK[0], 0.35)



@block("graphite_moderator")
def graphite_moderator():
    # Stacked graphite bricks with a bored channel through the middle, RBMK style.
    t = Tex(seed=110)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 110, 16, 16, 3, 5.0)
            t.set(x, y, GRAPHITE[int(round(1.8 + n * 2.0))])
    for y in (0, 5, 10, 15):
        t.hline(0, 15, y, GRAPHITE[0])
    for row, y0 in enumerate((0, 5, 10)):
        for x in ((5, 11) if row % 2 == 0 else (2, 8, 14)):
            t.vline(x, y0, min(15, y0 + 5), GRAPHITE[0])
    for y in (1, 6, 11):
        t.hline(0, 15, y, GRAPHITE[4])
    t.disc(7.5, 7.5, 2.6, GRAPHITE[0])
    t.ring(7.5, 7.5, 3.3, 2.6, GRAPHITE[5])
    speckle(t, 0, 0, 15, 15, GRAPHITE[6], chance=0.04, seed=110)
    return t


@block("beryllium_reflector")
def beryllium_reflector():
    # Pale beryllium plates in a steel frame: bright face that bounces neutrons back.
    from pixelkit import Ramp
    ber = Ramp("#354039", "#546257", "#758777", "#98aa92", "#b6c9aa", "#d4e0c5", "#eff2dd")
    t = Tex(seed=111)
    brushed(t, ber, base=4.0, amp=0.6, seed=111)
    outline(t, 0, 0, 15, 15, STEEL[0])
    bevel(t, 1, 1, 14, 14, STEEL)
    for x0, y0 in ((2, 2), (8, 2), (2, 8), (8, 8)):
        t.rect(x0, y0, x0 + 5, y0 + 5, ber[4])
        bevel(t, x0, y0, x0 + 5, y0 + 5, ber)
        t.set(x0 + 1, y0 + 1, ber[6])
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        t.set(x, y, STEEL[1])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        rivet(t, x, y, STEEL)
    return t


# ======================================================================================
# control room
# ======================================================================================

@block("control_monitor_side")
def control_monitor_side():
    # Dark brushed housing with a vent strip, matching the control desk.
    t = Tex(seed=120)
    brushed(t, DARK, base=3.2, amp=0.5, seed=120)
    bevel(t, 0, 0, 15, 15, DARK)
    outline(t, 0, 0, 15, 15, DARK[0])
    for y in (5, 7, 9):
        t.hline(4, 11, y, DARK[0])
        t.hline(4, 11, y + 1, DARK[4])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, DARK)
    return t


@block("control_monitor_front")
def control_monitor_front():
    # A thin bezel round dark glass; the renderer draws the live screen over it.
    t = Tex(seed=121)
    t.rect(0, 0, 15, 15, DARK[2])
    outline(t, 0, 0, 15, 15, DARK[0])
    t.rect(1, 1, 14, 14, BLACK)
    for y in range(1, 15):
        for x in range(1, 15):
            t.set(x, y, mix(DARK[0], CYAN[0], 0.18 if (y % 2) else 0.10))
    for i in range(10):
        t.blend(2 + i, 13 - i, DARK[4], 0.18)
    return t


def _alarm(seed, lit, top):
    t = Tex(seed=seed)
    brushed(t, DARK, base=3.0, amp=0.5, seed=seed)
    outline(t, 0, 0, 15, 15, DARK[0])
    lamp = DANGER if lit else DARK
    if top:
        t.disc(7.5, 7.5, 5.6, DARK[0])
        t.disc(7.5, 7.5, 4.8, lamp[4] if lit else lamp[2])
        t.disc(6.5, 6.5, 1.6, lamp[6] if lit else lamp[4])
    else:
        t.rect(3, 2, 12, 13, DARK[0])
        t.rect(4, 3, 11, 12, lamp[4] if lit else lamp[2])
        for y in range(4, 12, 2):
            t.hline(4, 11, y, DARK[1])
        t.set(5, 4, lamp[6] if lit else lamp[4])
        hazard(t, 1, 14, 14, 15, WARN[4], DARK[0], period=4, seed=seed)
    if lit:
        glow(t, 7.5, 7.5, 8.0, DANGER[6], 0.25)
    return t


@block("nuclear_alarm_side")
def nuclear_alarm_side():
    return _alarm(123, False, False)


@block("nuclear_alarm_side_lit")
def nuclear_alarm_side_lit():
    return _alarm(123, True, False)


@block("nuclear_alarm_top")
def nuclear_alarm_top():
    return _alarm(124, False, True)


@block("nuclear_alarm_top_lit")
def nuclear_alarm_top_lit():
    return _alarm(124, True, True)


# ======================================================================================
# dirty bomb
# ======================================================================================

def _dirty_drum():
    # A rusty, olive-green waste drum on the bomb's frame, leaking a sickly glow.
    return STEEL.tinted(RADIUM[1], 0.45)


@block("dirty_bomb_body")
def dirty_bomb_body():
    t = Tex(seed=130)
    drum = _dirty_drum()
    brushed(t, drum, base=3.0, amp=0.8, seed=130)
    for x in range(16):
        f = 0.75 + 0.5 * math.sin(math.pi * (x + 0.5) / 16)
        for y in range(16):
            t.set(x, y, shade(t.get(x, y), f))
    for y in (3, 12):
        t.hline(0, 15, y, drum[0])
        t.hline(0, 15, y + 1, drum[5])
    # rust streaks and leaks
    for y in range(16):
        for x in range(16):
            if fbm(x, y, 131, 16, 16, 3, 4.0) > 0.70:
                t.blend(x, y, COPPER[1], 0.6)
    t.rect(5, 6, 10, 10, WARN[4])
    outline(t, 5, 6, 10, 10, DARK[0])
    trefoil(t, 7.5, 8, 2.0, DARK[0])
    speckle(t, 0, 0, 15, 15, RADIUM[6], chance=0.03, seed=131)
    return t


@block("dirty_bomb_top")
def dirty_bomb_top():
    t = Tex(seed=132)
    drum = _dirty_drum()
    brushed(t, drum, base=3.0, amp=0.6, seed=132)
    bevel(t, 0, 0, 15, 15, drum)
    outline(t, 0, 0, 15, 15, drum[0])
    t.disc(8, 8, 5.6, drum[1])
    t.ring(8, 8, 5.6, 4.8, drum[5])
    t.disc(10.5, 5.5, 1.2, DARK[0])
    t.disc(8, 8, 2.2, RADIUM[4])
    glow(t, 8, 8, 5.0, RADIUM[6], 0.3)
    return t


# ======================================================================================
# control desk
# ======================================================================================

from palette import CONSOLE, GLASS_OFF, GLASS_ON, MIMIC_BLUE  # noqa: E402


@block("control_desk_surface")
def control_desk_surface():
    # The slanted desk top as the operator reads it: row 0 is the back edge (under the
    # rail), row 14 the front edge, row 15 is never mapped. Module slots cover columns
    # 1-6 / 9-14 and rows 1-6 / 8-13. Mimic lines run the full width so they continue
    # into the next desk and mitre round corner pieces.
    t = Tex(seed=140)
    brushed(t, CONSOLE, base=3.4, amp=0.35, seed=140, axis="h")
    t.hline(0, 15, 0, CONSOLE[1])
    t.hline(0, 15, 14, CONSOLE[2])
    t.hline(0, 15, 15, CONSOLE[1])
    for u0, v0 in ((1, 1), (9, 1), (1, 8), (9, 8)):
        u1, v1 = u0 + 5, v0 + 5
        for x, y, dx, dy in ((u0, v0, 1, 1), (u1, v0, -1, 1), (u0, v1, 1, -1), (u1, v1, -1, -1)):
            t.set(x, y, CONSOLE[1])
            t.set(x + dx, y, CONSOLE[2])
            t.set(x, y + dy, CONSOLE[2])
    lines = ((3, MIMIC_BLUE), (7, DANGER), (11, RADIUM))
    for y, ramp in lines:
        t.hline(0, 15, y, ramp[4])
        for x in range(16):
            t.set(x, y + 1, shade(t.get(x, y + 1), 0.82))
    t.vline(8, 4, 10, WARN[4])
    t.set(7, 9, WARN[5])
    t.set(8, 9, DARK[0])
    t.set(7, 10, DARK[0])
    t.set(8, 10, WARN[5])
    for y, ramp in lines:
        t.set(8, y, ramp[6])
    return t


@block("control_desk_body")
def control_desk_body():
    # Rows 0-6 dress the back of the rail; rows 7-14 the cabinet (front and rear doors).
    # Door gaps sit on columns 0/15 so a merged console reads as a run of cabinets.
    t = Tex(seed=141)
    brushed(t, CONSOLE, base=3.0, amp=0.45, seed=141, axis="v")
    t.hline(0, 15, 0, CONSOLE[4])
    for y in (2, 4):
        t.hline(2, 13, y, CONSOLE[0])
        t.hline(2, 13, y + 1, CONSOLE[4])
    t.hline(0, 15, 6, CONSOLE[1])
    t.hline(0, 15, 7, CONSOLE[5])
    for x0, x1 in ((1, 6), (9, 14)):
        bevel(t, x0, 8, x1, 13, CONSOLE, hi=CONSOLE[5], lo=CONSOLE[1])
    for x in (0, 15):
        t.vline(x, 8, 13, CONSOLE[0])
    t.vline(7, 8, 13, CONSOLE[2])
    t.vline(8, 8, 13, CONSOLE[2])
    for y in (10, 11):
        t.set(7, y, STEEL[6])
        t.set(8, y, STEEL[4])
    t.hline(0, 15, 14, DARK[1])
    t.hline(0, 15, 15, DARK[0])
    return t


@block("control_desk_side")
def control_desk_side():
    # End cheek: plain enamel; the bottom row lines up with the dark plinth.
    t = Tex(seed=142)
    brushed(t, CONSOLE, base=2.7, amp=0.4, seed=142, axis="h")
    t.hline(0, 15, 15, DARK[1])
    for x, y in ((2, 9), (12, 9)):
        bolt(t, x, y, CONSOLE)
    return t


@block("control_desk_trim")
def control_desk_trim():
    # Near-black trim for the armrest, rails, plinth and cheek edges; uniform so any UV works.
    t = Tex(seed=143)
    brushed(t, DARK, base=2.0, amp=0.5, seed=143, axis="h")
    return t


# ------------------------------------------------------------------ desk modules
# Partial-model atlases. Unused texels stay opaque. Up faces are drawn as the operator reads
# them: row 0 = away from the operator.

def _atlas(seed):
    t = Tex(seed=seed)
    t.rect(0, 0, 15, 15, DARK[1])
    return t


@block("desk_module_base")
def desk_module_base():
    # 6x6 mounting plate at texels 5..10; its dark bottom bevel row (10) dresses the plate sides.
    t = Tex(seed=150)
    brushed(t, DARK, base=1.6, amp=0.4, seed=150)
    t.rect(5, 5, 10, 10, DARK[2])
    bevel(t, 5, 5, 10, 10, DARK, hi=DARK[4], lo=DARK[0])
    for x, y in ((6, 6), (9, 6), (6, 9), (9, 9)):
        t.set(x, y, STEEL[5])
    return t


@block("desk_push_button")
def desk_push_button():
    # [0,0,5,5] guard collar top | [0,5,5,6] collar side | [6,0,8,1] stem
    # [8,0,12,4] mushroom head top | [8,4,12,5] head side | [8,5,12,6] head underside
    t = _atlas(151)
    t.rect(0, 0, 4, 4, WARN[4])
    bevel(t, 0, 0, 4, 4, WARN, hi=WARN[6], lo=WARN[1])
    t.rect(1, 1, 3, 3, DARK[0])
    for x in range(5):
        t.set(x, 5, WARN[4] if x % 2 == 0 else DARK[0])
    t.hline(6, 7, 0, STEEL[2])
    t.rect(8, 0, 11, 3, DANGER[4])
    for x, y in ((8, 0), (11, 0), (8, 3), (11, 3)):
        t.set(x, y, DANGER[2])
    t.set(9, 1, DANGER[6])
    t.set(10, 1, DANGER[5])
    t.set(9, 2, DANGER[5])
    t.hline(9, 10, 3, DANGER[3])
    t.vline(11, 1, 2, DANGER[3])
    t.hline(8, 11, 4, DANGER[3])
    t.set(8, 4, DANGER[4])
    t.hline(8, 11, 5, DANGER[1])
    return t


@block("desk_lever")
def desk_lever():
    # [0,0,4,5] gate top with slot | [0,5,5,6] gate side | [6,0,7,4] stem | [6,4,7,5] stem top
    # [8,0,10,2] knob side | [8,2,10,4] knob top
    t = _atlas(152)
    t.rect(0, 0, 3, 4, DARK[2])
    bevel(t, 0, 0, 3, 4, DARK, hi=DARK[4], lo=DARK[0])
    t.vline(1, 1, 3, DARK[0])
    t.vline(2, 1, 3, DARK[0])
    t.set(3, 0, RADIUM[5])
    t.set(3, 4, DANGER[4])
    t.hline(0, 4, 5, DARK[3])
    for y, c in enumerate((STEEL[6], STEEL[5], STEEL[4], STEEL[3], STEEL[6])):
        t.set(6, y, c)
    t.hline(8, 9, 0, DARK[3])
    t.hline(8, 9, 1, DARK[1])
    t.rect(8, 2, 9, 3, DARK[2])
    t.set(8, 2, DARK[5])
    return t


@block("desk_dial")
def desk_dial():
    # [0,0,6,6] scale plate | [0,6,6,7] plate edge | [8,0,11,2] knurled knob side
    # [8,4,11,7] knob top with pointer | [12,0,16,1] skirt side | [12,1,16,5] skirt top | [14,6] tab
    t = _atlas(153)
    t.rect(0, 0, 5, 5, DARK[0])
    outline(t, 0, 0, 5, 5, DARK[2])
    for x, y in ((0, 4), (0, 1), (2, 0), (3, 0), (5, 1)):
        t.set(x, y, LEAD[6])
    t.set(5, 4, DANGER[5])
    t.hline(0, 5, 6, DARK[2])
    for x in range(8, 11):
        t.vline(x, 0, 1, DARK[2] if x % 2 == 0 else DARK[0])
    t.rect(8, 4, 10, 6, DARK[2])
    t.set(8, 4, DARK[4])
    t.vline(9, 4, 5, LEAD[6])
    t.hline(12, 15, 0, DARK[2])
    t.rect(12, 1, 15, 4, DARK[3])
    t.set(14, 6, LEAD[6])
    return t


@block("desk_lamp")
def desk_lamp():
    # [0,0,4,4] chrome bezel top | [0,4,4,5] bezel side
    t = _atlas(154)
    t.rect(0, 0, 3, 3, STEEL[5])
    bevel(t, 0, 0, 3, 3, STEEL, hi=STEEL[6], lo=STEEL[2])
    t.rect(1, 1, 2, 2, DARK[0])
    t.hline(0, 3, 4, STEEL[4])
    return t


def _lens(seed, glass):
    # Whole sheet is glass so every lens UV lands on it. Neutral grey: tinted at render time.
    t = Tex(seed=seed)
    t.rect(0, 0, 15, 15, glass[3])
    t.set(0, 0, glass[6])
    t.set(1, 0, glass[5])
    t.set(0, 1, glass[5])
    t.set(2, 2, glass[1])
    t.hline(0, 15, 3, glass[2])
    return t


@block("desk_lamp_lens_off")
def desk_lamp_lens_off():
    return _lens(157, GLASS_OFF)


@block("desk_lamp_lens_on")
def desk_lamp_lens_on():
    return _lens(158, GLASS_ON)


@block("desk_gauge")
def desk_gauge():
    # [0,0,10,8] dial face at 2 texels/px, pivot at texel (5,6) | [0,8,4,12] housing side
    # [4,8,9,12] housing back | [10,0,16,5] hood | [10,6] hub | [12,7..9] needle
    t = _atlas(155)
    t.rect(0, 0, 9, 7, CRYO[5])
    outline(t, 0, 0, 9, 7, DARK[0])
    for x, y in ((1, 4), (2, 2), (4, 1), (5, 1)):
        t.set(x, y, DARK[0])
    for x, y in ((7, 2), (8, 3), (8, 4)):
        t.set(x, y, DANGER[3])
    t.hline(1, 8, 6, CRYO[4])
    t.rect(0, 8, 8, 11, DARK[2])
    t.hline(0, 8, 8, DARK[4])
    t.hline(0, 8, 11, DARK[0])
    t.rect(10, 0, 15, 4, DARK[3])
    t.hline(10, 15, 0, DARK[5])
    t.hline(10, 15, 5, DARK[1])
    t.set(10, 6, DARK[0])
    t.vline(12, 7, 9, DANGER[4])
    t.set(12, 7, DANGER[5])
    return t


@block("desk_toggle")
def desk_toggle():
    # [0,0,5,5] red plate with ON/OFF marks | [0,5,5,6] plate side | [6,0,8,2] nut top
    # [6,2,8,3] nut side | [9,0..2] bat | [10..11,0..1] bat tip | [12,0..2] guard posts
    t = _atlas(156)
    t.rect(0, 0, 4, 4, DANGER[3])
    bevel(t, 0, 0, 4, 4, DANGER, hi=DANGER[5], lo=DANGER[1])
    t.set(2, 0, LEAD[6])
    t.set(2, 4, DARK[0])
    t.hline(0, 4, 5, DANGER[2])
    t.rect(6, 0, 7, 1, STEEL[5])
    t.set(7, 1, STEEL[1])
    t.hline(6, 7, 2, STEEL[3])
    for y, c in enumerate((STEEL[6], STEEL[5], STEEL[4])):
        t.set(9, y, c)
    t.rect(10, 0, 11, 1, STEEL[6])
    t.vline(12, 0, 2, STEEL[4])
    return t


# Each area keeps its textures in its own module; they register into the tables above.
import tex_pwr  # noqa: E402,F401
import tex_spentfuel  # noqa: E402,F401
import tex_waste  # noqa: E402,F401
import tex_wastetransport  # noqa: E402,F401
import tex_controlroom2  # noqa: E402,F401
import tex_civildefense  # noqa: E402,F401
import tex_fusion  # noqa: E402,F401
import tex_cryo  # noqa: E402,F401
import tex_heating  # noqa: E402,F401
import tex_reactorvisuals  # noqa: E402,F401
import tex_fuelcycle  # noqa: E402,F401
import tex_accelerator  # noqa: E402,F401
import tex_explosive  # noqa: E402,F401
import tex_antimatter  # noqa: E402,F401
import tex_energy  # noqa: E402,F401
import tex_isotopes  # noqa: E402,F401
import tex_maintenance  # noqa: E402,F401
import tex_propulsion  # noqa: E402,F401
import tex_missile  # noqa: E402,F401
import tex_rocketfx  # noqa: E402,F401
import tex_launch  # noqa: E402,F401
import tex_defense  # noqa: E402,F401
import tex_orbit  # noqa: E402,F401
import tex_silo_controller  # noqa: E402,F401
import tex_launch_base  # noqa: E402,F401
import tex_turbine  # noqa: E402,F401

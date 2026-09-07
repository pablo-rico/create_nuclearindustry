"""Block texture definitions."""

import math
from fusion_materials import FUSION_BLOCKS
from reactor_materials import steel_panel, steel_port, mineral, STEEL as REACTOR_STEEL

from common import (bar_readout, bezel, bolt, casing, cracks, flange, ore,
                    panel_screen, port_face, trefoil)
from palette import (BLACK, BRASS, CONCRETE, COPPER, CRYO, CYAN, DANGER, DARK, IN_ACCENT,
                     LEAD, OUT_ACCENT, PLASMA, RADIUM, STEEL, STONE, TRITIUM, URANIUM,
                     WARN, WHITE)
from pixelkit import (Tex, bevel, brushed, fbm, glow, hazard, inset, item_outline, mix,
                      outline, rivet, scanlines, shade, speckle, vents)

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
# turbine
# ======================================================================================

@block("turbine_casing")
def turbine_casing():
    t = Tex(seed=24)
    casing(t, STEEL, seed=24, seam=False)
    vents(t, 3, 4, 12, 11, STEEL, step=2)
    t.frame(2, 3, 13, 12, STEEL[1])
    t.hline(3, 12, 3, STEEL[5])
    return t


@block("turbine_blade")
def turbine_blade():
    t = Tex(seed=25)
    brushed(t, STEEL, base=2.6, amp=0.7, seed=25)
    for k in range(-16, 16, 4):
        for y in range(16):
            x = k + y
            if 0 <= x < 16:
                t.set(x, y, STEEL[6])
                if x + 1 < 16:
                    t.set(x + 1, y, STEEL[4])
                if x + 2 < 16:
                    t.set(x + 2, y, STEEL[2])
                if x + 3 < 16:
                    t.set(x + 3, y, STEEL[0])
    t.hline(0, 15, 0, STEEL[1])
    t.hline(0, 15, 15, STEEL[1])
    return t


@block("turbine_rotor_core")
def turbine_rotor_core():
    t = Tex(seed=26)
    brushed(t, BRASS, base=3.0, amp=0.8, seed=26, axis="v")
    t.disc(8, 8, 6.0, BRASS[4])
    t.ring(8, 8, 6.0, 5.2, BRASS[6])
    t.ring(8, 8, 6.3, 5.9, BRASS[0])
    t.disc(8, 8, 3.4, BRASS[2])
    t.disc(8, 8, 2.0, DARK[1])
    for a in range(0, 360, 45):
        r = math.radians(a)
        t.line(8 + math.cos(r) * 2.4, 8 + math.sin(r) * 2.4,
               8 + math.cos(r) * 5.2, 8 + math.sin(r) * 5.2, BRASS[1])
    glow(t, 6, 6, 5.0, BRASS[6], 0.18)
    return t


@block("turbine_output_front")
def turbine_output_front():
    t = Tex(seed=27)
    casing(t, STEEL, seed=27, seam=False)
    t.disc(8, 8, 6.2, STEEL[3])
    t.ring(8, 8, 6.2, 5.4, STEEL[5])
    t.ring(8, 8, 6.5, 6.1, STEEL[0])
    t.disc(8, 8, 5.0, DARK[1])
    for a in range(0, 360, 30):
        r = math.radians(a)
        t.line(8 + math.cos(r) * 1.6, 8 + math.sin(r) * 1.6,
               8 + math.cos(r) * 4.8, 8 + math.sin(r) * 4.8, STEEL[4])
    t.disc(8, 8, 1.8, STEEL[2])
    glow(t, 8, 8, 6.0, CRYO[6], 0.10)
    return t


@block("turbine_output_side")
def turbine_output_side():
    t = Tex(seed=28)
    casing(t, STEEL, seed=28, seam=False)
    t.rect(0, 5, 15, 10, STEEL[4])
    bevel(t, 0, 5, 15, 10, STEEL)
    t.hline(0, 15, 5, STEEL[6])
    t.hline(0, 15, 10, STEEL[1])
    for x in (3, 12):
        t.vline(x, 5, 10, STEEL[1])
        t.vline(x + 1, 5, 10, STEEL[5])
    return t


@block("turbine_fluid_port_input")
def turbine_fluid_port_input():
    return port_face(Tex(seed=29), STEEL, IN_ACCENT, seed=29, out=False)


@block("turbine_fluid_port_output")
def turbine_fluid_port_output():
    return port_face(Tex(seed=30), STEEL, OUT_ACCENT, seed=30, out=True)


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

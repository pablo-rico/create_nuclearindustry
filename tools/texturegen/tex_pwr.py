"""Textures for the pressurized water loop, steam generator, containment and accidents.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.
"""

import math

from blocks import block  # noqa: F401
from common import bolt, casing, cracks, port_face
from gui import gui  # noqa: F401
from items import item  # noqa: F401
from palette import BRASS, CONCRETE, COPPER, CYAN, DANGER, DARK, MIMIC_BLUE, PLASMA, RADIUM, STEEL, WARN, WHITE
from pixelkit import Tex, bevel, brushed, fbm, glow, mix, outline, plate, rivet, vents
from reactor_materials import STEEL as REACTOR_STEEL


# ======================================================================================
# pressurizer: a tall steel vessel standing on the reactor lid
# ======================================================================================

def _vessel_shell(t, seed):
    """Rolled plate of a vertical cylinder: lit down the middle, dark at the edges,
    with a weld seam at each end so stacked blocks read as one welded vessel."""
    S = REACTOR_STEEL
    for y in range(16):
        for x in range(16):
            curve = math.cos((x + 0.5 - 8.0) / 8.0 * 1.25)
            n = fbm(x, y, seed, 16, 16, 2, 6.0) - 0.5
            v = 1.6 + curve * 3.6 + n * 0.8
            t.set(x, y, S[max(0, min(6, int(round(v))))])
    for y in (0, 15):
        for x in range(16):
            t.set(x, y, S[1] if y == 15 else S[5])
    for x in (2, 13):
        rivet(t, x, 1, S)
        rivet(t, x, 13, S)
    return t


@block("pressurizer_side")
def pressurizer_side():
    t = _vessel_shell(Tex(seed=300), 300)
    # level gauge glass: a short water column behind a steel frame
    t.rect(10, 3, 12, 12, DARK[0])
    t.rect(11, 7, 11, 11, CYAN[4])
    t.set(11, 7, CYAN[6])
    t.rect(11, 4, 11, 6, DARK[2])
    t.vline(10, 3, 12, REACTOR_STEEL[5])
    t.vline(12, 3, 12, REACTOR_STEEL[1])
    return t


@block("pressurizer_top")
def pressurizer_top():
    S = REACTOR_STEEL
    t = Tex(seed=301)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 301, 16, 16, 2, 5.0)
            t.set(x, y, S[3 if n < 0.55 else 4])
    # domed head with a ring of flange bolts and a central spray nozzle
    t.ring(8, 8, 7.6, 6.6, S[1])
    t.ring(8, 8, 6.6, 5.8, S[5])
    for a in range(0, 360, 45):
        r = math.radians(a)
        bx = round(8 + math.cos(r) * 6.2 - 0.5)
        by = round(8 + math.sin(r) * 6.2 - 0.5)
        t.set(bx, by, S[6])
    t.disc(8, 8, 2.6, S[2])
    t.ring(8, 8, 2.6, 1.8, S[5])
    t.disc(8, 8, 1.2, DARK[0])
    outline(t, 0, 0, 15, 15, S[1])
    return t


def _heater_port(manual):
    S = REACTOR_STEEL
    t = _vessel_shell(Tex(seed=302 if manual else 303), 302)
    # heater terminal box with glowing elements; the lamp shows automatic (cyan) or manual (red)
    t.rect(3, 4, 12, 11, DARK[1])
    bevel(t, 3, 4, 12, 11, S, hi=S[5], lo=S[0])
    for x in (5, 7, 9):
        t.vline(x, 6, 9, COPPER[2])
        t.set(x, 6, PLASMA[5])
        t.set(x, 7, PLASMA[4])
        t.set(x, 8, PLASMA[3])
        t.set(x, 9, COPPER[1])
    lamp = DANGER if manual else CYAN
    t.rect(11, 5, 11, 6, lamp[5])
    t.set(11, 5, lamp[6])
    glow(t, 7, 7.5, 3.0, PLASMA[6], 0.25)
    for x, y in ((4, 10), (10, 10)):
        t.set(x, y, S[6])
    return t


@block("pressurizer_heater_port")
def pressurizer_heater_port():
    return _heater_port(False)


@block("pressurizer_heater_port_manual")
def pressurizer_heater_port_manual():
    return _heater_port(True)


def _relief_valve(open_):
    S = REACTOR_STEEL
    t = _vessel_shell(Tex(seed=304), 304)
    # spring-loaded valve body: bonnet, spring coils and a red lift lever
    t.rect(5, 2, 10, 13, S[2])
    bevel(t, 5, 2, 10, 13, S, hi=S[5], lo=S[0])
    for y in range(3, 9, 2):
        t.hline(6, 9, y, S[5])
        t.hline(6, 9, y + 1, S[1])
    t.rect(6, 9, 9, 12, DARK[1])
    t.hline(6, 9, 9, S[4])
    t.line(10, 4, 13, 2 if open_ else 6, DANGER[4])
    t.set(13, 2 if open_ else 6, DANGER[6])
    # outlet: white when steam is blowing through it
    t.rect(1, 10, 4, 11, S[1])
    t.rect(1, 10, 1, 11, WHITE if open_ else DARK[0])
    if open_:
        t.set(0, 9, WHITE)
        t.set(2, 9, CYAN[6])
        t.set(0, 12, CYAN[6])
        t.set(11, 11, WARN[6])
        t.set(11, 12, WARN[4])
    return t


@block("relief_valve_side")
def relief_valve_side():
    return _relief_valve(False)


@block("relief_valve_side_open")
def relief_valve_side_open():
    return _relief_valve(True)


@block("relief_valve_top")
def relief_valve_top():
    t = pressurizer_top()
    t.seed = 305
    # handwheel over the nozzle
    t.ring(8, 8, 4.4, 3.5, DANGER[3])
    t.ring(8, 8, 4.6, 4.3, DANGER[1])
    for x0, y0, x1, y1 in ((8, 4, 8, 12), (4, 8, 12, 8)):
        t.line(x0, y0, x1, y1, DANGER[2])
    t.disc(8, 8, 1.4, DANGER[5])
    t.set(7, 7, DANGER[6])
    return t


# ======================================================================================
# steam generator: copper tube bundles inside a heavy steel shell
# ======================================================================================

@block("steam_generator_casing")
def steam_generator_casing():
    t = Tex(seed=310)
    casing(t, STEEL, seed=310, seam=False)
    # copper weld bands of the shell courses
    for y in (5, 10):
        t.hline(3, 12, y, COPPER[4])
        t.hline(3, 12, y + 1, COPPER[1])
    return t


@block("steam_generator_controller")
def steam_generator_controller():
    t = steam_generator_casing()
    t.seed = 311
    # instrument plate: secondary pressure dial and the feedwater level column
    t.rect(2, 2, 13, 13, STEEL[3])
    bevel(t, 2, 2, 13, 13, STEEL)
    outline(t, 2, 2, 13, 13, STEEL[0])
    t.disc(6.5, 7.5, 3.4, DARK[2])
    t.ring(6.5, 7.5, 3.4, 2.8, BRASS[4])
    for a in range(200, 341, 20):
        r = math.radians(a)
        c = RADIUM[5] if a < 300 else DANGER[5]
        t.set(round(6.5 + math.cos(r) * 2.2 - 0.5), round(7.5 + math.sin(r) * 2.2 - 0.5), c)
    t.line(6, 7, 8, 5, WARN[6])
    t.rect(11, 4, 12, 11, DARK[0])
    t.rect(11, 7, 12, 11, MIMIC_BLUE[4])
    t.set(11, 7, MIMIC_BLUE[6])
    return t


@block("steam_generator_tube_bundle")
def steam_generator_tube_bundle():
    t = Tex(seed=312)
    # Rows of copper U-tubes standing in the shell: 2px tubes, bent over at the top.
    for y in range(16):
        for x in range(16):
            v = 4.4 if x % 4 in (0, 1) else 1.4
            if x % 4 == 1:
                v -= 1.0
            v += (fbm(x, y, 312, 16, 16, 2, 8.0) - 0.5) * 0.6
            t.set(x, y, COPPER[max(0, min(6, int(round(v))))])
    for x in range(16):
        t.set(x, 7, STEEL[5] if x % 2 == 0 else STEEL[3])
        t.set(x, 8, STEEL[1])
    for x in range(0, 16, 4):
        t.set(x, 0, COPPER[6])
        t.set(x + 1, 0, COPPER[5])
    return t


@block("steam_generator_tube_bundle_top")
def steam_generator_tube_bundle_top():
    t = Tex(seed=313)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 313, 16, 16, 2, 5.0)
            t.set(x, y, STEEL[3 if n < 0.55 else 4])
    # tube sheet: a grid of tube ends
    for cy in range(2, 16, 4):
        for cx in range(2, 16, 4):
            t.rect(cx - 1, cy - 1, cx, cy, COPPER[3])
            t.set(cx - 1, cy - 1, COPPER[5])
            t.set(cx, cy, DARK[0])
    outline(t, 0, 0, 15, 15, STEEL[1])
    return t


@block("steam_generator_port_primary_in")
def steam_generator_port_primary_in():
    return port_face(Tex(seed=314), STEEL, DANGER, seed=314)


@block("steam_generator_port_primary_out")
def steam_generator_port_primary_out():
    return port_face(Tex(seed=315), STEEL, MIMIC_BLUE, seed=315, out=True)


@block("steam_generator_port_feed_in")
def steam_generator_port_feed_in():
    return port_face(Tex(seed=316), STEEL, CYAN, seed=316, square=True)


@block("steam_generator_port_steam_out")
def steam_generator_port_steam_out():
    t = port_face(Tex(seed=317), STEEL, RADIUM, seed=317, out=True, square=True)
    bolt(t, 7, 12, STEEL)
    return t


# ======================================================================================
# containment building: reinforced concrete, a steel liner and its fittings
# ======================================================================================

def _concrete(t, seed, base=3.2):
    """Poured concrete: mottled grey with aggregate specks."""
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 3, 4.0) - 0.5
            v = base + n * 1.8
            t.set(x, y, CONCRETE[max(1, min(5, int(round(v))))])
    rng = t.rng
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        t.set(x, y, CONCRETE[rng.choice((1, 2, 5))])
    return t


@block("containment_concrete")
def containment_concrete():
    t = _concrete(Tex(seed=330), 330)
    # formwork joints and tie holes of a heavy pour
    for x in range(16):
        t.set(x, 7, mix(t.get(x, 7), CONCRETE[1], 0.45))
        t.set(x, 15, mix(t.get(x, 15), CONCRETE[1], 0.55))
    for x, y in ((3, 3), (12, 3), (3, 11), (12, 11)):
        t.set(x, y, CONCRETE[0])
        t.set(x + 1, y + 1, CONCRETE[4])
    return t


@block("steel_liner")
def steel_liner():
    t = Tex(seed=331)
    brushed(t, STEEL, base=3.6, amp=0.7, seed=331, axis="v")
    # welded plates: seams and studs where the liner is anchored into the concrete
    for x in range(16):
        t.set(x, 0, STEEL[1])
        t.set(x, 8, STEEL[1])
        t.set(x, 9, STEEL[5])
    for y in range(16):
        t.set(0, y, STEEL[1])
        t.set(15, y, STEEL[2])
    for x in (3, 8, 13):
        rivet(t, x, 4, STEEL)
        rivet(t, x, 12, STEEL)
    return t


@block("containment_monitor")
def containment_monitor():
    t = steel_liner()
    t.seed = 332
    # pressure dial beside a hydrogen bar gauge, and status lamps
    t.rect(2, 2, 13, 13, STEEL[3])
    bevel(t, 2, 2, 13, 13, STEEL)
    outline(t, 2, 2, 13, 13, STEEL[0])
    t.disc(6.0, 6.5, 3.4, DARK[1])
    t.ring(6.0, 6.5, 3.4, 2.8, BRASS[4])
    for a in range(200, 341, 20):
        r = math.radians(a)
        c = RADIUM[5] if a < 280 else WARN[5] if a < 320 else DANGER[5]
        t.set(round(6.0 + math.cos(r) * 2.2 - 0.5), round(6.5 + math.sin(r) * 2.2 - 0.5), c)
    t.line(6, 6, 4, 5, WHITE)
    t.rect(10, 3, 12, 9, DARK[0])
    t.rect(11, 6, 11, 9, CYAN[4])
    t.set(11, 6, CYAN[6])
    t.rect(4, 11, 5, 12, RADIUM[5])
    t.set(4, 11, RADIUM[6])
    t.rect(8, 11, 9, 12, DANGER[2])
    t.rect(11, 11, 12, 12, DARK[2])
    return t


@block("containment_penetration_side")
def containment_penetration_side():
    t = _concrete(Tex(seed=333), 333)
    # a steel sleeve runs through the wall, flanged at both faces
    t.rect(0, 5, 15, 10, STEEL[3])
    t.hline(0, 15, 5, STEEL[5])
    t.hline(0, 15, 10, STEEL[1])
    for x in (0, 1, 14, 15):
        t.vline(x, 4, 11, STEEL[4])
    t.vline(1, 4, 11, STEEL[1])
    t.vline(14, 4, 11, STEEL[5])
    for x in range(3, 13, 3):
        t.set(x, 7, STEEL[4])
        t.set(x, 8, STEEL[2])
    return t


@block("containment_penetration_end")
def containment_penetration_end():
    t = _concrete(Tex(seed=334), 334)
    # sleeve flange with the pipe bore through the middle
    t.disc(8, 8, 6.4, STEEL[2])
    t.ring(8, 8, 6.4, 5.6, STEEL[5])
    for a in range(0, 360, 90):
        r = math.radians(a + 45)
        bolt(t, round(8 + math.cos(r) * 4.6 - 1), round(8 + math.sin(r) * 4.6 - 1), STEEL)
    t.disc(8, 8, 3.2, DARK[1])
    t.ring(8, 8, 3.2, 2.5, COPPER[3])
    t.disc(8, 8, 1.6, DARK[0])
    return t


@block("recombiner_side")
def recombiner_side():
    t = Tex(seed=335)
    plate(t, STEEL, seed=335, base=3.2)
    # louvred housing over the copper catalyst plates
    t.rect(3, 3, 12, 12, DARK[1])
    for y in range(4, 12, 2):
        t.hline(3, 12, y, STEEL[4])
        t.hline(4, 11, y + 1, COPPER[2])
    t.set(4, 5, COPPER[4])
    t.set(9, 9, COPPER[4])
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(t, x, y, STEEL)
    return t


@block("recombiner_top")
def recombiner_top():
    t = Tex(seed=336)
    plate(t, STEEL, seed=336, base=3.4)
    # chimney outlet where the warm air rises out
    t.rect(4, 4, 11, 11, STEEL[1])
    vents(t, 5, 5, 10, 10, STEEL, step=1)
    outline(t, 4, 4, 11, 11, STEEL[0])
    return t


def _vent(open_):
    t = _concrete(Tex(seed=337), 337)
    # filter housing with a valve wheel; open, the stack breathes white
    t.rect(2, 2, 13, 13, STEEL[2])
    bevel(t, 2, 2, 13, 13, STEEL, hi=STEEL[5], lo=STEEL[0])
    vents(t, 4, 3, 11, 8, STEEL, step=1, dark=DARK[0], lite=STEEL[4])
    t.ring(8, 11, 2.8, 2.0, DANGER[3])
    t.line(6, 11, 10, 11, DANGER[2])
    t.line(8, 9, 8, 13, DANGER[2])
    t.set(8, 11, DANGER[5])
    lamp = RADIUM if open_ else DARK
    t.rect(12, 3, 12, 4, lamp[5])
    if open_:
        for x, y in ((5, 4), (8, 4), (10, 6), (6, 6)):
            t.set(x, y, WHITE)
        t.set(7, 3, CYAN[6])
    return t


@block("filtered_vent")
def filtered_vent():
    return _vent(False)


@block("filtered_vent_open")
def filtered_vent_open():
    return _vent(True)


# ======================================================================================
# accidents: molten core, its slag, and the floor that catches it
# ======================================================================================

def _melt(seed, hot):
    t = Tex(seed=seed)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 3, 3.5)
            if hot:
                ramp = PLASMA if n > 0.45 else DANGER
                v = 2.0 + (n - 0.3) * 6.0
            else:
                ramp = DANGER if n > 0.55 else DARK
                v = 1.0 + (n - 0.3) * 4.0
            t.set(x, y, ramp[max(0, min(6, int(round(v))))])
    # dark crust plates floating on the melt
    rng = t.rng
    for _ in range(4 if hot else 7):
        cx, cy = rng.randrange(16), rng.randrange(16)
        for dx in range(-1, 2):
            for dy in range(-1, 1):
                if rng.random() < 0.7:
                    t.set((cx + dx) % 16, (cy + dy) % 16, DARK[1] if hot else DARK[0])
    return t


@block("corium")
def corium():
    return _melt(340, True)


@block("corium_cooling")
def corium_cooling():
    return _melt(341, False)


@block("solidified_corium")
def solidified_corium():
    t = Tex(seed=342)
    # glassy black slag with metal inclusions and a sickly green sheen
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 342, 16, 16, 3, 3.0)
            v = 0.6 + n * 3.0
            t.set(x, y, DARK[max(0, min(3, int(round(v))))])
    rng = t.rng
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        t.set(x, y, STEEL[rng.choice((4, 5))])
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        t.set(x, y, RADIUM[rng.choice((2, 3))])
    cracks(t, DARK, seed=342, count=3)
    return t


@block("core_catcher_side")
def core_catcher_side():
    t = _concrete(Tex(seed=343), 343, base=2.6)
    # refractory brick courses over sacrificial concrete
    for y in range(0, 8):
        for x in range(16):
            n = fbm(x, y, 344, 16, 16, 2, 3.0) - 0.5
            t.set(x, y, DANGER[max(1, min(4, int(round(2.4 + n * 1.6))))])
    for x in range(16):
        t.set(x, 3, CONCRETE[4])
        t.set(x, 7, CONCRETE[1])
    for x in (2, 10):
        t.vline(x, 0, 2, CONCRETE[4])
    for x in (6, 14):
        t.vline(x, 4, 6, CONCRETE[4])
    return t


@block("core_catcher_top")
def core_catcher_top():
    t = Tex(seed=345)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 345, 16, 16, 2, 3.0) - 0.5
            t.set(x, y, DANGER[max(1, min(4, int(round(2.6 + n * 1.6))))])
    # a grid of refractory bricks, staggered
    for i in range(0, 16, 4):
        t.hline(0, 15, i, CONCRETE[4])
    for row in range(4):
        for x in range(0, 16, 4):
            t.vline(x + (2 if row % 2 else 0), row * 4 + 1, row * 4 + 3, CONCRETE[3])
    outline(t, 0, 0, 15, 15, CONCRETE[1])
    return t

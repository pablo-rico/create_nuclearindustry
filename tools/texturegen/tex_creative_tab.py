"""Creative tab section banners (creativetab/NuclearTabSection, NuclearTabBanners).

Each banner covers one row of the creative grid, 162x18, and comes as FRAMES frames stacked
vertically; the game shows frame 0 at rest and plays the rest while the mouse is over it, so the
animation must loop. The left part is a dithered gradient in the section's colours where the
title sits; the right part holds the section's picture.
"""

import math

from gui import gui
from palette import (BRASS, CONCRETE, CYAN, DANGER, DARK, FLUORITE, LEAD, MIMIC_BLUE, RADIUM, STEEL,
                     STONE, TRITIUM, URANIUM, WARN, YELLOWCAKE)
from pixelkit import Ramp, Tex, fbm, mix

FRAMES = 16  # NuclearTabSection.FRAMES
W, H = 162, 18
TAU = math.tau

BAYER = ((0, 8, 2, 10), (12, 4, 14, 6), (3, 11, 1, 9), (15, 7, 13, 5))


def _dither(x, y):
    return (BAYER[y % 4][x % 4] + 0.5) / 16


class Row:
    """One frame of a banner: drawing clipped to the inside of its 1px border."""

    def __init__(self, t, f):
        self.t, self.f, self.oy = t, f, f * H
        self.phase = f / FRAMES

    def set(self, x, y, c):
        x, y = int(x), int(y)
        if 1 <= x < W - 1 and 1 <= y < H - 1:
            self.t.set(x, y + self.oy, c)

    def get(self, x, y):
        return self.t.get(int(x), int(y) + self.oy)

    def blend(self, x, y, c, a):
        x, y = int(x), int(y)
        if 1 <= x < W - 1 and 1 <= y < H - 1 and a > 0:
            self.set(x, y, mix(self.get(x, y), c, min(1.0, a)))

    def rect(self, x0, y0, x1, y1, c):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.set(x, y, c)

    def disc(self, cx, cy, r, c, a=1.0):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r:
                    self.blend(x, y, c, a)

    def ring(self, cx, cy, rx, ry, c, width=0.6, a=1.0):
        for y in range(int(cy - ry - 2), int(cy + ry + 3)):
            for x in range(int(cx - rx - 2), int(cx + rx + 3)):
                d = math.hypot((x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry)
                if abs(d - 1) * min(rx, ry) <= width:
                    self.blend(x, y, c, a)

    def line(self, x0, y0, x1, y1, c, a=1.0):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n + 1):
            k = i / n
            self.blend(round(x0 + (x1 - x0) * k), round(y0 + (y1 - y0) * k), c, a)

    def trefoil(self, cx, cy, r, c, turn=0.0):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(dx, dy)
                if d <= r * 0.26:
                    self.set(x, y, c)
                    continue
                if not (r * 0.46 <= d <= r):
                    continue
                a = (math.degrees(math.atan2(dy, dx)) - turn * 360) % 360
                if any(min(abs(a - b), 360 - abs(a - b)) <= 30 for b in (90, 210, 330)):
                    self.set(x, y, c)


def _base(row, ramp, lo=1.0, hi=3.4):
    """Dithered left-to-right gradient over ramp[lo..hi], with the darker 1px border."""
    for y in range(H):
        for x in range(W):
            if x in (0, W - 1) or y in (0, H - 1):
                row.t.set(x, y + row.oy, ramp[0])
                continue
            v = lo + (hi - lo) * (x / (W - 1)) ** 1.4 - 0.25 * (y / H)
            row.t.set(x, y + row.oy, ramp[int(v + _dither(x, y))])
    for x in range(1, W - 1):
        row.blend(x, 1, ramp[6], 0.18)


def _fade(row, ramp, x0, x1):
    """Softens a picture's left edge back into the gradient."""
    for y in range(1, H - 1):
        for x in range(x0, x1):
            k = (x1 - x) / (x1 - x0)
            if _dither(x, y) < k * 0.8:
                v = 1.0 + 2.4 * (x / (W - 1)) ** 1.4 - 0.25 * (y / H)
                row.set(x, y, ramp[int(v + _dither(x + 1, y))])


def banner(name, ramp, seed):
    def deco(draw):
        @gui("creative_section/" + name)
        def make():
            t = Tex(W, H * FRAMES, seed)
            for f in range(FRAMES):
                row = Row(t, f)
                _base(row, ramp)
                draw(row, row.phase)
                _fade(row, ramp, 94, 104)
            return t
        return draw
    return deco


def _pulse(phase, offset=0.0):
    return 0.5 + 0.5 * math.sin(TAU * (phase + offset))


# --------------------------------------------------------------------------------------
# sections
# --------------------------------------------------------------------------------------

MATERIALS = Ramp("#1e2418", "#2b3326", "#3a4432", "#4b5640", "#5e6a50", "#7a8766", "#a4b28a")


@banner("materials", MATERIALS, 1101)
def _materials(row, phase):
    # A rock face with uranium veins that glint one after another.
    for y in range(1, H - 1):
        for x in range(104, W - 1):
            n = fbm(x, y, 1101, 64, 18, octaves=3, scale=6.0)
            row.set(x, y, STONE[int(1 + n * 4.5)])
    veins = ((112, 5), (113, 6), (114, 6), (121, 11), (122, 11), (122, 12), (131, 4), (132, 5),
             (139, 9), (140, 10), (141, 10), (148, 13), (149, 13), (152, 6), (153, 6), (127, 14))
    for i, (x, y) in enumerate(veins):
        g = _pulse(phase, i / len(veins))
        row.set(x, y, URANIUM[int(3 + g * 3.4)])
        if g > 0.93:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                row.blend(x + dx, y + dy, URANIUM[6], 0.7)
    # A pick leaning on the wall.
    row.line(155, 3, 145, 15, BRASS[2])
    row.line(150, 2, 159, 6, STEEL[5])


FUEL = Ramp("#2a1f08", "#3d2b0c", "#5b3f12", "#7a5716", "#a07820", "#c89a30", "#f0d070")


@banner("fuel_cycle", FUEL, 1102)
def _fuel_cycle(row, phase):
    # A cascade of gas centrifuges, their highlights turning, on a header pipe.
    row.rect(104, 14, W - 2, 15, STEEL[2])
    row.rect(104, 14, W - 2, 14, STEEL[4])
    for i, cx in enumerate(range(116, W - 4, 9)):
        for y in range(2, 14):
            for dx in range(-2, 3):
                shade = 2 + (2 - abs(dx)) * 0.9
                row.set(cx + dx, y, STEEL[int(shade)])
            band = (y + int((phase * 12) + i * 3)) % 6
            if band == 0:
                for dx in range(-2, 3):
                    row.blend(cx + dx, y, STEEL[6], 0.55)
        row.rect(cx - 2, 2, cx + 2, 2, DARK[2])
        row.set(cx, 1, YELLOWCAKE[5])
    for x in range(104, W - 1):
        if (x - int(phase * 9 * 2)) % 9 == 0:
            row.set(x, 15, YELLOWCAKE[5])


FISSION = Ramp("#081428", "#10284a", "#16386a", "#1e4c8a", "#2a64aa", "#4a8ad0", "#9ad0ff")
CHERENKOV = Ramp("#10306a", "#1a4ca0", "#2a6ad0", "#4a8cf0", "#78b4ff", "#a8d8ff", "#e0f4ff")


@banner("fission", FISSION, 1103)
def _fission(row, phase):
    # Fuel rods under water, Cherenkov light pulsing up them.
    for y in range(1, H - 1):
        for x in range(104, W - 1):
            row.set(x, y, FISSION[1 + int(_dither(x, y) + 0.6)])
    for i, rx in enumerate(range(110, W - 4, 8)):
        for y in range(3, H - 1):
            g = _pulse(phase, -y / 16 + i * 0.13)
            for dx in (-2, 2):
                row.blend(rx + dx, y, CHERENKOV[int(1 + g * 4)], 0.35 + 0.4 * g)
            for dx in (-1, 0, 1):
                row.set(rx + dx, y, LEAD[3 + (dx == 0)])
        row.rect(rx - 1, 2, rx + 1, 2, LEAD[1])
    for x in range(104, W - 1):
        row.blend(x, 1, CHERENKOV[5], 0.25)


PRIMARY = Ramp("#2a1816", "#3a2422", "#4c2928", "#733b35", "#9a5144", "#ba6754", "#eea18a")


@banner("primary_loop", PRIMARY, 1104)
def _primary_loop(row, phase):
    # The containment dome, with steam rising from the plant beside it.
    cx, base = 140, 16
    for y in range(4, base + 1):
        for x in range(122, 159):
            dx = (x + 0.5 - cx) / 17
            dy = (base - y) / 12
            if dx * dx + dy * dy <= 1:
                n = fbm(x, y, 1104, 40, 18, octaves=2, scale=8.0)
                row.set(x, y, CONCRETE[int(2 + n * 2.5 + (0.6 if dx < 0 else 0))])
    row.ring(cx, base, 17, 12, CONCRETE[1], width=0.5)
    row.rect(122, 16, 158, 16, CONCRETE[1])
    row.rect(108, 8, 113, 16, STEEL[2])
    row.rect(108, 8, 108, 16, STEEL[4])
    for k in range(3):
        p = (phase + k / 3) % 1
        y = 7 - p * 7
        row.disc(110.5 + p * 3, y, 1.4 + p * 2.2, (240, 240, 240, 255), a=0.75 * (1 - p))
    row.set(140, 4, DANGER[5] if phase < 0.5 else DANGER[2])


POWER = Ramp("#1a1d1b", "#2a2f2c", "#3a403c", "#4d5650", "#68736b", "#849087", "#b5bfb0")


@banner("power", POWER, 1105)
def _power(row, phase):
    # A turbine wheel turning beside a cooling tower and its plume.
    cx, cy = 117.5, 9.0
    for y in range(1, H - 1):
        for x in range(108, 128):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > 7.6:
                continue
            a = (math.atan2(dy, dx) / TAU - phase / 6) % (1 / 6) * 6
            if d < 1.8:
                row.set(x, y, BRASS[5])
            elif a < 0.55:
                row.set(x, y, BRASS[int(2 + (d / 7.6) * 2.5)])
            else:
                row.set(x, y, DARK[1])
    row.ring(cx, cy, 7.6, 7.6, STEEL[1], width=0.6)
    tx = 147
    for y in range(5, 17):
        k = (y - 5) / 11
        half = 9 - 3.6 * math.sin(math.pi * (0.2 + 0.65 * k))
        for x in range(int(tx - half), int(tx + half) + 1):
            row.set(x, y, CONCRETE[2 if x < tx - half * 0.4 else 4 if x > tx + half * 0.4 else 3])
    row.rect(int(tx - 5.6), 5, int(tx + 5.6), 5, CONCRETE[5])
    for k in range(4):
        p = (phase + k / 4) % 1
        row.disc(tx + math.sin(TAU * (p + k * 0.3)) * 1.5, 4 - p * 5, 3.0 + p * 2.5, (240, 242, 236, 255), a=0.85 * (1 - p))


CONTROL = Ramp("#141a17", "#1f2824", "#2f3a35", "#45534b", "#5c6d62", "#73867a", "#a7b8a8")


@banner("control_room", CONTROL, 1106)
def _control_room(row, phase):
    # Annunciator windows blinking over a chart recorder trace.
    lamps = ((DANGER, 0.0), (WARN, 0.3), (RADIUM, 0.55), (WARN, 0.8), (RADIUM, 0.15), (DANGER, 0.65))
    for i, (ramp, off) in enumerate(lamps):
        x0, y0 = 106 + (i % 3) * 8, 2 + (i // 3) * 7
        on = ((phase + off) * 2) % 1 < 0.5
        row.rect(x0, y0, x0 + 6, y0 + 5, DARK[0])
        row.rect(x0 + 1, y0 + 1, x0 + 5, y0 + 4, ramp[5] if on else ramp[1])
        if on:
            row.set(x0 + 1, y0 + 1, ramp[6])
    row.rect(132, 2, W - 3, 15, (226, 222, 204, 255))
    for x in range(132, W - 2):
        if (x - 132) % 6 == 0:
            for y in range(2, 16):
                row.set(x, y, (196, 190, 170, 255))
    prev = None
    for x in range(132, W - 2):
        s = (x - 132) / 26 + phase
        y = 8.5 + 3.2 * math.sin(TAU * s) + 1.6 * math.sin(TAU * 3 * s)
        if prev is not None:
            row.line(x - 1, prev, x, y, DANGER[2])
        prev = y
    row.rect(W - 4, 2, W - 3, 15, DARK[2])


SPENT = Ramp("#0c2020", "#14302f", "#203b3d", "#315b5c", "#477d7c", "#62a09b", "#b3dfcf")


@banner("spent_fuel", SPENT, 1107)
def _spent_fuel(row, phase):
    # Fuel racks glowing in the pool, and a waste drum on the deck.
    for y in range(4, H - 1):
        for x in range(104, 140):
            row.set(x, y, CYAN[1 + int(_dither(x, y) + 0.5 + (y > 10) * 0.6)])
    for x in range(104, 140):
        row.set(x, 4, CYAN[5] if (x + int(phase * 15)) % 5 else CYAN[6])
    for i, rx in enumerate(range(107, 139, 6)):
        g = _pulse(phase, i * 0.2)
        for y in range(9, H - 1):
            row.blend(rx + 1, y, CHERENKOV[5], 0.3 + 0.4 * g)
        row.rect(rx, 9, rx, H - 2, LEAD[2])
        row.rect(rx + 2, 9, rx + 2, H - 2, LEAD[2])
    row.rect(143, 4, 156, 16, WARN[3])
    row.rect(143, 4, 144, 16, WARN[4])
    row.rect(143, 6, 156, 6, WARN[1])
    row.rect(143, 14, 156, 14, WARN[1])
    row.trefoil(150, 10, 3.4, DARK[0])


ISOTOPES = Ramp("#12261a", "#1e3a2a", "#284a34", "#355a36", "#4d8042", "#70a453", "#c4e793")


@banner("isotopes", ISOTOPES, 1108)
def _isotopes(row, phase):
    # A nucleus shedding particles, an electron on its orbit.
    cx, cy = 132, 9
    for k in range(6):
        ang = TAU * (k / 6 + 0.08)
        p = (phase * 2 + k / 6) % 1
        d = 4 + p * 22
        row.disc(cx + math.cos(ang) * d, cy + math.sin(ang) * d * 0.45, 0.9, RADIUM[6], a=1 - p)
        row.blend(cx + math.cos(ang) * (d - 2), cy + math.sin(ang) * (d - 2) * 0.45, RADIUM[4], 0.5 * (1 - p))
    row.ring(cx, cy, 13, 4.6, ISOTOPES[5], width=0.45, a=0.8)
    for i, (dx, dy) in enumerate(((-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1), (0, -2), (0, 2), (-2, 0), (2, 0))):
        row.disc(cx + dx * 1.3, cy + dy * 1.3, 1.4, (DANGER if i % 2 else CYAN)[4 + (i == 2)])
    ang = TAU * phase
    row.disc(cx + math.cos(ang) * 13, cy + math.sin(ang) * 4.6, 1.2, CYAN[6])
    # The medical cross.
    row.rect(151, 6, 155, 12, (236, 236, 228, 255))
    row.rect(149, 8, 157, 10, (236, 236, 228, 255))
    row.rect(152, 7, 154, 11, DANGER[3])
    row.rect(150, 9, 156, 9, DANGER[3])


SAFETY = Ramp("#1a160a", "#2a2410", "#3d3418", "#574023", "#7b5b2c", "#a48038", "#e5c567")


@banner("safety", SAFETY, 1109)
def _safety(row, phase):
    # Hazard stripes running past a trefoil badge and a spanner.
    shift = int(phase * 8)
    for y in range(1, H - 1):
        for x in range(104, W - 1):
            row.set(x, y, WARN[4] if ((x + y - shift) % 8) < 4 else DARK[0])
    row.disc(127, 9, 8.0, DARK[0])
    row.disc(127, 9, 7.2, WARN[5])
    row.trefoil(127, 9, 6.4, DARK[0], turn=phase / 3)
    row.disc(149, 9, 6.6, DARK[0])
    row.disc(149, 9, 5.8, STEEL[3])
    row.line(145, 13, 153, 5, STEEL[6])
    row.line(146, 13, 154, 5, STEEL[5])
    row.disc(153.5, 5.5, 1.8, STEEL[6])
    row.set(154, 5, STEEL[3])


FUSION = Ramp("#1e1024", "#2e1a36", "#432344", "#654966", "#876487", "#aa80a1", "#e6c4d0")
PLASMA_PINK = Ramp("#5a1a5a", "#8a2a8a", "#b444b4", "#d86ad8", "#f09af0", "#ffc8ff", "#fff0ff")


@banner("fusion", FUSION, 1110)
def _fusion(row, phase):
    # A tokamak torus seen from the side, plasma bright spots running round it.
    cx, cy = 132, 9
    for y in range(1, H - 1):
        for x in range(106, 160):
            d = math.hypot((x + 0.5 - cx) / 25, (y + 0.5 - cy) / 7)
            if 0.55 <= d <= 1.0:
                row.set(x, y, TRITIUM[int(1 + (1 - abs(d - 0.78) / 0.22) * 2.2)])
    for k in range(40):
        ang = TAU * k / 40
        x = cx + math.cos(ang) * 19.5
        y = cy + math.sin(ang) * 5.4
        g = max(_pulse(phase * 2, -k / 40), _pulse(phase * 2, -k / 40 + 0.5)) ** 3
        front = math.sin(ang) > 0
        row.blend(x, y, PLASMA_PINK[int(2 + g * 4)], (0.6 if front else 0.35) + 0.4 * g)
    for x in range(126, 139):
        row.set(x, 9, TRITIUM[1])
    row.rect(131, 3, 133, 15, TRITIUM[2])
    row.rect(131, 3, 131, 15, TRITIUM[4])


CRYOGENICS = Ramp("#141e26", "#22303a", "#38494a", "#526e6d", "#74928c", "#9bb3a8", "#f0f2df")
ICE = Ramp("#5a7a90", "#7a9ab0", "#9abacc", "#bad4e0", "#d4e8f0", "#ecf6fa", "#ffffff")


@banner("cryogenics", CRYOGENICS, 1111)
def _cryogenics(row, phase):
    # Frost mist drifting, with snowflakes falling through it.
    for y in range(1, H - 1):
        for x in range(104, W - 1):
            n = fbm((x + int(phase * 64)) % 64, y, 1111, 64, 18, octaves=3, scale=4.0)
            row.set(x, y, CRYOGENICS[int(2 + n * 3.2 + _dither(x, y) * 0.6)])
    flakes = ((110, 0.0), (121, 0.4), (130, 0.75), (139, 0.2), (147, 0.55), (155, 0.9))
    for x, off in flakes:
        y = 1 + ((phase + off) % 1) * 17
        cx = x + math.sin(TAU * (phase + off)) * 1.2
        row.blend(cx, y, ICE[6], 0.95)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            row.blend(cx + dx, y + dy, ICE[4], 0.6)
    for x in range(104, W - 1):
        row.set(x, H - 2, ICE[3 + ((x * 7) % 3 == 0)])


ACCEL = Ramp("#0c1222", "#141d33", "#1d2a45", "#2a3f6b", "#385693", "#4a70b5", "#97b6e3")
BEAM = Ramp("#204a6a", "#2a6a9a", "#3a8aca", "#5ab0f0", "#8ad0ff", "#c0eaff", "#ffffff")


@banner("accelerator", ACCEL, 1112)
def _accelerator(row, phase):
    # A bunch of particles racing down the beam pipe between quadrupoles.
    row.rect(104, 7, W - 2, 10, STEEL[2])
    row.rect(104, 7, W - 2, 7, STEEL[4])
    row.rect(104, 10, W - 2, 10, STEEL[1])
    for x in range(108, W - 2, 14):
        row.rect(x, 3, x + 4, 6, DANGER[3])
        row.rect(x, 11, x + 4, 14, MIMIC_BLUE[4])
        row.rect(x, 3, x + 4, 3, DANGER[5])
        row.rect(x, 14, x + 4, 14, MIMIC_BLUE[2])
    span = W - 2 - 104
    head = 104 + phase * span
    for k in range(18):
        x = head - k
        if x < 104:
            x += span
        row.blend(x, 8, BEAM[6 - k // 3], 1 - k / 18)
        row.blend(x, 9, BEAM[5 - k // 3], 1 - k / 18)


ANTIMATTER = Ramp("#140a24", "#221438", "#2e1f45", "#46306a", "#614a8f", "#7e68b0", "#c0b1e0")
ANTI_CLOUD = Ramp("#4a2a8a", "#6a3aba", "#8a5ae0", "#4adc80", "#8aff9a", "#d0ffd8", "#ffffff")


@banner("antimatter", ANTIMATTER, 1113)
def _antimatter(row, phase):
    # A Penning trap: a cloud pulsing between its two magnet heads.
    cx, cy = 132, 9
    for x in range(118, 147):
        row.blend(x, 3, (180, 200, 220, 255), 0.35)
        row.blend(x, 15, (180, 200, 220, 255), 0.35)
    for y in range(4, 15):
        row.blend(118, y, (200, 220, 240, 255), 0.4)
        row.blend(146, y, (200, 220, 240, 255), 0.4)
    beat = _pulse(phase * 2)
    r = 3.2 + beat * 1.6
    row.disc(cx, cy, r + 2.4, FLUORITE[4], a=0.35)
    row.disc(cx, cy, r + 1.0, ANTI_CLOUD[2], a=0.7)
    row.disc(cx, cy, r, ANTI_CLOUD[4], a=0.85)
    row.disc(cx, cy, r * 0.45, ANTI_CLOUD[6])
    for y0, y1 in ((1, 3), (15, 16)):
        row.rect(114, y0, 150, y1, STEEL[2])
        row.rect(114, y0, 150, y0, STEEL[4])
        for x in range(116, 149, 3):
            row.set(x, y0 + (y1 - y0) // 2 + (y0 == 1), BRASS[3])
    row.rect(122, 2, 142, 2, ANTI_CLOUD[3 + (beat > 0.5)])
    row.rect(122, 15, 142, 15, ANTI_CLOUD[3 + (beat > 0.5)])
    for k in range(5):
        ang = TAU * (phase + k / 5)
        row.blend(cx + math.cos(ang) * (r + 3), cy + math.sin(ang) * (r + 1) * 0.8, ANTI_CLOUD[5], 0.8)


CIVIL = Ramp("#181a0e", "#262a18", "#353a20", "#4a5230", "#5f6a3e", "#7a8650", "#c4cc90")
SCOPE = Ramp("#0a1a0a", "#123012", "#1a4a1a", "#2a6a2a", "#4a9a4a", "#7ad07a", "#c0ffc0")


@banner("civil_defense", CIVIL, 1114)
def _civil_defense(row, phase):
    # A radar scope sweeping, and a siren with its warning light turning.
    cx, cy, r = 124.5, 9.0, 7.6
    sweep = phase * TAU
    for y in range(1, H - 1):
        for x in range(116, 134):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > r:
                continue
            behind = (sweep - math.atan2(dy, dx)) % TAU
            g = max(0.0, 1 - behind / 2.2)
            row.set(x, y, SCOPE[int(1 + g * 4)])
    row.ring(cx, cy, r, r, CIVIL[0], width=0.6)
    row.ring(cx, cy, r * 0.5, r * 0.5, SCOPE[3], width=0.4, a=0.7)
    for bx, by, bang in ((128, 6, 5.6), (121, 12, 2.4)):
        lit = (sweep - bang) % TAU
        if lit < 2.6:
            row.set(bx, by, SCOPE[6])
    # The siren on its pole.
    row.rect(150, 9, 151, 16, STEEL[2])
    row.rect(145, 4, 156, 8, STEEL[3])
    row.rect(145, 4, 156, 4, STEEL[5])
    for x in range(145, 157):
        row.set(x, 6, DARK[1] if x % 2 else STEEL[2])
    flash = _pulse(phase * 2) ** 4
    row.disc(150.5, 2.5, 1.5, DANGER[int(2 + flash * 4)])
    row.disc(150.5, 2.5, 4.5, DANGER[6], a=0.25 * flash)


WEAPONS = Ramp("#200e06", "#3a1a0c", "#4c2410", "#743818", "#a05020", "#d07830", "#ffc080")
FIRE = Ramp("#5a1a08", "#8a2a0a", "#c04a10", "#e87a20", "#ffaa40", "#ffd890", "#fff4d0")


@banner("weapons", WEAPONS, 1115)
def _weapons(row, phase):
    # A mushroom cloud boiling over the horizon.
    cx = 136
    for y in range(1, H - 1):
        for x in range(112, 160):
            n = fbm((x * 2 + int(phase * 64)) % 64, (y * 2 - int(phase * 32)) % 32, 1115, 64, 32, octaves=2, scale=4.0)
            cap = ((x + 0.5 - cx) / 15) ** 2 + ((y + 0.5 - 5.5) / 4.6) ** 2
            stem = abs(x + 0.5 - cx) <= 2.6 + (y - 8) * 0.35 and 8 <= y < 16
            if cap <= 1 or stem:
                heat = (1 - min(1.0, cap)) * 0.6 + n * 0.5 + (0.2 if stem else 0)
                row.set(x, y, FIRE[int(1 + heat * 4.6)])
    for y in (5, 6):
        for x in range(cx - 13, cx + 14):
            if (x + int(phase * 28)) % 7 == 0:
                row.blend(x, y, FIRE[6], 0.7)
    for x in range(104, W - 1):
        row.set(x, 16, WEAPONS[0])
        row.blend(x, 15, FIRE[3], 0.5 * max(0.0, 1 - abs(x - cx) / 26))


OTHER = Ramp("#1a1a1a", "#2a2a2a", "#3a3a3a", "#4e4e4e", "#666666", "#808080", "#c0c0c0")


@banner("other", OTHER, 1116)
def _other(row, phase):
    # A slowly turning trefoil.
    row.disc(132, 9, 8.0, DARK[0])
    row.disc(132, 9, 7.2, WARN[4])
    row.trefoil(132, 9, 6.4, DARK[0], turn=phase / 3)

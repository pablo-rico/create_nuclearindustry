"""The mod's icon (NeoForge `logoFile`, Modrinth project icon) and the README banner.

The icon is painted at 64x64 and written upscaled x8 with nearest-neighbour, so it stays
pixel art at 512x512 and still reads at 32px in the mods list: a brass Create cogwheel
framing a glowing core with a dark trefoil, on a dark rounded square lit from the top-left.
The banner puts the same icon, at the same x4 pixel scale as its background, in front of a
power plant at dusk. No lettering: the mods list and the README carry the title.
"""

import math

from palette import BRASS, CONCRETE, DANGER, DARK, RADIUM, STEEL, URANIUM
from pixelkit import Ramp, Tex, fbm, mix

ICON = 64
ICON_SCALE = 8
BANNER_W, BANNER_H = 300, 75
BANNER_SCALE = 4

# Relative to the repository root.
LOGO_PATH = ("src", "main", "resources", "logo.png")
BANNER_PATH = ("docs", "banner.png")

BAYER = ((0, 8, 2, 10), (12, 4, 14, 6), (3, 11, 1, 9), (15, 7, 13, 5))

# The glowing core: deep green at the rim up to a near-white centre.
CORE = Ramp("#1d4a2a", "#2f7a3a", "#4fa844", "#7fd25a", "#b0ec78", "#dcfaa8", "#f8ffe4")
# Cherenkov blue-green at the rim of the core.
CHERENKOV = (72, 214, 196, 255)
# Background of the icon plate: a cool blue-black, close to DARK but deeper.
PLATE = Ramp("#0b0f12", "#12191d", "#182226", "#202c30", "#2b393c", "#3a4a4b", "#55665f")
# Dusk sky over the plant, top to horizon.
SKY = Ramp("#0b1219", "#101b26", "#172636", "#213244", "#353f4d", "#5c5450", "#9a7254",
           "#d09a62")
OUTLINE = (8, 10, 12, 255)


def _dither(x, y):
    return (BAYER[y % 4][x % 4] + 0.5) / 16


def _pick(ramp, v, x, y):
    """Ordered-dither a fractional ramp index."""
    return ramp[int(v + _dither(x, y))]


def _upscale(im, k):
    from PIL import Image
    return im.resize((im.width * k, im.height * k), Image.Resampling.NEAREST)


# --------------------------------------------------------------------------------------
# masks
# --------------------------------------------------------------------------------------

def _rounded_square(x, y, size, radius):
    """Signed distance from pixel (x, y)'s centre to a rounded square filling the canvas."""
    c = size / 2
    h = c - radius
    qx, qy = abs(x + 0.5 - c) - h, abs(y + 0.5 - c) - h
    out = math.hypot(max(qx, 0.0), max(qy, 0.0))
    return out + min(max(qx, qy), 0.0) - radius


def _cog(x, y, cx, cy, teeth, root, tip, bore, half_width, turn=0.0):
    """True if pixel (x, y) lies on a cogwheel: a ring root..bore plus square teeth."""
    dx, dy = x + 0.5 - cx, y + 0.5 - cy
    d = math.hypot(dx, dy)
    if d < bore or d > tip + 1:
        return False
    if d <= root:
        return True
    step = math.tau / teeth
    for k in range(teeth):
        a = turn + k * step
        u = dx * math.cos(a) + dy * math.sin(a)
        v = -dx * math.sin(a) + dy * math.cos(a)
        if root - 1 <= u <= tip and abs(v) <= half_width:
            return True
    return False


def _trefoil(x, y, cx, cy, r):
    """The radiation trefoil, one blade pointing down."""
    dx, dy = x + 0.5 - cx, y + 0.5 - cy
    d = math.hypot(dx, dy)
    if d <= r * 0.26:
        return True
    if not (r * 0.45 <= d <= r):
        return False
    a = (math.degrees(math.atan2(dy, dx)) + 360) % 360
    return any(min(abs(a - b), 360 - abs(a - b)) <= 30 for b in (90, 210, 330))


def _shade_mask(t, mask, ox, oy, w, h, face, hi, lo, edge):
    """Paints a mask with `face(x, y)`, a top-left highlight, a bottom-right shadow and a
    1px outline just outside it."""
    def inside(x, y):
        return (x, y) in mask
    for (x, y) in mask:
        t.set(x, y, face(x, y))
    for (x, y) in mask:
        if not inside(x - 1, y) or not inside(x, y - 1):
            t.set(x, y, hi(x, y))
        elif not inside(x + 1, y) or not inside(x, y + 1):
            t.set(x, y, lo(x, y))
    for y in range(oy - 1, oy + h + 1):
        for x in range(ox - 1, ox + w + 1):
            if inside(x, y):
                continue
            if any(inside(x + ddx, y + ddy) for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                t.set(x, y, edge)


# --------------------------------------------------------------------------------------
# icon
# --------------------------------------------------------------------------------------

def paint_icon():
    n = ICON
    t = Tex(n, n, 7101)
    c = n / 2
    radius = 10

    # Plate: a dark rounded square, slightly lighter towards the top-left, with a bevel.
    for y in range(n):
        for x in range(n):
            d = _rounded_square(x, y, n, radius)
            if d > 0:
                continue
            if d > -1:
                t.set(x, y, OUTLINE)
                continue
            lit = (x + 0.5 - c) + (y + 0.5 - c) < 0
            if d > -2:
                t.set(x, y, PLATE[6] if lit else PLATE[1])
                continue
            if d > -3:
                t.set(x, y, PLATE[4] if lit else PLATE[2])
                continue
            v = 3.1 - 1.6 * ((x + y) / (2 * n)) + 0.25 * (fbm(x, y, 7101, n, n, 2, 4.0) - 0.5)
            t.set(x, y, _pick(PLATE, v, x, y))

    # Radiance: a green haze around the wheel, strongest just outside the teeth.
    for y in range(n):
        for x in range(n):
            if _rounded_square(x, y, n, radius) > -3:
                continue
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            g = max(0.0, 1 - abs(d - 26) / 8)
            level = int(g * 1.6 + _dither(x, y))
            if level:
                t.set(x, y, mix(t.get(x, y), CORE[1], (0.22, 0.42)[min(level, 2) - 1]))

    # The cogwheel: brass, twelve square teeth, lit from the top-left.
    root, tip, bore = 23.0, 29.0, 16.5
    cog = {(x, y) for y in range(n) for x in range(n)
           if _cog(x, y, c, c, 12, root, tip, bore, 3.6)}

    def face(x, y):
        dx, dy = x + 0.5 - c, y + 0.5 - c
        light = -(dx + dy) / (2 * tip)                     # -0.7 .. 0.7
        d = math.hypot(dx, dy)
        v = 3.3 + light * 2.8
        if d > root:                                     # the teeth sit one shade lower
            v -= 0.5
        return BRASS[int(round(v))]

    _shade_mask(t, cog, 0, 0, n, n, face,
                hi=lambda x, y: BRASS[5] if (x + y) < n + 6 else BRASS[3],
                lo=lambda x, y: BRASS[1],
                edge=OUTLINE)

    # A groove round the ring and six bolts on it, like Create's large cogwheel hub.
    for y in range(n):
        for x in range(n):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            if (x, y) in cog and 19.4 <= d <= 20.4:
                t.set(x, y, BRASS[1] if (x + y) > n else BRASS[2])
    for k in range(6):
        a = math.tau * (k / 6 + 1 / 12)
        bx, by = int(c + math.cos(a) * 21.6 - 0.5), int(c + math.sin(a) * 21.6 - 0.5)
        t.set(bx, by, BRASS[6])
        t.set(bx + 1, by, BRASS[4])
        t.set(bx, by + 1, BRASS[4])
        t.set(bx + 1, by + 1, BRASS[0])

    # The core: a recessed steel collar, then a glowing disc fading out from the centre.
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            d = math.hypot(dx, dy)
            if d > bore:
                continue
            if d > bore - 1.6:
                lit = dx + dy > 0                         # recess: shadow falls top-left
                t.set(x, y, STEEL[3] if lit else DARK[0])
                continue
            k = d / (bore - 1.6)
            v = 6.6 - 4.0 * k ** 1.5
            col = _pick(CORE, v, x, y)
            if k > 0.8:
                col = mix(col, CHERENKOV, 0.6)              # a Cherenkov rim
            t.set(x, y, col)

    # The trefoil, dark on the glow with a 1px soft edge so it does not bleed at 32px.
    r = 12.2
    tre = {(x, y) for y in range(n) for x in range(n) if _trefoil(x, y, c, c, r)}
    for (x, y) in tre:
        t.set(x, y, (16, 26, 18, 255))
    for (x, y) in tre:
        for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + ddx, y + ddy)
            if p not in tre and math.hypot(p[0] + 0.5 - c, p[1] + 0.5 - c) < bore - 1.6:
                t.set(*p, mix(t.get(*p), (16, 26, 18, 255), 0.35))
    # A single glint on the top-left of the core glass.
    for (x, y) in ((24, 22), (25, 21), (23, 23)):
        if (x, y) not in tre:
            t.set(x, y, CORE[6])
    return t


# --------------------------------------------------------------------------------------
# banner
# --------------------------------------------------------------------------------------

def _cooling_tower(t, cx, base, height, half_base, seed):
    """A hyperboloid tower lit from the left, with its plume billowing up and drifting right."""
    top = base - height
    waist = 0.3
    for y in range(top, base):
        k = (y - top) / height
        if k < waist:
            f = 0.6 + 0.1 * ((waist - k) / waist) ** 2
        else:
            f = 0.6 + 0.4 * ((k - waist) / (1 - waist)) ** 2
        half = half_base * f
        x0, x1 = int(round(cx - half)), int(round(cx + half))
        for x in range(x0, x1 + 1):
            u = (x + 0.5 - (cx - half)) / (2 * half)         # 0 at the lit left side
            v = 4.8 - 3.4 * u - 0.5 * k
            t.set(x, y, _pick(CONCRETE, v, x, y))
        t.set(x0, y, CONCRETE[0])
        t.set(x1, y, CONCRETE[0])
    lip = half_base * 0.7
    for x in range(int(round(cx - lip)), int(round(cx + lip)) + 1):
        t.set(x, top, CONCRETE[0])
        t.set(x, top + 1, CONCRETE[6] if x < cx else CONCRETE[4])

    # Billows, back to front: each a lumpy disc lit from the upper left.
    steam = Ramp("#4e5866", "#6c7684", "#8e96a0", "#b0b6ba", "#ccd0ce", "#e4e6e0", "#f6f6f0")
    puffs = []
    for i in range(9):
        r = i / 8
        puffs.append((cx + r * r * 40 + math.sin(i * 1.7) * 1.5, top - 1 - r * 22,
                      lip * 0.8 + r * 3.5, r))
    for px, py, pr, r in reversed(puffs):
        for y in range(int(py - pr - 2), int(py + pr + 2)):
            for x in range(int(px - pr - 2), int(px + pr + 2)):
                if not (0 <= x < t.w and 0 <= y < t.h):
                    continue
                dx, dy = x + 0.5 - px, y + 0.5 - py
                nn = fbm(x % 64, y % 64, seed, 64, 64, 2, 8.0)
                d = math.hypot(dx, dy) / (pr * (0.85 + 0.3 * nn))
                if d > 1:
                    continue
                light = -(dx * 0.7 + dy * 0.7) / pr
                v = 4.2 + light * 1.6 + (1 - d) * 0.6
                t.set(x, y, mix(_pick(steam, v, x, y), t.get(x, y), 0.45 * r))


def paint_banner(icon):
    w, h = BANNER_W, BANNER_H
    t = Tex(w, h, 7201)
    ground = 63

    # Sky: night overhead fading to an amber horizon, ordered-dithered.
    for y in range(h):
        for x in range(w):
            v = (y / ground) ** 1.5 * 7.2
            t.set(x, y, _pick(SKY, v, x, y))
    for i in range(46):
        sx = (i * 97 + 13) % w
        sy = (i * 37 + 5) % 26
        if t.get(sx, sy) in (SKY[0], SKY[1]):
            t.set(sx, sy, mix(SKY[1], (230, 236, 255, 255), 0.35 + 0.4 * ((i * 7) % 3 == 0)))

    # Distant hills.
    for x in range(w):
        top = ground - 6 - 4 * fbm(x, 0, 7202, w, 16, 3, 5.0) * 2
        for y in range(int(top), ground):
            t.set(x, y, mix(SKY[3], SKY[0], 0.55))

    # Pylons and their sagging lines, behind the plant.
    pylons = (96, 146, 196, 246, 296)
    for px in pylons:
        for y in range(ground - 24, ground):
            spread = (y - (ground - 24)) / 24 * 3
            t.set(int(px - spread), y, DARK[0])
            t.set(int(px + spread), y, DARK[0])
            if (y - ground) % 4 == 0:
                for x in range(int(px - spread), int(px + spread) + 1):
                    t.set(x, y, DARK[0])
        for x in range(px - 5, px + 6):
            t.set(x, ground - 22, DARK[0])
    for a, b in zip(pylons, pylons[1:]):
        for x in range(a, b + 1):
            s = (x - a) / (b - a)
            t.set(x, int(ground - 22 + 1 + 4 * s * (1 - s) * 4), mix(DARK[1], SKY[4], 0.3))

    # Cooling towers, the turbine hall, the containment dome and its stack.
    _cooling_tower(t, 128, ground, 34, 15, 7203)
    _cooling_tower(t, 168, ground, 30, 13, 7204)

    hall = (196, ground - 13, 254, ground - 1)
    for y in range(hall[1], hall[3] + 1):
        for x in range(hall[0], hall[2] + 1):
            t.set(x, y, _pick(STEEL, 2.4 - (y - hall[1]) / 14, x, y))
    for x in range(hall[0], hall[2] + 1):
        t.set(x, hall[1], STEEL[4])
    for y in range(hall[1] + 3, hall[3] - 2, 4):
        for x in range(hall[0] + 3, hall[2] - 2, 5):
            t.set(x, y, URANIUM[5])
            t.set(x + 1, y, URANIUM[4])
    for x in (hall[0], hall[2]):
        for y in range(hall[1], hall[3] + 1):
            t.set(x, y, DARK[0])

    dome_cx, dome_r = 236, 17
    for y in range(ground - 32, ground):
        for x in range(dome_cx - dome_r - 1, dome_cx + dome_r + 2):
            dx = x + 0.5 - dome_cx
            top = ground - 14 - math.sqrt(max(0.0, dome_r * dome_r - dx * dx)) * 0.95
            if y < top or abs(dx) > dome_r:
                continue
            u = dx / dome_r
            v = 4.4 - 2.6 * (u + 1) / 2 + (0.6 if y - top < 1 else 0)
            t.set(x, y, _pick(CONCRETE, v, x, y))
            if abs(abs(dx) - dome_r) < 0.8 or y - top < 0.6:
                t.set(x, y, CONCRETE[0])
    t.set(dome_cx, ground - 31, DANGER[5])

    sx = 270
    for y in range(ground - 46, ground):
        band = ((y - (ground - 46)) // 5) % 2
        for x in range(sx - 2, sx + 3):
            col = (DANGER[3] if band else CONCRETE[5]) if x < sx + 1 else (DANGER[1] if band else CONCRETE[3])
            t.set(x, y, col)
        t.set(sx - 3, y, CONCRETE[0])
        t.set(sx + 3, y, CONCRETE[0])
    t.set(sx, ground - 47, DANGER[6])

    # Ground: a dark apron with a road and a fence line catching the horizon light.
    for y in range(ground, h):
        for x in range(w):
            t.set(x, y, _pick(DARK, 1.6 - (y - ground) / 10, x, y))
    for x in range(w):
        t.set(x, ground, mix(DARK[3], SKY[6], 0.35))
        if x % 6 < 3:
            t.set(x, ground + 6, DARK[3])
        if x % 4 == 0:
            t.set(x, ground - 1, DARK[1])
            t.set(x, ground - 2, DARK[1])
    for x in range(w):
        t.set(x, ground - 3, mix(DARK[2], SKY[5], 0.25))

    # Shade the left side so the icon sits on a calm, darker field with a faint green halo.
    ix, iy = 8, (h - ICON) // 2
    cx, cy = ix + ICON / 2, iy + ICON / 2
    for y in range(h):
        for x in range(w):
            k = max(0.0, 1 - x / 150)
            if k > 0:
                t.set(x, y, mix(t.get(x, y), SKY[0], 0.75 * k ** 1.2))
            g = max(0.0, 1 - math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 1.2) / 52)
            if g > 0 and g * 0.5 > _dither(x, y) * 0.5:
                t.set(x, y, mix(t.get(x, y), RADIUM[1], 0.5 * g))

    t.im.alpha_composite(icon.im, (ix, iy))
    return t


def logo():
    return _upscale(paint_icon().im, ICON_SCALE)


def banner():
    return _upscale(paint_banner(paint_icon()).im.convert("RGB"), BANNER_SCALE)


# Published paths -> painters, written by generate.py relative to the repository root.
LOGOS = {LOGO_PATH: logo, BANNER_PATH: banner}

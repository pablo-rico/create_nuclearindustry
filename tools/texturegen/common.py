"""Reusable composite elements shared by several textures."""

import math
import random

from palette import BLACK, DARK, STEEL
from pixelkit import (Tex, bevel, brushed, fbm, glow, inset, mix, outline, rivet,
                      scanlines, shade)


# ======================================================================================
# machine casing language
# ======================================================================================

def bolt(t, x, y, ramp):
    """A 2x2 raised bolt head - the signature detail of the whole set."""
    t.set(x, y, ramp[6])
    t.set(x + 1, y, ramp[5])
    t.set(x, y + 1, ramp[4])
    t.set(x + 1, y + 1, ramp[0])


def casing(t, ramp=STEEL, seed=0, base=3.4, amp=0.85, rivets=True, seam=True,
           axis="h", panel=(4, 4, 11, 11)):
    """Outlined, bevelled, bolted machine plate. Everything else builds on this."""
    brushed(t, ramp, 0, 0, t.w - 1, t.h - 1, base=base, amp=amp, seed=seed, axis=axis)
    if seam:
        x0, y0, x1, y1 = panel
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                t.set(x, y, shade(t.get(x, y), 0.86))
        t.hline(x0, x1, y0, ramp[1])
        t.vline(x0, y0, y1, ramp[1])
        t.hline(x0, x1, y1, ramp[5])
        t.vline(x1, y0, y1, ramp[5])
    outline(t, 0, 0, t.w - 1, t.h - 1, ramp[0])
    bevel(t, 1, 1, t.w - 2, t.h - 2, ramp)
    if rivets:
        for x, y in ((2, 2), (t.w - 4, 2), (2, t.h - 4), (t.w - 4, t.h - 4)):
            bolt(t, x, y, ramp)
    return t


def bezel(t, x0, y0, x1, y1, ramp):
    """Raised frame around a recessed feature."""
    t.rect(x0 - 1, y0 - 1, x1 + 1, y1 + 1, ramp[4])
    bevel(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, ramp)
    outline(t, x0 - 1, y0 - 1, x1 + 1, y1 + 1, ramp[0])


def panel_screen(t, x0, y0, x1, y1, ramp, accent=None, lit=False, seed=0):
    """A recessed display: dark glass so lit content reads as emissive."""
    bezel(t, x0, y0, x1, y1, ramp)
    t.rect(x0, y0, x1, y1, DARK[1] if not lit else accent[1])
    if not lit:
        for i in range(min(x1 - x0, y1 - y0) + 1):
            t.blend(x0 + i, y1 - i, ramp[5], 0.22)
        t.set(x0, y0, DARK[0])
        return
    # faint phosphor wash + scanlines
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (y - y0) % 2 == 0:
                t.set(x, y, accent[2])
    glow(t, (x0 + x1) / 2 + 0.5, (y0 + y1) / 2 + 0.5,
         max(x1 - x0, y1 - y0), accent[5], 0.18)


def bar_readout(t, x0, y0, x1, y1, accent, heights, warn=None):
    """Bright bar graph drawn inside a lit screen."""
    for i, h in enumerate(heights):
        x = x0 + i * 2
        if x > x1:
            break
        c = accent[6] if (warn is None or i < warn) else accent[4]
        t.rect(x, max(y0, y1 - h + 1), x, y1, c)
        t.set(x, max(y0, y1 - h + 1), accent[6])


# ======================================================================================
# ports
# ======================================================================================

ARROW = [
    "..##..",
    "..##..",
    "######",
    ".####.",
    "..##..",
    "......",
]


def chevron(t, dark, lite, ox=5, oy=5, up=False):
    art = ARROW if not up else list(reversed(ARROW))
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch != "#":
                continue
            t.set(ox + x, oy + y, dark)
            if y == 0 or art[y - 1][x] != "#":
                t.blend(ox + x, oy + y, lite, 0.30)


def flange(t, ramp, accent, cx=8.0, cy=8.0, r=6.0):
    """Bolted pipe flange with a bright throat."""
    t.disc(cx, cy, r, ramp[4])
    t.ring(cx, cy, r, r - 1.2, ramp[5])
    t.ring(cx, cy, r + 0.3, r - 0.3, ramp[0])
    # throat: a dark bore so the bright glyph inside it reads at a glance
    t.disc(cx, cy, r - 2.2, ramp[1])
    t.ring(cx, cy, r - 2.2, r - 2.9, ramp[0])
    t.disc(cx, cy, r - 2.9, DARK[1])
    t.ring(cx, cy, r - 2.9, r - 3.4, accent[3])
    for a in (45, 135, 225, 315):
        bx = round(cx + math.cos(math.radians(a)) * (r - 1.0) - 0.5)
        by = round(cy + math.sin(math.radians(a)) * (r - 1.0) - 0.5)
        t.set(bx, by, ramp[6])
        t.set(bx + 1, by + 1, ramp[1])


def port_face(t, ramp, accent, seed=0, out=False, square=False):
    """Casing + flange + direction glyph. The mod-wide port language."""
    casing(t, ramp, seed=seed, seam=False, rivets=False)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        bolt(t, x, y, ramp)
    if square:
        t.rect(3, 3, 12, 12, ramp[4])
        bevel(t, 3, 3, 12, 12, ramp)
        outline(t, 3, 3, 12, 12, ramp[0])
        t.rect(5, 5, 10, 10, DARK[1])
        t.hline(5, 10, 5, accent[3])
        t.vline(5, 5, 10, accent[3])
        t.hline(5, 10, 10, accent[1])
        outline(t, 4, 4, 11, 11, ramp[0])
    else:
        flange(t, ramp, accent)
    chevron(t, accent[5], accent[6], 5, 5, up=out)
    glow(t, 8, 8, 6.0, accent[6], 0.10)
    return t


# ======================================================================================
# decoration
# ======================================================================================

def trefoil(t, cx, cy, r, color):
    """Radiation trefoil, drawn analytically so it stays crisp at 16px."""
    for y in range(t.h):
        for x in range(t.w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r * 0.24:
                t.set(x, y, color)
                continue
            if not (r * 0.44 <= d <= r):
                continue
            a = (math.degrees(math.atan2(dy, dx)) + 360) % 360
            for blade in (90, 210, 330):
                if min(abs(a - blade), 360 - abs(a - blade)) <= 29:
                    t.set(x, y, color)
                    break


def cracks(t, ramp, seed=0, count=3):
    """Jagged fracture lines for damaged variants."""
    rng = random.Random(seed)
    for _ in range(count):
        x, y = rng.randrange(3, t.w - 3), rng.randrange(3, t.h - 3)
        dirx, diry = rng.choice([(1, 1), (1, -1), (-1, 1), (1, 0), (0, 1)])
        for _ in range(rng.randrange(4, 9)):
            t.set(x, y, ramp[0])
            t.blend(x + 1, y, ramp[6], 0.35)
            if rng.random() < 0.4:
                dirx, diry = rng.choice([(1, 1), (1, -1), (-1, 1), (1, 0), (0, 1)])
            x += dirx
            y += diry
            if not (1 <= x < t.w - 1 and 1 <= y < t.h - 1):
                break


# ======================================================================================
# ores
# ======================================================================================

def blob_pts(cx, cy, r, seed, size=16):
    """Irregular mineral cluster: a union of jittered discs, torus-wrapped."""
    rng = random.Random(seed)
    lobes = [(cx, cy, r)]
    for i in range(3):
        a = rng.uniform(0, math.tau)
        lobes.append((cx + math.cos(a) * r * 0.75,
                      cy + math.sin(a) * r * 0.75,
                      r * rng.uniform(0.45, 0.68)))
    pts = set()
    for y in range(size):
        for x in range(size):
            for (lx, ly, lr) in lobes:
                dx = abs(x + 0.5 - lx)
                dy = abs(y + 0.5 - ly)
                dx = min(dx, size - dx)
                dy = min(dy, size - dy)
                if math.hypot(dx, dy) <= lr:
                    pts.add((x, y))
                    break
    return pts


def ore(seed, host, ore_ramp, blobs, glow_color=None, size=16):
    """Stone host + shaded mineral clusters, vanilla-style."""
    t = Tex(size, size, seed)
    for y in range(size):
        for x in range(size):
            n = fbm(x, y, seed + 7, size, size, 3, 5.0)
            m = fbm(x, y, seed + 31, size, size, 2, 9.0)
            t.set(x, y, host[int(round(1.0 + n * 3.6 + (m - 0.5) * 1.2))])

    rng = random.Random(seed)
    for i, (cx, cy, r) in enumerate(blobs):
        pts = blob_pts(cx, cy, r, seed + i * 13, size)
        for (x, y) in sorted(pts):
            up = ((x, (y - 1) % size) not in pts)
            down = ((x, (y + 1) % size) not in pts)
            left = (((x - 1) % size, y) not in pts)
            right = (((x + 1) % size, y) not in pts)
            v = 3.5
            v += 1.3 if up else 0
            v += 0.5 if left else 0
            v -= 1.4 if down else 0
            v -= 0.7 if right else 0
            v += (fbm(x, y, seed + 61, size, size, 2, 6.0) - 0.5) * 1.4
            t.set(x, y, ore_ramp[int(round(max(1, min(6, v))))])
        # dark contact edge against the stone
        for (x, y) in sorted(pts):
            for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                nx, ny = (x + dx) % size, (y + dy) % size
                if (nx, ny) not in pts:
                    t.blend(nx, ny, ore_ramp[0], 0.45)
        # specular sparkle
        top = min(pts, key=lambda p: (p[1], p[0]))
        t.set(top[0] + 1, top[1] + 1, ore_ramp[6])
        if glow_color:
            glow(t, cx + 0.5, cy + 0.5, r + 2.0, glow_color, 0.10)
    return t

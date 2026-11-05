"""Textures of the explosion effects: the mushroom cloud's sprite atlas, its far impostor, the heat-haze noise, the flash's
afterimage and the particles every blast throws out.

This module is imported at the end of ``blocks.py``; ``generate.py`` writes ``FX`` into ``textures/fx/`` (sampled directly by
the explosion shaders, not stitched into an atlas). Everything is greyscale or close to white: the shaders and the particles tint
it (fire by temperature, smoke by age, dust and debris by the terrain), so nothing here carries a colour of its own. Alpha is
straight and every sprite fades to fully clear well inside its tile, so neighbouring tiles never bleed into one another.

The cloud sprites are built from overlapping spheres ("lobes"): the front-most sphere at a pixel gives its normal, lit from above
and a little from the left, and billowy noise eats into the outline so it reads as cauliflower turrets, not discs.
"""

import math
import random

from particles import PARTICLES
from pixelkit import Tex

FX = {}

# Lit from above, a little from the left, towards the viewer (image space: x right, y down, z out of the screen).
_L = (-0.30, -0.80, 0.52)
_LN = math.sqrt(sum(c * c for c in _L))
LIGHT = tuple(c / _LN for c in _L)


def fx(name):
    def deco(fn):
        FX[name] = fn
        return fn
    return deco


def _clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v


def _smooth(e0, e1, v):
    u = _clamp((v - e0) / (e1 - e0))
    return u * u * (3.0 - 2.0 * u)


class Perlin:
    """Deterministic 2D gradient noise. With ``period`` (in lattice cells) it tiles."""

    def __init__(self, seed, period=0):
        rng = random.Random(seed)
        p = list(range(256))
        for i in range(255, 0, -1):
            j = int(rng.random() * (i + 1))
            p[i], p[j] = p[j], p[i]
        self.p = p + p
        self.g = [(math.cos(2 * math.pi * k / 16.0), math.sin(2 * math.pi * k / 16.0)) for k in range(16)]
        self.period = period

    def noise(self, x, y, period=0):
        """About -1..1."""
        xi, yi = math.floor(x), math.floor(y)
        xf, yf = x - xi, y - yi
        if period:
            x0, y0 = xi % period, yi % period
            x1, y1 = (x0 + 1) % period, (y0 + 1) % period
        else:
            x0, y0 = xi & 255, yi & 255
            x1, y1 = (x0 + 1) & 255, (y0 + 1) & 255
        p, g = self.p, self.g
        a, b = p[x0], p[x1]
        g00 = g[p[a + y0] & 15]
        g10 = g[p[b + y0] & 15]
        g01 = g[p[a + y1] & 15]
        g11 = g[p[b + y1] & 15]
        n00 = g00[0] * xf + g00[1] * yf
        n10 = g10[0] * (xf - 1) + g10[1] * yf
        n01 = g01[0] * xf + g01[1] * (yf - 1)
        n11 = g11[0] * (xf - 1) + g11[1] * (yf - 1)
        u = xf * xf * xf * (xf * (xf * 6 - 15) + 10)
        v = yf * yf * yf * (yf * (yf * 6 - 15) + 10)
        nx0 = n00 + u * (n10 - n00)
        nx1 = n01 + u * (n11 - n01)
        return 1.41 * (nx0 + v * (nx1 - nx0))

    def fbm(self, x, y, octaves=4, gain=0.5):
        total, amp, norm, f = 0.0, 1.0, 0.0, 1.0
        for o in range(octaves):
            period = self.period * int(f) if self.period else 0
            total += amp * self.noise(x * f + o * 17.3, y * f + o * 31.7, period)
            norm += amp
            amp *= gain
            f *= 2.0
        return total / norm

    def billow(self, x, y, octaves=4, gain=0.5):
        """Folded noise, 0..1: rounded humps with sharp creases between them, the look of cauliflower turrets."""
        total, amp, norm, f = 0.0, 1.0, 0.0, 1.0
        for o in range(octaves):
            total += amp * abs(self.noise(x * f + o * 17.3, y * f + o * 31.7))
            norm += amp
            amp *= gain
            f *= 2.0
        return total / norm


def _window(x, y, w, h, margin):
    """1 inside, 0 within ``margin`` pixels of the tile's border, a 6 px ramp between: nothing can reach the edge."""
    return _clamp((min(x, y, w - 1 - x, h - 1 - y) - margin) / 6.0)


# ======================================================================================
# the lobe renderer: overlapping lit spheres with a noisy, billowy outline
# ======================================================================================

def render_lobes(t, ox, oy, w, h, lobes, seed, edge_amp=0.35, edge_freq=0.045, soft=0.25, body=1.0,
                 lum_lo=0.55, lum_hi=1.0, grain=0.0, grain_freq=0.25, bump=0.45, ao=0.18,
                 margin=5, cutoff=1.0 / 255.0, grey=(1.0, 1.0, 1.0), vgrad=0.12, melt=5.0):
    """Draws ``lobes`` (``(cx, cy, r, z)`` in tile pixels) into the ``w`` x ``h`` tile at ``ox, oy``.

    ``edge_amp`` is how far billowy noise eats into or swells the outline (in lobe radii) and ``soft`` how long the alpha fade
    is; ``body`` the opacity of the middle. Brightness runs from ``lum_lo`` (shadowed underside) to ``lum_hi`` (lit top);
    ``grain`` adds fine speckle, ``bump`` perturbs the normals with noise so turrets show inside the outline, ``ao`` darkens
    the creases where two lobes meet, ``melt`` (pixels of depth) how softly they merge. ``grey`` scales the RGB channels
    (1 = white)."""
    edge = Perlin(seed)
    bumpx = Perlin(seed + 1)
    bumpy = Perlin(seed + 2)
    fine = Perlin(seed + 3)
    lx, ly, lz = LIGHT
    for y in range(h):
        for x in range(w):
            win = _window(x, y, w, h, margin)
            if win <= 0.0:
                continue
            px, py = x + 0.5, y + 0.5
            e = -9.0
            near = None
            covers = []
            zmax = -1e9
            for lobe in lobes:
                cx, cy, r, zc = lobe
                dx, dy = px - cx, py - cy
                d2 = dx * dx + dy * dy
                el = 1.0 - math.sqrt(d2) / r
                if el > e:
                    e = el
                    near = lobe
                if d2 < r * r:
                    z = zc + math.sqrt(r * r - d2)
                    covers.append((z, lobe))
                    if z > zmax:
                        zmax = z
            n = edge.billow(px * edge_freq, py * edge_freq, 5)
            en = e + (n - 0.42) * edge_amp
            if en <= 0.0:
                continue
            a = _smooth(0.0, soft, en) * body * win
            if a < cutoff:
                continue
            crease = 0.0
            if covers:
                # A soft union of the spheres: each covering lobe's normal weighted by how near the front it is, so the
                # seams where turrets meet read as soft folds, not cuts.
                nx = ny = nz = wsum = wtop = 0.0
                for z, lobe in covers:
                    wgt = math.exp((z - zmax) / melt)
                    mx, my, mz = _sphere_normal(lobe, px, py)
                    nx += mx * wgt
                    ny += my * wgt
                    nz += mz * wgt
                    wsum += wgt
                    wtop = max(wtop, wgt)
                crease = _clamp(2.0 * (1.0 - wtop / wsum))
            else:
                nx, ny, nz = _sphere_normal(near, px, py)
            if bump:
                f = edge_freq * 2.2
                nx += bumpx.fbm(px * f, py * f, 4) * bump
                ny += bumpy.fbm(px * f, py * f, 4) * bump
                # The billow's creases shade like folds between turrets.
                nz += (n - 0.4) * bump
            k = 1.0 / (math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0)
            nx, ny, nz = nx * k, ny * k, nz * k
            d = nx * lx + ny * ly + nz * lz
            lit = _clamp((d + 0.35) / 1.35)
            # The top of the tile catches more sky, the underside less.
            lit *= 1.0 - vgrad + vgrad * 2.0 * _clamp(1.0 - py / h)
            if ao:
                lit *= 1.0 - ao * crease
            if grain:
                lit += fine.fbm(px * grain_freq, py * grain_freq, 2) * grain
            lum = lum_lo + (lum_hi - lum_lo) * _clamp(lit)
            lum = _clamp(lum)
            t.set(ox + x, oy + y, (int(255 * lum * grey[0]), int(255 * lum * grey[1]), int(255 * lum * grey[2]),
                                   int(round(255 * _clamp(a)))))


def _sphere_normal(lobe, px, py):
    cx, cy, r, _ = lobe
    nx, ny = (px - cx) / r, (py - cy) / r
    nz2 = 1.0 - nx * nx - ny * ny
    if nz2 < 0.0:
        k = 1.0 / math.sqrt(nx * nx + ny * ny)
        return nx * k, ny * k, 0.0
    return nx, ny, math.sqrt(nz2)


def _cauliflower(rng, cx, cy, r, count, reach=52.0, spread=(0.45, 0.9), small=(0.42, 0.7), upper=0.8):
    """A main lobe and ``count`` turrets clustered over its upper half (``upper`` of them; the rest round its sides),
    every lobe kept within ``reach`` of the tile's centre."""
    lobes = [(cx, cy, r, 0.0)]
    for i in range(count):
        if rng.random() < upper:
            a = math.pi * (1.08 + 0.84 * rng.random())
        else:
            a = (rng.random() - 0.5) * 0.9 + (0.0 if rng.random() < 0.5 else math.pi)
        dist = r * (spread[0] + (spread[1] - spread[0]) * rng.random())
        lr = r * (small[0] + (small[1] - small[0]) * rng.random())
        lx, ly = cx + math.cos(a) * dist, cy + math.sin(a) * dist
        # Keep the lobe, and the noise that swells its outline, inside the reach.
        out = math.hypot(lx - 64.0, ly - 64.0) + lr * 1.15
        if out > reach:
            lr *= reach / out
        lobes.append((lx, ly, lr, (rng.random() - 0.3) * r * 0.35))
    return lobes


# ======================================================================================
# the cloud atlas: 4x4 tiles of 128 px, index row * 4 + col
# ======================================================================================

TILE = 128


def _soft_puff(t, ox, oy, seed):
    rng = random.Random(seed)
    lobes = _cauliflower(rng, 64.0 + rng.uniform(-3, 3), 66.0 + rng.uniform(-2, 3), 39.0, 11 + int(rng.random() * 3),
                         reach=57.0, spread=(0.5, 0.95), upper=0.65)
    render_lobes(t, ox, oy, TILE, TILE, lobes, seed, edge_amp=0.46, edge_freq=0.04, soft=0.42, body=0.9,
                 lum_lo=0.5, lum_hi=1.0, bump=0.42, ao=0.12, vgrad=0.16)


def _dense_puff(t, ox, oy, seed):
    rng = random.Random(seed)
    lobes = _cauliflower(rng, 64.0 + rng.uniform(-3, 3), 65.0 + rng.uniform(-2, 3), 37.0, 18 + int(rng.random() * 4),
                         reach=56.0, spread=(0.4, 1.0), small=(0.3, 0.55), upper=0.6)
    render_lobes(t, ox, oy, TILE, TILE, lobes, seed, edge_amp=0.34, edge_freq=0.06, soft=0.16, body=1.0,
                 lum_lo=0.42, lum_hi=0.95, grain=0.09, grain_freq=0.45, bump=0.55, ao=0.2, vgrad=0.16)


def _fire(t, ox, oy, seed, tongues):
    """Turbulent fire, greyscale intensity: a hot, bright core broken by darker lanes of cooler gas, and (with ``tongues``)
    flames licking up from it. The shader tints it by intensity and draws it emissive."""
    rng = random.Random(seed)
    warp_a = Perlin(seed)
    warp_b = Perlin(seed + 1)
    turb = Perlin(seed + 2)
    lick = Perlin(seed + 3)
    cx = 64.0 + rng.uniform(-3, 3)
    cy = (74.0 if tongues else 64.0) + rng.uniform(-3, 3)
    core_r = 33.0 if tongues else 44.0
    for y in range(TILE):
        for x in range(TILE):
            win = _window(x, y, TILE, TILE, 5)
            if win <= 0.0:
                continue
            px, py = x + 0.5, y + 0.5
            # Domain warp: the gas swirls.
            wx = warp_a.fbm(px * 0.03, py * 0.03, 3) * 14.0
            wy = warp_b.fbm(px * 0.03, py * 0.03, 3) * 14.0
            qx, qy = px + wx, py + wy
            dx, dy = (qx - cx) / core_r, (qy - cy) / core_r
            if tongues and dy < 0:
                # Flames stretch upward from the core: above it the falloff is slower, cut into tongues by vertical
                # ridges that waver with height.
                ridge = 1.0 - abs(lick.fbm(qx * 0.045, qy * 0.016 + seed, 3))
                dy *= 0.5 + 0.45 * (1.0 - ridge ** 2)
            r = math.sqrt(dx * dx + dy * dy)
            base = 1.0 - r
            n = turb.fbm(qx * 0.055, qy * 0.055, 5)
            v = base + n * 0.38
            if v <= 0.0:
                continue
            a = _smooth(0.0, 0.3, v) * win
            if a < 1.0 / 255.0:
                continue
            # Hottest in the middle; cooler, sooty lanes where the noise dips.
            heat = _clamp(0.02 + 1.15 * v + 0.6 * n)
            lum = _clamp(0.2 + 0.8 * heat ** 1.25)
            c = int(255 * lum)
            t.set(ox + x, oy + y, (c, c, c, int(round(255 * a))))


def _veil(t, ox, oy, seed):
    """A condensation veil: a thin, translucent, streaky sheet of fog, wider than tall."""
    streak = Perlin(seed)
    shape = Perlin(seed + 1)
    for y in range(TILE):
        for x in range(TILE):
            win = _window(x, y, TILE, TILE, 5)
            if win <= 0.0:
                continue
            px, py = x + 0.5, y + 0.5
            ex = (px - 64.0) / 56.0
            ey = (py - 64.0 + 6.0 * shape.fbm(px * 0.03, 0.5, 2)) / 30.0
            base = 1.0 - (ex * ex + ey * ey)
            base += shape.fbm(px * 0.04, py * 0.04, 3) * 0.3
            if base <= 0.0:
                continue
            # Streaks drawn out sideways and bent by the swirl, broken into patches.
            sy = py + shape.fbm(px * 0.02 + 5.0, py * 0.02, 3) * 14.0
            s = 0.5 + 0.5 * streak.fbm(px * 0.014, sy * 0.085, 4)
            s *= 0.55 + 0.45 * (0.5 + 0.5 * streak.fbm(px * 0.035 + 9.0, py * 0.035, 3))
            a = _smooth(0.0, 0.55, base) * (0.18 + 0.62 * s) * 0.75 * win
            if a < 1.0 / 255.0:
                continue
            lum = 0.86 + 0.14 * _clamp(1.0 - (py - 34.0) / 60.0) + (s - 0.5) * 0.06
            c = int(255 * _clamp(lum))
            t.set(ox + x, oy + y, (c, c, c, int(round(255 * a))))


def _pileus(t, ox, oy, seed):
    """A pileus: the smooth, thin cap of cloud that forms over a rising column. A hood-shaped sheet, convex upward, thickest
    over the middle and thinning to nothing at its ends, its surface silky with faint striations along the curve."""
    silk = Perlin(seed)
    for y in range(TILE):
        for x in range(TILE):
            win = _window(x, y, TILE, TILE, 5)
            if win <= 0.0:
                continue
            px, py = x + 0.5, y + 0.5
            u = (px - 64.0) / 57.0
            if abs(u) >= 1.0:
                continue
            arch = math.sqrt(1.0 - u * u)
            top = 84.0 - 50.0 * arch
            thick = 5.0 + 24.0 * arch ** 1.5
            s = (py - top) / thick
            if s <= 0.0 or s >= 1.0:
                continue
            profile = math.sin(math.pi * s) ** 1.1
            a = profile * (0.35 + 0.45 * arch) * _smooth(0.0, 0.25, 1.0 - abs(u)) * win
            stri = silk.fbm(u * 3.0, s * 9.0 + u * 2.0, 3)
            a *= 0.9 + 0.1 * stri
            if a < 1.0 / 255.0:
                continue
            # Its upper face catches the light; the underside is a touch greyer.
            lum = 1.0 - 0.16 * s + 0.03 * stri
            c = int(255 * _clamp(lum))
            t.set(ox + x, oy + y, (c, c, c, int(round(255 * _clamp(a)))))


def _shock_ring(t, ox, oy, seed):
    """A radial gradient for the shock's condensation ring and its dust ring: a soft annulus, steeper on its outer edge
    (the front) than on its inner one, clear in the middle."""
    ripple = Perlin(seed)
    for y in range(TILE):
        for x in range(TILE):
            win = _window(x, y, TILE, TILE, 5)
            if win <= 0.0:
                continue
            px, py = x + 0.5, y + 0.5
            r = math.hypot(px - 64.0, py - 64.0)
            ang = math.atan2(py - 64.0, px - 64.0)
            r += ripple.fbm(math.cos(ang) * 2.0 + 3.0, math.sin(ang) * 2.0 + 3.0, 3) * 1.6
            d = r - 44.0
            w = 5.0 if d > 0 else 11.0
            a = math.exp(-(d / w) ** 2) * 0.9
            a *= 0.88 + 0.12 * (0.5 + 0.5 * ripple.fbm(px * 0.08, py * 0.08, 3))
            a *= _smooth(58.0, 54.0, r) * win
            if a < 1.0 / 255.0:
                continue
            c = int(255 * (0.93 + 0.07 * _clamp(1.0 - py / 128.0)))
            t.set(ox + x, oy + y, (c, c, c, int(round(255 * _clamp(a)))))


def _bolt(rng, x0, y0, x1, y1, rough, depth):
    """A jagged path by midpoint displacement, as a list of points."""
    pts = [(x0, y0), (x1, y1)]
    for _ in range(depth):
        nxt = [pts[0]]
        for (ax, ay), (bx, by) in zip(pts, pts[1:]):
            length = math.hypot(bx - ax, by - ay)
            mx = (ax + bx) / 2 + (rng.random() - 0.5) * rough * length
            my = (ay + by) / 2 + (rng.random() - 0.5) * rough * length * 0.3
            nxt += [(mx, my), (bx, by)]
        pts = nxt
    return pts


def _lightning(t, ox, oy, seed):
    """A lightning streak: a thin, white, branching bolt on clear, its core sharp and a faint glow round it. Runs top to
    bottom of the tile, fading at its ends so the quad's edges never show."""
    rng = random.Random(seed)
    strokes = []
    main = _bolt(rng, 62.0 + rng.uniform(-4, 4), 10.0, 66.0 + rng.uniform(-8, 8), 118.0, 0.55, 6)
    strokes.append((main, 1.0, 1.0))
    for _ in range(4):
        i = 8 + int(rng.random() * (len(main) - 24))
        sx, sy = main[i]
        side = -1.0 if rng.random() < 0.5 else 1.0
        length = 18.0 + rng.random() * 26.0
        ex = sx + side * length * (0.5 + 0.4 * rng.random())
        ey = min(116.0, sy + length * (0.6 + 0.5 * rng.random()))
        ex = max(12.0, min(116.0, ex))
        strokes.append((_bolt(rng, sx, sy, ex, ey, 0.6, 4), 0.55, 0.65))
        if rng.random() < 0.6:
            b = _bolt(rng, sx, sy, ex, ey, 0.6, 4)
            j = len(b) // 2
            fx_, fy_ = b[j]
            strokes.append((_bolt(rng, fx_, fy_, max(12.0, min(116.0, fx_ + side * 12.0)), min(116.0, fy_ + 14.0), 0.7, 3),
                            0.35, 0.45))
    field = [[0.0] * TILE for _ in range(TILE)]
    for pts, width, strength in strokes:
        for (ax, ay), (bx, by) in zip(pts, pts[1:]):
            x_lo, x_hi = int(min(ax, bx)) - 6, int(max(ax, bx)) + 7
            y_lo, y_hi = int(min(ay, by)) - 6, int(max(ay, by)) + 7
            vx, vy = bx - ax, by - ay
            ll = vx * vx + vy * vy or 1e-9
            for y in range(max(0, y_lo), min(TILE, y_hi)):
                for x in range(max(0, x_lo), min(TILE, x_hi)):
                    px, py = x + 0.5, y + 0.5
                    u = _clamp(((px - ax) * vx + (py - ay) * vy) / ll)
                    d = math.hypot(px - (ax + u * vx), py - (ay + u * vy))
                    core = math.exp(-(d / (0.7 * width)) ** 2)
                    halo = 0.28 * math.exp(-(d / (3.2 * width)) ** 2)
                    v = strength * (core + halo)
                    if v > field[y][x]:
                        field[y][x] = v
    for y in range(TILE):
        fade = _smooth(6.0, 20.0, y) * _smooth(122.0, 108.0, y)
        for x in range(TILE):
            v = field[y][x] * fade * _window(x, y, TILE, TILE, 5)
            if v < 1.0 / 255.0:
                continue
            c = int(255 * _clamp(0.78 + 0.22 * v))
            t.set(ox + x, oy + y, (c, c, c, int(round(255 * _clamp(v)))))


@fx("nuke_cloud")
def nuke_cloud():
    t = Tex(4 * TILE, 4 * TILE, 3100)
    for col in range(4):
        _soft_puff(t, col * TILE, 0, 3110 + col)
        _dense_puff(t, col * TILE, TILE, 3120 + col)
        _fire(t, col * TILE, 2 * TILE, 3130 + col, tongues=col > 0)
    _veil(t, 0, 3 * TILE, 3140)
    _pileus(t, TILE, 3 * TILE, 3141)
    _shock_ring(t, 2 * TILE, 3 * TILE, 3142)
    _lightning(t, 3 * TILE, 3 * TILE, 3143)
    return t


# ======================================================================================
# the far impostor: the cap and the stem as two silhouettes
# ======================================================================================

@fx("nuke_impostor")
def nuke_impostor():
    """The mushroom drawn far off as two billboards: the cap (left tile) and the stem (right), each 256 px, lit from above."""
    size = 256
    t = Tex(2 * size, size, 3200)
    rng = random.Random(3200)
    # The cap: a broad, flattened dome packed with turrets, rolling over at its rim into a shaded, lumpy underside that
    # draws in towards the stem.
    cap = []
    while len(cap) < 46:
        x = 128.0 + (rng.random() * 2.0 - 1.0) * 100.0
        y = 52.0 + rng.random() * 112.0
        u, v = (x - 128.0) / 104.0, (y - 126.0) / 72.0
        if u * u + v * v > 1.0 or (y > 128.0 and abs(u) < 0.3 * (y - 128.0) / 36.0):
            continue
        dome = math.sqrt(max(0.0, 1.0 - u * u - min(0.0, v) ** 2))
        r = 18.0 + rng.random() * 14.0
        cap.append((x, y, r, 40.0 * dome - (14.0 if y > 140.0 else 0.0)))
    # Turrets along the dome's crown.
    for i in range(13):
        ang = math.pi * (1.06 + 0.88 * i / 12.0)
        x, y = 128.0 + math.cos(ang) * 96.0, 128.0 + math.sin(ang) * 68.0
        cap.append((x + rng.uniform(-4, 4), y + rng.uniform(-3, 5), 17.0 + rng.uniform(-3, 5), 22.0 + rng.uniform(-4, 6)))
    # The rolled-under rim either side and the underside, set back in shade.
    for i in range(12):
        uu = -1.0 + 2.0 * i / 11.0
        cap.append((128.0 + uu * 96.0, 160.0 + 10.0 * abs(uu) ** 3 - 6.0 * (1.0 - uu * uu) + rng.uniform(-3, 3),
                    17.0 + rng.uniform(-3, 4), -10.0 + rng.uniform(-4, 4)))
    render_lobes(t, 0, 0, size, size, cap, 3201, edge_amp=0.34, edge_freq=0.035, soft=0.24, body=1.0,
                 lum_lo=0.38, lum_hi=1.0, bump=0.45, ao=0.2, margin=5, vgrad=0.22, melt=7.0)
    # The underside is out of the sun: shade the lower part of the cap.
    for yy in range(140, size):
        k = 1.0 - 0.3 * _smooth(140.0, 190.0, yy)
        for xx in range(size):
            r, g, b, a = t.px[xx, yy]
            if a:
                t.px[xx, yy] = (int(r * k), int(g * k), int(b * k), a)
    # The stem: a column of rising, overlapping lobes, narrowest in the middle, flaring at its foot (the dust skirt) and
    # where it spreads into the cap, faded out top and bottom so it blends into both.
    stem = []
    y = 238.0
    while y > 12.0:
        u = (238.0 - y) / 226.0
        half = 20.0 + 42.0 * (1.0 - u) ** 7 + 30.0 * u ** 5
        across = 1 + int(half / 18.0)
        for j in range(across):
            off = 0.0 if across == 1 else (j / (across - 1) * 2.0 - 1.0) * (half - 12.0) * 0.8
            r = max(12.0, min(half, 26.0)) * rng.uniform(0.9, 1.15)
            stem.append((128.0 + off + rng.uniform(-3, 3), y + rng.uniform(-3, 3), r,
                         math.sqrt(max(0.0, 1.0 - (off / max(1.0, half)) ** 2)) * half * 0.6))
        y -= 9.0
    stem_tile = Tex(size, size, 3202)
    render_lobes(stem_tile, 0, 0, size, size, stem, 3203, edge_amp=0.28, edge_freq=0.045, soft=0.22, body=1.0,
                 lum_lo=0.42, lum_hi=0.94, bump=0.4, ao=0.14, margin=5, vgrad=0.1, melt=6.0)
    px = stem_tile.px
    streak = Perlin(3204)
    for yy in range(size):
        fade = _smooth(6.0, 44.0, yy) * _smooth(250.0, 218.0, yy)
        for xx in range(size):
            r, g, b, a = px[xx, yy]
            if a:
                # Faint vertical striations of the rising column.
                k = 1.0 + 0.06 * streak.fbm(xx * 0.09, yy * 0.012, 3)
                t.set(size + xx, yy, (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), int(a * fade)))
    return t


# ======================================================================================
# heat haze and the flash's afterimage
# ======================================================================================

@fx("heat_haze_noise")
def heat_haze_noise():
    """Two independent, smooth, tileable noise fields in R and G for the heat haze's refraction offsets."""
    size = 128
    t = Tex(size, size, 3300)
    red, green = Perlin(3301, period=8), Perlin(3302, period=8)
    for y in range(size):
        for x in range(size):
            u, v = x * 8.0 / size, y * 8.0 / size
            r = _clamp(0.5 + 0.5 * red.fbm(u, v, 3, gain=0.45) * 1.25)
            g = _clamp(0.5 + 0.5 * green.fbm(u, v, 3, gain=0.45) * 1.25)
            t.set(x, y, (int(round(255 * r)), int(round(255 * g)), 0, 255))
    return t


@fx("flash_afterimage")
def flash_afterimage():
    """The blob a fireball burns into the eye: a soft, slightly irregular round spot, white in the middle."""
    size = 64
    t = Tex(size, size, 3400)
    wob = Perlin(3401)
    for y in range(size):
        for x in range(size):
            px, py = x + 0.5, y + 0.5
            dx, dy = px - 32.0, py - 32.0
            ang = math.atan2(dy, dx)
            r = math.hypot(dx, dy) / (17.0 * (1.0 + 0.1 * wob.fbm(math.cos(ang) * 1.5 + 2.0, math.sin(ang) * 1.5 + 2.0, 3)))
            a = math.exp(-r * r * 1.1) * _smooth(29.0, 22.0, math.hypot(dx, dy)) * _window(x, y, size, size, 1)
            if a < 1.0 / 255.0:
                continue
            c = int(255 * (0.82 + 0.18 * math.exp(-r * r * 2.0)))
            t.set(x, y, (c, c, c, int(round(255 * a))))
    return t


# ======================================================================================
# particles
# ======================================================================================

def _particle_puff(seed, size, count, body, soft, lum_lo, lum_hi, grain=0.0, edge_amp=0.4, grey=(1.0, 1.0, 1.0), upper=0.7):
    """A puff sprite for a particle. The particle shader drops fragments under 10% alpha, so the fade is short and the outline
    comes from the noise, not from a long faint tail."""
    def make():
        t = Tex(size, size, seed)
        rng = random.Random(seed)
        k = size / 128.0
        lobes = [(cx * k, cy * k, r * k, z * k) for cx, cy, r, z in
                 _cauliflower(rng, 64.0, 66.0, 36.0, count, reach=60.0, upper=upper)]
        render_lobes(t, 0, 0, size, size, lobes, seed, edge_amp=edge_amp, edge_freq=0.05 / k, soft=soft, body=body,
                     lum_lo=lum_lo, lum_hi=lum_hi, grain=grain, grain_freq=0.4 / k, bump=0.45, ao=0.18,
                     margin=1, cutoff=0.11, grey=grey)
        return t
    return make


def _flame(seed, size):
    """A tongue of flame, warm white (the game colours it as it cools): a rounded base licking up into a ragged tip."""
    def make():
        t = Tex(size, size, seed)
        warp = Perlin(seed)
        turb = Perlin(seed + 1)
        rng = random.Random(seed)
        cx = size * (0.5 + rng.uniform(-0.05, 0.05))
        cy = size * 0.66
        rad = size * 0.28
        for y in range(size):
            for x in range(size):
                px, py = x + 0.5, y + 0.5
                wx = warp.fbm(px / size * 3.0, py / size * 3.0, 3) * size * 0.12
                qx, qy = px + wx, py
                dx, dy = (qx - cx) / rad, (qy - cy) / rad
                if dy < 0:
                    dy *= 0.45
                    dx *= 1.0 + (-dy) * 0.9
                v = 1.0 - math.hypot(dx, dy) + turb.fbm(qx / size * 5.0, qy / size * 3.0, 4) * 0.4
                v *= _window(x, y, size, size, 0) if size > 8 else 1.0
                if v <= 0.0:
                    continue
                a = _smooth(0.0, 0.25, v)
                if a < 0.11:
                    continue
                heat = _clamp(v * 1.6)
                t.set(x, y, (255, int(205 + 50 * heat), int(150 + 95 * heat ** 1.4), int(round(255 * a))))
        return t
    return make


def _spark(seed, length):
    """A hot fragment of casing: a white-hot fleck with a short streak, warm white so the game can cool it to red."""
    def make():
        t = Tex(8, 8, seed)
        for y in range(8):
            for x in range(8):
                px, py = x + 0.5 - 4.0, y + 0.5 - 4.0
                # Along the diagonal: the streak.
                u = (px + py) / math.sqrt(2.0)
                v = (px - py) / math.sqrt(2.0)
                d = math.hypot(u / length, v / 1.0)
                if d >= 1.6:
                    continue
                a = _clamp(1.4 - d)
                if a < 0.12:
                    continue
                hot = _clamp(1.0 - d)
                t.set(x, y, (255, int(215 + 40 * hot), int(160 + 95 * hot), int(round(255 * _clamp(a * 1.2)))))
        return t
    return make


def _ash(seed):
    """A flake of ash: a small, irregular, flat flake, light grey with a darker charred edge or core."""
    def make():
        t = Tex(8, 8, seed)
        rng = random.Random(seed)
        n = 5 + int(rng.random() * 3)
        rad = 1.8 + rng.random() * 1.6
        cx, cy = 3.8 + rng.uniform(-0.4, 0.4), 3.8 + rng.uniform(-0.4, 0.4)
        poly = []
        for i in range(n):
            a = 2 * math.pi * (i + rng.uniform(-0.25, 0.25)) / n
            r = rad * rng.uniform(0.55, 1.0)
            poly.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
        light = rng.uniform(0.66, 0.86)
        for y in range(8):
            for x in range(8):
                if not _inside(poly, x + 0.5, y + 0.5):
                    continue
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / rad
                # Paler where it burned through; the edge curls and catches the light at the top.
                v = light - 0.18 * d + (0.08 if y + 0.5 < cy else -0.05) + rng.uniform(-0.05, 0.05)
                c = int(255 * _clamp(v))
                t.set(x, y, (c, c, c, 255))
        return t
    return make


def _inside(poly, x, y):
    inside = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def _rain(seed, length):
    """A drop of black rain: a short, sooty streak, faint at its tail and gathering into a dark, rounded drop at its foot."""
    def make():
        t = Tex(8, 8, seed)
        x = 3 + (seed & 1)
        top = 6 - length
        for y in range(top, 6):
            u = (y - top + 1) / float(length)
            c = int(255 * (0.3 - 0.08 * u))
            t.set(x, y, (c, c, c, int(255 * (0.25 + 0.6 * u))))
        # The drop at its foot, a glint of light on its top, with a faint shoulder either side.
        t.set(x, 6, (84, 84, 84, 255))
        t.set(x, 7, (34, 34, 34, 255))
        t.set(x - 1, 7, (44, 44, 44, 120))
        t.set(x + 1, 7, (40, 40, 40, 120))
        return t
    return make


def _chunk(seed, size, rad):
    """A chunk of the ground: an angular, faceted lump, near white so the game tints it by the block it came from. Each
    facet is lit by which way it faces."""
    def make():
        t = Tex(size, size, seed)
        rng = random.Random(seed)
        n = 6 + int(rng.random() * 3)
        c = size / 2.0
        poly = []
        for i in range(n):
            a = 2 * math.pi * (i + rng.uniform(-0.3, 0.3)) / n
            r = rad * rng.uniform(0.6, 1.0)
            poly.append((c + math.cos(a) * r, c + math.sin(a) * r))
        # A ridge point off centre, towards the light: the facets fan out from it.
        tipx, tipy = c - rad * 0.25 + rng.uniform(-0.6, 0.6), c - rad * 0.25 + rng.uniform(-0.6, 0.6)
        facets = []
        for i in range(n):
            ax, ay = poly[i]
            bx, by = poly[(i + 1) % n]
            mx, my = (ax + bx) / 2 - tipx, (ay + by) / 2 - tipy
            ml = math.hypot(mx, my) or 1.0
            facing = -(mx / ml) * 0.45 - (my / ml) * 0.9
            facets.append(((tipx, tipy), (ax, ay), (bx, by), 0.78 + 0.2 * facing))
        for y in range(size):
            for x in range(size):
                px, py = x + 0.5, y + 0.5
                if not _inside(poly, px, py):
                    continue
                v = 0.7
                for tri in facets:
                    if _inside(tri[:3], px, py):
                        v = tri[3]
                        break
                v += rng.uniform(-0.04, 0.04)
                cc = int(255 * _clamp(v, 0.45, 0.98))
                t.set(x, y, (cc, cc, cc, 255))
        # A darker rim on the shadowed side for readability.
        src = t.im.copy().load()
        for y in range(size):
            for x in range(size):
                if src[x, y][3] == 0:
                    continue
                for dx, dy in ((1, 0), (0, 1)):
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < size and 0 <= ny < size) or src[nx, ny][3] == 0:
                        r, g, b, a = src[x, y]
                        k = 0.72
                        t.set(x, y, (int(r * k), int(g * k), int(b * k), a))
                        break
        return t
    return make


def _mist(seed):
    """A wisp of condensation: a soft, stretched, translucent veil of white, lumpy with noise."""
    def make():
        size = 64
        t = Tex(size, size, seed)
        shape = Perlin(seed)
        streak = Perlin(seed + 1)
        rng = random.Random(seed)
        cx, cy = 32.0 + rng.uniform(-2, 2), 32.0 + rng.uniform(-2, 2)
        for y in range(size):
            for x in range(size):
                px, py = x + 0.5, y + 0.5
                d = math.hypot((px - cx) / 29.0, (py - cy) / 19.0)
                n = shape.fbm(px * 0.06, py * 0.06, 4)
                s = 0.5 + 0.5 * streak.fbm(px * 0.03, py * 0.14, 3)
                v = 1.0 - d * d + n * 0.45
                if v <= 0.0:
                    continue
                a = _smooth(0.0, 0.4, v) * (0.45 + 0.3 * s) * _window(x, y, size, size, 0)
                if a < 0.11:
                    continue
                c = int(255 * _clamp(0.92 + 0.08 * _clamp(1.0 - py / size) + 0.03 * n))
                t.set(x, y, (c, c, c, int(round(255 * a))))
        return t
    return make


for i in range(4):
    PARTICLES["nuke_ash_%d" % i] = _ash(3500 + i)
    # Soot-laden smoke: already a mid grey, the game only shades it.
    PARTICLES["fire_smoke_%d" % i] = _particle_puff(3510 + i, 64, 6 + i, body=0.92, soft=0.3, lum_lo=0.3, lum_hi=0.62,
                                                    grain=0.04, edge_amp=0.46)
    PARTICLES["fireball_flame_%d" % i] = _flame(3520 + i, 32)
    # Near white: the game tints dust and debris by the block they came from.
    PARTICLES["blast_dust_%d" % i] = _particle_puff(3530 + i, 64, 9 + i, body=0.95, soft=0.22, lum_lo=0.7, lum_hi=1.0,
                                                    grain=0.08, edge_amp=0.42, upper=0.55)
    PARTICLES["debris_chunk_%d" % i] = _chunk(3540 + i, 16, (3.2, 4.4, 5.6, 6.8)[i])
for i in range(2):
    PARTICLES["black_rain_%d" % i] = _rain(3550 + i, (4, 6)[i])
    PARTICLES["shrapnel_spark_%d" % i] = _spark(3560 + i, (1.4, 2.6)[i])
    PARTICLES["condensation_mist_%d" % i] = _mist(3570 + i)

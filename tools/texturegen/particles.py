"""Particle sprites."""

from palette import RADIUM
from pixelkit import Tex

PARTICLES = {}


def _dust(size, seed):
    """A soft green fleck: bright core, dimmer rim. Several sizes give the dust some variety."""
    def make():
        t = Tex(8, 8, seed)
        c = 3.5 + 0.5
        t.disc(c, c, size + 0.6, RADIUM[3])
        t.disc(c, c, size, RADIUM[5])
        if size >= 1.5:
            t.disc(c, c, size - 1, RADIUM[6])
        return t
    return make


for i, size in enumerate((0.8, 1.5, 2.4)):
    PARTICLES["radioactive_dust_%d" % i] = _dust(size, 470 + i)


def _gas(seed):
    """A soft, stretched wisp of hydrogen: pale bluish white, its alpha fading smoothly to nothing well inside
    the edges (no square outline when the quad is drawn large), the noise making it lumpy. Drawn
    at 32x32 since the particle is drawn a block or two across. Almost white, so the particle's
    own colour and alpha give the haze its tint and depth."""
    def make():
        import math
        from pixelkit import fbm
        size = 32
        t = Tex(size, size, seed)
        half = size / 2.0
        cx = half + t.rng.uniform(-1.5, 1.5)
        cy = half + t.rng.uniform(-1.5, 1.5)
        for y in range(size):
            for x in range(size):
                d = math.hypot((x + 0.5 - cx) / (half - 1.5), (y + 0.5 - cy) / (half * 0.62))
                n = fbm(x, y, seed, size, size, octaves=3, scale=3.0)
                window = max(0.0, 1.0 - d * d)
                # a broad, nearly flat body that falls off near the rim: the particle shader drops
                # fragments under 10% alpha, so a long faint tail would only show as a hard edge
                a = window ** 0.6 * (0.7 + 0.3 * n)
                if a <= 0.01:
                    continue
                v = 0.92 + 0.08 * n
                t.set(x, y, (int(232 * v), int(240 * v), 255, int(255 * min(1.0, a * 1.15))))
        return t
    return make


for i in range(3):
    PARTICLES["hydrogen_gas_%d" % i] = _gas(910 + i)

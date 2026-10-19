"""Textures of the rocket effects: smoke, embers and the plume and trail sheets.

This module is imported at the end of ``blocks.py``. The plumes, trails, flashes and the far outline of a rocket are drawn by
shaders (``shaders/core/rocket_*``) and need no texture; the particles below are white or pale so the game tints them
(grey exhaust, white steam, brown dust).
"""

import math

from particles import PARTICLES
from pixelkit import Tex, fbm


def _puff(seed, lobes, body, softness):
    """A soft, lumpy puff of smoke, near white, 64x64 (they are drawn several blocks across): a few overlapping blobs whose edge noise eats into, lit a touch from
    the top left. ``body`` is how opaque its middle is; ``softness`` how far its edge fades. The particle shader drops
    fragments under 10% alpha, so the fade is kept short and the outline comes from the noise, not from a long faint tail."""
    def make():
        size = 64
        k = size / 32.0
        t = Tex(size, size, seed)
        half = size / 2.0
        blobs = []
        for i in range(lobes):
            a = t.rng.uniform(0, 2 * math.pi)
            d = t.rng.uniform(2.0, 6.0) * k if i else 0.0
            blobs.append((half + math.cos(a) * d, half + math.sin(a) * d, (t.rng.uniform(7.0, 10.0) if i else 11.0) * k))
        for y in range(size):
            for x in range(size):
                px, py = x + 0.5, y + 0.5
                # The nearest blob decides how deep inside the puff this pixel is.
                depth = max(1.0 - math.hypot(px - bx, py - by) / br for bx, by, br in blobs)
                # Never past the canvas' edge.
                depth = min(depth, 1.0 - math.hypot(px - half, py - half) / (half - 0.5))
                n = fbm(x, y, seed, size, size, octaves=5, scale=5.0)
                v = depth + (n - 0.5) * 0.75
                if v <= 0.0:
                    continue
                a = min(1.0, v / softness) * body
                if a < 0.11:
                    continue
                # Lit from the top left, shadowed bottom right.
                light = 0.86 + 0.14 * ((half - px) + (half - py)) / size + 0.08 * (n - 0.5)
                light = max(0.7, min(1.0, light))
                c = int(255 * light)
                t.set(x, y, (c, c, min(255, c + 2), int(255 * a)))
        return t
    return make


for i in range(4):
    PARTICLES["rocket_smoke_%d" % i] = _puff(1300 + i, 3, 0.85, 0.45)
    PARTICLES["rocket_smoke_dense_%d" % i] = _puff(1340 + i, 5, 1.0, 0.25)


def _ember(seed, size):
    """A hot fleck: a white-yellow core in a soft rim. The game colours it from yellow to red as it cools."""
    def make():
        t = Tex(8, 8, seed)
        c = 3.5
        for y in range(8):
            for x in range(8):
                d = math.hypot(x - c, y - c) / size
                if d >= 1.0:
                    continue
                a = (1.0 - d) ** 0.8
                if a < 0.12:
                    continue
                v = int(255 * (0.75 + 0.25 * (1.0 - d)))
                t.set(x, y, (255, v, int(v * 0.85), int(255 * min(1.0, a * 1.3))))
        return t
    return make


PARTICLES["ember_0"] = _ember(1380, 2.2)
PARTICLES["ember_1"] = _ember(1381, 3.2)

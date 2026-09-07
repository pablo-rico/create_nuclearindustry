"""Animated fluid textures.

Every fluid in the mod (steam, heavy water, deuterium, tritium, plasma steam) reuses
these two textures with a tint colour, so they are kept neutral/near-white and rely on
value contrast only.
"""

import math

from PIL import Image

from pixelkit import Tex, fbm, mix

FLUIDS = {}


def fluid(name):
    def deco(fn):
        FLUIDS[name] = fn
        return fn
    return deco


def _grey(v):
    v = max(0, min(255, int(v)))
    return (v, v, v, 255)


@fluid("steam_still")
def steam_still():
    """8 frames of slowly churning vapour; loops seamlessly."""
    frames = 8
    im = Image.new("RGBA", (16, 16 * frames))
    px = im.load()
    for f in range(frames):
        shift = f * 2
        for y in range(16):
            for x in range(16):
                a = fbm(x, (y + shift) % 16, 501, 16, 16, 3, 4.0)
                b = fbm((x + shift) % 16, (y - shift) % 16, 733, 16, 16, 2, 6.0)
                v = 168 + int((a * 0.65 + b * 0.35) * 5) * 16
                px[x, f * 16 + y] = _grey(v)
    return im, {"animation": {"frametime": 3}}


@fluid("steam_flow")
def steam_flow():
    """16 frames of downward streaks for the flowing face."""
    frames = 16
    im = Image.new("RGBA", (16, 16 * frames))
    px = im.load()
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                sy = (y - f) % 16
                streak = 0.5 + 0.5 * math.sin((x * 1.6 + sy * 0.55) * 0.9)
                n = fbm(x, sy, 907, 16, 16, 2, 5.0)
                v = 152 + int((streak * 0.55 + n * 0.45) * 5) * 18
                px[x, f * 16 + y] = _grey(v)
    return im, {"animation": {"frametime": 2}}

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

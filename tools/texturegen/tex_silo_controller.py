"""Textures of the launch controller: the rack cabinet, its panel, the generator in its plinth and its lamps.

Register block textures with ``@block``; this module is imported at the end of ``blocks.py``. Its models live in
``models/block/silo_controller/`` (the body and item, and the partials ``SiloPartials`` names: flywheel and lamps).
"""

from blocks import block  # noqa: F401
from common import bolt
from palette import DANGER, DARK, RADIUM, STEEL, WARN
from pixelkit import Tex, hazard, outline, shade

from tex_missile import _silo_concrete


@block("silo_controller_casing")
def silo_controller_casing():
    """The controller's housing: concrete set in a steel frame, like the silo's walls."""
    t = Tex(seed=937)
    _silo_concrete(t, 937)
    outline(t, 0, 0, 15, 15, DARK)
    t.hline(1, 14, 1, STEEL[4])
    t.vline(1, 1, 14, STEEL[4])
    t.hline(1, 14, 14, STEEL[1])
    t.vline(14, 1, 14, STEEL[1])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(t, x, y, STEEL)
    hazard(t, 3, 13, 12, 13, WARN[4], DARK[0])
    return t


@block("silo_controller_panel")
def silo_controller_panel():
    """The launch console set in its front: a green status screen, three lamps and a key switch."""
    t = Tex(seed=938)
    t.rect(0, 0, 15, 15, DARK[1])
    # The panel sits on pixels 2..13 x 3..12 of the face.
    t.rect(2, 3, 13, 12, DARK[2])
    outline(t, 2, 3, 13, 12, DARK[0])
    t.rect(3, 4, 12, 7, RADIUM[0])
    for x in range(4, 11, 2):
        t.set(x, 5, RADIUM[4])
    t.hline(4, 9, 6, RADIUM[3])
    for x, c in ((4, DANGER[4]), (6, WARN[4]), (8, RADIUM[4])):
        t.set(x, 9, c)
        t.set(x, 10, shade(c, 0.7))
    t.rect(10, 9, 11, 10, STEEL[4])
    t.set(11, 10, STEEL[1])
    return t

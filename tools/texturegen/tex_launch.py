"""Textures of the launch platform, the Tactical Linker and the launch controller's and command table's screens.

Placeholders from the contract commit; the art pass replaces them. Screen sheets take every coordinate from
``missile_layouts.py`` and never hard-code one.
"""

from blocks import block  # noqa: F401
from items import item, mask_from, sculpt
from items import LINKER
from palette import DANGER, DARK, STEEL, WARN
from pixelkit import Tex, glow, hazard, item_outline, outline

import tex_missile


@block("launch_base_top")
def launch_base_top():
    t = Tex(seed=930)
    tex_missile._silo_concrete(t, 930)
    hazard(t, 0, 0, 15, 1, WARN[5], DARK[0], period=4, seed=930)
    hazard(t, 0, 14, 15, 15, WARN[5], DARK[0], period=4, seed=931)
    outline(t, 0, 0, 15, 15, DARK)
    return t


@block("launch_base_side")
def launch_base_side():
    t = Tex(seed=932)
    tex_missile._silo_concrete(t, 932)
    t.hline(0, 15, 0, STEEL[4])
    outline(t, 0, 0, 15, 15, DARK)
    return t


@item("tactical_linker")
def tactical_linker():
    t = Tex(16, 16, 933)
    sculpt(t, mask_from(LINKER), DARK, base=4.0, seed=933, grain=0.5)
    t.disc(10.5, 4.5, 1.3, DANGER[5])
    glow(t, 10.5, 4.5, 4.0, DANGER[6], 0.3)
    item_outline(t, DARK[0])
    return t

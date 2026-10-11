"""Textures for the visible and audible fission reactor: the casing viewport and the fuel channels.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.
"""

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from gui import gui  # noqa: F401
from items import item  # noqa: F401
from palette import CYAN, LEAD
from pixelkit import Tex, bevel

# The viewport is a window: lead glass in a steel frame.
CUTOUT_BLOCKS["reactor_viewport"] = (16, 16)


@block("reactor_viewport")
def reactor_viewport():
    # Placeholder: a frame round a clear pane with a couple of glints.
    t = Tex(seed=901)
    bevel(t, 0, 0, 15, 15, LEAD, hi=LEAD[5], lo=LEAD[1])
    bevel(t, 1, 1, 14, 14, LEAD, hi=LEAD[4], lo=LEAD[2], corner=False)
    for i in range(4):
        t.set(4 + i, 7 - i, CYAN[5])
        t.set(8 + i, 11 - i, CYAN[4])
    return t

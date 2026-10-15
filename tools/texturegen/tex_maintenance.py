"""Textures of plant operation and maintenance (the Maintenance Kit).

Register item textures with ``@item``; this module is imported at the end of ``blocks.py``, like the
other areas' modules, so the generator picks the textures up.
"""
from items import item, rect_pts, sculpt
from palette import BRASS, DANGER, DARK, STEEL, WARN
from pixelkit import Tex, item_outline

# Safety orange for the toolbox's enamel.
SAFETY = WARN.tinted(DANGER[4], 0.45)


@item("maintenance_kit")
def maintenance_kit():
    """A safety-orange toolbox with a hinged lid, a steel spanner laid across it, brass catches and a
    dark carrying handle."""
    t = Tex(16, 16, 1730)
    body = rect_pts(1, 6, 14, 13)
    sculpt(t, body, SAFETY, base=4.0, seed=1730, grain=0.25)
    # Lid seam and the shadowed base.
    t.hline(1, 14, 8, SAFETY[2])
    t.hline(1, 14, 13, SAFETY[1])
    # The spanner across the front: an open jaw on the left, a ring on the right.
    t.hline(4, 11, 10, STEEL[5])
    t.hline(4, 11, 11, STEEL[3])
    t.rect(2, 9, 3, 12, STEEL[4])
    t.set(2, 10, SAFETY[3])
    t.set(2, 11, SAFETY[3])
    t.rect(12, 9, 13, 12, STEEL[4])
    t.set(12, 10, STEEL[1])
    t.set(13, 11, STEEL[2])
    # Catches.
    for x in (3, 12):
        t.rect(x, 7, x, 8, BRASS[5])
        t.set(x, 8, BRASS[2])
    # Handle.
    t.hline(5, 10, 3, DARK[4])
    t.vline(5, 4, 5, DARK[3])
    t.vline(10, 4, 5, DARK[2])
    t.hline(6, 9, 4, DARK[1])
    item_outline(t, SAFETY[0])
    return t

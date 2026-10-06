"""Container screen backgrounds.

Screens draw their live readouts (text, gauge fills) on top, so these textures only
provide the panel, recessed troughs and slot wells, at the exact pixel positions the
Java screens use. Keep the coordinates here in sync with the screen classes.
"""

from palette import DANGER, DARK, STEEL, WARN
from pixelkit import Tex, bevel, brushed, hazard, outline, rivet

GUIS = {}


def gui(name):
    def deco(fn):
        GUIS[name] = fn
        return fn
    return deco


def _plate(w, h, seed):
    t = Tex(w, h, seed)
    brushed(t, STEEL, base=3.4, amp=0.5, seed=seed, scale=9.0)
    bevel(t, 0, 0, w - 1, h - 1, STEEL)
    outline(t, 0, 0, w - 1, h - 1, STEEL)
    for x, y in ((3, 3), (w - 5, 3), (3, h - 5), (w - 5, h - 5)):
        rivet(t, x, y, STEEL)
    return t


def _recess(t, x0, y0, x1, y1):
    """A dark glass window sunk into the plate."""
    t.rect(x0, y0, x1, y1, DARK[1])
    t.hline(x0, x1, y0, DARK[0])
    t.vline(x0, y0, y1, DARK[0])
    t.hline(x0, x1, y1, STEEL[5])
    t.vline(x1, y0, y1, STEEL[5])


def _slot(t, x, y):
    """Vanilla-sized slot well whose 16x16 item area starts at (x, y)."""
    t.rect(x - 1, y - 1, x + 16, y + 16, DARK[2])
    t.hline(x - 1, x + 16, y - 1, DARK[0])
    t.vline(x - 1, y - 1, y + 16, DARK[0])
    t.hline(x - 1, x + 16, y + 16, STEEL[6])
    t.vline(x + 16, y - 1, y + 16, STEEL[6])


def _trough(t, x, y, w, h):
    """Gauge trough; the screen fills it from the left."""
    t.rect(x - 1, y - 1, x + w, y + h, DARK[0])
    t.rect(x, y, x + w - 1, y + h - 1, DARK[2])
    for i in range(1, 10):
        tx = x + (w * i) // 10
        t.vline(tx, y + h - 3, y + h - 1, DARK[4])


@gui("reactor_control")
def reactor_control():
    w, h = 256, 304
    t = _plate(w, h, 401)
    # header with hazard trim
    _recess(t, 8, 6, w - 9, 20)
    hazard(t, 9, 7, 30, 19, WARN[4], DARK[0], period=6, seed=401)
    hazard(t, w - 31, 7, w - 10, 19, WARN[4], DARK[0], period=6, seed=402)
    # readout window
    _recess(t, 12, 26, w - 13, 168)
    for gy in (46, 70, 94):
        _trough(t, 20, gy, 180, 12)
    # fuel in / spent wells
    for i in range(4):
        _slot(t, 20 + i * 18, 180)
    for i in range(2):
        _slot(t, 164 + i * 18, 180)
    # SCRAM button bay
    t.rect(205, 175, 249, 197, DANGER[1])
    outline(t, 205, 175, 249, 197, DARK)
    # player inventory
    _recess(t, 40, 214, 217, 299)
    for row in range(3):
        for col in range(9):
            _slot(t, 48 + col * 18, 222 + row * 18)
    for col in range(9):
        _slot(t, 48 + col * 18, 280)
    return t

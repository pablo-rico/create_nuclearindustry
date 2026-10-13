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
    w, h = 256, 316
    t = _plate(w, h, 401)
    # header with hazard trim
    _recess(t, 8, 6, w - 9, 20)
    hazard(t, 9, 7, 30, 19, WARN[4], DARK[0], period=6, seed=401)
    hazard(t, w - 31, 7, w - 10, 19, WARN[4], DARK[0], period=6, seed=402)
    # readout window
    _recess(t, 12, 26, w - 13, 180)
    for gy in (46, 70, 94):
        _trough(t, 20, gy, 180, 12)
    # fuel in / spent wells
    for i in range(4):
        _slot(t, 20 + i * 18, 192)
    for i in range(2):
        _slot(t, 164 + i * 18, 192)
    # SCRAM button bay
    t.rect(205, 187, 249, 209, DANGER[1])
    outline(t, 205, 187, 249, 209, DARK)
    # player inventory
    _recess(t, 40, 226, 217, 311)
    for row in range(3):
        for col in range(9):
            _slot(t, 48 + col * 18, 234 + row * 18)
    for col in range(9):
        _slot(t, 48 + col * 18, 292)
    return t


@gui("control_module")
def control_module():
    """ControlModuleScreen: 256x240. Coordinates are the screen's constants.

    The lists, the setting value and the live signal are drawn by the screen over these recesses.
    """
    w, h = 256, 240
    t = _plate(w, h, 405)
    # header: module type and console, warning and machine status (two text lines)
    _recess(t, 6, 4, w - 7, 26)
    # function list (left, 98x84 at 7,40) and options / description (right, 139x84 at 110,40)
    _recess(t, 6, 39, 105, 124)
    _recess(t, 109, 39, w - 7, 124)
    # setting input (81x16 at 91,129) between the label box and the four output buttons (from 177)
    _recess(t, 90, 128, 172, 145)
    # link column: Redstone Link icon at (14,156), frequency wells at (14,178) and (14,200); with
    # the Advanced Redstone Link output the screen covers the wells with the code box
    _recess(t, 8, 150, 36, 235)
    _slot(t, 14, 178)
    _slot(t, 14, 200)
    # player inventory
    _recess(t, 40, 150, 217, 235)
    for row in range(3):
        for col in range(9):
            _slot(t, 48 + col * 18, 158 + row * 18)
    for col in range(9):
        _slot(t, 48 + col * 18, 216)
    # signal meter: value at the top, trough 231..238 x 168..227 filled from the bottom, 4 px a level
    _recess(t, 220, 150, w - 7, 235)
    t.rect(230, 167, 239, 228, DARK[0])
    t.rect(231, 168, 238, 227, DARK[2])
    for level in range(1, 15):
        t.hline(231, 232, 228 - 4 * level, DARK[4])
    return t


@gui("tokamak_control")
def tokamak_control():
    """TokamakScreen: 256x222. Coordinates are the screen's constants."""
    from palette import CRYO, CYAN
    w, h = 256, 222
    t = _plate(w, h, 402)
    _recess(t, 8, 4, w - 9, 18)
    hazard(t, 9, 5, 26, 17, CYAN[3], DARK[0], period=6, seed=403)
    hazard(t, w - 27, 5, w - 10, 17, CYAN[3], DARK[0], period=6, seed=404)
    # phase panel: two text lines and the phase lamp (filled 236..243 x 27..38)
    _recess(t, 8, 22, w - 9, 43)
    t.rect(235, 26, 244, 39, DARK[0])
    t.rect(236, 27, 243, 38, DARK[2])
    # telemetry window: ten rows from y=51 every 11 px; gauges (48x5) on the first three
    _recess(t, 8, 47, 171, 163)
    for gy in (52, 63, 74):
        _trough(t, 118, gy, 48, 5)
    # tank column: D, T, water, plasma steam, liquid helium, warm helium (8x96 from y=51)
    _recess(t, 175, 47, w - 9, 163)
    for i in range(6):
        x = 178 + 12 * i
        t.rect(x - 1, 50, x + 8, 147, DARK[0])
        t.rect(x, 51, x + 7, 146, DARK[2])
        for y in range(59, 147, 12):
            t.hline(x, x + 2, y, DARK[4])
    # controls: Start, Shutdown and heating buttons on top; three setpoints with -/+ below
    _recess(t, 8, 166, w - 9, 217)
    for i in range(3):
        gx = 12 + 78 * i
        t.rect(gx - 1, 186, gx + 76, 214, CRYO[1])
        outline(t, gx - 1, 186, gx + 76, 214, DARK)
        t.rect(gx + 15, 199, gx + 61, 212, DARK[1])
        t.hline(gx + 15, gx + 61, 199, DARK[0])
    return t

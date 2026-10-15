"""Textures for radioactive waste transport: the Radioactive Waste Cistern and the Shielded Waste Cask.

Register block textures with ``@block``; this module is imported at the end of ``blocks.py``.

Both blocks use Create's connected textures (``AllCTTypes.RECTANGLE``): every face has a lone 16x16
tile (items and unconnected blocks) and a 64x64 ``_connected`` sheet of 4x4 tiles, index =
column + 4 * row. Column 0 is joined on neither side, 1 joined right, 2 joined both, 3 joined
left; row 0 is joined below, 1 joined both, 2 joined above, 3 joined neither. Frames, hazard bands
and rivets are drawn only on the sides of a tile that meet the outside of the multiblock, and the
surface noise uses tile-local coordinates wrapped at 16, so neighbouring tiles join seamlessly.

The look is deliberately unlike Create's copper tank and brass vault: a lead-grey ribbed cistern
with a yellow-and-black hazard band and a trefoil at the top of each column, and a pale concrete
cask held by dark steel straps, its ends hazard-striped and stencilled with trefoils.

The cask's long sides are drawn turned 90 degrees in its model (as Create's vault): texture rows run
along the cask's axis, so the end bands sit on the tile edges that meet the cask's ends.
"""

from blocks import SHEET_BLOCKS, block
from palette import CONCRETE, DARK, LEAD, STEEL, WARN
from pixelkit import Tex, fbm, rivet, shade

YELLOW = WARN[4]
BLACK_PAINT = DARK[0]


def _sheet(name, seed, painter):
    """Registers ``name`` (the lone tile) and ``name_connected`` (the RECTANGLE sheet) for ``painter``."""
    SHEET_BLOCKS[name + "_connected"] = (64, 64)

    @block(name)
    def lone():
        t = Tex(seed=seed)
        painter(t, 0, 0, True, True, True, True)
        return t

    @block(name + "_connected")
    def connected():
        t = Tex(64, 64, seed + 1)
        for row in range(4):
            for col in range(4):
                painter(t, col * 16, row * 16, left=col in (0, 1), right=col in (0, 3),
                        top=row in (0, 3), bottom=row in (2, 3))
        return t

    lone.__doc__ = painter.__doc__
    connected.__doc__ = painter.__doc__


def _hazard_rows(t, ox, oy, y0, y1):
    """Diagonal yellow-and-black stripes across a tile between rows y0..y1; period 4 so tiles join."""
    for y in range(y0, y1 + 1):
        for x in range(16):
            t.set(ox + x, oy + y, YELLOW if (x + y) % 4 < 2 else BLACK_PAINT)


def _hazard_cols(t, ox, oy, x0, x1):
    for x in range(x0, x1 + 1):
        for y in range(16):
            t.set(ox + x, oy + y, YELLOW if (x + y) % 4 < 2 else BLACK_PAINT)


# A 9x9 trefoil sign: black blades reaching a black rim on a yellow ground (rim corners in dark trim).
TREFOIL_BADGE = [
    "rrKKKKKrr",
    "rKKYYYKKr",
    "KKKKYKKKK",
    "KYKKYKKYK",
    "KYYYKYYYK",
    "KYYYYYYYK",
    "KYYKKKYYK",
    "rKYKKKYKr",
    "rrKKKKKrr",
]


def _trefoil_badge(t, ox, oy, x0, y0):
    """Stamps the trefoil badge with its top-left corner at (x0, y0) of the tile."""
    t.stamp(TREFOIL_BADGE, {"Y": YELLOW, "K": BLACK_PAINT, "r": DARK[1]}, ox + x0, oy + y0)


# ======================================================================================
# cistern
# ======================================================================================

def _cistern_side(t, ox, oy, left, right, top, bottom):
    """Lead-grey ribbed shell; a hazard band and a trefoil at the top of each column, a riveted plinth below."""
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 7101, 16, 16, 2, 4.0)
            c = LEAD[3] if n < 0.52 else LEAD[4]
            if x % 4 == 3:
                c = LEAD[2]
            elif x % 4 == 0:
                c = LEAD[5]
            t.set(ox + x, oy + y, c)
    if top:
        t.hline(ox, ox + 15, oy, STEEL[5])
        t.hline(ox, ox + 15, oy + 1, STEEL[3])
        _hazard_rows(t, ox, oy, 2, 3)
        t.hline(ox, ox + 15, oy + 4, DARK[1])
        _trefoil_badge(t, ox, oy, 4, 5)
    if bottom:
        t.hline(ox, ox + 15, oy + 14, STEEL[3])
        t.hline(ox, ox + 15, oy + 15, DARK[0])
        for x in (2, 6, 10, 14):
            t.set(ox + x, oy + 14, STEEL[6])
    if left:
        t.vline(ox, oy, oy + 15, STEEL[5])
        t.vline(ox + 1, oy, oy + 15, STEEL[3])
        t.vline(ox + 2, oy, oy + 15, DARK[1])
    if right:
        t.vline(ox + 15, oy, oy + 15, DARK[0])
        t.vline(ox + 14, oy, oy + 15, STEEL[2])
        t.vline(ox + 13, oy, oy + 15, DARK[1])
    if top and left:
        t.set(ox, oy, STEEL[6])
    if bottom and right:
        t.set(ox + 15, oy + 15, BLACK_PAINT)


def _cistern_top(t, ox, oy, left, right, top, bottom):
    """A steel lid with a tread pattern, riveted where it meets the edge; a hatch on lone and centre tiles."""
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 7201, 16, 16, 2, 4.0)
            c = STEEL[3] if n < 0.5 else STEEL[2]
            if (x + y) % 4 == 0 and (x - y) % 8 == 0:
                c = STEEL[5]
            elif (x + y) % 4 == 1 and (x - y) % 8 == 7:
                c = STEEL[1]
            t.set(ox + x, oy + y, c)
    if top:
        t.hline(ox, ox + 15, oy, DARK[0])
        t.hline(ox, ox + 15, oy + 1, STEEL[5])
    if bottom:
        t.hline(ox, ox + 15, oy + 15, DARK[0])
        t.hline(ox, ox + 15, oy + 14, STEEL[1])
    if left:
        t.vline(ox, oy, oy + 15, DARK[0])
        t.vline(ox + 1, oy, oy + 15, STEEL[5])
    if right:
        t.vline(ox + 15, oy, oy + 15, DARK[0])
        t.vline(ox + 14, oy, oy + 15, STEEL[1])
    for i in (3, 7, 11):
        if top:
            rivet(t, ox + i, oy + 2, STEEL)
        if bottom:
            rivet(t, ox + i, oy + 12, STEEL)
        if left:
            rivet(t, ox + 2, oy + i, STEEL)
        if right:
            rivet(t, ox + 12, oy + i, STEEL)
    if left == right and top == bottom:
        # The filling hatch, with its yellow rim.
        for y in range(16):
            for x in range(16):
                d = (x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2
                if d <= 4.2 ** 2:
                    t.set(ox + x, oy + y, YELLOW)
                if d <= 3.4 ** 2:
                    t.set(ox + x, oy + y, STEEL[4])
                if d <= 2.2 ** 2:
                    t.set(ox + x, oy + y, STEEL[2])
        t.hline(ox + 6, ox + 9, oy + 8, DARK[1])
        t.set(ox + 7, oy + 7, STEEL[6])


_sheet("radioactive_waste_cistern_side", 7100, _cistern_side)
_sheet("radioactive_waste_cistern_top", 7200, _cistern_top)


# ======================================================================================
# cask
# ======================================================================================

def _concrete(t, ox, oy, seed, base=3):
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, seed, 16, 16, 3, 4.0)
            c = CONCRETE[base + 1] if n > 0.6 else CONCRETE[base] if n > 0.33 else CONCRETE[base - 1]
            t.set(ox + x, oy + y, c)


def _strap(t, ox, oy, y):
    """A dark steel strap across the tile at row y (and its shadow below), with two bolts."""
    t.hline(ox, ox + 15, oy + y, STEEL[4])
    t.hline(ox, ox + 15, oy + y + 1, STEEL[2])
    t.hline(ox, ox + 15, oy + y + 2, DARK[1])
    for x in (3, 12):
        t.set(ox + x, oy + y, STEEL[6])
        t.set(ox + x, oy + y + 1, DARK[0])


def _cask_ends(t, ox, oy, top, bottom):
    """The steel end rings of the cask (texture rows run along its axis), each with a hazard stripe."""
    if top:
        t.hline(ox, ox + 15, oy, DARK[0])
        t.hline(ox, ox + 15, oy + 1, STEEL[4])
        t.hline(ox, ox + 15, oy + 2, STEEL[2])
        _hazard_rows(t, ox, oy, 3, 4)
    if bottom:
        _hazard_rows(t, ox, oy, 11, 12)
        t.hline(ox, ox + 15, oy + 13, STEEL[3])
        t.hline(ox, ox + 15, oy + 14, STEEL[1])
        t.hline(ox, ox + 15, oy + 15, DARK[0])


def _cask_edges(t, ox, oy, left, right):
    if left:
        t.vline(ox, oy, oy + 15, DARK[0])
        t.vline(ox + 1, oy, oy + 15, STEEL[3])
    if right:
        t.vline(ox + 15, oy, oy + 15, DARK[0])
        t.vline(ox + 14, oy, oy + 15, STEEL[1])


def _cask_front(t, ox, oy, left, right, top, bottom):
    """The end walls: dark lead plate, hazard-striped round the outside, a bolted trefoil plate on every tile."""
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 7301, 16, 16, 2, 4.0)
            t.set(ox + x, oy + y, LEAD[2] if n < 0.45 else LEAD[3])
    if top:
        _hazard_rows(t, ox, oy, 0, 1)
        t.hline(ox, ox + 15, oy + 2, DARK[0])
    if bottom:
        _hazard_rows(t, ox, oy, 14, 15)
        t.hline(ox, ox + 15, oy + 13, DARK[0])
    if left:
        _hazard_cols(t, ox, oy, 0, 1)
        t.vline(ox + 2, oy, oy + 15, DARK[0])
    if right:
        _hazard_cols(t, ox, oy, 14, 15)
        t.vline(ox + 13, oy, oy + 15, DARK[0])
    # Bolted stencil plate.
    t.rect(ox + 3, oy + 3, ox + 13, oy + 13, LEAD[4])
    t.hline(ox + 3, ox + 13, oy + 3, LEAD[6])
    t.vline(ox + 3, oy + 3, oy + 13, LEAD[5])
    t.hline(ox + 3, ox + 13, oy + 13, LEAD[1])
    t.vline(ox + 13, oy + 3, oy + 13, LEAD[1])
    _trefoil_badge(t, ox, oy, 4, 4)
    for x, y in ((3, 3), (13, 3), (3, 13), (13, 13)):
        t.set(ox + x, oy + y, STEEL[6] if (x, y) == (3, 3) else DARK[0])


def _cask_side(t, ox, oy, left, right, top, bottom):
    """Pale concrete held by dark steel straps, its end rings hazard-striped."""
    _concrete(t, ox, oy, 7401)
    _strap(t, ox, oy, 7)
    _cask_ends(t, ox, oy, top, bottom)
    _cask_edges(t, ox, oy, left, right)


def _cask_top(t, ox, oy, left, right, top, bottom):
    """The top: the same concrete and straps, with a lifting lug on each strap."""
    _concrete(t, ox, oy, 7501)
    _strap(t, ox, oy, 7)
    for x in (6, 9):
        t.rect(ox + x, oy + 6, ox + x, oy + 9, DARK[1])
        t.set(ox + x, oy + 6, STEEL[5])
    _cask_ends(t, ox, oy, top, bottom)
    _cask_edges(t, ox, oy, left, right)


def _cask_bottom(t, ox, oy, left, right, top, bottom):
    """The underside: bare dark steel, strapped like the rest."""
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 7601, 16, 16, 2, 4.0)
            t.set(ox + x, oy + y, shade(DARK[3] if n < 0.5 else DARK[4], 1.0))
    _strap(t, ox, oy, 7)
    _cask_ends(t, ox, oy, top, bottom)
    _cask_edges(t, ox, oy, left, right)


_sheet("shielded_waste_cask_front", 7300, _cask_front)
_sheet("shielded_waste_cask_side", 7400, _cask_side)
_sheet("shielded_waste_cask_top", 7500, _cask_top)
_sheet("shielded_waste_cask_bottom", 7600, _cask_bottom)

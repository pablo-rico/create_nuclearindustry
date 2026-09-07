"""Neutral rolled-steel reactor surfaces and mineral overlays on vanilla rock."""

from pathlib import Path
from PIL import Image
from pixelkit import Tex, Ramp, fbm, bevel

STEEL = Ramp("#45494c", "#5a5e61", "#707577", "#868b8d", "#999fa1", "#adb3b4", "#c2c8c8")
REFERENCE = Path(__file__).parent / "reference/assets/minecraft/textures/block"


def steel_panel(t=None, seed=11):
    t = t if t is not None else Tex(seed=seed)
    # The broad face is deliberately uninterrupted; low-contrast clusters describe
    # the material without turning every structural block into a control panel.
    for y in range(16):
        for x in range(16):
            grain = fbm(x, y, 611, 16, 16, 2, 7.0)
            band = fbm(x, y, 91, 16, 16, 2, 3.0)
            value = 3 if grain + band * 0.25 < 0.67 else 4
            t.set(x, y, STEEL[value])
    t.hline(0, 15, 0, STEEL[2])
    t.vline(0, 0, 15, STEEL[2])
    t.hline(1, 14, 1, STEEL[5])
    t.vline(1, 2, 14, STEEL[4])
    t.hline(0, 15, 15, STEEL[1])
    t.vline(15, 1, 14, STEEL[1])
    t.hline(2, 14, 14, STEEL[2])
    for x, y in ((3, 3), (12, 12)):
        t.set(x, y, STEEL[1])
        t.set(x + 1, y, STEEL[2])
        t.set(x, y + 1, STEEL[5])
    for x, y, length in ((5, 4, 3), (8, 10, 3), (3, 12, 2)):
        t.hline(x, x + length - 1, y, STEEL[4])
        t.set(x + length, y, STEEL[2])
    return t


def steel_port(accent, output=False, fuel=False):
    t = steel_panel()
    # Recessed welded socket, with a small painted direction marker.
    t.rect(4, 4, 11, 11, STEEL[1])
    bevel(t, 4, 4, 11, 11, STEEL, hi=STEEL[0], lo=STEEL[5])
    t.rect(5, 5, 10, 10, STEEL[0])
    if fuel:
        for y in (6, 8):
            t.hline(5, 10, y, STEEL[2])
            t.hline(5, 10, y + 1, STEEL[1])
        t.vline(9, 6, 9, STEEL[4])
    else:
        t.disc(8, 8, 3.0, STEEL[3])
        t.disc(8, 8, 2.0, STEEL[0])
        t.set(6, 6, STEEL[5])
        t.hline(7, 8, 9, accent[2])
    t.hline(6, 9, 12, accent[3])
    t.set(8 if output else 7, 11 if output else 13, accent[4])
    return t


def mineral(host, colors, offset=0):
    t = Tex()
    with Image.open(REFERENCE / (host + ".png")) as source:
        t.im = source.convert("RGBA")
    t.px = t.im.load()
    # Broken angular inclusions. Every pixel outside these masks remains vanilla.
    clusters = ((2, 3, (".21", "243", ".30")),
                (9, 1, ("12.", "243", ".30")),
                (6, 7, (".12", "243", "30.")),
                (12, 10, ("21", "43", "30")),
                (2, 12, ("12.", "243")))
    for ox, oy, rows in clusters:
        for y, row in enumerate(rows):
            for x, cell in enumerate(row):
                if cell != ".":
                    t.set((ox + x + offset) % 16, oy + y, colors[int(cell)])
    return t

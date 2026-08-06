#!/usr/bin/env python3
"""Regenerate the whole Create: Nuclear Industry texture set.

    python3 tools/texturegen/generate.py [--out <assets dir>] [--sheet <png>]

Everything is deterministic, so running it twice yields byte-identical files.
"""

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageDraw  # noqa: E402

import blocks  # noqa: E402
import fluids  # noqa: E402
import items  # noqa: E402

DEFAULT_OUT = os.path.join("src", "main", "resources", "assets",
                           "create_nuclearindustry", "textures")


def write(path, image):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=DEFAULT_OUT)
    ap.add_argument("--sheet", default=None, help="write a contact sheet here")
    args = ap.parse_args()

    made = []

    for name, fn in sorted(blocks.BLOCKS.items()):
        t = fn()
        p = os.path.join(args.out, "block", name + ".png")
        write(p, t.im)
        made.append((p, t.im))

    for name, fn in sorted(items.ITEMS.items()):
        t = fn()
        p = os.path.join(args.out, "item", name + ".png")
        write(p, t.im)
        made.append((p, t.im))

    for name, fn in sorted(fluids.FLUIDS.items()):
        im, meta = fn()
        p = os.path.join(args.out, "block", name + ".png")
        write(p, im)
        with open(p + ".mcmeta", "w") as fh:
            json.dump(meta, fh, indent=2)
            fh.write("\n")
        made.append((p, im.crop((0, 0, 16, 16))))

    print("generated %d textures -> %s" % (len(made), args.out))

    if args.sheet:
        cols, cell = 10, 64
        rows = (len(made) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * cell, rows * (cell + 12)), (32, 34, 40, 255))
        d = ImageDraw.Draw(sheet)
        for i, (p, im) in enumerate(made):
            im = im.resize((cell, cell), Image.NEAREST)
            x, y = (i % cols) * cell, (i // cols) * (cell + 12)
            sheet.paste(im, (x, y), im)
            d.text((x + 1, y + cell + 1), os.path.basename(p)[:-4][:15],
                   fill=(230, 230, 235, 255))
        sheet.save(args.sheet)
        print("contact sheet -> " + args.sheet)


if __name__ == "__main__":
    main()

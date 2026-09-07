#!/usr/bin/env python3
"""Build the optional Create Classic block resource pack from native pixel art."""

import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

from PIL import Image, ImageDraw

import palette
from pixelkit import Ramp, brushed, outline, bevel

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "resourcepacks" / "create-nuclear-classic"
ASSETS = Path("assets/create_nuclearindustry/textures")


def configure_palette():
    ramps = {
        "STEEL": ("#303431", "#505751", "#737a70", "#959b8d", "#b0b6a6", "#c9cebd", "#e5e8d6"),
        "DARK": ("#202324", "#303739", "#434c4c", "#59635f", "#707c72", "#8c9687", "#abb49f"),
        "BRASS": ("#44351e", "#6a512c", "#927442", "#b59658", "#d0b575", "#e5cf96", "#f6e7b9"),
        "COPPER": ("#422a22", "#704334", "#965c43", "#b87853", "#d5976b", "#eab38b", "#f6d2af"),
        "LEAD": ("#252b2c", "#41494a", "#5b6362", "#767f7a", "#939b90", "#afb5a6", "#cbd0bc"),
        "CRYO": ("#344749", "#526d6d", "#7b9690", "#a7bab0", "#cbd6c6", "#e3e8d8", "#f5f6e8"),
        "CONCRETE": ("#383934", "#52544c", "#707268", "#8d9084", "#a8ac9e", "#c1c6b5", "#d9ddcd"),
        "STONE": ("#494a43", "#5c5e54", "#707367", "#838779", "#969b8b", "#a9af9e", "#bec4b2"),
        "URANIUM": ("#23341a", "#3f5627", "#5b7a36", "#7a9b48", "#9abd5e", "#bedb80", "#e1efad"),
        "CYAN": ("#163538", "#27565a", "#3d787b", "#569b9b", "#76bbb6", "#a0d9cc", "#d0eee0"),
        "PLASMA": ("#42291d", "#704329", "#9c6235", "#c18847", "#dfae60", "#f0cd85", "#ffe8b8"),
    }
    # Mutate the existing ramps so shared helpers retain their palette references.
    for name, colors in ramps.items():
        getattr(palette, name).c = Ramp(*colors).c


def classic_casing(t, ramp=palette.STEEL, seed=0, base=3.4, amp=0.85,
                   rivets=True, seam=True, axis="h", panel=(4, 4, 11, 11)):
    import common
    brushed(t, ramp, base=base, amp=amp * 0.6, seed=seed, axis=axis)
    outline(t, 0, 0, 15, 15, ramp[0])
    bevel(t, 1, 1, 14, 14, ramp, hi=ramp[5], lo=ramp[1])
    # Folded metal edging and a recessed service plate match Create's casings.
    t.hline(2, 13, 3, ramp[2])
    t.hline(2, 13, 12, ramp[5])
    if seam:
        x0, y0, x1, y1 = panel
        t.rect(x0, y0, x1, y1, ramp[3])
        bevel(t, x0, y0, x1, y1, ramp, hi=ramp[1], lo=ramp[5])
    if rivets:
        for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
            common.bolt(t, x, y, palette.BRASS)
    return t


def main():
    configure_palette()
    import common
    common.casing = classic_casing
    import blocks
    import fluids

    images = {}
    for name, factory in sorted(blocks.BLOCKS.items()):
        images[name] = factory().im
    for name, factory in sorted(fluids.FLUIDS.items()):
        im, meta = factory()
        # Quantized highlights keep vapor readable alongside vanilla pixel art.
        im = im.point(lambda channel: min(255, (channel // 12) * 12 + 6))
        images[name] = im
        dest = OUT / ASSETS / "block" / (name + ".png.mcmeta")
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(json.dumps(meta, indent=2) + "\n")

    for name, im in images.items():
        dest = OUT / ASSETS / "block" / (name + ".png")
        dest.parent.mkdir(parents=True, exist_ok=True)
        im.save(dest)

    source = ROOT / "src/main/resources" / ASSETS / "block"
    expected = {str(p.relative_to(source).with_suffix("")) for p in source.rglob("*.png")}
    assert expected == set(images), (expected - set(images), set(images) - expected)
    for name, im in images.items():
        assert im.width == 16 and im.height % 16 == 0, name
        assert im.getbbox(), name
        original = Image.open(source / (name + ".png")).convert("RGBA")
        assert im.tobytes() != original.tobytes(), "Unchanged texture: " + name

    # Every local block-model texture must be supplied by the pack.
    for model in (ROOT / "src/main/resources/assets/create_nuclearindustry/models/block").rglob("*.json"):
        for ref in json.loads(model.read_text()).get("textures", {}).values():
            if ref.startswith("create_nuclearindustry:block/"):
                assert ref.split(":block/", 1)[1] in images, (model, ref)

    (OUT / "pack.mcmeta").write_text(json.dumps({"pack": {
        "pack_format": 34,
        "description": "Create: Nuclear Industry - Classic Industry 16x"
    }}, indent=2) + "\n")
    images["reactor_casing"].resize((128, 128), Image.Resampling.NEAREST).save(OUT / "pack.png")

    cols, cell_w, cell_h = 6, 180, 158
    sheet = Image.new("RGB", (cols * cell_w, ((len(images) + cols - 1) // cols) * cell_h), "#292e2b")
    draw = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(images.items()):
        x, y = (i % cols) * cell_w, (i // cols) * cell_h
        tile = im.crop((0, 0, 16, 16)).resize((112, 112), Image.Resampling.NEAREST)
        sheet.paste(tile, (x + 34, y + 8), tile)
        label = name.replace("projectile/", "").replace("_", " ")
        words, line, lines = label.split(), "", []
        for word in words:
            if len(line + word) > 27:
                lines.append(line.strip())
                line = ""
            line += word + " "
        lines.append(line.strip())
        draw.multiline_text((x + 6, y + 124), "\n".join(lines), fill="#e5e8d6", spacing=2)
    sheet.save(OUT.parent / "create-nuclear-classic-preview.png")

    archive = OUT.with_suffix(".zip")
    with ZipFile(archive, "w", compression=ZIP_DEFLATED) as zip_file:
        for path in sorted(OUT.rglob("*")):
            if path.is_file():
                info = ZipInfo(path.relative_to(OUT).as_posix(), date_time=(2026, 1, 1, 0, 0, 0))
                info.compress_type = ZIP_DEFLATED
                zip_file.writestr(info, path.read_bytes())
    print(f"Validated {len(images)} new block textures and all local model references.")
    print(archive)


if __name__ == "__main__":
    main()

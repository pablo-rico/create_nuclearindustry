"""Check generated texture coverage, UV dimensions, alpha and JAR contents."""

import argparse
import json
from pathlib import Path
from zipfile import ZipFile

from PIL import Image
from reactor_materials import REFERENCE
import armor
import blocks
import fluids
import gui
import items
import particles

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/create_nuclearindustry"
GENERATED_ASSETS = ROOT / "src/generated/resources/assets/create_nuclearindustry"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path)
    args = parser.parse_args()
    textures = ASSETS / "textures"
    expected = {}
    for folder, factories in (("block", blocks.BLOCKS), ("item", items.ITEMS)):
        for name, factory in factories.items():
            expected[f"{folder}/{name}.png"] = factory().im
    # Larger textures with their own layouts: screens, armor layers, effect icons.
    special = {}
    for folder, factories in (("gui", gui.GUIS), ("models/armor", armor.ARMOR), ("mob_effect", armor.EFFECTS),
                                ("particle", particles.PARTICLES)):
        for name, factory in factories.items():
            special[f"{folder}/{name}.png"] = factory().im
    expected.update(special)
    for name, factory in fluids.FLUIDS.items():
        im, meta = factory()
        expected[f"block/{name}.png"] = im
        assert json.loads((textures / f"block/{name}.png.mcmeta").read_text()) == meta
    actual = {p.relative_to(textures).as_posix() for p in textures.rglob("*.png")}
    assert actual == set(expected), (actual - set(expected), set(expected) - actual)
    for name, expected_image in expected.items():
        with Image.open(textures / name) as im:
            assert im.size == expected_image.size, name
            assert im.tobytes() == expected_image.tobytes(), "Stale generated texture: " + name
            assert im.getbbox() is not None, name
            if name in special:
                continue
            cutout = name.startswith("block/") and name[len("block/"):-4] in blocks.CUTOUT_BLOCKS
            if cutout:
                assert im.size == blocks.CUTOUT_BLOCKS[name[len("block/"):-4]], name
                assert set(im.getchannel("A").tobytes()) == {0, 255}, "Cutout texture needs clear and opaque pixels: " + name
                continue
            assert im.width == 16 and im.height % 16 == 0, name
            alpha = im.getchannel("A")
            assert set(alpha.tobytes()) <= {0, 255}, "Blended alpha: " + name
            if name.startswith("block/"):
                assert alpha.getextrema() == (255, 255), "Transparent block face: " + name
            else:
                assert alpha.getextrema() == (0, 255), "Missing item silhouette: " + name
    models = list((ASSETS / "models").rglob("*.json")) + list((GENERATED_ASSETS / "models").rglob("*.json"))
    for model in models:
        for ref in json.loads(model.read_text()).get("textures", {}).values():
            if ref.startswith("create_nuclearindustry:"):
                assert ref.split(":", 1)[1] + ".png" in expected, (model, ref)
    for name, host in (("uranium_ore", "deepslate"), ("borax_ore", "stone"), ("thorium_ore", "stone"),
                       ("spodumene_ore", "stone")):
        with Image.open(REFERENCE / (host + ".png")) as source:
            original = source.convert("RGBA").tobytes()
        ore = expected[f"block/{name}.png"].tobytes()
        unchanged = sum(original[i:i + 4] == ore[i:i + 4] for i in range(0, len(ore), 4))
        assert unchanged >= 200, "Vanilla rock modified outside mineral inclusions: " + name
    if args.jar:
        with ZipFile(args.jar) as archive:
            for file in textures.rglob("*"):
                if file.is_file():
                    entry = "assets/create_nuclearindustry/textures/" + file.relative_to(textures).as_posix()
                    assert archive.read(entry) == file.read_bytes(), "Stale JAR asset: " + entry
    print(f"Validated {len(expected)} textures, alpha, animation metadata and {len(models)} models' texture references.")
    if args.jar:
        print("All textures in the JAR match the source assets.")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Writes empty structure templates used as GameTest arenas.

GameTests build their multiblocks in code, so the templates only need to
reserve space. Usage:

    python3 tools/structgen/empty_template.py
"""
import gzip
import pathlib
import struct

DATA_VERSION = 3955  # Minecraft 1.21.1
OUT = pathlib.Path(__file__).resolve().parents[2] / "src/main/resources/data/create_nuclearindustry/structure/gametest"

# name -> (x, y, z)
TEMPLATES = {
    "empty_13x16x13": (13, 16, 13),
    "empty_41x9x41": (41, 9, 41),
}

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def _name(name: str) -> bytes:
    raw = name.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _int(name: str, value: int) -> bytes:
    return bytes([TAG_INT]) + _name(name) + struct.pack(">i", value)


def _string_payload(value: str) -> bytes:
    return _name(value)


def _list(name: str, element_type: int, payloads: list) -> bytes:
    return bytes([TAG_LIST]) + _name(name) + bytes([element_type]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def _compound_payload(*entries: bytes) -> bytes:
    return b"".join(entries) + bytes([TAG_END])


def template(size) -> bytes:
    air = _compound_payload(bytes([TAG_STRING]) + _name("Name") + _string_payload("minecraft:air"))
    body = _compound_payload(
        _int("DataVersion", DATA_VERSION),
        _list("size", TAG_INT, [struct.pack(">i", v) for v in size]),
        _list("palette", TAG_COMPOUND, [air]),
        _list("blocks", TAG_END, []),
        _list("entities", TAG_END, []),
    )
    return bytes([TAG_COMPOUND]) + _name("") + body


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, size in TEMPLATES.items():
        path = OUT / f"{name}.nbt"
        # mtime=0 keeps the output byte-identical between runs.
        with open(path, "wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as fh:
            fh.write(template(size))
        print(f"wrote {path.relative_to(OUT.parents[6])}")


if __name__ == "__main__":
    main()

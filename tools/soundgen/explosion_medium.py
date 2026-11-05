#!/usr/bin/env python3
"""Synthesise a medium conventional blast heard close by, a few tens of kilograms of charge (3 s): a heavier
bang, a deeper thump, a short roll and a longer rain of debris.

    python tools/soundgen/explosion_medium.py [--out <ogg>]

The crack of the shock, the thump, the blast's broadband body, a short roll and the debris it threw up landing again; see
``blastkit.he_blast``. The bigger the charge, the longer the shock's positive phase, so the lower and longer the boom. See
soundkit.py for the requirements.
"""

import argparse

import blastkit as bk
import soundkit as sk

SECONDS = 3.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("explosion_medium"))
    args = ap.parse_args()
    sk.write(args.out, bk.he_blast(SECONDS, 2820, 0.018, 50.0, 0.35, 0.15, 5000.0, 0.9, 60.0, 2.3, 1.8), peak=0.9)


if __name__ == "__main__":
    main()

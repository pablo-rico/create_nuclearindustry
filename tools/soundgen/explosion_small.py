#!/usr/bin/env python3
"""Synthesise a small conventional blast heard close by, up to about 8 kg of charge (2 s): a sharp, hard
bang, a quick thump and a short patter of debris.

    python tools/soundgen/explosion_small.py [--out <ogg>]

The crack of the shock, the thump, the blast's broadband body, a short roll and the debris it threw up landing again; see
``blastkit.he_blast``. The bigger the charge, the longer the shock's positive phase, so the lower and longer the boom. See
soundkit.py for the requirements.
"""

import argparse

import blastkit as bk
import soundkit as sk

SECONDS = 2.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("explosion_small"))
    args = ap.parse_args()
    sk.write(args.out, bk.he_blast(SECONDS, 2810, 0.008, 70.0, 0.18, 0.07, 7000.0, 0.4, 40.0, 1.4, 1.2), peak=0.9)


if __name__ == "__main__":
    main()

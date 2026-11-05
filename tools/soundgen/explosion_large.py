#!/usr/bin/env python3
"""Synthesise a large conventional blast heard close by, 60 kg of charge and more (5 s): a huge, deep boom, a
long rolling rumble and debris raining down for seconds.

    python tools/soundgen/explosion_large.py [--out <ogg>]

The crack of the shock, the thump, the blast's broadband body, a short roll and the debris it threw up landing again; see
``blastkit.he_blast``. The bigger the charge, the longer the shock's positive phase, so the lower and longer the boom. See
soundkit.py for the requirements.
"""

import argparse

import blastkit as bk
import soundkit as sk

SECONDS = 5.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("explosion_large"))
    args = ap.parse_args()
    sk.write(args.out, bk.he_blast(SECONDS, 2830, 0.04, 36.0, 0.7, 0.35, 3500.0, 1.8, 80.0, 4.2, 2.8), peak=0.9)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Synthesise debris and earth pattering down after a blast (3.5 s).

    python tools/soundgen/debris_rain.py [--out <ogg>]

What a blast throws up comes down over a few seconds: a dense shower of grit and pebbles, each a tiny knock at its own
pitch, a few heavy clods thudding in among them, and the hiss of loose soil. The shower thickens fast and thins out
slowly. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import blastkit as bk
import soundkit as sk

SECONDS = 3.5


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("debris_rain"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    grit = bk.impacts(samples, 2851, 0.0, 0.35, 3.3, 160.0, low=300.0, high=4000.0, thuds=0.12)
    clods = bk.lowpass(bk.impacts(samples, 2852, 0.05, 0.4, 2.5, 8.0, low=60.0, high=200.0, size=2.0, thuds=1.0), 800.0)
    soil = bk.burst(samples, 2853, 150.0, 3000.0, 0.0, 0.2, 1.0, tilt=-0.8, hold=0.6)
    # Soft-limit the shower so a few big stones do not set the level for the whole patter.
    grit = np.tanh(3.0 * bk.normalise(grit))
    mix = 1.0 * bk.normalise(grit) + 0.6 * bk.normalise(clods) + 0.25 * soil
    mix = bk.reverb(mix, 1.0, 2854, wet=0.2, damp=3000.0)
    mix *= bk.envelope(t, 0.0, 0.08, 10.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=0.5, cutoff=6000.0), peak=0.85)


if __name__ == "__main__":
    main()

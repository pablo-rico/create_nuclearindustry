#!/usr/bin/env python3
"""Synthesise the crack of a nuclear shock front arriving close by (three variants, nuke_crack_0..2).

    python tools/soundgen/nuke_crack.py [--out-dir <dir>]

The front of a strong shock is near-instant: a supersonic snap, the pressure jumping and falling away in a fraction of a
second (the N-wave), the second edge snapping again as it returns to rest, then a short, rough tail as the air behind it
rattles everything it passes. The variants differ in the N-wave's length and the snap's edge, so a battery of them does not
repeat. See soundkit.py for the requirements.
"""

import argparse
import os

import blastkit as bk
import soundkit as sk

SECONDS = 2.0
# Seed, length of the N-wave (s), rise time of its edges (s).
VARIANTS = ((2610, 0.22, 0.0004), (2620, 0.30, 0.0005), (2630, 0.26, 0.0003))


def make(seed, length, rise):
    samples, t = sk.timeline(SECONDS)
    start = 0.01
    wave = bk.n_wave(samples, start, length, rise_s=rise, asym=0.85)
    snap = bk.crack(samples, seed, start, decay=0.010, cutoff=1800.0)
    snap_back = bk.crack(samples, seed + 1, start + length, decay=0.008, cutoff=1500.0)
    rattle = bk.burst(samples, seed + 2, 300.0, 6000.0, start, 0.002, 0.07, tilt=-0.5)
    tail = bk.burst(samples, seed + 3, 25.0, 900.0, start + 0.02, 0.02, 0.5, tilt=-1.0)
    mix = 1.0 * wave + 0.55 * snap + 0.3 * snap_back + 0.25 * rattle + 0.5 * tail
    mix = bk.reverb(mix, 1.6, seed + 4, wet=0.25, damp=1600.0)
    return bk.finish(mix, t, SECONDS, fade=0.4)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-dir", default=sk.SOUNDS)
    args = ap.parse_args()
    for i, (seed, length, rise) in enumerate(VARIANTS):
        sk.write(os.path.join(args.out_dir, "nuke_crack_%d.ogg" % i), make(seed, length, rise), peak=0.9)


if __name__ == "__main__":
    main()

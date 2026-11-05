#!/usr/bin/env python3
"""Synthesise lightning inside a young mushroom cloud: a crackling discharge and a soft thunder tail (2.8 s).

    python tools/soundgen/cloud_lightning.py [--out <ogg>]

The charge torn apart in the rising cloud discharges in a ragged stroke: a fast train of sharp pops (the channel crackling
as it forms), a second stroke down the same path a moment later, then thunder rolling softly away. See soundkit.py for the
requirements.
"""

import argparse

import blastkit as bk
import rocketkit as rk
import soundkit as sk

SECONDS = 2.8


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("cloud_lightning"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    pops = rk.crackle(samples, 900, seed=2741, length_s=0.0015, loop=False, sharpness=2.0)
    pops = bk.highpass(pops, 1500.0) * (bk.envelope(t, 0.0, 0.002, 0.08) + 0.6 * bk.envelope(t, 0.13, 0.002, 0.06))
    strike = bk.crack(samples, 2742, 0.0, decay=0.02, cutoff=1200.0) + 0.5 * bk.crack(samples, 2743, 0.13, decay=0.015)
    thunder = bk.burst(samples, 2744, 30.0, 700.0, 0.05, 0.1, 0.9, tilt=-1.0) * bk.rolling(samples, 2745, 4.0, 0.7)
    rumble = bk.burst(samples, 2746, 20.0, 200.0, 0.2, 0.3, 1.0, tilt=-1.0)
    mix = 0.7 * bk.normalise(pops) + 0.6 * strike + 1.0 * thunder + 0.6 * rumble
    mix = bk.reverb(mix, 2.0, 2747, wet=0.3, damp=2000.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=0.6), peak=0.9)


if __name__ == "__main__":
    main()

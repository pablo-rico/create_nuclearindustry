#!/usr/bin/env python3
"""Synthesise a cluster warhead's bomblets going off one after another (4 s).

    python tools/soundgen/cluster_crackle.py [--out <ogg>]

Dozens of small charges land scattered over the ground and go off in a ragged, popcorn chain: a few first, then a dense
burst, then stragglers. Each is a small, sharp bang; the far ones are quieter and duller, the near ones crack. They are
Poisson-timed, and their rumble adds up under them. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import blastkit as bk
import soundkit as sk

SECONDS = 4.0
PEAK = 0.9


def bang(rng, distance):
    """One bomblet, ``distance`` 0 (near) .. 1 (far across the strike)."""
    n = int(0.35 * sk.RATE)
    seed = int(rng.integers(1 << 30))
    pitch = rng.uniform(90.0, 130.0)
    b = (bk.n_wave(n, 0.002, rng.uniform(0.002, 0.004), rise_s=0.0002, asym=0.5)
         + 0.5 * bk.crack(n, seed, 0.002, decay=0.004, cutoff=1800.0)
         + 0.8 * bk.sub_thump(n, 0.002, pitch * 1.6, pitch, 0.06)
         + 0.5 * bk.burst(n, seed + 1, 60.0, 6000.0, 0.002, 0.001, 0.04, tilt=-0.6))
    return bk.lowpass(b, 6000.0 / (1.0 + 4.0 * distance), order=2) / (1.0 + 3.0 * distance)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("cluster_crackle"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    rng = np.random.default_rng(2941)
    out = np.zeros(samples + sk.RATE)
    when = 0.05
    while True:
        rate = 3.0 + 40.0 * np.exp(-((when - PEAK) / 0.5) ** 2)
        when += rng.exponential(1.0 / rate)
        if when > 3.3:
            break
        b = bang(rng, rng.random()) * (rng.pareto(3.0) * 0.3 + 0.7)
        i = int(when * sk.RATE)
        out[i:i + len(b)] += b
    chain = out[:samples]
    rumble = bk.burst(samples, 2942, 20.0, 250.0, 0.1, 0.4, 1.5, tilt=-1.0, hold=0.6) * bk.rolling(samples, 2943, 2.0, 0.5)
    mix = bk.normalise(chain) + 0.3 * rumble
    mix = bk.reverb(mix, 1.8, 2944, wet=0.35, damp=2000.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=0.6), peak=0.9)


if __name__ == "__main__":
    main()

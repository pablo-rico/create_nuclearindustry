#!/usr/bin/env python3
"""Synthesise a cluster warhead's casing blown open to free its bomblets (1.5 s).

    python tools/soundgen/cluster_open.py [--out <ogg>]

A small cutting charge splits the casing: a sharp pop, the casing's halves ringing as they peel away, the bomblets knocking
against each other and the shell as they spill out, and the whoosh of them spreading into the airstream. See soundkit.py
for the requirements.
"""

import argparse

import numpy as np

import blastkit as bk
import soundkit as sk

SECONDS = 1.5


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("cluster_open"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    pop = (bk.n_wave(samples, 0.01, 0.005, rise_s=0.0002, asym=0.5) + 0.6 * bk.crack(samples, 2931, 0.01, decay=0.005, cutoff=2000.0)
           + 0.7 * bk.sub_thump(samples, 0.01, 140.0, 90.0, 0.08))
    rng = np.random.default_rng(2932)
    # The casing ringing: inharmonic partials of torn sheet metal.
    casing = np.zeros(samples)
    for f in rng.uniform(600.0, 4500.0, 10):
        casing += np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28)) * bk.envelope(t, 0.012, 0.001, rng.uniform(0.05, 0.3)) \
            * rng.uniform(0.3, 1.0)
    # Bomblets knocking together: short metallic pings, thick at first and thinning out.
    clatter = np.zeros(samples + sk.RATE // 4)
    when = 0.02
    while True:
        when += rng.exponential(1.0 / (60.0 * np.exp(-when / 0.25) + 4.0))
        if when > 0.8:
            break
        n = int(0.06 * sk.RATE)
        tt = np.arange(n) / sk.RATE
        ping = np.zeros(n)
        for f in rng.uniform(1500.0, 6000.0, 3):
            ping += np.sin(2 * np.pi * f * tt + rng.uniform(0, 6.28)) * np.exp(-tt / rng.uniform(0.012, 0.04))
        i = int(when * sk.RATE)
        clatter[i:i + n] += ping * (rng.pareto(2.5) + 0.3)
    clatter = clatter[:samples]
    whoosh = bk.burst(samples, 2933, 300.0, 4000.0, 0.02, 0.05, 0.35, tilt=-0.4)
    mix = 1.0 * bk.normalise(pop) + 0.3 * bk.normalise(casing) + 0.35 * bk.normalise(clatter) + 0.2 * whoosh
    mix = bk.reverb(mix, 0.8, 2934, wet=0.25, damp=4000.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=0.3), peak=0.9)


if __name__ == "__main__":
    main()

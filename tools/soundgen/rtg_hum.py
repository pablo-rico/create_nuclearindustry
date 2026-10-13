#!/usr/bin/env python3
"""Synthesise a radioisotope generator's soft hum, looped.

    python tools/soundgen/rtg_hum.py [--out <ogg>]

The thermocouples' converter whines faintly over a soft electrical hum, with a sparse ticking of
decays. Quiet on purpose: an RTG has no moving parts. See soundkit.py for the requirements and the
looping rules.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("rtg_hum"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    hum = pk.harmonic_tone(t, SECONDS, 120.0, (1.0, 0.35, 0.15)) * pk.lfo(t, SECONDS, 0.25, 0.15)
    converter = 0.12 * sk.tone(t, SECONDS, 3200.0) * pk.lfo(t, SECONDS, 0.5, 0.3)
    rng = np.random.default_rng(121)

    def tick(i):
        n = int(sk.RATE * 0.004)
        return rng.uniform(-1, 1, n) * np.exp(-np.arange(n) / (0.0008 * sk.RATE))

    ticks = pk.cyclic_events(samples, np.sort(rng.uniform(0, samples, 18)), tick)
    sk.write(args.out, 0.7 * pk.normalise(hum) + converter + 0.25 * ticks, peak=0.6)


if __name__ == "__main__":
    main()

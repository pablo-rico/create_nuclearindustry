#!/usr/bin/env python3
"""Synthesise the annihilation generator running, looped.

    python tools/soundgen/annihilation_drone.py [--out <ogg>]

A deep, powerful drone with a crackle of gamma flashes in the chamber and the hum of its own
confinement field. See soundkit.py for the requirements and the looping rules.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("annihilation_drone"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    drone = pk.harmonic_tone(t, SECONDS, 55.0, (1.0, 0.7, 0.5, 0.35, 0.2, 0.1)) * pk.lfo(t, SECONDS, 0.75, 0.15)
    field = 0.25 * sk.tone(t, SECONDS, 140.0)
    rng = np.random.default_rng(351)

    def crackle(i):
        n = int(sk.RATE * 0.006)
        return rng.uniform(-1, 1, n) * np.exp(-np.arange(n) / (0.0012 * sk.RATE))

    crackles = pk.cyclic_events(samples, np.sort(rng.uniform(0, samples, 40)), crackle)
    sk.write(args.out, 0.8 * pk.normalise(drone) + field + 0.3 * crackles, peak=0.75)


if __name__ == "__main__":
    main()

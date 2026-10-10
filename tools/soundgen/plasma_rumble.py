#!/usr/bin/env python3
"""Synthesise the burning plasma's looping roar.

    python tools/soundgen/plasma_rumble.py [--out <ogg>]

A deep, dark rumble (low band noise) under a softer roar, breathing slowly, with a sparse crackle
of small arcs. The game raises pitch and volume with the plasma temperature and wobbles the pitch
when the plasma grows unstable. See soundkit.py for the requirements and the looping rules.
"""

import argparse

import numpy as np

import soundkit as sk

SECONDS = 6.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("plasma_rumble"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    rumble = sk.periodic_noise(samples, 22.0, 160.0, seed=21, tilt=-0.6)
    roar = sk.periodic_noise(samples, 160.0, 1100.0, seed=22, tilt=-1.0)
    # Slow breathing: whole cycles over the loop (1/6 Hz and 1/2 Hz).
    breath = 0.8 + 0.12 * sk.tone(t, SECONDS, 1.0 / 6.0) + 0.08 * sk.tone(t, SECONDS, 0.5, phase=0.7)
    # A steady low drone gives the roar a floor.
    drone = 0.35 * sk.tone(t, SECONDS, 41.0) + 0.2 * sk.tone(t, SECONDS, 61.5, phase=0.5)

    # Sparse crackle: short decaying clicks at fixed spots, wrapped round the loop.
    rng = np.random.default_rng(23)
    crackle = np.zeros(samples)
    click_noise = rng.uniform(-1.0, 1.0, 600)
    envelope = np.exp(-np.arange(600) / 70.0)
    for _ in range(14):
        start = int(rng.integers(0, samples))
        level = rng.uniform(0.2, 0.6)
        idx = (start + np.arange(600)) % samples
        crackle[idx] += level * click_noise * envelope
    crackle = sk.lowpass(crackle, 4500.0)

    mix = breath * (1.0 * rumble + 0.35 * roar) + drone + 0.25 * crackle
    sk.write(args.out, mix)


if __name__ == "__main__":
    main()

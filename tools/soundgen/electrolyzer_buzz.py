#!/usr/bin/env python3
"""Synthesise the electrolyzer's cells working, looped.

    python tools/soundgen/electrolyzer_buzz.py [--out <ogg>]

The rectifier's harsh 100 Hz buzz (rich in harmonics) over the fizz of gas bubbling off the
electrodes. The game raises its volume with the shaft's speed. See soundkit.py for the requirements
and the looping rules.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 2.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("electrolyzer_buzz"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    buzz = pk.harmonic_tone(t, SECONDS, 100.0, tuple(1.0 / n ** 0.8 for n in range(1, 16)), phase_step=0.1)
    buzz = np.tanh(1.5 * pk.normalise(buzz))
    fizz = sk.periodic_noise(samples, 3000.0, 9500.0, seed=91) * pk.lfo(t, SECONDS, 3.0, 0.3)
    rng = np.random.default_rng(92)
    bubbles = pk.cyclic_events(samples, np.sort(rng.uniform(0, samples, 90)),
                               lambda i: pk.bubble(rng, 900.0, 1.2, length_s=0.02 + 0.02 * rng.random()))
    sk.write(args.out, 0.5 * buzz + 0.22 * fizz + 0.3 * pk.normalise(bubbles), peak=0.7)


if __name__ == "__main__":
    main()

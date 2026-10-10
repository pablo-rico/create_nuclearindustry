#!/usr/bin/env python3
"""Synthesise the tokamak magnets' looping hum.

    python tools/soundgen/magnet_hum.py [--out <ogg>]

Big coils on a rectified supply hum at twice the mains frequency with a stack of harmonics; a
slightly detuned copy of the fundamental gives the slow beat of a large transformer, and a faint
high whine comes from the power converters. The game raises pitch and volume with the field.
See soundkit.py for the requirements and the looping rules.
"""

import argparse

import numpy as np

import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("magnet_hum"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    hum = np.zeros(samples)
    # 100 Hz and its harmonics, falling off; odd ones a little stronger, like magnetostriction.
    for n, level in ((1, 1.0), (2, 0.55), (3, 0.45), (4, 0.22), (5, 0.2), (6, 0.08), (7, 0.07)):
        hum += level * sk.tone(t, SECONDS, 100.0 * n, phase=0.4 * n)
    # A detuned fundamental: a 0.5 Hz beat.
    hum += 0.45 * sk.tone(t, SECONDS, 100.5, phase=1.1)
    hum /= np.max(np.abs(hum))
    whine = 0.05 * sk.tone(t, SECONDS, 1200.0) + 0.025 * sk.tone(t, SECONDS, 2400.25)
    # A little electrical buzz on top, pulsing with the 100 Hz ripple.
    buzz = sk.periodic_noise(samples, 1500.0, 5000.0, seed=11)
    ripple = 0.5 + 0.5 * sk.tone(t, SECONDS, 100.0) ** 2
    mix = 0.9 * hum + whine + 0.035 * buzz * ripple
    sk.write(args.out, mix)


if __name__ == "__main__":
    main()

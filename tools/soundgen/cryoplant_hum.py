#!/usr/bin/env python3
"""Synthesise the cryogenic plant's looping compressor sound.

    python tools/soundgen/cryoplant_hum.py [--out <ogg>]

A motor hum, the steady thud of the compressor pistons (four strokes a second), the hiss of cold
gas through the valves and the faint whine of the turbo-expander. See soundkit.py for the
requirements and the looping rules.
"""

import argparse

import numpy as np

import soundkit as sk

SECONDS = 2.0
STROKES_PER_SECOND = 4


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("cryoplant_hum"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    motor = (sk.tone(t, SECONDS, 60.0) + 0.5 * sk.tone(t, SECONDS, 120.0, phase=0.3)
             + 0.25 * sk.tone(t, SECONDS, 180.0, phase=0.9))

    # Piston strokes: a low thud at the start of each stroke, evenly spaced so the loop is seamless.
    local = np.mod(t, 1.0 / STROKES_PER_SECOND)
    thud = np.sin(2 * np.pi * 70.0 * local) * np.exp(-local / 0.045) * np.minimum(1.0, local / 0.004)

    # Valves hiss with each stroke; the gas never quite stops.
    hiss = sk.periodic_noise(samples, 1800.0, 7000.0, seed=41)
    valve = 0.35 + 0.65 * np.exp(-((local - 0.06) / 0.05) ** 2)

    whine = sk.tone(t, SECONDS, 1850.0) + 0.4 * sk.tone(t, SECONDS, 3700.0, phase=0.2)
    mix = 0.6 * motor + 0.8 * thud + 0.12 * hiss * valve + 0.04 * whine
    sk.write(args.out, mix)


if __name__ == "__main__":
    main()

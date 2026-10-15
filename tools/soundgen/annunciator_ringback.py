#!/usr/bin/env python3
"""Synthesise an annunciator board's ringback chime (one-shot).

    python tools/soundgen/annunciator_ringback.py [--out <ogg>]

An acknowledged condition has cleared and its window waits for a reset: a single soft bell stroke,
a fundamental with a few slightly inharmonic partials dying away, so it reads as "all clear, reset
me" rather than as an alarm. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import soundkit as sk

SECONDS = 1.2
# Bell partials: ratio to the fundamental, level, decay time (s).
PARTIALS = ((1.0, 1.0, 0.55), (2.0, 0.45, 0.35), (2.76, 0.25, 0.22), (4.07, 0.12, 0.14))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("annunciator_ringback"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    bell = np.zeros(samples)
    for ratio, level, decay in PARTIALS:
        bell += level * np.sin(2 * np.pi * 880.0 * ratio * t) * np.exp(-t / decay)
    bell *= np.clip(t / 0.004, 0.0, 1.0)
    sk.write(args.out, sk.soft_lowpass(bell, 4000.0, loop=False), peak=0.55)


if __name__ == "__main__":
    main()

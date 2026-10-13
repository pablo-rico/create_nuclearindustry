#!/usr/bin/env python3
"""Synthesise a personnel interlock tripping the beam (one-shot).

    python tools/soundgen/interlock_buzzer.py [--out <ogg>]

Three harsh square-ish buzzes, the kind a safety system makes so nobody mistakes it. See
soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 1.8


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("interlock_buzzer"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    buzz = np.zeros(samples)
    for start in (0.0, 0.6, 1.2):
        tl = t - start
        on = (tl >= 0) & (tl < 0.45)
        note = np.sign(np.sin(2 * np.pi * 220.0 * tl)) * 0.6 + np.sin(2 * np.pi * 440.0 * tl) * 0.4
        buzz += note * pk.envelope(np.clip(tl, 0, None), 0.01, 0.05, 0.45) * on
    sk.write(args.out, sk.lowpass(buzz, 4000.0), peak=0.7)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Synthesise an annunciator board's warning tone (one-shot, sounded every two seconds).

    python tools/soundgen/annunciator_warning.py [--out <ogg>]

Softer than the alarm horn, so the two are told apart by ear: one rounded, falling two-note tone
(660 then 550 Hz, nearly pure) with soft edges. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 0.8


def note(t, start, length, frequency):
    local = t - start
    wave = np.sin(2 * np.pi * frequency * local) + 0.18 * np.sin(2 * np.pi * 2 * frequency * local)
    return wave * pk.envelope(np.clip(local, 0, None), 0.02, 0.09, length) * (local >= 0)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("annunciator_warning"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    tone = note(t, 0.0, 0.28, 660.0) + note(t, 0.3, 0.34, 550.0)
    sk.write(args.out, sk.soft_lowpass(tone, 1800.0, loop=False), peak=0.5)


if __name__ == "__main__":
    main()

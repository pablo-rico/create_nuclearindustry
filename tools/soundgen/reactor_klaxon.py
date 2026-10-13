#!/usr/bin/env python3
"""Synthesise the control room klaxon, looped while a scrammed core is still hot.

    python tools/soundgen/reactor_klaxon.py [--out <ogg>]

One blast of an electric horn per loop: a 180 Hz sawtooth (harmonics only up to about 2 kHz) through
a horn's formant around 650 Hz, gently saturated and low-passed so it blares low rather than
screeches, on for 0.75 s and silent for the rest of the 1.4 s loop. See soundkit.py for the requirements and
the looping rules.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 1.4
ON = 0.75


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("reactor_klaxon"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    # A band-limited sawtooth, its harmonics shaped by a formant around 650 Hz.
    horn = np.zeros(samples)
    base = sk.loop_frequency(180.0, SECONDS)
    for n in range(1, 12):
        freq = base * n
        formant = 1.0 + 2.0 * np.exp(-((freq - 650.0) / 350.0) ** 2)
        horn += formant * np.sin(2 * np.pi * freq * t + 0.2 * n) / n
    # The diaphragm's rasp: soft saturation instead of a raw square, which aliases into a screech.
    horn = np.tanh(1.6 * pk.normalise(horn))
    horn = sk.soft_lowpass(horn, 1600.0)
    blast = horn * pk.envelope(t, 0.04, 0.08, ON)
    sk.write(args.out, blast, peak=0.75)


if __name__ == "__main__":
    main()

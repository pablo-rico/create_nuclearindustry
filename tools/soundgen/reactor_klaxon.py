#!/usr/bin/env python3
"""Synthesise the control room klaxon, looped while a scrammed core is still hot.

    python tools/soundgen/reactor_klaxon.py [--out <ogg>]

One blast of an electric horn per loop: a buzzy 210 Hz sawtooth through a horn's nasal formant,
on for 0.75 s and silent for the rest of the 1.4 s loop. See soundkit.py for the requirements and
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
    # A band-limited sawtooth, its harmonics shaped by a formant around 1.1 kHz.
    horn = np.zeros(samples)
    base = sk.loop_frequency(210.0, SECONDS)
    for n in range(1, 22):
        freq = base * n
        formant = 1.0 + 2.5 * np.exp(-((freq - 1100.0) / 450.0) ** 2)
        horn += formant * np.sin(2 * np.pi * freq * t + 0.2 * n) / n
    # The diaphragm's rasp.
    horn += 0.6 * np.sign(sk.tone(t, SECONDS, 210.0)) * 0.3
    horn = pk.normalise(horn)
    blast = horn * pk.envelope(t, 0.03, 0.06, ON)
    sk.write(args.out, blast)


if __name__ == "__main__":
    main()

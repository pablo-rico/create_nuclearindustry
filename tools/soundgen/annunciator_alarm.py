#!/usr/bin/env python3
"""Synthesise an annunciator board's alarm horn (one-shot, sounded once a second).

    python tools/soundgen/annunciator_alarm.py [--out <ogg>]

The electronic horn of a control room annunciator: two quick, hard pulses of a reedy tone (a
squarish 740 Hz with its fifth), urgent from the rhythm and the edge, low-passed so it never turns
shrill. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 0.9
PULSE = 0.22


def pulse(t, start):
    local = t - start
    on = (local >= 0) & (local < PULSE)
    reed = np.tanh(2.5 * np.sin(2 * np.pi * 740.0 * local)) + 0.35 * np.sin(2 * np.pi * 1110.0 * local)
    return reed * pk.envelope(np.clip(local, 0, None), 0.008, 0.03, PULSE) * on


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("annunciator_alarm"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    horn = pulse(t, 0.0) + pulse(t, 0.3)
    sk.write(args.out, sk.soft_lowpass(horn, 3200.0, loop=False), peak=0.65)


if __name__ == "__main__":
    main()

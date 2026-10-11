#!/usr/bin/env python3
"""Synthesise the over-temperature alarm, looped while the core is in the warning zone.

    python tools/soundgen/reactor_overtemp_alarm.py [--out <ogg>]

An urgent electronic beeper: two quick square-ish beeps, a high one and a lower one, then a short
pause, in a 1 s loop. The game raises its pitch as the core nears meltdown. See soundkit.py for the
requirements and the looping rules.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 1.0


def beep(t, start, length, frequency):
    local = t - start
    # odd harmonics only: a soft square wave
    wave = sum(np.sin(2 * np.pi * frequency * n * local) / n for n in (1, 3, 5, 7))
    return wave * pk.envelope(local, 0.004, 0.01, length) * (local >= 0)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("reactor_overtemp_alarm"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    alarm = beep(t, 0.0, 0.18, 1320.0) + beep(t, 0.25, 0.18, 990.0)
    sk.write(args.out, pk.normalise(alarm), peak=0.7)


if __name__ == "__main__":
    main()

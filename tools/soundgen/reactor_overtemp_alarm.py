#!/usr/bin/env python3
"""Synthesise the over-temperature alarm, looped while the core is in the warning zone.

    python tools/soundgen/reactor_overtemp_alarm.py [--out <ogg>]

An urgent but mellow two-tone chime: two rounded notes a fourth apart (520 and 390 Hz, nearly pure,
with soft edges) then a pause, in a 1 s loop, low-passed so no shrill partial survives. Urgency comes
from the rhythm, not from a piercing pitch. The game raises its pitch a little as the core nears
meltdown. See soundkit.py for the requirements and the looping rules.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 1.0


def chime(t, start, length, frequency):
    local = t - start
    # A sine with a touch of second and third harmonic: round, not buzzy.
    wave = (np.sin(2 * np.pi * frequency * local) + 0.25 * np.sin(2 * np.pi * 2 * frequency * local)
            + 0.08 * np.sin(2 * np.pi * 3 * frequency * local))
    # A soft onset and a decaying tail instead of a hard click on and off.
    swell = pk.envelope(local, 0.02, 0.08, length) * np.exp(-np.clip(local, 0.0, None) / 0.35)
    return wave * swell * (local >= 0)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("reactor_overtemp_alarm"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    alarm = chime(t, 0.0, 0.26, 520.0) + chime(t, 0.32, 0.26, 390.0)
    sk.write(args.out, sk.soft_lowpass(alarm, 1400.0), peak=0.6)


if __name__ == "__main__":
    main()

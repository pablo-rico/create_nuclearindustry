#!/usr/bin/env python3
"""Synthesise the running fission core's looping hum.

    python tools/soundgen/reactor_hum.py [--out <ogg>]

A deep electrical hum from the coolant pumps' motors and the core's instrumentation (60 Hz and its
harmonics, with a slow transformer beat), a low rumble of water forced through the channels, and a
faint high whine. The game raises pitch and volume with power. See soundkit.py for the requirements
and the looping rules.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("reactor_hum"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    hum = pk.harmonic_tone(t, SECONDS, 60.0, (1.0, 0.7, 0.45, 0.3, 0.12, 0.08), phase_step=0.5)
    # A detuned fundamental beats slowly against the hum, like a big transformer.
    hum += 0.5 * sk.tone(t, SECONDS, 60.25, phase=1.3)
    hum = pk.normalise(hum)
    # Water rushing through the core: dark, rumbling noise that swells gently.
    rumble = sk.periodic_noise(samples, 25.0, 400.0, seed=101, tilt=-1.2) * pk.lfo(t, SECONDS, 0.5, 0.25)
    whine = 0.04 * sk.tone(t, SECONDS, 1750.0) + 0.02 * sk.tone(t, SECONDS, 3500.0)
    mix = 0.75 * hum + 0.45 * rumble + whine
    sk.write(args.out, mix)


if __name__ == "__main__":
    main()

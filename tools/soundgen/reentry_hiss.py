#!/usr/bin/env python3
"""Synthesise a warhead tearing back down through the air, looped.

    python tools/soundgen/reentry_hiss.py [--out <ogg>]

A high, ragged roar of air torn apart round the warhead: bright rushing noise with a howl through it that wavers, and a
low rumble of its wake. The game raises its pitch with the speed. See soundkit.py for the requirements and the looping
rules.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 3.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("reentry_hiss"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    tear = sk.periodic_noise(samples, 400.0, 7000.0, seed=251, tilt=-0.35)
    wake = sk.periodic_noise(samples, 40.0, 500.0, seed=252, tilt=-1.0)
    # The howl: a narrow band of noise round a pitch that wavers, made by ring-modulating low noise up.
    wobble = 2 * np.pi * np.cumsum(sk.loop_frequency(820.0, SECONDS) + 40.0 * sk.tone(t, SECONDS, 1.0)) / sk.RATE
    howl = sk.periodic_noise(samples, 5.0, 80.0, seed=253) * np.sin(wobble)
    flutter = pk.lfo(t, SECONDS, 11.0, 0.15) * pk.lfo(t, SECONDS, 3.0, 0.1, phase=0.9)
    mix = (0.8 * tear + 0.45 * wake + 0.35 * pk.normalise(howl)) * flutter
    sk.write(args.out, sk.soft_lowpass(mix, 6500.0, order=2), peak=0.8)


if __name__ == "__main__":
    main()

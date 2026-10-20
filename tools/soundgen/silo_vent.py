#!/usr/bin/env python3
"""Synthesise a silo venting steam in the last seconds of its countdown.

    python tools/soundgen/silo_vent.py [--out <ogg>]

Valves banging open, then a hard rush of steam out of the shaft with a deep rumble in the concrete under it, holding
and dying away as the engines light. Three seconds, as the vents are open for. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import rocketkit as rk
import soundkit as sk

SECONDS = 3.2


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("silo_vent"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    clank = rk.band_noise(samples, 300.0, 5000.0, seed=271) * rk.decay(t, 0.02)
    clank += 0.6 * np.roll(clank, int(0.08 * sk.RATE)) * (t > 0.08)
    rush = rk.band_noise(samples, 500.0, 6000.0, seed=272, tilt=-0.5)
    roar = rk.band_noise(samples, 30.0, 500.0, seed=273, tilt=-1.0)
    swell = np.clip((t - 0.05) / 0.35, 0, 1) * np.clip((SECONDS - t) / 0.8, 0, 1)
    flutter = 1 + 0.08 * np.sin(2 * np.pi * 5.0 * t) + 0.05 * np.sin(2 * np.pi * 13.0 * t + 1.0)
    mix = 0.5 * clank + (0.8 * rush + 0.55 * roar) * swell * flutter
    sk.write(args.out, sk.soft_lowpass(mix, 5000.0, order=3, loop=False), peak=0.8)


if __name__ == "__main__":
    main()

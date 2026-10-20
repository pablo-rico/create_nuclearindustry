#!/usr/bin/env python3
"""Synthesise a spent stage (or an inert warhead) crashing into the ground.

    python tools/soundgen/debris_impact.py [--out <ogg>]

A heavy thud into the ground, the crunch and clatter of the tank crumpling (inharmonic metal partials over gravel), and
earth and fragments raining back down. The game plays an inert warhead's impact off it, lower. See soundkit.py for the
requirements.
"""

import argparse

import numpy as np

import rocketkit as rk
import soundkit as sk

SECONDS = 3.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("debris_impact"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    thud = rk.thump(t, 42.0, 0.35, drop=0.35) + 0.6 * rk.thump(t, 85.0, 0.12)
    crunch = rk.band_noise(samples, 150.0, 5000.0, seed=261, tilt=-0.6) * rk.decay(t, 0.12)
    metal = np.zeros(samples)
    rng = np.random.default_rng(262)
    for f in rng.uniform(180.0, 2600.0, 9):
        metal += np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28)) * rk.decay(t, rng.uniform(0.08, 0.4)) * rng.uniform(0.3, 1.0)
    rain = sk.soft_lowpass(rk.crackle(samples, 70, seed=263, length_s=0.006, loop=False), 4000.0, order=2, loop=False)
    rain *= np.clip((t - 0.25) / 0.3, 0, 1) * rk.decay(np.maximum(0, t - 0.3), 0.8)
    mix = 1.2 * thud + 0.7 * crunch + 0.25 * metal / max(1e-9, np.max(np.abs(metal))) + 0.45 * rain / max(1e-9, np.max(np.abs(rain)))
    sk.write(args.out, sk.soft_lowpass(mix, 6000.0, order=2, loop=False), peak=0.9)


if __name__ == "__main__":
    main()

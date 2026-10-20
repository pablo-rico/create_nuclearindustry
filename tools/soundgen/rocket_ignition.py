#!/usr/bin/env python3
"""Synthesise a rocket's engines lighting at lift-off.

    python tools/soundgen/rocket_ignition.py [--out <ogg>]

A deep whump as the propellant catches, a sharp crack, then the roar building up with its crackle over a second and a
half and fading out as the looped roar takes over. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import rocketkit as rk
import soundkit as sk

SECONDS = 4.5


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("rocket_ignition"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    whump = rk.thump(t, 48.0, 0.45, drop=0.4) + 0.5 * rk.thump(t, 95.0, 0.2, drop=0.5)
    crack = rk.band_noise(samples, 200.0, 7000.0, seed=221, tilt=-0.3) * rk.decay(t, 0.05)
    roar = 0.9 * rk.band_noise(samples, 25.0, 3000.0, seed=222, tilt=-0.9) + 0.25 * rk.band_noise(samples, 1500.0, 8000.0, seed=223)
    pops = sk.soft_lowpass(rk.crackle(samples, 120, seed=224, length_s=0.005, loop=False), 6000.0, order=2, loop=False)
    build = np.clip((t - 0.08) / 1.4, 0.0, 1.0) ** 1.5
    tail = np.clip((SECONDS - t) / 1.6, 0.0, 1.0)
    mix = 1.1 * whump + 0.6 * crack + (roar + 0.5 * pops / max(1e-9, np.max(np.abs(pops)))) * build * tail
    sk.write(args.out, sk.soft_lowpass(mix, 7000.0, order=2, loop=False), peak=0.9)


if __name__ == "__main__":
    main()

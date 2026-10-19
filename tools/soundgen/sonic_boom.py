#!/usr/bin/env python3
"""Synthesise the sonic boom of a warhead streaking past.

    python tools/soundgen/sonic_boom.py [--out <ogg>]

The N-wave of a shock passing: a sudden jump in pressure, a fall, and a second jump a tenth of a second later (the
double crack), each a hard edge low-passed by the air, then the boom's rumble rolling away. See soundkit.py for the
requirements.
"""

import argparse

import numpy as np

import rocketkit as rk
import soundkit as sk

SECONDS = 2.5
GAP = 0.12


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("sonic_boom"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    # The N: up at 0, falling straight down to the second edge, then back to rest.
    n_wave = np.where(t < GAP, 1.0 - 2.0 * t / GAP, 0.0) * (t >= 0.01)
    n_wave = np.roll(n_wave, int(0.01 * sk.RATE))
    edges = sk.soft_lowpass(n_wave, 2500.0, order=2, loop=False)
    snap = rk.band_noise(samples, 800.0, 8000.0, seed=241) * (rk.decay(t, 0.01) + 0.7 * rk.decay(np.maximum(0, t - GAP - 0.01), 0.01)
                                                                * (t > GAP + 0.01))
    rumble = rk.band_noise(samples, 20.0, 300.0, seed=242, tilt=-1.0) * np.clip(t / 0.05, 0, 1) * rk.decay(t, 0.7)
    mix = 1.0 * edges + 0.35 * snap + 0.7 * rumble
    sk.write(args.out, mix, peak=0.95)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Synthesise the wind rushing back in towards a rising fireball (10 s, streamed).

    python tools/soundgen/nuke_return_wind.py [--out <ogg>]

After the shock has passed outward, the air behind it is thin and the fireball is rising: the wind turns and rushes back
in, building over a few seconds to a gusting roar, then dying away. Broadband noise, the low roar following the strength
of the wind and the hiss on top following it more steeply, with dust and grit rattling past at the peak. See soundkit.py
for the requirements.
"""

import argparse

import numpy as np

import blastkit as bk
import rocketkit as rk
import soundkit as sk

SECONDS = 10.0
PEAK = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("nuke_return_wind"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    rise = np.clip(t / PEAK, 0.0, 1.0) ** 2
    fall = np.where(t > PEAK, np.exp(-(t - PEAK) / 2.2), 1.0)
    wind = rise * fall
    low = rk.band_noise(samples, 40.0, 700.0, 2721, tilt=-0.8) * wind * bk.rolling(samples, 2722, 0.8, 0.35)
    high = rk.band_noise(samples, 500.0, 6000.0, 2723, tilt=-0.6) * wind ** 2 * bk.rolling(samples, 2724, 1.5, 0.45)
    grit = bk.lowpass(bk.impacts(samples, 2725, 2.5, 4.5, 8.0, 25.0, low=800.0, high=5000.0, thuds=0.0), 6000.0)
    mix = 1.0 * low + 0.55 * high + 0.12 * bk.normalise(grit)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=1.0), peak=0.85)


if __name__ == "__main__":
    main()

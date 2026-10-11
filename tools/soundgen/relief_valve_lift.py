#!/usr/bin/env python3
"""Synthesise the pressurizer's relief valve lifting (one-shot).

    python tools/soundgen/relief_valve_lift.py [--out <ogg>]

The valve disc slams open with a metallic pop and steam bursts out: a violent hiss with a shrill
whistle that settles as the pressure drops. While it stays open the steam hiss loop carries on.
See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 2.2


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("relief_valve_lift"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    attack = np.minimum(1.0, t / 0.001)
    pop = sk.one_shot_noise(samples, seed=71) * np.exp(-t / 0.015) * attack
    pop += 0.8 * np.sin(2 * np.pi * 620.0 * t) * np.exp(-t / 0.06) * attack
    burst = sk.lowpass(sk.one_shot_noise(samples, seed=72), 10000.0)
    burst = burst - sk.lowpass(burst, 900.0)
    burst *= np.minimum(1.0, t / 0.03) * (0.35 + 0.65 * np.exp(-t / 0.5))
    # The whistle slides down as the pressure falls.
    freq = 2100.0 + 600.0 * np.exp(-t / 0.4)
    whistle = np.sin(2 * np.pi * np.cumsum(freq) / sk.RATE) * pk.envelope(t, 0.05, 0.8, SECONDS) * 0.25
    mix = 0.9 * pop + 0.8 * burst + whistle
    fade = np.clip((SECONDS - t) / 0.6, 0.0, 1.0)
    sk.write(args.out, mix * fade, peak=0.9)


if __name__ == "__main__":
    main()

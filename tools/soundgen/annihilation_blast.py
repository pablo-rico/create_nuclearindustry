#!/usr/bin/env python3
"""Synthesise antimatter annihilating all at once (one-shot).

    python tools/soundgen/annihilation_blast.py [--out <ogg>]

A dry, instant crack - there is no fireball to build up - and an electric zap diving through the
spectrum as the gamma flash hits everything, then a deep thump and a short rumble. Cleaner and
shorter than a nuclear explosion: no long roar of a burning cloud. See soundkit.py for the
requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("annihilation_blast"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    noise = sk.one_shot_noise(samples, seed=1320)
    crack = (noise - sk.soft_lowpass(noise, 1500.0, loop=False)) * np.exp(-t / 0.012) * np.minimum(1.0, t / 0.0005)
    zap_f = 200.0 + 3000.0 * np.exp(-t / 0.05)
    zap = np.sin(2 * np.pi * np.cumsum(zap_f) / sk.RATE) * np.exp(-t / 0.12)
    thump_f = 38.0 + 50.0 * np.exp(-t / 0.08)
    thump = np.sin(2 * np.pi * np.cumsum(thump_f) / sk.RATE) * np.exp(-t / 0.5) * np.minimum(1.0, t / 0.004)
    rumble = pk.normalise(sk.soft_lowpass(sk.one_shot_noise(samples, seed=1321), 180.0, loop=False)) \
        * np.exp(-t / 1.1) * np.minimum(1.0, t / 0.05)
    sizzle = (noise - sk.soft_lowpass(noise, 4000.0, loop=False)) * np.exp(-t / 0.6) * 0.5
    fade = np.clip((SECONDS - t) / 0.4, 0.0, 1.0)
    mix = 1.0 * crack + 0.5 * zap + 1.1 * thump + 0.8 * rumble + 0.15 * sizzle
    sk.write(args.out, sk.soft_lowpass(mix, 7000.0, loop=False) * fade, peak=0.9)


if __name__ == "__main__":
    main()

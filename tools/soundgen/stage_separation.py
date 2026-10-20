#!/usr/bin/env python3
"""Synthesise a rocket stage falling away.

    python tools/soundgen/stage_separation.py [--out <ogg>]

The pyrotechnic bolts firing (a hard double bang with a metallic ring of the interstage), then the rush of the spent stage
dropping back and a dull clunk. The game plays the payload's separation off it, higher. See soundkit.py for the
requirements.
"""

import argparse

import numpy as np

import rocketkit as rk
import soundkit as sk

SECONDS = 2.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("stage_separation"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    bang = rk.band_noise(samples, 80.0, 6000.0, seed=231, tilt=-0.5) * rk.decay(t, 0.03)
    second = np.roll(bang, int(0.045 * sk.RATE)) * 0.7
    second[:int(0.045 * sk.RATE)] = 0
    ring = np.zeros(samples)
    for f, a in ((310.0, 1.0), (743.0, 0.6), (1190.0, 0.4), (1873.0, 0.25)):
        ring += a * np.sin(2 * np.pi * f * t) * rk.decay(t, 0.35)
    whoosh = rk.band_noise(samples, 150.0, 2500.0, seed=232, tilt=-0.7) * np.clip((t - 0.05) / 0.25, 0, 1) * rk.decay(t, 0.55)
    clunk = rk.thump(np.maximum(0, t - 0.12), 70.0, 0.15) * (t > 0.12)
    mix = 1.0 * bang + second + 0.25 * ring + 0.55 * whoosh + 0.6 * clunk
    sk.write(args.out, sk.soft_lowpass(mix, 7000.0, order=2, loop=False), peak=0.9)


if __name__ == "__main__":
    main()

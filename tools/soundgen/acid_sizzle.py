#!/usr/bin/env python3
"""Synthesise acid eating into items in a fan's air current (one-shot).

    python tools/soundgen/acid_sizzle.py [--out <ogg>]

A bright hiss that swells and fades, broken up by fizzing: dozens of tiny bubbles popping, each a
short click of high noise, and a few wetter pops lower down. Played now and then while the acid
leaching treatment works on an item, so it is short and never loud. See soundkit.py for the
requirements.
"""

import argparse

import numpy as np

import soundkit as sk

SECONDS = 1.2


def bubbles(samples, t, rate, seed, length, cutoff):
    """Pops at random times: each a burst of band-limited noise with a fast decay."""
    rng = np.random.default_rng(seed)
    out = np.zeros(samples)
    count = rng.poisson(rate * SECONDS)
    burst = sk.lowpass(sk.one_shot_noise(samples, seed + 1), cutoff)
    for start in rng.uniform(0.0, SECONDS - 0.05, count):
        i = int(start * sk.RATE)
        n = min(samples - i, int(length * sk.RATE * 6))
        env = np.exp(-np.arange(n) / (length * sk.RATE)) * rng.uniform(0.3, 1.0)
        out[i:i + n] += burst[i:i + n] * env
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("acid_sizzle"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    # The hiss: bright noise with the lows taken out, swelling in and dying away.
    noise = sk.one_shot_noise(samples, seed=71)
    hiss = noise - sk.lowpass(noise, 2500.0)
    swell = np.minimum(1.0, t / 0.08) * np.exp(-t / 0.55)
    # The fizz: many tiny pops, and a few wetter ones.
    fizz = bubbles(samples, t, rate=90, seed=72, length=0.004, cutoff=9000.0)
    pops = bubbles(samples, t, rate=12, seed=73, length=0.012, cutoff=1800.0)
    # A slow flutter makes the hiss crackle rather than stay steady.
    flutter = 0.7 + 0.3 * np.abs(sk.lowpass(sk.one_shot_noise(samples, seed=74), 30.0)) * 4.0
    mix = 0.45 * hiss * swell * np.clip(flutter, 0.0, 1.5) + 0.9 * fizz * swell + 0.6 * pops * swell
    fade = np.clip((SECONDS - t) / 0.2, 0.0, 1.0)
    sk.write(args.out, mix * fade, peak=0.7)


if __name__ == "__main__":
    main()

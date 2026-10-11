#!/usr/bin/env python3
"""Synthesise items crackling in a fan's air current past a radioactive block (one-shot).

    python tools/soundgen/irradiation_crackle.py [--out <ogg>]

What a player hears near strong radiation is a Geiger counter: sharp dry clicks at random, in a
fast burst that thins out, with a faint electric fizz under them where the air glows. Played now
and then while the irradiation treatment works on an item. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import soundkit as sk

SECONDS = 1.0


def clicks(samples, t, seed):
    """Geiger clicks: a rate that falls through the sound, each click a ringing 2.6 kHz blip."""
    rng = np.random.default_rng(seed)
    out = np.zeros(samples)
    click_len = int(0.006 * sk.RATE)
    k = np.arange(click_len) / sk.RATE
    shape = np.sin(2 * np.pi * 2600.0 * k) * np.exp(-k / 0.0012)
    shape[0] = 1.0
    time = 0.0
    while True:
        rate = 60.0 * np.exp(-time / 0.45) + 8.0
        time += rng.exponential(1.0 / rate)
        if time >= SECONDS - 0.02:
            break
        i = int(time * sk.RATE)
        n = min(click_len, samples - i)
        out[i:i + n] += shape[:n] * rng.uniform(0.5, 1.0)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("irradiation_crackle"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    geiger = clicks(samples, t, seed=81)
    # The fizz: thin high noise, flickering.
    noise = sk.one_shot_noise(samples, seed=82)
    fizz = (noise - sk.lowpass(noise, 4000.0)) * (0.5 + 0.5 * np.sin(2 * np.pi * 7.0 * t) ** 2)
    envelope = np.minimum(1.0, t / 0.01) * np.exp(-t / 0.6)
    mix = geiger + 0.12 * fizz * envelope
    fade = np.clip((SECONDS - t) / 0.15, 0.0, 1.0)
    sk.write(args.out, mix * fade, peak=0.6)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Synthesise the crack of plasma breakdown (one-shot).

    python tools/soundgen/plasma_breakdown.py [--out <ogg>]

The central solenoid fires and the gas breaks down: an electric crack, a falling zap as the
current rings down, a heavy thump through the structure and a short hiss, low-passed so the crack
snaps without stinging. See soundkit.py for the
requirements.
"""

import argparse

import numpy as np

import soundkit as sk

SECONDS = 1.8


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("plasma_breakdown"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    attack = np.minimum(1.0, t / 0.002)

    # The crack: a burst of bright noise that dies in a few tens of milliseconds.
    crack = sk.soft_lowpass(sk.one_shot_noise(samples, seed=31), 2500.0, loop=False)
    crack = crack / np.max(np.abs(crack)) * np.exp(-t / 0.03) * attack

    # The zap: a chirp falling from 900 Hz to 120 Hz, ringing down over half a second.
    f0, f1, sweep = 900.0, 120.0, 0.5
    frequency = f1 + (f0 - f1) * np.exp(-t / (sweep / 4))
    phase = 2 * np.pi * np.cumsum(frequency) / sk.RATE
    zap = np.tanh(1.5 * np.sin(phase)) * np.exp(-t / 0.22) * attack

    # The thump: a low sine with a dropping pitch.
    thump_freq = 38.0 + 40.0 * np.exp(-t / 0.08)
    thump = np.sin(2 * np.pi * np.cumsum(thump_freq) / sk.RATE) * np.exp(-t / 0.35) * np.minimum(1.0, t / 0.008)

    # The hiss of the fresh plasma, fading out.
    hiss = sk.lowpass(sk.one_shot_noise(samples, seed=32), 2500.0) * np.exp(-t / 0.5) * np.minimum(1.0, t / 0.05)

    mix = 0.9 * crack + 0.55 * zap + 0.9 * thump + 0.18 * hiss
    mix = sk.soft_lowpass(mix, 2500.0, loop=False)
    # Fade the tail to silence.
    fade = np.clip((SECONDS - t) / 0.3, 0.0, 1.0)
    sk.write(args.out, mix * fade, peak=0.9)


if __name__ == "__main__":
    main()

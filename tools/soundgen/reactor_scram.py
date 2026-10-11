#!/usr/bin/env python3
"""Synthesise the SCRAM: the control rods dropping into the core (one-shot).

    python tools/soundgen/reactor_scram.py [--out <ogg>]

The latches let go with a metallic clank, the rods rattle down their channels and land with a
heavy thud through the vessel; a hydraulic sigh follows. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 2.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("reactor_scram"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    attack = np.minimum(1.0, t / 0.001)

    # The latches: an inharmonic metal clank.
    clank = np.zeros(samples)
    for freq, level, decay in ((340.0, 1.0, 0.18), (910.0, 0.6, 0.12), (1530.0, 0.4, 0.08), (2410.0, 0.25, 0.05)):
        clank += level * np.sin(2 * np.pi * freq * t) * np.exp(-t / decay)
    clank *= attack

    # The rods sliding down: a rattle of small knocks speeding up, between 0.05 and 0.55 s.
    rng = np.random.default_rng(41)
    rattle = np.zeros(samples)
    when = 0.05
    gap = 0.06
    while when < 0.55:
        start = int(when * sk.RATE)
        n = int(0.02 * sk.RATE)
        knock = rng.uniform(-1, 1, n) * np.exp(-np.arange(n) / (0.004 * sk.RATE))
        rattle[start:start + n] += knock * (0.4 + 0.6 * rng.random())
        when += gap
        gap = max(0.018, gap * 0.85)
    rattle = sk.lowpass(rattle, 3500.0)

    # Landing: a heavy thud at 0.58 s, its pitch dropping.
    land = 0.58
    tl = np.clip(t - land, 0.0, None)
    on = t >= land
    thud_freq = 32.0 + 45.0 * np.exp(-tl / 0.06)
    thud = np.sin(2 * np.pi * np.cumsum(thud_freq * on) / sk.RATE) * np.exp(-tl / 0.4) * np.minimum(1.0, tl / 0.004) * on
    ring = np.sin(2 * np.pi * 220.0 * tl) * np.exp(-tl / 0.25) * on

    # The hydraulic sigh, after the landing.
    sigh = sk.lowpass(sk.one_shot_noise(samples, seed=42), 2500.0) * pk.envelope(tl, 0.15, 0.7, 1.2) * on

    mix = 0.7 * clank + 0.55 * rattle + 1.0 * thud + 0.25 * ring + 0.12 * sigh
    fade = np.clip((SECONDS - t) / 0.25, 0.0, 1.0)
    sk.write(args.out, mix * fade, peak=0.9)


if __name__ == "__main__":
    main()

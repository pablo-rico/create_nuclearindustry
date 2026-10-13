#!/usr/bin/env python3
"""Synthesise the beam being kicked into the dump (one-shot).

    python tools/soundgen/beam_dump.py [--out <ogg>]

The kicker magnets fire with a hard electric snap; the dump answers with a deep, muffled thump and
the RF dies away in a falling whine. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import soundkit as sk

SECONDS = 1.5


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("beam_dump"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    snap = sk.lowpass(sk.one_shot_noise(samples, seed=331), 9000.0) * np.exp(-t / 0.02) * np.minimum(1.0, t / 0.0005)
    thump_freq = 45.0 + 60.0 * np.exp(-t / 0.05)
    thump = np.sin(2 * np.pi * np.cumsum(thump_freq) / sk.RATE) * np.exp(-t / 0.35) * np.minimum(1.0, t / 0.003)
    whine_freq = 2400.0 * np.exp(-t / 0.4) + 300.0
    whine = np.sin(2 * np.pi * np.cumsum(whine_freq) / sk.RATE) * np.exp(-t / 0.5)
    fade = np.clip((SECONDS - t) / 0.2, 0.0, 1.0)
    sk.write(args.out, (0.6 * snap + 1.0 * thump + 0.2 * whine) * fade, peak=0.85)


if __name__ == "__main__":
    main()

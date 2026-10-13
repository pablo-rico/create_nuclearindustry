#!/usr/bin/env python3
"""Synthesise hot primary coolant churning through an output port, looped.

    python tools/soundgen/coolant_bubbling.py [--out <ogg>]

A dense stream of small bubbles over a low, watery rumble. The bubbles are placed round the loop
and wrap at its end, so it repeats without a seam. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 3.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("coolant_bubbling"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    rng = np.random.default_rng(61)
    starts = np.sort(rng.uniform(0, samples, 150))
    bubbles = pk.cyclic_events(samples, starts, lambda i: pk.bubble(rng, 320.0, 1.6, length_s=0.04 + 0.05 * rng.random()))
    flow = sk.periodic_noise(samples, 40.0, 700.0, seed=62, tilt=-1.0) * pk.lfo(t, SECONDS, 1.0, 0.2)
    sk.write(args.out, 0.8 * pk.normalise(bubbles) + 0.45 * flow, peak=0.75)


if __name__ == "__main__":
    main()

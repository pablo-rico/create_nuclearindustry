#!/usr/bin/env python3
"""Synthesise the ringing in a deafened player's ears (4 s seamless loop, quiet).

    python tools/soundgen/tinnitus.py [--out <ogg>]

The whistle after a blast: a high, near-pure tone, here about 5.2 kHz, wavering very slightly in pitch and level, with a
faint second tone a couple of hertz off beating against it and a thin hiss under it. Every tone and wobble completes whole
cycles in the loop and the hiss is built periodic, so it loops without a seam. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("tinnitus"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    f = sk.loop_frequency(5200.0, SECONDS)
    wobble = sk.loop_frequency(0.75, SECONDS)
    # Phase modulation by a whole number of wobble cycles keeps the loop seamless: about +/- 2 Hz of drift.
    ring = np.sin(2 * np.pi * f * t + 2.5 * np.sin(2 * np.pi * wobble * t))
    beat = sk.tone(t, SECONDS, f + 2.0, phase=1.1)
    level = pk.lfo(t, SECONDS, 0.5, 0.06)
    hiss = sk.periodic_noise(samples, 3000.0, 9000.0, seed=2951)
    mix = (ring + 0.12 * beat) * level + 0.05 * hiss
    sk.write(args.out, mix, peak=0.07)


if __name__ == "__main__":
    main()

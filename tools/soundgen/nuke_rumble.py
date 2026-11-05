#!/usr/bin/env python3
"""Synthesise the long rolling rumble after a nuclear boom (25 s, streamed).

    python tools/soundgen/nuke_rumble.py [--out <ogg>]

The shock does not arrive once: it comes back off every hill, valley wall and building for tens of seconds, each echo
later, weaker and duller than the last, over the low, swelling roar of the rising fireball. The echoes are Poisson-timed
copies of a short boom; the roar is deep noise rolling in and out. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import blastkit as bk
import soundkit as sk

SECONDS = 25.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("nuke_rumble"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    # One echo: a short, rounded boom.
    n = int(0.9 * sk.RATE)
    source = (bk.n_wave(n, 0.0, 0.35, rise_s=0.006, asym=0.7) + 0.9 * bk.sub_thump(n, 0.0, 42.0, 26.0, 0.35, attack=0.01)
              + 0.7 * bk.burst(n, 2701, 20.0, 400.0, 0.0, 0.01, 0.25, tilt=-1.0))
    echoes = bk.reflections(samples, 2702, source, start=0.3, rate=3.0, span=21.0, gain=0.9, decay=7.0, dull=(900.0, 140.0))
    base = bk.burst(samples, 2703, 18.0, 260.0, 0.0, 0.8, 8.0, tilt=-1.2, hold=1.5) * bk.rolling(samples, 2704, 0.6, 0.6)
    roar = bk.burst(samples, 2705, 80.0, 1200.0, 0.0, 0.5, 4.0, tilt=-0.8) * bk.rolling(samples, 2706, 1.5, 0.5)
    mix = 0.8 * bk.normalise(base) + 1.0 * bk.normalise(echoes) + 0.3 * roar
    mix = bk.reverb(mix, 4.0, 2707, wet=0.4, damp=600.0)
    # Fade in over the first half second: the boom before it carries the attack.
    mix *= np.clip(t / 0.5, 0.0, 1.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=4.0, cutoff=2000.0), peak=0.9)


if __name__ == "__main__":
    main()

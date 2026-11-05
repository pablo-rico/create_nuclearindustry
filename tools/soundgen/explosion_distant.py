#!/usr/bin/env python3
"""Synthesise a conventional blast heard from far off: a low thud and its rolling echo (4 s).

    python tools/soundgen/explosion_distant.py [--out <ogg>]

At a distance the crack is gone: the air has taken the highs, leaving a dull thud and the echoes of it coming back off the
land, rolling away. Nothing above a few hundred hertz. See soundkit.py for the requirements.
"""

import argparse

import blastkit as bk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("explosion_distant"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    n = int(1.0 * sk.RATE)

    def thud(m, seed):
        return (bk.n_wave(m, 0.02, 0.06, rise_s=0.004, asym=0.6) + 0.9 * bk.sub_thump(m, 0.02, 60.0, 38.0, 0.5, attack=0.01)
                + 0.7 * bk.burst(m, seed, 30.0, 300.0, 0.02, 0.01, 0.4, tilt=-1.0))

    first = thud(samples, 2841)
    echoes = bk.reflections(samples, 2842, thud(n, 2843), start=0.25, rate=2.5, span=3.0, gain=0.6, decay=1.0,
                            dull=(300.0, 120.0))
    mix = bk.normalise(first) + 0.8 * bk.normalise(echoes)
    mix = bk.reverb(bk.lowpass(mix, 350.0, order=3), 2.5, 2844, wet=0.35, damp=300.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=0.8), peak=0.9)


if __name__ == "__main__":
    main()

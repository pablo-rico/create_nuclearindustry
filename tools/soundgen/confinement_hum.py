#!/usr/bin/env python3
"""Synthesise a magnetic confinement vessel's field, looped.

    python tools/soundgen/confinement_hum.py [--out <ogg>]

A soft, glassy hum: a pure low tone and its fifth, shimmering slowly, with a whisper of the
cryogenics. Quieter than the accelerator's magnets: the vessel is meant to sit in a store room.
See soundkit.py for the requirements and the looping rules.
"""

import argparse

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("confinement_hum"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    tone = sk.tone(t, SECONDS, 140.0) + 0.5 * sk.tone(t, SECONDS, 210.0) + 0.25 * sk.tone(t, SECONDS, 560.0)
    shimmer = pk.lfo(t, SECONDS, 0.5, 0.2) * pk.lfo(t, SECONDS, 1.25, 0.1)
    air = 0.06 * sk.periodic_noise(samples, 3000, 8000, seed=341)
    sk.write(args.out, pk.normalise(tone) * shimmer + air, peak=0.5)


if __name__ == "__main__":
    main()

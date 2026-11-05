#!/usr/bin/env python3
"""Synthesise the ground's tremor, felt before a nuclear shock arrives (6 s).

    python tools/soundgen/seismic_rumble.py [--out <ogg>]

The ground carries the detonation's energy far faster than the air: a very low, uneven tremor, 20 to 60 Hz, more a
pressure in the chest than a sound, with a faint trace higher up so small speakers still give a hint of it. Subtle. See
soundkit.py for the requirements.
"""

import argparse

import blastkit as bk
import rocketkit as rk
import soundkit as sk

SECONDS = 6.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("seismic_rumble"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    env = bk.envelope(t, 0.0, 0.6, 1.4, hold=1.2) * bk.rolling(samples, 2731, 1.5, 0.4)
    tremor = rk.band_noise(samples, 20.0, 60.0, 2732, tilt=-0.5) * env
    trace = rk.band_noise(samples, 60.0, 110.0, 2733) * env
    mix = tremor + 0.15 * trace
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=1.0), peak=0.7)


if __name__ == "__main__":
    main()

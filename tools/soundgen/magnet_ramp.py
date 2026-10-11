#!/usr/bin/env python3
"""Synthesise the accelerator magnets' field, looped; the game raises its pitch with the beam energy.

    python tools/soundgen/magnet_ramp.py [--out <ogg>]

A low electromagnetic drone with a slow beat between two close partials, and a faint buzz of the
laminations. See soundkit.py for the requirements and the looping rules.
"""

import argparse

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("magnet_ramp"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    drone = sk.tone(t, SECONDS, 180.0) + 0.8 * sk.tone(t, SECONDS, 181.5) + 0.35 * sk.tone(t, SECONDS, 360.0)
    buzz = 0.15 * pk.harmonic_tone(t, SECONDS, 300.0, (0.0, 0.4, 0.0, 0.3, 0.0, 0.2))
    air = 0.08 * sk.periodic_noise(samples, 200, 2000, seed=311, tilt=-1.0)
    sk.write(args.out, pk.normalise(drone) + buzz + air, peak=0.6)


if __name__ == "__main__":
    main()

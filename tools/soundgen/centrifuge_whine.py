#!/usr/bin/env python3
"""Synthesise a gas centrifuge spinning, looped.

    python tools/soundgen/centrifuge_whine.py [--out <ogg>]

A pure high whine from the rotor with a slightly detuned partner (a slow shimmer), a little air
noise from the casing and the hum of the drive. The game raises the pitch with the shaft's speed.
See soundkit.py for the requirements and the looping rules.
"""

import argparse

import plantkit as pk
import soundkit as sk

SECONDS = 2.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("centrifuge_whine"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    whine = sk.tone(t, SECONDS, 1180.0) + 0.6 * sk.tone(t, SECONDS, 1181.5, phase=0.9) + 0.25 * sk.tone(t, SECONDS, 2360.0)
    drive = pk.harmonic_tone(t, SECONDS, 100.0, (0.5, 0.3, 0.15))
    air = sk.periodic_noise(samples, 2500.0, 7000.0, seed=81)
    sk.write(args.out, 0.55 * pk.normalise(whine) + 0.25 * pk.normalise(drive) + 0.08 * air, peak=0.7)


if __name__ == "__main__":
    main()

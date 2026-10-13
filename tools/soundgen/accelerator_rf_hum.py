#!/usr/bin/env python3
"""Synthesise the particle accelerator's klystrons and cavities, looped.

    python tools/soundgen/accelerator_rf_hum.py [--out <ogg>]

A dense mains-like hum from the klystrons' power supplies under a thin, high whine of the
radio-frequency drive, pulsing gently with the machine's repetition rate. See soundkit.py for the
requirements and the looping rules.
"""

import argparse

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("accelerator_rf_hum"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    hum = pk.harmonic_tone(t, SECONDS, 100.0, (1.0, 0.55, 0.4, 0.2, 0.12))
    whine = 0.18 * sk.tone(t, SECONDS, 2400.0) + 0.08 * sk.tone(t, SECONDS, 3600.0)
    hiss = 0.12 * sk.periodic_noise(samples, 1500, 6000, seed=301, tilt=-0.5)
    pulse = pk.lfo(t, SECONDS, 2.5, 0.25)
    sk.write(args.out, (0.8 * pk.normalise(hum) + whine) * pulse + hiss, peak=0.7)


if __name__ == "__main__":
    main()

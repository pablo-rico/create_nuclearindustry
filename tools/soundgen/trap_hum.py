#!/usr/bin/env python3
"""Synthesise a Penning trap holding its field, looped.

    python tools/soundgen/trap_hum.py [--out <ogg>]

A steady electrical drone from the magnet heads with a slow beat between them, over a faint glassy
ring of the trapping field. The trap modulates this loop's pitch and volume by its fill, so it is
kept plain and centred. See soundkit.py for the requirements and the looping rules.
"""

import argparse

import plantkit as pk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("trap_hum"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    # The two heads hum a hair apart, so the drone breathes once every two seconds.
    lower = pk.harmonic_tone(t, SECONDS, 100.0, (1.0, 0.55, 0.3, 0.12))
    upper = pk.harmonic_tone(t, SECONDS, 100.5, (0.8, 0.45, 0.25, 0.1), phase_step=0.7)
    drone = pk.normalise(lower + upper)
    # The field's ring: high partials, a slow tremolo, kept well below the drone.
    ring = (sk.tone(t, SECONDS, 1320.0) + 0.6 * sk.tone(t, SECONDS, 1980.0, 0.4)) * pk.lfo(t, SECONDS, 0.75, 0.4)
    air = sk.soft_lowpass(sk.periodic_noise(samples, 40.0, 2000.0, seed=1301, tilt=-1.0), 500.0)
    mix = 0.85 * drone + 0.07 * pk.normalise(ring) + 0.12 * pk.normalise(air)
    sk.write(args.out, sk.soft_lowpass(mix, 4000.0), peak=0.6)


if __name__ == "__main__":
    main()

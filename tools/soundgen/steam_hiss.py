#!/usr/bin/env python3
"""Synthesise steam rushing through an output port, looped. The lifted relief valve uses it too.

    python tools/soundgen/steam_hiss.py [--out <ogg>]

A muffled rush of jet noise (kept under about 3 kHz, so it whooshes instead of hissing) with a low
roar underneath, gently fluttering. The game raises its volume and pitch with the flow. See soundkit.py for the requirements and the looping rules.
"""

import argparse

import plantkit as pk
import soundkit as sk

SECONDS = 3.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("steam_hiss"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    jet = sk.periodic_noise(samples, 500.0, 5000.0, seed=51, tilt=-0.7)
    roar = sk.periodic_noise(samples, 80.0, 900.0, seed=52, tilt=-0.8)
    flutter = pk.lfo(t, SECONDS, 4.0, 0.08) * pk.lfo(t, SECONDS, 1.0, 0.12, phase=0.7)
    mix = sk.soft_lowpass(0.7 * jet + 0.5 * roar, 2200.0, order=3)
    sk.write(args.out, mix * flutter, peak=0.7)


if __name__ == "__main__":
    main()

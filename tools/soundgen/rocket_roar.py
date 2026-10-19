#!/usr/bin/env python3
"""Synthesise a rocket's engines heard from near by, looped.

    python tools/soundgen/rocket_roar.py [--out <ogg>]

Broadband jet noise from a deep rumble up to a tearing hiss, a heavy flutter, and the crackle of shocks in the exhaust
that sets a rocket apart from a jet. The game crossfades it with ``rocket_roar_distant`` by distance. See soundkit.py for
the requirements and the looping rules.
"""

import argparse

import plantkit as pk
import rocketkit as rk
import soundkit as sk

SECONDS = 4.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("rocket_roar"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    rumble = sk.periodic_noise(samples, 25.0, 400.0, seed=201, tilt=-1.0)
    body = sk.periodic_noise(samples, 120.0, 3500.0, seed=202, tilt=-0.8)
    hiss = sk.periodic_noise(samples, 1500.0, 8000.0, seed=203, tilt=-0.4)
    flutter = pk.lfo(t, SECONDS, 7.0, 0.12) * pk.lfo(t, SECONDS, 2.5, 0.1, phase=0.4) * pk.lfo(t, SECONDS, 0.5, 0.06)
    crack = sk.soft_lowpass(rk.crackle(samples, 140, seed=204, length_s=0.005), 6000.0, order=2)
    mix = (1.3 * rumble + 0.6 * body + 0.1 * hiss) * flutter + 0.5 * pk.normalise(crack)
    sk.write(args.out, sk.soft_lowpass(mix, 5000.0, order=2), peak=0.85)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Synthesise a rocket's engines heard from far off, looped.

    python tools/soundgen/rocket_roar_distant.py [--out <ogg>]

What is left of the roar after kilometres of air: a deep, rolling rumble that swells and fades as the wind carries it,
with the crackle only a muffled popping underneath. See soundkit.py for the requirements and the looping rules.
"""

import argparse

import plantkit as pk
import rocketkit as rk
import soundkit as sk

SECONDS = 6.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("rocket_roar_distant"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    rumble = sk.periodic_noise(samples, 18.0, 260.0, seed=211, tilt=-1.2)
    roll = sk.periodic_noise(samples, 60.0, 700.0, seed=212, tilt=-1.0)
    swell = pk.lfo(t, SECONDS, 0.5, 0.25) * pk.lfo(t, SECONDS, 1.5, 0.12, phase=1.3) * pk.lfo(t, SECONDS, 0.17, 0.1)
    pops = sk.soft_lowpass(rk.crackle(samples, 30, seed=213, length_s=0.012), 500.0, order=3)
    mix = (1.0 * rumble + 0.45 * roll) * swell + 0.35 * pk.normalise(pops)
    sk.write(args.out, sk.soft_lowpass(mix, 600.0, order=3), peak=0.85)


if __name__ == "__main__":
    main()

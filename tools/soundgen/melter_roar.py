#!/usr/bin/env python3
"""Synthesise the vitrification melter's furnace, looped.

    python tools/soundgen/melter_roar.py [--out <ogg>]

A deep, flickering roar of the burner under the crucible, with the odd crackle and pop of the glass
melt. The flicker and the pops repeat whole within the loop. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 3.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("melter_roar"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    roar = sk.periodic_noise(samples, 30.0, 900.0, seed=111, tilt=-1.3)
    flicker = pk.lfo(t, SECONDS, 2.0, 0.15) * pk.lfo(t, SECONDS, 5.0, 0.1, phase=1.1) * pk.lfo(t, SECONDS, 0.33, 0.1)
    rng = np.random.default_rng(112)

    def crackle(i):
        n = int(sk.RATE * 0.012)
        return rng.uniform(-1, 1, n) * np.exp(-np.arange(n) / (0.002 * sk.RATE)) * (0.3 + 0.7 * rng.random())

    pops = pk.cyclic_events(samples, np.sort(rng.uniform(0, samples, 26)), crackle)
    sk.write(args.out, 0.85 * pk.normalise(roar) * flicker + 0.35 * pops, peak=0.8)


if __name__ == "__main__":
    main()

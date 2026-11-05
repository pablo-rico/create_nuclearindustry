#!/usr/bin/env python3
"""Synthesise a thermobaric charge going off close by (6 s).

    python tools/soundgen/thermobaric_blast.py [--out <ogg>]

A thermobaric weapon goes off twice. A small burster pops the canister and throws its fuel out into a cloud; for half a
second the air rushes in and the cloud spreads with a rising whoosh. Then the cloud ignites all at once: not a crack but a
long pressure whump, its positive phase far longer than a high explosive's, and the cloud burns on as a turbulent roar of
fire, crackling, before the rumble rolls away. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import blastkit as bk
import rocketkit as rk
import soundkit as sk

SECONDS = 6.0
IGNITION = 0.55


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("thermobaric_blast"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    # The burster.
    pop = (bk.n_wave(samples, 0.01, 0.006, rise_s=0.0002, asym=0.5) + 0.5 * bk.crack(samples, 2911, 0.01, decay=0.006)
           + 0.6 * bk.sub_thump(samples, 0.01, 160.0, 100.0, 0.06))
    # The inrush: a whoosh swelling as the cloud spreads, cut off by the ignition.
    u = np.clip((t - 0.06) / (IGNITION - 0.06), 0.0, 1.0)
    swell = u ** 3 * np.clip((IGNITION + 0.03 - t) / 0.03, 0.0, 1.0)
    inrush = rk.band_noise(samples, 200.0, 3000.0, 2912, tilt=-0.5) * swell
    # The whump: a long, rounded pressure pulse and a deep thump.
    whump = (bk.n_wave(samples, IGNITION, 0.12, rise_s=0.003, asym=0.4)
             + 1.0 * bk.sub_thump(samples, IGNITION, 48.0, 26.0, 1.2, attack=0.03, glide=0.3)
             + 0.8 * bk.burst(samples, 2913, 20.0, 600.0, IGNITION, 0.03, 0.8, tilt=-1.1, hold=0.15))
    # The fireball burning: a turbulent roar with a crackle in it.
    roar_env = bk.envelope(t, IGNITION + 0.05, 0.15, 1.8, hold=0.6)
    roar = rk.band_noise(samples, 60.0, 2500.0, 2914, tilt=-0.7) * roar_env * bk.rolling(samples, 2915, 6.0, 0.6)
    crackle = bk.lowpass(rk.crackle(samples, 120, seed=2916, length_s=0.004, loop=False), 4000.0) * roar_env
    rumble = bk.burst(samples, 2917, 18.0, 200.0, IGNITION + 0.2, 0.4, 2.0, tilt=-1.0)
    mix = (0.35 * bk.normalise(pop) + 0.3 * inrush + 1.0 * bk.normalise(whump) + 0.45 * roar
           + 0.15 * bk.normalise(crackle) + 0.5 * rumble)
    mix = bk.reverb(mix, 2.5, 2918, wet=0.35, damp=1500.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=1.0), peak=0.9)


if __name__ == "__main__":
    main()

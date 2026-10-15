#!/usr/bin/env python3
"""Synthesise the antiproton injector firing its dose into the plasma (one-shot).

    python tools/soundgen/antiproton_shot.py [--out <ogg>]

The trap's field opens: a short rising whine as the kicker charges, a hard electric snap as the
bunch leaves, a falling zap down the beam line and a muffled thump from the tokamak as the plasma
takes the hit. About a second and a half. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 1.5
FIRE = 0.3


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("antiproton_shot"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    after = np.clip(t - FIRE, 0.0, None)
    fired = (t >= FIRE).astype(float)
    # Kicker charging: a whine sweeping up to the moment of firing.
    charge_f = 600.0 + 2400.0 * np.clip(t / FIRE, 0.0, 1.0) ** 2
    charge = np.sin(2 * np.pi * np.cumsum(charge_f) / sk.RATE) * np.clip(t / FIRE, 0.0, 1.0) ** 1.5 * (1.0 - fired)
    # The snap: bright noise, a few milliseconds.
    noise = sk.one_shot_noise(samples, seed=1420)
    snap = (noise - sk.soft_lowpass(noise, 2500.0, loop=False)) * np.exp(-after / 0.008) * fired
    # The bunch down the line: a zap falling through the spectrum.
    zap_f = 300.0 + 2600.0 * np.exp(-after / 0.06)
    zap = np.sin(2 * np.pi * np.cumsum(zap_f * fired) / sk.RATE) * np.exp(-after / 0.15) * fired
    # The plasma taking it: a low thump and a short rumble.
    thump_f = 45.0 + 40.0 * np.exp(-after / 0.06)
    thump = np.sin(2 * np.pi * np.cumsum(thump_f * fired) / sk.RATE) * np.exp(-after / 0.25) \
        * np.minimum(1.0, after / 0.003) * fired
    rumble = pk.normalise(sk.soft_lowpass(sk.one_shot_noise(samples, seed=1421), 220.0, loop=False)) \
        * np.exp(-after / 0.4) * fired
    fade = np.clip((SECONDS - t) / 0.2, 0.0, 1.0)
    mix = 0.35 * charge + 0.9 * snap + 0.55 * zap + 1.0 * thump + 0.4 * rumble
    sk.write(args.out, sk.soft_lowpass(mix, 8000.0, loop=False) * fade, peak=0.85)


if __name__ == "__main__":
    main()

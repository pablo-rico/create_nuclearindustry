#!/usr/bin/env python3
"""Synthesise a superconducting magnet quenching (one-shot).

    python tools/soundgen/magnet_quench.py [--out <ogg>]

A sharp electrical bang as the stored field dumps into the resistors, a falling groan of the coil,
then the long rush of liquid helium boiling off through the relief valves, low-passed so the bang
thumps rather than stings. See soundkit.py for the
requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 3.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("magnet_quench"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    bang = pk.normalise(sk.soft_lowpass(sk.one_shot_noise(samples, seed=321), 2000.0, loop=False)) * np.exp(-t / 0.05) * np.minimum(1.0, t / 0.001)
    freq = 90.0 + 160.0 * np.exp(-t / 0.4)
    groan = np.sin(2 * np.pi * np.cumsum(freq) / sk.RATE) * np.exp(-t / 0.9)
    hiss = sk.lowpass(sk.one_shot_noise(samples, seed=322), 3000.0) * pk.envelope(t - 0.15, 0.3, 1.5, 2.8) * (t >= 0.15)
    fade = np.clip((SECONDS - t) / 0.3, 0.0, 1.0)
    mix = sk.soft_lowpass(1.0 * bang + 0.7 * groan + 0.45 * hiss, 2500.0, loop=False)
    sk.write(args.out, mix * fade, peak=0.85)


if __name__ == "__main__":
    main()

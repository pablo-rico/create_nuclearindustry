#!/usr/bin/env python3
"""Synthesise the boom of a nuclear shock (two variants, nuke_boom_0..1; ``nuclear_explosion`` plays them too).

    python tools/soundgen/nuke_boom.py [--out-dir <dir>]

A kiloton shock's positive phase lasts the better part of a second, so what reaches the ear is not a crack but a deep push:
a long, rounded N-wave felt as much as heard, a sub-bass thump sliding down under it, the low roar of the fireball and the
ground's rumble behind. Everything above a couple of kilohertz is gone. See soundkit.py for the requirements.
"""

import argparse
import os

import blastkit as bk
import soundkit as sk

# Seed, length (s), N-wave length (s), thump's start pitch (Hz).
VARIANTS = ((2650, 5.0, 0.55, 55.0), (2660, 5.5, 0.7, 48.0))


def make(seed, seconds, wave_s, thump_hz):
    samples, t = sk.timeline(seconds)
    start = 0.02
    wave = bk.n_wave(samples, start, wave_s, rise_s=0.004, asym=0.7)
    thump = bk.sub_thump(samples, start + 0.005, thump_hz, 24.0, 1.3, attack=0.01, glide=0.4)
    body = bk.burst(samples, seed, 20.0, 450.0, start, 0.015, 1.1, tilt=-1.2)
    roar = bk.burst(samples, seed + 1, 60.0, 1500.0, start + 0.05, 0.25, 1.6, tilt=-0.8) * bk.rolling(samples, seed + 2, 2.5, 0.5)
    rumble = bk.burst(samples, seed + 3, 18.0, 160.0, start + 0.2, 0.6, 2.2, tilt=-1.0)
    mix = 1.0 * wave + 1.1 * thump + 0.8 * body + 0.35 * roar + 0.6 * rumble
    mix = bk.reverb(mix, 3.0, seed + 4, wet=0.3, damp=900.0)
    return bk.finish(mix, t, seconds, fade=1.0, cutoff=2500.0)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-dir", default=sk.SOUNDS)
    args = ap.parse_args()
    for i, variant in enumerate(VARIANTS):
        sk.write(os.path.join(args.out_dir, "nuke_boom_%d.ogg" % i), make(*variant), peak=0.9)


if __name__ == "__main__":
    main()

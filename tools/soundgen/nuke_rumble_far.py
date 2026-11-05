#!/usr/bin/env python3
"""Synthesise a nuclear detonation heard from far off: a low thud and a long roll (9 s, streamed).

    python tools/soundgen/nuke_rumble_far.py [--out <ogg>]

Tens of kilometres of air take every high frequency off the shock: what is left is a soft, deep thud, more felt than heard,
and a low roll of echoes going on and on. Nothing here is above a couple of hundred hertz. See soundkit.py for the
requirements.
"""

import argparse

import blastkit as bk
import soundkit as sk

SECONDS = 9.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("nuke_rumble_far"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    thud = bk.n_wave(samples, 0.05, 0.9, rise_s=0.02, asym=0.6) + bk.sub_thump(samples, 0.05, 40.0, 22.0, 1.5, attack=0.05)
    n = int(1.2 * sk.RATE)
    source = bk.n_wave(n, 0.0, 0.6, rise_s=0.02, asym=0.6) + bk.burst(n, 2711, 15.0, 160.0, 0.0, 0.05, 0.4, tilt=-1.0)
    echoes = bk.reflections(samples, 2712, source, start=0.6, rate=1.5, span=7.0, gain=0.7, decay=2.5, dull=(250.0, 100.0))
    roll = bk.burst(samples, 2713, 15.0, 140.0, 0.1, 0.4, 3.0, tilt=-1.0, hold=0.5) * bk.rolling(samples, 2714, 0.8, 0.7)
    mix = 1.0 * bk.normalise(thud) + 0.7 * bk.normalise(echoes) + 0.6 * roll
    mix = bk.reverb(bk.lowpass(mix, 200.0, order=3), 3.0, 2715, wet=0.35, damp=300.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=2.0, cutoff=220.0), peak=0.9)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Synthesise a Penning trap losing its field (one-shot, about three seconds: the quench countdown).

    python tools/soundgen/trap_quench.py [--out <ogg>]

An urgent two-tone alarm warbling ever faster over a whine that climbs as the field collapses,
with electrical crackle growing denser towards the end. It stops dead at three seconds, when the
trap lets go of its antimatter. See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 3.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("trap_quench"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    progress = t / SECONDS
    # Alarm: alternate 880/660 Hz, the switching rate rising from 4 to 10 per second.
    rate = 4.0 + 6.0 * progress
    switch = np.sign(np.sin(2 * np.pi * np.cumsum(rate) / sk.RATE))
    freq = np.where(switch > 0, 880.0, 660.0)
    phase = 2 * np.pi * np.cumsum(freq) / sk.RATE
    alarm = 0.7 * np.sin(phase) + 0.3 * np.sign(np.sin(phase))
    # Collapsing field: a whine sliding up from 300 to 1500 Hz.
    whine_f = 300.0 * (5.0 ** progress)
    whine = np.sin(2 * np.pi * np.cumsum(whine_f) / sk.RATE) * (0.3 + 0.7 * progress)
    # Sparks: short noise clicks, ever more of them.
    rng = np.random.default_rng(1310)
    starts = np.sort(SECONDS * np.sqrt(rng.uniform(0, 1, 70)) * sk.RATE)

    def spark(i):
        n = int(sk.RATE * 0.004)
        return rng.uniform(-1, 1, n) * np.exp(-np.arange(n) / (0.0008 * sk.RATE))

    sparks = pk.cyclic_events(samples, np.minimum(starts, samples - 200), spark)
    fade = np.clip((SECONDS - t) / 0.02, 0.0, 1.0) * np.minimum(1.0, t / 0.01)
    mix = 0.55 * alarm + 0.35 * whine + 0.45 * sparks * (0.3 + progress)
    sk.write(args.out, sk.soft_lowpass(mix, 5000.0, loop=False) * fade, peak=0.8)


if __name__ == "__main__":
    main()

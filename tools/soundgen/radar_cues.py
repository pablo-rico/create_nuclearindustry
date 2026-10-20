#!/usr/bin/env python3
"""Synthesise the radar's and the holographic command table's electronic cues.

    python tools/soundgen/radar_cues.py [--only <name>] [--out-dir <dir>]

* ``radar_ping``: the sweep passing a contact, a clean sonar-like ping with a soft echo off the room.
* ``radar_new_track``: a new contact, two rising console tones.
* ``radar_klaxon``: a threat not yet acknowledged, looped: a harsh tone sweeping up twice in its two seconds, silent at
  the loop's ends so it repeats cleanly.
* ``radar_ack``: the alarm acknowledged, two falling tones.
* ``holo_blip``: the holographic table, a soft glassy blip with a shimmer.

See soundkit.py for the requirements and the looping rules.
"""

import argparse
import os

import numpy as np

import plantkit as pk
import rocketkit as rk
import soundkit as sk


def radar_ping():
    n, t = sk.timeline(0.7)
    tone = np.sin(2 * np.pi * 1250.0 * t) + 0.3 * np.sin(2 * np.pi * 2500.0 * t) * rk.decay(t, 0.05)
    ping = tone * np.minimum(1.0, t / 0.003) * rk.decay(t, 0.12)
    return rk.echo(ping, 0.09, 0.35, repeats=2)[:n]


def radar_new_track():
    a = rk.beep(0.11, 880.0, square=0.35)
    gap = np.zeros(int(0.03 * sk.RATE))
    b = rk.beep(0.16, 1320.0, square=0.35)
    return np.concatenate([a, gap, b, np.zeros(int(0.08 * sk.RATE))])


def radar_klaxon():
    seconds = 2.0
    n, t = sk.timeline(seconds)
    half = n // 2
    out = np.zeros(n)
    for start in (0, half):
        seg_t = t[:half]
        sweep = rk.tone_sweep(seg_t, 420.0, 880.0, shape=0.7)
        # A harsh, buzzy tone: the sweep's own odd harmonics.
        phase = np.arcsin(np.clip(sweep, -1, 1))
        buzz = np.sign(sweep) * np.abs(sweep) ** 0.35 + 0.2 * np.sin(3 * phase)
        out[start:start + half] = buzz * pk.envelope(seg_t, 0.02, 0.12, seconds / 2 - 0.08)
    return sk.soft_lowpass(out, 4000.0, order=2)


def radar_ack():
    a = rk.beep(0.08, 1180.0, square=0.2)
    gap = np.zeros(int(0.02 * sk.RATE))
    b = rk.beep(0.14, 790.0, square=0.2)
    return np.concatenate([a, gap, b, np.zeros(int(0.05 * sk.RATE))])


def holo_blip():
    n, t = sk.timeline(0.3)
    f = 2100.0 - 500.0 * (1 - rk.decay(t, 0.05))
    carrier = np.sin(2 * np.pi * np.cumsum(f) / sk.RATE + 0.8 * np.sin(2 * np.pi * 37.0 * t))
    shimmer = 0.25 * np.sin(2 * np.pi * 4200.0 * t) * rk.decay(t, 0.04)
    return (carrier + shimmer) * np.minimum(1.0, t / 0.004) * rk.decay(t, 0.07)


CUES = {
    "radar_ping": (radar_ping, 0.7),
    "radar_new_track": (radar_new_track, 0.7),
    "radar_klaxon": (radar_klaxon, 0.55),
    "radar_ack": (radar_ack, 0.7),
    "holo_blip": (holo_blip, 0.6),
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", choices=sorted(CUES), default=None)
    ap.add_argument("--out-dir", default=sk.SOUNDS)
    args = ap.parse_args()
    for name, (make, peak) in sorted(CUES.items()):
        if args.only and name != args.only:
            continue
        sk.write(os.path.join(args.out_dir, name + ".ogg"), make(), peak=peak)


if __name__ == "__main__":
    main()

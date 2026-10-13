#!/usr/bin/env python3
"""Synthesise the pressurizer's relief valve lifting (one-shot).

    python tools/soundgen/relief_valve_lift.py [--out <ogg>]

The valve disc slams open with a dull metallic pop and steam bursts out: a violent rush whose roar
(a band of noise, not a whistle) slides down as the pressure drops, low-passed so it never shrieks. While it stays open the steam hiss loop carries on.
See soundkit.py for the requirements.
"""

import argparse

import numpy as np

import plantkit as pk
import soundkit as sk

SECONDS = 2.2


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("relief_valve_lift"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    attack = np.minimum(1.0, t / 0.001)
    pop = pk.normalise(sk.soft_lowpass(sk.one_shot_noise(samples, seed=71), 1500.0, loop=False)) * np.exp(-t / 0.02) * attack
    pop += 0.8 * np.sin(2 * np.pi * 310.0 * t) * np.exp(-t / 0.08) * attack
    burst = sk.lowpass(sk.one_shot_noise(samples, seed=72), 4000.0)
    burst = burst - sk.lowpass(burst, 250.0)
    burst *= np.minimum(1.0, t / 0.03) * (0.35 + 0.65 * np.exp(-t / 0.5))
    # The roar slides down as the pressure falls: noise swept through a resonance, frame by frame.
    frame = 512
    noise = sk.one_shot_noise(samples + 3 * frame, seed=73)
    roar = np.zeros(samples + 3 * frame)
    window = np.hanning(2 * frame)
    freqs = np.fft.rfftfreq(2 * frame, 1.0 / sk.RATE)
    for start in range(0, samples + frame, frame):
        # Each window is centred on sample ``start`` of the sound.
        centre = 500.0 + 500.0 * np.exp(-start / sk.RATE / 0.4)
        spectrum = np.fft.rfft(noise[start:start + 2 * frame] * window)
        spectrum *= np.exp(-((freqs - centre) / (0.35 * centre)) ** 2)
        roar[start:start + 2 * frame] += np.fft.irfft(spectrum, 2 * frame)
    roar = pk.normalise(roar[frame:frame + samples]) * pk.envelope(t, 0.05, 0.8, SECONDS)
    mix = 0.9 * pop + 0.8 * pk.normalise(burst) + 0.45 * roar
    mix = sk.soft_lowpass(mix, 2500.0, loop=False)
    fade = np.clip((SECONDS - t) / 0.6, 0.0, 1.0)
    sk.write(args.out, mix * fade, peak=0.9)


if __name__ == "__main__":
    main()

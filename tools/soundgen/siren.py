#!/usr/bin/env python3
"""Synthesise the air raid siren's looping tone.

    python tools/soundgen/siren.py [--out <ogg>]

Needs numpy and soundfile (whose bundled libsndfile writes Ogg Vorbis). The tone is one steady
chord of two rotors (a major third apart) with the rasp of air through the ports; the game bends
its pitch into the rising and falling wail, so the file itself never changes pitch. Every partial
completes a whole number of cycles in the loop and the air noise is built periodic, so the loop is
seamless. The audio is deterministic; the Ogg container's stream serial is not, so regenerating
rewrites the file even when it sounds the same.
"""

import argparse
import os

import numpy as np
import soundfile as sf

RATE = 22050
SECONDS = 2.0
OUT = os.path.join("src", "main", "resources", "assets", "create_nuclearindustry", "sounds", "air_raid_siren.ogg")


def rotor(t, frequency, harmonics=9):
    """A port siren is close to a band-limited sawtooth: every harmonic, falling off."""
    wave = np.zeros_like(t)
    for n in range(1, harmonics + 1):
        wave += np.sin(2 * np.pi * frequency * n * t + 0.3 * n) / n ** 1.15
    return wave


def periodic_noise(samples, low, high, seed):
    """Band-limited noise that loops: random phases on an exact FFT grid."""
    rng = np.random.default_rng(seed)
    spectrum = np.zeros(samples // 2 + 1, dtype=complex)
    freqs = np.fft.rfftfreq(samples, 1.0 / RATE)
    band = (freqs >= low) & (freqs <= high)
    spectrum[band] = np.exp(2j * np.pi * rng.random(band.sum()))
    noise = np.fft.irfft(spectrum, samples)
    return noise / np.max(np.abs(noise))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=OUT)
    args = ap.parse_args()

    samples = int(RATE * SECONDS)
    t = np.arange(samples) / RATE
    # 420 Hz and 525 Hz (a major third), plus a slightly detuned copy of the lower rotor for body.
    tone = rotor(t, 420.0) + 0.8 * rotor(t, 525.0) + 0.35 * rotor(t, 421.5)
    tone /= np.max(np.abs(tone))
    air = periodic_noise(samples, 900.0, 5000.0, seed=7)
    # The ports chop the air at the rotor frequency, so the rasp pulses with it.
    chop = 0.6 + 0.4 * np.sin(2 * np.pi * 420.0 * t) ** 2
    mix = 0.9 * tone + 0.08 * air * chop
    mix = 0.8 * mix / np.max(np.abs(mix))

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    sf.write(args.out, mix.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    print("wrote %s (%d samples, %.1f s)" % (args.out, samples, SECONDS))


if __name__ == "__main__":
    main()

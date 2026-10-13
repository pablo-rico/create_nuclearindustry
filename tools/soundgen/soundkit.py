"""Shared helpers for the synthesised tokamak sounds (magnet hum, plasma rumble and breakdown, cryoplant).

Needs numpy and soundfile (whose bundled libsndfile writes Ogg Vorbis). Every sound is mono, so the
game can place it in the world. Loops are seamless: each tone completes a whole number of cycles in
the loop and the noise is built periodic on an exact FFT grid. The audio is deterministic; the Ogg
container's stream serial is not, so regenerating rewrites the files even when they sound the same.
"""

import os

import numpy as np
import soundfile as sf

RATE = 22050
SOUNDS = os.path.join("src", "main", "resources", "assets", "create_nuclearindustry", "sounds")


def out_path(name):
    return os.path.join(SOUNDS, name + ".ogg")


def timeline(seconds):
    samples = int(round(RATE * seconds))
    return samples, np.arange(samples) / RATE


def loop_frequency(frequency, seconds):
    """The nearest frequency that completes a whole number of cycles in the loop."""
    return max(1, round(frequency * seconds)) / seconds


def tone(t, seconds, frequency, phase=0.0):
    """A sine that loops cleanly over ``seconds``."""
    return np.sin(2 * np.pi * loop_frequency(frequency, seconds) * t + phase)


def periodic_noise(samples, low, high, seed, tilt=0.0):
    """Band-limited noise that loops: random phases on an exact FFT grid.

    ``tilt`` bends the spectrum: negative values darken it (more weight on the low end)."""
    rng = np.random.default_rng(seed)
    freqs = np.fft.rfftfreq(samples, 1.0 / RATE)
    band = (freqs >= low) & (freqs <= high)
    spectrum = np.zeros(samples // 2 + 1, dtype=complex)
    weights = np.ones(band.sum())
    if tilt:
        weights = (freqs[band] / max(low, 1.0)) ** tilt
    spectrum[band] = weights * np.exp(2j * np.pi * rng.random(band.sum()))
    noise = np.fft.irfft(spectrum, samples)
    return noise / np.max(np.abs(noise))


def one_shot_noise(samples, seed):
    """Plain white noise for one-shot sounds."""
    return np.random.default_rng(seed).uniform(-1.0, 1.0, samples)


def lowpass(signal, cutoff):
    """Brick-wall low-pass in the frequency domain (fine for one-shots and periodic loops alike)."""
    spectrum = np.fft.rfft(signal)
    freqs = np.fft.rfftfreq(len(signal), 1.0 / RATE)
    spectrum[freqs > cutoff] = 0
    return np.fft.irfft(spectrum, len(signal))


def soft_lowpass(signal, cutoff, order=4, loop=True):
    """A gentle low-pass with a Butterworth slope (``order`` x 6 dB per octave past ``cutoff``).

    Unlike ``lowpass`` it does not ring, so it can take the edge off a whole mix. It runs in the
    frequency domain, which keeps a loop seamless; a one-shot (``loop=False``) is padded first so
    its attack does not wrap round into its tail."""
    pad = 0 if loop else RATE // 4
    padded = np.concatenate([signal, np.zeros(pad)])
    spectrum = np.fft.rfft(padded)
    freqs = np.fft.rfftfreq(len(padded), 1.0 / RATE)
    spectrum *= 1.0 / np.sqrt(1.0 + (freqs / cutoff) ** (2 * order))
    return np.fft.irfft(spectrum, len(padded))[:len(signal)]


def write(path, signal, peak=0.8):
    signal = peak * signal / np.max(np.abs(signal))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    sf.write(path, signal.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    print("wrote %s (%d samples, %.2f s)" % (path, len(signal), len(signal) / RATE))

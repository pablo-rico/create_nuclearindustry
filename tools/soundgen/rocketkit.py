"""Helpers shared by the rocket, radar and command table sounds, on top of soundkit.py and plantkit.py.

A rocket's roar is broadband jet noise with a crackle on top: sharp pressure pops from the shocks in the exhaust, the
sound that sets a rocket apart from a jet. ``crackle`` makes those pops; ``band_noise`` gives one-shots their noise
without the wrap-round a periodic spectrum would put in them; ``tone_sweep`` and ``beep`` make the radar's electronic cues.
"""

import numpy as np

import plantkit as pk
import soundkit as sk


def band_noise(samples, low, high, seed, tilt=0.0):
    """Band-limited noise for a one-shot: filtered with padding, so its start and end do not bleed into each other.

    ``tilt`` bends the spectrum like ``soundkit.periodic_noise``: negative values darken it."""
    pad = sk.RATE // 4
    rng = np.random.default_rng(seed)
    noise = rng.standard_normal(samples + pad)
    spectrum = np.fft.rfft(noise)
    freqs = np.fft.rfftfreq(len(noise), 1.0 / sk.RATE)
    band = (freqs >= low) & (freqs <= high)
    weights = np.zeros_like(freqs)
    weights[band] = 1.0
    if tilt:
        weights[band] = (freqs[band] / max(low, 1.0)) ** tilt
    out = np.fft.irfft(spectrum * weights, len(noise))[:samples]
    return out / max(1e-9, np.max(np.abs(out)))


def crackle(samples, rate_per_second, seed, length_s=0.004, loop=True, sharpness=1.0):
    """The crackle of a rocket's exhaust: short, one-sided pressure pops at random, louder ones rarer.

    With ``loop`` the pops that run past the end wrap round to the start."""
    rng = np.random.default_rng(seed)
    count = int(rate_per_second * samples / sk.RATE)
    starts = np.sort(rng.uniform(0, samples, count))
    n = max(4, int(sk.RATE * length_s))
    t = np.arange(n) / sk.RATE

    def pop(i):
        # A steep rise and an exponential fall, the skewed wave of a shock; size by a power law.
        size = rng.pareto(2.5) + 0.3
        rise = np.minimum(1.0, t / (0.0003 / sharpness))
        return size * rise * np.exp(-t / (length_s / 3.0)) * (1.0 if rng.random() < 0.8 else -0.6)

    if loop:
        return pk.cyclic_events(samples, starts, pop)
    out = np.zeros(samples + n)
    for i, s in enumerate(starts):
        out[int(s):int(s) + n] += pop(i)
    return out[:samples]


def decay(t, seconds):
    """An exponential fall with time constant ``seconds``."""
    return np.exp(-t / seconds)


def thump(t, frequency, seconds, drop=0.5):
    """A low thud: a sine sliding down by ``drop`` of its pitch as it dies away."""
    f = frequency * (1.0 - drop * (1.0 - np.exp(-t / (seconds * 0.6))))
    phase = 2 * np.pi * np.cumsum(f) / sk.RATE
    return np.sin(phase) * decay(t, seconds) * np.minimum(1.0, t / 0.002)


def tone_sweep(t, f0, f1, shape=1.0):
    """A tone gliding from ``f0`` to ``f1`` over the length of ``t`` (``shape`` bends the glide)."""
    u = (t - t[0]) / max(1e-9, t[-1] - t[0])
    f = f0 + (f1 - f0) * u ** shape
    return np.sin(2 * np.pi * np.cumsum(f) / sk.RATE)


def soft_square(t, frequency, edge=3.0):
    """A square wave with its edges rounded (a few odd harmonics): the radar's console tone."""
    out = np.zeros_like(t)
    for k in range(1, 8, 2):
        out += np.sin(2 * np.pi * frequency * k * t) / k * np.exp(-(k - 1) / edge)
    return out


def beep(seconds, frequency, attack=0.004, release=0.05, square=0.0):
    """A clean beep of ``seconds``: a sine, or with ``square`` part of a rounded square."""
    n, t = sk.timeline(seconds)
    tone = (1 - square) * np.sin(2 * np.pi * frequency * t) + square * soft_square(t, frequency)
    return tone * pk.envelope(t, attack, release, seconds)


def echo(signal, delay_s, gain, repeats=3):
    """A few fading echoes, as off the walls of a control room."""
    out = np.concatenate([signal, np.zeros(int(sk.RATE * delay_s * repeats))])
    d = int(sk.RATE * delay_s)
    for k in range(1, repeats + 1):
        out[k * d:k * d + len(signal)] += signal * gain ** k
    return out

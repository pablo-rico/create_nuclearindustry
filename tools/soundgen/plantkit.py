"""Helpers shared by the fission plant's synthesised sounds (reactor, steam, machines), on top of soundkit.py.

Everything here keeps loops seamless: events placed with ``cyclic_events`` wrap round the end of
the loop, and modulators run a whole number of cycles in it.
"""

import numpy as np

import soundkit as sk


def lfo(t, seconds, frequency, depth, phase=0.0):
    """A slow wobble, 1 +/- ``depth``, that completes whole cycles in the loop."""
    return 1.0 + depth * sk.tone(t, seconds, frequency, phase)


def harmonic_tone(t, seconds, frequency, levels, phase_step=0.3):
    """A stack of harmonics of a loop-safe fundamental; ``levels`` are the partials' amplitudes."""
    out = np.zeros_like(t)
    for n, level in enumerate(levels, start=1):
        out += level * sk.tone(t, seconds, frequency * n, phase=phase_step * n)
    return out


def cyclic_events(samples, starts, make):
    """Adds one event per start time (in samples) and wraps whatever runs past the end to the start.

    ``make(index)`` returns the event's samples."""
    out = np.zeros(samples)
    for i, start in enumerate(starts):
        event = make(i)
        idx = (int(start) + np.arange(len(event))) % samples
        np.add.at(out, idx, event)
    return out


def bubble(rng, base, spread, length_s=0.06):
    """One bubble: a short sine rising in pitch as it shrinks, with a fast decay."""
    n = int(sk.RATE * length_s)
    t = np.arange(n) / sk.RATE
    f0 = base * (1.0 + spread * rng.random())
    freq = f0 * (1.0 + 1.8 * t / length_s)
    phase = 2 * np.pi * np.cumsum(freq) / sk.RATE
    env = np.minimum(1.0, t / 0.002) * np.exp(-t / (length_s / 3.5))
    return np.sin(phase) * env * (0.5 + 0.5 * rng.random())


def envelope(t, attack, release, length):
    """Linear attack and release over a note of ``length`` seconds starting at 0."""
    up = np.clip(t / attack, 0.0, 1.0)
    down = np.clip((length - t) / release, 0.0, 1.0)
    return up * down * (t < length)


def normalise(signal):
    return signal / max(1e-9, np.max(np.abs(signal)))

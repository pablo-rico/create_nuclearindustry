"""Helpers shared by the explosion sounds (nuclear, conventional, thermobaric and cluster), on top of soundkit.py and
rocketkit.py.

An explosion heard in the open is a few physical pieces, and each helper makes one:

- ``n_wave``: the shock front itself, a near-instant jump in pressure, a straight fall below ambient and a jump back. Its
  length sets the pitch of the boom: centimetres of charge give a crack, kilotons a deep, long push.
- ``burst``: the roar of the fireball and the blast's broadband body, band-limited noise with a fast attack.
- ``sub_thump``: the chest thump, a sine sliding down into the sub-bass as it dies.
- ``reflections``: the rolling rumble, the shock coming back off terrain and buildings, Poisson-timed, each echo later,
  weaker and duller than the last.
- ``impacts``: debris and earth raining down, many tiny Poisson-timed knocks with their own pitch and size.
- ``reverb`` and ``air``: the space round the listener, and the air soaking up the highs over distance.

All one-shot: filtering is padded so a sound's tail never wraps into its attack. Deterministic for a given seed.
"""

import numpy as np

import rocketkit as rk
import soundkit as sk

RATE = sk.RATE


def _filter(signal, gain):
    """Applies a magnitude response ``gain(freqs)`` in the frequency domain, padded so nothing wraps round."""
    pad = RATE // 2
    padded = np.concatenate([signal, np.zeros(pad)])
    spectrum = np.fft.rfft(padded)
    freqs = np.fft.rfftfreq(len(padded), 1.0 / RATE)
    return np.fft.irfft(spectrum * gain(freqs), len(padded))[:len(signal)]


def lowpass(signal, cutoff, order=2):
    """A Butterworth-sloped low-pass (``order`` x 6 dB per octave), without ringing."""
    return _filter(signal, lambda f: 1.0 / np.sqrt(1.0 + (f / cutoff) ** (2 * order)))


def highpass(signal, cutoff, order=2):
    return _filter(signal, lambda f: 1.0 / np.sqrt(1.0 + (cutoff / np.maximum(f, 1e-3)) ** (2 * order)))


def bandpass(signal, low, high, order=2):
    return highpass(lowpass(signal, high, order), low, order)


def air(signal, distance_blocks):
    """The air's absorption over a distance: the farther, the lower the cut-off (roughly a kilometre takes everything
    over a few hundred hertz off a boom)."""
    cutoff = max(90.0, 9000.0 / (1.0 + distance_blocks / 60.0))
    return lowpass(signal, cutoff, order=2)


def normalise(signal):
    return signal / max(1e-9, np.max(np.abs(signal)))


def at(samples, start_s):
    """A 0/1 gate that opens ``start_s`` seconds in."""
    gate = np.zeros(samples)
    gate[min(samples, max(0, int(round(start_s * RATE)))):] = 1.0
    return gate


def envelope(t, start, attack, decay, hold=0.0):
    """An attack ramp from ``start``, ``hold`` seconds at full, then an exponential fall with time constant ``decay``."""
    u = t - start
    rise = np.clip(u / max(attack, 1e-5), 0.0, 1.0)
    fall = np.exp(-np.maximum(0.0, u - attack - hold) / decay)
    return np.where(u >= 0.0, rise * fall, 0.0)


def fade_out(t, length, seconds):
    """Ramps the last ``seconds`` down to silence, so no sound ends on a click."""
    return np.clip((length - t) / seconds, 0.0, 1.0) ** 1.5


def n_wave(samples, start_s, length_s, rise_s=0.0004, asym=1.0):
    """The shock's pressure trace: a jump up at ``start_s``, a straight fall to ``-asym`` over ``length_s``, a jump back to
    rest. ``rise_s`` rounds the jumps (the air's viscosity; distant shocks round off more)."""
    t = np.arange(samples) / RATE
    u = (t - start_s) / length_s
    wave = np.where((u >= 0.0) & (u < 1.0), 1.0 - (1.0 + asym) * u, 0.0)
    return lowpass(wave, 0.35 / rise_s, order=2)


def burst(samples, seed, low, high, start, attack, decay, tilt=0.0, hold=0.0):
    """Band-limited noise with an attack and exponential decay: a blast's broadband body or a fireball's roar."""
    t = np.arange(samples) / RATE
    noise = rk.band_noise(samples, low, high, seed, tilt=tilt)
    return noise * envelope(t, start, attack, decay, hold)


def sub_thump(samples, start, f0, f1, decay, attack=0.004, glide=0.25):
    """A sine sliding from ``f0`` down to ``f1`` (time constant ``glide``) and dying away: the chest thump."""
    t = np.arange(samples) / RATE
    u = np.maximum(0.0, t - start)
    f = f1 + (f0 - f1) * np.exp(-u / glide)
    phase = 2 * np.pi * np.cumsum(f * (t >= start)) / RATE
    return np.sin(phase) * envelope(t, start, attack, decay)


def rolling(samples, seed, rate_hz, depth):
    """A slow, random swell and fade (1 +/- ``depth``) with changes about ``rate_hz`` times a second: the roll of thunder."""
    n = rk.band_noise(samples, 0.05, rate_hz, seed)
    return np.clip(1.0 + depth * n, 0.0, None)


def fftconvolve(a, b):
    n = len(a) + len(b) - 1
    size = 1 << int(np.ceil(np.log2(n)))
    return np.fft.irfft(np.fft.rfft(a, size) * np.fft.rfft(b, size), size)[:n]


def reverb(signal, seconds, seed, wet=0.35, damp=3000.0, predelay=0.02):
    """The space round the listener: the signal convolved with a decaying, darkening noise tail ``seconds`` long (to
    -60 dB), mixed in at ``wet``."""
    n = int(RATE * seconds)
    t = np.arange(n) / RATE
    rng = np.random.default_rng(seed)
    ir = rng.standard_normal(n) * np.exp(-6.9 * t / seconds)
    ir = lowpass(ir, damp, order=1)
    ir[:int(predelay * RATE)] = 0.0
    ir /= np.sqrt(np.sum(ir ** 2))
    tail = fftconvolve(signal, ir)[:len(signal)]
    return signal + wet * tail * (np.max(np.abs(signal)) / max(1e-9, np.max(np.abs(tail))))


def reflections(samples, seed, source, start, rate, span, gain=0.6, decay=None, dull=(2000.0, 250.0), spread=0.5):
    """The rolling rumble: Poisson-timed echoes of ``source`` (a short boom) off terrain. Echoes come at ``rate`` per second
    over ``span`` seconds after ``start``, each weaker the later it comes (time constant ``decay``, default ``span``/3) and
    duller, from ``dull[0]`` to ``dull[1]`` hertz. ``spread`` smears each echo (a broad hillside, not a wall)."""
    rng = np.random.default_rng(seed)
    decay = decay or span / 3.0
    # A few dulled copies of the source, picked per echo, so each echo need not be filtered on its own.
    steps = 6
    # Padded in front so the filters' smear before the attack is kept, then the copy's ends are ramped: a truncated edge
    # would click.
    lead = int(0.05 * RATE)
    padded = np.concatenate([np.zeros(lead), source])
    ramp = int(0.004 * RATE)
    copies = []
    for i in range(steps):
        cutoff = dull[0] * (dull[1] / dull[0]) ** (i / (steps - 1))
        copy = lowpass(padded, cutoff, order=2)
        if spread:
            smear = int(RATE * 0.02 * (1 + i) * spread)
            kernel = np.hanning(2 * smear + 1)
            copy = np.convolve(copy, kernel / kernel.sum(), mode="same")
        copy[:ramp] *= np.linspace(0.0, 1.0, ramp)
        copy[-ramp:] *= np.linspace(1.0, 0.0, ramp)
        copies.append(normalise(copy))
    out = np.zeros(samples + lead + len(padded))
    when = start
    while True:
        when += rng.exponential(1.0 / rate)
        if when >= start + span:
            break
        age = (when - start) / span
        level = gain * np.exp(-(when - start) / decay) * rng.uniform(0.35, 1.0)
        copy = copies[min(steps - 1, int(age * steps))]
        # The copy's attack sits ``lead`` samples in; ``out`` is offset by the same so it lands at ``when``.
        i = int(when * RATE)
        out[i:i + len(copy)] += level * copy * (1.0 if rng.random() < 0.85 else -1.0)
    return out[lead:lead + samples]


def impacts(samples, seed, start, peak, end, rate, low=250.0, high=3500.0, size=1.0, thuds=0.15):
    """Debris and earth raining down: tiny knocks, Poisson-timed, their rate rising from ``start`` to ``rate`` per second at
    ``peak`` and thinning out by ``end``. Each knock is a few milliseconds of noise at its own pitch between ``low`` and
    ``high``; ``thuds`` of them are heavier, lower clods."""
    rng = np.random.default_rng(seed)
    out = np.zeros(samples + RATE // 2)
    when = start
    while when < end:
        if when < peak:
            r = rate * max(0.05, (when - start) / max(1e-3, peak - start))
        else:
            r = rate * max(0.03, (1.0 - (when - peak) / max(1e-3, end - peak)) ** 1.5)
        when += rng.exponential(1.0 / r)
        if when >= end:
            break
        heavy = rng.random() < thuds
        length = rng.uniform(0.004, 0.012) * (3.0 if heavy else 1.0)
        n = max(8, int(length * RATE))
        tt = np.arange(n) / RATE
        knock = rng.standard_normal(n) * np.exp(-tt / (length / 3.0))
        # Ring at a pitch, as a stone or a clod does when it lands.
        f = rng.uniform(80.0, 220.0) if heavy else np.exp(rng.uniform(np.log(low), np.log(high)))
        knock = 0.6 * knock + np.sin(2 * np.pi * f * tt + rng.uniform(0, 6.28)) * np.exp(-tt / (length / 2.0)) * 2.0
        level = size * (rng.pareto(2.2) + 0.4) * (1.8 if heavy else 1.0)
        i = int(when * RATE)
        out[i:i + n] += level * knock
    return out[:samples]


def crack(samples, seed, start, decay=0.012, cutoff=1500.0):
    """The snap of a supersonic front: a click of broadband noise, its highs only, gone in a few milliseconds."""
    t = np.arange(samples) / RATE
    noise = sk.one_shot_noise(samples, seed)
    return highpass(noise, cutoff, order=2) * envelope(t, start, 0.0003, decay)


def finish(signal, t, length, fade=0.6, cutoff=None):
    """Smooths a mix's top end, fades out its last ``fade`` seconds and ramps in its first few milliseconds (the filters
    are zero-phase, so a little of every attack smears ahead of it and would start the file on a click)."""
    if cutoff:
        signal = lowpass(signal, cutoff, order=2)
    # Remove any DC the N-waves leave, which would thump on and off.
    signal = highpass(signal, 15.0, order=1)
    return signal * fade_out(t, length, fade) * np.clip(t / 0.004, 0.0, 1.0)


def he_blast(seconds, seed, shock_s, thump_hz, thump_s, body_s, body_hz, rumble_s, debris_rate, debris_s, room_s):
    """A conventional high-explosive blast heard from close by: the crack of the shock (``shock_s`` long, longer for a
    bigger charge), the thump (``thump_hz``, dying over ``thump_s``), the broadband body of the blast up to ``body_hz``, a
    short rolling rumble, then the debris it threw up raining down for ``debris_s``. ``room_s`` is the echo's length."""
    samples, t = sk.timeline(seconds)
    start = 0.01
    shock = n_wave(samples, start, shock_s, rise_s=0.0003, asym=0.6)
    snap = crack(samples, seed, start, decay=0.005 + shock_s * 0.15, cutoff=1500.0)
    thump = sub_thump(samples, start, thump_hz * 1.8, thump_hz, thump_s, attack=0.002, glide=0.05)
    body = burst(samples, seed + 1, 40.0, body_hz, start, 0.001, body_s, tilt=-0.7)
    rumble = burst(samples, seed + 2, 20.0, 300.0, start + 0.02, 0.05, rumble_s, tilt=-1.0) * rolling(samples, seed + 3, 3.0, 0.5)
    debris = lowpass(impacts(samples, seed + 4, start + 0.25, start + 0.55, start + debris_s, debris_rate), 5000.0)
    mix = 1.0 * shock + 0.45 * snap + 0.9 * thump + 0.8 * body + 0.5 * rumble + 0.2 * normalise(debris)
    mix = reverb(mix, room_s, seed + 5, wet=0.3, damp=2500.0)
    return finish(mix, t, seconds, fade=0.4)

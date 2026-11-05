#!/usr/bin/env python3
"""Synthesise a thermobaric charge heard from far off (5 s).

    python tools/soundgen/thermobaric_distant.py [--out <ogg>]

From a distance the burster's pop is barely there and the inrush is lost; what carries is the cloud's long, low whump and
the dull roar of it burning, then the echoes rolling back. The air has taken everything above a few hundred hertz. See
soundkit.py for the requirements.
"""

import argparse

import blastkit as bk
import rocketkit as rk
import soundkit as sk

SECONDS = 5.0
IGNITION = 0.55


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=sk.out_path("thermobaric_distant"))
    args = ap.parse_args()

    samples, t = sk.timeline(SECONDS)
    pop = bk.sub_thump(samples, 0.01, 110.0, 80.0, 0.08, attack=0.003)

    def whump(m, start, seed):
        return (bk.n_wave(m, start, 0.2, rise_s=0.01, asym=0.4) + bk.sub_thump(m, start, 40.0, 24.0, 1.4, attack=0.05)
                + 0.7 * bk.burst(m, seed, 20.0, 250.0, start, 0.05, 0.8, tilt=-1.0))

    main_whump = whump(samples, IGNITION, 2921)
    roar = rk.band_noise(samples, 40.0, 400.0, 2922, tilt=-1.0) * bk.envelope(t, IGNITION + 0.1, 0.3, 1.6, hold=0.5) \
        * bk.rolling(samples, 2923, 3.0, 0.5)
    echoes = bk.reflections(samples, 2924, whump(int(1.2 * sk.RATE), 0.0, 2925), start=IGNITION + 0.3, rate=2.0, span=3.5,
                            gain=0.6, decay=1.2, dull=(300.0, 120.0))
    mix = 0.15 * bk.normalise(pop) + bk.normalise(main_whump) + 0.4 * roar + 0.6 * bk.normalise(echoes)
    mix = bk.reverb(bk.lowpass(mix, 400.0, order=3), 2.5, 2926, wet=0.35, damp=400.0)
    sk.write(args.out, bk.finish(mix, t, SECONDS, fade=1.0), peak=0.9)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Generate the canonical Better Survival Physiology badge strip and reviewable sound motifs."""

from __future__ import annotations

import argparse
from pathlib import Path
import shutil

import numpy as np
import soundfile as sf
from PIL import Image

RATE = 48_000
ASPECTS = ("impact", "tempo", "work", "mobility", "endurance", "robustness", "renewal", "control")


def envelope(length: int, attack: float = .008, release: float = .08) -> np.ndarray:
    result = np.ones(length)
    a = min(length, int(RATE * attack))
    r = min(length, int(RATE * release))
    if a: result[:a] = np.linspace(0.0, 1.0, a)
    if r: result[-r:] *= np.linspace(1.0, 0.0, r)
    return result


def tone(duration: float, start: float, end: float | None = None, harmonics=(1.0,)) -> np.ndarray:
    count = int(RATE * duration)
    frequencies = np.linspace(start, end if end is not None else start, count)
    phase = 2.0 * np.pi * np.cumsum(frequencies) / RATE
    signal = sum(weight * np.sin(phase * (index + 1)) for index, weight in enumerate(harmonics))
    return signal * envelope(count)


def noise(duration: float, seed: int, decay: float = 18.0) -> np.ndarray:
    count = int(RATE * duration)
    rng = np.random.default_rng(seed)
    return rng.normal(0.0, 1.0, count) * np.exp(-np.linspace(0.0, decay, count)) * envelope(count, .001, .03)


def place(parts: list[tuple[float, np.ndarray]], duration: float) -> np.ndarray:
    result = np.zeros(int(RATE * duration))
    for offset, part in parts:
        start = int(offset * RATE)
        stop = min(len(result), start + len(part))
        result[start:stop] += part[:stop - start]
    peak = max(.001, float(np.max(np.abs(result))))
    return np.tanh(result / peak * 1.4) * .42


def motifs() -> dict[str, np.ndarray]:
    return {
        "impact": place([(0, tone(.22, 105, 72, (1, .45, .18))), (0, noise(.055, 11))], .24),
        "tempo": place([(0, tone(.075, 690, 830, (1, .25))), (.072, tone(.09, 810, 1040, (1, .22)))], .19),
        "work": place([(0, noise(.035, 23)), (0, tone(.10, 510, 370, (1, .35))), (.095, tone(.14, 185, 155, (1, .3)))], .25),
        "mobility": place([(0, noise(.19, 31, 5) * .22), (.015, tone(.23, 310, 780, (1, .18)))], .27),
        "endurance": place([(0, tone(.30, 145, 132, (1, .5, .2)))], .32),
        "robustness": place([(0, noise(.045, 47)), (0, tone(.17, 250, 118, (1, .55, .2)))], .19),
        "renewal": place([(0, tone(.17, 590, 760, (1, .18))), (.105, tone(.19, 790, 1080, (1, .15)))], .31),
        "control": place([(0, tone(.045, 470, 560)), (.052, tone(.045, 560, 650)), (.104, tone(.16, 760, 760, (1, .16)))], .29),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--badge-source", type=Path, required=True)
    parser.add_argument("--resource-root", type=Path, required=True)
    parser.add_argument("--namespace", required=True)
    parser.add_argument("--audio", action="store_true")
    parser.add_argument("--audio-from", type=Path)
    args = parser.parse_args()

    texture_dir = args.resource_root / "assets" / args.namespace / "textures" / "gui"
    texture_dir.mkdir(parents=True, exist_ok=True)
    with Image.open(args.badge_source) as image:
        if image.size != (144, 18):
            raise SystemExit(f"badge source must be 144x18, got {image.size}")
        image.convert("RGBA").save(texture_dir / "aspect_badges.png", optimize=False)

    if args.audio or args.audio_from:
        sound_dir = args.resource_root / "assets" / args.namespace / "sounds" / "aspect"
        sound_dir.mkdir(parents=True, exist_ok=True)
        if args.audio_from:
            for name in ASPECTS:
                shutil.copyfile(args.audio_from / f"{name}.ogg", sound_dir / f"{name}.ogg")
        else:
            for name, signal in motifs().items():
                sf.write(sound_dir / f"{name}.ogg", signal, RATE, format="OGG", subtype="VORBIS")


if __name__ == "__main__":
    main()

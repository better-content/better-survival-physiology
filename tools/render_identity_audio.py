#!/usr/bin/env python3
"""Render physical-foley Better Survival Physiology review candidates.

The renderer deliberately contains no oscillator or pitched-tone generator. Identity is
carried by recorded material, rhythm, envelope, filtering, and transient placement.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import html
from pathlib import Path
import tomllib

import numpy as np
import soundfile as sf
from scipy.signal import butter, resample, sosfilt, sosfiltfilt

RATE = 48_000
ASPECTS = ("impact", "tempo", "work", "mobility", "endurance", "robustness", "renewal", "control")
DURATIONS = {
    "impact": .36, "tempo": .22, "work": .38, "mobility": .42,
    "endurance": .50, "robustness": .34, "renewal": .48, "control": .36,
}
SELECTIONS = {
    "impact": "impact_c", "tempo": "tempo_b", "work": "work_a", "mobility": "mobility_a",
    "endurance": "endurance_e", "robustness": "robustness_b", "renewal": "renewal_b",
    "control": "control_b3",
}


class Foley:
    def __init__(self, source_root: Path):
        self.source_root = source_root
        self.cache: dict[str, np.ndarray] = {}

    def get(self, name: str) -> np.ndarray:
        if name not in self.cache:
            signal, rate = sf.read(self.source_root / f"{name}.wav", dtype="float64")
            if rate != RATE or signal.ndim != 1:
                raise ValueError(f"{name} must be mono {RATE} Hz")
            self.cache[name] = signal
        return self.cache[name].copy()

    def shape(self, name: str, *, start: float = 0, duration: float | None = None,
              speed: float = 1, reverse: bool = False, low: float | None = None,
              high: float | None = 45, attack: float = .003, release: float = .035) -> np.ndarray:
        signal = self.get(name)
        begin = max(0, int(start * RATE))
        end = len(signal) if duration is None else min(len(signal), begin + int(duration * RATE))
        signal = signal[begin:end]
        if reverse:
            signal = signal[::-1]
        if speed != 1 and len(signal):
            signal = resample(signal, max(1, int(len(signal) / speed)))
        signal = filtered(signal, low=low, high=high)
        return faded(signal, attack, release)


def filtered(signal: np.ndarray, *, low: float | None, high: float | None) -> np.ndarray:
    if not len(signal):
        return signal
    filters = []
    if high:
        filters.append(butter(3, high, btype="highpass", fs=RATE, output="sos"))
    if low:
        filters.append(butter(3, low, btype="lowpass", fs=RATE, output="sos"))
    for coefficients in filters:
        signal = sosfiltfilt(coefficients, signal) if len(signal) > 32 else sosfilt(coefficients, signal)
    return signal


def faded(signal: np.ndarray, attack: float, release: float) -> np.ndarray:
    result = signal.copy()
    a = min(len(result), int(attack * RATE))
    r = min(len(result), int(release * RATE))
    if a:
        result[:a] *= np.sin(np.linspace(0, np.pi / 2, a)) ** 2
    if r:
        result[-r:] *= np.cos(np.linspace(0, np.pi / 2, r)) ** 2
    return result


def mix(duration: float, *layers: tuple[float, float, np.ndarray]) -> np.ndarray:
    dry_samples = int(duration * RATE)
    result = np.zeros(dry_samples, dtype=np.float64)
    for offset, gain, signal in layers:
        begin = max(0, int(offset * RATE))
        end = min(len(result), begin + len(signal))
        if end > begin:
            result[begin:end] += gain * signal[:end - begin]
    return master(with_room_decay(result, tail_seconds=1.0))


def with_room_decay(dry: np.ndarray, *, tail_seconds: float) -> np.ndarray:
    """Add a quiet, diffuse decay made only from delayed copies of the recording.

    This is intentionally not a pitched resonator. A fixed irregular reflection field
    gives every cue enough air to leave the scene naturally while keeping its identity
    in the original physical foley.
    """
    tail_samples = int(tail_seconds * RATE)
    result = np.pad(dry, (0, tail_samples))
    wet = np.zeros_like(result)
    rng = np.random.default_rng(0x5A11EACE)
    delays = np.sort(rng.uniform(.032, tail_seconds, 180))
    gains = rng.choice((-1.0, 1.0), len(delays)) * np.exp(-delays / .31)
    gains *= .22 / np.sqrt(np.sum(gains ** 2))
    for delay, gain in zip(delays, gains, strict=True):
        shift = int(delay * RATE)
        available = min(len(dry), len(wet) - shift)
        wet[shift:shift + available] += gain * dry[:available]
    wet = filtered(wet, low=9_500, high=170)
    wet = faded(wet, 0, .20)
    result += wet
    return result


def master(signal: np.ndarray) -> np.ndarray:
    signal = filtered(signal, low=15_000, high=42)
    signal = faded(signal, .005, .012)
    active = np.abs(signal) > max(1e-6, np.max(np.abs(signal)) * .01)
    rms = float(np.sqrt(np.mean(signal[active] ** 2))) if np.any(active) else 0
    if rms:
        # Sparse mechanisms and dense cloth beds must compare fairly during A/B review.
        # A conservative -28 dBFS active RMS also leaves Minecraft's per-event volume
        # controls meaningful without hard-limiting tactile transients.
        signal *= 10 ** (-28 / 20) / rms
    peak = float(np.max(np.abs(signal))) if len(signal) else 0
    if peak > .501:
        signal *= .501 / peak
    return signal


def candidates(f: Foley) -> dict[str, np.ndarray]:
    s = f.shape
    return {
        "impact_a": mix(.36,
            (0, 1.00, s("owlish_hit", duration=.34, low=5_500)),
            (0, .46, s("kenney_wood", speed=.88, low=2_300)),
            (.015, .16, s("blanket_one", duration=.28, high=500, low=7_000, reverse=True))),
        "impact_b": mix(.36,
            (0, .90, s("owlish_slap", duration=.34, low=5_000)),
            (0, .52, s("kenney_soft", speed=.82, low=1_700)),
            (.045, .24, s("spring_stone_small", duration=.28, high=180, low=4_000))),

        "tempo_a": mix(.22,
            (0, 1.00, s("click_sixty", high=250, low=8_000, release=.018)),
            (.070, .92, s("click_eighty", duration=.10, high=250, low=8_000, release=.022)),
            (.012, .20, s("click_ten", duration=.16, high=800, low=6_000))),
        "tempo_b": mix(.22,
            (0, .92, s("click_twenty", duration=.09, high=200, low=7_000, release=.018)),
            (.082, 1.00, s("click_one", duration=.13, high=200, low=7_000, release=.025)),
            (.055, .14, s("mouse", duration=.15, high=900, low=6_000))),

        "work_a": mix(.38,
            (0, .88, s("kenney_mining", duration=.34, low=6_000)),
            (.080, .30, s("owlish_scrape", duration=.28, high=450, low=7_000)),
            (.175, .33, s("spring_stone_small", duration=.19, high=250, low=5_500))),
        "work_b": mix(.38,
            (0, .94, s("spring_stone_tap", duration=.30, high=100, low=5_000)),
            (.055, .40, s("spring_stone_break", duration=.29, high=300, low=6_500)),
            (.200, .18, s("spring_weeds", duration=.17, high=800, low=8_000, reverse=True))),

        "mobility_a": mix(.42,
            (0, .52, s("blanket_two", duration=.38, high=180, low=8_000, reverse=True, attack=.025)),
            (.045, .32, s("tissue_pull", duration=.34, high=450, low=9_000, reverse=True, attack=.030)),
            (.190, .12, s("spray", duration=.18, high=1_100, low=10_000))),
        "mobility_b": mix(.42,
            (0, .52, s("fabric_rustle", duration=.39, high=180, low=8_500, reverse=True, attack=.030)),
            (.060, .28, s("spring_weeds", duration=.32, high=400, low=8_000, reverse=True, attack=.025)),
            (.205, .16, s("page_turn", duration=.18, high=700, low=9_000))),

        "endurance_a": mix(.50,
            (0, .58, s("breath_male", duration=.48, high=90, low=2_500, attack=.035, release=.080)),
            (.025, .35, s("blanket_three", duration=.46, high=80, low=2_800, attack=.035, release=.080)),
            (0, .24, s("kenney_soft", duration=.30, speed=.72, low=700, release=.090))),
        "endurance_b": mix(.50,
            (0, .58, s("breath_female", duration=.48, high=100, low=2_800, attack=.040, release=.085)),
            (.020, .40, s("blanket_one", duration=.45, high=100, low=3_200, attack=.035, release=.080)),
            (0, .21, s("owlish_grab", duration=.34, speed=.78, low=850, release=.100))),

        "robustness_a": mix(.34,
            (0, .86, s("kenney_plate", duration=.30, high=90, low=5_800)),
            (0, .48, s("owlish_grab", duration=.32, speed=.88, low=2_200)),
            (.082, .26, s("click_forty", duration=.20, high=250, low=5_000))),
        "robustness_b": mix(.34,
            (0, .84, s("kenney_wood", duration=.30, speed=.90, low=3_500)),
            (.018, .48, s("spring_stone_tap", duration=.27, low=4_500)),
            (.075, .30, s("click_ten", duration=.20, high=220, low=5_000))),

        "renewal_a": mix(.48,
            (0, .52, s("water_tap", duration=.42, high=260, low=7_500, attack=.020, release=.075)),
            (.025, .33, s("breath_female", duration=.42, high=250, low=4_500, reverse=True, attack=.035, release=.075)),
            (.170, .16, s("fabric_rustle", duration=.25, high=700, low=9_000, reverse=True))),
        "renewal_b": mix(.48,
            (0, .46, s("pill", duration=.40, high=300, low=7_000, attack=.020, release=.080)),
            (.035, .34, s("spray", duration=.36, high=500, low=8_500, reverse=True, attack=.035, release=.075)),
            (.155, .20, s("page_turn", duration=.22, high=600, low=8_000, reverse=True))),

        "control_a": mix(.36,
            (0, .85, s("click_sixty", high=300, low=8_000, release=.015)),
            (.080, .78, s("click_twenty", duration=.075, high=300, low=8_000, release=.015)),
            (.140, .76, s("click_eighty", duration=.075, high=300, low=8_000, release=.015)),
            (.185, 1.00, s("click_ten", duration=.17, high=220, low=6_000, release=.035))),
        "control_b": mix(.36,
            (0, .44, s("mouse", duration=.30, high=250, low=6_500, attack=.008, release=.050)),
            (.030, .70, s("click_one", duration=.08, high=280, low=7_000, release=.015)),
            (.110, .76, s("click_forty", duration=.09, high=280, low=7_000, release=.015)),
            (.178, .94, s("click_eighty", duration=.15, high=220, low=6_000, release=.030))),

        # Second-review revisions.  These deliberately retain the recorded upper
        # transient detail that the first Impact/Endurance pair filtered away.
        "impact_c": mix(.36,
            (0, .88, s("owlish_hit", duration=.34, low=14_500, release=.045)),
            (.004, .52, s("kenney_plate", duration=.31, high=80, low=14_500, release=.050)),
            (.010, .28, s("kenney_metal", duration=.12, high=160, low=14_500, release=.025)),
            (.030, .13, s("blanket_one", duration=.25, high=350, low=14_500,
                          reverse=True, attack=.010, release=.050))),
        "impact_d": mix(.36,
            (0, .92, s("owlish_slap", duration=.34, low=14_500, release=.045)),
            (.008, .46, s("spring_stone_small", duration=.32, high=100, low=13_500,
                          release=.055)),
            (.055, .14, s("owlish_clamour", duration=.27, high=500, low=14_000,
                          release=.060))),

        "endurance_c": mix(.50,
            (0, .48, s("breath_male", duration=.48, high=65, low=14_500,
                        attack=.030, release=.080)),
            (.022, .24, s("blanket_three", duration=.46, high=70, low=14_500,
                          attack=.030, release=.080)),
            (.055, .28, s("fabric_rustle", duration=.40, high=350, low=15_000,
                          attack=.025, release=.075))),
        "endurance_d": mix(.50,
            (0, .48, s("breath_female", duration=.48, high=70, low=14_500,
                        attack=.035, release=.085)),
            (.018, .22, s("blanket_one", duration=.45, high=70, low=14_500,
                          attack=.030, release=.080)),
            (.090, .28, s("page_turn", duration=.34, high=250, low=15_000,
                          reverse=True, attack=.025, release=.070))),
        # The breath is reduced to reversed, band-limited airflow here: no intact
        # inhale/exhale envelope survives to read as a person close to the listener.
        "endurance_e": mix(.56,
            (0, .24, s("breath_male", start=.08, duration=.46, reverse=True,
                        high=780, low=13_500, attack=.075, release=.110)),
            (.012, .42, s("fabric_rustle", duration=.52, high=170, low=14_500,
                          reverse=True, attack=.045, release=.105)),
            (.055, .30, s("blanket_three", duration=.46, high=90, low=14_000,
                          attack=.045, release=.105)),
            (.015, .11, s("kenney_wood", duration=.28, high=65, low=1_500,
                          attack=.012, release=.120))),
        "endurance_f": mix(.56,
            (0, .20, s("breath_female", start=.12, duration=.42, reverse=True,
                        high=1_150, low=14_500, attack=.085, release=.120)),
            (.010, .40, s("spring_weeds", duration=.52, high=190, low=14_500,
                          reverse=True, attack=.050, release=.110)),
            (.045, .34, s("blanket_one", duration=.48, high=85, low=14_000,
                          reverse=True, attack=.055, release=.110)),
            (.018, .10, s("owlish_grab", duration=.30, high=70, low=1_600,
                          attack=.015, release=.130))),

        "control_b2": mix(.36,
            (0, .13, s("mouse", duration=.28, high=350, low=12_000,
                        attack=.006, release=.040)),
            (.025, .72, s("click_one", duration=.08, high=180, low=13_500, release=.014)),
            (.112, .80, s("click_forty", duration=.08, high=180, low=13_500, release=.014)),
            (.178, 1.00, s("click_eighty", duration=.16, high=150, low=12_500, release=.035)),
            (.218, .22, s("kenney_metal", duration=.11, high=280, low=13_000, release=.030))),
        "control_b3": mix(.36,
            (.020, .68, s("click_one", duration=.07, high=180, low=14_000, release=.012)),
            (.115, .76, s("click_forty", duration=.07, high=180, low=14_000, release=.012)),
            (.184, .86, s("click_ten", duration=.08, high=180, low=14_000, release=.014)),
            (.230, 1.00, s("click_eighty", duration=.12, high=140, low=13_500, release=.030)),
            (.238, .25, s("kenney_metal", duration=.10, high=250, low=14_000, release=.025))),
    }


def metrics(name: str, signal: np.ndarray) -> dict[str, str]:
    peak = float(np.max(np.abs(signal)))
    active = np.abs(signal) > max(1e-6, peak * .01)
    rms = float(np.sqrt(np.mean(signal[active] ** 2))) if np.any(active) else 0
    spectrum = np.abs(np.fft.rfft(signal * np.hanning(len(signal)))) + 1e-12
    flatness = float(np.exp(np.mean(np.log(spectrum))) / np.mean(spectrum))
    return {
        "candidate": name,
        "duration_ms": str(round(len(signal) / RATE * 1000)),
        "peak_dbfs": f"{20 * np.log10(max(peak, 1e-12)):.2f}",
        "active_rms_dbfs": f"{20 * np.log10(max(rms, 1e-12)):.2f}",
        "spectral_flatness": f"{flatness:.4f}",
    }


def contextual_variants(f: Foley, rendered: dict[str, np.ndarray]) -> dict[str, dict[str, np.ndarray]]:
    """Derive related, not byte-identical, cues for the three causal authorities."""
    result = {"rpg": {}, "nutrition": {}, "ore": {}}
    for aspect, selected in SELECTIONS.items():
        core = rendered[selected]
        # RPG allocation is the cleanest statement. Control keeps B3's gesture but
        # borrows the barely audible mechanism bed that made B2 equally successful.
        rpg = core.copy()
        if aspect == "control":
            mouse = f.shape("mouse", duration=.28, high=400, low=12_000,
                            attack=.006, release=.045)
            rpg[:len(mouse)] += .025 * mouse
        result["rpg"][aspect] = master(rpg)

        # Nutrition is embodied through a restrained cloth/body resonance without
        # introducing another recognizable breath recording.
        body = f.shape("blanket_three", duration=.44, high=85, low=4_800,
                       attack=.035, release=.095)
        nutrition = core.copy()
        offset = int(.018 * RATE)
        count = min(len(body), len(nutrition) - offset)
        nutrition[offset:offset + count] += .055 * body[:count]
        result["nutrition"][aspect] = master(nutrition)

        # Matter retains the gesture beneath a very quiet unpitched stone contact.
        stone = f.shape("spring_stone_small", duration=.36, high=75, low=2_600,
                        attack=.008, release=.100)
        ore = core.copy()
        offset = int(.012 * RATE)
        count = min(len(stone), len(ore) - offset)
        ore[offset:offset + count] += .075 * stone[:count]
        result["ore"][aspect] = master(ore)

    # Sugar crash: the Tempo mechanism misses its beat, scrapes, and settles.
    broken = mix(.54,
        (0, .72, f.shape("click_twenty", duration=.10, high=180, low=11_000, release=.022)),
        (.126, .50, f.shape("click_one", duration=.11, high=180, low=11_000, release=.030)),
        (.315, .86, f.shape("owlish_scrape", duration=.22, high=300, low=10_000,
                            reverse=True, attack=.020, release=.085)),
        (.378, .40, f.shape("click_eighty", duration=.13, high=180, low=10_000,
                            release=.050)))
    result["nutrition"]["tempo_broken"] = broken
    return result


def write_final(output: Path, variants: dict[str, dict[str, np.ndarray]]) -> None:
    for context, sounds in variants.items():
        context_root = output / context
        context_root.mkdir(parents=True, exist_ok=True)
        for name, signal in sounds.items():
            write_ogg(context_root / f"{name}.ogg", signal, f"{context}/{name}")


def write_ogg(path: Path, signal: np.ndarray, identity: str) -> None:
    """Write Vorbis and canonicalize libsndfile's randomized Ogg stream serial."""
    sf.write(path, signal, RATE, format="OGG", subtype="VORBIS")
    data = bytearray(path.read_bytes())
    serial = hashlib.sha256(identity.encode("utf-8")).digest()[:4]
    cursor = 0
    while cursor < len(data):
        if data[cursor:cursor + 4] != b"OggS":
            raise ValueError(f"invalid Ogg page at byte {cursor} in {path}")
        segments = data[cursor + 26]
        page_size = 27 + segments + sum(data[cursor + 27:cursor + 27 + segments])
        page = data[cursor:cursor + page_size]
        page[14:18] = serial
        page[22:26] = b"\0\0\0\0"
        page[22:26] = ogg_crc(page).to_bytes(4, "little")
        data[cursor:cursor + page_size] = page
        cursor += page_size
    path.write_bytes(data)


def ogg_crc(page: bytes | bytearray) -> int:
    crc = 0
    for value in page:
        crc ^= value << 24
        for _ in range(8):
            crc = ((crc << 1) ^ 0x04C11DB7) & 0xFFFFFFFF if crc & 0x80000000 else (crc << 1) & 0xFFFFFFFF
    return crc


def verify_sources(source_root: Path, manifest_path: Path) -> None:
    manifest = tomllib.loads(manifest_path.read_text(encoding="utf-8"))["manifest"]["clips"]
    declared = set()
    for entry in manifest:
        name, _pack, _original, _start, _duration, expected = entry.split("|")
        declared.add(name)
        actual = hashlib.sha256((source_root / f"{name}.wav").read_bytes()).hexdigest()
        if actual != expected:
            raise ValueError(f"source hash mismatch for {name}: {actual} != {expected}")
    present = {path.stem for path in source_root.glob("*.wav")}
    if present != declared:
        raise ValueError(f"source manifest mismatch: undeclared={present - declared}, missing={declared - present}")


def write_review(output: Path, rendered: dict[str, np.ndarray], *, title: str) -> None:
    output.mkdir(parents=True, exist_ok=True)
    rows = []
    for name, signal in rendered.items():
        path = output / f"{name}.ogg"
        write_ogg(path, signal, name)
        rows.append(metrics(name, signal))
    with (output / "metrics.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=rows[0].keys())
        writer.writeheader(); writer.writerows(rows)
    cards = []
    for aspect in ASPECTS:
        names = [name for name in rendered if name.startswith(aspect + "_")]
        if names:
            players = "".join(f"<label>{html.escape(name.removeprefix(aspect + '_').upper())} "
                              f"<audio controls src='{html.escape(name)}.ogg'></audio></label>"
                              for name in names)
            cards.append(f"<section><h2>{html.escape(aspect.title())}</h2>{players}</section>")
    (output / "review.html").write_text("<!doctype html><meta charset='utf-8'><title>Better Survival Physiology base A/B</title>"
        "<style>body{max-width:760px;margin:2rem auto;background:#16191d;color:#eee;font:16px sans-serif}"
        "section{border-bottom:1px solid #444;padding:.7rem}label{display:flex;align-items:center;gap:1rem;margin:.5rem}"
        f"audio{{flex:1}}</style><h1>{html.escape(title)}</h1>"
        "<p>Review at ordinary gameplay volume. Context variants come after base approval.</p>"
        + "".join(cards), encoding="utf-8")
    manifest = "\n".join(f"{hashlib.sha256((output / (name + '.ogg')).read_bytes()).hexdigest()}  {name}.ogg"
                         for name in rendered)
    (output / "SHA256SUMS").write_text(manifest + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, default=Path("audio/sources"))
    parser.add_argument("--source-manifest", type=Path, default=Path("audio/SOURCES.toml"))
    parser.add_argument("--output", type=Path, default=Path("build/audio-review/base"))
    parser.add_argument("--revisions", action="store_true",
                        help="render only the Impact, Endurance, and Control follow-up candidates")
    parser.add_argument("--endurance-followup", action="store_true",
                        help="render only the de-humanized Endurance follow-up candidates")
    parser.add_argument("--final-output", type=Path,
                        help="write approved RPG, nutrition, and ore contextual variants")
    args = parser.parse_args()
    verify_sources(args.source_root, args.source_manifest)
    foley = Foley(args.source_root)
    rendered = candidates(foley)
    if args.final_output:
        write_final(args.final_output, contextual_variants(foley, rendered))
        return
    if args.endurance_followup:
        wanted = {"endurance_e", "endurance_f"}
        rendered = {name: signal for name, signal in rendered.items() if name in wanted}
    elif args.revisions:
        wanted = {"impact_c", "impact_d", "endurance_c", "endurance_d", "control_b2", "control_b3"}
        rendered = {name: signal for name, signal in rendered.items() if name in wanted}
    else:
        wanted = {f"{aspect}_{variant}" for aspect in ASPECTS for variant in ("a", "b")}
        rendered = {name: signal for name, signal in rendered.items() if name in wanted}
    title = ("Better Survival Physiology — processed Endurance revisions" if args.endurance_followup
             else "Better Survival Physiology — full-band revisions" if args.revisions
             else "Better Survival Physiology — physical-foley base A/B")
    write_review(args.output, rendered, title=title)


if __name__ == "__main__":
    main()

# Better Survival Physiology identity audio

The eight approved gestures are Impact C, Tempo B, Work A, Mobility A, Endurance E,
Robustness B, Renewal B, and Control B3. Control B2 contributes only the faint mechanism
bed used by the RPG-context Control render.

Every gesture is assembled from recorded CC0 foley. The renderer contains no oscillator or
pitched-tone generator. It uses filtering, envelopes, unshifted recorded layers, irregular
reflections, and a quiet one-second decay. Endurance reduces a reversed, band-isolated breath
to airflow beneath cloth movement so no intact inhale/exhale remains. The broken Tempo render
is reserved for sugar crash.

`SOURCES.toml` records the source pages, licences, archive hashes, original filenames, excerpt
ranges, and hashes of every checked-in mono 48 kHz PCM24 excerpt. Re-render with:

```sh
python -m venv .venv
.venv/bin/pip install -r audio/requirements.txt
.venv/bin/python tools/render_identity_audio.py --final-output build/audio-final
```

The generated contexts are `rpg`, `nutrition`, and `ore`. Generated review and final output is
ignored; only the intentional runtime OGG files in each owning mod are committed.

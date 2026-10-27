# Spice badge — approved internal byte-identical reuse

The standalone native asset at
`src/main/resources/assets/better_survival_physiology/textures/gui/spice_badge.png`
is an unmodified byte-for-byte copy of Better Spicy Farmers' generated 18×18 RGBA native badge.
Root coordinator and art owner explicitly authorized this internal copy on 2026-10-26.
No atlas pixels, generated image pixels, palette, dimensions, or alpha were changed.

- Native source: `../better-spicy-farmers/src/main/resources/assets/better_spicy_farmers/textures/gui/spice_badge.png`
- Source and copied native SHA-256: `bf9a0486ee1cfeaff54ab5ecabaa04df4195fb4d22ed99abe107967238404f8a`
- Original generated image: `../better-spicy-farmers/assets-source/generated/spice-badge.png`
- Original image SHA-256: `65e004efa0000868a64215a08b6600c0010c664b0c0dba754ca3b4d2a4be8662`
- Prompt, generation route and source/native hashes:
  [`../better-spicy-farmers/assets-source/PROVENANCE.md#spice-badge`](../../better-spicy-farmers/assets-source/PROVENANCE.md#spice-badge),
  section beginning at line 127 when copied.

Art owner identifies the route as built-in image generation within its Codex GPT-6-Sol session;
the underlying image engine is undisclosed. This records generated origin and approved internal
reuse, **not** a third-party art attribution, independent license grant, or invented licensing terms.
There is no separate art license file supplied by the source owner.

The physiology-owned copy allows its standalone font to load without Better Spicy Farmers.
When that mod is present, the HUD selects the identical source badge in that mod; its absence
selects this owned copy. Real-client visual acceptance remains a separate qualification gate.

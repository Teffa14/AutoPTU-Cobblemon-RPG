# 0892 Urshifu — Cobra Dojo Champion

Status: `BLOCKED_NO_OFFICIAL_PRESENTATION_MODEL`

Production target: `Urshifu Single Strike Style`, normal non-G-Max form only.

The selected official species aspect is `single_strike-style`. The pinned Cobblemon species record identifies this form as Fighting/Dark and includes Wicked Blow. Rapid Strike Style, G-Max Single Strike Style and G-Max Rapid Strike Style are outside this skin slice.

This is a presentation-only Ouros skin direction for Urshifu Single Strike Style. The requested high-level fantasy is a ruthless black/gold martial-arts champion with a cobra-like visual language. The production design name is `Cobra Dojo Champion`; it must not reproduce third-party logos, wordmarks or distinctive protected insignia.

## Art direction when upstream source becomes available

The target must exceed the current minimum artistic floor established by Pikachu Storm Courier v4. It must read as a premium transformation at gameplay distance rather than Urshifu plus small accessories.

Planned dominant reads:

- a large high cobra-hood cowl/collar framing the official head and shoulders;
- a connected sleeveless gi/coat shell with broad black/charcoal masses and controlled gold edging;
- one asymmetric champion pauldron/fang-fan system to break symmetry deliberately;
- substantial forearm wraps/guards parented to the corresponding official animated arm bones;
- a wide champion belt and split front/rear sash system with large readable hanging planes;
- an original geometric fang/cobra motif, not copied branding;
- material breakup between cloth, wrapped fabric, lacquered guard surfaces and restrained metallic gold hardware.

The Single Strike identity should dominate the read: compact, heavy, direct and predatory. The silhouette should emphasize forward pressure and decisive striking rather than Rapid Strike's flowing/water-oriented visual language. This is an art-direction distinction only; no Cobblemon battle-state logic is used.

The official Urshifu anatomy must remain visible and intact under the added geometry. No replacement head, muzzle, eyes, ears, crest, neck, torso, limbs, hands, feet, pivots, locators, hierarchy, UVs or original bone names are permitted.

## Exact official source audit

Target release:

- Minecraft 1.21.1
- Cobblemon 1.7.3 Fabric
- Modrinth version id `kF7CvxTo`
- `Cobblemon-fabric-1.7.3+1.21.1.jar`
- JAR SHA-256 `f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3`
- JAR SHA-512 `7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e`

GitHub Actions run `33287893709` exhaustively inspected the exact official JAR by species name, national dex numbering variants and textual Urshifu references. It checked presentation-relevant model, Bedrock, poser, resolver, animation and Pokemon texture paths.

The release contains official Urshifu species data, Wushu Style feature assignment, Dynamax feature assignment, Pokédex data and sounds. The species data exposes Single Strike Style, Rapid Strike Style, G-Max Single Strike Style and G-Max Rapid Strike Style.

For this slice, the base species record is the selected form source: it carries aspect `single_strike-style`, secondary type `dark` and move `1:wickedblow`. No other Urshifu form is a production target.

The same release distributes zero Urshifu `.geo.json` files, zero Urshifu Pokemon textures, zero Urshifu posers, zero Urshifu resolvers and zero Urshifu animation files. No numbered `0892`/`892` presentation asset or shared presentation reference was found.

See `cobra-dojo-source-audit.json` for the machine-readable source contract, selected-form lock and exact hashes.

## Required outcome

Under the repository's `FUENTE OFICIAL O NADA` and `NO RECONSTRUIR ANATOMIA` rules, there is no lawful production body from which this Single Strike skin can currently be derived.

Do not:

- manually reconstruct Urshifu anatomy;
- derive Urshifu from Kubfu or another species;
- convert concept art or screenshots into geometry;
- clone Single Strike geometry from an unofficial source;
- substitute Rapid Strike or either G-Max model if one appears independently;
- create a placeholder body and later call it official;
- claim Blockbench acceptance without an upstream Single Strike model baseline.

The skin becomes implementable only when a stable Cobblemon release compatible with the repository target ships an official Urshifu Single Strike Style presentation model and corresponding presentation assets. At that point the slice must restart from those exact files, preserve every original bone in order, add only `ouros_*` cosmetic geometry and pass matched-camera Blockbench review with official animations.

## Authority boundary

Nothing in this source-audit slice gives Cobblemon or Minecraft battle-state authority. AutoPTU/Ouros remains authoritative for combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.

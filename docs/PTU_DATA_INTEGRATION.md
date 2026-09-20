# PTU data and creation profiles — 0.4.0-ptuprofiles1

This is a data-integration release, **not a completed PTU battle-engine migration**.
The user direction is PTU rules with Cobblemon Pokémon/presentation. Native battle
results must never be described as PTU results. No native damage, HP, PP or ability
is silently rewritten by this data attachment.

## Implemented

- Core-generated creation profiles now persist nature, allocated points, post-nature stats,
  base maximum HP and selected abilities on each resolved Pokémon UUID. See
  [creation profiles](PTU_CREATION_PROFILES.md) for the exact execution and remaining limits.
- The original Cobblemon starter selection, Pokémon Summary and PC now include a
  `Ficha PTU` / `PTU sheet` button. Its scrollable drawer stays inside the original
  screen; it does not create another starter, party, inventory or battle menu.
- Starter previews resolve the server's configured category and option, including
  species/form and PTU ability pools. Random starters are not rolled by previews;
  no Pokémon is created until Cobblemon handles its normal confirmation.
- Team/PC queries revalidate the requesting player's current ownership on the server.
  Clients send identity/selection only, with bounded payloads and per-player throttling.
  Selection correlation rejects late responses; resize reinstalls screen callbacks.
- Native starter, capture and acquisition events attach sheets and UUID-bound provenance
  immediately. Trading or moving between stores preserves recorded creation provenance.
  Native captures are explicitly recorded as `NATIVE_CAPTURE`, never as PTU rolls.
- The drawer consumes input within its bounds to avoid clicking native move/release
  controls behind it. Scroll or arrow keys navigate; R refreshes; Esc closes the drawer.
  Loading, unavailable, unsupported-server and missing-data states are explicit.
- Sixteen source datasets, including 36 PTU natures, are bundled in the mod.
- Source paths, source revision and per-file SHA-256 are recorded in `manifest.json`.
- The Python AutoPTU Moves Data CSV takes priority over supplemental move descriptions.
- Species/form records include PTU base stats, types, movement, capabilities, skills,
  naturewalk, size, weight and egg groups.
- Moves retain AC, Damage Base, category, frequency, range and effect descriptions.
  Base dice are obtained from the pinned AutoPTU-Java `PtuTables`, not a second table.
- Ability definitions retain frequency, trigger, effect, target, keywords and source.
  Species pools retain basic/advanced/high tiers; they are not all granted or activated.
- Learnsets include the source CSV, Galar/Hisui rulebook additions and the supplemental
  learnsets already used by Python AutoPTU. Their source remains recorded. Inherited
  positive-level moves are followed through the source lineage map, with cycle protection.
- A server-resolved sheet is attached to the actual Pokémon's persistent data under
  `autoptu:ptu_data_v1`; UUID and all other persistent keys are preserved.
- Loaded world Pokémon are attached on entity load. Player parties are refreshed at
  join and every five seconds for level, evolution, form, moves and ability changes.
  Unchanged inputs do not rewrite the record. Stored Pokémon are attached on withdrawal
  or explicitly through the authenticated player's `sync pc` command.
- Ambiguous identifiers, unknown forms and foreign namespaces do not silently fall back
  to an unrelated species. Missing definitions and incompatible native moves/abilities
  appear in the sheet's issues and can be inspected in-game.

## Commands (no OP unless noted)

```text
/autoptu ptu
/autoptu ptu party 1
/autoptu ptu target
/autoptu ptu move Tackle
/autoptu ptu ability Overgrow
/autoptu ptu learnset 1 1
/autoptu ptu sync
/autoptu ptu sync pc
/autoptu ptu audit
```

`party` and `learnset` take actual team slots 1–6. `target` uses the same visible,
wall-occluded eight-block raycast as native battle targeting. `audit` requires OP
and reports the dataset's duplicate/invalid records, not invented replacements.
Level-zero learnset entries are not automatically learned: the merged source does
not distinguish all TM, egg, tutor and unspecified acquisitions. A native ability
outside a PTU pool is reported, not automatically accepted as a PTU ability.

## Not implemented by this release

- **This is not the complete requested native-menu PTU gameplay conversion.**
  The creation-stat subset and ability selection now execute in Java, but acquired moves,
  progression, capture accuracy/rolls and battle results still need PTU integration.
- Python's capture resolver also includes accuracy, trainer features and ball effects:
  substituting a single chance formula would not be parity. The creation port does not
  claim to implement capture resolution.
- The native Cobblemon battle engine still runs `/autoptu battle wild` and native PvP.
  Attaching a sheet does not change that authority.
- PTU creation stats, point allocations, nature and selected abilities are generated by
  the core, not copied from native stats. Unlocks and an independent PTU move loadout
  are not inferred from native moves.
- Effect text is not an executable ability/move handler. Full action, targeting,
  accuracy, critical, STAB, damage, frequency, status, ability and outcome integration
  must be connected to the AutoPTU-Java runtime and checked against the Python oracle.
- No graphical end-to-end battle/persistence test has been performed for this release.

## Rebuild data

```powershell
./tools/import-ptu-catalog.ps1 -AutoPtuRoot C:/path/to/AutoPTU
```

The importer reads explicitly selected data files only; it does not execute the
Python application or modify that checkout. Generated JSON uses `-text` Git attributes
because runtime checksum verification is byte-for-byte. Review source diffs and run
`PtuDataCatalogTest` after each import. The corpus test checks every valid CSV move's
DB, AC, category, frequency, range and effect text against the loaded catalog.

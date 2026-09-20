# Native-menu PTU sheets: verification record

Version: `0.4.0-ptuprofiles1`. This is a partial integration, not a complete PTU gameplay release.

## Local checks performed

```powershell
gradle :fabric-adapter:test --tests '*Ptu*Test' --tests '*Native*Test' :fabric-adapter:packageNativeBattle
```

- Creation core separately passes three tests including 3,600 Python-oracle cases.
- 40 focused adapter tests pass, including canonical profile persistence/reconciliation;
  see `PTU_CREATION_PROFILES.md`. The earlier menus-only release passed 28 selected tests.
- Includes catalog/source corpus checks, native command/entrypoint checks, lifecycle
  identity/provenance preservation, request throttling, Unicode packet round trips,
  payload size limits and drawer bounds at multiple scaled screen dimensions.
- Production-remapped JAR and the four-mod distribution ZIP built successfully.
- The bundled manifest now covers 16 resources, including the nature CSV export.

These tests do **not** prove a graphical playthrough, packet ownership checks against
a live player, native event ordering, or persistence after a real game restart.

## Graphical acceptance still required

Use a copied test world, client and server with matching mod versions.

1. On the native starter screen, select a species, open PTU sheet, inspect base
   stats/pools, close with Esc, and confirm with Cobblemon's original button.
   Confirm exactly one starter is granted and no parallel starter screen appears.
2. On a random starter category, confirm preview does not resolve a random species.
3. Open the native Summary. Inspect PTU data, scroll and refresh. Switch Pokémon
   quickly: no late response may show the previous Pokémon's sheet.
4. Resize the window/change GUI scale and reopen the sheet. Check button focus,
   panel bounds, scroll, Esc and no duplicated callbacks/buttons.
5. Open the native PC. Inspect a stored Pokémon, move it to the party and back.
   Verify the same UUID and provenance; clicks inside the drawer must not release,
   move or change a Pokémon behind it.
6. Capture an actual wild Pokémon using native Cobblemon. Its sheet must explicitly
   say native capture, not PTU capture. Verify no duplicate grant.
7. Save/restart and check data/provenance again. Trade to another player; creation
   provenance must survive, while the old owner can no longer retrieve its sheet.
   The saved PTU nature, points, stats and abilities must not reroll. A native level
   or species change must flag reconciliation and preserve the saved profile.
8. Test missing species/forms and a server without the sheet protocol. The screen
   must show a clear unavailable/unsupported state, not invent data or hang input.

No screenshot, video or end-to-end success is claimed for this release.

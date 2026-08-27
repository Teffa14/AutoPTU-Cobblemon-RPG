# Grand Palace V4 browser review — 2026-08-27

The low-memory viewer is now loading the authoritative Fabric geometry reliably. The first clean browser review exposed visual-authoring defects rather than data/export defects.

## Review findings

- Diagnostic ten-material sampler strips were still visible inside authored salons.
- The shared baseline furniture grammar created repeated tables, solid color bars and oversized chandeliers that read as test fixtures rather than palace interiors.
- Blooming Salon used full-height fence trellises that read as structural poles.
- Blue Salon used long full-block wool benches that read as barricades.
- Accounting Office and Railing Salon repeated furniture across the whole room instead of preserving circulation and hierarchy.
- Gallery of Art is the only ground side pavilion without a second authored room above it; its former low ceiling left an unfinished intermediate volume below the roof.
- The first V4 independent-roof proof used too many tall stepped pyramids. Independent roofs were preserved, but the roofscape overwhelmed the facades and flattened architectural hierarchy.

## Implemented response

- `OurosGrandPalaceV4InteriorRefinementPass` removes surviving diagnostic sample blocks, repaints only the generic interior wall layer, converts surviving baseline accent bars to carpet, replaces oversized chandeliers with compact hanging crowns, and gives the most visibly problematic rooms dedicated layouts.
- Gallery of Art now opens into an upper clerestory volume with balcony and tall windows instead of ending at an exposed intermediate deck.
- `OurosGrandPalaceV4AuthoredRoofPass` replaces the tall per-room roof mountains with low mansards, shallow upper roofs, two central cupolas and only selective dormers.
- Structural, room-count, capture-envelope, anti-box and browser-memory gates remain authoritative and unchanged.

Do not accept this visual pass on source inspection alone. Re-export exact Fabric geometry and review the deployed browser mesh before merging.

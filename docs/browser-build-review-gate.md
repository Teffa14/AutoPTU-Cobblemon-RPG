# Browser Build Review Gate

Status: REQUIRED FOR SIGNATURE STRUCTURES AND REGIONAL SET PIECES

Important Ouros Minecraft spaces must be reviewable without launching Minecraft.

Before a SIGNATURE STRUCTURE or REGIONAL SET PIECE can be described as showcase-ready, it must expose a browser Build Review containing an approach view, an interior or sectional view, a circulation-oriented view, footprint and height, the current quality classification and visible parity gaps.

The Build Review is an approval surface. It does not replace Minecraft playtesting.

The preferred end state is one geometry source feeding Minecraft placement and exact schematic export. Exact schematic artifacts should then be inspectable through a browser viewer such as Craftmatic, SchemViewer or another compatible WebGL viewer. Regional world placement should additionally be inspectable through BlueMap when reproducible world saves exist.

Temporary hand-maintained preview geometry is allowed only for GAMEPLAY PROTOTYPE review and must say that exact block parity is pending.

A major build with no browser-visible review evidence cannot pass visual approval.

## Build-doctrine enforcement

`docs/ouros-build-doctrine.md` is part of this gate.

The browser capture must contain the complete authored structure. Do not clip a tower, roof, basement, terrain transition, service wing or other meaningful geometry because an older review envelope was smaller.

There is no fixed Ouros review-height cap. Capture bounds must follow the actual build, within Minecraft world limits and safe implementation constraints.

For SIGNATURE STRUCTURE review, inspect at minimum:

- distant or three-quarter silhouette;
- opposite/service-side three-quarter view;
- high roof-composition view;
- ground or cutaway circulation view;
- primary gameplay destination such as the leader arena;
- representative close-detail areas when the viewer supports useful inspection at that scale.

A technically valid build must still fail visual approval when the review shows doctrine violations such as dominant exposed scaffolding, unfinished roof enclosure, visually unsupported masses, simplistic arena composition, flat facade treatment, repeated procedural vegetation, obvious terrain plates or insufficient close-range detailing.

Automated structural evidence and browser visual approval are separate required gates.

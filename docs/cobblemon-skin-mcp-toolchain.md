# Cobblemon Skin MCP Toolchain

Effective: 2026-08-31

This document defines the supported local MCP-assisted workflow for Ouros Cobblemon model development. It complements `cobblemon-skin-blockbench-authoring.md` and does not change the AutoPTU battle-authority boundary.

The currently reviewed upstreams are pinned in `tools/cobblemon-model-review/mcp-toolchain.lock.json`.

## Blockbench MCP

Upstream: `jasonjgardner/blockbench-mcp-plugin`.

Reviewed upstream commit: `6b069e308fdfc9b0a1c15bc924ca78150815f143`.

License: GPL-3.0. Ouros uses it as an external authoring tool. Do not vendor its source into this repository unless a separate license review explicitly approves that change.

The plugin exposes Blockbench-native project, cube, element, mesh, armature, texture, paint, UV, animation, camera, import, export and history operations through MCP. Its mesh surface includes placement, extrusion, subdivision, vertex movement, face creation, cylinder creation and knife operations. Several mesh operations are marked experimental upstream, so a successful MCP call is not proof that Cobblemon runtime serialization is safe.

This MCP is the preferred live authoring bridge for premium Ouros skin geometry. The agent should use it to manipulate the model in Blockbench, inspect the result visually, iterate on contour and then save/export the authored source. It replaces the old habit of making artistic decisions by editing Python cube literals.

Default local endpoint: `http://127.0.0.1:3000/bb-mcp`.

Blockbench installation procedure:

1. Open the desktop Blockbench application.
2. Open File > Plugins.
3. Choose Load Plugin from URL.
4. Load `https://jasonjgardner.github.io/blockbench-mcp-plugin/mcp.js`.
5. In Settings > General, keep the MCP server bound to loopback and use port `3000` with endpoint `/bb-mcp` unless the local setup requires a different port.

Do not expose the Blockbench MCP server on a public interface.

## Minecraft Dev MCP

Upstream: `MCDxAI/minecraft-dev-mcp`.

Reviewed upstream commit: `1120c5b4ad78f56a879553722b23da219208eeeb`.

License: MIT.

Minecraft Dev MCP is the runtime/source-inspection bridge. It provides Minecraft decompilation, source lookup, mappings, registry extraction, Minecraft-version comparison, Mixin validation, access-widener/access-transformer validation, third-party mod JAR analysis, mod decompilation and indexed source search.

For the Cobblemon skin pipeline it is useful for questions such as:

- which Minecraft rendering or resource path is active in the target version;
- how a relevant Fabric/Cobblemon class loads or transforms model resources;
- whether a proposed integration requires a Mixin or access widener;
- whether a runtime behavior changed between Minecraft versions;
- where a third-party mod class implements a presentation hook we need to understand.

It is not an art-authoring tool. It must not decide combatants, HP, legality, RNG, damage, tactical positions or battle outcomes. AutoPTU remains authoritative for those facts.

Recommended local HTTP startup for this project:

`npx -y @mcdxai/minecraft-dev-mcp --http --host 127.0.0.1 --port 3001`

Default local endpoint: `http://127.0.0.1:3001/mcp`.

## Project MCP configuration

The repository contains `.vscode/mcp.json` with separate loopback endpoints for both services. Port separation is deliberate: Blockbench owns `3000`; Minecraft Dev MCP owns `3001`.

A local authoring session is considered MCP-ready only when both endpoints required for the task are reachable. Blockbench MCP is mandatory for a premium geometry authoring pass. Minecraft Dev MCP is mandatory only when the pass depends on Minecraft/Cobblemon implementation details that are not already proven by pinned assets and existing tests.

## Model-authoring procedure

Start from the exact pinned official Cobblemon species model and official animation assets. Never start from a rejected Ouros costume geometry and try to polish it into acceptance.

Open the species model in the pinned Blockbench version. Use Blockbench MCP to create only `ouros_*` additions. Preserve every official biological bone, cube, pivot, locator, UV definition and ordering required by the professional validators.

Work on one signature macro-form at a time. Use camera changes and animation states throughout the modeling pass. Correct silhouette, taper, depth, negative space, attachment and side/rear composition before texture detail.

When a capability is uncertain, use Minecraft Dev MCP to inspect the runtime or mod implementation and then prove the result with an isolated fixture. Do not weaken the validator because Blockbench can display a feature.

After the authored form is visually acceptable, save/export the exact authored source. The deterministic materializer may normalize or merge authored bytes into production, but it may not redesign them.

Run the repository validators and matched-camera Blockbench evidence pass. If CI is green but the owner rejects the visual result, the candidate remains rejected.

## Lucario V41 immediate use

Lucario V41 is the first candidate governed by this MCP-assisted standard.

The next modeling session must open the official Cobblemon 1.7.3 Lucario model in Blockbench and build the cap and garment silhouette through Blockbench MCP. V40 cosmetic geometry is rejection evidence only. Do not use its stacked cap or board-like apron as a starting mesh.

The first checkpoint is deliberately narrow: a cap and torso/waist garment silhouette that reads correctly in front, side, rear, three-quarter and 160 px gameplay views before accessory detail is added.

If that checkpoint still reads as stacked slabs, boxes or a front plate, discard the new cosmetic geometry and rebuild the macro-form instead of adding detail.

# Test evidence and recordings

Integration CI keeps durable evidence for every run instead of relying only on the final GitHub Actions status.

## Evidence produced today

`Integration Core CI` uploads an artifact named `integration-test-evidence-<run-id>-<attempt>` on success and failure. The artifact is retained for 14 days and contains, when available:

- `test-evidence/run-metadata.txt`: repository, commit SHA, run ID, run attempt and trigger event.
- `test-evidence/gradle-test.log`: complete stdout/stderr from the authority test run.
- `test-evidence/prepare-production-smoke.log`: complete output from production smoke-mod preparation.
- `test-evidence/runtime/fabric-server-smoke.log`: dedicated-server process output.
- `test-evidence/runtime/minecraft-latest.log`: Minecraft/Fabric/Cobblemon runtime log.
- `test-evidence/runtime/minecraft-debug.log`: debug log when Minecraft creates it.
- `test-evidence/runtime/smoke-markers.txt`: required runtime acceptance markers, including explicit `MISSING:` entries when collection cannot find one.
- Gradle/JUnit XML and HTML reports from the root project and Fabric adapter when those files exist.

The collection and artifact upload steps use `if: always()`. A failed test or failed dedicated-server boot therefore still preserves diagnostic evidence whenever the runner produced it.

## Visual recordings

The current production smoke boots a `nogui` dedicated Minecraft server. It has no rendered frames, so an MP4 recording would not add visual information. Server-side integration evidence is therefore logs, JUnit reports and acceptance markers.

Graphical client tests must write visual evidence under `test-evidence/visual/`. CI uploads that directory without requiring another workflow change. Use:

- MP4 for an end-to-end interaction or battle sequence.
- PNG for a stable state that is easier to inspect as a still frame.
- Runtime logs alongside the visual evidence so the rendered behavior can be matched to the authoritative server events.

A graphical test should record the commit/run metadata already written by CI. Randomized tests must also record their seed and relevant input fixture so a failure can be reproduced exactly.

## Acceptance rule

A green workflow result is necessary but is not the only evidence for a live integration slice. Runtime claims should point to the corresponding artifact contents. For a failure, preserve the same evidence set so before/after regression comparisons remain possible.

For the battle interception smoke, the minimum runtime evidence remains:

1. Fabric adapter initialization.
2. Cobblemon runtime detection.
3. Relocation smoke completion.
4. HP projection smoke completion.
5. Battle interception and canonical reservation smoke completion.
6. Minecraft server readiness.

The smoke-marker file records those exact checks from the live server log.

## Future graphical harness

When the project adds an automated Minecraft client under a virtual display, the harness should capture the client window to `test-evidence/visual/*.mp4` and store screenshots at important checkpoints. The artifact contract is already in place; only the graphical runner and capture process remain to be added.

Do not infer PTU correctness from the video. Visual evidence proves projection and user-visible sequencing. Canonical legality, calculations and state transitions still require server-authoritative tests and logs.

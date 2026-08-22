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

Graphical client tests must write visual evidence under `test-evidence/visual/`. CI uploads that directory without requiring another workflow change. Use MP4 for end-to-end interaction or battle sequences, PNG for stable checkpoints, and keep the runtime logs alongside the visual evidence so rendered behavior can be matched to authoritative server events.

A graphical test should record the commit/run metadata already written by CI. Randomized tests must also record their seed and relevant input fixture so a failure can be reproduced exactly.

## Acceptance rule

A green workflow result is necessary but is not the only evidence for a live integration slice. Runtime claims should point to the corresponding artifact contents. For a failure, preserve the same evidence set so before/after regression comparisons remain possible.

For the current production smoke, the minimum runtime evidence is Fabric adapter initialization, Cobblemon runtime detection, relocation smoke completion, HP projection smoke completion, battle interception/canonical reservation completion, authenticated-player-context fail-closed completion, and Minecraft server readiness.

The authenticated-player marker is `AutoPTU live authenticated player context smoke passed`. It proves that the production `MinecraftServer`/`PlayerManager` path rejects a known offline UUID and malformed actor identity before the canonical PTU context source is queried. Because the current runner has no logged-in client, this marker does not prove the successful authenticated encounter path.

The smoke-marker file records those exact checks from the live server log.

## Future graphical harness

When the project adds an automated Minecraft client under a virtual display, the harness should capture the client window to `test-evidence/visual/*.mp4` and store screenshots at important checkpoints. The next graphical acceptance sequence should show a connected player starting a Cobblemon encounter, `BATTLE_STARTED_PRE` reaching the adapter, successful player-session authentication, canonical player-versus-wild reservation and Cobblemon preemption.

Do not infer PTU correctness from the video. Visual evidence proves projection and user-visible sequencing. Canonical legality, calculations and state transitions still require server-authoritative tests and logs.

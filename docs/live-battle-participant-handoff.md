# Live Cobblemon battle participant handoff

This note records the runtime boundary exercised by the dedicated-server battle interception smoke.

Cobblemon 1.7.3 exposes `BATTLE_STARTED_PRE` before a `PokemonBattle` is registered/launched. The integration reads only identity data needed for canonical lookup:

- battle UUID
- side number
- actor kind (`PLAYER`, `NPC`, `WILD`)
- actor UUID
- Pokémon UUIDs owned by that battle actor

The claim handler receives an adapter-owned DTO containing strings/enums/lists only. No Minecraft, Fabric, Cobblemon, Pokémon-stat, move, HP, ability, item or Showdown type is present in that DTO.

These UUIDs are not authoritative PTU state. They are external identity keys. `CobblemonCanonicalEncounterIdentityRegistry` maps them to canonical participant/combatant IDs only. It rejects missing identities, participant roster mismatches and identity aliasing. It does not carry battle values.

`CobblemonBattleStartReservationCoordinator` then submits only the resolved canonical IDs to `BattleEncounterRosterReservationService`. The authority service re-resolves canonical records, freezes owner-neutral combatant state, generates battle team IDs server-side and atomically reserves both encounter rosters. Cobblemon cancellation is accepted only when that reservation succeeds.

The runtime smoke intentionally creates canonical wild combatant fixtures independently from the live `PokemonEntity` objects. Species ID, level and HP used in the canonical reservation are fixed server-owned fixture values. The live entities contribute UUID identity keys only.

The original `BattleAuthoritySnapshot` remains the single-player Trainer/item authority path. The newer multi-side roster reservation does not replace its Trainer/item/arena guarantees yet. The smoke is wild-versus-wild because dedicated CI has no authenticated client player, so it proves the identity-to-canonical-roster path rather than full player encounter composition.

Runtime acceptance criteria:

1. start a real Cobblemon battle from live `PokemonEntity` fixtures;
2. intercept `BATTLE_STARTED_PRE`;
3. capture both side/actor/Pokémon UUID identities exactly;
4. map each external identity to independently seeded canonical IDs;
5. resolve those canonical IDs through the server-owned authority repository;
6. atomically reserve both canonical encounter rosters with explicit opposing teams;
7. cancel the Cobblemon battle only after reservation succeeds;
8. prove `BattleRegistry` never registers the Cobblemon battle;
9. prove `BATTLE_STARTED_POST` never fires;
10. verify the stored reservation contains the fixed canonical IDs/level/HP values rather than live Cobblemon battle values.

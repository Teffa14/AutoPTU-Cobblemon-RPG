# Live Cobblemon battle participant handoff

This note records the runtime boundary exercised by the dedicated-server battle interception smoke.

Cobblemon 1.7.3 exposes `BATTLE_STARTED_PRE` before a `PokemonBattle` is registered/launched. The integration reads only identity data needed for a later canonical lookup:

- battle UUID
- side number
- actor kind (`PLAYER`, `NPC`, `WILD`)
- actor UUID
- Pokémon UUIDs owned by that battle actor

The claim handler receives an adapter-owned DTO containing strings/enums/lists only. No Minecraft, Fabric, Cobblemon, Pokémon-stat, move, HP, ability, item or Showdown type is present in that DTO.

These UUIDs are not authoritative PTU state. They are external identity keys. A future reservation coordinator must resolve them through server-owned canonical repositories before creating a reservation.

The current `BattleAuthoritySnapshot` is deliberately not reused for a full encounter yet because it models one canonical player and requires every Pokémon/item in the snapshot to share that owner. A player-versus-wild or player-versus-NPC encounter needs an explicit multi-side authority model first. Synthetic ownership is not an acceptable workaround.

Runtime acceptance criteria for this slice:

1. start a real Cobblemon battle from live `PokemonEntity` fixtures;
2. intercept `BATTLE_STARTED_PRE`;
3. capture both side/actor/Pokémon identities exactly;
4. cancel the Cobblemon battle;
5. prove `BattleRegistry` never registers it;
6. prove `BATTLE_STARTED_POST` never fires.

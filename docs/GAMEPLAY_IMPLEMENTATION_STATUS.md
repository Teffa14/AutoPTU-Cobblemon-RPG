# Gameplay implementation status

This file tracks mechanics that a player can execute in the current Minecraft/Cobblemon build. It is not a design backlog.

## 2026-09-03

### Persistent Marea exploration

- Puerto Bruma, Sendero del Vidrio, Loma Clara and Estacion Mirador use fixed server-authored world anchors.
- Entering their server-observed discovery footprint persists the location for the authenticated Trainer.
- Discovery survives restart and can advance location-driven quest objectives.
- The world hierarchy is Ouros -> Marea -> locality/route. This hierarchy does not replace physical Minecraft coordinates.

### Discovered fast travel

- The travel catalogue now exposes Overworld Spawn plus Puerto Bruma, Sendero del Vidrio, Loma Clara and Estacion Mirador.
- A Marea destination is locked until the Trainer has physically discovered its canonical location.
- Travel starts only from an observed nearby vanilla lodestone.
- `/autoptu travel` lists READY/LOCKED/UNAVAILABLE state from server-owned data.
- `/autoptu travel <destination>` accepts only a destination id. The server resolves the destination coordinate from `CanonicalWorldMapCatalogue` and performs the teleport.
- Clients cannot submit trusted coordinates or unlock state.

### Visible wild ecology

- Authored Marea habitats provision canonical visible wild populations only while players are within activation/retention footprints.
- Wild actors preserve canonical encounter identity across ordinary chunk unload/reload and hibernation.
- True destruction is reconciled by the server without replacing unrelated population identities.

### Current battle boundary

- Visible wild interaction and immutable encounter handoff are implemented.
- Normal production battle-start remains blocked until AutoPTU-Java exposes the missing authoritative movement profile, dynamic accuracy/evasion values/flags and damage modifiers. These values must not be fabricated in this repository.

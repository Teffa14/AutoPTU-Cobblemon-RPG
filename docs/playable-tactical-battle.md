# Playable tactical battle sandbox

Use Minecraft 1.21.1, Java 21, Fabric Loader 0.18.2, Fabric API 0.116.11+1.21.1,
Fabric Language Kotlin 1.13.6+kotlin.2.2.20 and Cobblemon 1.8.0+1.21.1.
Put the remapped AutoPTU mod in the client and server mods folders.
The mod embeds its pinned AutoPTU-Java core; no separate core JAR is needed.
See [Local build](build-local.md).

## Play

In a flat, clear area of a world where you have operator commands:

```text
/autoptu admin battle play charmander pikachu
```

Species names choose the displayed Cobblemon models. Combat stats and moves belong to the
fixed server-owned sandbox profile; they are not imported from the displayed species.

- Press B to open actions. The player turn waits for input.
- Choose an attack or a movement destination. The field highlights the choice.
- Enter confirms that particular preview; Backspace cancels it.
- Movement consumes Shift. You can then attack from the new position.
- Confirming an attack plays its windup and result, then the rival responds.
- End turn in the menu passes unused actions to the rival.
- `/autoptu admin battle stop` closes your sandbox session immediately.
- After cleanup, repeat the play command for another match.

Key bindings can be changed in Minecraft Controls. Chat fallback commands remain available:
`/autoptu battle choices`, `/autoptu battle preview <choiceId>`,
`/autoptu battle confirm <token>`, `/autoptu battle cancel`, `/autoptu battle endturn`.
A token belongs to a single preview; an old confirmation cannot execute a replacement choice.

## Field and feedback

The whole 7×4 board is sent as one visual frame. Cyan lines mark cells, green marks legal
movement, gold marks movement selection, and red marks attack targeting. Only cells included
in the core's legal action list are shown as legal. The connector is an aiming cue, not a path.

The HUD shows actual server HP, damage changes, turn, round, attack windup and impact.
Hit, miss and critical animation flags come from the resolved core event. The rival moves toward
the player using a legal Shift if it cannot reach with an attack. Victory/defeat remains visible
briefly before entity and HUD cleanup.

| Custom attack | Range | Damage base | Accuracy class | Presentation |
|---|---:|---:|---:|---|
| demo-strike | 5 | 4 | 2 | Melee-style cue |
| demo-burst | 2 | 6 | 4 | Fire burst |
| demo-arc | 3 | 3 | 1 | Electric arc |

These are custom single-target profiles. The burst animation does not imply area damage.
AutoPTU-Java determines range eligibility, action consumption, accuracy, damage and HP changes.

## Automatic demonstration

`/autoptu admin battle demo <species> <opponent>` and the legacy
`/autoptu testbattle charmander` run automatically for presentation demonstrations.
They do not attach to the interactive action menu.

## Scope and verification

This is an operator sandbox, not the normal PLAYER-vs-WILD campaign path. It does not commit XP,
loot, capture, progression, statuses, abilities, items, Trainer Features, weather or terrain effects.
The displayed Pokémon's native stats and HP never become PTU authority.

The changes were compiled and packaged locally. Focused checks exercise range filtering,
each attack's resolution and action consumption, and rejection of a changed target after
preview. A full manual playthrough of this revised client has not yet been recorded.

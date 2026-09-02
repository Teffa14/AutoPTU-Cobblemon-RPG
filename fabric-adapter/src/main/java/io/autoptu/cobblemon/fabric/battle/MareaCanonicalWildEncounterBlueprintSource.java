package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fabric-side adapter from project-owned Marea encounter content to the trusted WILD blueprint boundary.
 *
 * <p>No Cobblemon entity/Pokemon data is accepted. All PTU values already exist in the root canonical
 * catalogue before this source is queried.</p>
 */
public final class MareaCanonicalWildEncounterBlueprintSource implements CanonicalWildEncounterBlueprintSource {
    private final CanonicalWildEncounterCatalogue catalogue;

    public MareaCanonicalWildEncounterBlueprintSource() {
        this(CanonicalWildEncounterCatalogue.DEFAULT);
    }

    public MareaCanonicalWildEncounterBlueprintSource(CanonicalWildEncounterCatalogue catalogue) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
    }

    @Override
    public Optional<CanonicalWildEncounterBlueprint> resolve(String canonicalEncounterId) {
        return catalogue.encounter(canonicalEncounterId).map(MareaCanonicalWildEncounterBlueprintSource::blueprint);
    }

    private static CanonicalWildEncounterBlueprint blueprint(
            CanonicalWildEncounterCatalogue.EncounterDefinition definition
    ) {
        var pokemon = new ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint(
                definition.speciesId(),
                definition.level(),
                definition.capabilities(),
                definition.statuses(),
                definition.statusState(),
                definition.combatStats(),
                definition.health(),
                definition.moveLoadout(),
                definition.baseMovement(),
                definition.battleTraits(),
                definition.accuracyEvasion(),
                definition.injuryState(),
                definition.heldItemInstanceId(),
                definition.revision()
        );
        return new CanonicalWildEncounterBlueprint(
                definition.canonicalEncounterId(),
                definition.side(),
                List.of(pokemon)
        );
    }
}

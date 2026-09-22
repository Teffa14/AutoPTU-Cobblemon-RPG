package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.CobblemonNetwork;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.net.messages.client.ui.SummaryUIPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetail;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetailService;
import io.autoptu.cobblemon.fabric.network.FabricCanonicalPokemonSummaryPayload;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opens Cobblemon's native read-only Summary UI from server-authoritative PTU snapshots.
 *
 * The Pokemon instances sent to Cobblemon are disposable presentation DTOs only. They are never
 * inserted into Cobblemon storage and are never read back as RPG truth. Exact HP and combat stats
 * are sent in an AutoPTU S2C projection keyed by each disposable Pokemon UUID.
 */
public final class FabricPokemonDetailRuntime {
    private static final int MAX_PARTY_SLOTS = 6;

    private FabricPokemonDetailRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("pokemon")
                                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, MAX_PARTY_SLOTS))
                                        .executes(context -> show(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "slot")
                                        ))))));
    }

    private static int show(ServerCommandSource source, int slot) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Pokemon inspection must be requested by an authenticated player."));
            return 0;
        }
        return openScreen(player, slot) ? 1 : 0;
    }

    public static boolean openScreen(ServerPlayerEntity player, int slot) {
        if (player == null || player.getServer() == null || slot < 1 || slot > MAX_PARTY_SLOTS) return false;

        ArrayList<CanonicalPokemonDetail> details = new ArrayList<>();
        int selection = -1;
        for (int partySlot = 1; partySlot <= MAX_PARTY_SLOTS; partySlot++) {
            CanonicalPokemonDetail detail = queryDetail(player, partySlot);
            if (detail == null) continue;
            if (partySlot == slot) selection = details.size();
            details.add(detail);
        }

        if (selection < 0 || details.isEmpty()) {
            player.sendMessage(Text.literal("No canonical Pokemon exists in party slot " + slot + "."), false);
            return false;
        }

        if (selection > 0) {
            CanonicalPokemonDetail selected = details.remove(selection);
            details.addFirst(selected);
        }

        ArrayList<Pokemon> presentationParty = new ArrayList<>();
        ArrayList<FabricCanonicalPokemonSummaryPayload.Projection> projections = new ArrayList<>();
        for (CanonicalPokemonDetail detail : details) {
            Pokemon presentation = createPresentationPokemon(detail);
            if (presentation == null) {
                player.sendMessage(Text.literal(
                        "Cannot open the native summary because Cobblemon has no presentation species for "
                                + detail.speciesId() + "."), false);
                return false;
            }
            presentationParty.add(presentation);
            projections.add(FabricCanonicalPokemonSummaryPayload.Projection.from(
                    presentation.getUuid(), detail));
        }

        ServerPlayNetworking.send(player, new FabricCanonicalPokemonSummaryPayload(projections));
        player.sendMessage(Text.literal(conditionLabel(details.getFirst())), true);
        CobblemonNetwork.INSTANCE.sendPacketToPlayer(
                player,
                new SummaryUIPacket(List.copyOf(presentationParty), false)
        );
        return true;
    }

    static Pokemon createPresentationPokemon(CanonicalPokemonDetail detail) {
        if (detail == null) return null;
        Species species = PokemonSpecies.INSTANCE.getByName(speciesPath(detail.speciesId()));
        if (species == null) return null;

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(detail.level());
        if (detail.health() != null) {
            pokemon.setCurrentHealth(Math.min(detail.health().currentHp(), pokemon.getMaxHealth()));
        }
        return pokemon;
    }

    static CanonicalPokemonDetail queryDetail(ServerPlayerEntity player, int slot) {
        if (player == null || player.getServer() == null || slot < 1) return null;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPokemonDetailService service = new CanonicalPokemonDetailService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        try {
            return service.findPokemon(playerId, slot).orElse(null);
        } catch (IllegalStateException invalidCanonicalState) {
            player.sendMessage(Text.literal("AutoPTU Pokemon state is inconsistent and cannot be displayed safely."), false);
            return null;
        }
    }

    static String speciesPath(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) return "";
        String normalized = speciesId.strip();
        int separator = normalized.indexOf(':');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    static String health(CanonicalPokemonDetail detail) {
        return detail.health() == null ? "unavailable" : detail.health().currentHp() + "/" + detail.health().maxHp();
    }

    static String stats(CanonicalCombatStats stats) {
        if (stats == null) return "Combat stats unavailable";
        return "ATK " + stats.atk() + " | DEF " + stats.def()
                + " | SPATK " + stats.spatk() + " | SPDEF " + stats.spdef() + " | SPD " + stats.spd();
    }

    static String movement(CanonicalBaseMovement movement) {
        if (movement == null) return "Base movement unavailable";
        return "Movement OVR " + movement.overland() + " | SWIM " + movement.swim()
                + " | SKY " + movement.sky() + " | LJ " + movement.longJump()
                + " | HJ " + movement.highJump();
    }

    static String accuracy(CanonicalAccuracyEvasion accuracy) {
        if (accuracy == null) return "Accuracy/evasion unavailable";
        return "Accuracy " + accuracy.accuracyStage()
                + " | PEV " + accuracy.physicalEvasionBonus()
                + " | SEV " + accuracy.specialEvasionBonus()
                + " | STEV " + accuracy.statusEvasionBonus();
    }

    static String traits(CanonicalBattleTraits traits) {
        if (traits == null) return "Types unavailable | Abilities unavailable";
        return "Types " + listOr(traits.types(), "unavailable")
                + " | Abilities " + listOr(traits.abilities(), "none");
    }

    static String injuries(CanonicalPokemonDetail detail) {
        return detail.injuryState() == null ? "unavailable" : Integer.toString(detail.injuryState().injuries());
    }

    static String conditionLabel(CanonicalPokemonDetail detail) {
        String statuses = detail.statuses().isEmpty() ? "none" : String.join(", ", detail.statuses());
        return "PTU | " + displayName(detail.speciesId()) + " Lv " + detail.level()
                + " | HP " + health(detail) + " | " + stats(detail.combatStats())
                + " | Status " + statuses + " | Injuries " + injuries(detail);
    }

    private static String listOr(java.util.List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : String.join(", ", values);
    }

    static String displayName(String speciesId) {
        String path = speciesPath(speciesId);
        if (path.isEmpty()) return "Unknown";
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
    }
}

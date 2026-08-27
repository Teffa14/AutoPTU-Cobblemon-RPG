package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetail;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetailService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

/** Minecraft-facing detailed read-only view of one durable canonical party Pokemon. */
public final class FabricPokemonDetailRuntime {
    private FabricPokemonDetailRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("pokemon")
                                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1))
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

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPokemonDetailService service = new CanonicalPokemonDetailService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );

        CanonicalPokemonDetail detail;
        try {
            detail = service.findPokemon(playerId, slot).orElse(null);
        } catch (IllegalStateException invalidCanonicalState) {
            source.sendError(Text.literal("AutoPTU Pokemon state is inconsistent and cannot be displayed safely."));
            return 0;
        }
        if (detail == null) {
            source.sendError(Text.literal("No canonical Pokemon exists in party slot " + slot + "."));
            return 0;
        }

        player.sendMessage(Text.literal(
                "AutoPTU Pokemon [" + detail.slot() + "] " + displayName(detail.speciesId())
                        + " Lv." + detail.level() + " — save revision " + detail.revision()), false);
        player.sendMessage(Text.literal(
                "HP: " + health(detail) + " | status: " + listOr(detail.statuses(), "clear")
                        + " | injuries: " + (detail.injuryState() == null ? "unavailable" : detail.injuryState().injuries())), false);
        player.sendMessage(Text.literal(traits(detail.battleTraits()) + " | moves: "
                + (detail.moveLoadout() == null ? "unavailable" : listOr(detail.moveLoadout().moveIds(), "none"))), false);
        player.sendMessage(Text.literal(stats(detail.combatStats())), false);
        player.sendMessage(Text.literal(movement(detail.baseMovement())), false);
        player.sendMessage(Text.literal(accuracy(detail.accuracyEvasion())), false);
        player.sendMessage(Text.literal(
                "Capabilities: " + listOr(detail.capabilities(), "none")
                        + " | held item: " + (detail.heldItemEquipped() ? "equipped" : "none")), false);
        return 1;
    }

    static String health(CanonicalPokemonDetail detail) {
        return detail.health() == null ? "unavailable" : detail.health().currentHp() + "/" + detail.health().maxHp();
    }

    static String stats(CanonicalCombatStats stats) {
        if (stats == null) return "Combat stats: unavailable";
        return "Combat stats: ATK " + stats.atk() + " | DEF " + stats.def()
                + " | SPATK " + stats.spatk() + " | SPDEF " + stats.spdef() + " | SPD " + stats.spd();
    }

    static String movement(CanonicalBaseMovement movement) {
        if (movement == null) return "Base movement: unavailable";
        return "Base movement: overland " + movement.overland() + " | swim " + movement.swim()
                + " | sky " + movement.sky() + " | long jump " + movement.longJump()
                + " | high jump " + movement.highJump();
    }

    static String accuracy(CanonicalAccuracyEvasion accuracy) {
        if (accuracy == null) return "Accuracy/evasion: unavailable";
        return "Accuracy/evasion: stage " + accuracy.accuracyStage()
                + " | physical " + accuracy.physicalEvasionBonus()
                + " | special " + accuracy.specialEvasionBonus()
                + " | status " + accuracy.statusEvasionBonus();
    }

    private static String traits(CanonicalBattleTraits traits) {
        if (traits == null) return "Types: unavailable | abilities: unavailable";
        return "Types: " + listOr(traits.types(), "unavailable")
                + " | abilities: " + listOr(traits.abilities(), "none");
    }

    private static String listOr(java.util.List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : String.join(", ", values);
    }

    private static String displayName(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) return "Unknown";
        String path = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        if (path.isEmpty()) return speciesId;
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
    }
}

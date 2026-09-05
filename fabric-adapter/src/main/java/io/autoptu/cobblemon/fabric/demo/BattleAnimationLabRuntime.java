package io.autoptu.cobblemon.fabric.demo;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.fabric.presentation.CobblemonMoveAnimationRouting;
import io.autoptu.cobblemon.fabric.presentation.CobblemonPresentationEntityBackend;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Operator-only visual animation laboratory.
 *
 * This runtime deliberately bypasses battle resolution because it is a QA surface, not gameplay.
 * It may choose species, move id, hit/miss, damage-for-q.hurt and crit presentation inputs solely
 * to preview already-loaded rendering assets. It never persists Pokemon, changes Trainer state,
 * grants rewards, mutates AutoPTU battle state or claims the preview is a mechanically valid move.
 */
public final class BattleAnimationLabRuntime {
    private static final CobblemonPresentationEntityBackend PRESENTATION =
            new CobblemonPresentationEntityBackend();
    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();
    private static final int DEFAULT_TEST_DAMAGE = 10;
    private static final int PAGE_SIZE = 20;

    private BattleAnimationLabRuntime() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("animation")
                                        .executes(context -> help(context.getSource()))
                                        .then(CommandManager.literal("start")
                                                .then(CommandManager.argument("attacker", StringArgumentType.word())
                                                        .then(CommandManager.argument("target", StringArgumentType.word())
                                                                .executes(context -> start(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "attacker"),
                                                                        StringArgumentType.getString(context, "target"))))))
                                        .then(CommandManager.literal("play")
                                                .then(CommandManager.argument("move", StringArgumentType.word())
                                                        .executes(context -> play(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "move"),
                                                                true,
                                                                false,
                                                                DEFAULT_TEST_DAMAGE))
                                                        .then(CommandManager.argument("damage", IntegerArgumentType.integer(0))
                                                                .executes(context -> play(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "move"),
                                                                        true,
                                                                        false,
                                                                        IntegerArgumentType.getInteger(context, "damage"))))))
                                        .then(CommandManager.literal("miss")
                                                .then(CommandManager.argument("move", StringArgumentType.word())
                                                        .executes(context -> play(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "move"),
                                                                false,
                                                                false,
                                                                0))))
                                        .then(CommandManager.literal("crit")
                                                .then(CommandManager.argument("move", StringArgumentType.word())
                                                        .executes(context -> play(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "move"),
                                                                true,
                                                                true,
                                                                DEFAULT_TEST_DAMAGE))
                                                        .then(CommandManager.argument("damage", IntegerArgumentType.integer(0))
                                                                .executes(context -> play(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "move"),
                                                                        true,
                                                                        true,
                                                                        IntegerArgumentType.getInteger(context, "damage"))))))
                                        .then(CommandManager.literal("repeat")
                                                .executes(context -> repeat(context.getSource())))
                                        .then(CommandManager.literal("next")
                                                .executes(context -> cycle(context.getSource(), 1)))
                                        .then(CommandManager.literal("previous")
                                                .executes(context -> cycle(context.getSource(), -1)))
                                        .then(CommandManager.literal("swap")
                                                .executes(context -> swap(context.getSource())))
                                        .then(CommandManager.literal("reset")
                                                .executes(context -> resetPositions(context.getSource())))
                                        .then(CommandManager.literal("inspect")
                                                .then(CommandManager.argument("move", StringArgumentType.word())
                                                        .executes(context -> inspect(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "move")))))
                                        .then(CommandManager.literal("list")
                                                .executes(context -> listEffects(context.getSource(), 1))
                                                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(context -> listEffects(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "page")))))
                                        .then(CommandManager.literal("status")
                                                .executes(context -> status(context.getSource())))
                                        .then(CommandManager.literal("stop")
                                                .executes(context -> stop(context.getSource())))))));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cleanup(handler.player.getUuid()));
    }

    private static int help(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("AutoPTU animation lab (visual-only QA):"), false);
        source.sendFeedback(() -> Text.literal("/autoptu admin animation start <attacker> <target>"), false);
        source.sendFeedback(() -> Text.literal("/autoptu admin animation play <move> [damage] | miss <move> | crit <move> [damage]"), false);
        source.sendFeedback(() -> Text.literal("/autoptu admin animation repeat | next | previous | swap | reset"), false);
        source.sendFeedback(() -> Text.literal("/autoptu admin animation inspect <move> | list [page] | status | stop"), false);
        return 1;
    }

    private static int start(ServerCommandSource source, String attackerId, String targetId) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;

        String normalizedAttacker = speciesId(attackerId);
        String normalizedTarget = speciesId(targetId);
        Species attackerSpecies = PokemonSpecies.INSTANCE.getByName(normalizedAttacker);
        Species targetSpecies = PokemonSpecies.INSTANCE.getByName(normalizedTarget);
        if (attackerSpecies == null || targetSpecies == null) {
            source.sendError(Text.literal("Unknown Cobblemon species. Example: charizard blastoise"));
            return 0;
        }

        cleanup(player.getUuid());
        ServerWorld world = player.getServerWorld();
        BlockPos attackerOrigin = player.getBlockPos().add(2, 0, 0).toImmutable();
        BlockPos targetOrigin = player.getBlockPos().add(6, 0, 0).toImmutable();
        PokemonEntity attacker = spawn(world, attackerSpecies, attackerOrigin);
        PokemonEntity target = spawn(world, targetSpecies, targetOrigin);
        Session session = new Session(
                attacker,
                target,
                displayName(normalizedAttacker),
                displayName(normalizedTarget),
                attackerOrigin,
                targetOrigin
        );
        ACTIVE.put(player.getUuid(), session);
        player.sendMessage(Text.literal(
                "ANIMATION LAB: " + session.attackerName + " -> " + session.targetName
                        + ". Use /autoptu admin animation play <move>."), false);
        return 1;
    }

    private static int play(
            ServerCommandSource source,
            String moveId,
            boolean hit,
            boolean crit,
            int damage
    ) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        Session session = ACTIVE.get(player.getUuid());
        if (session == null || session.invalid()) {
            source.sendError(Text.literal("Start an animation lab pair first."));
            return 0;
        }
        if (!hit && (crit || damage != 0)) {
            source.sendError(Text.literal("Animation-lab miss must use damage 0 and cannot be critical."));
            return 0;
        }

        String normalized = normalizeMove(moveId);
        CobblemonMoveAnimationRouting.Route route = CobblemonMoveAnimationRouting.resolve(normalized);
        PRESENTATION.animateMove(
                session.attacker,
                session.target,
                new BattlePresentationCommand(
                        0,
                        0,
                        BattlePresentationCommand.Kind.MOVE_ANIMATION,
                        "animation-lab-attacker",
                        Map.of(
                                "targetId", "animation-lab-target",
                                "moveId", normalized,
                                "hit", Boolean.toString(hit),
                                "crit", Boolean.toString(crit),
                                "damage", Integer.toString(damage)
                        )
                )
        );

        session.lastMove = normalized;
        session.lastHit = hit;
        session.lastCrit = crit;
        session.lastDamage = damage;
        session.nativeIndex = indexOfLoadedEffect(route.effectPath());
        player.sendMessage(Text.literal(routeText(route, normalized, hit, crit, damage)), false);
        return 1;
    }

    private static int repeat(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        Session session = ACTIVE.get(player.getUuid());
        if (session == null || session.lastMove == null) {
            source.sendError(Text.literal("No animation has been played yet."));
            return 0;
        }
        return play(source, session.lastMove, session.lastHit, session.lastCrit, session.lastDamage);
    }

    private static int cycle(ServerCommandSource source, int delta) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        Session session = ACTIVE.get(player.getUuid());
        if (session == null || session.invalid()) {
            source.sendError(Text.literal("Start an animation lab pair first."));
            return 0;
        }

        List<String> effects = CobblemonMoveAnimationRouting.loadedNativeMoveEffects();
        if (effects.isEmpty()) {
            source.sendError(Text.literal("Cobblemon has no loaded move ActionEffects."));
            return 0;
        }
        int current = session.nativeIndex;
        if (current < 0 && session.lastMove != null) current = effects.indexOf(session.lastMove);
        int next = Math.floorMod(current + delta, effects.size());
        session.nativeIndex = next;
        return play(source, effects.get(next), true, false, DEFAULT_TEST_DAMAGE);
    }

    private static int swap(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        Session session = ACTIVE.get(player.getUuid());
        if (session == null || session.invalid()) {
            source.sendError(Text.literal("Start an animation lab pair first."));
            return 0;
        }

        PokemonEntity oldAttacker = session.attacker;
        session.attacker = session.target;
        session.target = oldAttacker;
        String oldName = session.attackerName;
        session.attackerName = session.targetName;
        session.targetName = oldName;
        BlockPos oldOrigin = session.attackerOrigin;
        session.attackerOrigin = session.targetOrigin;
        session.targetOrigin = oldOrigin;
        player.sendMessage(Text.literal(
                "ANIMATION LAB swapped: " + session.attackerName + " -> " + session.targetName), false);
        return 1;
    }

    private static int resetPositions(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        Session session = ACTIVE.get(player.getUuid());
        if (session == null || session.invalid()) {
            source.sendError(Text.literal("Start an animation lab pair first."));
            return 0;
        }
        teleport(session.attacker, session.attackerOrigin);
        teleport(session.target, session.targetOrigin);
        player.sendMessage(Text.literal("ANIMATION LAB actors reset to their test anchors."), false);
        return 1;
    }

    private static int inspect(ServerCommandSource source, String moveId) {
        CobblemonMoveAnimationRouting.Route route = CobblemonMoveAnimationRouting.resolve(moveId);
        source.sendFeedback(() -> Text.literal(routeText(route, normalizeMove(moveId), true, false, 0)), false);
        return 1;
    }

    private static int listEffects(ServerCommandSource source, int page) {
        List<String> effects = CobblemonMoveAnimationRouting.loadedNativeMoveEffects();
        if (effects.isEmpty()) {
            source.sendError(Text.literal("Cobblemon has no loaded move ActionEffects."));
            return 0;
        }
        int pages = Math.max(1, (effects.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.min(page, pages);
        int from = (safePage - 1) * PAGE_SIZE;
        int to = Math.min(effects.size(), from + PAGE_SIZE);
        source.sendFeedback(() -> Text.literal(
                "Cobblemon move ActionEffects page " + safePage + "/" + pages + " (" + effects.size() + " loaded):"), false);
        for (String effect : effects.subList(from, to)) {
            source.sendFeedback(() -> Text.literal("- " + effect), false);
        }
        return 1;
    }

    private static int status(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        Session session = ACTIVE.get(player.getUuid());
        if (session == null || session.invalid()) {
            source.sendFeedback(() -> Text.literal("ANIMATION LAB: no active pair."), false);
            return 1;
        }
        source.sendFeedback(() -> Text.literal(
                "ANIMATION LAB: " + session.attackerName + " -> " + session.targetName
                        + " | last=" + (session.lastMove == null ? "none" : session.lastMove)), false);
        return 1;
    }

    private static int stop(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        boolean removed = cleanup(player.getUuid());
        player.sendMessage(Text.literal(removed ? "ANIMATION LAB stopped." : "ANIMATION LAB was not active."), false);
        return 1;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) source.sendError(Text.literal("Animation lab requires an in-game operator player."));
        return player;
    }

    private static PokemonEntity spawn(ServerWorld world, Species species, BlockPos position) {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.setAiDisabled(true);
        entity.refreshPositionAndAngles(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        if (!world.spawnEntity(entity)) {
            throw new IllegalStateException("failed to spawn animation-lab PokemonEntity");
        }
        return entity;
    }

    private static void teleport(PokemonEntity entity, BlockPos position) {
        entity.requestTeleport(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
    }

    private static boolean cleanup(UUID playerId) {
        Session session = ACTIVE.remove(playerId);
        if (session == null) return false;
        if (!session.attacker.isRemoved()) session.attacker.discard();
        if (!session.target.isRemoved()) session.target.discard();
        return true;
    }

    private static int indexOfLoadedEffect(String effectPath) {
        if (effectPath == null || effectPath.isBlank()) return -1;
        return CobblemonMoveAnimationRouting.loadedNativeMoveEffects().indexOf(effectPath);
    }

    private static String routeText(
            CobblemonMoveAnimationRouting.Route route,
            String move,
            boolean hit,
            boolean crit,
            int damage
    ) {
        String asset = route.nativeEffect() ? route.effectPath() : "project-generic";
        String substitute = route.substituted() ? " variant=" + route.variant() : "";
        return "ANIMATION " + move + " -> " + route.source() + " [" + asset + "]" + substitute
                + " | hit=" + hit + " crit=" + crit + " damage=" + damage;
    }

    private static String normalizeMove(String moveId) {
        if (moveId == null || moveId.isBlank()) throw new IllegalArgumentException("moveId is required");
        return moveId.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String speciesId(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("species is required");
        String normalized = raw.strip().toLowerCase(Locale.ROOT);
        int namespace = normalized.indexOf(':');
        return namespace >= 0 ? normalized.substring(namespace + 1) : normalized;
    }

    private static String displayName(String speciesId) {
        if (speciesId.isBlank()) return speciesId;
        return Character.toUpperCase(speciesId.charAt(0)) + speciesId.substring(1);
    }

    private static final class Session {
        private PokemonEntity attacker;
        private PokemonEntity target;
        private String attackerName;
        private String targetName;
        private BlockPos attackerOrigin;
        private BlockPos targetOrigin;
        private String lastMove;
        private boolean lastHit = true;
        private boolean lastCrit;
        private int lastDamage = DEFAULT_TEST_DAMAGE;
        private int nativeIndex = -1;

        private Session(
                PokemonEntity attacker,
                PokemonEntity target,
                String attackerName,
                String targetName,
                BlockPos attackerOrigin,
                BlockPos targetOrigin
        ) {
            this.attacker = attacker;
            this.target = target;
            this.attackerName = attackerName;
            this.targetName = targetName;
            this.attackerOrigin = attackerOrigin;
            this.targetOrigin = targetOrigin;
        }

        private boolean invalid() {
            return attacker.isRemoved() || target.isRemoved();
        }
    }
}

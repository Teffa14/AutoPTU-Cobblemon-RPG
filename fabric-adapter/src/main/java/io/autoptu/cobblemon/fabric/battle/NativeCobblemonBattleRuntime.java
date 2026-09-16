package io.autoptu.cobblemon.fabric.battle;

import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.cobblemon.mod.common.battles.SuccessfulBattleStart;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** A thin entry into Cobblemon's real PvE builder, not a parallel battle simulator. */
public final class NativeCobblemonBattleRuntime {
    private static final double REACH = 8.0;
    private NativeCobblemonBattleRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(commands()));
    }

    static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> commands() {
        return CommandManager.literal("autoptu")
                    .then(CommandManager.literal("battle")
                            .executes(context -> help(context.getSource()))
                            .then(CommandManager.literal("help").executes(context -> help(context.getSource())))
                            .then(CommandManager.literal("wild").executes(context -> wild(context.getSource())))
                            .then(CommandManager.literal("status").executes(context -> status(context.getSource())))
                            .then(CommandManager.literal("practice").executes(context -> legacy(context.getSource())))
                            .then(CommandManager.literal("menu").executes(context -> legacy(context.getSource()))))
                    .then(CommandManager.literal("starter").executes(context -> help(context.getSource())));
    }

    private static int help(ServerCommandSource source) {
        source.sendFeedback(() -> Text.translatable("autoptu.native.help.title").formatted(Formatting.AQUA), false);
        for (String key : new String[]{"starter", "controls", "wild", "duel", "authority", "legacy"}) {
            source.sendFeedback(() -> Text.translatable("autoptu.native.help." + key), false);
        }
        source.sendFeedback(() -> Text.literal("/autoptu battle wild").styled(style -> style.withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/autoptu battle wild"))), false);
        return 1;
    }

    private static int legacy(ServerCommandSource source) {
        source.sendFeedback(() -> Text.translatable("autoptu.native.no_demo"), false);
        return help(source);
    }

    private static int status(ServerCommandSource source) {
        var player = source.getPlayer();
        if (player == null) return 0;
        var battle = BattleRegistry.getBattleByParticipatingPlayer(player);
        source.sendFeedback(() -> Text.translatable(battle == null ? "autoptu.native.idle" : "autoptu.native.active"), false);
        return battle == null ? 0 : 1;
    }

    private static int wild(ServerCommandSource source) {
        var player = source.getPlayer();
        if (player == null || !player.isAlive() || player.isSpectator()) return 0;
        if (BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
            source.sendError(Text.translatable("autoptu.native.already_battling"));
            return 0;
        }
        var target = targetedPokemon(player);
        if (target == null) {
            source.sendError(Text.translatable("autoptu.native.no_target"));
            return 0;
        }
        // Select the first visible Pokémon before checking ownership: never target through a pet.
        if (!target.getPokemon().isWild()) {
            source.sendError(Text.translatable("autoptu.native.not_wild"));
            return 0;
        }
        if (!target.isAlive() || target.isRemoved() || target.isBusy() || target.getBattleId() != null
                || target.getPokemon().getCurrentHealth() <= 0) {
            source.sendError(Text.translatable("autoptu.native.busy"));
            return 0;
        }
        // The native builder obtains the actual player party, validates it and owns all battle state.
        // Do not copy HP/moves, create replacement entities, force an action or teleport either side.
        var result = BattleBuilder.INSTANCE.pve(player, target);
        if (result instanceof ErroredBattleStart error) {
            error.sendTo(player, text -> text);
            return 0;
        }
        return result instanceof SuccessfulBattleStart ? 1 : 0;
    }

    private static PokemonEntity targetedPokemon(ServerPlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(player.getRotationVec(1F).multiply(REACH));
        var blockHit = player.getServerWorld().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        double closest = blockHit.getType() == HitResult.Type.MISS ? REACH * REACH : start.squaredDistanceTo(blockHit.getPos());
        PokemonEntity selected = null;
        for (var candidate : player.getServerWorld().getEntitiesByClass(PokemonEntity.class,
                player.getBoundingBox().stretch(player.getRotationVec(1F).multiply(REACH)).expand(1),
                entity -> !entity.isRemoved() && !entity.isInvisible())) {
            var intersection = candidate.getBoundingBox().expand(0.15).raycast(start, end);
            if (intersection.isEmpty()) continue;
            double distance = start.squaredDistanceTo(intersection.get());
            if (distance < closest && player.canSee(candidate)) {
                closest = distance;
                selected = candidate;
            }
        }
        return selected;
    }
}

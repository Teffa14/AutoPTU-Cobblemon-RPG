package io.autoptu.cobblemon.fabric.battle;

import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.cobblemon.mod.common.battles.SuccessfulBattleStart;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.battlecore.NativeDuelInvitations;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.UUID;

/** Explicit two-player consent, followed by Cobblemon's unmodified PvP builder. */
public final class NativeCobblemonDuelRuntime {
    private static final NativeDuelInvitations INVITATIONS = new NativeDuelInvitations();
    private NativeCobblemonDuelRuntime() {}
    private static long now() { return System.nanoTime() / 1_000_000; }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(commands()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            INVITATIONS.cancel(handler.player.getUuid()).ifPresent(invitation -> {
                UUID other = invitation.challenger().equals(handler.player.getUuid()) ? invitation.recipient() : invitation.challenger();
                var peer = server.getPlayerManager().getPlayer(other);
                if (peer != null) peer.sendMessage(Text.translatable("autoptu.duel.disconnected"), false);
            });
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) INVITATIONS.expire(now());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> INVITATIONS.clear());
    }

    static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> commands() {
        return CommandManager.literal("autoptu").then(CommandManager.literal("duel")
                .executes(context -> status(context.getSource()))
                .then(CommandManager.literal("challenge").then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> challenge(context.getSource(), EntityArgumentType.getPlayer(context, "player")))))
                .then(CommandManager.literal("accept").then(CommandManager.argument("token", StringArgumentType.word())
                        .executes(context -> accept(context.getSource(), StringArgumentType.getString(context, "token")))))
                .then(CommandManager.literal("cancel").executes(context -> cancel(context.getSource()))));
    }

    private static boolean available(ServerPlayerEntity player) {
        return player != null && player.isAlive() && !player.isSpectator()
                && BattleRegistry.getBattleByParticipatingPlayer(player) == null;
    }

    private static boolean together(ServerPlayerEntity first, ServerPlayerEntity second) {
        return first.getServerWorld() == second.getServerWorld() && first.squaredDistanceTo(second) <= 256;
    }

    private static int challenge(ServerCommandSource source, ServerPlayerEntity recipient) {
        var player = source.getPlayer();
        if (!available(player) || !available(recipient) || player == recipient || !together(player, recipient)) {
            source.sendError(Text.translatable("autoptu.duel.unavailable"));
            return 0;
        }
        final NativeDuelInvitations.Invitation invitation;
        try { invitation = INVITATIONS.invite(player.getUuid(), recipient.getUuid(), now()); }
        catch (IllegalStateException error) {
            source.sendError(Text.translatable("autoptu.duel.pending"));
            return 0;
        }
        player.sendMessage(Text.translatable("autoptu.duel.sent", recipient.getName()), false);
        recipient.sendMessage(Text.translatable("autoptu.duel.received", player.getName()), false);
        recipient.sendMessage(Text.translatable("autoptu.duel.accept").styled(style -> style.withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/autoptu duel accept " + invitation.token()))), false);
        return 1;
    }

    private static int accept(ServerCommandSource source, String value) {
        var recipient = source.getPlayer();
        if (recipient == null) return 0;
        UUID token;
        try { token = UUID.fromString(value); }
        catch (IllegalArgumentException error) { source.sendError(Text.translatable("autoptu.duel.expired")); return 0; }
        var invitation = INVITATIONS.accept(recipient.getUuid(), token, now()).orElse(null);
        if (invitation == null) { source.sendError(Text.translatable("autoptu.duel.expired")); return 0; }
        var challenger = source.getServer().getPlayerManager().getPlayer(invitation.challenger());
        if (!available(challenger) || !available(recipient) || !together(challenger, recipient)) {
            source.sendError(Text.translatable("autoptu.duel.unavailable"));
            if (challenger != null) challenger.sendMessage(Text.translatable("autoptu.duel.unavailable"), false);
            return 0;
        }
        var result = BattleBuilder.INSTANCE.pvp1v1(challenger, recipient);
        if (result instanceof ErroredBattleStart error) {
            error.sendTo(challenger, text -> text);
            error.sendTo(recipient, text -> text);
        }
        return result instanceof SuccessfulBattleStart ? 1 : 0;
    }

    private static int cancel(ServerCommandSource source) {
        var player = source.getPlayer();
        if (player == null) return 0;
        var invitation = INVITATIONS.cancel(player.getUuid()).orElse(null);
        if (invitation == null) return status(source);
        player.sendMessage(Text.translatable("autoptu.duel.cancelled"), false);
        UUID peerId = invitation.challenger().equals(player.getUuid()) ? invitation.recipient() : invitation.challenger();
        var peer = source.getServer().getPlayerManager().getPlayer(peerId);
        if (peer != null) peer.sendMessage(Text.translatable("autoptu.duel.cancelled"), false);
        return 1;
    }

    private static int status(ServerCommandSource source) {
        var player = source.getPlayer();
        if (player == null) return 0;
        var invitation = INVITATIONS.find(player.getUuid(), now()).orElse(null);
        if (invitation == null) {
            source.sendFeedback(() -> Text.translatable("autoptu.duel.help"), false);
            return 0;
        }
        source.sendFeedback(() -> Text.translatable("autoptu.duel.remaining", Math.max(0, (invitation.expiresAt() - now() + 999) / 1000)), false);
        if (invitation.recipient().equals(player.getUuid())) {
            source.sendFeedback(() -> Text.literal("/autoptu duel accept " + invitation.token()), false);
        }
        return 1;
    }
}

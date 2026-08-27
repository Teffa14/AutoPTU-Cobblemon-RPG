package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPartyLeadService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Minecraft fallback surface for selecting the durable server-owned party lead. */
public final class FabricPartyLeadRuntime {
    private FabricPartyLeadRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("party")
                                .then(CommandManager.literal("lead")
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(1))
                                                .executes(context -> setLead(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "slot")
                                                )))))));
    }

    private static int setLead(ServerCommandSource source, int slot) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Party lead selection must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPartyLeadService service = new CanonicalPartyLeadService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer())
        );
        CanonicalPartyLeadService.Decision decision = service.setLead(playerId, slot);

        return switch (decision.outcome()) {
            case APPLIED -> {
                player.sendMessage(Text.literal("AutoPTU party lead changed to slot 1."), false);
                yield 1;
            }
            case ALREADY_LEAD -> {
                player.sendMessage(Text.literal("That Pokemon is already your party lead."), false);
                yield 1;
            }
            case INVALID_SLOT, NO_PARTY, CONCURRENT_WRITE -> {
                source.sendError(Text.literal("AutoPTU could not change party lead: " + decision.reason()));
                yield 0;
            }
        };
    }
}

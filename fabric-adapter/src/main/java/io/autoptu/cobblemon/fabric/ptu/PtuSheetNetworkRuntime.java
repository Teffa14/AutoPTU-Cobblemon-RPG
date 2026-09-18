package io.autoptu.cobblemon.fabric.ptu;

import com.cobblemon.mod.common.Cobblemon;
import com.google.gson.Gson;
import io.autoptu.cobblemon.fabric.network.PtuSheetPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PtuSheetNetworkRuntime {
    private static final Gson JSON = new Gson();
    private static final PtuSheetRequestGate GATE = new PtuSheetRequestGate();
    private PtuSheetNetworkRuntime() {}
    public static void register() {
        PtuSheetPayloads.register();
        ServerPlayNetworking.registerGlobalReceiver(PtuSheetPayloads.Request.ID, (request, context) -> context.server().execute(() -> {
            var player = context.player();
            if (!ServerPlayNetworking.canSend(player, PtuSheetPayloads.Response.ID)) return;
            if (!GATE.allow(player.getUuid(), System.nanoTime() / 1_000_000L)) {
                return;
            }
            // Never trust the client's selected object, owner ID, stats or a previously authorized UUID.
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            var pokemon = party.get(request.pokemonId());
            if (pokemon == null) pokemon = Cobblemon.INSTANCE.getStorage().getPC(player).get(request.pokemonId());
            if (pokemon == null) { reply(player, request, "unavailable", "", ""); return; }
            String json = JSON.toJson(PtuPokemonDataRuntime.bind(pokemon));
            if (json.length() > PtuSheetPayloads.MAX_JSON) { reply(player, request, "too_large", "", ""); return; }
            reply(player, request, "ready", PtuNativeLifecycleRuntime.origin(pokemon), json);
        }));
        ServerPlayNetworking.registerGlobalReceiver(PtuSheetPayloads.StarterRequest.ID, (request, context) -> context.server().execute(() -> {
            var player = context.player();
            if (!ServerPlayNetworking.canSend(player, PtuSheetPayloads.StarterResponse.ID)
                    || !GATE.allow(player.getUuid(), System.nanoTime() / 1_000_000L)) return;
            String status = "unavailable", json = "";
            var category = Cobblemon.INSTANCE.getStarterHandler().getStarterList(player).stream()
                    .filter(value -> value.getName().equals(request.category())).findFirst().orElse(null);
            if (category != null && category.getRandomStarter()) status = "random";
            else if (category != null && request.option() >= 0 && request.option() < category.getPokemon().size()) {
                try {
                var properties = category.getPokemon().get(request.option());
                if (properties.getSpecies() != null && !properties.getSpecies().isBlank()) {
                    // Renderable data only: create()/createEntity() would generate state and consume RNG.
                    var renderable = properties.asRenderablePokemon();
                    var species = renderable.getSpecies();
                    var form = properties.getForm() == null ? renderable.getForm() : species.getFormByName(properties.getForm());
                    var id = species.getResourceIdentifier();
                    var catalog = PtuPokemonDataRuntime.catalog();
                    boolean exactForm = form != null && (properties.getForm() == null || properties.getForm().equalsIgnoreCase(form.getName()));
                    var data = exactForm ? catalog.resolveSpecies(id.getNamespace(), id.getPath(), form.getName(), form == species.getStandardForm()).orElse(null) : null;
                    if (data == null) status = "missing";
                    else {
                        json = JSON.toJson(new PtuStarterPreview(catalog.revision(), data, catalog.pools(data.id()), properties.getLevel()));
                        status = json.length() > PtuSheetPayloads.MAX_JSON ? "too_large" : "ready";
                        if (!status.equals("ready")) json = "";
                    }
                }
                } catch (RuntimeException invalidStarterData) {
                    status = "missing"; json = "";
                    org.slf4j.LoggerFactory.getLogger(PtuSheetNetworkRuntime.class).warn("Cannot preview configured starter category {} option {}", request.category(), request.option(), invalidStarterData);
                }
            }
            ServerPlayNetworking.send(player, new PtuSheetPayloads.StarterResponse(request.category(), request.option(), request.sequence(), status, json));
        }));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GATE.remove(handler.player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> GATE.clear());
    }
    private static void reply(ServerPlayerEntity player, PtuSheetPayloads.Request request, String status, String origin, String json) {
        ServerPlayNetworking.send(player, new PtuSheetPayloads.Response(request.pokemonId(), request.sequence(), status, origin, json));
    }
}

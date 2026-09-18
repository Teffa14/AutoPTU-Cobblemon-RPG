package io.autoptu.cobblemon.fabric.ptu;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.battle.NativeCobblemonBattleRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Attaches PTU data to real server-side Pokémon. Does not mutate Cobblemon combat values. */
public final class PtuPokemonDataRuntime {
    public static final String DATA_KEY = "autoptu:ptu_data_v1";
    private static final Gson JSON = new Gson();
    private static final Map<Pokemon, Cached> CACHE = new WeakHashMap<>();
    private record Cached(PtuPokemonBinding.Input input, PtuPokemonBinding binding) {}
    private static PtuDataCatalog catalog;
    private PtuPokemonDataRuntime() {}

    public static void register() {
        catalog = PtuDataCatalog.bundled();
        var logger = LoggerFactory.getLogger(PtuPokemonDataRuntime.class);
        logger.info("PTU data loaded: {} species, {} moves, {} abilities; revision {}; {} data diagnostics",
                catalog.speciesCount(), catalog.moveCount(), catalog.abilityCount(), catalog.revision(), catalog.diagnostics().size());
        for (var diagnostic : catalog.diagnostics()) logger.warn("PTU catalog: {}", diagnostic);
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> dispatcher.register(commands()));
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof PokemonEntity pokemon) bind(pokemon.getPokemon());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncParty(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 100 != 0) return;
            for (var player : server.getPlayerManager().getPlayerList()) syncParty(player);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CACHE.clear());
    }

    public static PtuDataCatalog catalog() {
        if (catalog == null) throw new IllegalStateException("PTU catalog is not initialized");
        return catalog;
    }

    public static PtuPokemonBinding bind(Pokemon pokemon) {
        var species = pokemon.getSpecies();
        var id = species.getResourceIdentifier();
        var input = new PtuPokemonBinding.Input(pokemon.getUuid(), id.getNamespace(), id.getPath(),
                pokemon.getForm().getName(), pokemon.getForm() == species.getStandardForm(), pokemon.getLevel(),
                pokemon.getMoveSet().getMoves().stream().map(move -> move.getName()).toList(), pokemon.getAbility().getName());
        var cached = CACHE.get(pokemon);
        if (cached != null && cached.input().equals(input)) return cached.binding();
        var binding = PtuPokemonBinding.resolve(catalog(), input);
        String encoded = JSON.toJson(binding);
        var data = pokemon.getPersistentData();
        if (!encoded.equals(data.getString(DATA_KEY))) {
            // Only our namespaced key is changed. UUID, native stats, HP, moves, PP and ability stay intact.
            data.putString(DATA_KEY, encoded);
            var coordinates = pokemon.getStoreCoordinates().get();
            if (coordinates != null) coordinates.getStore().onPokemonChanged(pokemon);
        }
        CACHE.put(pokemon, new Cached(input, binding));
        return binding;
    }

    public static void syncParty(ServerPlayerEntity player) {
        for (var pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) if (pokemon != null) bind(pokemon);
    }

    static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> commands() {
        return CommandManager.literal("autoptu").then(CommandManager.literal("ptu")
                .executes(context -> status(context.getSource()))
                .then(CommandManager.literal("status").executes(context -> status(context.getSource())))
                .then(CommandManager.literal("sync").executes(context -> {
                    var player = context.getSource().getPlayerOrThrow(); syncParty(player);
                    tell(context.getSource(), "autoptu.ptu.synced"); return 1;
                }).then(CommandManager.literal("pc").executes(context -> {
                    var player = context.getSource().getPlayerOrThrow();
                    int count = 0;
                    for (var pokemon : Cobblemon.INSTANCE.getStorage().getPC(player)) {
                        if (pokemon != null) { bind(pokemon); count++; }
                    }
                    tell(context.getSource(), "autoptu.ptu.pc_synced", count); return count;
                })))
                .then(CommandManager.literal("party").then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(context -> inspect(context.getSource(), IntegerArgumentType.getInteger(context, "slot")))))
                .then(CommandManager.literal("target").executes(context -> {
                    var target = NativeCobblemonBattleRuntime.targetedPokemon(context.getSource().getPlayerOrThrow());
                    if (target == null) { context.getSource().sendError(Text.translatable("autoptu.native.no_target")); return 0; }
                    display(context.getSource(), bind(target.getPokemon())); return 1;
                }))
                .then(CommandManager.literal("move").then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .suggests((context, builder) -> net.minecraft.command.CommandSource.suggestMatching(catalog().moveIds(), builder))
                        .executes(context -> showMove(context.getSource(), StringArgumentType.getString(context, "name")))))
                .then(CommandManager.literal("ability").then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .suggests((context, builder) -> net.minecraft.command.CommandSource.suggestMatching(catalog().abilityIds(), builder))
                        .executes(context -> showAbility(context.getSource(), StringArgumentType.getString(context, "name")))))
                .then(CommandManager.literal("learnset").then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(context -> learnset(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1, 1000))
                                .executes(context -> learnset(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), IntegerArgumentType.getInteger(context, "page"))))))
                .then(CommandManager.literal("audit").requires(source -> source.hasPermissionLevel(2)).executes(context -> {
                    for (String issue : catalog().diagnostics()) context.getSource().sendFeedback(() -> Text.literal(issue), false);
                    return catalog().diagnostics().size();
                })));
    }

    private static int status(ServerCommandSource source) {
        tell(source, "autoptu.ptu.catalog", catalog().speciesCount(), catalog().moveCount(), catalog().abilityCount(), catalog().revision().substring(0, 12));
        tell(source, "autoptu.ptu.boundary");
        tell(source, "autoptu.ptu.commands");
        return 1;
    }
    private static Pokemon partyPokemon(ServerCommandSource source, int slot) {
        var player = source.getPlayer();
        if (player == null) return null;
        var pokemon = Cobblemon.INSTANCE.getStorage().getParty(player).get(slot - 1);
        if (pokemon == null) source.sendError(Text.translatable("autoptu.ptu.empty_slot", slot));
        return pokemon;
    }
    private static int inspect(ServerCommandSource source, int slot) {
        var pokemon = partyPokemon(source, slot);
        if (pokemon == null) return 0;
        display(source, bind(pokemon)); return 1;
    }
    private static void display(ServerCommandSource source, PtuPokemonBinding binding) {
        tell(source, "autoptu.ptu.pokemon", binding.nativeSpecies(), binding.nativeForm(), binding.level());
        if (binding.species() != null) {
            var species = binding.species();
            tell(source, "autoptu.ptu.base_stats", species.name(), species.baseStats().toString());
            tell(source, "autoptu.ptu.types", String.join(", ", species.types()));
            tell(source, "autoptu.ptu.capabilities", String.join(", ", species.capabilities()));
            tell(source, "autoptu.ptu.pools", String.join(", ", binding.abilityPools().basic()),
                    String.join(", ", binding.abilityPools().advanced()), String.join(", ", binding.abilityPools().high()));
        }
        for (var move : binding.equippedMoves()) {
            if (move.data() != null) tell(source, "autoptu.ptu.bound_move", move.data().name(), move.data().damageBase(),
                    move.data().baseDice(), move.data().frequency(), Text.translatable("autoptu.ptu.learning." + move.learning().name().toLowerCase(Locale.ROOT)));
        }
        for (var issue : binding.issues()) source.sendFeedback(() -> Text.literal(issue).formatted(Formatting.YELLOW), false);
        tell(source, "autoptu.ptu.boundary");
    }
    private static int showMove(ServerCommandSource source, String name) {
        var move = catalog().move(name).orElse(null);
        if (move == null) { source.sendError(Text.translatable("autoptu.ptu.missing", name)); return 0; }
        tell(source, "autoptu.ptu.move", move.name(), move.type(), move.category(), move.ac() == null ? "—" : move.ac(),
                move.damageBase(), move.baseDice(), move.frequency(), move.range());
        source.sendFeedback(() -> Text.literal(move.effects()), false);
        tell(source, "autoptu.ptu.source", move.source()); return 1;
    }
    private static int showAbility(ServerCommandSource source, String name) {
        var ability = catalog().ability(name).orElse(null);
        if (ability == null) { source.sendError(Text.translatable("autoptu.ptu.missing", name)); return 0; }
        tell(source, "autoptu.ptu.ability", ability.name(), ability.frequency(), ability.trigger());
        source.sendFeedback(() -> Text.literal(ability.effect()), false);
        tell(source, "autoptu.ptu.source", ability.source());
        tell(source, "autoptu.ptu.ability_boundary"); return 1;
    }
    private static int learnset(ServerCommandSource source, int slot, int page) {
        var pokemon = partyPokemon(source, slot);
        if (pokemon == null) return 0;
        var binding = bind(pokemon);
        if (binding.species() == null) { source.sendError(Text.translatable("autoptu.ptu.missing", binding.nativeSpecies())); return 0; }
        var entries = catalog().learnset(binding.species().id());
        int pages = Math.max(1, (entries.size() + 9) / 10);
        if (page > pages) { source.sendError(Text.translatable("autoptu.ptu.page_missing", pages)); return 0; }
        tell(source, "autoptu.ptu.learnset", binding.species().name(), page, pages);
        for (var entry : entries.subList((page - 1) * 10, Math.min(page * 10, entries.size()))) {
            tell(source, "autoptu.ptu.learn", entry.move(), entry.level() > 0 ? entry.level() : "TM / Tutor / Egg / ?",
                    catalog().move(entry.move()).isPresent() ? "✓" : "?");
        }
        return 1;
    }
    private static void tell(ServerCommandSource source, String key, Object... arguments) {
        source.sendFeedback(() -> Text.translatable(key, arguments), false);
    }
}

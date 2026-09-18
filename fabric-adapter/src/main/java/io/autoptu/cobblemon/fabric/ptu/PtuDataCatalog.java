package io.autoptu.cobblemon.fabric.ptu;

import com.google.gson.*;
import io.autoptu.core.rules.PtuTables;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;

/** Bundled, server-owned PTU records. Descriptions are data, not executable rule handlers. */
public final class PtuDataCatalog {
    public record Move(String id, String name, String type, String category, Integer ac,
                       int damageBase, String frequency, String range, String effects, String source) {
        /** Unmodified base dice, NOT final battle damage, STAB, criticals or ability modifiers. */
        public String baseDice() {
            if (category.equalsIgnoreCase("Status") || damageBase <= 0) return "—";
            var dice = PtuTables.dbToDice(damageBase);
            return dice.count() + "d" + dice.sides() + (dice.flat() >= 0 ? "+" : "") + dice.flat();
        }
    }
    public record Species(String id, String name, Map<String, Integer> baseStats, List<String> types,
                          List<String> capabilities, String movementJson, String skillsJson,
                          List<String> naturewalk, String size, String weight, List<String> eggGroups, String source) {
        public Species {
            baseStats = Collections.unmodifiableMap(new TreeMap<>(baseStats)); types = List.copyOf(types);
            capabilities = List.copyOf(capabilities); naturewalk = List.copyOf(naturewalk); eggGroups = List.copyOf(eggGroups);
        }
    }
    public record Ability(String id, String name, String frequency, String trigger, String effect,
                          String target, String keywords, String source) {}
    public record Pools(List<String> basic, List<String> advanced, List<String> high) {
        public Pools { basic = List.copyOf(basic); advanced = List.copyOf(advanced); high = List.copyOf(high); }
        public static Pools empty() { return new Pools(List.of(), List.of(), List.of()); }
    }
    public record Learn(String move, int level, String source) {}

    private final Map<String, Move> moves = new TreeMap<>();
    private final Map<String, Species> species = new TreeMap<>();
    private final Map<String, Ability> abilities = new TreeMap<>();
    private final Map<String, Pools> pools = new TreeMap<>();
    private final Map<String, List<Learn>> learnsets = new TreeMap<>();
    private final Map<String, List<String>> lineage = new TreeMap<>();
    private final Set<String> ambiguous = new TreeSet<>();
    private final List<String> diagnostics = new ArrayList<>();
    private final String revision;

    @FunctionalInterface public interface Resources { InputStream open(String file) throws IOException; }
    public static PtuDataCatalog bundled() {
        return load(name -> PtuDataCatalog.class.getResourceAsStream("/data/autoptu/ptu/" + name));
    }

    public static PtuDataCatalog load(Resources resources) {
        try { return new PtuDataCatalog(resources); }
        catch (IOException exception) { throw new IllegalStateException("Cannot load bundled PTU catalog", exception); }
    }

    private PtuDataCatalog(Resources resources) throws IOException {
        byte[] manifestBytes = read(resources, "manifest.json");
        revision = hash(manifestBytes);
        var manifest = JsonParser.parseString(new String(manifestBytes, StandardCharsets.UTF_8)).getAsJsonObject();
        if (manifest.get("schema").getAsInt() != 1) throw new IOException("Unsupported PTU catalog schema");
        Map<String, JsonElement> files = new HashMap<>();
        for (var value : manifest.getAsJsonArray("files")) {
            var row = value.getAsJsonObject();
            String name = text(row, "resource");
            byte[] bytes = read(resources, name);
            if (!hash(bytes).equals(text(row, "sha256"))) throw new IOException("PTU checksum mismatch: " + name);
            files.put(name, JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)));
        }
        for (var row : files.get("moves.json").getAsJsonArray()) addMove(row.getAsJsonObject(), "moves.json", false);
        for (var row : files.get("species.json").getAsJsonArray()) addSpecies(row.getAsJsonObject(), "species.json", false);
        for (var entry : files.get("pools.json").getAsJsonObject().entrySet()) pools.put(key(entry.getKey()), pool(entry.getValue().getAsJsonObject()));
        for (var row : files.get("abilities.json").getAsJsonArray()) {
            var value = row.getAsJsonObject();
            String name = text(value, "Name");
            if (name.isBlank()) continue;
            var ability = new Ability(key(name), name, text(value, "Frequency"), text(value, "Trigger"),
                    text(value, "Effect"), text(value, "Target"), text(value, "Keywords"), "abilities.json");
            insert(abilities, ability.id(), ability, "ability", false);
        }
        for (var row : files.get("learnsets.json").getAsJsonArray()) {
            var value = row.getAsJsonObject();
            addLearn(text(value, "species"), text(value, "move"), integer(value, "level", 0), "learnsets.json");
        }
        // Match Python CsvRepository: its Moves Data CSV wins, supplements only fill missing names.
        for (var row : files.get("move_oracle.json").getAsJsonArray()) addMove(row.getAsJsonObject(), "move_oracle.json", true);
        for (String file : List.of("sumo_references.json", "galar_references.json", "hisui_references.json")) {
            var supplement = files.get(file).getAsJsonObject();
            for (var entry : supplement.getAsJsonObject("abilities").entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                String name = text(value, "name");
                var ability = new Ability(key(name), name, text(value, "frequency"), text(value, "trigger"),
                        text(value, "effect") + (text(value, "bonus").isBlank() ? "" : "\nBonus: " + text(value, "bonus")),
                        text(value, "target"), text(value, "keywords"), file);
                if (!abilities.containsKey(ability.id()) && !ambiguous.contains("ability:" + ability.id()))
                    insert(abilities, ability.id(), ability, "ability", false);
            }
            for (var entry : supplement.getAsJsonObject("moves").entrySet()) {
                if (!moves.containsKey(key(entry.getKey())) && !ambiguous.contains("move:" + key(entry.getKey())))
                    addMove(entry.getValue().getAsJsonObject(), file, false);
            }
        }
        for (String file : List.of("galar.json", "hisui.json")) {
            for (var entry : files.get(file).getAsJsonObject().getAsJsonObject("entries").entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                String name = text(value, "name");
                if (!species.containsKey(key(name)) && !ambiguous.contains("species:" + key(name))) addSpecies(value, file, false);
                pools.merge(key(name), pool(value.getAsJsonObject("abilities")), PtuDataCatalog::mergePools);
                if (value.has("moves") && value.getAsJsonObject("moves").has("level_up")) {
                    for (var learned : value.getAsJsonObject("moves").getAsJsonArray("level_up")) {
                        var move = learned.getAsJsonObject();
                        addLearn(name, text(move, "move"), integer(move, "level", 0), file);
                    }
                }
            }
        }
        for (String file : List.of("galar_learnsets.json", "hisui_learnsets.json")) {
            for (var entry : files.get(file).getAsJsonObject().getAsJsonObject("entries").entrySet()) {
                for (var value : entry.getValue().getAsJsonArray()) {
                    var learned = value.getAsJsonObject();
                    addLearn(entry.getKey(), text(learned, "move"), integer(learned, "level", 0), file);
                }
            }
        }
        for (var entry : files.get("ability_overrides.json").getAsJsonObject().entrySet()) pools.put(key(entry.getKey()), pool(entry.getValue().getAsJsonObject()));
        var evolution = files.get("evolution.json").getAsJsonObject();
        if (evolution.has("lineage")) for (var entry : evolution.getAsJsonObject("lineage").entrySet()) {
            lineage.put(key(entry.getKey()), strings(entry.getValue()).stream().map(PtuDataCatalog::key).toList());
        }
        if (moves.isEmpty() || species.isEmpty() || abilities.isEmpty()) throw new IOException("Empty PTU data catalog");
    }

    private void addMove(JsonObject value, String source, boolean override) {
        String name = text(value, "name");
        if (name.isBlank()) return;
        String category = text(value, value.has("category") ? "category" : "class");
        Integer ac = nullableInteger(value, "ac");
        int db = integer(value, "damage_base", 0);
        if (!Set.of("physical", "special", "status").contains(category.toLowerCase(Locale.ROOT)) || db < 0) {
            diagnostics.add("Invalid move: " + name + " in " + source); return;
        }
        insert(moves, key(name), new Move(key(name), name, text(value, "type"), category, ac, db,
                text(value, "frequency"), text(value, "range"), text(value, value.has("effects") ? "effects" : "effect"), source), "move", override);
    }

    private void addSpecies(JsonObject value, String source, boolean override) {
        String name = text(value, "name");
        Map<String, Integer> stats = new TreeMap<>();
        var base = value.getAsJsonObject("base_stats");
        if (base == null || name.isBlank()) { diagnostics.add("Missing species stats in " + source); return; }
        for (String stat : List.of("hp", "attack", "defense", "special_attack", "special_defense", "speed")) {
            Integer amount = nullableInteger(base, stat);
            if (amount == null || amount < 1) { diagnostics.add("Invalid stat: " + name + "/" + stat); return; }
            stats.put(stat, amount);
        }
        var record = new Species(key(name), name, stats, strings(value.get("types")), strings(value.get("capabilities")),
                json(value, "movement"), json(value, "skills"), strings(value.get("naturewalk")), text(value, "size"),
                text(value, "weight"), strings(value.get("egg_groups")), source);
        var old = species.get(record.id());
        // Aliases differing only in punctuation are equivalent only if their actual PTU data agree.
        if (old != null && old.baseStats().equals(stats) && old.types().equals(record.types())
                && old.capabilities().equals(record.capabilities()) && old.movementJson().equals(record.movementJson())
                && old.skillsJson().equals(record.skillsJson()) && old.naturewalk().equals(record.naturewalk())
                && old.size().equals(record.size()) && old.weight().equals(record.weight())
                && old.eggGroups().equals(record.eggGroups()) && !override) return;
        insert(species, record.id(), record, "species", override);
    }

    private <T> void insert(Map<String, T> index, String id, T record, String kind, boolean override) {
        String conflict = kind + ":" + id;
        if (override) { index.put(id, record); ambiguous.remove(conflict); return; }
        if (ambiguous.contains(conflict)) return;
        T previous = index.putIfAbsent(id, record);
        if (previous != null && !previous.equals(record)) {
            index.remove(id); ambiguous.add(conflict); diagnostics.add("Ambiguous " + conflict);
        }
    }

    private void addLearn(String name, String move, int level, String source) {
        if (!name.isBlank() && !move.isBlank() && level >= 0) learnsets.computeIfAbsent(key(name), unused -> new ArrayList<>()).add(new Learn(move, level, source));
    }
    public Optional<Move> move(String name) { return Optional.ofNullable(moves.get(key(name))); }
    public Optional<Ability> ability(String name) { return Optional.ofNullable(abilities.get(key(name))); }
    public Optional<Species> species(String name) { return Optional.ofNullable(species.get(key(name))); }

    /** Unknown nonstandard forms never silently become their base species. */
    public Optional<Species> resolveSpecies(String namespace, String name, String form, boolean standard) {
        if (!"cobblemon".equals(namespace)) return Optional.empty();
        if (standard) return species(name);
        if (key(form).isBlank()) return Optional.empty();
        String regional = switch (key(form)) {
            case "alola", "alolan" -> "Alolan";
            case "galar", "galarian" -> "Galarian";
            case "hisui", "hisuian" -> "Hisuian";
            case "paldea", "paldean" -> "Paldean";
            default -> form;
        };
        var suffix = species(name + regional);
        return suffix.isPresent() ? suffix : species(regional + name);
    }

    public Pools pools(String speciesId) { return pools.getOrDefault(key(speciesId), Pools.empty()); }
    public List<Learn> learnset(String speciesId) {
        Map<String, Learn> result = new LinkedHashMap<>();
        collect(key(speciesId), new HashSet<>(), result, false);
        return result.values().stream().sorted(Comparator.comparingInt(Learn::level).thenComparing(Learn::move)).toList();
    }
    private void collect(String id, Set<String> visiting, Map<String, Learn> result, boolean inherited) {
        if (!visiting.add(id)) return;
        for (var move : learnsets.getOrDefault(id, List.of())) {
            if (!inherited || move.level() > 0) result.putIfAbsent(key(move.move()) + ":" + move.level(), move);
        }
        for (String ancestor : lineage.getOrDefault(id, List.of())) collect(ancestor, visiting, result, true);
    }
    public String revision() { return revision; }
    public int moveCount() { return moves.size(); }
    public int speciesCount() { return species.size(); }
    public int abilityCount() { return abilities.size(); }
    public List<String> diagnostics() { return List.copyOf(diagnostics); }
    public Set<String> moveIds() { return Collections.unmodifiableSet(moves.keySet()); }
    public Set<String> abilityIds() { return Collections.unmodifiableSet(abilities.keySet()); }

    public static String key(String name) {
        if (name == null) return "";
        return Normalizer.normalize(name.replace("♀", "f").replace("♂", "m"), Normalizer.Form.NFD)
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    private static Pools pool(JsonObject value) {
        if (value == null) return Pools.empty();
        return new Pools(strings(value.get("basic")), strings(value.get("advanced")), strings(value.get("high")));
    }
    private static Pools mergePools(Pools a, Pools b) {
        return new Pools(mergeNames(a.basic(), b.basic()), mergeNames(a.advanced(), b.advanced()), mergeNames(a.high(), b.high()));
    }
    private static List<String> mergeNames(List<String> a, List<String> b) {
        Map<String, String> names = new LinkedHashMap<>();
        java.util.stream.Stream.concat(a.stream(), b.stream()).forEach(name -> names.putIfAbsent(key(name), name));
        return List.copyOf(names.values());
    }
    private static String text(JsonObject object, String name) {
        var value = object.get(name); return value == null || value.isJsonNull() ? "" : value.getAsString().strip();
    }
    private static String json(JsonObject object, String name) { return object.has(name) ? object.get(name).toString() : "{}"; }
    private static List<String> strings(JsonElement element) {
        if (element == null || !element.isJsonArray()) return List.of();
        List<String> values = new ArrayList<>();
        for (var value : element.getAsJsonArray()) if (!value.isJsonNull() && value.isJsonPrimitive()) values.add(value.getAsString());
        return List.copyOf(values);
    }
    private static Integer nullableInteger(JsonObject value, String field) {
        String raw = text(value, field);
        // Supplement DB values include an authoritative dice annotation after the colon.
        if (field.equals("damage_base") && raw.contains(":")) raw = raw.substring(0, raw.indexOf(':')).strip();
        try { return Integer.valueOf(raw); } catch (NumberFormatException exception) { return null; }
    }
    private static int integer(JsonObject value, String field, int fallback) {
        Integer parsed = nullableInteger(value, field); return parsed == null ? fallback : parsed;
    }
    private static byte[] read(Resources resources, String file) throws IOException {
        if (!file.matches("[a-z_]+\\.json")) throw new IOException("Invalid PTU resource name");
        try (var stream = resources.open(file)) {
            if (stream == null) throw new IOException("Missing PTU resource: " + file);
            return stream.readAllBytes();
        }
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}

package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable ordered status state frozen from server-owned PTU data. */
public record CanonicalStatusState(List<CanonicalStatusEntry> entries) {
    public CanonicalStatusState {
        entries = immutableUnique(entries);
    }

    public static CanonicalStatusState fromNames(Collection<String> names) {
        if (names == null || names.isEmpty()) return new CanonicalStatusState(List.of());
        ArrayList<CanonicalStatusEntry> entries = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            CanonicalStatusEntry entry = new CanonicalStatusEntry(name);
            if (seen.add(entry.name())) entries.add(entry);
        }
        return new CanonicalStatusState(entries);
    }

    public Set<String> names() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (CanonicalStatusEntry entry : entries) names.add(entry.name());
        return Set.copyOf(names);
    }

    private static List<CanonicalStatusEntry> immutableUnique(List<CanonicalStatusEntry> source) {
        if (source == null || source.isEmpty()) return List.of();
        ArrayList<CanonicalStatusEntry> copied = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (CanonicalStatusEntry entry : source) {
            if (entry == null) throw new IllegalArgumentException("status entries must not contain null");
            if (!seen.add(entry.name())) throw new IllegalArgumentException("duplicate canonical status: " + entry.name());
            copied.add(entry);
        }
        return List.copyOf(copied);
    }
}

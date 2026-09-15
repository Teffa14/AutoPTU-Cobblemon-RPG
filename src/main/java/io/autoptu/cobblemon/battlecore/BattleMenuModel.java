package io.autoptu.cobblemon.battlecore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** UI indexes over a server-authored menu. No client targeting or range calculation. */
public final class BattleMenuModel {
    private final List<BattleChoiceMenuService.Entry> entries;
    private final Map<String, BattleChoiceMenuService.Entry> byId;
    private final Map<String, List<BattleChoiceMenuService.Entry>> groups;

    public BattleMenuModel(List<BattleChoiceMenuService.Entry> entries) {
        this.entries = List.copyOf(Objects.requireNonNull(entries));
        Map<String, BattleChoiceMenuService.Entry> ids = new LinkedHashMap<>();
        Map<String, List<BattleChoiceMenuService.Entry>> grouped = new LinkedHashMap<>();
        for (var entry : entries) {
            if (ids.putIfAbsent(entry.choiceId(), entry) != null) {
                throw new IllegalArgumentException("duplicate menu choice");
            }
            grouped.computeIfAbsent(groupKey(entry), key -> new ArrayList<>()).add(entry);
        }
        Map<String, List<BattleChoiceMenuService.Entry>> immutableGroups = new LinkedHashMap<>();
        grouped.forEach((key, value) -> immutableGroups.put(key, List.copyOf(value)));
        this.byId = Collections.unmodifiableMap(ids);
        this.groups = Collections.unmodifiableMap(immutableGroups);
    }

    public static String groupKey(BattleChoiceMenuService.Entry entry) {
        return switch (entry.kind()) {
            case MOVEMENT -> "movement";
            case ATTACK -> "attack:" + entry.moveId();
            case OTHER -> "other:" + entry.choiceId();
        };
    }

    public List<BattleChoiceMenuService.Entry> entries() {
        return entries;
    }

    public List<String> groupKeys() {
        return List.copyOf(groups.keySet());
    }

    public List<String> attackGroups() {
        return groups.keySet().stream().filter(key -> key.startsWith("attack:")).toList();
    }

    public List<BattleChoiceMenuService.Entry> group(String key) {
        return groups.getOrDefault(key, List.of());
    }

    public Optional<BattleChoiceMenuService.Entry> choice(String key) {
        return Optional.ofNullable(byId.get(key));
    }

    public List<BattleChoiceMenuService.Entry> at(String groupKey, BattleGridCoordinate cell) {
        Objects.requireNonNull(cell);
        return group(groupKey).stream().filter(entry -> cell.equals(entry.anchor())).toList();
    }

    public List<BattleChoiceMenuService.Entry> unanchored(String groupKey) {
        return group(groupKey).stream().filter(entry -> entry.anchor() == null).toList();
    }

    public boolean hasMovement() {
        return !group("movement").isEmpty();
    }

    public Optional<BattleChoiceVisualPlan.GridWindow> window() {
        var cells = entries.stream().map(BattleChoiceMenuService.Entry::anchor)
                .filter(Objects::nonNull).toList();
        if (cells.isEmpty()) return Optional.empty();
        int minX = cells.stream().mapToInt(BattleGridCoordinate::x).min().orElseThrow();
        int minY = cells.stream().mapToInt(BattleGridCoordinate::y).min().orElseThrow();
        int maxX = cells.stream().mapToInt(BattleGridCoordinate::x).max().orElseThrow();
        int maxY = cells.stream().mapToInt(BattleGridCoordinate::y).max().orElseThrow();
        maxX = (int) Math.min(maxX, (long) minX + 11);
        maxY = (int) Math.min(maxY, (long) minY + 11);
        return Optional.of(new BattleChoiceVisualPlan.GridWindow(minX, maxX, minY, maxY));
    }

    public List<BattleChoiceMenuService.Entry> page(String groupKey, int pageIndex, int pageSize) {
        if (pageIndex < 0 || pageSize < 1) throw new IllegalArgumentException("invalid page");
        var group = group(groupKey);
        long first = (long) pageIndex * pageSize;
        if (first >= group.size()) return List.of();
        return group.subList((int) first, (int) Math.min(group.size(), first + pageSize));
    }
}

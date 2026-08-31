package io.autoptu.cobblemon.authority;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Server-authoritative cosmetic Trainer profile boundary. These fields never modify PTU rules or progression. */
public final class CanonicalTrainerProfileService {
    private static final Map<String, String> TITLES = Map.of(
            "rookie", "Rookie Trainer",
            "field_researcher", "Field Researcher",
            "league_challenger", "League Challenger"
    );
    private static final Set<String> CARD_THEMES = Set.of("classic", "cedar", "league");

    private final FileCanonicalTrainerProfileRepository repository;

    public CanonicalTrainerProfileService(FileCanonicalTrainerProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Snapshot inspect(String playerId) {
        return snapshot(repository.findOrCreate(playerId));
    }

    public Snapshot chooseTitle(String playerId, String titleId) {
        if (!TITLES.containsKey(titleId)) throw new IllegalArgumentException("unknown server-authored Trainer title");
        return mutate(playerId, titleId, null);
    }

    public Snapshot chooseCardTheme(String playerId, String cardThemeId) {
        if (!CARD_THEMES.contains(cardThemeId)) throw new IllegalArgumentException("unknown server-authored Trainer card theme");
        return mutate(playerId, null, cardThemeId);
    }

    public static Map<String, String> authoredTitles() {
        return TITLES;
    }

    public static Set<String> authoredCardThemes() {
        return CARD_THEMES;
    }

    private Snapshot mutate(String playerId, String titleId, String cardThemeId) {
        for (int attempt = 0; attempt < 4; attempt++) {
            var current = repository.findOrCreate(playerId);
            String nextTitle = titleId == null ? current.titleId() : titleId;
            String nextTheme = cardThemeId == null ? current.cardThemeId() : cardThemeId;
            if (nextTitle.equals(current.titleId()) && nextTheme.equals(current.cardThemeId())) return snapshot(current);
            var replacement = new FileCanonicalTrainerProfileRepository.TrainerProfile(
                    current.playerId(), nextTitle, nextTheme, current.revision() + 1);
            if (repository.replaceIfRevision(replacement, current.revision())) return snapshot(replacement);
        }
        throw new IllegalStateException("canonical Trainer profile changed concurrently");
    }

    private static Snapshot snapshot(FileCanonicalTrainerProfileRepository.TrainerProfile state) {
        return new Snapshot(
                state.playerId(),
                state.titleId(),
                TITLES.getOrDefault(state.titleId(), state.titleId()),
                state.cardThemeId(),
                state.revision());
    }

    public record Snapshot(String playerId, String titleId, String titleDisplayName, String cardThemeId, long revision) { }
}

package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;

/**
 * Server-owned read model for physical NPC dialogue.
 *
 * <p>The view may adapt presentation from durable RPG state, but it never changes quest state,
 * grants rewards, infers PTU legality, or trusts client/Cobblemon gameplay data.</p>
 */
public final class CanonicalNpcDialogueViewService {
    private final CanonicalNpcDialogueCatalogue dialogueCatalogue;
    private final CanonicalQuestCatalogue questCatalogue;
    private final FileCanonicalQuestJournalRepository questJournals;

    public CanonicalNpcDialogueViewService(
            CanonicalNpcDialogueCatalogue dialogueCatalogue,
            CanonicalQuestCatalogue questCatalogue,
            FileCanonicalQuestJournalRepository questJournals
    ) {
        this.dialogueCatalogue = Objects.requireNonNull(dialogueCatalogue, "dialogueCatalogue");
        this.questCatalogue = Objects.requireNonNull(questCatalogue, "questCatalogue");
        this.questJournals = Objects.requireNonNull(questJournals, "questJournals");
    }

    public DialogueView inspect(String playerId, String npcId) {
        var dialogue = dialogueCatalogue.dialogue(npcId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical npcId: " + npcId));
        var journal = questJournals.findOrCreate(playerId);
        List<OptionView> options = dialogue.options().stream()
                .map(option -> project(option, journal))
                .toList();
        return new DialogueView(dialogue.npcId(), dialogue.displayName(), dialogue.openingLine(), options, journal.revision());
    }

    private OptionView project(
            CanonicalNpcDialogueCatalogue.Option option,
            FileCanonicalQuestJournalRepository.JournalState journal
    ) {
        if (option.questId() == null) {
            return new OptionView(option.optionId(), option.label(), false, true, null, option.challengeId(), null);
        }
        var quest = questCatalogue.quest(option.questId())
                .orElseThrow(() -> new IllegalStateException("dialogue references unknown canonical questId: " + option.questId()));
        boolean accepted = journal.entries().containsKey(quest.questId());
        List<String> missing = accepted ? List.of() : quest.requiredAcceptedQuestIds().stream()
                .filter(requiredQuestId -> !journal.entries().containsKey(requiredQuestId))
                .toList();
        boolean eligible = accepted || missing.isEmpty();
        String label = accepted
                ? "Continue: " + quest.title()
                : eligible ? option.label() : "Locked: " + quest.title();
        String lockReason = eligible ? null : prerequisiteReason(missing);
        return new OptionView(option.optionId(), label, accepted, eligible, quest.questId(), null, lockReason);
    }

    private String prerequisiteReason(List<String> missingQuestIds) {
        String requiredTitles = missingQuestIds.stream()
                .map(questId -> questCatalogue.quest(questId).map(CanonicalQuestCatalogue.Quest::title).orElse(questId))
                .reduce((left, right) -> left + ", " + right)
                .orElse("required story work");
        return "Requires accepted quest: " + requiredTitles;
    }

    public record DialogueView(
            String npcId,
            String displayName,
            String openingLine,
            List<OptionView> options,
            long questJournalRevision
    ) {
        public DialogueView {
            options = List.copyOf(Objects.requireNonNull(options, "options"));
        }
    }

    public record OptionView(
            String optionId,
            String displayLabel,
            boolean acceptedQuest,
            boolean eligibleQuest,
            String questId,
            String challengeId,
            String lockReason
    ) { }
}

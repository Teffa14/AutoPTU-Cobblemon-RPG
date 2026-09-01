package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    private final FileCanonicalWorldStoryRepository worldStory;

    public CanonicalNpcDialogueViewService(
            CanonicalNpcDialogueCatalogue dialogueCatalogue,
            CanonicalQuestCatalogue questCatalogue,
            FileCanonicalQuestJournalRepository questJournals
    ) {
        this(dialogueCatalogue, questCatalogue, questJournals, null);
    }

    public CanonicalNpcDialogueViewService(
            CanonicalNpcDialogueCatalogue dialogueCatalogue,
            CanonicalQuestCatalogue questCatalogue,
            FileCanonicalQuestJournalRepository questJournals,
            FileCanonicalWorldStoryRepository worldStory
    ) {
        this.dialogueCatalogue = Objects.requireNonNull(dialogueCatalogue, "dialogueCatalogue");
        this.questCatalogue = Objects.requireNonNull(questCatalogue, "questCatalogue");
        this.questJournals = Objects.requireNonNull(questJournals, "questJournals");
        this.worldStory = worldStory;
    }

    public DialogueView inspect(String playerId, String npcId) {
        var dialogue = dialogueCatalogue.dialogue(npcId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical npcId: " + npcId));
        var journal = questJournals.findOrCreate(playerId);
        Set<String> storyFlags = worldStory == null
                ? Set.of()
                : worldStory.findOrCreate(playerId).storyFlags();
        List<OptionView> options = dialogue.options().stream()
                .map(option -> project(option, journal, storyFlags))
                .toList();
        return new DialogueView(dialogue.npcId(), dialogue.displayName(), dialogue.openingLine(), options, journal.revision());
    }

    private OptionView project(
            CanonicalNpcDialogueCatalogue.Option option,
            FileCanonicalQuestJournalRepository.JournalState journal,
            Set<String> storyFlags
    ) {
        if (option.questId() == null) {
            return new OptionView(option.optionId(), option.label(), false, true, null, option.challengeId(), null);
        }
        var quest = questCatalogue.quest(option.questId())
                .orElseThrow(() -> new IllegalStateException("dialogue references unknown canonical questId: " + option.questId()));
        boolean accepted = journal.entries().containsKey(quest.questId());
        List<String> missingQuests = accepted ? List.of() : quest.requiredAcceptedQuestIds().stream()
                .filter(requiredQuestId -> !journal.entries().containsKey(requiredQuestId))
                .toList();
        List<String> missingStoryFlags = accepted ? List.of() : quest.requiredStoryFlags().stream()
                .filter(requiredFlag -> !storyFlags.contains(requiredFlag))
                .toList();
        boolean eligible = accepted || (missingQuests.isEmpty() && missingStoryFlags.isEmpty());
        String label = accepted
                ? "Continue: " + quest.title()
                : eligible ? option.label() : "Locked: " + quest.title();
        String lockReason = eligible ? null : prerequisiteReason(missingQuests, missingStoryFlags);
        return new OptionView(option.optionId(), label, accepted, eligible, quest.questId(), null, lockReason);
    }

    private String prerequisiteReason(List<String> missingQuestIds, List<String> missingStoryFlags) {
        String questReason = missingQuestIds.isEmpty() ? null : "Accept first: " + missingQuestIds.stream()
                .map(questId -> questCatalogue.quest(questId).map(CanonicalQuestCatalogue.Quest::title).orElse(questId))
                .reduce((left, right) -> left + ", " + right)
                .orElse("required quest");
        String storyReason = missingStoryFlags.isEmpty() ? null : "Story choice required: " + missingStoryFlags.stream()
                .map(CanonicalNpcDialogueViewService::storyFlagLabel)
                .reduce((left, right) -> left + ", " + right)
                .orElse("required story choice");
        if (questReason == null) return storyReason;
        if (storyReason == null) return questReason;
        return questReason + "; " + storyReason;
    }

    private static String storyFlagLabel(String flag) {
        if ("cedar_meadow_observe_first".equals(flag)) return "Observe before approaching Cedar Meadow";
        return flag.replace('_', ' ');
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

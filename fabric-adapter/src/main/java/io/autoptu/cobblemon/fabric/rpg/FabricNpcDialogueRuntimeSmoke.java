package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalNpcDialogueCatalogue;
import io.autoptu.cobblemon.authority.CanonicalTrainerChallengeCatalogue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dedicated-server proof that authored NPC identity and challenge mapping are bound server-side. */
public final class FabricNpcDialogueRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveNpcDialogueSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live canonical NPC dialogue smoke passed";
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricNpcDialogueRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)
                && !Boolean.getBoolean(FabricHealingStationRuntimeSmoke.ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricNpcDialogueRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        VillagerEntity ranger = new VillagerEntity(EntityType.VILLAGER, world);
        try {
            FabricNpcDialogueRuntime.bind(ranger, "cedar-ranger");
            String resolved = FabricNpcDialogueRuntime.npcId(ranger).orElseThrow();
            if (!resolved.equals("cedar-ranger")) {
                throw new IllegalStateException("authored NPC binding resolved unexpected id " + resolved);
            }
            var challengeOption = CanonicalNpcDialogueCatalogue.DEFAULT.dialogue(resolved).orElseThrow()
                    .option("field-spar").orElseThrow();
            if (challengeOption.challengeId() == null) {
                throw new IllegalStateException("Cedar Ranger challenge option is missing challenge identity");
            }
            var challenge = CanonicalTrainerChallengeCatalogue.DEFAULT.challenge(challengeOption.challengeId()).orElseThrow();
            if (!challenge.npcId().equals(resolved)) {
                throw new IllegalStateException("authored Trainer challenge resolved to a different NPC");
            }
            VillagerEntity unbound = new VillagerEntity(EntityType.VILLAGER, world);
            if (FabricNpcDialogueRuntime.npcId(unbound).isPresent()) {
                throw new IllegalStateException("unbound villager was accepted as canonical NPC");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            ranger.discard();
        }
    }
}

package io.autoptu.cobblemon.fabric.admin;

import java.util.List;

/**
 * Server-owned operational capability view for the Minecraft RPG adapter.
 *
 * <p>This service exposes conservative integration readiness only. It never derives PTU legality,
 * outcomes, action costs, damage, item effects, or recovery state from Minecraft or Cobblemon.</p>
 */
public final class FabricFeatureGateService {
    public enum Status {
        ENABLED,
        PARTIAL,
        BLOCKED
    }

    public record Gate(String id, Status status, String reason) {}

    private static final List<Gate> GATES = List.of(
            new Gate(
                    "persistent_rpg_state",
                    Status.ENABLED,
                    "Trainer, Pokemon, party/storage, inventory, wallet and progression use server-owned durable state."),
            new Gate(
                    "wild_world_projection",
                    Status.PARTIAL,
                    "Server-authored WILD projection is live; broader species coverage still requires complete canonical blueprints."),
            new Gate(
                    "world_to_battle_handoff",
                    Status.PARTIAL,
                    "World encounters and battle bindings exist, but the complete normal-world battle/result loop is not yet closed."),
            new Gate(
                    "autoptu_battle_authority",
                    Status.PARTIAL,
                    "Minecraft delegates battle legality and outcomes to AutoPTU-Java; complete PTU category coverage is still incomplete upstream."),
            new Gate(
                    "durable_battle_recovery",
                    Status.BLOCKED,
                    "No authoritative persisted battle checkpoint/recovery contract exists."),
            new Gate(
                    "ptu_item_effects",
                    Status.BLOCKED,
                    "Canonical inventory is durable, but authoritative PTU item-use behavior is not available to Minecraft."),
            new Gate(
                    "full_ptu_action_economy",
                    Status.BLOCKED,
                    "Upstream owns reaction usage state, but complete action-economy and lifecycle execution is not yet verified."));

    public List<Gate> snapshot() {
        return GATES;
    }
}

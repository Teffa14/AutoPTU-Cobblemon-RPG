package io.autoptu.cobblemon.fabric.world;

/**
 * Server-authored ambient social presentation role for a visible wild actor.
 *
 * <p>This role has no PTU initiative, stats, targeting, encounter priority or battle semantics.
 * It exists only so Minecraft can project authored ecology without inferring rank from Cobblemon
 * gameplay state or mechanical values.</p>
 */
public enum WildSocialRole {
    MEMBER,
    ALPHA
}

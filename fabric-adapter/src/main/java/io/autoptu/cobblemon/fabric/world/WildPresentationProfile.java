package io.autoptu.cobblemon.fabric.world;

/**
 * Server-authored Minecraft presentation capabilities for one visible WILD encounter.
 *
 * <p>These flags control only the Cobblemon actor projection. They do not confer PTU rules,
 * encounter strength, level, moves, stats, abilities, initiative, aggression or battle outcomes.</p>
 */
public record WildPresentationProfile(
        boolean alphaVisual,
        HerdRole herdRole
) {
    public enum HerdRole {
        NONE,
        LEADER,
        FOLLOWER
    }

    public static final WildPresentationProfile STANDARD = new WildPresentationProfile(false, HerdRole.NONE);

    public WildPresentationProfile {
        if (herdRole == null) throw new IllegalArgumentException("herdRole is required");
        if (herdRole == HerdRole.LEADER && !alphaVisual) {
            throw new IllegalArgumentException("alpha herd leader presentation requires alphaVisual");
        }
    }

    public static WildPresentationProfile alphaLeader() {
        return new WildPresentationProfile(true, HerdRole.LEADER);
    }
}

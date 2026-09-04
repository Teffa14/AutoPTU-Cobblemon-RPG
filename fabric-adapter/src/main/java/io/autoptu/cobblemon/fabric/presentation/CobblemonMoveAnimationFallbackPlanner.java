package io.autoptu.cobblemon.fabric.presentation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Presentation-only planner for moves that do not have an exact Cobblemon ActionEffect.
 *
 * The planner never decides PTU mechanics. It only proposes already-existing Cobblemon animation
 * assets that are visually related to the requested move id. Runtime code still verifies that each
 * candidate is actually loaded before using it. A deterministic variant index is also returned so
 * reused native effects can receive a distinct project-owned visual accent.
 */
final class CobblemonMoveAnimationFallbackPlanner {
    private static final Map<BattleMoveAnimationProfile.Theme, List<String>> THEME_CANDIDATES = themeCandidates();
    private static final Map<BattleMoveAnimationProfile.Motion, List<String>> MOTION_CANDIDATES = motionCandidates();

    private CobblemonMoveAnimationFallbackPlanner() {
    }

    static Plan plan(String moveId) {
        String normalized = BattleMoveAnimationProfile.normalize(moveId);
        BattleMoveAnimationProfile profile = BattleMoveAnimationProfile.resolve(normalized);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.addAll(THEME_CANDIDATES.getOrDefault(profile.theme(), List.of()));
        candidates.addAll(MOTION_CANDIDATES.getOrDefault(profile.motion(), List.of()));
        candidates.remove(normalized);

        ArrayList<String> ordered = new ArrayList<>(candidates);
        int variant = Math.floorMod(normalized.hashCode(), 4);
        if (!ordered.isEmpty()) {
            int start = Math.floorMod(normalized.hashCode(), ordered.size());
            ArrayList<String> rotated = new ArrayList<>(ordered.size());
            for (int i = 0; i < ordered.size(); i++) {
                rotated.add(ordered.get((start + i) % ordered.size()));
            }
            ordered = rotated;
        }
        return new Plan(List.copyOf(ordered), variant);
    }

    record Plan(List<String> candidateEffectPaths, int variant) {
        Plan {
            candidateEffectPaths = List.copyOf(candidateEffectPaths);
            if (variant < 0 || variant > 3) throw new IllegalArgumentException("variant must be 0..3");
        }
    }

    private static Map<BattleMoveAnimationProfile.Theme, List<String>> themeCandidates() {
        EnumMap<BattleMoveAnimationProfile.Theme, List<String>> map =
                new EnumMap<>(BattleMoveAnimationProfile.Theme.class);
        map.put(BattleMoveAnimationProfile.Theme.FIRE,
                List.of("ember", "flamethrower", "fireblast", "heatwave"));
        map.put(BattleMoveAnimationProfile.Theme.WATER,
                List.of("watergun", "bubblebeam", "hydropump", "surf"));
        map.put(BattleMoveAnimationProfile.Theme.ELECTRIC,
                List.of("thundershock", "thunderbolt", "discharge", "electroweb"));
        map.put(BattleMoveAnimationProfile.Theme.ICE,
                List.of("iceshard", "icebeam", "icywind", "blizzard"));
        map.put(BattleMoveAnimationProfile.Theme.GRASS,
                List.of("razorleaf", "magicalleaf", "energyball", "solarbeam"));
        map.put(BattleMoveAnimationProfile.Theme.PSYCHIC,
                List.of("confusion", "psybeam", "psychic", "psyshock"));
        map.put(BattleMoveAnimationProfile.Theme.GHOST,
                List.of("shadowball", "shadowclaw", "hex", "nightshade"));
        map.put(BattleMoveAnimationProfile.Theme.POISON,
                List.of("acid", "sludgebomb", "poisonjab", "sludgewave"));
        map.put(BattleMoveAnimationProfile.Theme.GROUND,
                List.of("mudshot", "earthpower", "bulldoze", "earthquake"));
        map.put(BattleMoveAnimationProfile.Theme.ROCK,
                List.of("rockthrow", "rockslide", "powergem", "stoneedge"));
        map.put(BattleMoveAnimationProfile.Theme.DRAGON,
                List.of("dragonbreath", "dragonclaw", "dragonpulse", "dracometeor"));
        map.put(BattleMoveAnimationProfile.Theme.FAIRY,
                List.of("fairywind", "dazzlinggleam", "moonblast", "playrough"));
        map.put(BattleMoveAnimationProfile.Theme.FLYING,
                List.of("gust", "airslash", "aerialace", "hurricane"));
        map.put(BattleMoveAnimationProfile.Theme.STEEL,
                List.of("metalclaw", "flashcannon", "ironhead", "bulletpunch"));
        map.put(BattleMoveAnimationProfile.Theme.DARK,
                List.of("bite", "darkpulse", "crunch", "snarl"));
        map.put(BattleMoveAnimationProfile.Theme.BUG,
                List.of("bugbite", "signalbeam", "xscissor", "bugbuzz"));
        map.put(BattleMoveAnimationProfile.Theme.FIGHTING,
                List.of("karatechop", "aurasphere", "brickbreak", "closecombat"));
        map.put(BattleMoveAnimationProfile.Theme.NORMAL,
                List.of("tackle", "swift", "quickattack", "hyperbeam"));
        return Map.copyOf(map);
    }

    private static Map<BattleMoveAnimationProfile.Motion, List<String>> motionCandidates() {
        EnumMap<BattleMoveAnimationProfile.Motion, List<String>> map =
                new EnumMap<>(BattleMoveAnimationProfile.Motion.class);
        map.put(BattleMoveAnimationProfile.Motion.MELEE,
                List.of("tackle", "quickattack", "scratch", "bite", "aerialace"));
        map.put(BattleMoveAnimationProfile.Motion.PROJECTILE,
                List.of("swift", "shadowball", "energyball", "rockthrow", "ember"));
        map.put(BattleMoveAnimationProfile.Motion.BEAM,
                List.of("hyperbeam", "icebeam", "psybeam", "flamethrower", "hydropump"));
        map.put(BattleMoveAnimationProfile.Motion.WAVE,
                List.of("surf", "heatwave", "blizzard", "hurricane", "razorleaf"));
        map.put(BattleMoveAnimationProfile.Motion.BURST,
                List.of("explosion", "discharge", "dazzlinggleam", "boomburst"));
        map.put(BattleMoveAnimationProfile.Motion.ARC,
                List.of("swift", "airslash", "magicalleaf", "darkpulse", "aurasphere"));
        return Map.copyOf(map);
    }
}

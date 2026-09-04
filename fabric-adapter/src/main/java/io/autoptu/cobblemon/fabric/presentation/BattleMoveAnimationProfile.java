package io.autoptu.cobblemon.fabric.presentation;

import java.util.Locale;

/**
 * Presentation-only choreography selection for an already-authoritative move id.
 *
 * The profile changes only how Minecraft renders the move. It does not infer type effectiveness,
 * targeting, range, hit/crit, damage, status, displacement, legality or any other PTU rule.
 * Every non-blank move id resolves to a visible choreography, including unknown/custom moves.
 */
public record BattleMoveAnimationProfile(Motion motion, Theme theme) {
    public enum Motion {
        MELEE,
        PROJECTILE,
        BEAM,
        WAVE,
        BURST,
        ARC
    }

    public enum Theme {
        NORMAL,
        FIRE,
        WATER,
        ELECTRIC,
        ICE,
        GRASS,
        PSYCHIC,
        GHOST,
        POISON,
        GROUND,
        ROCK,
        DRAGON,
        FAIRY,
        FLYING,
        STEEL,
        DARK,
        BUG,
        FIGHTING
    }

    public BattleMoveAnimationProfile {
        if (motion == null) throw new IllegalArgumentException("motion is required");
        if (theme == null) throw new IllegalArgumentException("theme is required");
    }

    public static BattleMoveAnimationProfile resolve(String moveId) {
        String move = normalize(moveId);
        return new BattleMoveAnimationProfile(resolveMotion(move), resolveTheme(move));
    }

    static String normalize(String moveId) {
        if (moveId == null || moveId.isBlank()) throw new IllegalArgumentException("moveId is required");
        return moveId.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static Motion resolveMotion(String move) {
        if (containsAny(move,
                "explosion", "selfdestruct", "boomburst", "dazzlinggleam", "discharge", "eruption")) {
            return Motion.BURST;
        }
        if (containsAny(move,
                "beam", "ray", "laser", "pulse", "cannon", "lance", "thunderbolt", "solarbeam",
                "flamethrower", "dragonbreath")) {
            return Motion.BEAM;
        }
        if (containsAny(move,
                "ball", "bomb", "shot", "gun", "seed", "shard", "spear", "arrow", "spike",
                "meteor", "rockthrow", "sludge", "bubble", "ember", "acid", "mudshot")) {
            return Motion.PROJECTILE;
        }
        if (containsAny(move,
                "wave", "surf", "wind", "gust", "voice", "sound", "earthquake", "bulldoze",
                "hurricane", "blizzard", "whirlpool", "razorleaf", "heatwave")) {
            return Motion.WAVE;
        }
        if (containsAny(move,
                "tackle", "punch", "kick", "slash", "bite", "peck", "headbutt", "quickattack",
                "wingattack", "aerialace", "scratch", "pound", "ironhead", "tail", "claw",
                "chop", "fang", "slam", "strike", "rush", "charge")) {
            return Motion.MELEE;
        }
        // Unknown and custom moves still receive a complete source-to-target arc rather than a no-op.
        return Motion.ARC;
    }

    private static Theme resolveTheme(String move) {
        if (containsAny(move, "fire", "flame", "ember", "blaze", "burn", "heat", "inferno", "lava", "magma", "eruption")) {
            return Theme.FIRE;
        }
        if (containsAny(move, "water", "aqua", "bubble", "surf", "hydro", "scald", "brine", "waterfall", "whirlpool")) {
            return Theme.WATER;
        }
        if (containsAny(move, "thunder", "electric", "spark", "volt", "zap", "discharge", "electro")) {
            return Theme.ELECTRIC;
        }
        if (containsAny(move, "ice", "icy", "frost", "snow", "blizzard", "avalanche", "freeze", "glaciate")) {
            return Theme.ICE;
        }
        if (containsAny(move, "leaf", "seed", "vine", "grass", "petal", "solar", "spore", "pollen", "wood", "branch")) {
            return Theme.GRASS;
        }
        if (containsAny(move, "psychic", "psy", "zen", "confusion", "extrasensory", "telekinesis")) {
            return Theme.PSYCHIC;
        }
        if (containsAny(move, "shadow", "ghost", "hex", "phantom", "poltergeist", "spectral")) {
            return Theme.GHOST;
        }
        if (containsAny(move, "poison", "acid", "sludge", "toxic", "venom", "gunk", "smog")) {
            return Theme.POISON;
        }
        if (containsAny(move, "earthquake", "earth", "ground", "mud", "sand", "bulldoze", "fissure", "drill")) {
            return Theme.GROUND;
        }
        if (containsAny(move, "rock", "stone", "boulder", "meteor", "powergem")) {
            return Theme.ROCK;
        }
        if (containsAny(move, "dragon", "draco", "scale", "outrage", "twister")) {
            return Theme.DRAGON;
        }
        if (containsAny(move, "fairy", "moon", "dazzling", "drainingkiss", "playrough", "disarmingvoice")) {
            return Theme.FAIRY;
        }
        if (containsAny(move, "air", "aerial", "wing", "gust", "hurricane", "wind", "fly", "bounce")) {
            return Theme.FLYING;
        }
        if (containsAny(move, "iron", "steel", "metal", "gear", "gyro", "bulletpunch")) {
            return Theme.STEEL;
        }
        if (containsAny(move, "dark", "night", "sucker", "knockoff", "crunch", "thief")) {
            return Theme.DARK;
        }
        if (containsAny(move, "bug", "signal", "insect", "xscissor", "furycutter")) {
            return Theme.BUG;
        }
        if (containsAny(move, "punch", "kick", "karate", "brick", "combat", "aurasphere", "forcepalm", "chop")) {
            return Theme.FIGHTING;
        }
        return Theme.NORMAL;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }
}

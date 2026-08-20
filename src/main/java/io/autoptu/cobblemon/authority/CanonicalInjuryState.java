package io.autoptu.cobblemon.authority;

/** Server-owned persistent PTU injury count. */
public record CanonicalInjuryState(int injuries) {
    public CanonicalInjuryState {
        if (injuries < 0) throw new IllegalArgumentException("injuries must be >= 0");
    }
}

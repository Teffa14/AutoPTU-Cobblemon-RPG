package io.autoptu.cobblemon.battlecore;

/**
 * Server-owned handoff for converting opaque world identities into an authoritative battle claim.
 *
 * Implementations must resolve canonical Trainer/Pokemon records from server persistence and may
 * reject unresolved, duplicated, unavailable or otherwise ineligible participants. World adapters
 * must not construct canonical stats or battle state themselves.
 */
@FunctionalInterface
public interface CanonicalEncounterClaimService {
    ClaimResult tryClaim(EncounterClaimRequest request);

    record ClaimResult(boolean claimed, String reservationId, String rejectionCode) {
        public ClaimResult {
            if (claimed) {
                if (reservationId == null || reservationId.isBlank()) {
                    throw new IllegalArgumentException("claimed encounters require reservationId");
                }
                reservationId = reservationId.strip();
                rejectionCode = null;
            } else {
                reservationId = null;
                if (rejectionCode == null || rejectionCode.isBlank()) {
                    throw new IllegalArgumentException("rejected encounters require rejectionCode");
                }
                rejectionCode = rejectionCode.strip();
            }
        }

        public static ClaimResult claimed(String reservationId) {
            return new ClaimResult(true, reservationId, null);
        }

        public static ClaimResult rejected(String rejectionCode) {
            return new ClaimResult(false, null, rejectionCode);
        }
    }
}

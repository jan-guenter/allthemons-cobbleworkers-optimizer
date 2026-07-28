package io.github.janguenter.atmons.cobbleworkers;

import java.util.UUID;

/**
 * Pure deterministic scheduling logic used by the dispatcher Mixin.
 */
public final class DecisionSchedule {
    private static final long HASH_SEED = 0x9E3779B97F4A7C15L;

    private DecisionSchedule() {
    }

    /**
     * Tests whether an area scan should be dispatched on this tick.
     *
     * @param gameTime server game time
     * @param interval configured interval
     * @param stagger whether to derive a stable phase
     * @param dimension canonical dimension ID
     * @param pasturePosition packed pasture block position
     * @return true when the upstream dispatcher should run
     */
    public static boolean shouldRunArea(
            long gameTime,
            int interval,
            boolean stagger,
            String dimension,
            long pasturePosition) {
        if (interval <= 1) {
            return true;
        }
        int phase = stagger ? areaPhase(interval, dimension, pasturePosition) : 0;
        return Math.floorMod(gameTime, (long) interval) == phase;
    }

    /**
     * Tests whether one Pokémon should perform high-level decisions this tick.
     *
     * @param gameTime server game time
     * @param interval configured interval
     * @param stagger whether to derive a stable phase
     * @param dimension canonical dimension ID
     * @param pasturePosition packed pasture block position
     * @param pokemonId stable Pokémon UUID
     * @return true when the upstream dispatcher should run
     */
    public static boolean shouldRunWorker(
            long gameTime,
            int interval,
            boolean stagger,
            String dimension,
            long pasturePosition,
            UUID pokemonId) {
        if (interval <= 1) {
            return true;
        }
        int phase = stagger
                ? workerPhase(interval, dimension, pasturePosition, pokemonId)
                : 0;
        return Math.floorMod(gameTime, (long) interval) == phase;
    }

    static int areaPhase(int interval, String dimension, long pasturePosition) {
        long hash = mix(HASH_SEED, stableStringHash(dimension));
        hash = mix(hash, pasturePosition);
        return (int) Math.floorMod(hash, (long) interval);
    }

    static int workerPhase(
            int interval,
            String dimension,
            long pasturePosition,
            UUID pokemonId) {
        long hash = mix(HASH_SEED, stableStringHash(dimension));
        hash = mix(hash, pasturePosition);
        hash = mix(hash, pokemonId.getMostSignificantBits());
        hash = mix(hash, pokemonId.getLeastSignificantBits());
        return (int) Math.floorMod(hash, (long) interval);
    }

    static long stableStringHash(String value) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long mix(long state, long value) {
        long mixed = state ^ value;
        mixed ^= mixed >>> 30;
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }
}

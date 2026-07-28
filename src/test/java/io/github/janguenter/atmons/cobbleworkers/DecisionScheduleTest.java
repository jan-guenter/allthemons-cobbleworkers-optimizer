package io.github.janguenter.atmons.cobbleworkers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DecisionScheduleTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final UUID FIRST_POKEMON =
            UUID.fromString("018f3f51-84bb-7c11-b064-8f81a42f0175");
    private static final UUID SECOND_POKEMON =
            UUID.fromString("018f3f51-84bb-7c11-b064-8f81a42f0176");

    @Test
    void intervalOneAlwaysRestoresStockDispatch() {
        for (long tick = 0; tick < 100; tick++) {
            assertTrue(DecisionSchedule.shouldRunArea(tick, 1, true, OVERWORLD, 42L));
            assertTrue(DecisionSchedule.shouldRunWorker(
                    tick,
                    1,
                    true,
                    OVERWORLD,
                    42L,
                    FIRST_POKEMON));
        }
    }

    @Test
    void disabledStaggerAlignsToIntervalBoundary() {
        assertTrue(DecisionSchedule.shouldRunArea(0, 5, false, OVERWORLD, 42L));
        assertFalse(DecisionSchedule.shouldRunArea(1, 5, false, OVERWORLD, 42L));
        assertTrue(DecisionSchedule.shouldRunArea(5, 5, false, OVERWORLD, 42L));
    }

    @Test
    void staggerRunsExactlyOncePerInterval() {
        int executions = 0;
        for (long tick = 0; tick < 17; tick++) {
            if (DecisionSchedule.shouldRunWorker(
                    tick,
                    17,
                    true,
                    OVERWORLD,
                    42L,
                    FIRST_POKEMON)) {
                executions++;
            }
        }
        assertEquals(1, executions);
    }

    @Test
    void phaseUsesDimensionPastureAndPokemon() {
        int interval = 997;
        int first = DecisionSchedule.workerPhase(
                interval,
                OVERWORLD,
                42L,
                FIRST_POKEMON);
        assertNotEquals(
                first,
                DecisionSchedule.workerPhase(
                        interval,
                        "minecraft:the_nether",
                        42L,
                        FIRST_POKEMON));
        assertNotEquals(
                first,
                DecisionSchedule.workerPhase(
                        interval,
                        OVERWORLD,
                        43L,
                        FIRST_POKEMON));
        assertNotEquals(
                first,
                DecisionSchedule.workerPhase(
                        interval,
                        OVERWORLD,
                        42L,
                        SECOND_POKEMON));
    }

    @Test
    void phaseIsStable() {
        int first = DecisionSchedule.areaPhase(20, OVERWORLD, -123456789L);
        int second = DecisionSchedule.areaPhase(20, OVERWORLD, -123456789L);
        assertEquals(first, second);
    }
}

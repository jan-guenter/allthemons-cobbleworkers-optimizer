package io.github.janguenter.atmons.cobbleworkers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.janguenter.atmons.cobbleworkers.MetricsRegistry.DispatcherKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsRegistryTest {
    @BeforeEach
    void resetBeforeTest() {
        MetricsRegistry.reset();
    }

    @AfterEach
    void resetAfterTest() {
        MetricsRegistry.reset();
    }

    @Test
    void capturesJobAndTargetSetCounters() {
        MetricsRegistry.recordEligibility("EggHandler", 100L, true, false);
        MetricsRegistry.recordEligibility("EggHandler", 200L, false, true);
        MetricsRegistry.recordDecision("EggHandler", 300L, false);
        MetricsRegistry.recordTargetSet("EggHandler", 4);
        MetricsRegistry.recordTargetSet("EggHandler", 8);

        MetricsRegistry.JobSnapshot job = MetricsRegistry.snapshot().jobs().getFirst();
        assertEquals("EggHandler", job.job());
        assertEquals(2L, job.eligibilityChecks());
        assertEquals(1L, job.eligibleChecks());
        assertEquals(300L, job.eligibilityNanoseconds());
        assertEquals(1L, job.decisions());
        assertEquals(300L, job.decisionNanoseconds());
        assertEquals(1L, job.failures());
        assertEquals(2L, job.targetSetSamples());
        assertEquals(12L, job.targetSetTotal());
        assertEquals(8L, job.targetSetMaximum());
    }

    @Test
    void capturesDispatcherSkipsAndExecutions() {
        MetricsRegistry.dispatcherSkipped(DispatcherKind.AREA_SCAN);
        long started = MetricsRegistry.dispatcherStarted(DispatcherKind.AREA_SCAN);
        MetricsRegistry.dispatcherFinished(DispatcherKind.AREA_SCAN, started);

        MetricsRegistry.DispatcherSnapshot area =
                MetricsRegistry.snapshot().dispatchers().get(DispatcherKind.AREA_SCAN);
        assertEquals(2L, area.attempts());
        assertEquals(1L, area.executions());
        assertEquals(1L, area.skips());
        assertTrue(area.nanoseconds() >= 0);
    }

    @Test
    void resetRemovesAccumulatedState() {
        MetricsRegistry.recordDecision("HeartRipper", 12L, false);
        MetricsRegistry.reset();

        MetricsRegistry.Snapshot snapshot = MetricsRegistry.snapshot();
        assertTrue(snapshot.jobs().isEmpty());
        assertEquals(
                0L,
                snapshot.dispatchers().get(DispatcherKind.WORKER_DECISION).attempts());
    }
}

package io.github.janguenter.atmons.cobbleworkers;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Process-local diagnostics. No metrics are persisted into world or entity
 * data.
 */
public final class MetricsRegistry {
    private static final Map<DispatcherKind, DispatcherCounters> DISPATCHERS =
            new EnumMap<>(DispatcherKind.class);
    private static final Map<String, JobCounters> JOBS = new ConcurrentHashMap<>();

    static {
        for (DispatcherKind kind : DispatcherKind.values()) {
            DISPATCHERS.put(kind, new DispatcherCounters());
        }
    }

    private MetricsRegistry() {
    }

    public static long dispatcherStarted(DispatcherKind kind) {
        DispatcherCounters counters = DISPATCHERS.get(kind);
        counters.attempts.increment();
        counters.executions.increment();
        return System.nanoTime();
    }

    public static void dispatcherSkipped(DispatcherKind kind) {
        DispatcherCounters counters = DISPATCHERS.get(kind);
        counters.attempts.increment();
        counters.skips.increment();
    }

    public static void dispatcherFinished(DispatcherKind kind, long startedAt) {
        DISPATCHERS.get(kind).nanoseconds.add(System.nanoTime() - startedAt);
    }

    public static void recordEligibility(
            String job,
            long nanoseconds,
            boolean eligible,
            boolean failed) {
        JobCounters counters = JOBS.computeIfAbsent(job, ignored -> new JobCounters());
        counters.eligibilityChecks.increment();
        counters.eligibilityNanoseconds.add(nanoseconds);
        if (eligible) {
            counters.eligibleChecks.increment();
        }
        if (failed) {
            counters.failures.increment();
        }
    }

    public static void recordDecision(String job, long nanoseconds, boolean failed) {
        JobCounters counters = JOBS.computeIfAbsent(job, ignored -> new JobCounters());
        counters.decisions.increment();
        counters.decisionNanoseconds.add(nanoseconds);
        if (failed) {
            counters.failures.increment();
        }
    }

    public static void recordTargetSet(String job, int size) {
        JobCounters counters = JOBS.computeIfAbsent(job, ignored -> new JobCounters());
        counters.targetSetSamples.increment();
        counters.targetSetTotal.add(size);
        counters.targetSetMaximum.accumulateAndGet(size, Math::max);
    }

    public static Snapshot snapshot() {
        Map<DispatcherKind, DispatcherSnapshot> dispatchers =
                new EnumMap<>(DispatcherKind.class);
        DISPATCHERS.forEach((kind, counters) -> dispatchers.put(kind, counters.snapshot()));
        List<JobSnapshot> jobs = JOBS.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .sorted(Comparator.comparing(JobSnapshot::job))
                .toList();
        return new Snapshot(Map.copyOf(dispatchers), jobs);
    }

    public static void reset() {
        DISPATCHERS.values().forEach(DispatcherCounters::reset);
        JOBS.clear();
    }

    public enum DispatcherKind {
        AREA_SCAN("area scan"),
        WORKER_DECISION("worker decision");

        private final String label;

        DispatcherKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record DispatcherSnapshot(
            long attempts,
            long executions,
            long skips,
            long nanoseconds) {
    }

    public record JobSnapshot(
            String job,
            long eligibilityChecks,
            long eligibleChecks,
            long eligibilityNanoseconds,
            long decisions,
            long decisionNanoseconds,
            long failures,
            long targetSetSamples,
            long targetSetTotal,
            long targetSetMaximum) {
    }

    public record Snapshot(
            Map<DispatcherKind, DispatcherSnapshot> dispatchers,
            List<JobSnapshot> jobs) {

        public String format() {
            StringBuilder output = new StringBuilder("Cobbleworkers optimizer metrics");
            for (DispatcherKind kind : DispatcherKind.values()) {
                DispatcherSnapshot value = dispatchers.get(kind);
                output.append('\n')
                        .append(kind.label())
                        .append(": attempts=")
                        .append(value.attempts())
                        .append(", executions=")
                        .append(value.executions())
                        .append(", skips=")
                        .append(value.skips())
                        .append(", totalMs=")
                        .append(toMilliseconds(value.nanoseconds()));
            }
            for (JobSnapshot job : jobs) {
                output.append('\n')
                        .append(job.job())
                        .append(": checks=")
                        .append(job.eligibilityChecks())
                        .append(", eligible=")
                        .append(job.eligibleChecks())
                        .append(", decisions=")
                        .append(job.decisions())
                        .append(", failures=")
                        .append(job.failures())
                        .append(", eligibilityMs=")
                        .append(toMilliseconds(job.eligibilityNanoseconds()))
                        .append(", decisionMs=")
                        .append(toMilliseconds(job.decisionNanoseconds()))
                        .append(", targetSamples=")
                        .append(job.targetSetSamples())
                        .append(", targetAverage=")
                        .append(average(job.targetSetTotal(), job.targetSetSamples()))
                        .append(", targetMax=")
                        .append(job.targetSetMaximum());
            }
            return output.toString();
        }

        private static String average(long total, long count) {
            if (count == 0) {
                return "0.00";
            }
            return String.format(Locale.ROOT, "%.2f", (double) total / count);
        }

        private static String toMilliseconds(long nanoseconds) {
            return String.format(Locale.ROOT, "%.3f", nanoseconds / 1_000_000.0);
        }
    }

    private static final class DispatcherCounters {
        private final LongAdder attempts = new LongAdder();
        private final LongAdder executions = new LongAdder();
        private final LongAdder skips = new LongAdder();
        private final LongAdder nanoseconds = new LongAdder();

        private DispatcherSnapshot snapshot() {
            return new DispatcherSnapshot(
                    attempts.sum(),
                    executions.sum(),
                    skips.sum(),
                    nanoseconds.sum());
        }

        private void reset() {
            attempts.reset();
            executions.reset();
            skips.reset();
            nanoseconds.reset();
        }
    }

    private static final class JobCounters {
        private final LongAdder eligibilityChecks = new LongAdder();
        private final LongAdder eligibleChecks = new LongAdder();
        private final LongAdder eligibilityNanoseconds = new LongAdder();
        private final LongAdder decisions = new LongAdder();
        private final LongAdder decisionNanoseconds = new LongAdder();
        private final LongAdder failures = new LongAdder();
        private final LongAdder targetSetSamples = new LongAdder();
        private final LongAdder targetSetTotal = new LongAdder();
        private final AtomicLong targetSetMaximum = new AtomicLong();

        private JobSnapshot snapshot(String job) {
            return new JobSnapshot(
                    job,
                    eligibilityChecks.sum(),
                    eligibleChecks.sum(),
                    eligibilityNanoseconds.sum(),
                    decisions.sum(),
                    decisionNanoseconds.sum(),
                    failures.sum(),
                    targetSetSamples.sum(),
                    targetSetTotal.sum(),
                    targetSetMaximum.get());
        }
    }
}

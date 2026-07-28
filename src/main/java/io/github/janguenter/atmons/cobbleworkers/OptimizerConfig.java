package io.github.janguenter.atmons.cobbleworkers;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common server configuration. All behavioral features have a stock-mode
 * switch and interval one also restores stock scheduling.
 */
public final class OptimizerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_WORKER_DECISION_THROTTLING;
    public static final ModConfigSpec.IntValue WORKER_DECISION_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue ENABLE_AREA_SCAN_THROTTLING;
    public static final ModConfigSpec.IntValue AREA_SCAN_DISPATCH_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue ENABLE_DETERMINISTIC_STAGGERING;
    public static final ModConfigSpec.BooleanValue ENABLE_INSTRUMENTATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("scheduling");
        ENABLE_WORKER_DECISION_THROTTLING = builder
                .comment("Throttle Cobbleworkers high-level worker decisions.")
                .define("enableWorkerDecisionThrottling", true);
        WORKER_DECISION_INTERVAL_TICKS = builder
                .comment("Ticks between high-level decisions. Set to 1 for stock behavior.")
                .defineInRange("workerDecisionIntervalTicks", 5, 1, 1200);
        ENABLE_AREA_SCAN_THROTTLING = builder
                .comment("Throttle dispatch of each pasture's deferred area scanner.")
                .define("enableAreaScanDispatchThrottling", true);
        AREA_SCAN_DISPATCH_INTERVAL_TICKS = builder
                .comment("Ticks between area-scan dispatches. Set to 1 for stock behavior.")
                .defineInRange("areaScanDispatchIntervalTicks", 5, 1, 1200);
        ENABLE_DETERMINISTIC_STAGGERING = builder
                .comment("Spread work by dimension, pasture, and Pokemon instead of aligning ticks.")
                .define("staggerByPokemonAndPasture", true);
        builder.pop();

        builder.push("diagnostics");
        ENABLE_INSTRUMENTATION = builder
                .comment("Collect in-memory dispatcher, job, and target-set timing counters.")
                .define("enableInstrumentation", true);
        builder.pop();

        SPEC = builder.build();
    }

    private OptimizerConfig() {
    }

    public static int workerDecisionInterval() {
        if (!ENABLE_WORKER_DECISION_THROTTLING.get()) {
            return 1;
        }
        return WORKER_DECISION_INTERVAL_TICKS.get();
    }

    public static int areaScanInterval() {
        if (!ENABLE_AREA_SCAN_THROTTLING.get()) {
            return 1;
        }
        return AREA_SCAN_DISPATCH_INTERVAL_TICKS.get();
    }
}

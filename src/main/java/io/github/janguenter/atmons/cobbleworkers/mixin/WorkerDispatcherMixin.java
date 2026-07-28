package io.github.janguenter.atmons.cobbleworkers.mixin;

import accieo.cobbleworkers.interfaces.Worker;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.github.janguenter.atmons.cobbleworkers.DecisionSchedule;
import io.github.janguenter.atmons.cobbleworkers.MetricsRegistry;
import io.github.janguenter.atmons.cobbleworkers.MetricsRegistry.DispatcherKind;
import io.github.janguenter.atmons.cobbleworkers.OptimizerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps, but does not replace, the final dispatcher assembled by Cobbleworkers
 * and All The Mons core.
 */
@Mixin(targets = "accieo.cobbleworkers.jobs.WorkerDispatcher", remap = false)
public abstract class WorkerDispatcherMixin {
    @Unique
    private static final ThreadLocal<Long> atmonsOptimizer$areaStarted = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<Long> atmonsOptimizer$workerStarted = new ThreadLocal<>();

    @Inject(
            method = "tickAreaScan(Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void atmonsOptimizer$scheduleAreaScan(
            Level level,
            BlockPos pasture,
            CallbackInfo callback) {
        int interval = OptimizerConfig.areaScanInterval();
        boolean run = DecisionSchedule.shouldRunArea(
                level.getGameTime(),
                interval,
                OptimizerConfig.ENABLE_DETERMINISTIC_STAGGERING.get(),
                level.dimension().location().toString(),
                pasture.asLong());
        if (!run) {
            if (OptimizerConfig.ENABLE_INSTRUMENTATION.get()) {
                MetricsRegistry.dispatcherSkipped(DispatcherKind.AREA_SCAN);
            }
            callback.cancel();
            return;
        }
        if (OptimizerConfig.ENABLE_INSTRUMENTATION.get()) {
            atmonsOptimizer$areaStarted.set(
                    MetricsRegistry.dispatcherStarted(DispatcherKind.AREA_SCAN));
        }
    }

    @Inject(
            method = "tickAreaScan(Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;)V",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private void atmonsOptimizer$finishAreaScan(
            Level level,
            BlockPos pasture,
            CallbackInfo callback) {
        Long started = atmonsOptimizer$areaStarted.get();
        if (started != null) {
            atmonsOptimizer$areaStarted.remove();
            MetricsRegistry.dispatcherFinished(DispatcherKind.AREA_SCAN, started);
        }
    }

    @Inject(
            method = "tickPokemon(Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void atmonsOptimizer$scheduleWorkerDecision(
            Level level,
            BlockPos pasture,
            PokemonEntity pokemonEntity,
            CallbackInfo callback) {
        int interval = OptimizerConfig.workerDecisionInterval();
        boolean run = DecisionSchedule.shouldRunWorker(
                level.getGameTime(),
                interval,
                OptimizerConfig.ENABLE_DETERMINISTIC_STAGGERING.get(),
                level.dimension().location().toString(),
                pasture.asLong(),
                pokemonEntity.getPokemon().getUuid());
        if (!run) {
            if (OptimizerConfig.ENABLE_INSTRUMENTATION.get()) {
                MetricsRegistry.dispatcherSkipped(DispatcherKind.WORKER_DECISION);
            }
            callback.cancel();
            return;
        }
        if (OptimizerConfig.ENABLE_INSTRUMENTATION.get()) {
            atmonsOptimizer$workerStarted.set(
                    MetricsRegistry.dispatcherStarted(DispatcherKind.WORKER_DECISION));
        }
    }

    @Inject(
            method = "tickPokemon(Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private void atmonsOptimizer$finishWorkerDecision(
            Level level,
            BlockPos pasture,
            PokemonEntity pokemonEntity,
            CallbackInfo callback) {
        Long started = atmonsOptimizer$workerStarted.get();
        if (started != null) {
            atmonsOptimizer$workerStarted.remove();
            MetricsRegistry.dispatcherFinished(DispatcherKind.WORKER_DECISION, started);
        }
    }

    @Redirect(
            method = "tickPokemon(Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Laccieo/cobbleworkers/interfaces/Worker;"
                            + "shouldRun(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)Z"
            ),
            require = 1,
            remap = false
    )
    private boolean atmonsOptimizer$timeEligibility(
            Worker worker,
            PokemonEntity pokemonEntity) {
        if (!OptimizerConfig.ENABLE_INSTRUMENTATION.get()) {
            return worker.shouldRun(pokemonEntity);
        }
        String job = worker.getJobType().name();
        long started = System.nanoTime();
        try {
            boolean eligible = worker.shouldRun(pokemonEntity);
            MetricsRegistry.recordEligibility(
                    job,
                    System.nanoTime() - started,
                    eligible,
                    false);
            return eligible;
        } catch (RuntimeException exception) {
            MetricsRegistry.recordEligibility(
                    job,
                    System.nanoTime() - started,
                    false,
                    true);
            throw exception;
        }
    }

    @Redirect(
            method = "tickPokemon(Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Laccieo/cobbleworkers/interfaces/Worker;"
                            + "tick(Lnet/minecraft/world/level/Level;"
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V"
            ),
            require = 1,
            remap = false
    )
    private void atmonsOptimizer$timeDecision(
            Worker worker,
            Level level,
            BlockPos pasture,
            PokemonEntity pokemonEntity) {
        if (!OptimizerConfig.ENABLE_INSTRUMENTATION.get()) {
            worker.tick(level, pasture, pokemonEntity);
            return;
        }
        String job = worker.getJobType().name();
        long started = System.nanoTime();
        try {
            worker.tick(level, pasture, pokemonEntity);
            MetricsRegistry.recordDecision(job, System.nanoTime() - started, false);
        } catch (RuntimeException exception) {
            MetricsRegistry.recordDecision(job, System.nanoTime() - started, true);
            throw exception;
        }
    }
}

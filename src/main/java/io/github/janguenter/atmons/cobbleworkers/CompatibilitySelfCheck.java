package io.github.janguenter.atmons.cobbleworkers;

import accieo.cobbleworkers.cache.CobbleworkersCacheManager;
import accieo.cobbleworkers.enums.JobType;
import accieo.cobbleworkers.interfaces.Worker;
import accieo.cobbleworkers.jobs.WorkerDispatcher;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Runtime verification for the exact dispatcher contract and the three worker
 * implementations injected by All The Mons core 0.3.0.
 */
public final class CompatibilitySelfCheck {
    private static final Set<String> REQUIRED_PACK_WORKERS = Set.of(
            "net.allthemods.allthemons.compat.cobbleworkers.EggHandler",
            "net.allthemods.allthemons.compat.cobbleworkers.EggHatcher",
            "net.allthemods.allthemons.compat.cobbleworkers.HeartRipper");
    private static final Set<String> REQUIRED_PACK_JOB_TYPES = Set.of(
            "EGG_HANDLER",
            "EGG_HATCHER",
            "HEART_RIPPER");

    private CompatibilitySelfCheck() {
    }

    /**
     * Verifies signatures and the final composed worker registry.
     *
     * @return the number of workers in the composed registry
     */
    public static int verify() {
        try {
            Method areaScan = WorkerDispatcher.class.getDeclaredMethod(
                    "tickAreaScan",
                    Level.class,
                    BlockPos.class);
            requirePublicFinalVoid(areaScan);

            Method workerTick = WorkerDispatcher.class.getDeclaredMethod(
                    "tickPokemon",
                    Level.class,
                    BlockPos.class,
                    PokemonEntity.class);
            requirePublicFinalVoid(workerTick);

            Method eligibility = Worker.class.getDeclaredMethod(
                    "shouldRun",
                    PokemonEntity.class);
            if (eligibility.getReturnType() != boolean.class) {
                throw new IllegalStateException("Worker.shouldRun no longer returns boolean");
            }

            Method decision = Worker.class.getDeclaredMethod(
                    "tick",
                    Level.class,
                    BlockPos.class,
                    PokemonEntity.class);
            if (decision.getReturnType() != void.class) {
                throw new IllegalStateException("Worker.tick no longer returns void");
            }

            Method getTargets = CobbleworkersCacheManager.class.getDeclaredMethod(
                    "getTargets",
                    BlockPos.class,
                    JobType.class);
            if (!Set.class.isAssignableFrom(getTargets.getReturnType())) {
                throw new IllegalStateException(
                        "CobbleworkersCacheManager.getTargets no longer returns Set");
            }

            Field workersField = WorkerDispatcher.class.getDeclaredField("workers");
            int modifiers = workersField.getModifiers();
            if (!Modifier.isPrivate(modifiers)
                    || !Modifier.isStatic(modifiers)
                    || !Modifier.isFinal(modifiers)
                    || !List.class.isAssignableFrom(workersField.getType())) {
                throw new IllegalStateException("WorkerDispatcher.workers shape changed");
            }
            if (!workersField.trySetAccessible()) {
                throw new IllegalStateException("Cannot inspect WorkerDispatcher.workers");
            }

            Object value = workersField.get(null);
            if (!(value instanceof List<?> workers)) {
                throw new IllegalStateException("WorkerDispatcher.workers is not a List");
            }
            if (workers.size() < 25 || workers.stream().anyMatch(worker -> !(worker instanceof Worker))) {
                throw new IllegalStateException(
                        "Expected at least 25 valid workers after All The Mons composition");
            }

            Set<String> classNames = workers.stream()
                    .map(worker -> worker.getClass().getName())
                    .collect(Collectors.toUnmodifiableSet());
            if (!classNames.containsAll(REQUIRED_PACK_WORKERS)) {
                Set<String> missing = REQUIRED_PACK_WORKERS.stream()
                        .filter(required -> !classNames.contains(required))
                        .collect(Collectors.toUnmodifiableSet());
                throw new IllegalStateException("Missing All The Mons workers: " + missing);
            }

            Set<String> jobTypes = workers.stream()
                    .map(Worker.class::cast)
                    .map(Worker::getJobType)
                    .map(JobType::name)
                    .collect(Collectors.toUnmodifiableSet());
            if (!jobTypes.containsAll(REQUIRED_PACK_JOB_TYPES)) {
                throw new IllegalStateException(
                        "Missing All The Mons job types: " + REQUIRED_PACK_JOB_TYPES);
            }
            return workers.size();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Cobbleworkers 1.8.0 dispatcher compatibility check failed",
                    exception);
        }
    }

    private static void requirePublicFinalVoid(Method method) {
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers)
                || !Modifier.isFinal(modifiers)
                || method.getReturnType() != void.class) {
            throw new IllegalStateException(method + " no longer has the expected shape");
        }
    }
}

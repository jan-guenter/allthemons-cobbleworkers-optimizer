package io.github.janguenter.atmons.cobbleworkers.mixin;

import accieo.cobbleworkers.enums.JobType;
import io.github.janguenter.atmons.cobbleworkers.MetricsRegistry;
import io.github.janguenter.atmons.cobbleworkers.OptimizerConfig;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Read-only target-set sampling for all upstream and pack-added jobs.
 */
@Mixin(targets = "accieo.cobbleworkers.cache.CobbleworkersCacheManager", remap = false)
public abstract class CobbleworkersCacheManagerMixin {
    @Inject(
            method = "getTargets(Lnet/minecraft/core/BlockPos;"
                    + "Laccieo/cobbleworkers/enums/JobType;)Ljava/util/Set;",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private void atmonsOptimizer$sampleTargetSet(
            BlockPos pasture,
            JobType jobType,
            CallbackInfoReturnable<Set<BlockPos>> callback) {
        if (OptimizerConfig.ENABLE_INSTRUMENTATION.get()) {
            Set<BlockPos> targets = callback.getReturnValue();
            MetricsRegistry.recordTargetSet(jobType.name(), targets.size());
        }
    }
}

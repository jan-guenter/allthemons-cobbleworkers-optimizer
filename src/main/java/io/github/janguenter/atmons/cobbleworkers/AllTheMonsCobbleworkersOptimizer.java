package io.github.janguenter.atmons.cobbleworkers;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server entry point.
 */
@Mod(
        value = AllTheMonsCobbleworkersOptimizer.MOD_ID,
        dist = Dist.DEDICATED_SERVER
)
public final class AllTheMonsCobbleworkersOptimizer {
    public static final String MOD_ID = "atmons_cobbleworkers_optimizer";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AllTheMonsCobbleworkersOptimizer.class);

    public AllTheMonsCobbleworkersOptimizer(ModContainer container) {
        container.registerConfig(
                ModConfig.Type.COMMON,
                OptimizerConfig.SPEC,
                "atmons_cobbleworkers_optimizer-common.toml");
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onServerStarting(ServerStartingEvent event) {
        int workers = CompatibilitySelfCheck.verify();
        LOGGER.info(
                "Compatibility self-check passed with {} composed Cobbleworkers jobs",
                workers);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("atmons_cobbleworkers_optimizer")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("stats")
                                .executes(AllTheMonsCobbleworkersOptimizer::showStats)
                                .then(Commands.literal("reset")
                                        .executes(AllTheMonsCobbleworkersOptimizer::resetStats))));
    }

    private static int showStats(CommandContext<CommandSourceStack> context) {
        String output = MetricsRegistry.snapshot().format();
        context.getSource().sendSuccess(() -> Component.literal(output), false);
        return 1;
    }

    private static int resetStats(CommandContext<CommandSourceStack> context) {
        MetricsRegistry.reset();
        context.getSource().sendSuccess(
                () -> Component.literal("Cobbleworkers optimizer metrics reset"),
                false);
        return 1;
    }
}

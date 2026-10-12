package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

import java.util.function.Predicate;

/** Client-local counters; no per-frame logging, timing calls or server packets. */
public final class ClientRenderDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static long heldGpu, heldBuffered, heldLive;
    private static long heldPoseReuses, heldPoseEvaluations;
    private static long heldSelectiveFlushes, heldFullFlushes;
    private ClientRenderDiagnostics() {}

    static void heldGpu() { heldGpu++; }
    static void heldBuffered() { heldBuffered++; }
    static void heldLive() { heldLive++; }
    public static void heldPoseReused() { heldPoseReuses++; }
    public static void heldPoseEvaluated() { heldPoseEvaluations++; }
    static void heldFlush(boolean selective) { if (selective) heldSelectiveFlushes++; else heldFullFlushes++; }

    /** Called by the main mod entry point only on a physical client. */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ClientRenderDiagnostics::registerCommands);
        LOGGER.debug("CS2 render diagnostic command listener installed");
    }

    public static void registerCommands(RegisterClientCommandsEvent event) {
        registerCommandTree(event.getDispatcher(), source -> source.getEntity() instanceof Player player && player.isCreative());
        LOGGER.debug("Registered /cs2lootbox client render diagnostics (Creative mode required to execute)");
    }

    static void registerCommandTree(CommandDispatcher<CommandSourceStack> dispatcher, Predicate<CommandSourceStack> creativePlayer) {
        // Forge merges suggestions before player-info packets establish the local
        // game mode. A .requires check here permanently removes this command from
        // that tree at login. Keep it discoverable and check each execution instead.
        dispatcher.register(Commands.literal("cs2lootbox")
                .executes(creativeOnly(creativePlayer, context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "CS2 render diagnostics: /cs2lootbox renderstats [reset]"), false);
                    return 1;
                }))
                .then(Commands.literal("renderstats")
                        .executes(creativeOnly(creativePlayer, context -> {
                            var cache = HeldCaseMeshCache.statistics();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "CS2 held cases: GPU cache draws=" + heldGpu + ", buffered draws=" + heldBuffered
                                            + ", live geometry draws=" + heldLive
                                            + ", pose reuse=" + heldPoseReuses + ", pose evaluations=" + heldPoseEvaluations
                                            + ", selective flushes=" + heldSelectiveFlushes + ", full flushes=" + heldFullFlushes), false);
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "CS2 GPU cache (all contexts): uploads=" + cache.uploads() + ", hits=" + cache.hits()
                                            + ", evictions=" + cache.evictions() + ", entries=" + cache.entries()), false);
                            var scratch = RenderBufferPool.statistics();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "CS2 native scratch buffers: created=" + scratch.created()
                                            + ", pooled=" + scratch.available() + ", in use=" + scratch.borrowed()
                                            + ", peak concurrent=" + scratch.peakBorrowed()), false);
                            return 1;
                        }))
                        .then(Commands.literal("reset").executes(creativeOnly(creativePlayer, context -> {
                            heldGpu = heldBuffered = heldLive = 0;
                            heldPoseReuses = heldPoseEvaluations = 0;
                            heldSelectiveFlushes = heldFullFlushes = 0;
                            HeldCaseMeshCache.resetStatistics();
                            context.getSource().sendSuccess(() -> Component.literal("CS2 render counters reset; cached meshes retained."), false);
                            return 1;
                        })))));
    }

    private static Command<CommandSourceStack> creativeOnly(Predicate<CommandSourceStack> creativePlayer, Command<CommandSourceStack> command) {
        return context -> {
            if (!creativePlayer.test(context.getSource())) {
                context.getSource().sendFailure(Component.literal("CS2 render diagnostics require Creative mode."));
                return 0;
            }
            return command.run(context);
        };
    }
}

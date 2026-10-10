package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TaskChainer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.command.CommandHelper;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Supplier;

/** Reproduces Forge's login-time suggestion merge before the game mode is known. */
public final class RenderCommandCheck {
    public static void main(String[] args) throws Exception {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var source = new TestSource();
        ClientRenderDiagnostics.registerCommandTree(dispatcher, candidate -> ((TestSource) candidate).creative);

        // Login has not received player-info yet: Creative mode currently reads false.
        var suggestions = new CommandDispatcher<CommandSourceStack>();
        CommandHelper.mergeCommandNode(dispatcher.getRoot(), suggestions.getRoot(), new IdentityHashMap<>(),
                source, context -> 0, provider -> provider);
        require(suggestions.getRoot().getChild("cs2lootbox") != null, "Root disappeared during login");
        require(suggestions.getRoot().getChild("cs2lootbox").getChild("renderstats").getChild("reset") != null,
                "Diagnostic subcommands disappeared during login");

        String[] commands = {"cs2lootbox", "cs2lootbox renderstats", "cs2lootbox renderstats reset"};
        for (String command : commands) require(dispatcher.execute(command, source) == 0, "Non-Creative access allowed: " + command);
        require(source.successes.isEmpty() && source.failures.size() == 3, "Denied commands must only report the Creative requirement");

        source.creative = true; // Player-info arrives without rebuilding the command tree.
        for (String command : commands) require(dispatcher.execute(command, source) == 1, "Creative command failed: " + command);
        require(source.successes.size() == 4, "Usage, two statistics lines and reset message expected");
        require(source.successes.get(1).startsWith("CS2 held cases:"), "Held counters missing");
        require(source.successes.get(2).startsWith("CS2 GPU cache"), "Cache counters missing");

        source.creative = false; // Returning to Survival/Spectator must revoke execution immediately.
        for (String command : commands) require(dispatcher.execute(command, source) == 0, "Mode change did not revoke access: " + command);
        require(source.successes.size() == 4 && source.failures.size() == 6, "Denied commands executed after leaving Creative");
        System.out.println("PASS: Forge login merge retains all commands; Creative execution works; mode changes revoke access immediately");
    }

    private static final class TestSource extends CommandSourceStack {
        boolean creative;
        final List<String> successes = new ArrayList<>(), failures = new ArrayList<>();

        TestSource() {
            super(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 0, "test", Component.literal("test"), null, null,
                    false, null, EntityAnchorArgument.Anchor.FEET, CommandSigningContext.ANONYMOUS,
                    TaskChainer.immediate(Runnable::run), result -> {});
        }

        @Override public void sendSuccess(Supplier<Component> message, boolean broadcast) { successes.add(message.get().getString()); }
        @Override public void sendFailure(Component message) { failures.add(message.getString()); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

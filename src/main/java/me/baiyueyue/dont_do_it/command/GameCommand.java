package me.baiyueyue.dont_do_it.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.baiyueyue.dont_do_it.game.GameManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * 游戏控制命令 —— /dontdoit
 *
 * 用法:
 *   /dontdoit start  —— 开始游戏
 *   /dontdoit stop   —— 强制结束
 *   /dontdoit status —— 查看状态
 */
public class GameCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(GameCommand::onRegister);
    }

    private static void onRegister(CommandDispatcher<ServerCommandSource> dispatcher,
                                    CommandRegistryAccess registryAccess,
                                    CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                literal("dontdoit")
                        .then(literal("start").executes(GameCommand::startGame))
                        .then(literal("stop").executes(GameCommand::stopGame))
                        .then(literal("status").executes(GameCommand::showStatus))
        );
    }

    private static int startGame(CommandContext<ServerCommandSource> ctx) {
        GameManager.getInstance().startGame(ctx.getSource().getServer());
        return 1;
    }

    private static int stopGame(CommandContext<ServerCommandSource> ctx) {
        GameManager gm = GameManager.getInstance();
        if (gm.isRunning()) {
            gm.endGame(ctx.getSource().getServer());
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal("§e游戏当前未在运行。"), false);
        }
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> ctx) {
        GameManager gm = GameManager.getInstance();
        String state = switch (gm.getState()) {
            case WAITING -> "§a等待中";
            case LOBBY -> "§b大厅中";
            case RUNNING -> "§e进行中";
            case ENDING -> "§6结算中";
            case ENDED -> "§c已结束";
        };
        int playerCount = gm.getAllPlayerData().size();
        ctx.getSource().sendFeedback(() ->
                Text.literal("§6[不要做挑战] §f状态: %s  §f| 参与玩家: §b%d".formatted(state, playerCount)), false);
        return 1;
    }
}

package me.baiyueyue.dont_do_it.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.baiyueyue.dont_do_it.game.GameManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * 游戏控制命令 —— /dontdoit
 *
 * 用法:
 *   /dontdoit start              —— 开始游戏
 *   /dontdoit stop               —— 强制结束
 *   /dontdoit status             —— 查看状态
 *   /dontdoit vote <玩家> true   —— 猜对加心并换词条
 *   /dontdoit vote <玩家> false  —— 猜错扣心并换词条
 *   /dontdoit skip <玩家>        —— 跳过当前词条
 *   /dontdoit setword <玩家> <词条> —— 为玩家设置指定词条（测试用）
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
                        .then(literal("vote")
                                .then(argument("player", EntityArgumentType.player())
                                        .then(argument("correct", BoolArgumentType.bool())
                                                .executes(GameCommand::votePlayer))))
                        .then(literal("skip")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(GameCommand::skipPlayer)))
                        .then(literal("setword")
                                .then(argument("player", EntityArgumentType.player())
                                        .then(argument("word", StringArgumentType.greedyString())
                                                .executes(GameCommand::setWord))))
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

    // ---- vote / skip ----

    private static int votePlayer(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        GameManager gm = GameManager.getInstance();
        if (!gm.isRunning()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§e游戏当前未在运行。"), false);
            return 0;
        }
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        boolean correct = BoolArgumentType.getBool(ctx, "correct");
        gm.voteOnPlayer(ctx.getSource().getServer(), target, correct);
        return 1;
    }

    private static int skipPlayer(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        GameManager gm = GameManager.getInstance();
        if (!gm.isRunning()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§e游戏当前未在运行。"), false);
            return 0;
        }
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        gm.skipPlayerWord(ctx.getSource().getServer(), target);
        return 1;
    }

    private static int setWord(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        GameManager gm = GameManager.getInstance();
        if (!gm.isRunning()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§e游戏当前未在运行。"), false);
            return 0;
        }
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        String wordText = StringArgumentType.getString(ctx, "word");
        boolean success = gm.setPlayerWord(ctx.getSource().getServer(), target, wordText);
        if (!success) {
            ctx.getSource().sendFeedback(() -> Text.literal("§c词条「" + wordText + "」不存在，或玩家已淘汰。"), false);
            return 0;
        }
        return 1;
    }
}

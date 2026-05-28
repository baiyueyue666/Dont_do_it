package me.baiyueyue.dont_do_it.game;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * 每人独立词条倒计时管理器 —— 每秒 tick 一次
 */
public class GameCountdownManager {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GameCountdownManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (!GameManager.getInstance().isRunning()) return;

        tickCounter++;
        // 每秒执行一次（20 ticks = 1 秒）
        if (tickCounter % 20 != 0) return;

        for (PlayerWordData data : GameManager.getInstance().getAllPlayerData()) {
            if (data.isEliminated()) continue;
            boolean expired = data.tickCountdown();
            if (expired) {
                // onWordTimerExpired 内部会 replaceWord 并重设倒计时
                GameManager.getInstance().onWordTimerExpired(server, data.getPlayerId());
            }
        }
    }
}

package me.baiyueyue.dont_do_it.game;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * 每人独立词条倒计时 + 全局特殊事件倒计时管理器 —— 每秒 tick 一次
 */
public class GameCountdownManager {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GameCountdownManager::onServerTick);
    }

    /** 每秒执行一次：词条倒计时 + 特殊事件计时 */
    private static void onServerTick(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        if (!gm.isRunning()) return;

        tickCounter++;
        // 每秒执行一次（20 ticks = 1 秒）
        if (tickCounter % 20 != 0) return;

        // ---- 每人词条倒计时 ----
        for (PlayerWordData data : gm.getAllPlayerData()) {
            if (data.isEliminated()) continue;
            boolean expired = data.tickCountdown();
            if (expired) {
                gm.onWordTimerExpired(server, data.getPlayerId());
            }
        }

        // ---- 全局特殊事件计时 ----
        gm.tickSpecialEvent(server);
    }
}

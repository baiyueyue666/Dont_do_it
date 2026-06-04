package me.baiyueyue.dont_do_it.game;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * 每人独立词条倒计时 + 全局特殊事件倒计时 + 准备阶段倒计时 —— 每秒 tick 一次
 */
public class GameCountdownManager {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GameCountdownManager::onServerTick);
    }

    /** 每秒执行一次：词条倒计时 + 特殊事件计时 + 准备阶段 */
    private static void onServerTick(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();

        // 准备阶段倒计时（即使游戏未正式运行也要处理）
        if (gm.isPrepPhase() && gm.isRunning()) {
            tickCounter++;
            if (tickCounter % 20 != 0) return;
            int remaining = gm.getPrepCountdown() - 1;
            // 通过反射无法直接设置 private，需要在 GameManager 中提供 tickPrep
            gm.tickPrepPhase(server);
            // 边界检测在准备阶段也要执行
            gm.enforceBoundary(server);
            return;
        }

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

        // ---- 游戏范围边界检测 ----
        gm.enforceBoundary(server);
    }
}

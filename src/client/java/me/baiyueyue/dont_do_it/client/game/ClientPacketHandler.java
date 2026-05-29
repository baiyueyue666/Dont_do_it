package me.baiyueyue.dont_do_it.client.game;

import me.baiyueyue.dont_do_it.game.GameState;
import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * 客户端接收服务端同步数据，更新自身状态和 BossBar
 *
 * 对手词条+血量由原版计分板 sidebar 自动同步，无需在此处理。
 */
public class ClientPacketHandler {

    public static void register() {

        // ---- 增量同步自身状态 ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.SyncOnePlayerPayload.ID,
                (payload, context) -> {
                    MinecraftClient client = context.client();
                    client.execute(() -> {
                        if (client.player != null && client.player.getUuid().equals(payload.playerId())) {
                            // 是自己：更新自身状态 + BossBar
                            GameHudRenderer.currentState = GameState.RUNNING;
                            GameHudRenderer.myHearts = payload.hearts();
                            GameHudRenderer.myEliminated = payload.eliminated();
                            GameHudRenderer.myCountdownSeconds = payload.countdownSeconds();
                            GameHudRenderer.totalTimerSeconds = payload.totalTimerSeconds();
                            GameHudRenderer.myTeamColor = payload.teamColor();

                            BossBarManager.updateHealthBar(payload.hearts(), payload.eliminated());
                            BossBarManager.updateCountdownBar(
                                    payload.countdownSeconds(), payload.totalTimerSeconds());
                        }
                        // 对手状态由原版计分板 sidebar 自动同步，无需处理
                    });
                });

        // ---- 中央通知（含标题播报） ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.NotificationPayload.ID,
                (payload, context) -> {
                    MinecraftClient client = context.client();
                    client.execute(() -> {
                        GameHudRenderer.addNotification(payload.type(), payload.message());
                        // 触发 / 淘汰 → 显示 Minecraft 标题
                        if ("trigger".equals(payload.type())) {
                            client.inGameHud.setTitle(Text.literal(payload.message()));
                        } else if ("elimination".equals(payload.type())) {
                            client.inGameHud.setTitle(Text.literal(payload.message()));
                        }
                    });
                });

        // ---- 游戏结束 ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.GameEndPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        GameHudRenderer.currentState = GameState.ENDING;
                        GameHudRenderer.addNotification("win",
                                "§6🏆 " + payload.teamName() + " §f" + payload.winnerName() + " 获胜！");

                        // 清除所有 Boss 血条
                        BossBarManager.clear();
                    });
                });

        // ---- 游戏状态重置 ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.GameStateResetPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        GameHudRenderer.currentState = GameState.WAITING;
                        GameHudRenderer.notifications.clear();
                        GameHudRenderer.myEliminated = false;
                        BossBarManager.clear();
                    });
                });
    }
}

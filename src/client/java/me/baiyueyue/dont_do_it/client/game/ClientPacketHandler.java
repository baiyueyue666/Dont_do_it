package me.baiyueyue.dont_do_it.client.game;

import me.baiyueyue.dont_do_it.game.GameState;
import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端接收服务端同步数据，更新自身状态和 BossBar
 *
 * 对手词条+血量由客户端自行渲染（从 SyncOnePlayerPayload 提取非自身数据）。
 */
public class ClientPacketHandler {

    /** 对手数据缓存：UUID → 对手词条/血量/队伍等 */
    public static final Map<UUID, GameHudRenderer.OpponentEntry> opponentMap = new ConcurrentHashMap<>();

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
                            GameHudRenderer.myMaxHearts = payload.maxHearts();
                            GameHudRenderer.myTeamColor = payload.teamColor();

                            BossBarManager.updateHealthBar(payload.hearts(), payload.eliminated(), payload.maxHearts());
                            BossBarManager.updateCountdownBar(
                                    payload.countdownSeconds(), payload.totalTimerSeconds());
                        } else {
                            // 是其他玩家：存储对手词条/血量，供 HUD 渲染
                            opponentMap.put(payload.playerId(), new GameHudRenderer.OpponentEntry(
                                    payload.playerId(), payload.teamColor(), payload.wordText(),
                                    payload.hearts(), payload.eliminated()));
                        }
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
                        opponentMap.clear();
                        BossBarManager.clear();
                    });
                });

        // ---- 特殊事件 BossBar 同步 ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.SpecialEventBossBarPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (payload.displayText().isEmpty() || payload.percent() <= 0f) {
                            BossBarManager.removeSpecialEventBar();
                            return;
                        }
                        BossBar.Color color;
                        try {
                            color = BossBar.Color.valueOf(payload.barColor());
                        } catch (IllegalArgumentException e) {
                            color = BossBar.Color.WHITE;
                        }
                        BossBarManager.updateSpecialEventBar(
                                payload.displayText(), payload.percent(), color);
                    });
                });

        // ---- 游戏范围边界 ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.GameBoundaryPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        BoundaryRenderer.setBoundary(
                                payload.minX(), payload.minZ(),
                                payload.maxX(), payload.maxZ());
                    });
                });

        // ---- 准备阶段倒计时 ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.PrepCountdownPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        int sec = payload.seconds();
                        if (sec > 0) {
                            GameHudRenderer.prepCountdown = sec;
                            GameHudRenderer.currentState = GameState.RUNNING;
                        } else {
                            GameHudRenderer.prepCountdown = -1;
                        }
                    });
                });
    }
}

package me.baiyueyue.dont_do_it.client.game;

import me.baiyueyue.dont_do_it.game.GameState;
import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端接收服务端同步数据，更新 HUD 缓存
 */
public class ClientPacketHandler {

    public static void register() {

        // ---- 全量同步对手词条 ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.SyncAllWordsPayload.ID,
                (payload, context) -> {
                    MinecraftClient client = context.client();
                    client.execute(() -> {
                        GameHudRenderer.visibleWords.clear();
                        GameHudRenderer.currentState = GameState.RUNNING;

                        List<BossBarManager.BossBarEntry> bossBarEntries = new ArrayList<>();
                        for (var entry : payload.entries()) {
                            var world = client.world;
                            String name = "?";
                            if (world != null) {
                                var player = world.getPlayerByUuid(entry.playerId());
                                if (player != null) name = player.getName().getString();
                            }
                            GameHudRenderer.visibleWords.put(entry.playerId(),
                                    new GameHudRenderer.VisibleWord(name, entry.teamColor(),
                                            entry.wordText(), entry.hearts(),
                                            entry.eliminated(), entry.countdownSeconds()));
                            bossBarEntries.add(new BossBarManager.BossBarEntry(
                                    entry.playerId(), entry.teamColor(), entry.wordText(),
                                    entry.hearts(), entry.eliminated(), entry.countdownSeconds()));
                        }

                        // 更新 Boss 血条
                        BossBarManager.syncAllWords(bossBarEntries);
                    });
                });

        // ---- 增量同步单玩家 ----
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.SyncOnePlayerPayload.ID,
                (payload, context) -> {
                    MinecraftClient client = context.client();
                    client.execute(() -> {
                        if (client.player != null && client.player.getUuid().equals(payload.playerId())) {
                            // 是自己：更新自身状态
                            GameHudRenderer.myHearts = payload.hearts();
                            GameHudRenderer.myEliminated = payload.eliminated();
                            GameHudRenderer.myCountdownSeconds = payload.countdownSeconds();
                            GameHudRenderer.myTeamColor = payload.teamColor();
                        } else {
                            // 是对手：更新可视词条
                            var vw = GameHudRenderer.visibleWords.get(payload.playerId());
                            String name = vw != null ? vw.playerName() : "?";
                            GameHudRenderer.visibleWords.put(payload.playerId(),
                                    new GameHudRenderer.VisibleWord(name, payload.teamColor(),
                                            payload.wordText(), payload.hearts(),
                                            payload.eliminated(), payload.countdownSeconds()));

                            // 更新单个 Boss 血条
                            BossBarManager.syncOnePlayer(payload.playerId(), payload.teamColor(),
                                    payload.wordText(), payload.hearts(),
                                    payload.eliminated(), payload.countdownSeconds());
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
    }
}

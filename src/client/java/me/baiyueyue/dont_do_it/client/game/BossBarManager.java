package me.baiyueyue.dont_do_it.client.game;

import me.baiyueyue.dont_do_it.game.GameSettings;
import me.baiyueyue.dont_do_it.game.TeamColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;

import java.util.*;

/**
 * 客户端 Boss 血条管理器 —— 用 BossBar 实时显示对手词条
 *
 * 每个对手玩家拥有一个 BossBar：
 * - 名称：{玩家名}: {词条文本}
 * - 颜色：对应队伍颜色
 * - 进度：剩余生命值百分比（hearts / DEFAULT_HEARTS）
 * - 淘汰后变灰并显示划掉效果
 */
public class BossBarManager {

    /** playerId → BossBar */
    private static final Map<UUID, ClientBossBar> bossBars = new LinkedHashMap<>();

    // ==================== 全量同步 ====================

    /**
     * 收到 SyncAllWords 时调用 —— 批量创建/更新所有对手的 BossBar
     */
    public static void syncAllWords(List<BossBarEntry> entries) {
        MinecraftClient client = MinecraftClient.getInstance();
        Set<UUID> currentIds = new HashSet<>();

        for (BossBarEntry entry : entries) {
            currentIds.add(entry.playerId());
            String name = resolvePlayerName(client, entry.playerId());
            updateOrCreateBossBar(client, entry.playerId(), name, entry);
        }

        // 移除不在当前列表中的 BossBar（有玩家退出等）
        bossBars.keySet().removeIf(id -> {
            if (!currentIds.contains(id)) {
                removeBossBar(client, id);
                return true;
            }
            return false;
        });
    }

    // ==================== 增量同步 ====================

    /**
     * 收到 SyncOnePlayer 时调用 —— 更新单个对手的 BossBar
     */
    public static void syncOnePlayer(UUID playerId, String teamColorName,
                                      String wordText, int hearts, boolean eliminated, int countdownSeconds) {
        MinecraftClient client = MinecraftClient.getInstance();
        String name = resolvePlayerName(client, playerId);

        BossBarEntry entry = new BossBarEntry(playerId, teamColorName, wordText, hearts,
                eliminated, countdownSeconds);
        updateOrCreateBossBar(client, playerId, name, entry);
    }

    // ==================== 清理 ====================

    /**
     * 游戏结束或状态重置时调用 —— 移除所有 BossBar
     */
    public static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        for (UUID id : new ArrayList<>(bossBars.keySet())) {
            removeBossBar(client, id);
        }
        bossBars.clear();
    }

    // ==================== 内部方法 ====================

    private static void updateOrCreateBossBar(MinecraftClient client, UUID playerId,
                                               String playerName, BossBarEntry entry) {
        TeamColor teamColor;
        try {
            teamColor = TeamColor.valueOf(entry.teamColorName());
        } catch (IllegalArgumentException e) {
            teamColor = TeamColor.RED; // fallback
        }

        BossBar.Color color = teamColor.getBossBarColor();
        float percent = entry.eliminated()
                ? 0f
                : Math.max(0f, Math.min(1f, (float) entry.hearts() / GameSettings.DEFAULT_HEARTS));

        // 组装显示文本
        String teamPrefix = teamColor.getDisplayName();
        String wordDisplay = entry.eliminated()
                ? "§8§m" + entry.wordText()
                : entry.wordText();
        Text displayText = Text.literal(teamPrefix + " §r" + playerName + ": " + wordDisplay
                + " §c❤×" + entry.hearts());

        // 先移除旧 BossBar（ClientBossBar 名称不可变，需重建）
        ClientBossBar existing = bossBars.remove(playerId);
        if (existing != null) {
            client.inGameHud.getBossBarHud().bossBars.remove(playerId);
        }

        // 创建新 BossBar 并注册到 HUD
        ClientBossBar bar = new ClientBossBar(playerId, displayText, percent,
                color, BossBar.Style.PROGRESS, false, false, false);
        bossBars.put(playerId, bar);
        client.inGameHud.getBossBarHud().bossBars.put(playerId, bar);
    }

    private static void removeBossBar(MinecraftClient client, UUID playerId) {
        client.inGameHud.getBossBarHud().bossBars.remove(playerId);
    }

    /** 从客户端世界解析玩家名 */
    private static String resolvePlayerName(MinecraftClient client, UUID playerId) {
        if (client.world != null) {
            var player = client.world.getPlayerByUuid(playerId);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return "?";
    }

    // ==================== 数据载体 ====================

    public record BossBarEntry(UUID playerId, String teamColorName, String wordText,
                                int hearts, boolean eliminated, int countdownSeconds) {}
}

package me.baiyueyue.dont_do_it.client.game;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * 客户端 Boss 血条管理器 —— 显示自己的两项信息
 *
 * 两条固定的 BossBar：
 * - 血量条：显示自己剩余生命值 (❤ hearts/DEFAULT_HEARTS)
 * - 倒计时条：显示词条更换倒计时 (⏰ countdown/totalTimer)
 */
public class BossBarManager {

    /** 血量条 UUID */
    private static final UUID HEALTH_BAR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    /** 倒计时条 UUID */
    private static final UUID COUNTDOWN_BAR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    /** 特殊事件条 UUID */
    private static final UUID SPECIAL_EVENT_BAR_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private static ClientBossBar healthBar;
    private static ClientBossBar countdownBar;
    private static ClientBossBar specialEventBar;
    private static boolean initialized = false;

    // ==================== 初始化 ====================

    /**
     * 客户端启动时调用，创建两条 BossBar 但不添加到 HUD（等游戏开始时再添加）
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        // BossBar 将在 update 时按需创建并添加到 HUD
    }

    // ==================== 更新血量条 ====================

    /**
     * 更新自己的血量 BossBar
     * @param hearts     剩余生命值
     * @param eliminated 是否已淘汰
     * @param maxHearts  血量上限
     */
    public static void updateHealthBar(int hearts, boolean eliminated, int maxHearts) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;

        float percent = eliminated
                ? 0f
                : Math.max(0f, Math.min(1f, (float) hearts / maxHearts));

        String displayStr;
        if (eliminated) {
            displayStr = "§c§m❤ 已淘汰";
        } else {
            displayStr = "§c❤ 剩余生命值: " + hearts + "/" + maxHearts;
        }
        Text displayText = Text.literal(displayStr);

        healthBar = replaceBossBar(client, HEALTH_BAR_ID, healthBar, displayText, percent,
                BossBar.Color.RED, BossBar.Style.NOTCHED_10);
    }

    // ==================== 更新特殊事件条 ====================

    /**
     * 更新特殊事件 BossBar（共用一个血条：显示倒计时或持续时间）
     * @param displayText     显示文本
     * @param percent         进度 0~1
     * @param color           血条颜色
     */
    public static void updateSpecialEventBar(String displayText, float percent, BossBar.Color color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;

        Text text = Text.literal(displayText);
        specialEventBar = replaceBossBar(client, SPECIAL_EVENT_BAR_ID, specialEventBar, text, percent,
                color, BossBar.Style.NOTCHED_10);
    }

    /** 移除特殊事件条 */
    public static void removeSpecialEventBar() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;
        client.inGameHud.getBossBarHud().bossBars.remove(SPECIAL_EVENT_BAR_ID);
        specialEventBar = null;
    }

    // ==================== 更新倒计时条 ====================

    /**
     * 更新词条更换倒计时 BossBar
     * @param countdownSeconds 剩余秒数
     * @param totalSeconds     总秒数（用于计算进度）
     */
    public static void updateCountdownBar(int countdownSeconds, int totalSeconds) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;

        float percent = totalSeconds > 0
                ? Math.max(0f, Math.min(1f, (float) countdownSeconds / totalSeconds))
                : 0f;

        String displayStr = "§b⏰ 词条更换倒计时: " + countdownSeconds + "s";
        Text displayText = Text.literal(displayStr);

        countdownBar = replaceBossBar(client, COUNTDOWN_BAR_ID, countdownBar, displayText, percent,
                BossBar.Color.BLUE, BossBar.Style.NOTCHED_10);
    }

    // ==================== 清理 ====================

    /**
     * 游戏结束或状态重置时调用 —— 从 HUD 移除两条 BossBar
     */
    public static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;

        client.inGameHud.getBossBarHud().bossBars.remove(HEALTH_BAR_ID);
        client.inGameHud.getBossBarHud().bossBars.remove(COUNTDOWN_BAR_ID);
        client.inGameHud.getBossBarHud().bossBars.remove(SPECIAL_EVENT_BAR_ID);
        healthBar = null;
        countdownBar = null;
        specialEventBar = null;
    }

    // ==================== 内部方法 ====================

    /**
     * 替换 BossBar（因为 ClientBossBar 文本不可变，改名需重建）
     */
    private static ClientBossBar replaceBossBar(MinecraftClient client, UUID id,
                                                  ClientBossBar existing, Text text, float percent,
                                                  BossBar.Color color, BossBar.Style style) {
        // 移除旧条
        if (existing != null) {
            client.inGameHud.getBossBarHud().bossBars.remove(id);
        }

        ClientBossBar bar = new ClientBossBar(id, text, percent, color, style,
                false, false, false);
        client.inGameHud.getBossBarHud().bossBars.put(id, bar);
        return bar;
    }
}

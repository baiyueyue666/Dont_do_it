package me.baiyueyue.dont_do_it.client.game;

import me.baiyueyue.dont_do_it.game.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.*;

/**
 * 客户端 HUD 渲染器
 * - 经验条上方：剩余生命值 + 词条倒计时
 * - 屏幕左侧：对手词条列表（含队伍颜色）
 * - 屏幕中央：触发/淘汰通知（淡出效果）
 */
public class GameHudRenderer {

    // ---- 自己状态 ----
    public static GameState currentState = GameState.WAITING;
    public static int myHearts = 15;
    public static boolean myEliminated = false;
    public static int myCountdownSeconds = 60;
    public static String myTeamColor = "RED";

    /** 对手词条: playerId → (playerName, teamColor, wordText, hearts, eliminated, countdown) */
    public static final Map<UUID, VisibleWord> visibleWords = new LinkedHashMap<>();

    public record VisibleWord(String playerName, String teamColor, String wordText,
                               int hearts, boolean eliminated, int countdownSeconds) {}

    /** 中央通知列表 */
    public static final List<NotificationEntry> notifications = new ArrayList<>();

    public record NotificationEntry(String type, String message, long createdAtMs) {}

    // ---- TeamColor 颜色映射（名字→§代码） ----
    private static final Map<String, String> TEAM_COLOR_MAP = Map.of(
            "RED", "§c", "BLUE", "§9", "GREEN", "§a", "YELLOW", "§e",
            "PURPLE", "§d", "ORANGE", "§6", "CYAN", "§b", "PINK", "§d"
    );

    private static int tickCounter = 0;

    public static void register() {
        HudRenderCallback.EVENT.register(GameHudRenderer::onHudRender);
        // 客户端每秒 tick：递减倒计时
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (currentState != GameState.RUNNING && currentState != GameState.ENDING) return;
            tickCounter++;
            if (tickCounter % 20 != 0) return;
            // 自己倒计时
            if (!myEliminated && myCountdownSeconds > 0) {
                myCountdownSeconds--;
            }
            // 对手倒计时
            visibleWords.replaceAll((id, vw) -> {
                if (!vw.eliminated() && vw.countdownSeconds() > 0) {
                    return new VisibleWord(vw.playerName(), vw.teamColor(), vw.wordText(),
                            vw.hearts(), vw.eliminated(), vw.countdownSeconds() - 1);
                }
                return vw;
            });
        });
    }

    private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (currentState != GameState.RUNNING && currentState != GameState.ENDING) {
            // 安全清理：非游戏状态时移除所有 Boss 血条
            BossBarManager.clear();
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // ===== 1. 经验条上方：生命值 + 倒计时 =====
        renderAboveXpBar(context, client, screenWidth, screenHeight);

        // ===== 2. 左侧：对手词条 =====
        renderOpponentWords(context, client);

        // ===== 3. 中央通知 =====
        renderCenterNotifications(context, client, screenWidth);
    }

    // ==================== 经验条上方 ====================

    private static void renderAboveXpBar(DrawContext context, MinecraftClient client,
                                          int screenWidth, int screenHeight) {
        if (myEliminated) {
            Text text = Text.literal("§c💀 已淘汰");
            int textWidth = client.textRenderer.getWidth(text);
            context.drawTextWithShadow(client.textRenderer, text,
                    screenWidth / 2 - textWidth / 2, screenHeight - 52, 0xFFFFFF);
            return;
        }

        // 团队颜色前缀
        String teamPrefix = TEAM_COLOR_MAP.getOrDefault(myTeamColor, "§f");

        Text text = Text.literal("%s剩余生命值：§c%d §r| §e当前词条时长：§b%ds"
                .formatted(teamPrefix, myHearts, myCountdownSeconds));
        int textWidth = client.textRenderer.getWidth(text);
        context.drawTextWithShadow(client.textRenderer, text,
                screenWidth / 2 - textWidth / 2, screenHeight - 52, 0xFFFFFF);
    }

    // ==================== 左侧对手词条 ====================

    private static void renderOpponentWords(DrawContext context, MinecraftClient client) {
        if (visibleWords.isEmpty()) return;

        int x = 6;
        int y = 10;

        context.drawTextWithShadow(client.textRenderer,
                Text.literal("§6§l对手词条:"), x, y, 0xFFFFFF);
        y += 13;

        // 按队伍颜色排序
        List<Map.Entry<UUID, VisibleWord>> sorted = new ArrayList<>(visibleWords.entrySet());
        sorted.sort(Comparator.comparing(e -> e.getValue().teamColor()));

        for (var entry : sorted) {
            VisibleWord vw = entry.getValue();
            String teamPrefix = TEAM_COLOR_MAP.getOrDefault(vw.teamColor(), "§f");

            String line;
            if (vw.eliminated()) {
                line = "§8§m%s%s: %s".formatted(teamPrefix, vw.playerName(), vw.wordText());
                context.drawTextWithShadow(client.textRenderer, Text.literal(line), x, y, 0x666666);
            } else {
                line = "%s%s: §f%s  §c%s".formatted(teamPrefix, vw.playerName(),
                        vw.wordText(), "❤".repeat(Math.min(vw.hearts(), 15)));
                context.drawTextWithShadow(client.textRenderer, Text.literal(line), x, y, 0xFFFFFF);
            }
            y += 11;
        }
    }

    // ==================== 中央通知 ====================

    private static void renderCenterNotifications(DrawContext context, MinecraftClient client, int screenWidth) {
        long now = System.currentTimeMillis();
        // 清除超过 4 秒的通知
        notifications.removeIf(n -> (now - n.createdAtMs()) > 4000);

        int centerX = screenWidth / 2;
        int baseY = client.getWindow().getScaledHeight() / 2 - 20;

        for (int i = 0; i < notifications.size(); i++) {
            NotificationEntry n = notifications.get(i);
            long elapsed = now - n.createdAtMs();

            // 淡出效果：前1秒不透明，后3秒渐隐
            int alpha;
            if (elapsed < 1000) {
                alpha = 255;
            } else {
                alpha = (int) (255 * (1.0 - (elapsed - 1000) / 3000.0));
                alpha = Math.max(0, alpha);
            }

            int color = (alpha << 24) | 0xFFFFFF;
            Text text = Text.literal(n.message());
            int textWidth = client.textRenderer.getWidth(text);
            context.drawTextWithShadow(client.textRenderer, text,
                    centerX - textWidth / 2, baseY - i * 14, color);
        }
    }

    /** 添加中央通知 */
    public static void addNotification(String type, String message) {
        notifications.add(0, new NotificationEntry(type, message, System.currentTimeMillis()));
        // 限制最多 5 条
        if (notifications.size() > 5) {
            notifications.remove(notifications.size() - 1);
        }
    }
}

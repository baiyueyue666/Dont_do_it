package me.baiyueyue.dont_do_it.client.game;

import me.baiyueyue.dont_do_it.game.GameState;
import me.baiyueyue.dont_do_it.game.TeamColor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

/**
 * 客户端 HUD 渲染器
 * - 屏幕中央：触发/淘汰通知（淡出效果）
 * - 屏幕左侧：对手词条+血量列表
 *
 * 注意：
 * - 自己血量+倒计时 → BossBar（见 BossBarManager）
 */
public class GameHudRenderer {

    // ---- 自己状态 ----
    public static GameState currentState = GameState.WAITING;
    public static int myHearts = 15;
    public static boolean myEliminated = false;
    public static int myCountdownSeconds = 60;
    public static int totalTimerSeconds = 60;
    public static String myTeamColor = "RED";

    /** 中央通知列表 */
    public static final List<NotificationEntry> notifications = new ArrayList<>();

    public record NotificationEntry(String type, String message, long createdAtMs) {}

    /** 对手数据条目（由 ClientPacketHandler 填充） */
    public record OpponentEntry(UUID playerId, String teamColor, String wordText,
                                 int hearts, boolean eliminated) {}

    private static int tickCounter = 0;

    public static void register() {
        HudRenderCallback.EVENT.register(GameHudRenderer::onHudRender);
        // 客户端每秒 tick：递减倒计时并更新 BossBar
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (currentState != GameState.RUNNING && currentState != GameState.ENDING) return;
            tickCounter++;
            if (tickCounter % 20 != 0) return;

            // 自己倒计时
            if (!myEliminated && myCountdownSeconds > 0) {
                myCountdownSeconds--;
            }
            // 更新自己的 BossBar
            BossBarManager.updateHealthBar(myHearts, myEliminated);
            BossBarManager.updateCountdownBar(myCountdownSeconds, totalTimerSeconds);
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

        // ===== 中央通知 =====
        renderCenterNotifications(context, client, screenWidth);

        // ===== 左侧对手列表 =====
        renderOpponentList(context, client, screenWidth, screenHeight);
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
            context.drawText(client.textRenderer, text,
                    centerX - textWidth / 2, baseY - i * 14, color, true);
        }
    }

    // ==================== 左侧对手列表 ====================

    private static void renderOpponentList(DrawContext context, MinecraftClient client,
                                            int screenWidth, int screenHeight) {
        Map<UUID, OpponentEntry> opponents = ClientPacketHandler.opponentMap;
        if (opponents.isEmpty()) return;

        // 标题
        String title = "◤ 禁止事件 ◢";
        int leftX = 5;
        int y = 10;
        context.drawText(client.textRenderer, Text.literal(title), leftX, y, 0xFFDAA520, true);
        y += 14;

        // 分隔线
        int titleWidth = client.textRenderer.getWidth(title);
        context.fill(leftX, y, leftX + Math.max(titleWidth, 120), y + 1, 0x40FFFFFF);
        y += 3;

        for (OpponentEntry entry : opponents.values()) {
            // 获取玩家名称
            String playerName;
            PlayerEntity player = client.world.getPlayerByUuid(entry.playerId());
            if (player != null) {
                playerName = player.getName().getString();
            } else {
                playerName = "?";
            }

            // 队伍颜色
            int teamColor;
            try {
                teamColor = TeamColor.valueOf(entry.teamColor()).getColorRgb() | 0xFF000000;
            } catch (IllegalArgumentException e) {
                teamColor = 0xFFFFFFFF; // 回退白色
            }

            if (entry.eliminated()) {
                // 淘汰：队伍色名+词条 + 灰色 ✗
                String prefix = playerName + ": ";
                int prefixWidth = client.textRenderer.getWidth(prefix);

                String wordPart = entry.wordText();
                int wordWidth = client.textRenderer.getWidth(wordPart);

                String crossPart = " ✗";

                context.drawText(client.textRenderer, Text.literal(prefix), leftX, y, teamColor, true);
                context.drawText(client.textRenderer, Text.literal(wordPart), leftX + prefixWidth, y, teamColor, true);
                context.drawText(client.textRenderer, Text.literal(crossPart), leftX + prefixWidth + wordWidth, y, 0xFF888888, true);
            } else {
                // 正常：玩家名+词条 → 队伍色，血量 → 红色
                String prefix = playerName + ": ";
                int prefixWidth = client.textRenderer.getWidth(prefix);

                String wordPart = entry.wordText();
                int wordWidth = client.textRenderer.getWidth(wordPart);

                String heartPart = " ❤×" + entry.hearts();

                context.drawText(client.textRenderer, Text.literal(prefix), leftX, y, teamColor, true);
                context.drawText(client.textRenderer, Text.literal(wordPart), leftX + prefixWidth, y, teamColor, true);
                context.drawText(client.textRenderer, Text.literal(heartPart), leftX + prefixWidth + wordWidth, y, 0xFFFF5555, true);
            }
            y += 12;
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

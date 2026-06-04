package me.baiyueyue.dont_do_it.client.game;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * 游戏范围区块边界渲染器
 * 1.21.11: 3D 渲染 API 全面重构（RenderPipeline/GpuBuffer/MeshData），
 * 暂时使用 HUD 文字提示替代 3D 边界线渲染。
 * 服务器端 enforceBoundary 仍然生效（越界传送回中心）。
 */
public class BoundaryRenderer {

    /** 边界坐标，-1 表示未激活 */
    private static double boundMinX = -1, boundMinZ = -1, boundMaxX = -1, boundMaxZ = -1;

    public static void setBoundary(double minX, double minZ, double maxX, double maxZ) {
        boundMinX = minX;
        boundMinZ = minZ;
        boundMaxX = maxX;
        boundMaxZ = maxZ;
    }

    public static void clear() {
        boundMinX = boundMinZ = boundMaxX = boundMaxZ = -1;
    }

    public static boolean isActive() {
        return boundMinX >= 0;
    }

    /** 1.21.11: 不再使用 WorldRenderEvents，无操作 */
    public static void register() {
    }

    /**
     * 渲染边界 HUD 提示（由 GameHudRenderer 调用）
     * 当玩家接近边界时显示警告
     */
    public static void renderHud(DrawContext context, MinecraftClient client, int screenWidth, int screenHeight) {
        if (!isActive() || client.player == null) return;

        double x = client.player.getX();
        double z = client.player.getZ();
        double margin = 3.0; // 边界内 3 格以内显示警告

        boolean nearEdge = false;
        String direction = "";

        if (x - boundMinX < margin) { nearEdge = true; direction += "西"; }
        if (boundMaxX - x < margin) { nearEdge = true; direction += "东"; }
        if (z - boundMinZ < margin) { nearEdge = true; direction += "北"; }
        if (boundMaxZ - z < margin) { nearEdge = true; direction += "南"; }

        if (nearEdge) {
            String msg = "§c⚠ 接近边界！越界将传送回中心";
            if (!direction.isEmpty()) {
                msg += " [" + direction + "]";
            }
            int textWidth = client.textRenderer.getWidth(Text.literal(msg));
            int centerX = screenWidth / 2;
            context.drawTextWithShadow(client.textRenderer, Text.literal(msg),
                    centerX - textWidth / 2, screenHeight - 50, 0xFFFF5555);
        }
    }
}

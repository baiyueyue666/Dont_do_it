package me.baiyueyue.dont_do_it.client.screen;

import me.baiyueyue.dont_do_it.item.GameBookItem;
import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 游戏大厅界面 —— 手持书本右键打开
 * 两个按钮：开始游戏 / 游戏设置
 */
public class GameBookScreen extends Screen {

    private int currentTimer = 60; // 默认值

    public GameBookScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 开始游戏按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a▶ 游戏开始"),
                btn -> {
                    ClientPlayNetworking.send(new GamePackets.RequestStartGamePayload());
                    this.close();
                })
                .dimensions(centerX - 100, centerY - 30, 200, 20)
                .build());

        // 游戏设置按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§e⚙ 游戏设置"),
                btn -> {
                    GameBookItem.requestOpenSettings(currentTimer);
                })
                .dimensions(centerX - 100, centerY, 200, 20)
                .build());

        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§7✕ 关闭"),
                btn -> this.close())
                .dimensions(centerX - 100, centerY + 30, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 40, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

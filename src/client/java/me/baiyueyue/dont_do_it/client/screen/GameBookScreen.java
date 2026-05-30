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
 * 三个按钮：开始游戏 / 词条更换倒计时 / 特殊事件触发倒计时
 */
public class GameBookScreen extends Screen {

    private int wordTimer = 60; // 默认值
    private int specialEventTimer = 180; // 默认值

    public GameBookScreen(Text title) {
        super(title);
    }

    public GameBookScreen(Text title, int wordTimer, int specialEventTimer) {
        super(title);
        this.wordTimer = wordTimer;
        this.specialEventTimer = specialEventTimer;
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

        // 词条更换倒计时按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§e⏱ 词条更换倒计时"),
                btn -> {
                    GameBookItem.requestOpenSettings(wordTimer, specialEventTimer, "WORD_TIMER");
                })
                .dimensions(centerX - 100, centerY, 200, 20)
                .build());

        // 特殊事件触发倒计时按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§6⚡ 特殊事件触发倒计时"),
                btn -> {
                    GameBookItem.requestOpenSettings(wordTimer, specialEventTimer, "SPECIAL_EVENT_TIMER");
                })
                .dimensions(centerX - 100, centerY + 25, 200, 20)
                .build());

        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§7✕ 关闭"),
                btn -> this.close())
                .dimensions(centerX - 100, centerY + 55, 200, 20)
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

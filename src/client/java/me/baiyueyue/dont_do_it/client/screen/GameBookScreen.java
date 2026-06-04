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
 * 五个按钮：开始游戏 / 词条更换倒计时 / 特殊事件触发倒计时 / 血量上限 / 游戏范围
 */
public class GameBookScreen extends Screen {

    private int wordTimer = 180; // 默认值
    private int specialEventTimer = 300; // 默认值
    private int hearts = 15; // 默认血量
    private int gameRange = 2; // 默认游戏范围（2×2区块）

    public GameBookScreen(Text title) {
        super(title);
    }

    public GameBookScreen(Text title, int wordTimer, int specialEventTimer) {
        super(title);
        this.wordTimer = wordTimer;
        this.specialEventTimer = specialEventTimer;
    }

    public GameBookScreen(Text title, int wordTimer, int specialEventTimer, int hearts) {
        super(title);
        this.wordTimer = wordTimer;
        this.specialEventTimer = specialEventTimer;
        this.hearts = hearts;
    }

    public GameBookScreen(Text title, int wordTimer, int specialEventTimer, int hearts, int gameRange) {
        super(title);
        this.wordTimer = wordTimer;
        this.specialEventTimer = specialEventTimer;
        this.hearts = hearts;
        this.gameRange = gameRange;
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
                .dimensions(centerX - 100, centerY - 40, 200, 20)
                .build());

        // 词条更换倒计时按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§e⏱ 词条更换倒计时"),
                btn -> {
                    GameBookItem.requestOpenSettings(wordTimer, specialEventTimer, hearts, gameRange, "WORD_TIMER");
                })
                .dimensions(centerX - 100, centerY - 15, 200, 20)
                .build());

        // 特殊事件触发倒计时按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§6⚡ 特殊事件触发倒计时"),
                btn -> {
                    GameBookItem.requestOpenSettings(wordTimer, specialEventTimer, hearts, gameRange, "SPECIAL_EVENT_TIMER");
                })
                .dimensions(centerX - 100, centerY + 10, 200, 20)
                .build());

        // 血量上限按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§c❤ 血量上限"),
                btn -> {
                    GameBookItem.requestOpenSettings(wordTimer, specialEventTimer, hearts, gameRange, "HEARTS");
                })
                .dimensions(centerX - 100, centerY + 35, 200, 20)
                .build());

        // 游戏范围按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§3🏠 游戏范围"),
                btn -> {
                    GameBookItem.requestOpenSettings(wordTimer, specialEventTimer, hearts, gameRange, "GAME_RANGE");
                })
                .dimensions(centerX - 100, centerY + 60, 200, 20)
                .build());

        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§7✕ 关闭"),
                btn -> this.close())
                .dimensions(centerX - 100, centerY + 90, 200, 20)
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

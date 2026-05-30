package me.baiyueyue.dont_do_it.client.screen;

import me.baiyueyue.dont_do_it.game.GameSettings;
import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 游戏设置界面 —— 单独设置词条更换时间 或 特殊事件触发间隔
 */
public class SettingsScreen extends Screen {

    public enum Mode { WORD_TIMER, SPECIAL_EVENT_TIMER }

    private final Mode mode;
    private int selectedWordTimer;
    private int selectedSpecialEventTimer;

    public SettingsScreen(Text title, int wordTimer, int specialEventTimer, Mode mode) {
        super(title);
        this.selectedWordTimer = wordTimer;
        this.selectedSpecialEventTimer = specialEventTimer;
        this.mode = mode;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int y = this.height / 2 - 60;

        if (mode == Mode.WORD_TIMER) {
            // ---- 词条更换时间 ----
            for (int i = 0; i < GameSettings.TIMER_OPTIONS.length; i++) {
                int sec = GameSettings.TIMER_OPTIONS[i];
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("§e" + sec + " 秒" + (selectedWordTimer == sec ? " §a✔" : "")),
                        btn -> selectWordTimer(sec))
                        .dimensions(centerX - 100, y + i * 22, 200, 20)
                        .build());
            }
            y += GameSettings.TIMER_OPTIONS.length * 22 + 10;
        } else {
            // ---- 特殊事件触发间隔 ----
            for (int i = 0; i < GameSettings.SPECIAL_EVENT_TIMER_OPTIONS.length; i++) {
                int sec = GameSettings.SPECIAL_EVENT_TIMER_OPTIONS[i];
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("§6" + sec + " 秒" + (selectedSpecialEventTimer == sec ? " §a✔" : "")),
                        btn -> selectSpecialEventTimer(sec))
                        .dimensions(centerX - 100, y + i * 22, 200, 20)
                        .build());
            }
            y += GameSettings.SPECIAL_EVENT_TIMER_OPTIONS.length * 22 + 10;
        }

        // 返回（携带当前设置值回到大厅）
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§7← 返回"),
                btn -> {
                    if (this.client != null) {
                        this.client.setScreen(new GameBookScreen(
                                Text.literal("§6不要做挑战"),
                                selectedWordTimer, selectedSpecialEventTimer));
                    }
                })
                .dimensions(centerX - 100, y, 200, 20)
                .build());
    }

    private void selectWordTimer(int seconds) {
        selectedWordTimer = seconds;
        sendSettings();
        rebuild();
    }

    private void selectSpecialEventTimer(int seconds) {
        selectedSpecialEventTimer = seconds;
        sendSettings();
        rebuild();
    }

    private void sendSettings() {
        ClientPlayNetworking.send(new GamePackets.UpdateSettingsFullPayload(
                selectedWordTimer, selectedSpecialEventTimer));
    }

    private void rebuild() {
        this.clearChildren();
        this.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int y = this.height / 2 - 80;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, y, 0xFFFFFF);
        y += 25;
        if (mode == Mode.WORD_TIMER) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§7选择词条更换倒计时"), centerX, y, 0xAAAAAA);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§7选择特殊事件触发倒计时"), centerX, y, 0xAAAAAA);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

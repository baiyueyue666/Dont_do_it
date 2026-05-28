package me.baiyueyue.dont_do_it.client.screen;

import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 游戏设置界面 —— 设置词条更换时间
 */
public class SettingsScreen extends Screen {

    private static final int[] OPTIONS = {60, 120, 180};
    private int selectedTimer;

    public SettingsScreen(Text title, int currentTimer) {
        super(title);
        this.selectedTimer = currentTimer;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 60秒
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§e60 秒" + (selectedTimer == 60 ? " §a✔" : "")),
                btn -> selectAndSend(60))
                .dimensions(centerX - 100, centerY - 30, 200, 20)
                .build());

        // 120秒
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§e120 秒" + (selectedTimer == 120 ? " §a✔" : "")),
                btn -> selectAndSend(120))
                .dimensions(centerX - 100, centerY, 200, 20)
                .build());

        // 180秒
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§e180 秒" + (selectedTimer == 180 ? " §a✔" : "")),
                btn -> selectAndSend(180))
                .dimensions(centerX - 100, centerY + 30, 200, 20)
                .build());

        // 返回
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§7← 返回"),
                btn -> {
                    if (this.client != null) {
                        this.client.setScreen(new GameBookScreen(
                                Text.literal("§6不要做挑战")));
                    }
                })
                .dimensions(centerX - 100, centerY + 60, 200, 20)
                .build());
    }

    private void selectAndSend(int seconds) {
        selectedTimer = seconds;
        ClientPlayNetworking.send(new GamePackets.UpdateSettingsPayload(seconds));
        // 重建按钮以更新勾选标记
        this.clearChildren();
        this.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 40, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7词条更换时间"), centerX, 70, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

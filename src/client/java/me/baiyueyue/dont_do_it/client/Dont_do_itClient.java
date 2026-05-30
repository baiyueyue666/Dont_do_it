package me.baiyueyue.dont_do_it.client;

import me.baiyueyue.dont_do_it.client.game.BossBarManager;
import me.baiyueyue.dont_do_it.client.game.ClientPacketHandler;
import me.baiyueyue.dont_do_it.client.game.GameHudRenderer;
import me.baiyueyue.dont_do_it.client.screen.GameBookScreen;
import me.baiyueyue.dont_do_it.client.screen.SettingsScreen;
import me.baiyueyue.dont_do_it.item.GameBookItem;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.text.Text;

public class Dont_do_itClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注入屏幕打开器（避免 main 源码直接引用 client 类）
        GameBookItem.setScreenOpener(() -> {
            net.minecraft.client.MinecraftClient.getInstance()
                    .setScreen(new GameBookScreen(Text.literal("§6不要做挑战")));
        });
        GameBookItem.setSettingsOpener((wordTimer, specialEventTimer, mode) -> {
            var modeEnum = "SPECIAL_EVENT_TIMER".equals(mode)
                    ? SettingsScreen.Mode.SPECIAL_EVENT_TIMER
                    : SettingsScreen.Mode.WORD_TIMER;
            String title = modeEnum == SettingsScreen.Mode.WORD_TIMER
                    ? "§6词条更换倒计时" : "§6特殊事件触发倒计时";
            net.minecraft.client.MinecraftClient.getInstance()
                    .setScreen(new SettingsScreen(Text.literal(title), wordTimer, specialEventTimer, modeEnum));
        });

        // 注册 HUD 渲染
        GameHudRenderer.register();

        // 初始化 BossBar（预创建两条血条结构）
        BossBarManager.init();

        // 注册数据包接收
        ClientPacketHandler.register();
    }
}

package me.baiyueyue.dont_do_it.client;

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
        GameBookItem.setSettingsOpener(currentTimer -> {
            net.minecraft.client.MinecraftClient.getInstance()
                    .setScreen(new SettingsScreen(Text.literal("§6游戏设置"), currentTimer));
        });

        // 注册 HUD 渲染
        GameHudRenderer.register();

        // 注册数据包接收
        ClientPacketHandler.register();
    }
}

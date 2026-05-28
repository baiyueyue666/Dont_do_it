package me.baiyueyue.dont_do_it;

import me.baiyueyue.dont_do_it.command.GameCommand;
import me.baiyueyue.dont_do_it.game.GameCountdownManager;
import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.GameState;
import me.baiyueyue.dont_do_it.game.trigger.WordTriggerDetector;
import me.baiyueyue.dont_do_it.item.GameBookItem;
import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Dont_do_it implements ModInitializer {

    @Override
    public void onInitialize() {
        // 注册物品
        RegistryKey<Item> bookKey = RegistryKey.of(Registries.ITEM.getKey(),
                Identifier.of("dont_do_it", "game_book"));
        Item gameBook = Registry.register(Registries.ITEM, bookKey,
                new GameBookItem(new Item.Settings().registryKey(bookKey).maxCount(1)));

        // 注入到 GameManager，供开始/结束时清包发书使用
        GameManager.getInstance().setGameBookItem(gameBook);

        // 注册网络包 codec
        GamePackets.register();

        // 注册事件检测器
        WordTriggerDetector.register();

        // 注册倒计时管理器
        GameCountdownManager.register();

        // 注册命令
        GameCommand.register();

        // ---- C2S 包处理 ----

        // 客户端请求开始游戏
        ServerPlayNetworking.registerGlobalReceiver(GamePackets.RequestStartGamePayload.ID,
                (payload, context) -> {
                    GameManager.getInstance().startGame(
                            ((ServerWorld) context.player().getEntityWorld()).getServer());
                });

        // 客户端更新设置
        ServerPlayNetworking.registerGlobalReceiver(GamePackets.UpdateSettingsPayload.ID,
                (payload, context) -> {
                    GameManager.getInstance().getSettings().setWordChangeTimerSeconds(payload.timerSeconds());
                    context.player().sendMessage(
                            Text.literal("§a✔ 词条更换时间已设为 §e%d 秒".formatted(payload.timerSeconds())), true);
                });

        // 玩家加入时给予书本（仅在 WAITING 状态下发放）
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (GameManager.getInstance().getState() == GameState.WAITING) {
                    GameManager.giveGameBook(player);
                }
            }
        });
    }
}

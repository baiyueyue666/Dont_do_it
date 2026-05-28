package me.baiyueyue.dont_do_it.game.trigger;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

import java.util.*;

/**
 * 词条触发检测器 —— 注册 Fabric API 事件，匹配玩家行为与词条
 *
 * Tick 轮询事件（潜行/疾跑/跳跃）使用状态变化检测，只在第一次触发时上报，
 * 后续连续状态不再重复触发。
 */
public class WordTriggerDetector {

    // 记录玩家上一次的潜行/疾跑/着地状态
    private static final Map<UUID, Boolean> wasSneaking = new HashMap<>();
    private static final Map<UUID, Boolean> wasSprinting = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnGround = new HashMap<>();

    public static void register() {

        // ---- 攻击生物 ----
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity sp && entity != null && world instanceof ServerWorld sw) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.ATTACK);
            }
            return ActionResult.PASS;
        });

        // ---- 破坏方块 ----
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.BLOCK_BREAK);
            }
            return true;
        });

        // ---- 放置方块 ----
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
                var stack = sp.getStackInHand(hand);
                if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.BLOCK_PLACE);
                }
            }
            return ActionResult.PASS;
        });

        // ---- 聊天 ----
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender instanceof ServerPlayerEntity sp) {
                GameManager.getInstance().onPlayerTriggered(
                        ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.CHAT);
            }
        });

        // ---- 受到伤害 ----
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, originalHealth, newHealth) -> {
            if (entity instanceof ServerPlayerEntity sp && amount > 0) {
                GameManager.getInstance().onPlayerTriggered(
                        ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.TAKE_DAMAGE);
            }
        });

        // ---- 吃东西 ----
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
                var stack = sp.getStackInHand(hand);
                if (stack.contains(DataComponentTypes.FOOD)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.EAT);
                }
            }
            return ActionResult.PASS;
        });

        // ---- Tick 轮询：潜行/疾跑/跳跃状态变化检测 ----
        ServerTickEvents.END_SERVER_TICK.register(WordTriggerDetector::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (!GameManager.getInstance().isRunning()) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();

            boolean sneaking = player.isSneaking();
            Boolean prevSneak = wasSneaking.put(id, sneaking);
            // 只在从不潜行→潜行时触发（边缘检测）
            if (sneaking && (prevSneak == null || !prevSneak)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.SNEAK);
            }

            boolean sprinting = player.isSprinting();
            Boolean prevSprint = wasSprinting.put(id, sprinting);
            if (sprinting && (prevSprint == null || !prevSprint)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.SPRINT);
            }

            boolean onGround = player.isOnGround();
            Boolean prevGround = wasOnGround.put(id, onGround);
            // 从不在地面→在地面，说明刚落地=完成了一次跳跃
            if (onGround && prevGround != null && !prevGround) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.JUMP);
            }
        }

        // 清理离线玩家状态
        wasSneaking.keySet().retainAll(
                server.getPlayerManager().getPlayerList().stream().map(p -> (UUID)p.getUuid()).toList());
        wasSprinting.keySet().retainAll(
                server.getPlayerManager().getPlayerList().stream().map(p -> (UUID)p.getUuid()).toList());
        wasOnGround.keySet().retainAll(
                server.getPlayerManager().getPlayerList().stream().map(p -> (UUID)p.getUuid()).toList());
    }
}

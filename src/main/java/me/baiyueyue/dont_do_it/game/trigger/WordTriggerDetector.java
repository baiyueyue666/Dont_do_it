package me.baiyueyue.dont_do_it.game.trigger;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

import java.util.*;

/**
 * 词条触发检测器 —— 注册 Fabric API 事件，匹配玩家行为与词条
 *
 * Tick 轮询事件（潜行/疾跑）使用状态变化检测，只在第一次触发时上报，
 * 后续连续状态不再重复触发。
 */
public class WordTriggerDetector {

    // 记录玩家上一次的潜行/疾跑/进食状态
    private static final Map<UUID, Boolean> wasSneaking = new HashMap<>();
    private static final Map<UUID, Boolean> wasSprinting = new HashMap<>();
    private static final Map<UUID, Boolean> wasUsingFood = new HashMap<>();

    public static void register() {

        // ---- 攻击生物（除玩家外） ----
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity sp && entity != null && world instanceof ServerWorld sw
                    && !(entity instanceof ServerPlayerEntity)) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.ATTACK);
            }
            return ActionResult.PASS;
        });

        // ---- 打怪（仅对敌对生物造成伤害后触发） ----
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, originalHealth, newHealth) -> {
            if (amount > 0 && entity instanceof HostileEntity
                    && source.getAttacker() instanceof ServerPlayerEntity sp
                    && sp.getEntityWorld() instanceof ServerWorld sw) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.ATTACK_HOSTILE);
            }
        });

        // ---- 破坏方块（破坏完后触发） ----
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.BLOCK_BREAK);
                String blockId = state.getBlock().getRegistryEntry().registryKey().getValue().getPath();
                // 挖矿：仅矿物方块
                if (isOreBlock(blockId)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_ORE);
                }
                // 细分矿石类型
                TriggerType specificOre = getSpecificOreType(blockId);
                if (specificOre != null) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, specificOre);
                }
                // 挖掘木头
                if (isWoodBlock(blockId)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_WOOD);
                }
                // 挖掘石头
                if (isStoneBlock(blockId)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_STONE);
                }
            }
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

        // ---- 吃东西（吃完后触发） ----
        // 通过 tick 轮询检测进食完成（见 onServerTick）

        // ---- Tick 轮询：潜行/疾跑状态变化检测 ----
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

            // 进食检测：从正在吃→不再吃=吃完
            boolean usingFood = player.isUsingItem()
                    && player.getActiveItem().contains(DataComponentTypes.FOOD);
            Boolean prevUsingFood = wasUsingFood.put(id, usingFood);
            if (!usingFood && prevUsingFood != null && prevUsingFood) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.EAT);
            }
        }

        // 清理离线玩家状态
        wasSneaking.keySet().retainAll(
                server.getPlayerManager().getPlayerList().stream().map(p -> (UUID)p.getUuid()).toList());
        wasSprinting.keySet().retainAll(
                server.getPlayerManager().getPlayerList().stream().map(p -> (UUID)p.getUuid()).toList());
        wasUsingFood.keySet().retainAll(
                server.getPlayerManager().getPlayerList().stream().map(p -> (UUID)p.getUuid()).toList());
    }

    /** 判断方块 ID 是否为矿物（含原版矿石和深板岩变种） */
    private static boolean isOreBlock(String blockId) {
        return blockId.contains("_ore") || blockId.equals("ancient_debris");
    }

    /** 判断方块 ID 是否为木头/原木/菌柄类 */
    private static boolean isWoodBlock(String blockId) {
        return blockId.endsWith("_log") || blockId.endsWith("_wood")
                || blockId.endsWith("_stem") || blockId.endsWith("_hyphae")
                || blockId.equals("bamboo_block");
    }

    /** 判断方块 ID 是否为石头/圆石类 */
    private static boolean isStoneBlock(String blockId) {
        return blockId.equals("stone") || blockId.equals("cobblestone")
                || blockId.equals("mossy_cobblestone");
    }

    /** 根据方块 ID 返回对应的细分矿石触发类型，非矿石返回 null */
    private static TriggerType getSpecificOreType(String blockId) {
        if (blockId.contains("coal_ore"))       return TriggerType.MINE_COAL;
        if (blockId.contains("iron_ore"))       return TriggerType.MINE_IRON;
        if (blockId.contains("copper_ore"))     return TriggerType.MINE_COPPER;
        if (blockId.contains("gold_ore"))       return TriggerType.MINE_GOLD;
        if (blockId.contains("diamond_ore"))    return TriggerType.MINE_DIAMOND;
        return null;
    }
}

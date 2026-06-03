package me.baiyueyue.dont_do_it.mixin;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import me.baiyueyue.dont_do_it.game.trigger.WordTriggerDetector;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为 DROP_ITEM 和 OPEN_CONTAINER 触发类型提供 Mixin 事件注入
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    /**
     * 丢弃物品 —— 在 dropSelectedItem 头部注入，检查手上是否有物品
     */
    @Inject(method = "dropSelectedItem", at = @At("HEAD"))
    private void onDropSelectedItem(boolean entireStack, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        var stack = self.getMainHandStack();
        if (!stack.isEmpty()) {
            GameManager.getInstance().onPlayerTriggered(
                    ((ServerWorld) self.getEntityWorld()).getServer(), self, TriggerType.DROP_ITEM);
            // 丢弃30个方块计数
            WordTriggerDetector.incrementDropCount(self.getUuid());

            // 丢弃特定物品检测
            String itemId = stack.getItem().getRegistryEntry().registryKey().getValue().getPath();
            TriggerType dropType = getDropItemType(itemId);
            if (dropType != null) {
                GameManager.getInstance().onPlayerTriggered(
                        ((ServerWorld) self.getEntityWorld()).getServer(), self, dropType);
            }
        }
    }

    /** 根据物品 ID 返回对应的丢弃触发类型 */
    private static TriggerType getDropItemType(String itemId) {
        switch (itemId) {
            case "dirt":               return TriggerType.DROP_DIRT;
            case "cobblestone":        return TriggerType.DROP_COBBLESTONE;
            case "cobbled_deepslate":  return TriggerType.DROP_COBBLED_DEEPSLATE;
            case "andesite":           return TriggerType.DROP_ANDESITE;
            case "granite":            return TriggerType.DROP_GRANITE;
            case "diorite":            return TriggerType.DROP_DIORITE;
            case "tuff":               return TriggerType.DROP_TUFF;
            case "wooden_pickaxe":     return TriggerType.DROP_WOODEN_PICKAXE;
            default:                   return null;
        }
    }

    /**
     * 打开容器 —— 在 openHandledScreen 头部注入
     */
    @Inject(method = "openHandledScreen", at = @At("HEAD"))
    private void onOpenHandledScreen(
            @SuppressWarnings("unused") net.minecraft.screen.NamedScreenHandlerFactory factory,
            CallbackInfoReturnable<java.util.OptionalInt> cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        GameManager.getInstance().onPlayerTriggered(
                ((ServerWorld) self.getEntityWorld()).getServer(), self, TriggerType.OPEN_CONTAINER);
    }

    /**
     * 村民交易 —— 通过统计 TRADE_WITH_VILLAGER 增长检测
     */
    @Inject(method = "increaseStat", at = @At("HEAD"))
    private void onIncreaseStat(Stat<?> stat, int amount, CallbackInfo ci) {
        if (stat.getValue() instanceof Identifier id && id.equals(Stats.TRADED_WITH_VILLAGER)) {
            ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
            GameManager.getInstance().onPlayerTriggered(
                    ((ServerWorld) self.getEntityWorld()).getServer(), self, TriggerType.VILLAGER_TRADE);
        }
    }
}

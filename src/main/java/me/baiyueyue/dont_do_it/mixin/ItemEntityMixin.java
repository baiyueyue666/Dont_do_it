package me.baiyueyue.dont_do_it.mixin;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 PICKUP_ITEM / PICKUP_WOOD 触发类型提供 Mixin 事件注入。
 *
 * 在 1.21.11 中，onPlayerCollision 仅在 Entity 基类中定义，
 * ItemEntity 并未覆写该方法，因此直接注入到 Entity.onPlayerCollision
 * 然后通过 instanceof ItemEntity 过滤。
 */
@Mixin(Entity.class)
public class ItemEntityMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("dont_do_it");

    /**
     * 注意：1.21.11 中 ItemEntity 不再调用 Entity.onPlayerCollision，
     * 此注入在 1.21.11 中不会被触发。PICKUP_WOOD 已迁移至 PlayerInventoryMixin。
     * 保留此 Mixin 以兼容未来版本。
     */
    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void onPickupItem(PlayerEntity player, CallbackInfo ci) {
        // 只处理 ItemEntity（物品掉落物实体）
        if (!((Object) this instanceof ItemEntity self)) return;
        if (!(player instanceof ServerPlayerEntity sp)) return;
        if (self.getStack().isEmpty()) return;

        String itemId = self.getStack().getItem().getRegistryEntry()
                .registryKey().getValue().getPath();

        // 先检查细分类型（如 PICKUP_WOOD），避免被通用 PICKUP_ITEM 的冷却影响
        boolean matched = false;
        if (isWoodItem(itemId)) {
            GameManager.getInstance().onPlayerTriggered(
                    ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.PICKUP_WOOD);
            matched = true;
            LOGGER.info("[Dont_do_it] PICKUP_WOOD 触发: itemId={}", itemId);
        }

        // 通用拾取事件
        GameManager.getInstance().onPlayerTriggered(
                ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.PICKUP_ITEM);
        if (!matched) {
            LOGGER.debug("[Dont_do_it] PICKUP_ITEM 触发: itemId={}", itemId);
        }
    }

    /** 判断物品 ID 是否为原木 */
    private static boolean isWoodItem(String itemId) {
        return itemId.endsWith("_log");
    }
}

package me.baiyueyue.dont_do_it.mixin;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 替代 ItemEntityMixin 的方案：
 * 在 1.21.11 中 ItemEntity 不触发 Entity.onPlayerCollision，
 * 改为监听物品进入玩家背包的底层方法。
 */
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("dont_do_it");

    static {
        LOGGER.info("[Dont_do_it] PlayerInventoryMixin 已加载");
    }

    // ---- insertStack(ItemStack) -> boolean ----
    @Inject(method = "insertStack", at = @At("HEAD"), require = 0)
    private void onInsertStack_head(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        LOGGER.info("[Dont_do_it] DIAG: insertStack(ItemStack) 被调用, item={}, count={}",
                stack.isEmpty() ? "EMPTY" : stack.getItem().getRegistryEntry().registryKey().getValue().getPath(),
                stack.getCount());
        checkPickupWood(stack);
    }

    // ---- offerOrDrop(ItemStack) ----
    @Inject(method = "offerOrDrop", at = @At("HEAD"), require = 0)
    private void onOfferOrDrop(ItemStack stack, CallbackInfo ci) {
        LOGGER.info("[Dont_do_it] DIAG: offerOrDrop(ItemStack) 被调用, item={}, count={}",
                stack.isEmpty() ? "EMPTY" : stack.getItem().getRegistryEntry().registryKey().getValue().getPath(),
                stack.getCount());
        checkPickupWood(stack);
    }

    // ---- addStack(ItemStack) -> int ----
    @Inject(method = "addStack", at = @At("HEAD"), require = 0)
    private void onAddStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        LOGGER.info("[Dont_do_it] DIAG: addStack(ItemStack) 被调用, item={}, count={}",
                stack.isEmpty() ? "EMPTY" : stack.getItem().getRegistryEntry().registryKey().getValue().getPath(),
                stack.getCount());
        checkPickupWood(stack);
    }

    @Unique
    private void checkPickupWood(ItemStack stack) {
        if (stack.isEmpty()) return;

        PlayerInventory self = (PlayerInventory) (Object) this;
        if (!(self.player instanceof ServerPlayerEntity sp)) return;

        String itemId = stack.getItem().getRegistryEntry()
                .registryKey().getValue().getPath();

        // 只检查原木（_log 结尾）
        if (itemId.endsWith("_log")) {
            GameManager.getInstance().onPlayerTriggered(
                    ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.PICKUP_WOOD);
            LOGGER.info("[Dont_do_it] PICKUP_WOOD 触发: itemId={}, count={}",
                    itemId, stack.getCount());
        }
    }
}

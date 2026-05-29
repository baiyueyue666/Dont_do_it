package me.baiyueyue.dont_do_it.mixin;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 PICKUP_ITEM 触发类型提供 Mixin 事件注入
 *
 * 1.21.2+ 中 sendPickup 从 PlayerEntity 移到 LivingEntity，
 * 参数从 (ItemEntity, int) 变为 (Entity, int)。
 */
@Mixin(LivingEntity.class)
public class ItemEntityMixin {

    @Inject(method = "sendPickup", at = @At("HEAD"))
    private void onSendPickup(Entity item, int count, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity sp) {
            GameManager.getInstance().onPlayerTriggered(
                    ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.PICKUP_ITEM);
        }
    }
}

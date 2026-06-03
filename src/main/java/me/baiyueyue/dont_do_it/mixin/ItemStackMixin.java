package me.baiyueyue.dont_do_it.mixin;

import me.baiyueyue.dont_do_it.game.GameManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截物品耐久消耗，实现「豁免祝福」（耐久不消耗）和「装备锈蚀」（五倍消耗）
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    /** 防止递归调用（装备锈蚀需要重新调用 damage 方法施加 5x 伤害） */
    private static final ThreadLocal<Boolean> MODIFYING_DAMAGE = ThreadLocal.withInitial(() -> false);

    @Inject(method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V",
            at = @At("HEAD"), cancellable = true)
    private void onDamage(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayerEntity)) return;
        if (MODIFYING_DAMAGE.get()) return;

        GameManager gm = GameManager.getInstance();
        if (gm.isDurabilityBlessingActive()) {
            // 豁免祝福：取消耐久消耗
            ci.cancel();
            return;
        }
        if (gm.isEquipmentRustActive()) {
            // 装备锈蚀：五倍损耗
            ci.cancel();
            MODIFYING_DAMAGE.set(true);
            try {
                ((ItemStack) (Object) this).damage(amount * 5, entity, slot);
            } finally {
                MODIFYING_DAMAGE.set(false);
            }
        }
    }
}

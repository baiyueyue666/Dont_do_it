package me.baiyueyue.dont_do_it.mixin;

import me.baiyueyue.dont_do_it.game.GameManager;
import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截食物回复，实现「饥饿疫病」（食物回复效果减半）
 */
@Mixin(HungerManager.class)
public class HungerManagerMixin {

    /** 防止递归调用 */
    private static final ThreadLocal<Boolean> MODIFYING_HUNGER = ThreadLocal.withInitial(() -> false);

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void onAddFood(int food, float saturationModifier, CallbackInfo ci) {
        if (MODIFYING_HUNGER.get()) return;
        GameManager gm = GameManager.getInstance();
        if (gm.isHungerDiseaseActive()) {
            ci.cancel();
            MODIFYING_HUNGER.set(true);
            try {
                // 食物回复效果减半
                int halvedFood = Math.max(1, food / 2);
                float halvedSaturation = saturationModifier / 2.0f;
                ((HungerManager) (Object) this).add(halvedFood, halvedSaturation);
            } finally {
                MODIFYING_HUNGER.set(false);
            }
        }
    }
}

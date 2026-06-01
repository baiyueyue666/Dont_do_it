package me.baiyueyue.dont_do_it.mixin;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为合成类触发类型提供 Mixin 事件注入
 *
 * 监听 CraftingResultSlot.onTakeItem，覆盖工作台和背包 2x2 合成两种场景。
 */
@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("dont_do_it");

    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onCraftItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        if (stack.isEmpty()) return;

        String itemId = stack.getItem().getRegistryEntry().registryKey().getValue().getPath();
        LOGGER.info("[Dont_do_it] CRAFT 检测: itemId={}", itemId);

        TriggerType type = getCraftTriggerType(itemId);
        if (type != null) {
            LOGGER.info("[Dont_do_it] 合成触发: itemId={}, type={}", itemId, type);
            GameManager.getInstance().onPlayerTriggered(
                    ((ServerWorld) sp.getEntityWorld()).getServer(), sp, type);
        }
    }

    /** 根据合成产物 ID 返回对应的触发类型，非目标物品返回 null */
    private static TriggerType getCraftTriggerType(String itemId) {
        switch (itemId) {
            case "crafting_table":    return TriggerType.CRAFT_CRAFTING_TABLE;
            case "wooden_pickaxe":    return TriggerType.CRAFT_WOODEN_PICKAXE;
            case "stone_pickaxe":     return TriggerType.CRAFT_STONE_PICKAXE;
            case "iron_pickaxe":      return TriggerType.CRAFT_IRON_PICKAXE;
            case "wooden_axe":        return TriggerType.CRAFT_WOODEN_AXE;
            case "stone_axe":         return TriggerType.CRAFT_STONE_AXE;
            case "iron_axe":          return TriggerType.CRAFT_IRON_AXE;
            case "wooden_sword":      return TriggerType.CRAFT_WOODEN_SWORD;
            case "stone_sword":       return TriggerType.CRAFT_STONE_SWORD;
            case "iron_sword":        return TriggerType.CRAFT_IRON_SWORD;
            default:                  return null;
        }
    }
}

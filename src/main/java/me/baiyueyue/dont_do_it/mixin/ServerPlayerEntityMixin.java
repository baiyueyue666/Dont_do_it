package me.baiyueyue.dont_do_it.mixin;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
        if (!self.getMainHandStack().isEmpty()) {
            GameManager.getInstance().onPlayerTriggered(
                    ((ServerWorld) self.getEntityWorld()).getServer(), self, TriggerType.DROP_ITEM);
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
}

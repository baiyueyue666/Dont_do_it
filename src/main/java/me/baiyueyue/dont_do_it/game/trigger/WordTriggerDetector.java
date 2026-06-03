package me.baiyueyue.dont_do_it.game.trigger;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.PlayerWordData;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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

    // ---- 视角方向边缘检测 ----
    private static final Map<UUID, Boolean> wasLookingDown = new HashMap<>();
    private static final Map<UUID, Boolean> wasLookingUp = new HashMap<>();
    private static final Map<UUID, Boolean> wasLookingEast = new HashMap<>();
    private static final Map<UUID, Boolean> wasLookingSouth = new HashMap<>();
    private static final Map<UUID, Boolean> wasLookingWest = new HashMap<>();
    private static final Map<UUID, Boolean> wasLookingNorth = new HashMap<>();

    // ---- 不动五秒计时器 ----
    private static final Map<UUID, Double> lastX = new HashMap<>();
    private static final Map<UUID, Double> lastY = new HashMap<>();
    private static final Map<UUID, Double> lastZ = new HashMap<>();
    private static final Map<UUID, Integer> standStillTicks = new HashMap<>();
    private static final Map<UUID, Boolean> standStillTriggered = new HashMap<>();

    // ---- 持续同方向计时器 ----
    private static final Map<UUID, Float> lastYaw = new HashMap<>();
    private static final Map<UUID, Float> lastPitch = new HashMap<>();
    private static final Map<UUID, Integer> lookSameDirTicks = new HashMap<>();
    private static final Map<UUID, Boolean> lookSameDirTriggered = new HashMap<>();

    // ---- 环境状态边缘检测 ----
    private static final Map<UUID, Boolean> wasEnclosed = new HashMap<>();
    private static final Map<UUID, Boolean> wasSubmerged = new HashMap<>();

    // ---- 站立方块边缘检测 ----
    private static final Map<UUID, Boolean> wasOnGrass = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnLeaves = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnStone = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnDeepslate = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnAndesite = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnDiorite = new HashMap<>();

    // ---- 浮空边缘检测 ----
    private static final Map<UUID, Boolean> wasFloating = new HashMap<>();

    // ---- 死亡计时 ----
    private static final Map<UUID, Long> deathTick = new HashMap<>();
    private static final Map<UUID, Boolean> notRespawn3sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> notRespawn5sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> notRespawn10sTriggered = new HashMap<>();

    // ---- 背包物品边缘检测 ----
    private static final Map<UUID, Boolean> wasHasCoal = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasIronIngot = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasCopperIngot = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasCraftingTable = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasFurnace = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasAxe = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasSword = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasStonePickaxe = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasWoodenPickaxe = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasIronPickaxe = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasRottenFlesh = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasDiamond = new HashMap<>();
    private static final Map<UUID, Boolean> wasHasDirt = new HashMap<>();

    // ---- 新增：饥饿度 / 高度 / 站立方块 边缘检测 ----
    private static final Map<UUID, Boolean> wasHungerBelow18 = new HashMap<>();
    private static final Map<UUID, Boolean> wasHungerAbove18 = new HashMap<>();
    private static final Map<UUID, Boolean> wasYAbove70 = new HashMap<>();
    private static final Map<UUID, Boolean> wasYBelow70 = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnGranite = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnTuff = new HashMap<>();

    // ---- 新增：距离相关 边缘检测 ----
    private static final Map<UUID, Boolean> wasFarFromAll15m = new HashMap<>();
    private static final Map<UUID, Boolean> wasTooCloseToPlayer = new HashMap<>();

    // ---- 新增：持续行为计时 / 计数 ----
    private static final Map<UUID, Long> sprintStartTick = new HashMap<>();  // serverTick when sprint started
    private static final Map<UUID, Long> sneakStartTick = new HashMap<>();    // serverTick when sneak started
    private static final Map<UUID, Integer> jumpCount = new HashMap<>();
    private static final Map<UUID, Boolean> sprint30sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> sneak5sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> jump10Triggered = new HashMap<>();

    // ---- 新增：经验/等级跟踪 ----
    private static final Map<UUID, Integer> prevExperienceLevel = new HashMap<>();
    private static final Map<UUID, Float> prevExperienceProgress = new HashMap<>();

    // ---- 新增：跳跃检测辅助（跟踪上一 tick 是否在地面）----
    private static final Map<UUID, Boolean> wasOnGround = new HashMap<>();

    // ---- 新增：吃腐肉检测辅助 ----
    private static final Map<UUID, String> lastEatenFoodId = new HashMap<>();

    // ---- 新增：下落高度检测辅助 ----
    private static final Map<UUID, Double> fallStartY = new HashMap<>();  // 离开地面时的 Y 坐标
    private static final Map<UUID, Boolean> fallTriggered = new HashMap<>();  // 防止同一次下落重复触发

    // ---- 新增：放置/丢弃计数 ----
    private static final Map<UUID, Integer> placeCount = new HashMap<>();
    private static final Map<UUID, Boolean> place30Triggered = new HashMap<>();
    private static final Map<UUID, Integer> dropCount = new HashMap<>();
    private static final Map<UUID, Boolean> drop30Triggered = new HashMap<>();

    // ---- 新增：不跳/不潜行/不疾跑倒计时 ----
    private static final Map<UUID, Long> lastJumpTick = new HashMap<>();
    private static final Map<UUID, Long> lastSneakActionTick = new HashMap<>();
    private static final Map<UUID, Long> lastSprintActionTick = new HashMap<>();
    private static final Map<UUID, Boolean> noJump30sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> noSneak30sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> noSprint30sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> noJump60sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> noSneak60sTriggered = new HashMap<>();
    private static final Map<UUID, Boolean> noSprint60sTriggered = new HashMap<>();

    // ---- 新增：头顶方块 / 站在基岩 边缘检测 ----
    private static final Map<UUID, Boolean> wasBlockAboveHead = new HashMap<>();
    private static final Map<UUID, Boolean> wasNoBlockAboveHead = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnBedrock = new HashMap<>();

    public static void register() {

        // ---- 攻击生物（除玩家外） ----
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity sp && entity != null && world instanceof ServerWorld sw
                    && !(entity instanceof ServerPlayerEntity)) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.ATTACK);
            }
            return ActionResult.PASS;
        });

        // ---- 攻击玩家 ----
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity sp && entity instanceof ServerPlayerEntity
                    && world instanceof ServerWorld sw) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.ATTACK_PLAYER);
                // 空手打人
                if (sp.getMainHandStack().isEmpty()) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.EMPTY_HAND_ATTACK);
                }
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

        // ---- 造成伤害（攻击者对任意实体造成伤害） ----
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, originalHealth, newHealth) -> {
            if (amount > 0 && source.getAttacker() instanceof ServerPlayerEntity sp
                    && sp.getEntityWorld() instanceof ServerWorld sw) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.DEAL_DAMAGE);
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
                // 钻石祝福：挖钻石矿回血
                if (blockId.contains("diamond_ore") && GameManager.getInstance().isDiamondBlessingActive()) {
                    PlayerWordData data = GameManager.getInstance().getPlayerData(sp.getUuid());
                    if (data != null && !data.isEliminated()) {
                        int maxHearts = GameManager.getInstance().getSettings().getDefaultHearts();
                        if (data.getHearts() < maxHearts) {
                            data.addHeart();
                            sp.sendMessage(Text.literal("§b💎 钻石祝福！回复一颗心 ❤×§c" + data.getHearts()), true);
                        }
                    }
                }
                // 挖掘木头
                if (isWoodBlock(blockId)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_WOOD);
                }
                // 挖掘石头
                if (isStoneBlock(blockId)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_STONE);
                }
                // 挖掘安山岩
                if (blockId.equals("andesite")) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_ANDESITE);
                }
                // 挖掘闪长岩
                if (blockId.equals("diorite")) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_DIORITE);
                }
                // 挖掘深板岩
                if (isDeepslateBlock(blockId)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_DEEPSLATE);
                }
                // 挖掘花岗岩
                if (blockId.equals("granite")) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_GRANITE);
                }
                // 挖掘凝灰岩
                if (blockId.equals("tuff")) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_TUFF);
                }
                // 挖掘工作台
                if (blockId.equals("crafting_table")) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_CRAFTING_TABLE);
                }
                // 挖掘熔炉
                if (blockId.equals("furnace")) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.MINE_FURNACE);
                }
            }
        });

        // ---- 放置方块 + 打开容器 + 特定方块放置 ----
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
                var stack = sp.getStackInHand(hand);
                String blockId = world.getBlockState(hitResult.getBlockPos()).getBlock()
                        .getRegistryEntry().registryKey().getValue().getPath();

                // 优先检测容器打开（无论手持何物，右击交互方块都应触发）
                TriggerType openType = getOpenContainerType(blockId);
                if (openType != null) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, openType);
                }

                if (stack.getItem() instanceof net.minecraft.item.BlockItem bi) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.BLOCK_PLACE);
                    // 放置30个方块计数
                    UUID id = sp.getUuid();
                    int cnt = placeCount.getOrDefault(id, 0) + 1;
                    placeCount.put(id, cnt);
                    if (cnt >= 30 && !place30Triggered.getOrDefault(id, false)) {
                        place30Triggered.put(id, true);
                        GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.PLACE_30_BLOCKS);
                    }
                    // 放置特定方块
                    String itemId = bi.getRegistryEntry().registryKey().getValue().getPath();
                    TriggerType placeType = getPlaceBlockType(itemId);
                    if (placeType != null) {
                        GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, placeType);
                    }
                }
            }
            return ActionResult.PASS;
        });

        // ---- 桶倒液（水桶/岩浆桶使用）+ 桶装液（空桶射线检测流体） ----
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
                var stack = sp.getStackInHand(hand);
                String itemId = stack.getItem().getRegistryEntry().registryKey().getValue().getPath();
                if (itemId.equals("water_bucket")) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.EMPTY_BUCKET_WATER);
                } else if (itemId.equals("lava_bucket")) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.EMPTY_BUCKET_LAVA);
                } else if (itemId.equals("bucket")) {
                    // 空桶：射线检测（含流体），判断目标是否为水或岩浆
                    var hit = player.raycast(5.0, 0.0F, true);
                    if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                        var blockHit = (net.minecraft.util.hit.BlockHitResult) hit;
                        String targetBlockId = world.getBlockState(blockHit.getBlockPos()).getBlock()
                                .getRegistryEntry().registryKey().getValue().getPath();
                        if (targetBlockId.equals("water")) {
                            GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.FILL_BUCKET_WATER);
                        } else if (targetBlockId.equals("lava")) {
                            GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.FILL_BUCKET_LAVA);
                        }
                    }
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

        // ---- 受到火焰伤害 ----
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, originalHealth, newHealth) -> {
            if (entity instanceof ServerPlayerEntity sp && amount > 0 && isFireDamage(source)) {
                GameManager.getInstance().onPlayerTriggered(
                        ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.TAKE_FIRE_DAMAGE);
            }
        });

        // ---- 弹射物伤害 ----
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, originalHealth, newHealth) -> {
            if (entity instanceof ServerPlayerEntity sp && amount > 0
                    && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                GameManager.getInstance().onPlayerTriggered(
                        ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.TAKE_PROJECTILE_DAMAGE);
            }
        });

        // ---- 一次性受到5滴血伤害 ----
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, originalHealth, newHealth) -> {
            if (entity instanceof ServerPlayerEntity sp && amount >= 5) {
                GameManager.getInstance().onPlayerTriggered(
                        ((ServerWorld) sp.getEntityWorld()).getServer(), sp, TriggerType.TAKE_5_DAMAGE);
            }
        });

        // ---- 吃东西（吃完后触发） ----
        // 通过 tick 轮询检测进食完成（见 onServerTick）

        // ---- 死亡 ----
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity sp
                    && sp.getEntityWorld() instanceof ServerWorld sw) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.DEATH);
                UUID id = sp.getUuid();
                deathTick.put(id, (long) sw.getServer().getTicks());
                notRespawn3sTriggered.remove(id);
                notRespawn5sTriggered.remove(id);
                notRespawn10sTriggered.remove(id);

                // ---- 死亡细分检测 ----
                if (source.isOf(DamageTypes.FALL)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.DEATH_BY_FALL);
                }
                if (source.isOf(DamageTypes.LAVA)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.DEATH_BY_LAVA);
                }
                if (source.isOf(DamageTypes.IN_WALL)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.DEATH_BY_SUFFOCATION);
                }
                if (source.isOf(DamageTypes.DROWN)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.DEATH_BY_DROWN);
                }
                if (source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.PLAYER_EXPLOSION)) {
                    GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.DEATH_BY_EXPLOSION);
                }
            }
            // 杀死铁傀儡
            if (entity instanceof IronGolemEntity
                    && source.getAttacker() instanceof ServerPlayerEntity sp
                    && sp.getEntityWorld() instanceof ServerWorld sw) {
                GameManager.getInstance().onPlayerTriggered(sw.getServer(), sp, TriggerType.KILL_IRON_GOLEM);
            }
        });

        // ---- 复活 ----
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            UUID id = newPlayer.getUuid();
            deathTick.remove(id);
            notRespawn3sTriggered.remove(id);
            notRespawn5sTriggered.remove(id);
            notRespawn10sTriggered.remove(id);
            GameManager.getInstance().onPlayerTriggered(
                    ((ServerWorld) newPlayer.getEntityWorld()).getServer(), newPlayer, TriggerType.RESPAWN);
        });

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
            // 记录正在吃的食物 ID，供吃完后判断类型
            if (usingFood) {
                lastEatenFoodId.put(id, player.getActiveItem().getItem()
                        .getRegistryEntry().registryKey().getValue().getPath());
            }
            Boolean prevUsingFood = wasUsingFood.put(id, usingFood);
            if (!usingFood && prevUsingFood != null && prevUsingFood) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.EAT);
                // 检查是否吃了腐肉
                String foodId = lastEatenFoodId.get(id);
                if ("rotten_flesh".equals(foodId)) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.EAT_ROTTEN_FLESH);
                }
            }

            // ---- 低头/抬头 边缘检测 ----
            boolean lookingDown = player.getPitch() > 60;
            Boolean prevDown = wasLookingDown.put(id, lookingDown);
            if (lookingDown && (prevDown == null || !prevDown)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.LOOK_DOWN);
            }

            boolean lookingUp = player.getPitch() < -60;
            Boolean prevUp = wasLookingUp.put(id, lookingUp);
            if (lookingUp && (prevUp == null || !prevUp)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.LOOK_UP);
            }

            // ---- 看向方向 边缘检测 ----
            float yaw = ((player.getYaw() % 360) + 360) % 360;
            boolean lookingEast = yaw > 225 && yaw < 315;
            Boolean prevEast = wasLookingEast.put(id, lookingEast);
            if (lookingEast && (prevEast == null || !prevEast)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.LOOK_EAST);
            }

            boolean lookingSouth = yaw > 315 || yaw < 45;
            Boolean prevSouth = wasLookingSouth.put(id, lookingSouth);
            if (lookingSouth && (prevSouth == null || !prevSouth)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.LOOK_SOUTH);
            }

            boolean lookingWest = yaw > 45 && yaw < 135;
            Boolean prevWest = wasLookingWest.put(id, lookingWest);
            if (lookingWest && (prevWest == null || !prevWest)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.LOOK_WEST);
            }

            boolean lookingNorth = yaw > 135 && yaw < 225;
            Boolean prevNorth = wasLookingNorth.put(id, lookingNorth);
            if (lookingNorth && (prevNorth == null || !prevNorth)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.LOOK_NORTH);
            }

            // ---- 不动五秒 ----
            double curX = player.getX();
            double curY = player.getY();
            double curZ = player.getZ();
            Double px = lastX.put(id, curX);
            Double py = lastY.put(id, curY);
            Double pz = lastZ.put(id, curZ);
            if (px != null && py != null && pz != null) {
                boolean isStill = Math.abs(curX - px) < 0.1 && Math.abs(curY - py) < 0.1 && Math.abs(curZ - pz) < 0.1;
                if (isStill) {
                    int ticks = standStillTicks.getOrDefault(id, 0) + 1;
                    standStillTicks.put(id, ticks);
                    if (ticks >= 100 && !standStillTriggered.getOrDefault(id, false)) {
                        GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_STILL_5S);
                        standStillTriggered.put(id, true);
                    }
                } else {
                    standStillTicks.put(id, 0);
                    standStillTriggered.put(id, false);
                }
            }

            // ---- 持续看向一个方向五秒 ----
            float curYaw = player.getYaw();
            float curPitch = player.getPitch();
            Float prevYaw = lastYaw.put(id, curYaw);
            Float prevPitchVal = lastPitch.put(id, curPitch);
            if (prevYaw != null && prevPitchVal != null) {
                float yawDiff = Math.abs(curYaw - prevYaw);
                if (yawDiff > 180) yawDiff = 360 - yawDiff;
                boolean sameDir = yawDiff < 10 && Math.abs(curPitch - prevPitchVal) < 10;
                if (sameDir) {
                    int ticks = lookSameDirTicks.getOrDefault(id, 0) + 1;
                    lookSameDirTicks.put(id, ticks);
                    if (ticks >= 100 && !lookSameDirTriggered.getOrDefault(id, false)) {
                        GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.LOOK_SAME_DIR_5S);
                        lookSameDirTriggered.put(id, true);
                    }
                } else {
                    lookSameDirTicks.put(id, 0);
                    lookSameDirTriggered.put(id, false);
                }
            }

            // ---- 自闭（被方块包围在1x2空间） ----
            boolean enclosed = isPlayerEnclosed(player);
            Boolean prevEnclosed = wasEnclosed.put(id, enclosed);
            if (enclosed && (prevEnclosed == null || !prevEnclosed)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.ENCLOSED_1X2);
            }

            // ---- 沉入水中 ----
            boolean submerged = player.isSubmergedInWater();
            Boolean prevSubmerged = wasSubmerged.put(id, submerged);
            if (submerged && (prevSubmerged == null || !prevSubmerged)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.SUBMERGED);
            }

            // ---- 站在各种方块上 边缘检测 ----
            String belowBlockId = player.getEntityWorld()
                    .getBlockState(player.getBlockPos().down()).getBlock()
                    .getRegistryEntry().registryKey().getValue().getPath();

            boolean onGrass = belowBlockId.equals("grass_block");
            Boolean prevGrass = wasOnGrass.put(id, onGrass);
            if (onGrass && (prevGrass == null || !prevGrass)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_GRASS);
            }

            boolean onLeaves = belowBlockId.endsWith("_leaves") || belowBlockId.equals("leaves");
            Boolean prevLeaves = wasOnLeaves.put(id, onLeaves);
            if (onLeaves && (prevLeaves == null || !prevLeaves)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_LEAVES);
            }

            boolean onStone = belowBlockId.equals("stone");
            Boolean prevStone = wasOnStone.put(id, onStone);
            if (onStone && (prevStone == null || !prevStone)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_STONE);
            }

            boolean onDeepslate = belowBlockId.equals("deepslate");
            Boolean prevDeepslate = wasOnDeepslate.put(id, onDeepslate);
            if (onDeepslate && (prevDeepslate == null || !prevDeepslate)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_DEEPSLATE);
            }

            boolean onAndesite = belowBlockId.equals("andesite");
            Boolean prevAndesite = wasOnAndesite.put(id, onAndesite);
            if (onAndesite && (prevAndesite == null || !prevAndesite)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_ANDESITE);
            }

            boolean onDiorite = belowBlockId.equals("diorite");
            Boolean prevDiorite = wasOnDiorite.put(id, onDiorite);
            if (onDiorite && (prevDiorite == null || !prevDiorite)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_DIORITE);
            }

            // ---- 浮空（脚底没有任何方块） ----
            boolean floating = player.getEntityWorld()
                    .getBlockState(player.getBlockPos().down()).isAir();
            Boolean prevFloating = wasFloating.put(id, floating);
            if (floating && (prevFloating == null || !prevFloating)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.FLOATING);
            }

            // ---- 头顶有方块遮挡 / 头顶无方块遮挡 ----
            // 向上扫描直到世界高度，检查头顶是否有任意非空气方块
            boolean blockAboveHead = hasBlockAboveHead(player);
            Boolean prevBlockAbove = wasBlockAboveHead.put(id, blockAboveHead);
            if (blockAboveHead && (prevBlockAbove == null || !prevBlockAbove)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.BLOCK_ABOVE_HEAD);
            }
            Boolean prevNoBlockAbove = wasNoBlockAboveHead.put(id, !blockAboveHead);
            if (!blockAboveHead && (prevNoBlockAbove == null || !prevNoBlockAbove)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_BLOCK_ABOVE_HEAD);
            }

            // ---- 死亡后 N 秒不复活 ----
            Long deathT = deathTick.get(id);
            if (deathT != null && player.isDead()) {
                long elapsedTicks = server.getTicks() - deathT;
                if (elapsedTicks >= 60 && !notRespawn3sTriggered.getOrDefault(id, false)) {
                    notRespawn3sTriggered.put(id, true);
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NOT_RESPAWN_3S);
                }
                if (elapsedTicks >= 100 && !notRespawn5sTriggered.getOrDefault(id, false)) {
                    notRespawn5sTriggered.put(id, true);
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NOT_RESPAWN_5S);
                }
                if (elapsedTicks >= 200 && !notRespawn10sTriggered.getOrDefault(id, false)) {
                    notRespawn10sTriggered.put(id, true);
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NOT_RESPAWN_10S);
                }
            }

            // ---- 背包物品直接检测 ----
            checkInventoryItem(server, player, id, "coal", wasHasCoal, TriggerType.HAS_COAL);
            checkInventoryItem(server, player, id, "iron_ingot", wasHasIronIngot, TriggerType.HAS_IRON_INGOT);
            checkInventoryItem(server, player, id, "copper_ingot", wasHasCopperIngot, TriggerType.HAS_COPPER_INGOT);
            checkInventoryItem(server, player, id, "crafting_table", wasHasCraftingTable, TriggerType.HAS_CRAFTING_TABLE);
            checkInventoryItem(server, player, id, "furnace", wasHasFurnace, TriggerType.HAS_FURNACE);
            checkInventoryItem(server, player, id, "rotten_flesh", wasHasRottenFlesh, TriggerType.HAS_ROTTEN_FLESH);
            checkInventoryItem(server, player, id, "diamond", wasHasDiamond, TriggerType.HAS_DIAMOND);
            checkInventoryItem(server, player, id, "dirt", wasHasDirt, TriggerType.HAS_DIRT);

            // 斧头（所有以 _axe 结尾的物品）
            if (playerHasItemEndingWith(player, "_axe")) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HAS_AXE);
            }

            // 剑（所有以 _sword 结尾的物品）
            if (playerHasItemEndingWith(player, "_sword")) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HAS_SWORD);
            }

            // 石镐
            checkInventoryItem(server, player, id, "stone_pickaxe", wasHasStonePickaxe, TriggerType.HAS_STONE_PICKAXE);
            // 木镐
            checkInventoryItem(server, player, id, "wooden_pickaxe", wasHasWoodenPickaxe, TriggerType.HAS_WOODEN_PICKAXE);
            // 铁镐
            checkInventoryItem(server, player, id, "iron_pickaxe", wasHasIronPickaxe, TriggerType.HAS_IRON_PICKAXE);

            // ==================== 新增：饥饿度检测（存在性触发）====================

            int hunger = player.getHungerManager().getFoodLevel();
            if (hunger < 18) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HUNGER_BELOW_18);
            }
            if (hunger > 18) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HUNGER_ABOVE_18);
            }

            // ==================== 新增：Y 高度检测（存在性触发）====================

            if (player.getY() > 70) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.Y_ABOVE_70);
            }
            if (player.getY() < 70) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.Y_BELOW_70);
            }

            // ==================== 新增：连续奔跑 30s / 潜行 5s ====================

            long tick = server.getTicks();

            if (sprinting) {
                sprintStartTick.putIfAbsent(id, tick);
                long sprintDuration = (tick - sprintStartTick.get(id)) / 20;
                if (sprintDuration >= 30 && !sprint30sTriggered.getOrDefault(id, false)) {
                    sprint30sTriggered.put(id, true);
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.SPRINT_30S);
                }
            } else {
                sprintStartTick.remove(id);
                sprint30sTriggered.remove(id);
            }

            if (sneaking) {
                sneakStartTick.putIfAbsent(id, tick);
                long sneakDuration = (tick - sneakStartTick.get(id)) / 20;
                if (sneakDuration >= 5 && !sneak5sTriggered.getOrDefault(id, false)) {
                    sneak5sTriggered.put(id, true);
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.SNEAK_5S);
                }
            } else {
                sneakStartTick.remove(id);
                sneak5sTriggered.remove(id);
            }

            // ==================== 新增：跳跃 10 次 ====================

            boolean onGround = player.isOnGround();
            Boolean prevGround = wasOnGround.put(id, onGround);
            // 从地面→离地 = 一次跳跃（不依赖 velocity，服务端 velocity 可能不准确）
            if (!onGround && prevGround != null && prevGround) {
                int jumps = jumpCount.getOrDefault(id, 0) + 1;
                jumpCount.put(id, jumps);
                if (jumps >= 10 && !jump10Triggered.getOrDefault(id, false)) {
                    jump10Triggered.put(id, true);
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.JUMP_10_TIMES);
                }
            }

            // ==================== 新增：距离检测（存在性触发）====================

            List<ServerPlayerEntity> allPlayers = server.getPlayerManager().getPlayerList();
            double minDist = Double.MAX_VALUE;
            for (ServerPlayerEntity other : allPlayers) {
                if (other.getUuid().equals(id)) continue;
                double dist = player.squaredDistanceTo(other);
                if (dist < minDist) minDist = dist;
            }

            // 距离所有玩家 > 15 米（即最近玩家距离也 > 15）
            if (minDist > 225) { // 15^2 = 225
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.FAR_FROM_ALL_15M);
            }

            // 和玩家贴贴（距离 < 2 米）
            if (minDist < 4) { // 2^2 = 4
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.TOO_CLOSE_TO_PLAYER);
            }

            // ==================== 新增：站在花岗岩 / 凝灰岩上 ====================

            boolean onGranite = belowBlockId.equals("granite");
            Boolean prevGranite = wasOnGranite.put(id, onGranite);
            if (onGranite && (prevGranite == null || !prevGranite)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_GRANITE);
            }

            boolean onTuff = belowBlockId.equals("tuff");
            Boolean prevTuff = wasOnTuff.put(id, onTuff);
            if (onTuff && (prevTuff == null || !prevTuff)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_TUFF);
            }

            // ---- 站在基岩上 ----
            boolean onBedrock = belowBlockId.equals("bedrock");
            Boolean prevBedrock = wasOnBedrock.put(id, onBedrock);
            if (onBedrock && (prevBedrock == null || !prevBedrock)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.STAND_ON_BEDROCK);
            }

            // ==================== 新增：经验 / 等级 ====================

            int curLevel = player.experienceLevel;
            float curProgress = player.experienceProgress;

            Integer pLevel = prevExperienceLevel.put(id, curLevel);
            if (pLevel != null && curLevel > pLevel) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.LEVEL_UP);
            }

            Float pProgress = prevExperienceProgress.put(id, curProgress);
            if (pProgress != null && (curProgress > pProgress || curLevel > (pLevel != null ? pLevel : curLevel))) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.GAIN_EXPERIENCE);
            }

            // ==================== 新增：穿装备检测 ====================
            if (isWearingAnyArmor(player)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.WEAR_ARMOR);
            }

            // ==================== 新增：手持物品检测 ====================
            var mainHandStack = player.getMainHandStack();
            if (!mainHandStack.isEmpty()) {
                String heldId = mainHandStack.getItem().getRegistryEntry().registryKey().getValue().getPath();
                if (heldId.equals("crafting_table")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_CRAFTING_TABLE);
                }
                if (heldId.equals("furnace")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_FURNACE);
                }
                if (heldId.equals("wooden_pickaxe")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_WOODEN_PICKAXE);
                }
                if (heldId.equals("iron_pickaxe")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_IRON_PICKAXE);
                }
                if (heldId.equals("stone_pickaxe")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_STONE_PICKAXE);
                }
                if (heldId.equals("wooden_axe")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_WOODEN_AXE);
                }
                if (heldId.equals("stone_axe")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_STONE_AXE);
                }
                if (heldId.equals("iron_axe")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_IRON_AXE);
                }
            }

            // ==================== 新增：快捷栏选择检测 ====================
            int selectedSlot = player.getInventory().selectedSlot;
            if (selectedSlot == 0) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.SELECT_SLOT_FIRST);
            }
            if (selectedSlot == 8) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.SELECT_SLOT_LAST);
            }

            // ==================== 新增：下降5格高度检测 ====================
            if (!onGround) {
                // 玩家在空中
                fallStartY.putIfAbsent(id, curY);
                double startY = fallStartY.get(id);
                if (startY - curY >= 5 && !fallTriggered.getOrDefault(id, false)) {
                    fallTriggered.put(id, true);
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.FALL_5_BLOCKS);
                }
            } else {
                // 玩家落地，重置状态
                fallStartY.remove(id);
                fallTriggered.remove(id);
            }

            // ==================== 新增：背包物品 —— 磨制石材 / 石头 / 凝灰岩 / 树叶 ====================
            checkInventoryItem(server, player, id, "polished_andesite", null, TriggerType.HAS_POLISHED_ANDESITE);
            checkInventoryItem(server, player, id, "polished_granite", null, TriggerType.HAS_POLISHED_GRANITE);
            checkInventoryItem(server, player, id, "polished_diorite", null, TriggerType.HAS_POLISHED_DIORITE);
            checkInventoryItem(server, player, id, "tuff", null, TriggerType.HAS_TUFF);
            checkInventoryItem(server, player, id, "stone", null, TriggerType.HAS_STONE);
            checkInventoryItem(server, player, id, "smooth_stone", null, TriggerType.HAS_SMOOTH_STONE);
            // 树叶（任意种类，ID 以 _leaves 结尾或就是 leaves）
            if (playerHasItemEndingWith(player, "_leaves") || playerHasItem(player, "leaves")) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HAS_LEAVES);
            }

            // ==================== 新增：背包物品 —— 杂项 ====================
            checkInventoryItem(server, player, id, "bone", null, TriggerType.HAS_BONE);
            checkInventoryItem(server, player, id, "string", null, TriggerType.HAS_STRING);
            checkInventoryItem(server, player, id, "ender_pearl", null, TriggerType.HAS_ENDER_PEARL);
            checkInventoryItem(server, player, id, "leather", null, TriggerType.HAS_LEATHER);
            // 羊毛（任意颜色，ID 以 _wool 结尾）
            if (playerHasItemEndingWith(player, "_wool")) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HAS_WOOL);
            }

            // ==================== 新增：背包桶类物品 ====================
            checkInventoryItem(server, player, id, "bucket", null, TriggerType.HAS_BUCKET);
            checkInventoryItem(server, player, id, "water_bucket", null, TriggerType.HAS_WATER_BUCKET);
            checkInventoryItem(server, player, id, "lava_bucket", null, TriggerType.HAS_LAVA_BUCKET);

            // ==================== 新增：背包里没有某类物品 ====================
            if (!playerHasIronToolOrArmor(player)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_IRON_TOOLS_OR_ARMOR);
            }
            if (!playerHasDiamondToolOrArmor(player)) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_DIAMOND_TOOLS_OR_ARMOR);
            }

            // ==================== 新增：副手持盾检测 ====================
            var offhandStack = player.getOffHandStack();
            if (!offhandStack.isEmpty()) {
                String offId = offhandStack.getItem().getRegistryEntry().registryKey().getValue().getPath();
                if (offId.equals("shield")) {
                    GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.HOLD_SHIELD_OFFHAND);
                }
            }

            // ==================== 新增：丢弃30个方块触发检查 ====================
            checkDrop30Trigger(server, player, id);

            // ==================== 新增：不跳/不潜行/不疾跑倒计时 ====================
            // 初始化：首次 tick 记录当前 tick 作为起点
            lastJumpTick.putIfAbsent(id, tick);
            lastSneakActionTick.putIfAbsent(id, tick);
            lastSprintActionTick.putIfAbsent(id, tick);

            // 检测跳跃：离地时更新 lastJumpTick
            if (!onGround && prevGround != null && prevGround) {
                lastJumpTick.put(id, tick);
            }
            // 检测潜行：开始潜行时更新 lastSneakActionTick
            if (sneaking && (prevSneak == null || !prevSneak)) {
                lastSneakActionTick.put(id, tick);
            }
            // 检测疾跑：开始疾跑时更新 lastSprintActionTick
            if (sprinting && (prevSprint == null || !prevSprint)) {
                lastSprintActionTick.put(id, tick);
            }

            long sinceJump = (tick - lastJumpTick.get(id)) / 20;
            long sinceSneak = (tick - lastSneakActionTick.get(id)) / 20;
            long sinceSprint = (tick - lastSprintActionTick.get(id)) / 20;

            if (sinceJump >= 30 && !noJump30sTriggered.getOrDefault(id, false)) {
                noJump30sTriggered.put(id, true);
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_JUMP_30S);
            }
            if (sinceSneak >= 30 && !noSneak30sTriggered.getOrDefault(id, false)) {
                noSneak30sTriggered.put(id, true);
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_SNEAK_30S);
            }
            if (sinceSprint >= 30 && !noSprint30sTriggered.getOrDefault(id, false)) {
                noSprint30sTriggered.put(id, true);
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_SPRINT_30S);
            }
            if (sinceJump >= 60 && !noJump60sTriggered.getOrDefault(id, false)) {
                noJump60sTriggered.put(id, true);
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_JUMP_60S);
            }
            if (sinceSneak >= 60 && !noSneak60sTriggered.getOrDefault(id, false)) {
                noSneak60sTriggered.put(id, true);
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_SNEAK_60S);
            }
            if (sinceSprint >= 60 && !noSprint60sTriggered.getOrDefault(id, false)) {
                noSprint60sTriggered.put(id, true);
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.NO_SPRINT_60S);
            }
        }

        // 清理离线玩家状态
        List<UUID> onlineIds = server.getPlayerManager().getPlayerList().stream()
                .map(p -> (UUID) p.getUuid()).toList();
        wasSneaking.keySet().retainAll(onlineIds);
        wasSprinting.keySet().retainAll(onlineIds);
        wasUsingFood.keySet().retainAll(onlineIds);
        wasLookingDown.keySet().retainAll(onlineIds);
        wasLookingUp.keySet().retainAll(onlineIds);
        wasLookingEast.keySet().retainAll(onlineIds);
        wasLookingSouth.keySet().retainAll(onlineIds);
        wasLookingWest.keySet().retainAll(onlineIds);
        wasLookingNorth.keySet().retainAll(onlineIds);
        lastX.keySet().retainAll(onlineIds);
        lastY.keySet().retainAll(onlineIds);
        lastZ.keySet().retainAll(onlineIds);
        standStillTicks.keySet().retainAll(onlineIds);
        standStillTriggered.keySet().retainAll(onlineIds);
        lastYaw.keySet().retainAll(onlineIds);
        lastPitch.keySet().retainAll(onlineIds);
        lookSameDirTicks.keySet().retainAll(onlineIds);
        lookSameDirTriggered.keySet().retainAll(onlineIds);
        wasEnclosed.keySet().retainAll(onlineIds);
        wasSubmerged.keySet().retainAll(onlineIds);
        wasOnGrass.keySet().retainAll(onlineIds);
        wasOnLeaves.keySet().retainAll(onlineIds);
        wasOnStone.keySet().retainAll(onlineIds);
        wasOnDeepslate.keySet().retainAll(onlineIds);
        wasOnAndesite.keySet().retainAll(onlineIds);
        wasOnDiorite.keySet().retainAll(onlineIds);
        wasFloating.keySet().retainAll(onlineIds);
        deathTick.keySet().retainAll(onlineIds);
        notRespawn3sTriggered.keySet().retainAll(onlineIds);
        notRespawn5sTriggered.keySet().retainAll(onlineIds);
        notRespawn10sTriggered.keySet().retainAll(onlineIds);
        wasHasCoal.keySet().retainAll(onlineIds);
        wasHasIronIngot.keySet().retainAll(onlineIds);
        wasHasCopperIngot.keySet().retainAll(onlineIds);
        wasHasCraftingTable.keySet().retainAll(onlineIds);
        wasHasFurnace.keySet().retainAll(onlineIds);
        wasHasAxe.keySet().retainAll(onlineIds);
        wasHasSword.keySet().retainAll(onlineIds);
        wasHasStonePickaxe.keySet().retainAll(onlineIds);
        wasHasWoodenPickaxe.keySet().retainAll(onlineIds);
        wasHasIronPickaxe.keySet().retainAll(onlineIds);
        wasHasRottenFlesh.keySet().retainAll(onlineIds);
        wasHasDiamond.keySet().retainAll(onlineIds);
        wasHasDirt.keySet().retainAll(onlineIds);
        // 新增
        wasHungerBelow18.keySet().retainAll(onlineIds);
        wasHungerAbove18.keySet().retainAll(onlineIds);
        wasYAbove70.keySet().retainAll(onlineIds);
        wasYBelow70.keySet().retainAll(onlineIds);
        wasOnGranite.keySet().retainAll(onlineIds);
        wasOnTuff.keySet().retainAll(onlineIds);
        wasOnBedrock.keySet().retainAll(onlineIds);
        wasBlockAboveHead.keySet().retainAll(onlineIds);
        wasNoBlockAboveHead.keySet().retainAll(onlineIds);
        wasFarFromAll15m.keySet().retainAll(onlineIds);
        wasTooCloseToPlayer.keySet().retainAll(onlineIds);
        sprintStartTick.keySet().retainAll(onlineIds);
        sneakStartTick.keySet().retainAll(onlineIds);
        jumpCount.keySet().retainAll(onlineIds);
        sprint30sTriggered.keySet().retainAll(onlineIds);
        sneak5sTriggered.keySet().retainAll(onlineIds);
        jump10Triggered.keySet().retainAll(onlineIds);
        prevExperienceLevel.keySet().retainAll(onlineIds);
        prevExperienceProgress.keySet().retainAll(onlineIds);
        wasOnGround.keySet().retainAll(onlineIds);
        lastEatenFoodId.keySet().retainAll(onlineIds);
        fallStartY.keySet().retainAll(onlineIds);
        fallTriggered.keySet().retainAll(onlineIds);
        placeCount.keySet().retainAll(onlineIds);
        place30Triggered.keySet().retainAll(onlineIds);
        dropCount.keySet().retainAll(onlineIds);
        drop30Triggered.keySet().retainAll(onlineIds);
        lastJumpTick.keySet().retainAll(onlineIds);
        lastSneakActionTick.keySet().retainAll(onlineIds);
        lastSprintActionTick.keySet().retainAll(onlineIds);
        noJump30sTriggered.keySet().retainAll(onlineIds);
        noSneak30sTriggered.keySet().retainAll(onlineIds);
        noSprint30sTriggered.keySet().retainAll(onlineIds);
        noJump60sTriggered.keySet().retainAll(onlineIds);
        noSneak60sTriggered.keySet().retainAll(onlineIds);
        noSprint60sTriggered.keySet().retainAll(onlineIds);
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

    /** 判断方块 ID 是否为深板岩（排除矿石变种） */
    private static boolean isDeepslateBlock(String blockId) {
        return blockId.contains("deepslate") && !blockId.contains("_ore");
    }

    /**
     * 判断玩家是否被方块包围在 1x2 空间内
     * 检查六面：脚底 + 头顶 + 脚部四水平方向 + 头部四水平方向
     */
    private static boolean isPlayerEnclosed(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var feetPos = player.getBlockPos();
        var headPos = feetPos.up();
        var belowFeetPos = feetPos.down();
        var aboveHeadPos = headPos.up();

        // 脚底没方块 → 不算自闭
        if (!world.getBlockState(belowFeetPos).isSolidBlock(world, belowFeetPos)) {
            return false;
        }

        // 头顶没方块 → 不算自闭
        if (!world.getBlockState(aboveHeadPos).isSolidBlock(world, aboveHeadPos)) {
            return false;
        }

        // 水平四个方向：东(+X) 西(-X) 南(+Z) 北(-Z)
        var directions = new net.minecraft.util.math.BlockPos[]{
                feetPos.east(), feetPos.west(), feetPos.south(), feetPos.north(),
                headPos.east(), headPos.west(), headPos.south(), headPos.north()
        };

        for (var pos : directions) {
            var state = world.getBlockState(pos);
            if (!state.isSolidBlock(world, pos)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 向上扫描直到世界高度，检查玩家头顶是否有任意非空气方块
     */
    private static boolean hasBlockAboveHead(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var feetPos = player.getBlockPos();
        int startY = feetPos.getY() + 2; // 从头顶上方一格开始
        int topY = world.getDimension().height() - 1;
        for (int y = startY; y <= topY; y++) {
            if (!world.getBlockState(new net.minecraft.util.math.BlockPos(feetPos.getX(), y, feetPos.getZ())).isAir()) {
                return true;
            }
        }
        return false;
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

    /**
     * 通用背包物品存在性检测：检查玩家背包中是否有指定 ID 的物品，
     * 只要有就直接触发（不依赖边缘变化）。
     */
    private static void checkInventoryItem(MinecraftServer server, ServerPlayerEntity player,
                                            UUID id, String itemId,
                                            Map<UUID, Boolean> stateMap, TriggerType type) {
        if (playerHasItem(player, itemId)) {
            GameManager.getInstance().onPlayerTriggered(server, player, type);
        }
    }

    /** 检查玩家背包中是否有指定 ID 的物品 */
    private static boolean playerHasItem(ServerPlayerEntity player, String itemId) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().getRegistryEntry().registryKey().getValue().getPath();
            if (id.equals(itemId)) return true;
        }
        return false;
    }

    /** 检查玩家背包中是否有以指定后缀结尾的物品 */
    private static boolean playerHasItemEndingWith(ServerPlayerEntity player, String suffix) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().getRegistryEntry().registryKey().getValue().getPath();
            if (id.endsWith(suffix)) return true;
        }
        return false;
    }

    /** 判断伤害来源是否为火焰相关 */
    private static boolean isFireDamage(net.minecraft.entity.damage.DamageSource source) {
        String typeId = source.getType().msgId();
        return typeId.equals("inFire") || typeId.equals("onFire")
                || typeId.equals("lava") || typeId.equals("hotFloor")
                || typeId.equals("campfire") || typeId.equals("inFire")
                || typeId.contains("fire") || typeId.contains("flame")
                || typeId.contains("burn") || typeId.contains("magma");
    }

    /** 根据方块物品 ID 返回对应的放置触发类型 */
    private static TriggerType getPlaceBlockType(String itemId) {
        switch (itemId) {
            case "dirt":               return TriggerType.PLACE_DIRT;
            case "cobblestone":        return TriggerType.PLACE_COBBLESTONE;
            case "cobbled_deepslate":  return TriggerType.PLACE_COBBLED_DEEPSLATE;
            case "andesite":           return TriggerType.PLACE_ANDESITE;
            case "granite":            return TriggerType.PLACE_GRANITE;
            case "diorite":            return TriggerType.PLACE_DIORITE;
            case "tuff":               return TriggerType.PLACE_TUFF;
            case "crafting_table":     return TriggerType.PLACE_CRAFTING_TABLE;
            case "furnace":            return TriggerType.PLACE_FURNACE;
            case "chest":              return TriggerType.PLACE_CHEST;
            default:                   return null;
        }
    }

    /** 根据方块 ID 返回对应的打开容器触发类型 */
    private static TriggerType getOpenContainerType(String blockId) {
        switch (blockId) {
            case "chest":
            case "trapped_chest":
            case "ender_chest":
                return TriggerType.OPEN_CHEST;
            case "furnace":
            case "blast_furnace":
            case "smoker":
                return TriggerType.OPEN_FURNACE;
            case "crafting_table":
                return TriggerType.OPEN_CRAFTING_TABLE;
            default:
                return null;
        }
    }

    /** 检查玩家是否穿戴了任意盔甲（头盔/胸甲/护腿/靴子任意一件） */
    private static boolean isWearingAnyArmor(ServerPlayerEntity player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!player.getEquippedStack(slot).isEmpty()) return true;
        }
        return false;
    }

    /** 检查玩家背包/装备栏中是否有铁质工具或防具 */
    private static boolean playerHasIronToolOrArmor(ServerPlayerEntity player) {
        return playerHasItemStartingWith(player, "iron_");
    }

    /** 检查玩家背包/装备栏中是否有钻石工具或防具 */
    private static boolean playerHasDiamondToolOrArmor(ServerPlayerEntity player) {
        return playerHasItemStartingWith(player, "diamond_");
    }

    /** 检查玩家背包中是否有以指定前缀开头的物品 */
    private static boolean playerHasItemStartingWith(ServerPlayerEntity player, String prefix) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().getRegistryEntry().registryKey().getValue().getPath();
            if (id.startsWith(prefix)) return true;
        }
        return false;
    }

    /** 重置指定玩家的跳跃计数（词条变为跳跃10次时调用） */
    public static void resetJumpCount(UUID playerId) {
        jumpCount.remove(playerId);
        jump10Triggered.remove(playerId);
    }

    /** 重置指定玩家的持续望向同方向状态（词条变为持续看向同一方向五秒时调用） */
    public static void resetLookSameDir(UUID playerId) {
        lastYaw.remove(playerId);
        lastPitch.remove(playerId);
        lookSameDirTicks.remove(playerId);
        lookSameDirTriggered.remove(playerId);
    }

    /** 重置指定玩家的放置计数（词条变为放置30个方块时调用） */
    public static void resetPlaceCount(UUID playerId) {
        placeCount.remove(playerId);
        place30Triggered.remove(playerId);
    }

    /** 重置指定玩家的丢弃计数（词条变为丢弃30个方块时调用） */
    public static void resetDropCount(UUID playerId) {
        dropCount.remove(playerId);
        drop30Triggered.remove(playerId);
    }

    /** 增加指定玩家的丢弃计数（由 Mixin 调用） */
    public static void incrementDropCount(UUID playerId) {
        int cnt = dropCount.getOrDefault(playerId, 0) + 1;
        dropCount.put(playerId, cnt);
    }

    /** 检查并触发丢弃30个方块（由 tick 调用） */
    private static void checkDrop30Trigger(MinecraftServer server, ServerPlayerEntity player, UUID id) {
        if (dropCount.getOrDefault(id, 0) >= 30 && !drop30Triggered.getOrDefault(id, false)) {
            drop30Triggered.put(id, true);
            GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.DROP_30_ITEMS);
        }
    }

    /** 重置指定玩家的不跳倒计时（词条变为不跳类时调用） */
    public static void resetNoJumpState(UUID playerId) {
        lastJumpTick.remove(playerId);
        noJump30sTriggered.remove(playerId);
        noJump60sTriggered.remove(playerId);
    }

    /** 重置指定玩家的不潜行倒计时（词条变为不潜行类时调用） */
    public static void resetNoSneakState(UUID playerId) {
        lastSneakActionTick.remove(playerId);
        noSneak30sTriggered.remove(playerId);
        noSneak60sTriggered.remove(playerId);
    }

    /** 重置指定玩家的不疾跑倒计时（词条变为不疾跑类时调用） */
    public static void resetNoSprintState(UUID playerId) {
        lastSprintActionTick.remove(playerId);
        noSprint30sTriggered.remove(playerId);
        noSprint60sTriggered.remove(playerId);
    }

    /** 重置指定玩家的头顶方块状态（词条变为头顶有/无方块遮挡时调用，使其立即触发） */
    public static void resetBlockAboveHeadState(UUID playerId) {
        wasBlockAboveHead.remove(playerId);
        wasNoBlockAboveHead.remove(playerId);
    }

    /** 清空所有状态 Map，在新游戏开始时调用 */
    public static void clearAllState() {
        wasSneaking.clear();
        wasSprinting.clear();
        wasUsingFood.clear();
        wasLookingDown.clear();
        wasLookingUp.clear();
        wasLookingEast.clear();
        wasLookingSouth.clear();
        wasLookingWest.clear();
        wasLookingNorth.clear();
        lastX.clear();
        lastY.clear();
        lastZ.clear();
        standStillTicks.clear();
        standStillTriggered.clear();
        lastYaw.clear();
        lastPitch.clear();
        lookSameDirTicks.clear();
        lookSameDirTriggered.clear();
        wasEnclosed.clear();
        wasSubmerged.clear();
        wasOnGrass.clear();
        wasOnLeaves.clear();
        wasOnStone.clear();
        wasOnDeepslate.clear();
        wasOnAndesite.clear();
        wasOnDiorite.clear();
        wasFloating.clear();
        deathTick.clear();
        notRespawn3sTriggered.clear();
        notRespawn5sTriggered.clear();
        notRespawn10sTriggered.clear();
        wasHasCoal.clear();
        wasHasIronIngot.clear();
        wasHasCopperIngot.clear();
        wasHasCraftingTable.clear();
        wasHasFurnace.clear();
        wasHasAxe.clear();
        wasHasSword.clear();
        wasHasStonePickaxe.clear();
        wasHasWoodenPickaxe.clear();
        wasHasIronPickaxe.clear();
        wasHasRottenFlesh.clear();
        wasHasDiamond.clear();
        wasHasDirt.clear();
        // 新增
        wasHungerBelow18.clear();
        wasHungerAbove18.clear();
        wasYAbove70.clear();
        wasYBelow70.clear();
        wasOnGranite.clear();
        wasOnTuff.clear();
        wasOnBedrock.clear();
        wasBlockAboveHead.clear();
        wasNoBlockAboveHead.clear();
        wasFarFromAll15m.clear();
        wasTooCloseToPlayer.clear();
        sprintStartTick.clear();
        sneakStartTick.clear();
        jumpCount.clear();
        sprint30sTriggered.clear();
        sneak5sTriggered.clear();
        jump10Triggered.clear();
        prevExperienceLevel.clear();
        prevExperienceProgress.clear();
        wasOnGround.clear();
        lastEatenFoodId.clear();
        fallStartY.clear();
        fallTriggered.clear();
        placeCount.clear();
        place30Triggered.clear();
        dropCount.clear();
        drop30Triggered.clear();
        lastJumpTick.clear();
        lastSneakActionTick.clear();
        lastSprintActionTick.clear();
        noJump30sTriggered.clear();
        noSneak30sTriggered.clear();
        noSprint30sTriggered.clear();
        noJump60sTriggered.clear();
        noSneak60sTriggered.clear();
        noSprint60sTriggered.clear();
    }
}

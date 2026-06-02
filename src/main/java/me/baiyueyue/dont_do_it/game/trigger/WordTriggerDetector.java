package me.baiyueyue.dont_do_it.game.trigger;

import me.baiyueyue.dont_do_it.game.GameManager;
import me.baiyueyue.dont_do_it.game.TriggerType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
            Boolean prevUsingFood = wasUsingFood.put(id, usingFood);
            if (!usingFood && prevUsingFood != null && prevUsingFood) {
                GameManager.getInstance().onPlayerTriggered(server, player, TriggerType.EAT);
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
    }
}

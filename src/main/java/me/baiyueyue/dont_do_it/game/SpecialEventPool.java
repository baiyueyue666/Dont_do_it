package me.baiyueyue.dont_do_it.game;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

import java.util.*;

/**
 * 特殊事件池 —— 管理特殊事件的随机抽取和执行
 */
public class SpecialEventPool {

    private final List<SpecialEventType> eventTypes = new ArrayList<>();
    private final java.util.Random random = new java.util.Random();

    /** 脚下出矿：记录每个世界中被替换的方块位置和原始状态，用于事件结束时恢复 */
    private final Map<ServerWorld, Map<BlockPos, BlockState>> oreUnderfootBlocks = new HashMap<>();

    /** 全员南瓜头：记录每个玩家原始头盔物品，用于事件结束时恢复 */
    private final Map<UUID, ItemStack> pumpkinHeadOriginals = new HashMap<>();

    /** 囚笼试炼：记录玩家周围的铁栅栏位置，事件结束时移除 */
    private final Map<UUID, Set<BlockPos>> cageBlocks = new HashMap<>();
    /** 囚笼试炼：记录需要生成苦力怕的玩家及其笼内位置（5秒后触发），null 表示已生成 */
    private final Map<UUID, BlockPos> cageCreeperSpawns = new HashMap<>();
    /** 高空落水挑战：正在被追踪的玩家 */
    private final Set<UUID> skyWaterTracking = new HashSet<>();

    /** 粘液附身：记录被替换的方块位置和原始状态，事件结束时恢复 */
    private final Map<UUID, Map<BlockPos, BlockState>> slimeBlocks = new HashMap<>();
    /** 箭雨试炼：记录事件开始时玩家的血量 */
    private final Map<UUID, Float> arrowTrialInitialHealth = new HashMap<>();

    /** 全员变幼体：记录每个玩家的原始移动速度和跳跃强度 */
    private final Map<UUID, double[]> everyoneBabyOriginalAttrs = new HashMap<>();
    /** 交易商人：记录每个玩家生成的村民 */
    private final Map<UUID, VillagerEntity> tradeMerchantVillagers = new HashMap<>();

    public SpecialEventPool() {
        initDefaultEvents();
    }

    private void initDefaultEvents() {
        eventTypes.add(SpecialEventType.MONSTER_RAMPAGE);
        eventTypes.add(SpecialEventType.DIAMOND_GIFT);
        eventTypes.add(SpecialEventType.DIAMOND_BLESSING);
        eventTypes.add(SpecialEventType.DIAMOND_CURSE);
        eventTypes.add(SpecialEventType.ECLIPSE_CURSE);
        eventTypes.add(SpecialEventType.CALM);
        eventTypes.add(SpecialEventType.CLOUD_EFFECT);
        eventTypes.add(SpecialEventType.FOOD_RAIN);
        eventTypes.add(SpecialEventType.XP_STORM);
        eventTypes.add(SpecialEventType.LIFE_BLESSING);
        eventTypes.add(SpecialEventType.ORE_UNDERFOOT);
        eventTypes.add(SpecialEventType.ANVIL_STORM);
        eventTypes.add(SpecialEventType.TNT_RAIN);
        eventTypes.add(SpecialEventType.CAVE_IN);
        eventTypes.add(SpecialEventType.PUMPKIN_HEAD);
        eventTypes.add(SpecialEventType.INVENTORY_SHUFFLE);
        eventTypes.add(SpecialEventType.CHICKEN_RAIN);
        eventTypes.add(SpecialEventType.PLAYER_SWAP);
        eventTypes.add(SpecialEventType.FIRE_TRAIL);
        eventTypes.add(SpecialEventType.CAGE_TRIAL);
        eventTypes.add(SpecialEventType.SKY_WATER_CHALLENGE);
        eventTypes.add(SpecialEventType.CROP_SPEED_GROW);
        eventTypes.add(SpecialEventType.DURABILITY_BLESSING);
        eventTypes.add(SpecialEventType.EQUIPMENT_RUST);
        eventTypes.add(SpecialEventType.HUNGER_DISEASE);
        eventTypes.add(SpecialEventType.INVENTORY_MIGRATION);
        eventTypes.add(SpecialEventType.EVERYONE_BABY);
        eventTypes.add(SpecialEventType.SLIME_POSSESSION);
        eventTypes.add(SpecialEventType.ARROW_TRIAL);
        eventTypes.add(SpecialEventType.TRADE_MERCHANT);
    }

    /** 随机抽取一个特殊事件（按权重加权随机） */
    public SpecialEventType drawRandom() {
        int totalWeight = 0;
        for (SpecialEventType type : eventTypes) {
            totalWeight += type.getWeight();
        }
        int rand = random.nextInt(totalWeight);
        for (SpecialEventType type : eventTypes) {
            rand -= type.getWeight();
            if (rand < 0) return type;
        }
        return eventTypes.get(random.nextInt(eventTypes.size())); // fallback
    }

    public int size() { return eventTypes.size(); }

    /** 根据显示名称查找特殊事件类型 */
    public SpecialEventType findByName(String name) {
        for (SpecialEventType type : eventTypes) {
            if (type.getDisplayName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    // ==================== 事件执行 ====================

    /**
     * 执行特殊事件
     * @return 事件实际持续时间（秒），0 表示瞬时完成
     */
    public int executeEvent(MinecraftServer server, SpecialEventType type) {
        return switch (type) {
            case MONSTER_RAMPAGE -> executeMonsterRampage(server);
            case DIAMOND_GIFT -> executeDiamondGift(server);
            case DIAMOND_BLESSING -> executeDiamondBlessing(server);
            case DIAMOND_CURSE -> executeDiamondCurse(server);
            case ECLIPSE_CURSE -> executeEclipseCurse(server);
            case CALM -> executeCalm(server);
            case CLOUD_EFFECT -> executeCloudEffect(server);
            case FOOD_RAIN -> executeFoodRain(server);
            case XP_STORM -> executeXpStorm(server);
            case LIFE_BLESSING -> executeLifeBlessing(server);
            case ORE_UNDERFOOT -> executeOreUnderfoot(server);
            case ANVIL_STORM -> executeAnvilStorm(server);
            case TNT_RAIN -> executeTntRain(server);
            case CAVE_IN -> executeCaveIn(server);
            case PUMPKIN_HEAD -> executePumpkinHead(server);
            case INVENTORY_SHUFFLE -> executeInventoryShuffle(server);
            case CHICKEN_RAIN -> executeChickenRain(server);
            case PLAYER_SWAP -> executePlayerSwap(server);
            case FIRE_TRAIL -> executeFireTrail(server);
            case CAGE_TRIAL -> executeCageTrial(server);
            case SKY_WATER_CHALLENGE -> executeSkyWaterChallenge(server);
            case CROP_SPEED_GROW -> executeCropSpeedGrow(server);
            case DURABILITY_BLESSING -> executeDurabilityBlessing(server);
            case EQUIPMENT_RUST -> executeEquipmentRust(server);
            case HUNGER_DISEASE -> executeHungerDisease(server);
            case INVENTORY_MIGRATION -> executeInventoryMigration(server);
            case EVERYONE_BABY -> executeEveryoneBaby(server);
            case SLIME_POSSESSION -> executeSlimePossession(server);
            case ARROW_TRIAL -> executeArrowTrial(server);
            case TRADE_MERCHANT -> executeTradeMerchant(server);
        };
    }

    /**
     * 怪物狂潮：在每个存活玩家周围生成怪物
     * 怪物数量和种类随游戏进程增强
     */
    private int executeMonsterRampage(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        int aliveCount = 0;

        // 先统计存活玩家
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data != null && !data.isEliminated()) {
                aliveCount++;
            }
        }

        if (aliveCount == 0) return 0;

        // 可生成的敌对生物类型
        EntityType<?>[] monsterTypes = {
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                EntityType.CREEPER, EntityType.WITCH, EntityType.ENDERMAN,
                EntityType.HUSK, EntityType.STRAY
        };

        int monstersPerPlayer = 3; // 每个玩家周围生成 3 只

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;

            ServerWorld world = (ServerWorld) player.getEntityWorld();

            for (int i = 0; i < monstersPerPlayer; i++) {
                EntityType<?> type = monsterTypes[random.nextInt(monsterTypes.length)];
                // 在玩家周围 3-10 格随机位置生成
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 3 + random.nextDouble() * 7;
                double spawnX = player.getX() + Math.cos(angle) * distance;
                double spawnZ = player.getZ() + Math.sin(angle) * distance;
                double spawnY = player.getY();

                BlockPos pos = new BlockPos((int) spawnX, (int) spawnY, (int) spawnZ);
                // 找一个安全的地面位置
                for (int yOff = 0; yOff < 5; yOff++) {
                    BlockPos checkPos = pos.down(yOff);
                    if (world.getBlockState(checkPos).isSolidBlock(world, checkPos)) {
                        pos = checkPos.up();
                        break;
                    }
                }

                var entity = type.create(world, SpawnReason.EVENT);
                if (entity != null) {
                    entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                            random.nextFloat() * 360, 0);
                    world.spawnEntity(entity);
                }
            }

            player.sendMessage(Text.literal("§c⚡ 怪物狂潮！怪物正在你身边生成！"), true);
        }

        server.getPlayerManager().broadcast(
                Text.literal("§4☠ 特殊事件「怪物狂潮」已触发！每位存活玩家周围生成 §c" + monstersPerPlayer + " 只 §4怪物！"),
                false);

        return 0; // 瞬时事件，无持续
    }

    // ==================== 持续事件每 tick 动作 ====================

    /**
     * 持续事件每 tick 调用（由 GameManager.tickSpecialEvent 调用）
     * @param remainingSeconds 剩余秒数（递减前）
     */
    public void tickActiveEvent(MinecraftServer server, SpecialEventType type, int remainingSeconds) {
        switch (type) {
            case FOOD_RAIN -> tickFoodRain(server);
            case XP_STORM -> tickXpStorm(server);
            case ANVIL_STORM -> tickAnvilStorm(server);
            case CAVE_IN -> tickCaveIn(server);
            case FIRE_TRAIL -> tickFireTrail(server);
            case CAGE_TRIAL -> tickCageTrial(server, remainingSeconds);
            case SKY_WATER_CHALLENGE -> tickSkyWaterChallenge(server);
            case CROP_SPEED_GROW -> tickCropSpeedGrow(server);
            case HUNGER_DISEASE -> tickHungerDisease(server);
            case SLIME_POSSESSION -> tickSlimePossession(server);
            case ARROW_TRIAL -> tickArrowTrial(server);
            default -> {} // 其他事件不需要 per-tick 逻辑
        }
    }

    /**
     * 事件结束时调用（由 GameManager.tickSpecialEvent 调用，在 remaining 归零时触发）
     */
    public void onEventEnd(MinecraftServer server, SpecialEventType type) {
        switch (type) {
            case DIAMOND_BLESSING -> endDiamondBlessing();
            case ORE_UNDERFOOT -> endOreUnderfoot(server);
            case PUMPKIN_HEAD -> endPumpkinHead(server);
            case CAGE_TRIAL -> endCageTrial(server);
            case SKY_WATER_CHALLENGE -> endSkyWaterChallenge(server);
            case DURABILITY_BLESSING -> endDurabilityBlessing();
            case EQUIPMENT_RUST -> endEquipmentRust();
            case HUNGER_DISEASE -> endHungerDisease();
            case EVERYONE_BABY -> endEveryoneBaby(server);
            case SLIME_POSSESSION -> endSlimePossession(server);
            case ARROW_TRIAL -> endArrowTrial(server);
            case TRADE_MERCHANT -> endTradeMerchant(server);
            default -> {} // 其他事件无需清理
        }
    }

    // ==================== 钻石馈赠 ====================

    private int executeDiamondGift(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        int count = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            player.getInventory().insertStack(new ItemStack(Items.DIAMOND, 15));
            count++;
            player.sendMessage(Text.literal("§b💎 钻石馈赠！你获得了 §e15颗钻石 §b！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§b💎 特殊事件「钻石馈赠」已触发！每位存活玩家获得 §e15颗钻石 §b！"), false);
        return 0;
    }

    // ==================== 钻石祝福 ====================

    private int executeDiamondBlessing(MinecraftServer server) {
        GameManager.getInstance().setDiamondBlessingActive(true);
        server.getPlayerManager().broadcast(
                Text.literal("§b💎 特殊事件「钻石祝福」已触发！§e2分钟内 §b挖钻石矿可回复一颗心！"), false);
        return SpecialEventType.DIAMOND_BLESSING.getDurationSeconds();
    }

    private void endDiamondBlessing() {
        GameManager.getInstance().setDiamondBlessingActive(false);
    }

    // ==================== 钻石诅咒 ====================

    private int executeDiamondCurse(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        int totalLost = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;

            int diamondCount = countItemInInventory(player, "diamond");
            if (diamondCount > 0) {
                int lost = Math.min(diamondCount, data.getHearts());
                for (int i = 0; i < lost; i++) {
                    data.loseHeart();
                }
                totalLost += lost;
                gm.syncPlayerDataPublic(server, data);
                player.sendMessage(Text.literal("§c💀 钻石诅咒！背包里 §e" + diamondCount + "颗钻石 §c扣除了你 §4" + lost + "颗心 §c！剩余 §c" + data.getHearts() + " ❤️"), true);

                if (data.isEliminated()) {
                    player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                    server.getPlayerManager().broadcast(
                            Text.literal("§c💀 " + data.getTeamColor().getDisplayName() + " §f因钻石诅咒被淘汰！"), false);
                }
            } else {
                player.sendMessage(Text.literal("§a😌 钻石诅咒！但你背包里没有钻石，安然无恙！"), true);
            }
        }
        server.getPlayerManager().broadcast(
                Text.literal("§c💀 特殊事件「钻石诅咒」已触发！共扣除 §4" + totalLost + "颗心 §c！"), false);

        // 检查胜利条件
        gm.checkWinConditionPublic(server);
        return 0;
    }

    // ==================== 日食诅咒 ====================

    private int executeEclipseCurse(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60 * 20, 0));
            player.sendMessage(Text.literal("§8🌑 日食诅咒！你什么都看不见了..."), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§8🌑 特殊事件「日食诅咒」已触发！所有玩家获得 §e1分钟失明 §8效果！"), false);
        return 0;
    }

    // ==================== 平静 ====================

    private int executeCalm(MinecraftServer server) {
        server.getPlayerManager().broadcast(
                Text.literal("§7☁ 特殊事件「平静」已触发！无事发生，倒计时重新开始..."), false);
        return 0;
    }

    // ==================== 唉，云朵？ ====================

    private int executeCloudEffect(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 30 * 20, 0));
            player.sendMessage(Text.literal("☁ 唉，云朵？你飘起来了！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("☁ 特殊事件「唉，云朵？」已触发！所有玩家获得 §e30秒漂浮 §f效果！"), false);
        return 0;
    }

    // ==================== 美食雨 ====================

    private int executeFoodRain(MinecraftServer server) {
        server.getPlayerManager().broadcast(
                Text.literal("🍖 特殊事件「美食雨」已触发！美食从天而降，持续 §e10秒 §f！"), false);
        return SpecialEventType.FOOD_RAIN.getDurationSeconds();
    }

    private void tickFoodRain(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        Item[] foodItems = {Items.COOKED_BEEF, Items.BREAD, Items.GOLDEN_CARROT};

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;

            ServerWorld world = (ServerWorld) player.getEntityWorld();
            // 每个玩家每 tick 掉落 1-2 个食物
            int count = 1 + random.nextInt(2);
            for (int i = 0; i < count; i++) {
                ItemStack food = new ItemStack(foodItems[random.nextInt(foodItems.length)]);
                double x = player.getX() + (random.nextDouble() - 0.5) * 6;
                double y = player.getY() + 8 + random.nextDouble() * 4;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 6;
                ItemEntity itemEntity = new ItemEntity(world, x, y, z, food);
                itemEntity.setVelocity(0, -0.2, 0);
                world.spawnEntity(itemEntity);
            }
        }
    }

    // ==================== 经验风暴 ====================

    private int executeXpStorm(MinecraftServer server) {
        server.getPlayerManager().broadcast(
                Text.literal("🌟 特殊事件「经验风暴」已触发！经验球从天而降，持续 §e10秒 §f！"), false);
        return SpecialEventType.XP_STORM.getDurationSeconds();
    }

    private void tickXpStorm(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;

            ServerWorld world = (ServerWorld) player.getEntityWorld();
            // 每秒产出 125 个经验球：每 tick 必出 6 个，额外 25% 概率再出 1 个（平均 6.25/tick = 125/s）
            int count = 6 + (random.nextFloat() < 0.25f ? 1 : 0);
            for (int i = 0; i < count; i++) {
                int xpValue = 3 + random.nextInt(8); // 3-10 XP each
                double x = player.getX() + (random.nextDouble() - 0.5) * 6;
                double y = player.getY() + 6 + random.nextDouble() * 4;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 6;
                ExperienceOrbEntity orb = new ExperienceOrbEntity(world, x, y, z, xpValue);
                world.spawnEntity(orb);
            }
        }
    }

    // ==================== 生命赐福 ====================

    private int executeLifeBlessing(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            // 生命恢复 II，持续 10 秒
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 10 * 20, 1));
            player.sendMessage(Text.literal("§d💚 生命赐福！你获得了生命恢复效果！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§d💚 特殊事件「生命赐福」已触发！所有玩家获得 §e10秒生命恢复 §d！"), false);
        return SpecialEventType.LIFE_BLESSING.getDurationSeconds();
    }

    // ==================== 脚下出矿 ====================

    private int executeOreUnderfoot(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        Block[] oreTypes = {
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
                Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE
        };

        oreUnderfootBlocks.clear();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;

            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos feetPos = player.getBlockPos();

            var worldBlocks = oreUnderfootBlocks.computeIfAbsent(world, k -> new HashMap<>());

            // 玩家周围 5×5×5 范围全部替换为矿物方块
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos pos = feetPos.add(dx, dy, dz);

                        // 跳过已记录的位置（多个玩家范围重叠时避免覆盖）
                        if (worldBlocks.containsKey(pos)) continue;

                        BlockState existing = world.getBlockState(pos);
                        if (existing.isAir()) continue; // 空气不替换

                        worldBlocks.put(pos, existing);
                        Block oreType = oreTypes[random.nextInt(oreTypes.length)];
                        world.setBlockState(pos, oreType.getDefaultState());
                    }
                }
            }

            player.sendMessage(Text.literal("⛏ 脚下出矿！你周围 5×5×5 的方块变成了矿物，§e10秒后恢复 §f！"), true);
        }

        server.getPlayerManager().broadcast(
                Text.literal("⛏ 特殊事件「脚下出矿」已触发！玩家周围 5×5×5 方块变为矿物，§e10秒后恢复 §f！"), false);
        return SpecialEventType.ORE_UNDERFOOT.getDurationSeconds();
    }

    private void endOreUnderfoot(MinecraftServer server) {
        for (var entry : oreUnderfootBlocks.entrySet()) {
            ServerWorld world = entry.getKey();
            for (var blockEntry : entry.getValue().entrySet()) {
                BlockPos pos = blockEntry.getKey();
                // 仅当该位置仍为矿物方块时才恢复（被玩家挖掉的不恢复）
                if (isOreBlock(world.getBlockState(pos))) {
                    world.setBlockState(pos, blockEntry.getValue());
                }
            }
        }
        oreUnderfootBlocks.clear();
    }

    /** 判断方块是否为矿物（含原版矿石和深板岩变种） */
    private boolean isOreBlock(BlockState state) {
        String id = state.getBlock().getRegistryEntry().registryKey().getValue().getPath();
        return id.contains("_ore") || id.equals("ancient_debris");
    }

    // ==================== 工具方法 ====================

    /** 统计玩家背包中指定 ID 的物品数量 */
    private int countItemInInventory(ServerPlayerEntity player, String itemId) {
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().getRegistryEntry().registryKey().getValue().getPath();
            if (id.equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // ==================== 铁砧暴雨 ====================

    private int executeAnvilStorm(MinecraftServer server) {
        server.getPlayerManager().broadcast(
                Text.literal("§7🔨 特殊事件「铁砧暴雨」已触发！铁砧从天而降，持续 §e10秒 §f！"), false);
        return SpecialEventType.ANVIL_STORM.getDurationSeconds();
    }

    private void tickAnvilStorm(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            // 每秒生成 50 个铁砧：每 tick 生成 2 个，额外 50% 概率再出 1 个（平均 2.5/tick = 50/s）
            int count = 2 + (random.nextFloat() < 0.5f ? 1 : 0);
            for (int i = 0; i < count; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 10;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 10;
                double y = player.getY() + 10;
                BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
                FallingBlockEntity anvil = FallingBlockEntity.spawnFromBlock(world, pos, Blocks.ANVIL.getDefaultState());
                anvil.setHurtEntities(2.0f, 40);
            }
            // 清理已落地的铁砧方块（在玩家周围 10×10 范围扫描并破坏铁砧）
            BlockPos playerPos = player.getBlockPos();
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    for (int dy = -5; dy <= 0; dy++) {
                        BlockPos checkPos = playerPos.add(dx, dy, dz);
                        BlockState bs = world.getBlockState(checkPos);
                        if (bs.getBlock() == Blocks.ANVIL || bs.getBlock() == Blocks.CHIPPED_ANVIL
                                || bs.getBlock() == Blocks.DAMAGED_ANVIL) {
                            world.breakBlock(checkPos, false);
                        }
                    }
                }
            }
        }
    }

    // ==================== TNT降雨 ====================

    private int executeTntRain(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        int totalTnt = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            // 每个玩家周围生成 20 个点燃的 TNT
            for (int i = 0; i < 20; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 12;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 12;
                double y = player.getY() + 15 + random.nextDouble() * 10;
                TntEntity tnt = new TntEntity(world, x, y, z, player);
                tnt.setFuse(40 + random.nextInt(40));
                world.spawnEntity(tnt);
                totalTnt++;
            }
            player.sendMessage(Text.literal("§c💣 TNT降雨！小心头上！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§c💣 特殊事件「TNT降雨」已触发！§4点燃的TNT §c从天而降！共 §e" + totalTnt + "个 §c！"), false);
        return 0;
    }

    // ==================== 地底塌陷 ====================

    private int executeCaveIn(MinecraftServer server) {
        server.getPlayerManager().broadcast(
                Text.literal("§8⛏ 特殊事件「地底塌陷」已触发！脚下方块持续破碎，持续 §e30秒 §f！"), false);
        return SpecialEventType.CAVE_IN.getDurationSeconds();
    }

    private void tickCaveIn(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            // 破坏玩家脚底 3×3 范围的方块（脚底 = 脚所在位置往下一格）
            BlockPos feetPos = player.getBlockPos();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = feetPos.add(dx, -1, dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.getBlock() == Blocks.BEDROCK) continue;
                    if (state.getBlock().getHardness() < 0) continue; // 不可破坏方块
                    world.breakBlock(pos, true);
                }
            }
        }
    }

    // ==================== 全员南瓜头 ====================

    private int executePumpkinHead(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        pumpkinHeadOriginals.clear();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            // 保存原始头盔
            ItemStack originalHelmet = player.getEquippedStack(EquipmentSlot.HEAD).copy();
            pumpkinHeadOriginals.put(player.getUuid(), originalHelmet);
            // 强制戴上带绑定诅咒的南瓜头（无法手动摘除）
            ItemStack pumpkin = new ItemStack(Blocks.CARVED_PUMPKIN);
            RegistryWrapper.Impl<Enchantment> wrapper = server.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT).orElseThrow();
            RegistryEntry<Enchantment> bindingCurse = wrapper.getOptional(Enchantments.BINDING_CURSE).orElseThrow();
            pumpkin.addEnchantment(bindingCurse, 1);
            player.equipStack(EquipmentSlot.HEAD, pumpkin);
            player.sendMessage(Text.literal("§6🎃 全员南瓜头！带绑定诅咒，无法摘除！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§6🎃 特殊事件「全员南瓜头」已触发！所有玩家强制戴上南瓜头盔，持续 §e60秒 §f！"), false);
        return SpecialEventType.PUMPKIN_HEAD.getDurationSeconds();
    }

    private void endPumpkinHead(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null) continue;
            // 移除南瓜头，恢复原始头盔
            ItemStack currentHelmet = player.getEquippedStack(EquipmentSlot.HEAD);
            if (!currentHelmet.isEmpty() && currentHelmet.getItem() == Blocks.CARVED_PUMPKIN.asItem()) {
                ItemStack original = pumpkinHeadOriginals.get(player.getUuid());
                player.equipStack(EquipmentSlot.HEAD, original != null ? original : ItemStack.EMPTY);
            }
            player.sendMessage(Text.literal("§a🎃 南瓜头已消失！视野恢复！"), true);
        }
        pumpkinHeadOriginals.clear();
    }

    // ==================== 物品栏洗牌 ====================

    private int executeInventoryShuffle(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            // 收集快捷栏 0-8 槽位物品
            List<ItemStack> hotbarItems = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                hotbarItems.add(player.getInventory().getStack(i).copy());
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
            // 随机打乱
            Collections.shuffle(hotbarItems, random);
            // 放回
            for (int i = 0; i < 9; i++) {
                player.getInventory().setStack(i, hotbarItems.get(i));
            }
            player.sendMessage(Text.literal("§d🔀 物品栏洗牌！你的快捷栏物品已被随机打乱！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§d🔀 特殊事件「物品栏洗牌」已触发！所有玩家快捷栏物品被随机打乱！"), false);
        return 0;
    }

    // ==================== 小鸡天降 ====================

    private int executeChickenRain(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        int totalChickens = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            // 每个玩家生成 50 只小鸡
            for (int i = 0; i < 50; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 12;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 12;
                double y = player.getY() + 8 + random.nextDouble() * 12;
                var chicken = EntityType.CHICKEN.create(world, SpawnReason.EVENT);
                if (chicken != null) {
                    chicken.refreshPositionAndAngles(x, y, z, random.nextFloat() * 360, 0);
                    world.spawnEntity(chicken);
                    totalChickens++;
                }
            }
            player.sendMessage(Text.literal("🐔 小鸡天降！小鸡正在你周围落下！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("🐔 特殊事件「小鸡天降」已触发！共 §e" + totalChickens + "只 §f小鸡从天而降！"), false);
        return 0;
    }

    // ==================== 玩家互换位置 ====================

    private int executePlayerSwap(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        // 收集所有存活玩家
        List<ServerPlayerEntity> alivePlayers = new ArrayList<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data != null && !data.isEliminated()) {
                alivePlayers.add(player);
            }
        }

        int count = alivePlayers.size();
        if (count < 2) {
            server.getPlayerManager().broadcast(
                    Text.literal("§d🔄 特殊事件「玩家互换位置」已触发！但存活玩家不足2人，无事发生。"), false);
            return 0;
        }

        // 收集所有位置和视角
        double[][] pos = new double[count][3];
        float[] yawArr = new float[count];
        float[] pitchArr = new float[count];
        for (int i = 0; i < count; i++) {
            ServerPlayerEntity p = alivePlayers.get(i);
            pos[i][0] = p.getX();
            pos[i][1] = p.getY();
            pos[i][2] = p.getZ();
            yawArr[i] = p.getYaw();
            pitchArr[i] = p.getPitch();
        }

        // 循环右移一位：每人一定换到另一个玩家的位置
        for (int i = 0; i < count; i++) {
            int srcIdx = (i + count - 1) % count; // 前一个人的位置
            ServerPlayerEntity player = alivePlayers.get(i);
            // 使用 requestTeleport 确保可靠传送（vanilla /tp 底层 API）
            player.requestTeleport(pos[srcIdx][0], pos[srcIdx][1], pos[srcIdx][2]);
            player.setYaw(yawArr[i]);
            player.setPitch(pitchArr[i]);
            player.sendMessage(Text.literal("§d🔄 你被传送到了另一个玩家的位置！"), true);
        }

        server.getPlayerManager().broadcast(
                Text.literal("§d🔄 特殊事件「玩家互换位置」已触发！§e" + count
                        + "名 §f玩家位置已被交换！"), false);
        return 0;
    }

    // ==================== 脚步生火 ====================

    private int executeFireTrail(MinecraftServer server) {
        server.getPlayerManager().broadcast(
                Text.literal("§c🔥 特殊事件「脚步生火」已触发！踩到的方块会起火，持续 §e30秒 §f！"), false);
        return SpecialEventType.FIRE_TRAIL.getDurationSeconds();
    }

    private void tickFireTrail(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos feetPos = player.getBlockPos();
            BlockPos groundPos = feetPos.down();
            // 玩家脚底接触到的方块直接起火
            if (world.getBlockState(groundPos).isSolidBlock(world, groundPos)) {
                world.setBlockState(feetPos, Blocks.FIRE.getDefaultState());
            }
        }
    }

    // ==================== 囚笼试炼 ====================

    private int executeCageTrial(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        cageBlocks.clear();
        cageCreeperSpawns.clear();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos feetPos = player.getBlockPos();
            Set<BlockPos> blocks = new HashSet<>();
            // 建造 3×3×4 中空铁栅栏笼子（玩家在中心 1×1 空间内）
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue; // 中心留空
                        BlockPos pos = feetPos.add(dx, dy, dz);
                        if (world.getBlockState(pos).isAir()) {
                            world.setBlockState(pos, Blocks.IRON_BARS.getDefaultState());
                            blocks.add(pos);
                        }
                    }
                }
            }
            cageBlocks.put(player.getUuid(), blocks);
            // 记录苦力怕生成位置（笼内玩家头顶上方）
            cageCreeperSpawns.put(player.getUuid(), feetPos.add(0, 2, 0));
            player.sendMessage(Text.literal("§c🔒 囚笼试炼！你被铁栅栏困住了，5秒后笼内将出现苦力怕！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§c🔒 特殊事件「囚笼试炼」已触发！所有存活玩家被铁栅栏囚禁，§e5秒后§c笼内出现苦力怕！"), false);
        return SpecialEventType.CAGE_TRIAL.getDurationSeconds();
    }

    private void tickCageTrial(MinecraftServer server, int remainingSeconds) {
        // 事件总长 10s，剩余 5s 时（已过 5s）才生成苦力怕
        if (remainingSeconds != 5) return;
        GameManager gm = GameManager.getInstance();
        for (var entry : new HashMap<>(cageCreeperSpawns).entrySet()) {
            UUID playerId = entry.getKey();
            BlockPos spawnPos = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) continue;
            PlayerWordData data = gm.getPlayerData(playerId);
            if (data == null || data.isEliminated()) {
                cageCreeperSpawns.remove(playerId);
                continue;
            }
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            CreeperEntity creeper = new CreeperEntity(EntityType.CREEPER, world);
            creeper.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    random.nextFloat() * 360, 0);
            world.spawnEntity(creeper);
            player.sendMessage(Text.literal("§c💥 苦力怕出现在笼子里！"), true);
        }
        cageCreeperSpawns.clear();
    }

    private void endCageTrial(MinecraftServer server) {
        for (var entry : cageBlocks.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            for (BlockPos pos : entry.getValue()) {
                if (world.getBlockState(pos).getBlock() == Blocks.IRON_BARS) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
            player.sendMessage(Text.literal("§a🔓 囚笼已消失！"), true);
        }
        cageBlocks.clear();
        cageCreeperSpawns.clear();
    }

    // ==================== 高空落水挑战 ====================

    private int executeSkyWaterChallenge(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        skyWaterTracking.clear();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            // 保存主手物品，尝试放入背包
            ItemStack mainHandItem = player.getMainHandStack().copy();
            if (!mainHandItem.isEmpty()) {
                if (!player.getInventory().insertStack(mainHandItem)) {
                    // 背包满则掉落在地上
                    player.dropItem(mainHandItem, false);
                }
            }
            // 主手替换为水桶
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            // 传送到 100 米高空
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos groundPos = player.getBlockPos();
            player.teleport(world, groundPos.getX() + 0.5, groundPos.getY() + 100, groundPos.getZ() + 0.5,
                    Set.of(), player.getYaw(), player.getPitch(), false);
            skyWaterTracking.add(player.getUuid());
            player.sendMessage(Text.literal("§b💧 高空落水挑战！你已被传送到百米高空，用主手水桶落地接水自救！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§b💧 特殊事件「高空落水挑战」已触发！所有存活玩家被传送到百米高空，落地水成功加一颗心！"), false);
        return SpecialEventType.SKY_WATER_CHALLENGE.getDurationSeconds();
    }

    private void tickSkyWaterChallenge(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        List<UUID> completed = new ArrayList<>();
        for (UUID playerId : skyWaterTracking) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                completed.add(playerId);
                continue;
            }
            PlayerWordData data = gm.getPlayerData(playerId);
            if (data == null || data.isEliminated()) {
                completed.add(playerId);
                continue;
            }
            // 检测玩家是否接触水
            if (player.isTouchingWater()) {
                data.addHeart();
                gm.syncPlayerDataPublic(server, data);
                player.sendMessage(Text.literal("§a💚 成功落水！加一颗心！当前 §c" + data.getHearts() + " ❤️"), true);
                completed.add(playerId);
            }
        }
        skyWaterTracking.removeAll(completed);
    }

    private void endSkyWaterChallenge(MinecraftServer server) {
        if (!skyWaterTracking.isEmpty()) {
            for (UUID playerId : skyWaterTracking) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                if (player != null) {
                    player.sendMessage(Text.literal("§c💧 高空落水挑战结束，你未能成功落水..."), true);
                }
            }
        }
        skyWaterTracking.clear();
    }

    // ==================== 作物速成 ====================

    private int executeCropSpeedGrow(MinecraftServer server) {
        server.getPlayerManager().broadcast(
                Text.literal("§a🌱 特殊事件「作物速成」已触发！脚边作物瞬间成熟，持续 §e15秒 §f！"), false);
        return SpecialEventType.CROP_SPEED_GROW.getDurationSeconds();
    }

    private static final Block[] FLOWER_TYPES = {
            Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
            Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY
    };

    private void tickCropSpeedGrow(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos feetPos = player.getBlockPos();

            // 玩家脚底 7×7 范围：农作物瞬间成熟、树苗长成大树
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = -1; dy <= 3; dy++) {
                        BlockPos pos = feetPos.add(dx, dy, dz);
                        BlockState state = world.getBlockState(pos);
                        Block block = state.getBlock();
                        if (block instanceof CropBlock crop) {
                            // 农作物直接成熟到最大阶段
                            int maxAge = crop.getMaxAge();
                            world.setBlockState(pos, crop.withAge(maxAge));
                        } else if (block instanceof SaplingBlock && block instanceof Fertilizable f) {
                            // 树苗催熟成大树
                            if (f.isFertilizable(world, pos, state)) {
                                f.grow(world, world.random, pos, state);
                            }
                        }
                    }
                }
            }

            // 大范围（15×15）生成花草
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    if (random.nextFloat() > 0.3f) continue; // 30% 概率生成
                    BlockPos pos = feetPos.add(dx, 0, dz);
                    BlockPos groundPos = pos.down();
                    BlockState ground = world.getBlockState(groundPos);
                    // 只在泥土/草方块上生成
                    if (ground.getBlock() == Blocks.GRASS_BLOCK || ground.getBlock() == Blocks.DIRT
                            || ground.getBlock() == Blocks.PODZOL || ground.getBlock() == Blocks.MYCELIUM) {
                        if (world.getBlockState(pos).isAir()) {
                            Block flower = FLOWER_TYPES[random.nextInt(FLOWER_TYPES.length)];
                            world.setBlockState(pos, flower.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    // ==================== 豁免祝福 ====================

    private int executeDurabilityBlessing(MinecraftServer server) {
        GameManager.getInstance().setDurabilityBlessingActive(true);
        server.getPlayerManager().broadcast(
                Text.literal("§d🛡 特殊事件「豁免祝福」已触发！§e2分钟内 §d装备耐久不再消耗，工具挖掘无损耗！"), false);
        return SpecialEventType.DURABILITY_BLESSING.getDurationSeconds();
    }

    private void endDurabilityBlessing() {
        GameManager.getInstance().setDurabilityBlessingActive(false);
    }

    // ==================== 装备锈蚀 ====================

    private int executeEquipmentRust(MinecraftServer server) {
        GameManager.getInstance().setEquipmentRustActive(true);
        server.getPlayerManager().broadcast(
                Text.literal("§7🔧 特殊事件「装备锈蚀」已触发！§e2分钟内 §7穿戴装备与工具损耗耐久 §c五倍 §7！"), false);
        return SpecialEventType.EQUIPMENT_RUST.getDurationSeconds();
    }

    private void endEquipmentRust() {
        GameManager.getInstance().setEquipmentRustActive(false);
    }

    // ==================== 饥饿疫病 ====================

    private int executeHungerDisease(MinecraftServer server) {
        GameManager.getInstance().setHungerDiseaseActive(true);
        server.getPlayerManager().broadcast(
                Text.literal("§c🍖 特殊事件「饥饿疫病」已触发！§e30秒内 §c饥饿值飞速下降，食物回复效果减半！"), false);
        return SpecialEventType.HUNGER_DISEASE.getDurationSeconds();
    }

    private void tickHungerDisease(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            // 每 tick 施加饥饿效果 III（大幅加速饥饿值下降）
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 40, 2,
                    false, false, true));
        }
    }

    private void endHungerDisease() {
        GameManager.getInstance().setHungerDiseaseActive(false);
    }

    // ==================== 物资迁徙 ====================

    private int executeInventoryMigration(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        int totalDropped = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            // 遍历所有背包槽位，将物品掉落在地
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                ItemStack dropped = stack.copy();
                player.getInventory().setStack(i, ItemStack.EMPTY);
                player.dropItem(dropped, false);
                totalDropped++;
            }
            player.sendMessage(Text.literal("§c📦 物资迁徙！你背包里的物品全部掉落在身边！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§c📦 特殊事件「物资迁徙」已触发！所有玩家背包物品掉落在身边！共 §e" + totalDropped + "个 §c物品！"), false);
        return 0;
    }

    // ==================== 全员变幼体 ====================

    private int executeEveryoneBaby(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        GameManager.getInstance().setEveryoneBabyActive(true);
        everyoneBabyOriginalAttrs.clear();

        RegistryWrapper.Impl<net.minecraft.entity.attribute.EntityAttribute> attrWrapper =
                server.getRegistryManager().getOptional(RegistryKeys.ATTRIBUTE).orElseThrow();

        // 获取 scale、movement_speed、jump_strength 属性
        var scaleEntry = attrWrapper.getOptional(
                net.minecraft.registry.RegistryKey.of(RegistryKeys.ATTRIBUTE,
                        net.minecraft.util.Identifier.ofVanilla("scale"))).orElseThrow();
        var speedEntry = attrWrapper.getOptional(
                net.minecraft.registry.RegistryKey.of(RegistryKeys.ATTRIBUTE,
                        net.minecraft.util.Identifier.ofVanilla("movement_speed"))).orElseThrow();
        var jumpEntry = attrWrapper.getOptional(
                net.minecraft.registry.RegistryKey.of(RegistryKeys.ATTRIBUTE,
                        net.minecraft.util.Identifier.ofVanilla("jump_strength"))).orElseThrow();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;

            // 保存原始值
            var scaleAttr = player.getAttributeInstance(scaleEntry);
            var speedAttr = player.getAttributeInstance(speedEntry);
            var jumpAttr = player.getAttributeInstance(jumpEntry);
            double origScale = scaleAttr != null ? scaleAttr.getBaseValue() : 1.0;
            double origSpeed = speedAttr != null ? speedAttr.getBaseValue() : 0.1;
            double origJump = jumpAttr != null ? jumpAttr.getBaseValue() : 0.42;
            everyoneBabyOriginalAttrs.put(player.getUuid(), new double[]{origScale, origSpeed, origJump});

            // 缩放为原来的 1/5
            double scale = 1.0 / 5.0;
            if (scaleAttr != null) scaleAttr.setBaseValue(scale);
            if (speedAttr != null) speedAttr.setBaseValue(origSpeed * scale);
            if (jumpAttr != null) jumpAttr.setBaseValue(origJump * scale);

            player.sendMessage(Text.literal("§a👶 全员变幼体！你缩小为原来的 1/5，移动速度和跳跃高度也等比缩小！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§a👶 特殊事件「全员变幼体」已触发！所有玩家缩小为 1/5，移动速度和跳跃高度等比缩小，持续 §e1分钟 §f！"), false);
        return SpecialEventType.EVERYONE_BABY.getDurationSeconds();
    }

    private void endEveryoneBaby(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        GameManager.getInstance().setEveryoneBabyActive(false);

        RegistryWrapper.Impl<net.minecraft.entity.attribute.EntityAttribute> attrWrapper =
                server.getRegistryManager().getOptional(RegistryKeys.ATTRIBUTE).orElseThrow();
        var scaleEntry = attrWrapper.getOptional(
                net.minecraft.registry.RegistryKey.of(RegistryKeys.ATTRIBUTE,
                        net.minecraft.util.Identifier.ofVanilla("scale"))).orElseThrow();
        var speedEntry = attrWrapper.getOptional(
                net.minecraft.registry.RegistryKey.of(RegistryKeys.ATTRIBUTE,
                        net.minecraft.util.Identifier.ofVanilla("movement_speed"))).orElseThrow();
        var jumpEntry = attrWrapper.getOptional(
                net.minecraft.registry.RegistryKey.of(RegistryKeys.ATTRIBUTE,
                        net.minecraft.util.Identifier.ofVanilla("jump_strength"))).orElseThrow();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null) continue;

            double[] orig = everyoneBabyOriginalAttrs.get(player.getUuid());
            double origScale = orig != null ? orig[0] : 1.0;
            double origSpeed = orig != null ? orig[1] : 0.1;
            double origJump = orig != null ? orig[2] : 0.42;

            var scaleAttr = player.getAttributeInstance(scaleEntry);
            var speedAttr = player.getAttributeInstance(speedEntry);
            var jumpAttr = player.getAttributeInstance(jumpEntry);
            if (scaleAttr != null) scaleAttr.setBaseValue(origScale);
            if (speedAttr != null) speedAttr.setBaseValue(origSpeed);
            if (jumpAttr != null) jumpAttr.setBaseValue(origJump);

            player.sendMessage(Text.literal("§a🔙 已恢复原始体型、速度和跳跃！"), true);
        }
        everyoneBabyOriginalAttrs.clear();
    }

    // ==================== 粘液附身 ====================

    private int executeSlimePossession(MinecraftServer server) {
        slimeBlocks.clear();
        server.getPlayerManager().broadcast(
                Text.literal("§a🟢 特殊事件「粘液附身」已触发！脚底不断生成粘液块，持续 §e30秒 §f！"), false);
        return SpecialEventType.SLIME_POSSESSION.getDurationSeconds();
    }

    private void tickSlimePossession(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos feetPos = player.getBlockPos();
            Map<BlockPos, BlockState> blocks = slimeBlocks.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
            // 脚底 4×4 范围替换为粘液块（替换任何方块，基岩除外）
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    BlockPos pos = feetPos.add(dx - 1, -1, dz - 1);
                    BlockState existing = world.getBlockState(pos);
                    // 跳过基岩和已记录的粘液块位置
                    if (existing.getBlock() == Blocks.BEDROCK) continue;
                    if (existing.getBlock() == Blocks.SLIME_BLOCK && blocks.containsKey(pos)) continue;
                    // 保存原始方块（仅首次替换时）
                    if (!blocks.containsKey(pos)) {
                        blocks.put(pos, existing);
                    }
                    world.setBlockState(pos, Blocks.SLIME_BLOCK.getDefaultState());
                }
            }
        }
    }

    private void endSlimePossession(MinecraftServer server) {
        for (var entry : slimeBlocks.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            for (var blockEntry : entry.getValue().entrySet()) {
                BlockPos pos = blockEntry.getKey();
                if (world.getBlockState(pos).getBlock() == Blocks.SLIME_BLOCK) {
                    world.setBlockState(pos, blockEntry.getValue());
                }
            }
            player.sendMessage(Text.literal("§a🟢 粘液块已消失，方块已恢复！"), true);
        }
        slimeBlocks.clear();
    }

    // ==================== 箭雨试炼 ====================

    private int executeArrowTrial(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        arrowTrialInitialHealth.clear();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            // 记录初始血量
            arrowTrialInitialHealth.put(player.getUuid(), player.getHealth());
            player.sendMessage(Text.literal("§b🏹 箭雨试炼！高空箭矢来袭，存活 §e10秒 §b奖励一颗心，受伤扣一颗心！"), true);
        }
        server.getPlayerManager().broadcast(
                Text.literal("§b🏹 特殊事件「箭雨试炼」已触发！箭矢从玩家头顶上方落下，持续 §e10秒 §f！"), false);
        return SpecialEventType.ARROW_TRIAL.getDurationSeconds();
    }

    private void tickArrowTrial(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;
            if (!arrowTrialInitialHealth.containsKey(player.getUuid())) continue;
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            // 每 tick 从玩家头顶 8-18 格高处射 3 支箭
            for (int i = 0; i < 3; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 2;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 2;
                double y = player.getY() + 8 + random.nextDouble() * 10;
                ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, world);
                arrow.setPosition(x, y, z);
                arrow.setVelocity(
                        (random.nextDouble() - 0.5) * 0.1,
                        -2.5 - random.nextDouble() * 1.5,
                        (random.nextDouble() - 0.5) * 0.1
                );
                arrow.setDamage(2.0);
                world.spawnEntity(arrow);
            }
        }
    }

    private void endArrowTrial(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (var entry : new HashMap<>(arrowTrialInitialHealth).entrySet()) {
            UUID playerId = entry.getKey();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) continue;
            // 判定：比较血量，如果受伤则扣心，否则加心
            Float initialHealth = entry.getValue();
            PlayerWordData data = gm.getPlayerData(playerId);
            if (data != null && !data.isEliminated()) {
                if (player.getHealth() < initialHealth) {
                    // 受伤：扣一颗心
                    boolean eliminated = data.loseHeart();
                    gm.syncPlayerDataPublic(server, data);
                    player.sendMessage(Text.literal("§c💔 你在箭雨中受伤了！扣一颗心！剩余 §c" + data.getHearts() + " ❤️"), true);
                    if (eliminated) {
                        player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                        server.getPlayerManager().broadcast(
                                Text.literal("§c💀 " + data.getTeamColor().getDisplayName() + " §f因箭雨试炼被淘汰！"), false);
                    }
                } else {
                    // 存活未受伤：加一颗心
                    data.addHeart();
                    gm.syncPlayerDataPublic(server, data);
                    player.sendMessage(Text.literal("§a💚 你成功在箭雨中存活！加一颗心！当前 §c" + data.getHearts() + " ❤️"), true);
                }
            }
            player.sendMessage(Text.literal("§a🏹 箭雨试炼结束！"), true);
        }
        arrowTrialInitialHealth.clear();
        gm.checkWinConditionPublic(server);
    }

    // ==================== 交易商人 ====================

    private int executeTradeMerchant(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        tradeMerchantVillagers.clear();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerWordData data = gm.getPlayerData(player.getUuid());
            if (data == null || data.isEliminated()) continue;

            ServerWorld world = (ServerWorld) player.getEntityWorld();

            // 在玩家附近 3-5 格生成村民
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 3 + random.nextDouble() * 2;
            double spawnX = player.getX() + Math.cos(angle) * distance;
            double spawnZ = player.getZ() + Math.sin(angle) * distance;
            double spawnY = player.getY();

            // 确保生成在地面上
            BlockPos spawnPos = new BlockPos((int) spawnX, (int) spawnY, (int) spawnZ);
            for (int yOff = 0; yOff < 5; yOff++) {
                BlockPos checkPos = spawnPos.down(yOff);
                if (world.getBlockState(checkPos).isSolidBlock(world, checkPos)) {
                    spawnPos = checkPos.up();
                    break;
                }
            }

            VillagerEntity villager = EntityType.VILLAGER.create(world, SpawnReason.EVENT);
            if (villager == null) continue;
            villager.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    random.nextFloat() * 360, 0);
            // 设置为盔甲匠职业
            RegistryWrapper<VillagerType> typeWrapper = world.getRegistryManager().getOptional(RegistryKeys.VILLAGER_TYPE).orElseThrow();
            RegistryEntry<VillagerType> typeEntry = typeWrapper.getOptional(VillagerType.PLAINS).orElseThrow();
            RegistryWrapper<VillagerProfession> profWrapper = world.getRegistryManager().getOptional(RegistryKeys.VILLAGER_PROFESSION).orElseThrow();
            RegistryEntry<VillagerProfession> profEntry = profWrapper.getOptional(VillagerProfession.ARMORER).orElseThrow();
            villager.setVillagerData(new VillagerData(typeEntry, profEntry, 5));

            // 设置自定义交易
            TradeOfferList tradeList = new TradeOfferList();
            // 1 木棍 + 2 钻石 → 1 下界合金剑
            tradeList.add(new TradeOffer(
                    new TradedItem(Items.STICK, 1),
                    Optional.of(new TradedItem(Items.DIAMOND, 2)),
                    new ItemStack(Items.NETHERITE_SWORD, 1),
                    1, 30, 0.05f));
            // 2 木棍 + 3 钻石 → 1 下界合金斧
            tradeList.add(new TradeOffer(
                    new TradedItem(Items.STICK, 2),
                    Optional.of(new TradedItem(Items.DIAMOND, 3)),
                    new ItemStack(Items.NETHERITE_AXE, 1),
                    1, 30, 0.05f));
            // 2 木棍 + 3 钻石 → 1 下界合金镐
            tradeList.add(new TradeOffer(
                    new TradedItem(Items.STICK, 2),
                    Optional.of(new TradedItem(Items.DIAMOND, 3)),
                    new ItemStack(Items.NETHERITE_PICKAXE, 1),
                    1, 30, 0.05f));
            // 5 钻石 → 下界合金头盔
            tradeList.add(new TradeOffer(
                    new TradedItem(Items.DIAMOND, 5),
                    new ItemStack(Items.NETHERITE_HELMET, 1),
                    1, 30, 0.05f));
            // 8 钻石 → 下界合金胸甲
            tradeList.add(new TradeOffer(
                    new TradedItem(Items.DIAMOND, 8),
                    new ItemStack(Items.NETHERITE_CHESTPLATE, 1),
                    1, 30, 0.05f));
            // 7 钻石 → 下界合金护腿
            tradeList.add(new TradeOffer(
                    new TradedItem(Items.DIAMOND, 7),
                    new ItemStack(Items.NETHERITE_LEGGINGS, 1),
                    1, 30, 0.05f));
            // 4 钻石 → 下界合金靴
            tradeList.add(new TradeOffer(
                    new TradedItem(Items.DIAMOND, 4),
                    new ItemStack(Items.NETHERITE_BOOTS, 1),
                    1, 30, 0.05f));

            villager.setOffers(tradeList);
            villager.setInvulnerable(true); // 防止被误杀
            villager.setCustomName(Text.literal("§6⚡ 神秘商人"));
            villager.setCustomNameVisible(true);

            world.spawnEntity(villager);
            tradeMerchantVillagers.put(player.getUuid(), villager);

            // 发送标题
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
            player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§6⚡ 交易商人来了！")));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§e快用钻石换取下界合金装备！")));
            player.sendMessage(Text.literal("§6⚡ 一位神秘商人出现在你身边！用钻石换取下界合金装备，§e30秒后§6消失！"), true);
        }

        // 向全体玩家播放特殊音效
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        server.getPlayerManager().broadcast(
                Text.literal("§6⚡ 超级事件「交易商人」已触发！每位存活玩家身边出现神秘商人，用钻石换取下界合金装备，持续 §e30秒 §f！"), false);
        return SpecialEventType.TRADE_MERCHANT.getDurationSeconds();
    }

    private void endTradeMerchant(MinecraftServer server) {
        GameManager gm = GameManager.getInstance();
        for (var entry : tradeMerchantVillagers.entrySet()) {
            VillagerEntity villager = entry.getValue();
            if (villager != null && villager.isAlive()) {
                villager.discard();
            }
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(Text.literal("§6⚡ 神秘商人已消失！"), true);
            }
        }
        tradeMerchantVillagers.clear();
    }

}

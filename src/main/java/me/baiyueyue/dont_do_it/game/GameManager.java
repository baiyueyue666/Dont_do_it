package me.baiyueyue.dont_do_it.game;

import me.baiyueyue.dont_do_it.game.trigger.WordTriggerDetector;
import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldProperties;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏状态管理器（单例）—— 管理整局游戏的生命周期
 *
 * 核心流程：
 *   WAITING → LOBBY（手持书本大厅）→ RUNNING（分配队伍/词条/倒计时）
 *   → ENDING（烟花+3秒）→ ENDED（传送出生点+生存模式）→ WAITING
 */
public class GameManager {

    private static final GameManager INSTANCE = new GameManager();

    private GameState state = GameState.WAITING;
    private final GameSettings settings = new GameSettings();
    private final WordPool wordPool = new WordPool();
    private final SpecialEventPool specialEventPool = new SpecialEventPool();
    private final Map<UUID, PlayerWordData> playerDataMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /** 游戏书本物品引用，由 Dont_do_it 入口注入 */
    private Item gameBookItem;

    /** 记录每个玩家每种触发类型的上次触发 tick，用于冷却（防潜行/疾跑连发） */
    private final Map<UUID, Map<TriggerType, Long>> lastTriggerTick = new ConcurrentHashMap<>();

    // ==================== 特殊事件状态 ====================

    /** 特殊事件触发倒计时（秒），从设置值倒计时到 0 时触发 */
    private int specialEventCountdown = 0;
    /** 当前激活的特殊事件类型，null 表示无活动事件 */
    private SpecialEventType activeSpecialEvent = null;
    /** 当前特殊事件剩余持续时间（秒），仅对有持续时长的活动事件有效 */
    private int activeSpecialEventRemaining = 0;

    /** 钻石祝福是否激活（挖钻石矿回血） */
    private boolean diamondBlessingActive = false;
    /** 豁免祝福是否激活（装备耐久不消耗） */
    private boolean durabilityBlessingActive = false;
    /** 装备锈蚀是否激活（耐久五倍消耗） */
    private boolean equipmentRustActive = false;
    /** 饥饿疫病是否激活（饥饿值飞速下降，食物回复减半） */
    private boolean hungerDiseaseActive = false;
    /** 全员变幼体是否激活（玩家缩小100倍） */
    private boolean everyoneBabyActive = false;

    // ==================== 游戏范围边界 ====================

    /** 游戏范围边界：-1 表示未启用 */
    private double boundMinX = -1, boundMinZ = -1, boundMaxX = -1, boundMaxZ = -1;
    /** 边界中心点（用于越界传送回） */
    private double boundCenterX, boundCenterZ;
    /** 边界起始区块坐标（用于方块恢复范围计算） */
    private int startChunkX, startChunkZ;

    // ==================== 准备阶段 ====================

    /** 准备阶段倒计时（秒），-1 = 未激活 */
    private int prepCountdown = -1;

    // ==================== 方块保存/恢复 ====================

    /** 保存的原始方块（不包括空气） */
    private Map<BlockPos, BlockState> savedBlocks = null;
    /** 保存的原始方块实体 NBT 数据 */
    private Map<BlockPos, NbtCompound> savedBlockEntities = null;
    /** 黑曜石边界墙位置（游戏结束时显式清理，因为墙高可能超出 save 范围） */
    private final List<BlockPos> obsidianWallPositions = new ArrayList<>();

    private GameManager() {}

    public static GameManager getInstance() { return INSTANCE; }

    /** 注入游戏书本物品引用 */
    public void setGameBookItem(Item item) { this.gameBookItem = item; }

    // ==================== 状态访问 ====================

    public GameState getState() { return state; }
    public boolean isRunning() { return state == GameState.RUNNING; }
    public boolean isPrepPhase() { return prepCountdown > 0; }
    public int getPrepCountdown() { return prepCountdown; }
    public GameSettings getSettings() { return settings; }
    public PlayerWordData getPlayerData(UUID playerId) { return playerDataMap.get(playerId); }
    public Collection<PlayerWordData> getAllPlayerData() { return playerDataMap.values(); }

    public boolean isDiamondBlessingActive() { return diamondBlessingActive; }
    public void setDiamondBlessingActive(boolean active) { this.diamondBlessingActive = active; }

    public boolean isDurabilityBlessingActive() { return durabilityBlessingActive; }
    public void setDurabilityBlessingActive(boolean active) { this.durabilityBlessingActive = active; }

    public boolean isEquipmentRustActive() { return equipmentRustActive; }
    public void setEquipmentRustActive(boolean active) { this.equipmentRustActive = active; }

    public boolean isHungerDiseaseActive() { return hungerDiseaseActive; }
    public void setHungerDiseaseActive(boolean active) { this.hungerDiseaseActive = active; }

    public boolean isEveryoneBabyActive() { return everyoneBabyActive; }
    public void setEveryoneBabyActive(boolean active) { this.everyoneBabyActive = active; }

    // ==================== 游戏流程 ====================

    /**
     * 开始游戏（阶段一：准备阶段）
     * - 游戏范围开启时：选区、存方块、高空传送、10s 无敌
     * - 游戏范围关闭时：直接进入阶段二
     */
    public void startGame(MinecraftServer server) {
        if (state == GameState.RUNNING) return;

        playerDataMap.clear();
        lastTriggerTick.clear();
        WordTriggerDetector.clearAllState();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();

        if (players.size() < 2) {
            server.getPlayerManager().broadcast(
                    Text.literal("§c⚠ 至少需要 2 名玩家才能开始「不要做挑战」！"), false);
            return;
        }

        int gameRange = settings.getGameRange();
        if (gameRange > 0) {
            // ---- 游戏范围边界设定（含海洋检测） ----
            ServerWorld overworld = server.getOverworld();
            long worldSeed = overworld.getSeed();
            Random boundaryRandom = new Random(worldSeed ^ System.currentTimeMillis());

            int sx, sz;
            int retries = 0;
            do {
                sx = boundaryRandom.nextInt(1001) - 500;
                sz = boundaryRandom.nextInt(1001) - 500;
                retries++;
            } while (isOceanArea(overworld, sx, sz, gameRange) && retries < 50);

            startChunkX = sx;
            startChunkZ = sz;
            boundMinX = sx * 16;
            boundMinZ = sz * 16;
            boundMaxX = (sx + gameRange) * 16 - 0.01;
            boundMaxZ = (sz + gameRange) * 16 - 0.01;
            boundCenterX = sx * 16 + gameRange * 8.0;
            boundCenterZ = sz * 16 + gameRange * 8.0;

            // 保存原始方块
            saveAreaBlocks(overworld);

            // 建造黑曜石边界墙（基岩层 ~ Y=225）
            buildObsidianWall(overworld, gameRange);

            // 发送边界到客户端
            sendBoundaryPacket(server);

            // 传送所有玩家到高空（Y=150）并开启无敌
            for (ServerPlayerEntity player : players) {
                player.getAbilities().invulnerable = true;
                player.sendAbilitiesUpdate();
                player.teleport(overworld, boundCenterX + 0.5, 150, boundCenterZ + 0.5,
                        Set.of(), player.getYaw(), player.getPitch(), false);
            }

            // 启动准备阶段倒计时
            prepCountdown = 10;
            state = GameState.RUNNING;
            sendPrepCountdown(server, prepCountdown);

            server.getPlayerManager().broadcast(
                    Text.literal("§e🌍 游戏范围已选定 §3%d×%d区块 §e| 10秒后正式开始！".formatted(gameRange, gameRange)), false);

        } else {
            // 关闭范围，清除边界，直接进入阶段二
            boundMinX = -1;
            boundMaxX = -1;
            boundMinZ = -1;
            boundMaxZ = -1;
            startPhase2(server);
        }
    }

    /**
     * 阶段二：正式开始游戏（词条分配、倒计时等）
     */
    private void startPhase2(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();

        // 关闭无敌，恢复生命值与饥饿值，并将玩家传送到游戏区域地表，同时设置出生点
        for (ServerPlayerEntity player : players) {
            player.getAbilities().invulnerable = false;
            player.sendAbilitiesUpdate();
            // 恢复生命值和饥饿值至满格
            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);
            player.getHungerManager().setSaturationLevel(5.0f);
            if (boundMinX >= 0) {
                ServerWorld overworld = server.getOverworld();
                int safeY = findSafeSurfaceY(overworld, (int) boundCenterX, (int) boundCenterZ);
                // 在出生点地表生成 3×3 黑曜石平台（防 TNT 破坏）
                buildObsidianPlatform(overworld, (int) boundCenterX, safeY - 1, (int) boundCenterZ);
                player.teleport(overworld, boundCenterX + 0.5, safeY, boundCenterZ + 0.5,
                        Set.of(), player.getYaw(), player.getPitch(), false);
                // 设置出生点为游戏区域当前位置，死亡后自动复活在此
                WorldProperties.SpawnPoint spawnPoint = WorldProperties.SpawnPoint.create(
                        overworld.getRegistryKey(),
                        new BlockPos((int) boundCenterX, safeY, (int) boundCenterZ),
                        player.getYaw(),
                        player.getPitch());
                player.setSpawnPoint(new ServerPlayerEntity.Respawn(spawnPoint, true), false);
            }
        }
        sendPrepCountdown(server, 0);

        // 打乱玩家顺序，轮流分配队伍颜色
        List<ServerPlayerEntity> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, random);
        int timerSec = settings.getWordChangeTimerSeconds();

        // 抽取词条
        List<WordPool.WordEntry> drawn = wordPool.drawWords(shuffled.size());

        for (int i = 0; i < shuffled.size(); i++) {
            ServerPlayerEntity player = shuffled.get(i);
            TeamColor team = TeamColor.fromIndex(i);
            WordPool.WordEntry entry = drawn.get(i);
            PlayerWordData data = new PlayerWordData(player.getUuid(), team, entry,
                    settings.getDefaultHearts(), timerSec);
            playerDataMap.put(player.getUuid(), data);
        }

        // 处理初始分配的即时触发词条（直接扣心/回心）
        for (ServerPlayerEntity player : players) {
            PlayerWordData data = playerDataMap.get(player.getUuid());
            if (data != null) {
                handleInstantTrigger(data, server, 0);
            }
        }

        // 初始化特殊事件倒计时
        specialEventCountdown = settings.getSpecialEventTimerSeconds();
        activeSpecialEvent = null;
        activeSpecialEventRemaining = 0;
        diamondBlessingActive = false;
        durabilityBlessingActive = false;
        equipmentRustActive = false;
        hungerDiseaseActive = false;
        everyoneBabyActive = false;

        // 清空所有玩家背包
        for (ServerPlayerEntity player : players) {
            player.getInventory().clear();
        }

        // 创建原版计分板队伍并分配玩家
        assignVanillaTeams(server, shuffled);

        // 给所有玩家添加 Glowing 高亮效果（颜色由队伍决定）
        for (ServerPlayerEntity player : players) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,
                    StatusEffectInstance.INFINITE, 0, false, false));
        }

        // 同步每个玩家的数据到全体（客户端据此区分自己/对手）
        for (ServerPlayerEntity player : players) {
            PlayerWordData selfData = playerDataMap.get(player.getUuid());
            if (selfData != null) {
                syncOnePlayer(server, selfData);
            }
        }

        // 广播开始
        String rangeMsg = settings.isGameRangeEnabled()
                ? " §7| 游戏范围: §3%d×%d区块".formatted(settings.getGameRange(), settings.getGameRange())
                : "";
        server.getPlayerManager().broadcast(
                Text.literal("§a🎮 「不要做挑战」开始！每人 §c%d ❤️ §a| 词条每 §e%d秒 §a自动更换%s"
                        .formatted(settings.getDefaultHearts(), timerSec, rangeMsg)), false);
    }

    /**
     * 结束游戏 —— 清理并重置
     */
    public void endGame(MinecraftServer server) {
        state = GameState.ENDED;

        // 清理特殊事件
        activeSpecialEvent = null;
        activeSpecialEventRemaining = 0;
        diamondBlessingActive = false;
        durabilityBlessingActive = false;
        equipmentRustActive = false;
        hungerDiseaseActive = false;
        everyoneBabyActive = false;
        specialEventCountdown = 0;
        removeSpecialEventBossBar(server);

        // 移除所有玩家的 Glowing 高亮效果
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.removeStatusEffect(StatusEffects.GLOWING);
        }

        // 清理黑曜石边界墙（避免墙高超出 save 范围导致残留）
        ServerWorld world = server.getOverworld();
        for (BlockPos pos : obsidianWallPositions) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }
        obsidianWallPositions.clear();

        // 恢复原始方块
        restoreAreaBlocks(world);

        // 清除游戏范围边界
        boundMinX = -1;
        boundMaxX = -1;
        boundMinZ = -1;
        boundMaxZ = -1;
        sendBoundaryClearPacket(server);

        // 传送所有玩家回出生点并清除个人出生点（使用手动搜索避免 Heightmap 缓存过期导致卡方块）
        ServerWorld overworld = server.getOverworld();
        BlockPos spawnPos = overworld.getSpawnPoint().getPos();
        int safeY = findSafeSurfaceY(overworld, spawnPos.getX(), spawnPos.getZ());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.changeGameMode(GameMode.SURVIVAL);
            player.teleport(overworld, spawnPos.getX() + 0.5, safeY, spawnPos.getZ() + 0.5,
                    Set.of(), player.getYaw(), player.getPitch(), false);
            // 清除游戏期间设定的个人出生点，恢复为世界出生点
            player.setSpawnPoint(null, false);
        }
        removeVanillaTeams(server);

        // 清空背包并发还游戏书本
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.getInventory().clear();
            if (gameBookItem != null) {
                giveGameBook(player);
            }
        }

        // 通知所有客户端重置 HUD 状态
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, new GamePackets.GameStateResetPayload());
        }

        playerDataMap.clear();
        lastTriggerTick.clear();
        settings.reset();
        state = GameState.WAITING;
    }

    // ==================== 触发检测 ====================

    /** 冷却时间（毫秒）：同一行为类型至少间隔这么久才能再次触发 */
    private static final long TRIGGER_COOLDOWN_MS = 1500;

    /**
     * 玩家触发了某行为类型 —— 由 WordTriggerDetector 调用
     */
    public void onPlayerTriggered(MinecraftServer server, ServerPlayerEntity player, TriggerType triggeredType) {
        if (!isRunning()) return;
        if (isPrepPhase()) return; // 准备阶段不触发词条

        PlayerWordData data = playerDataMap.get(player.getUuid());
        if (data == null || data.isEliminated()) return;

        // 冷却检查（防止潜行/疾跑等 tick 事件连发）
        long now = System.currentTimeMillis();
        Map<TriggerType, Long> playerCooldowns = lastTriggerTick.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
        Long last = playerCooldowns.get(triggeredType);
        if (last != null && (now - last) < TRIGGER_COOLDOWN_MS) return;
        playerCooldowns.put(triggeredType, now);

        // 检查词条匹配
        if (data.getTriggerType() == triggeredType) {
            boolean eliminated = data.loseHeart();
            String teamName = data.getTeamColor().getDisplayName();
            String word = data.getWordText();

            if (eliminated) {
                // 淘汰
                String eliminationMsg = "§c💀 " + teamName + " §f已淘汰！（词条：§b" + word + "§f）";
                broadcastNotification(server, "elimination", eliminationMsg);
                // 聊天栏播报
                server.getPlayerManager().broadcast(Text.literal(eliminationMsg), false);

                player.changeGameMode(GameMode.SPECTATOR);

                // 播放末影龙吼叫声
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.playSound(SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
                }

                // 同步淘汰状态
                syncOnePlayer(server, data);
                checkWinCondition(server);
            } else {
                // 扣心 + 换词条
                String triggerMsg = "§e⚡ " + teamName + " §f触发了「§b" + word + "§f」！剩余 §c%d ❤️".formatted(data.getHearts());
                broadcastNotification(server, "trigger", triggerMsg);
                // 聊天栏播报
                server.getPlayerManager().broadcast(Text.literal(triggerMsg), false);

                replaceWordForPlayer(data, server);
                syncOnePlayer(server, data);

                // 播放叮声
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.5f);
                }
            }
        }
    }

    /**
     * 词条倒计时到期（由 CountdownManager 调用）
     */
    public void onWordTimerExpired(MinecraftServer server, UUID playerId) {
        if (!isRunning()) return;
        PlayerWordData data = playerDataMap.get(playerId);
        if (data == null || data.isEliminated()) return;

        String oldWord = data.getWordText();
        replaceWordForPlayer(data, server);
        syncOnePlayer(server, data);

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player != null) {
            player.sendMessage(Text.literal("§7⏰ 你的词条「" + oldWord + "」已过期，已更换为新词条"), true);
        }
    }

    // ==================== 特殊事件计时 ====================

    /**
     * 每秒 tick，由 GameCountdownManager 调用
     * 处理特殊事件触发倒计时和活动事件的持续时间
     */
    public void tickSpecialEvent(MinecraftServer server) {
        if (!isRunning()) return;

        // 特殊事件已关闭，跳过所有逻辑
        if (!settings.isSpecialEventEnabled()) {
            if (activeSpecialEvent != null) {
                activeSpecialEvent = null;
                activeSpecialEventRemaining = 0;
                removeSpecialEventBossBar(server);
            }
            return;
        }

        if (activeSpecialEvent != null) {
            // 有活动中的特殊事件
            if (!activeSpecialEvent.isInstant() && activeSpecialEventRemaining > 0) {
                // 每 tick 执行持续事件的动作（如美食雨、经验风暴）
                specialEventPool.tickActiveEvent(server, activeSpecialEvent, activeSpecialEventRemaining);

                activeSpecialEventRemaining--;
                syncSpecialEventBossBar(server, activeSpecialEvent.getDisplayName()
                                + " - " + activeSpecialEventRemaining + "s 剩余",
                        (float) activeSpecialEventRemaining / activeSpecialEvent.getDurationSeconds(),
                        activeSpecialEvent.getBossBarColor());

                if (activeSpecialEventRemaining <= 0) {
                    // 事件结束，执行清理
                    specialEventPool.onEventEnd(server, activeSpecialEvent);
                    server.getPlayerManager().broadcast(
                            Text.literal("§a✅ 特殊事件「" + activeSpecialEvent.getDisplayName() + "」已结束！"), false);
                    activeSpecialEvent = null;
                    // 重新开始特殊事件触发倒计时
                    specialEventCountdown = settings.getSpecialEventTimerSeconds();
                    syncSpecialEventBossBar(server);
                }
            }
        } else {
            // 没有活动事件，倒计时触发下一个
            if (specialEventCountdown > 0) {
                specialEventCountdown--;
                syncSpecialEventBossBar(server);

                if (specialEventCountdown <= 0) {
                    // 触发随机特殊事件
                    triggerSpecialEvent(server);
                }
            }
        }
    }

    /** 触发一个随机特殊事件 */
    private void triggerSpecialEvent(MinecraftServer server) {
        SpecialEventType type = specialEventPool.drawRandom();
        triggerSpecialEvent(server, type);
    }

    /** 触发指定特殊事件 */
    private void triggerSpecialEvent(MinecraftServer server, SpecialEventType type) {
        activeSpecialEvent = type;

        // 执行事件
        int duration = specialEventPool.executeEvent(server, type);

        if (type.isInstant()) {
            // 瞬时事件，立即结束
            activeSpecialEvent = null;
            specialEventCountdown = settings.getSpecialEventTimerSeconds();
            syncSpecialEventBossBar(server);
        } else {
            // 有持续时长的特殊事件
            activeSpecialEventRemaining = duration;
            syncSpecialEventBossBar(server, type.getDisplayName()
                            + " - " + activeSpecialEventRemaining + "s 剩余",
                    1.0f, type.getBossBarColor());
        }
    }

    /**
     * 根据名称触发指定特殊事件（供命令使用，无论游戏是否运行）
     * @return 是否成功触发
     */
    public boolean triggerSpecialEventByName(MinecraftServer server, String eventName) {
        if (!isRunning()) return false;
        SpecialEventType type = specialEventPool.findByName(eventName);
        if (type == null) return false;
        // 如果已有活动事件，先结束它
        if (activeSpecialEvent != null) {
            server.getPlayerManager().broadcast(
                    Text.literal("§e⚠ 特殊事件「" + activeSpecialEvent.getDisplayName() + "」被强制结束"), false);
            activeSpecialEvent = null;
            activeSpecialEventRemaining = 0;
        }
        triggerSpecialEvent(server, type);
        return true;
    }

    /** 同步特殊事件 BossBar（倒计时模式） */
    private void syncSpecialEventBossBar(MinecraftServer server) {
        int total = settings.getSpecialEventTimerSeconds();
        float percent = total > 0 ? (float) specialEventCountdown / total : 0f;
        String text = "§6⚡ 特殊事件将在 " + specialEventCountdown + "s 后触发";
        GamePackets.SpecialEventBossBarPayload payload = new GamePackets.SpecialEventBossBarPayload(
                text, percent, "YELLOW", specialEventCountdown);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** 同步特殊事件 BossBar（活动期间模式） */
    private void syncSpecialEventBossBar(MinecraftServer server, String displayText, float percent,
                                          net.minecraft.entity.boss.BossBar.Color color) {
        GamePackets.SpecialEventBossBarPayload payload = new GamePackets.SpecialEventBossBarPayload(
                "§c" + displayText, percent, color.name(), activeSpecialEventRemaining);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** 移除特殊事件 BossBar */
    private void removeSpecialEventBossBar(MinecraftServer server) {
        GamePackets.SpecialEventBossBarPayload payload = new GamePackets.SpecialEventBossBarPayload(
                "", 0f, "WHITE", -1);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
        // 客户端在收到 percent=0 时移除条
    }

    // ==================== 管理员指令 ====================

    /**
     * 管理员判断玩家猜词条是否正确
     * @param correct true=猜对加一颗心，false=猜错扣两颗心
     */
    public void voteOnPlayer(MinecraftServer server, ServerPlayerEntity target, boolean correct) {
        if (!isRunning()) return;

        PlayerWordData data = playerDataMap.get(target.getUuid());
        if (data == null || data.isEliminated()) return;

        String teamName = data.getTeamColor().getDisplayName();
        String playerName = target.getName().getString();
        String oldWord = data.getWordText();

        if (correct) {
            // 猜对：加一颗心 + 换词条
            data.addHeart();
            replaceWordForPlayer(data, server);
            syncOnePlayer(server, data);

            String msg = "§a✅ " + teamName + " " + playerName + " §f猜对了！加一颗心，当前词条：「§b"
                    + oldWord + "§f」 ❤×§c" + data.getHearts();
            server.getPlayerManager().broadcast(Text.literal(msg), false);
            broadcastNotification(server, "trigger", msg);
        } else {
            // 猜错：扣两颗心 + 换词条
            boolean eliminated = false;
            data.loseHeart();
            if (data.isEliminated()) {
                eliminated = true;
            } else {
                data.loseHeart();
                if (data.isEliminated()) eliminated = true;
            }

            replaceWordForPlayer(data, server);
            syncOnePlayer(server, data);

            if (eliminated) {
                String eliminationMsg = "§c💀 " + teamName + " " + playerName
                        + " §f猜错被淘汰！（当前词条：「§b" + oldWord + "§f」）";
                server.getPlayerManager().broadcast(Text.literal(eliminationMsg), false);
                broadcastNotification(server, "elimination", eliminationMsg);

                target.changeGameMode(GameMode.SPECTATOR);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.playSound(SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
                }
                checkWinCondition(server);
            } else {
                String msg = "§c❌ " + teamName + " " + playerName + " §f猜错了！扣两颗心，当前词条：「§b"
                        + oldWord + "§f」 ❤×§c" + data.getHearts();
                server.getPlayerManager().broadcast(Text.literal(msg), false);
                broadcastNotification(server, "trigger", msg);

                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.5f);
                }
            }
        }
    }

    /**
     * 管理员跳过当前词条（用于测试时跳过未能正确触发的词条）
     */
    public void skipPlayerWord(MinecraftServer server, ServerPlayerEntity target) {
        if (!isRunning()) return;

        PlayerWordData data = playerDataMap.get(target.getUuid());
        if (data == null || data.isEliminated()) return;

        String oldWord = data.getWordText();
        replaceWordForPlayer(data, server);
        syncOnePlayer(server, data);

        String teamName = data.getTeamColor().getDisplayName();
        String playerName = target.getName().getString();
        server.getPlayerManager().broadcast(Text.literal(
                "§7⏭ " + teamName + " " + playerName + " §f的词条已跳过「§b" + oldWord + "§f」"), false);
    }

    /**
     * 管理员为玩家设置指定词条（用于测试）
     * @param wordText 词条显示文本，需与词条池中完全匹配
     */
    public boolean setPlayerWord(MinecraftServer server, ServerPlayerEntity target, String wordText) {
        if (!isRunning()) return false;

        PlayerWordData data = playerDataMap.get(target.getUuid());
        if (data == null || data.isEliminated()) return false;

        WordPool.WordEntry entry = wordPool.findByDisplayText(wordText);
        if (entry == null) return false;

        String oldWord = data.getWordText();
        data.replaceWord(entry, settings.getWordChangeTimerSeconds());
        // 处理即时触发词条（直接扣心/回心）
        handleInstantTrigger(data, server, 0);
        // 重置跳跃计数（如果新词条是跳跃10次）
        if (entry.triggerType() == TriggerType.JUMP_10_TIMES) {
            WordTriggerDetector.resetJumpCount(target.getUuid());
        }
        // 重置持续看向方向状态（如果新词条是持续看向同一方向五秒）
        if (entry.triggerType() == TriggerType.LOOK_SAME_DIR_5S) {
            WordTriggerDetector.resetLookSameDir(target.getUuid());
        }
        // 重置放置/丢弃计数
        if (entry.triggerType() == TriggerType.PLACE_30_BLOCKS) {
            WordTriggerDetector.resetPlaceCount(target.getUuid());
        }
        if (entry.triggerType() == TriggerType.DROP_30_ITEMS) {
            WordTriggerDetector.resetDropCount(target.getUuid());
        }
        // 重置不跳/不潜行/不疾跑倒计时
        if (entry.triggerType() == TriggerType.NO_JUMP_30S || entry.triggerType() == TriggerType.NO_JUMP_60S) {
            WordTriggerDetector.resetNoJumpState(target.getUuid());
        }
        if (entry.triggerType() == TriggerType.NO_SNEAK_30S || entry.triggerType() == TriggerType.NO_SNEAK_60S) {
            WordTriggerDetector.resetNoSneakState(target.getUuid());
        }
        if (entry.triggerType() == TriggerType.NO_SPRINT_30S || entry.triggerType() == TriggerType.NO_SPRINT_60S) {
            WordTriggerDetector.resetNoSprintState(target.getUuid());
        }
        if (entry.triggerType() == TriggerType.BLOCK_ABOVE_HEAD || entry.triggerType() == TriggerType.NO_BLOCK_ABOVE_HEAD) {
            WordTriggerDetector.resetBlockAboveHeadState(target.getUuid());
        }
        syncOnePlayer(server, data);

        String teamName = data.getTeamColor().getDisplayName();
        String playerName = target.getName().getString();
        server.getPlayerManager().broadcast(Text.literal(
                "§6📝 " + teamName + " " + playerName + " §f的词条更换为「§b" + wordText + "§f」（原词条：「§7" + oldWord + "§f」）"), false);
        return true;
    }

    // ==================== 内部方法 ====================

    private void replaceWordForPlayer(PlayerWordData data, MinecraftServer server) {
        replaceWordForPlayer(data, server, 0);
    }

    /** 替换词条，支持即时词条递归处理（maxDepth 防无限递归） */
    private void replaceWordForPlayer(PlayerWordData data, MinecraftServer server, int depth) {
        if (depth > 5) return; // 防无限递归
        WordPool.WordEntry newEntry = wordPool.drawSingle();
        data.replaceWord(newEntry, settings.getWordChangeTimerSeconds());
        // 重置跳跃计数（如果新词条是跳跃10次）
        if (newEntry.triggerType() == TriggerType.JUMP_10_TIMES) {
            WordTriggerDetector.resetJumpCount(data.getPlayerId());
        }
        // 重置持续看向方向状态（如果新词条是持续看向同一方向五秒）
        if (newEntry.triggerType() == TriggerType.LOOK_SAME_DIR_5S) {
            WordTriggerDetector.resetLookSameDir(data.getPlayerId());
        }
        // 重置放置/丢弃计数
        if (newEntry.triggerType() == TriggerType.PLACE_30_BLOCKS) {
            WordTriggerDetector.resetPlaceCount(data.getPlayerId());
        }
        if (newEntry.triggerType() == TriggerType.DROP_30_ITEMS) {
            WordTriggerDetector.resetDropCount(data.getPlayerId());
        }
        // 重置不跳/不潜行/不疾跑倒计时
        if (newEntry.triggerType() == TriggerType.NO_JUMP_30S || newEntry.triggerType() == TriggerType.NO_JUMP_60S) {
            WordTriggerDetector.resetNoJumpState(data.getPlayerId());
        }
        if (newEntry.triggerType() == TriggerType.NO_SNEAK_30S || newEntry.triggerType() == TriggerType.NO_SNEAK_60S) {
            WordTriggerDetector.resetNoSneakState(data.getPlayerId());
        }
        if (newEntry.triggerType() == TriggerType.NO_SPRINT_30S || newEntry.triggerType() == TriggerType.NO_SPRINT_60S) {
            WordTriggerDetector.resetNoSprintState(data.getPlayerId());
        }
        // 重置头顶方块状态（词条变为头顶有/无方块遮挡时立即触发）
        if (newEntry.triggerType() == TriggerType.BLOCK_ABOVE_HEAD || newEntry.triggerType() == TriggerType.NO_BLOCK_ABOVE_HEAD) {
            WordTriggerDetector.resetBlockAboveHeadState(data.getPlayerId());
        }
        // 检查是否为即时触发词条
        handleInstantTrigger(data, server, depth);
    }

    /** 处理即时触发词条（直接扣心/回心） */
    private void handleInstantTrigger(PlayerWordData data, MinecraftServer server, int depth) {
        TriggerType type = data.getTriggerType();
        if (type != TriggerType.INSTANT_LOSE_HEART && type != TriggerType.INSTANT_GAIN_HEART) {
            return; // 非即时词条，无需处理
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(data.getPlayerId());
        if (player == null) return;

        String teamName = data.getTeamColor().getDisplayName();
        String playerName = player.getName().getString();

        if (type == TriggerType.INSTANT_LOSE_HEART) {
            boolean eliminated = data.loseHeart();
            if (eliminated) {
                String eliminationMsg = "§c💀 " + teamName + " " + playerName + " §f因「§b直接扣一颗心§f」被淘汰！";
                broadcastNotification(server, "elimination", eliminationMsg);
                server.getPlayerManager().broadcast(Text.literal(eliminationMsg), false);
                player.changeGameMode(GameMode.SPECTATOR);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.playSound(SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
                }
                syncOnePlayer(server, data);
                checkWinCondition(server);
            } else {
                String triggerMsg = "§e⚡ " + teamName + " " + playerName + " §f触发了「§b直接扣一颗心§f」！剩余 §c%d ❤️".formatted(data.getHearts());
                broadcastNotification(server, "trigger", triggerMsg);
                server.getPlayerManager().broadcast(Text.literal(triggerMsg), false);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.5f);
                }
                replaceWordForPlayer(data, server, depth + 1);
                syncOnePlayer(server, data);
            }
        } else { // INSTANT_GAIN_HEART
            if (data.getHearts() < settings.getDefaultHearts()) {
                data.addHeart();
            }
            String triggerMsg = "§a💚 " + teamName + " " + playerName + " §f触发了「§b直接回一颗心§f」！当前 §c%d ❤️".formatted(data.getHearts());
            broadcastNotification(server, "trigger", triggerMsg);
            server.getPlayerManager().broadcast(Text.literal(triggerMsg), false);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 2.0f);
            }
            replaceWordForPlayer(data, server, depth + 1);
            syncOnePlayer(server, data);
        }
    }

    public void checkWinConditionPublic(MinecraftServer server) {
        checkWinCondition(server);
    }

    /**
     * 准备阶段每秒 tick（由 GameCountdownManager 调用）
     */
    public void tickPrepPhase(MinecraftServer server) {
        if (prepCountdown <= 0) return;
        prepCountdown--;
        if (prepCountdown > 0) {
            sendPrepCountdown(server, prepCountdown);
            // 原版标题显示倒计时
            Text titleText = Text.literal("§6§l" + prepCountdown);
            Text subText = Text.literal("§e游戏即将开始…");
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 15, 5));
                p.networkHandler.sendPacket(new TitleS2CPacket(titleText));
                p.networkHandler.sendPacket(new SubtitleS2CPacket(subText));
            }
        } else {
            // 清除标题
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.networkHandler.sendPacket(new TitleS2CPacket(Text.empty()));
                p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.empty()));
            }
            server.getPlayerManager().broadcast(
                    Text.literal("§a🎮 游戏正式开始！"), false);
            startPhase2(server);
        }
    }

    // ==================== 区块范围辅助方法 ====================

    /** 检测目标区块区域是否主要为海洋生物群系 */
    private boolean isOceanArea(ServerWorld world, int chunkX, int chunkZ, int range) {
        int sampleCount = 0;
        int oceanCount = 0;
        for (int dx = 0; dx < range; dx++) {
            for (int dz = 0; dz < range; dz++) {
                BlockPos pos = new BlockPos((chunkX + dx) * 16 + 8, 64, (chunkZ + dz) * 16 + 8);
                var biomeEntry = world.getBiome(pos);
                biomeEntry.getKey().ifPresent(key -> {
                    String path = key.getValue().getPath();
                    // 不计数，仅做标记 —— 使用共享可变容器
                });
                String path = biomeEntry.getKey().map(k -> k.getValue().getPath()).orElse("");
                sampleCount++;
                if (path.contains("ocean") || path.contains("deep_")) {
                    oceanCount++;
                }
            }
        }
        // 如果半数以上区块是海洋，则判定为海洋区域
        return sampleCount > 0 && oceanCount > sampleCount / 2;
    }

    /** 保存边界区域内的所有非空气方块和方块实体 */
    private void saveAreaBlocks(ServerWorld world) {
        savedBlocks = new HashMap<>();
        savedBlockEntities = new HashMap<>();
        int minBlockX = startChunkX * 16;
        int minBlockZ = startChunkZ * 16;
        int maxBlockX = (startChunkX + settings.getGameRange()) * 16 - 1;
        int maxBlockZ = (startChunkZ + settings.getGameRange()) * 16 - 1;
        int minY = world.getBottomY();
        int maxY = world.getTopY(Heightmap.Type.WORLD_SURFACE, startChunkX * 16, startChunkZ * 16) + 64; // 保守上界

        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir()) {
                        savedBlocks.put(pos, state);
                    }
                }
            }
        }
    }

    /**
     * 手动搜索安全的生成 Y 坐标（不依赖可能过期的 Heightmap）。
     * 从世界顶部向下搜索第一个实心方块，确认其上方 2 格空气（脚 + 头）后返回脚部 Y。
     */
    private int findSafeSurfaceY(ServerWorld world, int x, int z) {
        int bottomY = world.getBottomY();
        int topY = bottomY + world.getHeight() - 1;
        for (int y = topY; y >= bottomY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            // 跳过空气和非碰撞方块（高草丛、花等），只找实心方块作为地面
            if (!state.blocksMovement()) continue;
            // 脚部 (y+1) 和头部 (y+2) 必须没有碰撞（玩家可以站在高草丛中，但不能站在实心方块中）
            BlockState above1 = world.getBlockState(pos.up());
            BlockState above2 = world.getBlockState(pos.up(2));
            if (!above1.blocksMovement() && !above2.blocksMovement()) {
                return y + 1;
            }
        }
        return bottomY + 1;
    }

    /** 恢复边界区域内的方块到原始状态 */
    private void restoreAreaBlocks(ServerWorld world) {
        if (savedBlocks == null || savedBlockEntities == null) return;

        int minBlockX = startChunkX * 16;
        int minBlockZ = startChunkZ * 16;
        int maxBlockX = (startChunkX + settings.getGameRange()) * 16 - 1;
        int maxBlockZ = (startChunkZ + settings.getGameRange()) * 16 - 1;
        int minY = world.getBottomY();
        int maxY = world.getTopY(Heightmap.Type.WORLD_SURFACE, startChunkX * 16, startChunkZ * 16) + 64;

        // 先清空所有方块
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (savedBlocks.containsKey(pos)) continue; // 稍后恢复
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                }
            }
        }

        // 恢复原始方块
        for (Map.Entry<BlockPos, BlockState> entry : savedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            world.setBlockState(pos, entry.getValue(), 2);
            // 方块实体 NBT 恢复跳过（1.21.4 API 变更，游戏场景下区块状态恢复即可）
        }

        savedBlocks = null;
        savedBlockEntities = null;
    }

    /**
     * 建造黑曜石边界墙（基岩层 ~ Y=225），四边全覆盖
     * 墙高可能超出 saveAreaBlocks 范围，endGame 时需显式清理。
     */
    private void buildObsidianWall(ServerWorld world, int gameRange) {
        obsidianWallPositions.clear();
        int minBlockX = startChunkX * 16;
        int minBlockZ = startChunkZ * 16;
        int maxBlockX = (startChunkX + gameRange) * 16 - 1;
        int maxBlockZ = (startChunkZ + gameRange) * 16 - 1;
        int bottomY = world.getBottomY();
        int topY = 225;
        BlockState obsidian = Blocks.OBSIDIAN.getDefaultState();

        // 北边墙 (z = minBlockZ)
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int y = bottomY; y <= topY; y++) {
                BlockPos pos = new BlockPos(x, y, minBlockZ);
                world.setBlockState(pos, obsidian, 2);
                obsidianWallPositions.add(pos);
            }
        }
        // 南边墙 (z = maxBlockZ)
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int y = bottomY; y <= topY; y++) {
                BlockPos pos = new BlockPos(x, y, maxBlockZ);
                world.setBlockState(pos, obsidian, 2);
                obsidianWallPositions.add(pos);
            }
        }
        // 西边墙 (x = minBlockX)，跳过四角已放置的
        for (int z = minBlockZ + 1; z < maxBlockZ; z++) {
            for (int y = bottomY; y <= topY; y++) {
                BlockPos pos = new BlockPos(minBlockX, y, z);
                world.setBlockState(pos, obsidian, 2);
                obsidianWallPositions.add(pos);
            }
        }
        // 东边墙 (x = maxBlockX)，跳过四角已放置的
        for (int z = minBlockZ + 1; z < maxBlockZ; z++) {
            for (int y = bottomY; y <= topY; y++) {
                BlockPos pos = new BlockPos(maxBlockX, y, z);
                world.setBlockState(pos, obsidian, 2);
                obsidianWallPositions.add(pos);
            }
        }
    }

    /**
     * 在指定位置生成 3×3 黑曜石平台（防 TNT 破坏出生点）。
     * @param world 目标世界
     * @param centerX 平台中心 X
     * @param groundY 平台所在 Y（地表方块层）
     * @param centerZ 平台中心 Z
     */
    private void buildObsidianPlatform(ServerWorld world, int centerX, int groundY, int centerZ) {
        BlockState obsidian = Blocks.OBSIDIAN.getDefaultState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = new BlockPos(centerX + dx, groundY, centerZ + dz);
                world.setBlockState(pos, obsidian, 2);
            }
        }
    }

    /** 发送边界数据到所有客户端 */
    private void sendBoundaryPacket(MinecraftServer server) {
        GamePackets.GameBoundaryPayload payload = new GamePackets.GameBoundaryPayload(
                boundMinX, boundMinZ, boundMaxX, boundMaxZ);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** 清除客户端边界渲染 */
    private void sendBoundaryClearPacket(MinecraftServer server) {
        GamePackets.GameBoundaryPayload payload = new GamePackets.GameBoundaryPayload(
                -1, -1, -1, -1);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** 同步准备阶段倒计时到客户端 */
    private void sendPrepCountdown(MinecraftServer server, int seconds) {
        GamePackets.PrepCountdownPayload payload = new GamePackets.PrepCountdownPayload(seconds);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    // ==================== enforceBoundary / syncPlayerData ====================

    /**
     * 检查并强制所有存活玩家留在边界内
     * 由 GameCountdownManager 每秒调用
     */
    public void enforceBoundary(MinecraftServer server) {
        if ((!isRunning() && !isPrepPhase()) || boundMinX < 0) return;

        ServerWorld overworld = server.getOverworld();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // 准备阶段（无敌期间）所有玩家都受边界约束
            boolean isPrep = isPrepPhase();
            double x = player.getX();
            double z = player.getZ();
            boolean outOfBounds = x < boundMinX || x > boundMaxX || z < boundMinZ || z > boundMaxZ;

            if (outOfBounds) {
                if (isPrep) {
                    // 准备阶段传送回高空
                    player.teleport(overworld, boundCenterX + 0.5, 150, boundCenterZ + 0.5,
                            Set.of(), player.getYaw(), player.getPitch(), false);
                } else {
                    PlayerWordData data = playerDataMap.get(player.getUuid());
                    if (data == null || data.isEliminated()) continue;
                    if (player.isSpectator()) continue;
                    int surfaceY = findSafeSurfaceY(overworld, (int) boundCenterX, (int) boundCenterZ);
                    player.teleport(overworld, boundCenterX + 0.5, surfaceY, boundCenterZ + 0.5,
                            Set.of(), player.getYaw(), player.getPitch(), false);
                    player.sendMessage(Text.literal("§c⚠ 你已超出游戏范围，已传送回游戏区域！"), true);
                }
            }
        }
    }

    /** 同步单个玩家数据（供 SpecialEventPool 等外部调用） */
    public void syncPlayerDataPublic(MinecraftServer server, PlayerWordData data) {
        syncOnePlayer(server, data);
    }

    private void checkWinCondition(MinecraftServer server) {
        List<PlayerWordData> alive = playerDataMap.values().stream()
                .filter(d -> !d.isEliminated()).toList();
        if (alive.size() == 1) {
            PlayerWordData winnerData = alive.get(0);
            ServerPlayerEntity winner = server.getPlayerManager().getPlayer(winnerData.getPlayerId());
            if (winner != null) {
                state = GameState.ENDING;
                String teamName = winnerData.getTeamColor().getDisplayName();
                server.getPlayerManager().broadcast(
                        Text.literal("§6🏆 " + teamName + " §f是最后的幸存者！（词条：§b" + winnerData.getWordText() + "§f）"), false);

                // 放烟花
                spawnFireworks((ServerWorld) winner.getEntityWorld(),
                        new Vec3d(winner.getX(), winner.getY(), winner.getZ()));

                // 同步结束通知给所有客户端
                GamePackets.GameEndPayload endPayload = new GamePackets.GameEndPayload(
                        winner.getUuid(), winner.getName().getString(), teamName);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(p, endPayload);
                }

                // 3 秒后彻底结束
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    server.execute(() -> {
                        endGame(server);
                    });
                }).start();
            }
        }
    }

    private void broadcastNotification(MinecraftServer server, String type, String message) {
        GamePackets.NotificationPayload payload = new GamePackets.NotificationPayload(type, message);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** 向所有玩家全量同步：每个人能看到除自己外所有人的词条（含队伍、计时） */
    private void syncAllWords(List<ServerPlayerEntity> allPlayers) {
        for (ServerPlayerEntity viewer : allPlayers) {
            List<PlayerWordData> visibleWords = new ArrayList<>();
            for (ServerPlayerEntity target : allPlayers) {
                if (viewer.getUuid().equals(target.getUuid())) continue;
                PlayerWordData td = playerDataMap.get(target.getUuid());
                if (td != null) visibleWords.add(td);
            }
            ServerPlayNetworking.send(viewer,
                    GamePackets.SyncAllWordsPayload.fromPlayerData(visibleWords));
        }
    }

    /** 增量同步单个玩家状态 */
    private void syncOnePlayer(MinecraftServer server, PlayerWordData data) {
        GamePackets.SyncOnePlayerPayload payload = new GamePackets.SyncOnePlayerPayload(
                data.getPlayerId(), data.getTeamColor().name(), data.getWordText(),
                data.getHearts(), data.isEliminated(), data.getCountdownSeconds(),
                settings.getWordChangeTimerSeconds(), settings.getDefaultHearts());
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    /** 复活所有淘汰玩家 */
    private void respawnAll(MinecraftServer server) {
        for (PlayerWordData data : playerDataMap.values()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(data.getPlayerId());
            if (player != null && player.isSpectator()) {
                player.changeGameMode(GameMode.SURVIVAL);
            }
        }
    }

    /**
     * 玩家死亡后复活处理：重新添加高亮效果（死亡会清除所有状态效果）。
     * 复活位置由 setSpawnPoint 设定的出生点自动决定，无需额外传送。
     * 由 WordTriggerDetector 的 AFTER_RESPAWN 事件调用。
     */
    public void handlePlayerRespawn(ServerPlayerEntity player) {
        if (!isRunning() && !isPrepPhase()) return;

        PlayerWordData data = playerDataMap.get(player.getUuid());
        if (data == null) return;

        // 重新添加 GLOWING 高亮（死亡会清除所有状态效果）
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,
                StatusEffectInstance.INFINITE, 0, false, false));
    }

    // ==================== 书本发放 ====================

    /** 向玩家发放游戏书本（避免重复发放） */
    public static void giveGameBook(ServerPlayerEntity player) {
        Item gameBook = INSTANCE.gameBookItem;
        if (gameBook == null) return;

        // 检查是否已有书本
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).getItem() == gameBook) {
                return; // 已有，不重复发放
            }
        }

        ItemStack bookStack = new ItemStack(gameBook);
        bookStack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§6不要做挑战 §7(右键打开)"));
        player.getInventory().insertStack(bookStack);
    }

    // ==================== 原版计分板队伍 ====================

    /** 创建原版计分板队伍并分配玩家，队伍名格式为 ddi_{color} */
    private void assignVanillaTeams(MinecraftServer server, List<ServerPlayerEntity> shuffled) {
        ServerScoreboard scoreboard = server.getScoreboard();

        // 先清理旧的 ddi_ 队伍
        for (TeamColor color : TeamColor.values()) {
            String teamName = "ddi_" + color.name().toLowerCase();
            Team existing = scoreboard.getTeam(teamName);
            if (existing != null) {
                scoreboard.removeTeam(existing);
            }
        }

        // 为每种颜色创建队伍
        for (TeamColor color : TeamColor.values()) {
            String teamName = "ddi_" + color.name().toLowerCase();
            Team team = scoreboard.addTeam(teamName);
            team.setColor(color.getFormatting());
            team.setDisplayName(Text.literal(color.getDisplayName()));
        }

        // 分配玩家到对应队伍
        for (ServerPlayerEntity player : shuffled) {
            PlayerWordData data = playerDataMap.get(player.getUuid());
            if (data == null) continue;
            String teamName = "ddi_" + data.getTeamColor().name().toLowerCase();
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                scoreboard.addScoreHolderToTeam(player.getName().getString(), team);
            }
        }
    }

    /** 清理所有 ddi_ 前缀的原版队伍 */
    private void removeVanillaTeams(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        for (TeamColor color : TeamColor.values()) {
            String teamName = "ddi_" + color.name().toLowerCase();
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                scoreboard.removeTeam(team);
            }
        }
    }

    // ==================== 烟花 ====================

    private void spawnFireworks(ServerWorld world, Vec3d pos) {
        MinecraftServer server = world.getServer();
        for (int i = 0; i < 5; i++) {
            int delay = i * 10; // ticks
            ItemStack fireworkStack = makeRandomFireworkStack();
            double dx = (random.nextDouble() - 0.5) * 2;
            double dz = (random.nextDouble() - 0.5) * 2;
            if (delay == 0) {
                // 立即发射第一枚
                FireworkRocketEntity fw = new FireworkRocketEntity(world,
                        pos.x + dx, pos.y + 1, pos.z + dz, fireworkStack);
                world.spawnEntity(fw);
            } else {
                // 延迟发射（使用 server tick 调度，不阻塞主线程）
                server.execute(() -> {
                    try { Thread.sleep(delay * 50L); } catch (InterruptedException e) { return; }
                    server.execute(() -> {
                        FireworkRocketEntity fw = new FireworkRocketEntity(world,
                                pos.x + dx, pos.y + 1, pos.z + dz, fireworkStack);
                        world.spawnEntity(fw);
                    });
                });
            }
        }
    }

    private ItemStack makeRandomFireworkStack() {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        int[] colorIds = {0xFF5555, 0x55FF55, 0x5555FF, 0xFFFF55, 0xFF55FF, 0x55FFFF, 0xFFAA00, 0xFFFFFF};
        int c1 = colorIds[random.nextInt(colorIds.length)];
        int c2 = colorIds[random.nextInt(colorIds.length)];

        FireworkExplosionComponent.Type[] types = FireworkExplosionComponent.Type.values();
        IntList colors = new IntArrayList(new int[]{c1, c2});
        IntList fadeColors = new IntArrayList(new int[]{});
        FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                types[random.nextInt(types.length)],
                colors,
                fadeColors,
                random.nextBoolean(),  // trail
                random.nextBoolean()   // twinkle
        );
        FireworksComponent fireworks = new FireworksComponent(
                random.nextInt(2) + 1,  // flight duration 1-2
                List.of(explosion)
        );
        stack.set(DataComponentTypes.FIREWORKS, fireworks);
        return stack;
    }
}

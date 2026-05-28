package me.baiyueyue.dont_do_it.game;

import me.baiyueyue.dont_do_it.network.GamePackets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

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
    private final Map<UUID, PlayerWordData> playerDataMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /** 游戏书本物品引用，由 Dont_do_it 入口注入 */
    private Item gameBookItem;

    /** 记录每个玩家每种触发类型的上次触发 tick，用于冷却（防潜行/疾跑连发） */
    private final Map<UUID, Map<TriggerType, Long>> lastTriggerTick = new ConcurrentHashMap<>();

    private GameManager() {}

    public static GameManager getInstance() { return INSTANCE; }

    /** 注入游戏书本物品引用 */
    public void setGameBookItem(Item item) { this.gameBookItem = item; }

    // ==================== 状态访问 ====================

    public GameState getState() { return state; }
    public boolean isRunning() { return state == GameState.RUNNING; }
    public GameSettings getSettings() { return settings; }
    public PlayerWordData getPlayerData(UUID playerId) { return playerDataMap.get(playerId); }
    public Collection<PlayerWordData> getAllPlayerData() { return playerDataMap.values(); }

    // ==================== 游戏流程 ====================

    /**
     * 开始游戏 —— 分配队伍、分配词条、启动倒计时、同步客户端
     */
    public void startGame(MinecraftServer server) {
        if (state == GameState.RUNNING) return;

        playerDataMap.clear();
        lastTriggerTick.clear();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();

        if (players.size() < 2) {
            server.getPlayerManager().broadcast(
                    Text.literal("§c⚠ 至少需要 2 名玩家才能开始「不要做挑战」！"), false);
            return;
        }

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
                    GameSettings.DEFAULT_HEARTS, timerSec);
            playerDataMap.put(player.getUuid(), data);
        }

        state = GameState.RUNNING;

        // 清空所有玩家背包
        for (ServerPlayerEntity player : players) {
            player.getInventory().clear();
        }

        // 创建原版计分板队伍并分配玩家
        assignVanillaTeams(server, shuffled);

        // 全量同步给每个玩家
        syncAllWords(players);

        // 广播开始
        server.getPlayerManager().broadcast(
                Text.literal("§a🎮 「不要做挑战」开始！每人 §c%d ❤️ §a| 词条每 §e%d秒 §a自动更换"
                        .formatted(GameSettings.DEFAULT_HEARTS, timerSec)), false);
    }

    /**
     * 结束游戏 —— 清理并重置
     */
    public void endGame(MinecraftServer server) {
        state = GameState.ENDED;
        respawnAll(server);
        removeVanillaTeams(server);

        // 清空背包并发还游戏书本
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.getInventory().clear();
            if (gameBookItem != null) {
                giveGameBook(player);
            }
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

    // ==================== 内部方法 ====================

    private void replaceWordForPlayer(PlayerWordData data, MinecraftServer server) {
        WordPool.WordEntry newEntry = wordPool.drawSingle();
        data.replaceWord(newEntry, settings.getWordChangeTimerSeconds());
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
                        for (PlayerWordData data : playerDataMap.values()) {
                            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(data.getPlayerId());
                            if (sp != null) {
                                sp.changeGameMode(GameMode.SURVIVAL);
                                ServerWorld overworld = server.getOverworld();
                                sp.teleport(overworld, 0.5, overworld.getTopY(
                                        net.minecraft.world.Heightmap.Type.WORLD_SURFACE, 0, 0), 0.5,
                                        Set.of(), sp.getYaw(), sp.getPitch(), false);
                            }
                        }
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
                data.getHearts(), data.isEliminated(), data.getCountdownSeconds());
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
        for (int i = 0; i < 5; i++) {
            // 延迟发射，产生连续烟花效果
            int delay = i * 10; // ticks
            world.getServer().execute(() -> {
                FireworkRocketEntity firework = new FireworkRocketEntity(world,
                        pos.x + (random.nextDouble() - 0.5) * 2,
                        pos.y + 1,
                        pos.z + (random.nextDouble() - 0.5) * 2,
                        makeRandomFireworkStack());
                world.spawnEntity(firework);
            });

            // 简单延迟（使用 server tick 调度）
            if (delay > 0) {
                try { Thread.sleep(delay * 50L); } catch (InterruptedException ignored) {}
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

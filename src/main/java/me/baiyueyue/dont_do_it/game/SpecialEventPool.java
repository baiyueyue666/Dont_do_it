package me.baiyueyue.dont_do_it.game;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * 特殊事件池 —— 管理特殊事件的随机抽取和执行
 */
public class SpecialEventPool {

    private final List<SpecialEventType> eventTypes = new ArrayList<>();
    private final java.util.Random random = new java.util.Random();

    public SpecialEventPool() {
        initDefaultEvents();
    }

    private void initDefaultEvents() {
        eventTypes.add(SpecialEventType.MONSTER_RAMPAGE);
    }

    /** 随机抽取一个特殊事件 */
    public SpecialEventType drawRandom() {
        return eventTypes.get(random.nextInt(eventTypes.size()));
    }

    public int size() { return eventTypes.size(); }

    // ==================== 事件执行 ====================

    /**
     * 执行特殊事件
     * @return 事件实际持续时间（秒），0 表示瞬时完成
     */
    public int executeEvent(MinecraftServer server, SpecialEventType type) {
        return switch (type) {
            case MONSTER_RAMPAGE -> executeMonsterRampage(server);
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
}

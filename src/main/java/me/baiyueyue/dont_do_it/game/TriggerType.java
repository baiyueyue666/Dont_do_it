package me.baiyueyue.dont_do_it.game;

/**
 * 词条触发类型枚举 —— 每个词条绑定一种可被事件系统检测的行为类型
 */
public enum TriggerType {
    SNEAK("潜行"),
    ATTACK("攻击生物"),
    ATTACK_HOSTILE("打怪"),
    BLOCK_BREAK("破坏方块"),
    MINE_ORE("挖矿"),
    BLOCK_PLACE("放置方块"),
    CHAT("发送聊天"),
    TAKE_DAMAGE("受到伤害"),
    EAT("吃东西"),
    SPRINT("疾跑"),
    DROP_ITEM("丢弃物品"),
    OPEN_CONTAINER("打开容器"),
    PICKUP_ITEM("捡起物品"),

    // ---- 挖掘类（破坏方块） ----
    MINE_WOOD("挖掘木头"),
    MINE_STONE("挖掘石头"),
    MINE_COAL("挖掘煤矿"),
    MINE_IRON("挖掘铁矿"),
    MINE_COPPER("挖掘铜矿"),
    MINE_GOLD("挖掘金矿"),
    MINE_DIAMOND("挖掘钻石矿"),

    // ---- 拾取类 ----
    PICKUP_WOOD("拾取原木"),

    // ---- 合成类 ----
    CRAFT_CRAFTING_TABLE("合成工作台"),
    CRAFT_WOODEN_PICKAXE("合成木镐"),
    CRAFT_STONE_PICKAXE("合成石镐"),
    CRAFT_IRON_PICKAXE("合成铁镐"),
    CRAFT_WOODEN_AXE("合成木斧"),
    CRAFT_STONE_AXE("合成石斧"),
    CRAFT_IRON_AXE("合成铁斧"),
    CRAFT_WOODEN_SWORD("合成木剑"),
    CRAFT_STONE_SWORD("合成石剑"),
    CRAFT_IRON_SWORD("合成铁剑"),

    // ---- 视角方向类 ----
    LOOK_DOWN("低头"),
    LOOK_UP("抬头"),
    LOOK_EAST("看向东方"),
    LOOK_SOUTH("看向南方"),
    LOOK_WEST("看向西方"),
    LOOK_NORTH("看向北方"),

    // ---- 持续行为类 ----
    STAND_STILL_5S("禁止不动五秒"),
    LOOK_SAME_DIR_5S("持续看向一个方向五秒"),

    // ---- 环境状态类 ----
    ENCLOSED_1X2("自闭"),
    SUBMERGED("沉入水中"),

    // ---- 挖掘细分 ----
    MINE_ANDESITE("挖掘安山岩"),
    MINE_DIORITE("挖掘闪长岩"),
    MINE_DEEPSLATE("挖掘深板岩"),

    // ---- 站立方块类 ----
    STAND_ON_GRASS("站在草方块上"),
    STAND_ON_LEAVES("站在树叶上"),
    STAND_ON_STONE("站在石头上"),
    STAND_ON_DEEPSLATE("站在深板岩上"),
    STAND_ON_ANDESITE("站在安山岩上"),
    STAND_ON_DIORITE("站在闪长岩上"),

    // ---- 浮空 ----
    FLOATING("浮空"),

    // ---- 死亡/复活类 ----
    DEATH("死亡"),
    RESPAWN("复活"),
    NOT_RESPAWN_3S("三秒不复活"),
    NOT_RESPAWN_5S("五秒不复活"),
    NOT_RESPAWN_10S("十秒不复活"),

    // ---- 拾取细分 ----
    PICKUP_DIAMOND("获得钻石"),

    // ---- 背包物品类 ----
    HAS_COAL("背包里有煤炭"),
    HAS_IRON_INGOT("背包里有铁锭"),
    HAS_COPPER_INGOT("背包里有铜锭"),
    HAS_CRAFTING_TABLE("背包里有工作台"),
    HAS_FURNACE("背包里有熔炉"),
    HAS_AXE("背包里有斧头"),
    HAS_SWORD("背包里有剑"),
    HAS_STONE_PICKAXE("背包里有石镐"),
    HAS_WOODEN_PICKAXE("背包里有木镐"),
    HAS_IRON_PICKAXE("背包里有铁镐"),
    HAS_ROTTEN_FLESH("背包里有腐肉"),
    HAS_DIAMOND("背包里有钻石"),
    HAS_DIRT("背包里有泥土");

    private final String displayName;

    TriggerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

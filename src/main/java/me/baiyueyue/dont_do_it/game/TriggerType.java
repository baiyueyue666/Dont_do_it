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
    CRAFT_IRON_SWORD("合成铁剑");

    private final String displayName;

    TriggerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

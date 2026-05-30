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
    PICKUP_ITEM("捡起物品");

    private final String displayName;

    TriggerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

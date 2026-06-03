package me.baiyueyue.dont_do_it.game;

import net.minecraft.entity.boss.BossBar;

/**
 * 特殊事件类型 —— 全局事件，周期触发影响所有存活玩家
 */
public enum SpecialEventType {
    MONSTER_RAMPAGE("怪物狂潮", 0, BossBar.Color.RED),
    DIAMOND_GIFT("钻石馈赠", 0, BossBar.Color.YELLOW),
    DIAMOND_BLESSING("钻石祝福", 120, BossBar.Color.BLUE),
    DIAMOND_CURSE("钻石诅咒", 0, BossBar.Color.RED),
    ECLIPSE_CURSE("日食诅咒", 0, BossBar.Color.PURPLE),
    CALM("平静", 0, BossBar.Color.WHITE),
    CLOUD_EFFECT("唉，云朵？", 0, BossBar.Color.WHITE),
    FOOD_RAIN("美食雨", 10, BossBar.Color.GREEN),
    XP_STORM("经验风暴", 10, BossBar.Color.GREEN),
    LIFE_BLESSING("生命赐福", 10, BossBar.Color.PINK),
    ORE_UNDERFOOT("脚下出矿", 10, BossBar.Color.BLUE),
    ANVIL_STORM("铁砧暴雨", 10, BossBar.Color.RED),
    TNT_RAIN("TNT降雨", 0, BossBar.Color.RED),
    CAVE_IN("地底塌陷", 30, BossBar.Color.PURPLE),
    PUMPKIN_HEAD("全员南瓜头", 60, BossBar.Color.YELLOW),
    INVENTORY_SHUFFLE("物品栏洗牌", 0, BossBar.Color.BLUE),
    CHICKEN_RAIN("小鸡天降", 0, BossBar.Color.WHITE),
    PLAYER_SWAP("玩家互换位置", 0, BossBar.Color.PURPLE),
    FIRE_TRAIL("脚步生火", 30, BossBar.Color.RED),
    CAGE_TRIAL("囚笼试炼", 10, BossBar.Color.RED),
    SKY_WATER_CHALLENGE("高空落水挑战", 30, BossBar.Color.BLUE),
    CROP_SPEED_GROW("作物速成", 15, BossBar.Color.GREEN),
    DURABILITY_BLESSING("豁免祝福", 120, BossBar.Color.PINK),
    EQUIPMENT_RUST("装备锈蚀", 120, BossBar.Color.RED),
    HUNGER_DISEASE("饥饿疫病", 30, BossBar.Color.RED),
    INVENTORY_MIGRATION("物资迁徙", 0, BossBar.Color.RED),
    EVERYONE_BABY("全员变幼体", 60, BossBar.Color.GREEN),
    SLIME_POSSESSION("粘液附身", 30, BossBar.Color.GREEN),
    ARROW_TRIAL("箭雨试炼", 10, BossBar.Color.RED),
    TRADE_MERCHANT("交易商人", 30, BossBar.Color.YELLOW, 1);

    private final String displayName;
    /** 持续时间（秒），0 表示立即触发一次 */
    private final int durationSeconds;
    private final BossBar.Color bossBarColor;
    /** 权重：数值越大被抽中的概率越高，默认 100 */
    private final int weight;

    SpecialEventType(String displayName, int durationSeconds, BossBar.Color bossBarColor) {
        this(displayName, durationSeconds, bossBarColor, 100);
    }

    SpecialEventType(String displayName, int durationSeconds, BossBar.Color bossBarColor, int weight) {
        this.displayName = displayName;
        this.durationSeconds = durationSeconds;
        this.bossBarColor = bossBarColor;
        this.weight = weight;
    }

    public String getDisplayName() { return displayName; }
    public int getDurationSeconds() { return durationSeconds; }
    public BossBar.Color getBossBarColor() { return bossBarColor; }
    public int getWeight() { return weight; }

    /** 是否为瞬时事件（无持续时长） */
    public boolean isInstant() { return durationSeconds <= 0; }
}

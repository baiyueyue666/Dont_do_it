package me.baiyueyue.dont_do_it.game;

import net.minecraft.entity.boss.BossBar;

/**
 * 特殊事件类型 —— 全局事件，周期触发影响所有存活玩家
 */
public enum SpecialEventType {
    MONSTER_RAMPAGE("怪物狂潮", 0, BossBar.Color.RED);

    private final String displayName;
    /** 持续时间（秒），0 表示立即触发一次 */
    private final int durationSeconds;
    private final BossBar.Color bossBarColor;

    SpecialEventType(String displayName, int durationSeconds, BossBar.Color bossBarColor) {
        this.displayName = displayName;
        this.durationSeconds = durationSeconds;
        this.bossBarColor = bossBarColor;
    }

    public String getDisplayName() { return displayName; }
    public int getDurationSeconds() { return durationSeconds; }
    public BossBar.Color getBossBarColor() { return bossBarColor; }

    /** 是否为瞬时事件（无持续时长） */
    public boolean isInstant() { return durationSeconds <= 0; }
}

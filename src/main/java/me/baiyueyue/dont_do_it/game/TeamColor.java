package me.baiyueyue.dont_do_it.game;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.util.Formatting;

/**
 * 队伍颜色 —— 玩家被随机分配到一个颜色队伍
 */
public enum TeamColor {
    RED("§c红队", 0xFF5555, Formatting.RED, BossBar.Color.RED),
    BLUE("§9蓝队", 0x5555FF, Formatting.BLUE, BossBar.Color.BLUE),
    GREEN("§a绿队", 0x55FF55, Formatting.GREEN, BossBar.Color.GREEN),
    YELLOW("§e黄队", 0xFFFF55, Formatting.YELLOW, BossBar.Color.YELLOW),
    PURPLE("§d紫队", 0xFF55FF, Formatting.DARK_PURPLE, BossBar.Color.PURPLE),
    ORANGE("§6橙队", 0xFFAA00, Formatting.GOLD, BossBar.Color.YELLOW),
    CYAN("§b青队", 0x55FFFF, Formatting.AQUA, BossBar.Color.BLUE),
    PINK("§d粉队", 0xFF88AA, Formatting.LIGHT_PURPLE, BossBar.Color.PINK);

    private final String displayName;
    private final int colorRgb;
    private final Formatting formatting;
    private final BossBar.Color bossBarColor;

    TeamColor(String displayName, int colorRgb, Formatting formatting, BossBar.Color bossBarColor) {
        this.displayName = displayName;
        this.colorRgb = colorRgb;
        this.formatting = formatting;
        this.bossBarColor = bossBarColor;
    }

    public String getDisplayName() { return displayName; }
    public int getColorRgb() { return colorRgb; }
    public Formatting getFormatting() { return formatting; }
    public BossBar.Color getBossBarColor() { return bossBarColor; }

    /** 根据索引循环取色 */
    public static TeamColor fromIndex(int index) {
        TeamColor[] values = values();
        return values[Math.abs(index) % values.length];
    }
}

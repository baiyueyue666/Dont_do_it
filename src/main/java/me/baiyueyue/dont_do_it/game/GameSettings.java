package me.baiyueyue.dont_do_it.game;

/**
 * 游戏设置 —— 全局可配置项
 */
public class GameSettings {

    public static final int DEFAULT_HEARTS = 15;
    public static final int DEFAULT_TIMER_SECONDS = 60;
    public static final int DEFAULT_SPECIAL_EVENT_TIMER_SECONDS = 180;

    /** 词条更换倒计时选项（秒） */
    public static final int[] TIMER_OPTIONS = {60, 120, 180, 300};

    /** 特殊事件触发倒计时选项（秒），0 表示关闭 */
    public static final int[] SPECIAL_EVENT_TIMER_OPTIONS = {0, 60, 180, 300, 420};

    /** 血量上限选项 */
    public static final int[] HEART_OPTIONS = {3, 5, 10, 15, 30};

    private int wordChangeTimerSeconds = DEFAULT_TIMER_SECONDS;
    private int specialEventTimerSeconds = DEFAULT_SPECIAL_EVENT_TIMER_SECONDS;
    private int defaultHearts = DEFAULT_HEARTS;

    public int getWordChangeTimerSeconds() { return wordChangeTimerSeconds; }

    public void setWordChangeTimerSeconds(int seconds) { this.wordChangeTimerSeconds = seconds; }

    public int getSpecialEventTimerSeconds() { return specialEventTimerSeconds; }

    public void setSpecialEventTimerSeconds(int seconds) { this.specialEventTimerSeconds = seconds; }

    public int getDefaultHearts() { return defaultHearts; }

    public void setDefaultHearts(int hearts) { this.defaultHearts = hearts; }

    /** 特殊事件是否启用 */
    public boolean isSpecialEventEnabled() { return specialEventTimerSeconds > 0; }

    /** 循环切换到下一个计时选项 */
    public int nextTimerOption() {
        for (int i = 0; i < TIMER_OPTIONS.length; i++) {
            if (TIMER_OPTIONS[i] == wordChangeTimerSeconds) {
                wordChangeTimerSeconds = TIMER_OPTIONS[(i + 1) % TIMER_OPTIONS.length];
                return wordChangeTimerSeconds;
            }
        }
        wordChangeTimerSeconds = TIMER_OPTIONS[0];
        return wordChangeTimerSeconds;
    }

    /** 循环切换到下一个特殊事件计时选项 */
    public int nextSpecialEventTimerOption() {
        for (int i = 0; i < SPECIAL_EVENT_TIMER_OPTIONS.length; i++) {
            if (SPECIAL_EVENT_TIMER_OPTIONS[i] == specialEventTimerSeconds) {
                specialEventTimerSeconds = SPECIAL_EVENT_TIMER_OPTIONS[(i + 1) % SPECIAL_EVENT_TIMER_OPTIONS.length];
                return specialEventTimerSeconds;
            }
        }
        specialEventTimerSeconds = SPECIAL_EVENT_TIMER_OPTIONS[0];
        return specialEventTimerSeconds;
    }

    /** 循环切换到下一个血量上限选项 */
    public int nextHeartOption() {
        for (int i = 0; i < HEART_OPTIONS.length; i++) {
            if (HEART_OPTIONS[i] == defaultHearts) {
                defaultHearts = HEART_OPTIONS[(i + 1) % HEART_OPTIONS.length];
                return defaultHearts;
            }
        }
        defaultHearts = HEART_OPTIONS[0];
        return defaultHearts;
    }

    public void reset() {
        wordChangeTimerSeconds = DEFAULT_TIMER_SECONDS;
        specialEventTimerSeconds = DEFAULT_SPECIAL_EVENT_TIMER_SECONDS;
        defaultHearts = DEFAULT_HEARTS;
    }
}

package me.baiyueyue.dont_do_it.game;

/**
 * 游戏设置 —— 全局可配置项
 */
public class GameSettings {

    public static final int DEFAULT_HEARTS = 15;
    public static final int DEFAULT_TIMER_SECONDS = 60;
    public static final int DEFAULT_SPECIAL_EVENT_TIMER_SECONDS = 180;

    /** 词条更换倒计时选项（秒） */
    public static final int[] TIMER_OPTIONS = {60, 120, 180};

    /** 特殊事件触发倒计时选项（秒） */
    public static final int[] SPECIAL_EVENT_TIMER_OPTIONS = {60, 180, 300, 420};

    private int wordChangeTimerSeconds = DEFAULT_TIMER_SECONDS;
    private int specialEventTimerSeconds = DEFAULT_SPECIAL_EVENT_TIMER_SECONDS;

    public int getWordChangeTimerSeconds() { return wordChangeTimerSeconds; }

    public void setWordChangeTimerSeconds(int seconds) { this.wordChangeTimerSeconds = seconds; }

    public int getSpecialEventTimerSeconds() { return specialEventTimerSeconds; }

    public void setSpecialEventTimerSeconds(int seconds) { this.specialEventTimerSeconds = seconds; }

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

    public void reset() {
        wordChangeTimerSeconds = DEFAULT_TIMER_SECONDS;
        specialEventTimerSeconds = DEFAULT_SPECIAL_EVENT_TIMER_SECONDS;
    }
}

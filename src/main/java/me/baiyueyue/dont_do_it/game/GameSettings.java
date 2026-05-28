package me.baiyueyue.dont_do_it.game;

/**
 * 游戏设置 —— 全局可配置项
 */
public class GameSettings {

    public static final int DEFAULT_HEARTS = 15;
    public static final int DEFAULT_TIMER_SECONDS = 60;

    /** 词条更换倒计时选项（秒） */
    public static final int[] TIMER_OPTIONS = {60, 120, 180};

    private int wordChangeTimerSeconds = DEFAULT_TIMER_SECONDS;

    public int getWordChangeTimerSeconds() { return wordChangeTimerSeconds; }

    public void setWordChangeTimerSeconds(int seconds) { this.wordChangeTimerSeconds = seconds; }

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

    public void reset() {
        wordChangeTimerSeconds = DEFAULT_TIMER_SECONDS;
    }
}

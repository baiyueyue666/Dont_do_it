package me.baiyueyue.dont_do_it.game;

/**
 * 游戏状态机
 */
public enum GameState {
    /** 等待中 —— 玩家可自由活动，手持书本可打开大厅 */
    WAITING,
    /** 准备阶段 —— 书本大厅已打开，等待房主点击开始 */
    LOBBY,
    /** 进行中 —— 词条已分配，检测/倒计时进行中 */
    RUNNING,
    /** 结束中 —— 烟花+3秒倒计时 */
    ENDING,
    /** 已重置 —— 所有人已传送回出生点 */
    ENDED
}

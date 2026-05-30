package me.baiyueyue.dont_do_it.game;

import java.util.UUID;

/**
 * 每个玩家的词条、队伍、生命、倒计时数据
 */
public class PlayerWordData {
    private final UUID playerId;
    private TeamColor teamColor;
    private String wordId;
    private String wordText;
    private TriggerType triggerType;
    private int hearts;
    private boolean eliminated;
    private int countdownSeconds; // 当前词条剩余时间（秒）

    public PlayerWordData(UUID playerId, TeamColor teamColor, WordPool.WordEntry entry, int initialHearts, int countdownSeconds) {
        this.playerId = playerId;
        this.teamColor = teamColor;
        this.wordId = entry.id();
        this.wordText = entry.displayText();
        this.triggerType = entry.triggerType();
        this.hearts = initialHearts;
        this.eliminated = false;
        this.countdownSeconds = countdownSeconds;
    }

    // ---------- Getters / Setters ----------

    public UUID getPlayerId() { return playerId; }
    public TeamColor getTeamColor() { return teamColor; }
    public String getWordId() { return wordId; }
    public String getWordText() { return wordText; }
    public TriggerType getTriggerType() { return triggerType; }
    public int getHearts() { return hearts; }
    public boolean isEliminated() { return eliminated; }
    public int getCountdownSeconds() { return countdownSeconds; }

    public void setTeamColor(TeamColor teamColor) { this.teamColor = teamColor; }

    // ---------- 生命操作 ----------

    /** 扣一颗心，返回是否被淘汰 */
    public boolean loseHeart() {
        if (eliminated) return true;
        hearts = Math.max(0, hearts - 1);
        if (hearts == 0) {
            eliminated = true;
        }
        return eliminated;
    }

    /** 加一颗心 */
    public void addHeart() {
        if (!eliminated) {
            hearts++;
        }
    }

    public void resetHearts(int initialHearts) {
        this.hearts = initialHearts;
        this.eliminated = false;
    }

    // ---------- 词条操作 ----------

    /** 替换词条（触发或计时结束时调用） */
    public void replaceWord(WordPool.WordEntry entry, int newCountdownSeconds) {
        this.wordId = entry.id();
        this.wordText = entry.displayText();
        this.triggerType = entry.triggerType();
        this.countdownSeconds = newCountdownSeconds;
    }

    // ---------- 倒计时操作 ----------

    /** 每秒 tick，返回是否到期（需要换词条） */
    public boolean tickCountdown() {
        if (eliminated) return false;
        if (countdownSeconds > 0) {
            countdownSeconds--;
        }
        return countdownSeconds <= 0;
    }

    public void resetCountdown(int seconds) {
        this.countdownSeconds = seconds;
    }
}

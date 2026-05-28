package me.baiyueyue.dont_do_it.game;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 词条池 —— 管理所有可用词条，支持分池随机抽取
 */
public class WordPool {

    /** 词条定义 */
    public record WordEntry(String id, String displayText, TriggerType triggerType) {}

    private final List<WordEntry> allWords = new ArrayList<>();
    private final Random random = new Random();

    public WordPool() {
        initDefaultWords();
    }

    /** 初始化默认测试词条 */
    private void initDefaultWords() {
        add("jump_01",    "跳跃",          TriggerType.JUMP);
        add("jump_02",    "蹦起来",         TriggerType.JUMP);
        add("sneak_01",   "潜行",          TriggerType.SNEAK);
        add("sneak_02",   "蹲下走",         TriggerType.SNEAK);
        add("attack_01",  "攻击生物",       TriggerType.ATTACK);
        add("attack_02",  "打怪",          TriggerType.ATTACK);
        add("break_01",   "破坏方块",       TriggerType.BLOCK_BREAK);
        add("break_02",   "挖矿",          TriggerType.BLOCK_BREAK);
        add("place_01",   "放置方块",       TriggerType.BLOCK_PLACE);
        add("place_02",   "搭方块",         TriggerType.BLOCK_PLACE);
        add("chat_01",    "发送聊天消息",    TriggerType.CHAT);
        add("chat_02",    "打字说话",       TriggerType.CHAT);
        add("damage_01",  "受到伤害",       TriggerType.TAKE_DAMAGE);
        add("eat_01",     "吃东西",         TriggerType.EAT);
        add("sprint_01",  "疾跑",          TriggerType.SPRINT);
        add("drop_01",    "丢弃物品",       TriggerType.DROP_ITEM);
        add("container_01","打开容器",       TriggerType.OPEN_CONTAINER);
        add("pickup_01",  "捡起物品",       TriggerType.PICKUP_ITEM);
    }

    public void add(String id, String displayText, TriggerType type) {
        allWords.add(new WordEntry(id, displayText, type));
    }

    /** 为指定数量的玩家各抽取一个不重复的词条 */
    public List<WordEntry> drawWords(int count) {
        if (count > allWords.size()) {
            throw new IllegalStateException("玩家数量(%d)超过词条池大小(%d)".formatted(count, allWords.size()));
        }
        List<WordEntry> shuffled = new ArrayList<>(allWords);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, count);
    }

    /** 随机抽取一个词条（可重复，用于触发/计时后替换） */
    public WordEntry drawSingle() {
        return allWords.get(random.nextInt(allWords.size()));
    }

    public List<WordEntry> getAllWords() { return Collections.unmodifiableList(allWords); }
    public int size() { return allWords.size(); }
}

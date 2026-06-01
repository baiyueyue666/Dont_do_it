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
        add("sneak_01",   "潜行",          TriggerType.SNEAK);
        add("attack_01",  "攻击生物",       TriggerType.ATTACK);
        add("attack_02",  "打怪",          TriggerType.ATTACK_HOSTILE);
        add("break_01",   "破坏方块",       TriggerType.BLOCK_BREAK);
        add("break_02",   "挖矿",          TriggerType.MINE_ORE);
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

        // ---- 挖掘类 ----
        add("mine_wood_01",   "挖掘木头",       TriggerType.MINE_WOOD);
        add("mine_stone_01",  "挖掘石头",       TriggerType.MINE_STONE);
        add("mine_coal_01",   "挖掘煤矿",       TriggerType.MINE_COAL);
        add("mine_iron_01",   "挖掘铁矿",       TriggerType.MINE_IRON);
        add("mine_copper_01", "挖掘铜矿",       TriggerType.MINE_COPPER);
        add("mine_gold_01",   "挖掘金矿",       TriggerType.MINE_GOLD);
        add("mine_diamond_01","挖掘钻石矿",      TriggerType.MINE_DIAMOND);

        // ---- 拾取类 ----
        add("pickup_wood_01", "拾取原木",       TriggerType.PICKUP_WOOD);

        // ---- 合成类 ----
        add("craft_table_01",       "合成工作台", TriggerType.CRAFT_CRAFTING_TABLE);
        add("craft_wooden_pickaxe", "合成木镐",   TriggerType.CRAFT_WOODEN_PICKAXE);
        add("craft_stone_pickaxe",  "合成石镐",   TriggerType.CRAFT_STONE_PICKAXE);
        add("craft_iron_pickaxe",   "合成铁镐",   TriggerType.CRAFT_IRON_PICKAXE);
        add("craft_wooden_axe",     "合成木斧",   TriggerType.CRAFT_WOODEN_AXE);
        add("craft_stone_axe",      "合成石斧",   TriggerType.CRAFT_STONE_AXE);
        add("craft_iron_axe",       "合成铁斧",   TriggerType.CRAFT_IRON_AXE);
        add("craft_wooden_sword",   "合成木剑",   TriggerType.CRAFT_WOODEN_SWORD);
        add("craft_stone_sword",    "合成石剑",   TriggerType.CRAFT_STONE_SWORD);
        add("craft_iron_sword",     "合成铁剑",   TriggerType.CRAFT_IRON_SWORD);
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

    /** 根据显示文本查找词条，找不到返回 null */
    public WordEntry findByDisplayText(String displayText) {
        for (WordEntry entry : allWords) {
            if (entry.displayText().equals(displayText)) {
                return entry;
            }
        }
        return null;
    }
}

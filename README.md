# 不要做挑战 / Don't Do It

一个基于 Minecraft Fabric 的多人派对游戏模组，将经典的「不要做挑战」桌游玩法带入 Minecraft 世界。

---

## 🎮 玩法介绍

### 基本规则

游戏开始后，每位玩家会被随机分配到一个 **颜色队伍**（共 8 色），并获得一个随机的 **禁止词条**——即你不能做的动作。

例如：你的词条是「潜行」，那你一旦按下 Shift 潜行，就会触发惩罚！

- ❤️ 每位玩家初始拥有 **15 点生命值**
- ⚠️ 触发自己的词条 → **扣 1 心**，并立即更换新词条
- ⏱️ 词条倒计时归零 → **自动更换新词条**（不扣心）
- 💀 生命值归零 → **淘汰**，进入旁观模式
- 🏆 最后存活的玩家 **获胜**，所在位置绽放烟花！

### HUD 显示

- 经验条上方：**剩余生命值** 与 **当前词条倒计时**
- 屏幕左侧：**对手词条栏**（用 BossBar 显示其他玩家的词条）
- 画面中央：触发 / 淘汰通知（带音效）

---

## ⚙️ 游戏设置

通过游戏书打开设置界面，可配置：

| 设置项 | 可选值 |
|--------|--------|
| 词条更换倒计时 | 60s / 120s / 180s / 300s |
| 特殊事件触发倒计时 | 关闭 / 60s / 180s / 300s / 420s |
| 血量上限 | 3 / 5 / 10 / 15 / 30 心 |

---

## 🎯 词条类型（共 171 种词条，26 类触发行为）

| 类别 | 词条示例 |
|------|----------|
| 基础行为 (15) | 潜行、攻击生物、打怪、破坏方块、挖矿、放置方块、搭方块、发送聊天消息、打字说话、受到伤害、吃东西、疾跑、丢弃物品、打开容器、捡起物品 |
| 挖掘矿石 (7) | 挖掘木头、石头、煤矿、铁矿、铜矿、金矿、钻石矿 |
| 挖掘细分 (7) | 挖掘安山岩、闪长岩、深板岩、花岗岩、凝灰岩、工作台、熔炉 |
| 拾取类 (2) | 拾取原木、获得钻石 |
| 合成类 (10) | 合成工作台、木/石/铁镐、木/石/铁斧、木/石/铁剑 |
| 视角方向 (6) | 低头、抬头、看向东/南/西/北 |
| 持续行为 (5) | 禁止不动五秒、持续看向一个方向五秒、连续奔跑30s、连续潜行5s、跳跃10次 |
| 环境状态 (3) | 自闭（1×2封闭空间）、沉入水中、浮空 |
| 站立方块 (9) | 站在草方块/树叶/石头/深板岩/安山岩/闪长岩/花岗岩/凝灰岩/基岩上 |
| 死亡/复活 (10) | 死亡、复活、三/五/十秒不复活、摔死、岩浆里游泳、窒息、溺死、炸死 |
| 背包物品 (34) | 背包里有煤炭/铁锭/铜锭/工作台/熔炉/斧头/剑/石镐/木镐/铁镐/腐肉/钻石/泥土/磨制安山岩/磨制花岗岩/磨制闪长岩/凝灰岩/石头/平滑石头/树叶/骨头/线/末影珍珠/皮革/羊毛/桶/水桶/岩浆桶；没有铁质工具或防具/没有钻石工具或防具 |
| 饮食/即时 (3) | 吃腐肉、直接扣一颗心、直接回一颗心 |
| 伤害细分 (4) | 受到火焰伤害、弹射物伤害、一次性受到5滴血伤害、造成伤害 |
| 饥饿/高度 (4) | 饱食度低于18、饱食度高于18、玩家高度Y＞70、玩家高度Y＜70 |
| 攻击类 (2) | 攻击玩家、空手打人 |
| 距离类 (2) | 距离所有玩家15米、和玩家贴贴 |
| 经验/等级 (2) | 获得经验、升级 |
| 穿戴装备 (1) | 穿装备 |
| 手持物品 (8) | 手持工作台、熔炉、木/铁/石镐、木/石/铁斧 |
| 快捷栏 (2) | 选中快捷栏第一位、选中快捷栏最后一位 |
| 下落高度 (1) | 下降5格高度 |
| 副手/容器/计数 (17) | 副手持盾、打开箱子/熔炉/工作台、杀死铁傀儡、村民交易、放置/丢弃30个方块、30秒/60秒不跳/不潜行/不疾跑 |
| 放置方块 (10) | 放置泥土、圆石、深板岩圆石、安山岩、花岗岩、闪长岩、凝灰岩、工作台、熔炉、箱子 |
| 丢弃特定物品 (8) | 丢弃泥土、圆石、深板岩圆石、安山岩、花岗岩、闪长岩、凝灰岩、木镐 |
| 头顶方块 (2) | 头顶有方块遮挡、头顶无方块遮挡 |
| 桶操作 (4) | 用桶装水、用桶倒水、用桶装岩浆、用桶倒岩浆 |

---

## ⚡ 特殊事件

特殊事件由独立倒计时控制，倒计时归零时随机触发，影响所有存活玩家。共有 **30 种**特殊事件，含 1 种 1% 概率超级事件。

| 事件 | 类型 | 效果 |
|------|------|------|
| 🔥 怪物狂潮 | 瞬时 | 每位存活玩家周围生成 **3 只**随机敌对生物 |
| ⚡ 交易商人 | 持续 30s | 1% 超级事件 — 每位玩家身边出现盔甲匠村民，可用钻石换取全套下界合金装备 |
| 🎯 …等 30 种事件 | — | 包含钻石馈赠、诅咒、美食雨、经验风暴、TNT雨、玩家互换、全员变幼体、粘液附身、箭雨试炼等 |

---

## 📋 管理员指令

所有指令以 `/dontdoit` 开头：

| 指令 | 说明 |
|------|------|
| `/dontdoit start` | 开始游戏 |
| `/dontdoit stop` | 强制结束游戏 |
| `/dontdoit status` | 查看游戏状态 |
| `/dontdoit vote <玩家> true` | 判定该玩家猜测词条→猜对（加心 + 换词条） |
| `/dontdoit vote <玩家> false` | 判定该玩家猜测词条→猜错（扣心 + 换词条） |
| `/dontdoit skip <玩家>` | 跳过该玩家当前词条（不扣心） |
| `/dontdoit setword <玩家> <词条>` | 为玩家设置指定词条（测试用） |
| `/dontdoit triggerspecialevent <事件名>` | 手动触发指定特殊事件（测试用） |

---

## 🎨 队伍颜色

| 队伍 | 颜色 |
|------|------|
| 红队 | 🔴 |
| 蓝队 | 🔵 |
| 绿队 | 🟢 |
| 黄队 | 🟡 |
| 紫队 | 🟣 |
| 橙队 | 🟠 |
| 青队 | 🩵 |
| 粉队 | 🩷 |

---

---

## 🎮 Gameplay (English)

### Basic Rules

When the game starts, each player is randomly assigned to a **color team** (8 colors total) and given a random **forbidden word** — an action you must NOT perform.

For example: if your word is "Sneak", pressing Shift to sneak will trigger a penalty!

- ❤️ Each player starts with **15 hearts**
- ⚠️ Triggering your word → **lose 1 heart** and receive a new word immediately
- ⏱️ Timer runs out → **auto-swap to a new word** (no penalty)
- 💀 Hearts reach 0 → **eliminated**, enter spectator mode
- 🏆 Last surviving player **wins** — fireworks at their location!

### HUD Display

- Above the XP bar: **remaining hearts** & **word countdown timer**
- Left side of screen: **opponent words bar** (BossBar showing other players' words)
- Center screen: trigger / elimination notifications (with sound effects)

---

## ⚙️ Game Settings (English)

Configurable via the Game Book:

| Setting | Options |
|---------|---------|
| Word Change Timer | 60s / 120s / 180s / 300s |
| Special Event Timer | Off / 60s / 180s / 300s / 420s |
| Max Hearts | 3 / 5 / 10 / 15 / 30 |

---

## 🎯 Trigger Types (171 Word Entries, 26 Trigger Categories)

| Category | Word Examples |
|----------|--------------|
| Basic Actions (15) | Sneak, Attack, Attack Hostile, Break Block, Mine Ore, Place Block, Send Chat, Take Damage, Eat, Sprint, Drop Item, Open Container, Pickup Item |
| Mining Ores (7) | Mine Wood, Stone, Coal, Iron, Copper, Gold, Diamond Ore |
| Mining Detailed (7) | Mine Andesite, Diorite, Deepslate, Granite, Tuff, Crafting Table, Furnace |
| Pickups (2) | Pickup Log, Pickup Diamond |
| Crafting (10) | Craft Crafting Table, Wooden/Stone/Iron Pickaxe/Axe/Sword |
| View Direction (6) | Look Down, Look Up, Look East/South/West/North |
| Sustained Actions (5) | Stand Still 5s, Look Same Dir 5s, Sprint 30s, Sneak 5s, Jump 10 Times |
| Environmental (3) | Enclosed (1×2), Submerged, Floating |
| Standing On (9) | Stand on Grass/Leaves/Stone/Deepslate/Andesite/Diorite/Granite/Tuff/Bedrock |
| Death/Respawn (10) | Death, Respawn, No Respawn 3s/5s/10s, Death by Fall/Lava/Suffocation/Drown/Explosion |
| Inventory Items (34) | Has Coal/Iron/Copper/Crafting Table/Furnace/Axe/Sword/Pickaxes/Rotten Flesh/Diamond/Dirt/Polished Stones/Tuff/Stone/Leaves/Bone/String/Ender Pearl/Leather/Wool/Bucket/Water Bucket/Lava Bucket; No Iron Tools/Armor, No Diamond Tools/Armor |
| Food/Instant (3) | Eat Rotten Flesh, Instant Lose Heart, Instant Gain Heart |
| Damage Types (4) | Take Fire Damage, Projectile Damage, 5-Damage at Once, Deal Damage |
| Hunger/Height (4) | Hunger Below 18, Hunger Above 18, Y > 70, Y < 70 |
| Attack Types (2) | Attack Player, Empty Hand Attack |
| Distance (2) | 15m Away from All, Too Close to Player |
| XP/Level (2) | Gain XP, Level Up |
| Armor (1) | Wear Armor |
| Holding Items (8) | Hold Crafting Table, Furnace, Wooden/Iron/Stone Pickaxe, Wooden/Stone/Iron Axe |
| Hotbar Slot (2) | Select First Slot, Select Last Slot |
| Fall Height (1) | Fall 5 Blocks |
| Offhand/Containers (17) | Hold Shield Offhand, Open Chest/Furnace/Crafting Table, Kill Iron Golem, Villager Trade, Place/Drop 30 Blocks, No Jump/Sneak/Sprint 30s/60s |
| Place Blocks (10) | Place Dirt, Cobblestone, Cobbled Deepslate, Andesite, Granite, Diorite, Tuff, Crafting Table, Furnace, Chest |
| Drop Specific (8) | Drop Dirt, Cobblestone, Cobbled Deepslate, Andesite, Granite, Diorite, Tuff, Wooden Pickaxe |
| Block Above (2) | Block Above Head, No Block Above Head |
| Bucket Ops (4) | Fill Bucket Water, Empty Bucket Water, Fill Bucket Lava, Empty Bucket Lava |

---

## ⚡ Special Events (English)

Special events are controlled by an independent countdown timer. When the timer reaches zero, a random event triggers, affecting all surviving players. There are **30** special events, including 1 super event with 1% probability.

| Event | Type | Effect |
|-------|------|--------|
| 🔥 Monster Rampage | Instant | Spawns **3** random hostile mobs around each surviving player |
| ⚡ Trade Merchant | Lasts 30s | 1% super event — Armorer villager appears near each player, trading full netherite gear for diamonds |
| 🎯 …30 events total | — | Including Diamond Gift, Curse, Food Rain, XP Storm, TNT Rain, Player Swap, Everyone Baby, Slime Possession, Arrow Trial, etc. |

---

## 📋 Admin Commands (English)

All commands start with `/dontdoit`:

| Command | Description |
|---------|-------------|
| `/dontdoit start` | Start the game |
| `/dontdoit stop` | Force-end the game |
| `/dontdoit status` | View game status |
| `/dontdoit vote <player> true` | Judge player guess word → correct guess (+heart + new word) |
| `/dontdoit vote <player> false` | Judge player guess word → wrong guess (-heart + new word) |
| `/dontdoit skip <player>` | Skip the player's current word (no penalty) |
| `/dontdoit setword <player> <word>` | Set a specific forbidden word for a player (testing) |
| `/dontdoit triggerspecialevent <name>` | Manually trigger a special event (testing) |

---

## 🎨 Team Colors (English)

| Team | Color |
|------|-------|
| Red | 🔴 |
| Blue | 🔵 |
| Green | 🟢 |
| Yellow | 🟡 |
| Purple | 🟣 |
| Orange | 🟠 |
| Cyan | 🩵 |
| Pink | 🩷 |

---

## 🛠️ Technical Info

- **Minecraft Version**: 1.21.x
- **Mod Loader**: Fabric
- **Language**: Java

---

## 🙏 Credits

This mod was conceived and designed by **Baiyueyue**, with all code built and implemented by **AI**.

> *A Minecraft party game mod born from human creativity × AI programming.*

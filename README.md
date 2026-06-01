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
| 词条更换倒计时 | 60s / 120s / 180s |
| 特殊事件触发倒计时 | 60s / 180s / 300s / 420s |

---

## 🎯 词条类型（共 35 种词条，12 类触发行为）

| 词条示例 | 触发行为 |
|----------|----------|
| 潜行 | 按下潜行键（Shift） |
| 攻击生物 / 打怪 | 攻击任何生物 / 攻击敌对生物 |
| 破坏方块 / 挖矿 | 破坏任意方块 / 挖掘矿石类方块 |
| 放置方块 / 搭方块 | 放置任意方块 |
| 发送聊天消息 / 打字说话 | 在聊天栏发送消息 |
| 受到伤害 | 受到任何来源的伤害 |
| 吃东西 | 进食 |
| 疾跑 | 按下疾跑键 |
| 丢弃物品 | 丢弃物品栏中的物品 |
| 打开容器 | 打开箱子、熔炉等容器 |
| 捡起物品 | 捡起地面上的掉落物 |
| 拾取原木 | 捡起原木 |
| 挖掘木头 / 石头 / 煤矿 / 铁矿 / 铜矿 / 金矿 / 钻石矿 | 破坏对应方块 |
| 合成工作台 / 木镐 / 石镐 / 铁镐 / 木斧 / 石斧 / 铁斧 / 木剑 / 石剑 / 铁剑 | 在工作台/背包合成对应物品 |

---

## ⚡ 特殊事件

特殊事件由独立倒计时控制，倒计时归零时随机触发，影响所有存活玩家。

| 事件 | 类型 | 效果 |
|------|------|------|
| 🔥 怪物狂潮 | 瞬时 | 每位存活玩家周围生成 **3 只**随机敌对生物（僵尸、骷髅、蜘蛛、苦力怕、女巫、末影人、尸壳、流浪者） |

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
| Word Change Timer | 60s / 120s / 180s |
| Special Event Timer | 60s / 180s / 300s / 420s |

---

## 🎯 Trigger Types (35 Word Entries, 12 Trigger Categories)

| Word Examples | Trigger Behavior |
|---------------|-----------------|
| Sneak | Press the sneak key (Shift) |
| Attack Mobs / Attack Hostile | Attack any creature / Attack hostile mobs |
| Break Block / Mine Ore | Destroy any block / Mine ore-type blocks |
| Place Block / Build Blocks | Place any block |
| Send Chat / Type in Chat | Send a message in chat |
| Take Damage | Take damage from any source |
| Eat Food | Eat something |
| Sprint | Press the sprint key |
| Drop Item | Drop an item from inventory |
| Open Container | Open chests, furnaces, etc. |
| Pickup Item | Pick up items from the ground |
| Pickup Log | Pick up a log |
| Mine Wood / Stone / Coal / Iron / Copper / Gold / Diamond Ore | Break the corresponding block |
| Craft Crafting Table / Wooden/Stone/Iron Pickaxe / Axe / Sword | Craft the corresponding item |

---

## ⚡ Special Events (English)

Special events are controlled by an independent countdown timer. When the timer reaches zero, a random event triggers, affecting all surviving players.

| Event | Type | Effect |
|-------|------|--------|
| 🔥 Monster Rampage | Instant | Spawns **3** random hostile mobs around each surviving player (Zombie, Skeleton, Spider, Creeper, Witch, Enderman, Husk, Stray) |

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

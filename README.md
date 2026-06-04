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

| 设置项 | 可选值 | 默认值 |
|--------|--------|--------|
| 词条更换倒计时 | 60s / 120s / 180s / 300s | 180s |
| 特殊事件触发倒计时 | 关闭 / 60s / 180s / 300s / 420s | 300s |
| 血量上限 | 3 / 5 / 10 / 15 / 30 心 | 15 心 |
| 游戏范围 | 关闭 / 1×1 / 2×2 / 3×3 区块 | 3×3 区块 |

### 🌍 游戏范围

开启后，每局游戏会在世界随机位置圈定一块区块区域作为游戏场地：
- **随机性**：基于世界种子与当前时间生成随机坐标（±500 区块范围），每局地图完全不同
- **边界限制**：玩家不得越出边界，否则每秒自动传送回区域中心
- **区块大小**：1×1=16×16格 / 2×2=32×32格 / 3×3=48×48格
- **关闭时**：无边界限制，玩家可在全世界自由活动（传统模式）

---

## 🎯 词条类型（共 171 种词条，26 类触发行为）

| 类别 | 数量 | 词条 |
|------|------|------|
| 基础行为 | 15 | 潜行、攻击生物、打怪、破坏方块、挖矿、放置方块、搭方块、发送聊天消息、打字说话、受到伤害、吃东西、疾跑、丢弃物品、打开容器、捡起物品 |
| 挖掘矿石 | 7 | 挖掘木头、挖掘石头、挖掘煤矿、挖掘铁矿、挖掘铜矿、挖掘金矿、挖掘钻石矿 |
| 挖掘细分 | 7 | 挖掘安山岩、挖掘闪长岩、挖掘深板岩、挖掘花岗岩、挖掘凝灰岩、挖掘工作台、挖掘熔炉 |
| 拾取类 | 2 | 拾取原木、获得钻石 |
| 合成类 | 10 | 合成工作台、合成木镐、合成石镐、合成铁镐、合成木斧、合成石斧、合成铁斧、合成木剑、合成石剑、合成铁剑 |
| 视角方向 | 6 | 低头、抬头、看向东方、看向南方、看向西方、看向北方 |
| 持续行为 | 5 | 禁止不动五秒、持续看向一个方向五秒、连续奔跑30s、连续潜行5s、跳跃10次 |
| 环境状态 | 3 | 自闭（1×2封闭空间）、沉入水中、浮空 |
| 站立方块 | 9 | 站在草方块上、站在树叶上、站在石头上、站在深板岩上、站在安山岩上、站在闪长岩上、站在花岗岩上、站在凝灰岩上、站在基岩上 |
| 死亡/复活 | 10 | 死亡、复活、三秒不复活、五秒不复活、十秒不复活、摔死、岩浆里游泳、窒息、溺死、炸死 |
| 背包物品 | 30 | 背包里有煤炭、背包里有铁锭、背包里有铜锭、背包里有工作台、背包里有熔炉、背包里有斧头、背包里有剑、背包里有石镐、背包里有木镐、背包里有铁镐、背包里有腐肉、背包里有钻石、背包里有泥土、背包里有磨制安山岩、背包里有磨制花岗岩、背包里有磨制闪长岩、背包里有凝灰岩、背包里有石头、背包里有平滑石头、背包里有树叶、背包里有骨头、背包里有线、背包里有末影珍珠、背包里有皮革、背包里有羊毛、背包里有桶、背包里有水桶、背包里有岩浆桶、背包里没有铁质工具或防具、背包里没有钻石工具或防具 |
| 饮食/即时 | 3 | 吃腐肉、直接扣一颗心、直接回一颗心 |
| 伤害细分 | 4 | 受到火焰伤害、弹射物伤害、一次性受到5滴血伤害、造成伤害 |
| 饥饿/高度 | 4 | 饱食度低于18、饱食度高于18、玩家高度Y＞70、玩家高度Y＜70 |
| 攻击类 | 2 | 攻击玩家、空手打人 |
| 距离类 | 2 | 距离所有玩家15米、和玩家贴贴 |
| 经验/等级 | 2 | 获得经验、升级 |
| 穿戴装备 | 1 | 穿装备 |
| 手持物品 | 8 | 手持工作台、手持熔炉、手持木镐、手持铁镐、手持石镐、手持木斧、手持石斧、手持铁斧 |
| 快捷栏 | 2 | 选中快捷栏第一位、选中快捷栏最后一位 |
| 下落高度 | 1 | 下降5格高度 |
| 副手/容器/计数 | 14 | 副手持盾、打开箱子、与熔炉交互、与工作台交互、杀死铁傀儡、村民交易、放置30个方块、丢弃30个方块、30秒不跳、30秒不潜行、30秒不疾跑、60秒不跳、60秒不潜行、60秒不疾跑 |
| 放置方块 | 10 | 放置泥土、放置圆石、放置深板岩圆石、放置安山岩、放置花岗岩、放置闪长岩、放置凝灰岩、放置工作台、放置熔炉、放置箱子 |
| 丢弃特定物品 | 8 | 丢弃泥土、丢弃圆石、丢弃深板岩圆石、丢弃安山岩、丢弃花岗岩、丢弃闪长岩、丢弃凝灰岩、丢弃木镐 |
| 头顶方块 | 2 | 头顶有方块遮挡、头顶无方块遮挡 |
| 桶操作 | 4 | 用桶装水、用桶倒水、用桶装岩浆、用桶倒岩浆 |

---

## ⚡ 特殊事件

特殊事件由独立倒计时控制，倒计时归零时随机触发，影响所有存活玩家。共有 **30 种**特殊事件（含 1 种 1% 概率超级事件）。

| 事件 | 类型 | 效果 |
|------|------|------|
| 🔥 怪物狂潮 | 瞬时 | 每位存活玩家周围生成 3 只随机敌对生物 |
| 💎 钻石馈赠 | 瞬时 | 每位存活玩家获得 15 颗钻石 |
| 💎 钻石祝福 | 持续 120s | 挖钻石矿回复 1 心 |
| 💀 钻石诅咒 | 瞬时 | 按背包钻石数量扣除等量血量 |
| 🌑 日食诅咒 | 瞬时 | 全体获得 1 分钟失明效果 |
| ☁️ 平静 | 瞬时 | 无事发生，倒计时重新开始 |
| ☁️ 唉，云朵？ | 瞬时 | 全体获得 30 秒漂浮效果 |
| 🍖 美食雨 | 持续 10s | 美食从天而降 |
| 🌟 经验风暴 | 持续 10s | 大量经验球从天而降 |
| 💚 生命赐福 | 持续 10s | 全体获得生命恢复 II 效果 |
| ⛏️ 脚下出矿 | 持续 10s | 玩家周围 5×5×5 变为矿物，结束恢复 |
| 🔨 铁砧暴雨 | 持续 10s | 铁砧从天而降 |
| 💣 TNT降雨 | 瞬时 | 每位玩家周围生成 20 个点燃的 TNT |
| ⛏️ 地底塌陷 | 持续 30s | 脚下方块持续破碎 |
| 🎃 全员南瓜头 | 持续 60s | 强制戴上有绑定诅咒的南瓜头 |
| 🔀 物品栏洗牌 | 瞬时 | 所有玩家快捷栏物品随机打乱 |
| 🐔 小鸡天降 | 瞬时 | 每位玩家周围生成 50 只小鸡 |
| 🔄 玩家互换位置 | 瞬时 | 存活玩家位置随机交换 |
| 🔥 脚步生火 | 持续 30s | 踩到的方块起火 |
| 🔒 囚笼试炼 | 持续 10s | 铁栅栏囚禁 + 5 秒后笼内出苦力怕 |
| 💧 高空落水挑战 | 持续 30s | 传送 100 米高空 + 水桶，落水成功加心 |
| 🌱 作物速成 | 持续 15s | 脚边作物瞬间成熟 + 小花生成 |
| 💚 豁免祝福 | 持续 120s | 装备耐久不消耗 |
| ⚙️ 装备锈蚀 | 持续 120s | 装备耐久五倍消耗 |
| 🍗 饥饿疫病 | 持续 30s | 饥饿值飞速下降，食物回复减半 |
| 📦 物资迁徙 | 瞬时 | 玩家背包物品转移至其他玩家 |
| 👶 全员变幼体 | 持续 60s | 玩家缩小 100 倍 (Baby scale) |
| 🟢 粘液附身 | 持续 30s | 玩家周围方块变为粘液块 |
| 🏹 箭雨试炼 | 持续 10s | 箭雨从四面射向玩家，存活加心 |
| ⚡ 交易商人 | 持续 30s | 1% 超级事件 — 生成村民提供特殊交易，30 秒后消失 |

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

| Setting | Options | Default |
|---------|---------|---------|
| Word Change Timer | 60s / 120s / 180s / 300s | 180s |
| Special Event Timer | Off / 60s / 180s / 300s / 420s | 300s |
| Max Hearts | 3 / 5 / 10 / 15 / 30 | 15 |
| Game Range | Off / 1×1 / 2×2 / 3×3 Chunks | 3×3 Chunks |

### 🌍 Game Range (English)

When enabled, each game randomly selects a chunk area in the world as the playing field:
- **Randomness**: Random coordinates generated from world seed + current time (within ±500 chunks), ensuring a unique map every game
- **Boundary**: Players cannot leave the area — teleported back to center if they do
- **Chunk Sizes**: 1×1=16×16 blocks / 2×2=32×32 / 3×3=48×48
- **Off**: No boundary — traditional free-roam gameplay

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
| Inventory Items (30) | Has Coal/Iron/Copper/Crafting Table/Furnace/Axe/Sword/Pickaxes/Rotten Flesh/Diamond/Dirt/Polished Stones/Tuff/Stone/Leaves/Bone/String/Ender Pearl/Leather/Wool/Bucket/Water Bucket/Lava Bucket; No Iron Tools/Armor, No Diamond Tools/Armor |
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
| Offhand/Containers (14) | Hold Shield Offhand, Open Chest/Furnace/Crafting Table, Kill Iron Golem, Villager Trade, Place/Drop 30 Blocks, No Jump/Sneak/Sprint 30s/60s |
| Place Blocks (10) | Place Dirt, Cobblestone, Cobbled Deepslate, Andesite, Granite, Diorite, Tuff, Crafting Table, Furnace, Chest |
| Drop Specific (8) | Drop Dirt, Cobblestone, Cobbled Deepslate, Andesite, Granite, Diorite, Tuff, Wooden Pickaxe |
| Block Above (2) | Block Above Head, No Block Above Head |
| Bucket Ops (4) | Fill Bucket Water, Empty Bucket Water, Fill Bucket Lava, Empty Bucket Lava |

---

## ⚡ Special Events (English)

Special events are controlled by an independent countdown timer. When the timer reaches zero, a random event triggers, affecting all surviving players. There are **30** special events (including 1 super event with 1% probability).

| Event | Type | Effect |
|-------|------|--------|
| 🔥 Monster Rampage | Instant | Spawns 3 random hostile mobs around each surviving player |
| 💎 Diamond Gift | Instant | Each surviving player receives 15 diamonds |
| 💎 Diamond Blessing | 120s | Mining diamond ore restores 1 heart |
| 💀 Diamond Curse | Instant | Lose hearts equal to the number of diamonds in inventory |
| 🌑 Eclipse Curse | Instant | All players receive 1 minute of Blindness |
| ☁️ Calm | Instant | Nothing happens, countdown resets |
| ☁️ Oh, Clouds? | Instant | All players receive 30 seconds of Levitation |
| 🍖 Food Rain | 10s | Food drops from the sky |
| 🌟 XP Storm | 10s | Massive XP orbs rain from the sky |
| 💚 Life Blessing | 10s | All players receive Regeneration II |
| ⛏️ Ore Underfoot | 10s | 5×5×5 area around players turns into ores, restored at end |
| 🔨 Anvil Storm | 10s | Anvils rain from the sky |
| 💣 TNT Rain | Instant | 20 lit TNT spawn around each surviving player |
| ⛏️ Cave In | 30s | Blocks under players continuously break |
| 🎃 Pumpkin Head | 60s | All players forced to wear a Curse of Binding pumpkin head |
| 🔀 Inventory Shuffle | Instant | Hotbar items randomly shuffled for all players |
| 🐔 Chicken Rain | Instant | 50 chickens spawn around each surviving player |
| 🔄 Player Swap | Instant | Surviving players' positions randomly exchanged |
| 🔥 Fire Trail | 30s | Blocks players walk on catch fire |
| 🔒 Cage Trial | 10s | Players trapped in iron bar cages + creeper spawns after 5 seconds |
| 💧 Sky Water Challenge | 30s | Teleported 100m up with a water bucket — successful water landing grants a heart |
| 🌱 Crop Speed Grow | 15s | Nearby crops instantly mature + small flowers spawn |
| 💚 Durability Blessing | 120s | Equipment durability does not decrease |
| ⚙️ Equipment Rust | 120s | Equipment durability decreases at 5× speed |
| 🍗 Hunger Disease | 30s | Hunger drops rapidly, food restores half |
| 📦 Inventory Migration | Instant | Players' inventory items transferred to other players |
| 👶 Everyone Baby | 60s | Players shrink 100× (Baby scale) |
| 🟢 Slime Possession | 30s | Blocks around players turn into slime blocks |
| 🏹 Arrow Trial | 10s | Arrows fire from all directions — surviving grants a heart |
| ⚡ Trade Merchant | 30s | 1% super event — villager with special trades spawns per player, disappears after 30s |

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

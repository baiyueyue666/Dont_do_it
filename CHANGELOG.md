# 开发日志

## 2026-06-03

### 1. 全员变幼体缩放调整
- 宝宝缩放从原来的 1/100 调整为 **1/5**（初版 1/100 → 第一轮 1/16 → 最终 1/5）
- 移动速度和跳跃高度同步缩放至 1/5

### 2. 新增「交易商人」超级事件
- **SpecialEventType.java** — 新增 `TRADE_MERCHANT("交易商人", 30, BossBar.Color.YELLOW, 1)`，权重=1
- **SpecialEventPool.java** — 新增加权随机抽取机制（默认权重 100，超级事件权重 1），实现约 1% 触发概率
- 每位存活玩家身边生成一名盔甲匠村民，提供下界合金装备交易：
  - 1 木棍 + 2 钻石 → 下界合金剑
  - 2 木棍 + 3 钻石 → 下界合金斧 / 下界合金镐
  - 5/8/7/4 钻石 → 下界合金头盔/胸甲/护腿/靴
- 触发时全体播放 `ENTITY_PLAYER_LEVELUP` 音效 + 特殊标题播报
- 持续 30 秒，结束后村民消失

### 3. 交易商人 Bug 修复
- **问题 1**：交易界面打开后立即被关闭
  - **根因**：`VillagerEntity` 创建后未正确设置 `VillagerData`，需通过 `RegistryWrapper.getOptional()` 将 `RegistryKey` 解析为 `RegistryEntry`
  - **修复**：设置村民职业为盔甲匠（`VillagerProfession.ARMORER`，等级 5）
- **问题 2**：触发音效不播放
  - **根因**：`SoundEvents.UI_TOAST_CHALLENGE_COMPLETE` 属于客户端 UI 音效，服务端 `playSound()` 无法触发
  - **修复**：改为 `SoundEvents.ENTITY_PLAYER_LEVELUP`（升级音效），并向全体玩家广播（与其他重要事件保持一致）

### 4. MC 1.21.1 API 兼容修复
- **BossBar.Color.GOLD** 在 1.21.1 中不存在 → 改为 `BossBar.Color.YELLOW`
- **TradeOffer** 构造函数需使用 `TradedItem` 包装物品，第二购买项需用 `Optional.of(new TradedItem(...))`
- **VillagerData** 构造函数参数必须为 `RegistryEntry<VillagerType>` + `RegistryEntry<VillagerProfession>`，不可直接传入 `RegistryKey`
- **VillagerEntity** 创建方式改为 `EntityType.VILLAGER.create(world, SpawnReason.EVENT)`

### 当前特殊事件总数：30 种 (其中交易商人为 1% 超级事件)

---

## 2026-06-02

### 1. 新增视角方向类词条（6 种）
- **TriggerType.java** — 新增 6 个视角方向枚举：
  - `LOOK_DOWN`（低头）、`LOOK_UP`（抬头）
  - `LOOK_EAST` / `LOOK_SOUTH` / `LOOK_WEST` / `LOOK_NORTH`（看向东/南/西/北）
- **WordPool.java** — 新增 6 个对应词条
- **WordTriggerDetector.java** — tick 轮询中基于 pitch/yaw 边缘检测触发（仅刚进入该状态时触发一次）

### 2. 新增持续行为类词条（2 种）
- `STAND_STILL_5S`（禁止不动五秒）— 连续 100 tick 坐标不变即触发，带防刷标记
- `LOOK_SAME_DIR_5S`（持续看向一个方向五秒）— 连续 100 tick yaw 不变即触发，带防刷标记

### 3. 新增环境状态类词条（2 种）
- `ENCLOSED_1X2`（自闭）— 检查玩家是否被实心方块包围在 1×2 空间内（六面检测：脚底+头顶+脚部四水平+头部四水平方向均为实心方块）
- `SUBMERGED`（沉入水中）— 检测玩家眼部位置是否为水方块

### 4. 新增挖掘细分词条（3 种）
- `MINE_ANDESITE`（挖掘安山岩）、`MINE_DIORITE`（挖掘闪长岩）、`MINE_DEEPSLATE`（挖掘深板岩）
- 与已有的矿石细分共用 `onBlockBreak` 事件，在区块破坏回调中按方块 ID 区分

### 5. 新增站立方块类词条 + 浮空（7 种）
- `STAND_ON_GRASS` / `STAND_ON_LEAVES` / `STAND_ON_STONE` / `STAND_ON_DEEPSLATE` / `STAND_ON_ANDESITE` / `STAND_ON_DIORITE` — tick 轮询中检测脚底方块 ID
- `FLOATING`（浮空）— 脚底无任何方块时触发

### 6. 新增死亡/复活类词条（5 种）
- `DEATH`（死亡）— 注册 `ServerLivingEntityEvents.AFTER_DEATH`，玩家死亡时触发并记录死亡时刻
- `RESPAWN`（复活）— 注册 `ServerPlayerEvents.AFTER_RESPAWN`，玩家复活时触发并清除死亡计时
- `NOT_RESPAWN_3S` / `NOT_RESPAWN_5S` / `NOT_RESPAWN_10S`（三/五/十秒不复活）— tick 中累计死亡时长，达到对应阈值时各触发一次

### 7. 新增背包物品类词条（13 种）
- **TriggerType.java** — 新增 `PICKUP_DIAMOND`、`HAS_COAL`、`HAS_IRON_INGOT`、`HAS_COPPER_INGOT`、`HAS_CRAFTING_TABLE`、`HAS_FURNACE`、`HAS_AXE`、`HAS_SWORD`、`HAS_STONE_PICKAXE`、`HAS_WOODEN_PICKAXE`、`HAS_IRON_PICKAXE`、`HAS_ROTTEN_FLESH`、`HAS_DIAMOND`、`HAS_DIRT`（共 13 种）
- **WordPool.java** — 新增 13 个对应词条
- **WordTriggerDetector.java** — 添加 `playerHasItem()` / `playerHasItemEndingWith()` 辅助方法，斧头和剑使用后缀匹配（`_axe` / `_sword`）
- **PlayerInventoryMixin.java** — 新增钻石拾取细分检测

### 8. 「自闭」判定修复
- **问题**：头顶无方块时仍然误触发
- **第一次修复**：增加 `aboveHeadPos = headPos.up()` 头顶方块检查
- **第二次修复**：用户反馈脚下无方块也会误触发，补充 `belowFeetPos = feetPos.down()` 脚底方块检查
- 最终实现完整的 1×2 六面封闭检测

### 9. 死亡/复活事件注册补回
- **问题**：死亡、复活、N 秒不复活共 5 个词条完全无法触发
- **根因**：前序修改中 `register()` 漏掉了 `AFTER_DEATH` 和 `AFTER_RESPAWN` 两个事件注册
- **修复**：补回两个事件监听器，死亡时记录 `deathTick` + 重置不复活标记，复活时清除死亡计时并触发 `RESPAWN`

### 10. 背包物品检测从「边缘触发」改为「存在性触发」
- **问题**：边缘触发模式下"背包里有XX"只在物品数量 0→1 时触发，已持有物品时永不触发；跨局时 `wasHas*` Map 残留上局状态导致本局不触发
- **修复**：
  - `checkInventoryItem()` 改为直接存在性触发：只要有对应物品就调用 `onPlayerTriggered`（反刷依赖 GameManager 自带的 1.5s 冷却）
  - 斧头和剑检测同样改为直接存在性触发，移除边缘判断
  - 新增 `clearAllState()` 静态方法，在 `GameManager.startGame()` 中调用，清理全部 40+ 个状态 Map

### 11. 类型修正
- `server.getTicks()` 返回 `int`，`deathTick` Map 为 `Long` 类型，添加 `(long)` 强转

### 当前词条总数：71 种（TriggerType 71 个枚举值）

---

## 2026-06-01

### 1. 新增合成类触发词条（10 种）
- **TriggerType.java** — 新增 10 个合成类枚举值：
  - `CRAFT_CRAFTING_TABLE`（合成工作台）
  - `CRAFT_WOODEN_PICKAXE` / `CRAFT_STONE_PICKAXE` / `CRAFT_IRON_PICKAXE`（镐）
  - `CRAFT_WOODEN_AXE` / `CRAFT_STONE_AXE` / `CRAFT_IRON_AXE`（斧）
  - `CRAFT_WOODEN_SWORD` / `CRAFT_STONE_SWORD` / `CRAFT_IRON_SWORD`（剑）
- **WordPool.java** — 新增 10 个对应词条

### 2. 新增 CraftingResultSlotMixin
- **新增 CraftingResultSlotMixin.java** — 监听 `CraftingResultSlot.onTakeItem`，当玩家从工作台或背包 2×2 合成格取出成品时触发对应词条判定
- 覆盖工作台和生存背包合成两种场景
- ✅ 已验证："合成木剑" 词条可正常触发

### 3. 修复拾取原木检测（1.21.11 兼容性）
- **问题**：1.21.11 中 `ItemEntity` 不再覆写 `Entity.onPlayerCollision`，前序版本的 Mixin 注入完全无效
- **ItemEntityMixin.java** — 将 `@Mixin(ItemEntity.class)` 改为 `@Mixin(Entity.class)` + `instanceof ItemEntity` 检查
- **新增 PlayerInventoryMixin.java** — 注入 `PlayerInventory.insertStack` / `offerOrDrop` / `addStack` 作为物品入库的备选检测路径
- **dont_do_it.mixins.json** — 注册两个新 Mixin

### 4. 词条文案调整
- "拾取木头" → **"拾取原木"**（TriggerType 显示名 + WordPool 词条文本）
- `isWoodItem` 匹配范围收窄：原 `_log/_wood/_stem/_hyphae/bamboo_block` → 仅 `_log`

---

## 2026-05-30

### 1. 删除"蹦起来"事件
- **TriggerType.java** — 删除 `JUMP("跳跃")` 枚举值
- **WordPool.java** — 删除 `jump_01`（跳跃）和 `jump_02`（蹦起来）两个词条
- **WordTriggerDetector.java** — 删除 `wasOnGround` 着地检测逻辑及相关清理代码

### 2. 事件池拆分：普通事件池 + 特殊事件池
- **新增 SpecialEventType.java** — 特殊事件类型枚举，当前包含 `MONSTER_RAMPAGE`（怪物狂潮）
- **新增 SpecialEventPool.java** — 特殊事件池，管理特殊事件的随机抽取与执行
- **WordPool.java** — 保持为普通事件池（词条触发）

### 3. 怪物狂潮事件
- 在每位存活玩家周围 3-10 格随机位置生成 3 只随机敌对生物（僵尸、骷髅、蜘蛛、苦力怕等）

### 4. BossBar 特殊事件显示
- **BossBarManager.java** — 新增第 3 条 BossBar，复用显示特殊事件：
  - 倒计时中：`⚡ 特殊事件将在 Xs 后触发`（黄色进度条）
  - 活动期间：`🔥 怪物狂潮 - Xs 剩余`（红色进度条）
- **GamePackets.java** — 新增 `SpecialEventBossBarPayload`、`UpdateSettingsFullPayload`
- **ClientPacketHandler.java** — 处理特殊事件 BossBar 同步包

### 5. 游戏设置拆分为两个独立按钮
- **GameBookScreen.java** — 原来的"⚙ 游戏设置"拆分为：
  - ⏱ 词条更换倒计时（60s / 120s / 180s）
  - ⚡ 特殊事件触发倒计时（60s / 180s / 300s / 420s）
- **SettingsScreen.java** — 添加 `Mode` 枚举（`WORD_TIMER` / `SPECIAL_EVENT_TIMER`），根据模式单独显示对应设置项
- **GameBookItem.java** — `SettingsOpener` 接口新增 `mode` 参数
- **Dont_do_itClient.java** — 适配新的 SettingsOpener 签名

### 6. 游戏设置扩展
- **GameSettings.java** — 新增 `specialEventTimerSeconds` 字段及选项数组 `[60, 180, 300, 420]`

### 7. 核心逻辑集成
- **GameManager.java** — 新增 `tickSpecialEvent()`、`triggerSpecialEvent()` 等方法；游戏开始初始化特殊事件倒计时，事件结束后重新计时
- **GameCountdownManager.java** — 每 tick 调用特殊事件计时
- **Dont_do_it.java** — 服务端处理 `UpdateSettingsFullPayload`

### 流程
```
游戏开始 → 特殊事件倒计时 Xs → 触发随机特殊事件 → 瞬时/持续 → 结束后重新倒计时 → 循环
```

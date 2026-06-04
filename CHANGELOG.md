# 开发日志

## 2026-06-04

### 1. 默认值调整
- 词条更换默认倒计时：60s → **180s**
- 特殊事件触发默认倒计时：180s → **300s**
- `GameSettings.DEFAULT_TIMER_SECONDS` 和 `DEFAULT_SPECIAL_EVENT_TIMER_SECONDS` 更新
- `GameBookScreen` 默认值同步更新

### 2. 新增游戏范围设置
- **GameSettings.java** — 新增 `GAME_RANGE_OPTIONS = {0, 1, 2, 3}`，`DEFAULT_GAME_RANGE = 2`
  - 0 = 关闭（无边界，玩家自由活动）
  - 1 = 1×1 区块（16×16 格）
  - 2 = 2×2 区块（32×32 格）
  - 3 = 3×3 区块（48×48 格）
- **GameManager.java** — 新增边界系统：
  - 游戏开始时，基于「世界种子 XOR 当前毫秒时间戳」生成随机种子
  - 从 ±500 区块（±8000 格）范围内随机选取起点区块
  - 计算边界坐标（minX, maxX, minZ, maxZ），传送所有玩家到区域中心（地表高度）
  - 游戏关闭时清除边界；结束广播中显示当前范围设置
- **GameCountdownManager.java** — 每秒调用 `enforceBoundary()`：存活玩家越界 → 传送回区域中心 + 提示
- **GameBookScreen.java** — 新增「🏠 游戏范围」按钮（第五个设置按钮）
- **SettingsScreen.java** — 新增 `GAME_RANGE` 模式，渲染 4 个范围选项
- **GamePackets.java** — `UpdateSettingsFullPayload` 新增 `gameRange` 字段 + codec
- **GameBookItem.java / Dont_do_itClient.java** — `SettingsOpener` 接口新增 `gameRange` 参数
- **Dont_do_it.java** — 网络接收器存储 `gameRange` 并播报

### 设计思路
采用「当前世界种子中随机选取区块」方案（而非开辟新维度/随机种子生成地形），原因：
- 不同区块坐标自然产生完全不同的地形（山/海/森林/洞穴），保证每局随机性
- 实现简洁，零额外性能开销
- 玩家无需更换地图即可获得新鲜体验

### 当前词条总数：171 种 | 指令：7 条 | 特殊事件：30 种 | 游戏范围：4 档

### 3. 游戏范围增强：海洋避让 + 准备阶段 + 高空传送 + 方块恢复

#### 海洋生物群系避让
- **GameManager.startGame()** — 抽取区块后检测区域内是否包含 `ocean`/`deep_` 生物群系
- 若检测到海洋区块，自动重新随机选取起点区块（最多 100 次），避免玩家在无法生存的水域开局

#### 准备阶段（10 秒无敌）
- 游戏开始后进入*准备阶段*：
  - 所有玩家设置为 `invulnerable = true`（10 秒无敌保护）
  - 玩家传送到游戏区域中心高空 **Y=150**
  - 客户端 HUD 中央大字显示 `游戏即将开始` + 倒计时数字 + `高空坠落中… 无敌保护中…`
- 准备阶段内不触发词条、不受边界越界惩罚（传送回高空中心）
- 10 秒后调用 `startPhase2()` 正式开局：无敌取消、词条分配、倒计时启动

#### 方块保存与恢复
- **saveAreaBlocks()** — 游戏开始时遍历边界区域内所有非空气方块，存入 `savedBlocks` HashMap
- **restoreAreaBlocks()** — 游戏结束时清空区域内后建方块，恢复原始方块状态
- 方块实体 NBT 恢复因 1.21.4 API 变更暂时跳过（`createCommandRegistryWrapper()` 签名不兼容）
- 边界 Y 范围：`world.getBottomY()` ~ `地表高度 + 64`（保守上界，避免漏掉高塔建筑）

#### 游戏结束恢复
- 所有玩家传送回世界出生点（`getSpawnPoint().getPos()`）
- 边界数据清除、背包清空并返还游戏书本

#### 边界 HUD 接近警告
- **BoundaryRenderer.java** — 因 1.21.11 渲染管线全面重构，3D 边界线渲染改为 HUD 文字提示
- 当玩家距离边界 ≤ 3 格时，屏幕底部显示红色警告 `⚠ 接近边界！越界将传送回中心 [方向]`
- 服务器端 `enforceBoundary()` 仍照常生效（越界传送 + 提示）

### 4. MC 1.21.11 API 兼容修复

#### 服务端编译修复
- `ServerWorld.getSpawnPos()` → `getSpawnPoint().getPos()`（1.21.4 中 SpawnPoint 改为 record，通过 `getPos()` 获取 BlockPos）
- `world.getTopY()` 无参版本已移除，统一使用 3 参数版本 `getTopY(Heightmap.Type, int, int)`
- 方块实体 NBT 保存/恢复跳过：`createNbt(registryWrapper)` 需 `getRegistryManager()`，`read()` 需 `RegistryWrapper.WrapperLookup`（`DynamicRegistryManager` 不再实现此接口）

#### 客户端编译修复
- `DrawContext.getMatrices()` 返回 `Matrix3x2fStack`（非 `MatrixStack`），无 `push()`/`pop()`/`scale()` 方法 → 改用 `drawCenteredTextWithShadow()` 替代缩放渲染
- `WorldRenderEvents` 在 Fabric API 0.141.4+1.21.11 中已移除 → BoundaryRenderer 注册方法改为空操作
- 渲染管线重构：`RenderSystem.setShader()`、`tessellator.draw()`、`BufferRenderer.drawWithGlobalProgram()`、`VertexFormat.DrawMode` 全部移除 → 改用新 GPU 管线（`RenderPipeline`/`GpuBuffer`/`MeshData`/`RenderPass`），因此 3D 边界线渲染改为 HUD 文字方案
- `Camera.getPos()` → `Camera.getCameraPos()`（1.21.11 中更名）

### 5. 边界可视化 + 出生点 Y 修复

#### Glowstone 边界标记方块
- 边界不再只有 HUD 文字提示，现在会在边界四边地表放置 **Glowstone（荧石）** 方块
- 每 3 格放置一个标记，四边（北/南/西/东）全覆盖，从高空坠落时清晰可见
- 方块保存/恢复机制自动清理边界标记（`saveAreaBlocks` 先保存原始方块 → 放置标记 → `restoreAreaBlocks` 恢复原始方块）
- `placeBoundaryMarkers()` — 新增方法，在 `startGame()` 中 `saveAreaBlocks()` 之后调用

#### 出生点 Y 坐标修复
- **问题**：游戏结束时玩家可能被传送到基岩层（Y 过低）
- **原因**：`getSpawnPoint().getPos()` 返回的 BlockPos 中 Y 可能是默认值，而 `getTopY()` 在该位置可能返回异常值
- **修复**：`spawnY = Math.max(spawnPos.getY(), surfaceY)` — 取两者中较大值
- 额外兜底：若结果仍 ≤ `bottomY + 5`，回退到海平面 64

### 6. 原版标题倒计时 + 黑曜石边界墙 + 默认 3×3 + 玩家高亮

#### 准备阶段倒计时改用原版标题
- 10 秒准备阶段不再使用客户端 HUD 大字渲染，改用 Minecraft 原版 **Title/Subtitle** 系统
- 每秒向全体玩家发送 `TitleS2CPacket`（金色大字倒计时数字）+ `SubtitleS2CPacket`（"游戏即将开始…"）
- `TitleFadeS2CPacket(5, 15, 5)` 控制渐入/停留/渐出时间
- 准备结束时发送空 Title/Subtitle 清除

#### 边界黑曜石墙（基岩层 ~ Y=225）
- 替换之前的 Glowstone 地表标记，改为四边完整的 **黑曜石（Obsidian）墙**
- 每边从 `world.getBottomY()` 到 Y=225 全覆盖，高空坠落时极具视觉冲击力
- 四角只放一次避免重复，西/东边跳过已放置的角
- `buildObsidianWall()` 新增方法，`endGame` 中显式清理所有墙方块再调用 `restoreAreaBlocks`

#### 区块范围默认 3×3
- `GameSettings.DEFAULT_GAME_RANGE` 2 → **3**（48×48 格）

#### 玩家高亮（队伍颜色）
- `startPhase2` 中 `assignVanillaTeams` 之后，给所有玩家添加 `StatusEffects.GLOWING`（永久时长）
- 高亮颜色由原版计分板队伍颜色决定（红/蓝/绿/黄/紫 等）
- `endGame` 中移除所有玩家的 Glowing 效果

### 7. 出生点传送修复（Heightmap 缓存过期 → 手动搜索安全 Y）

#### 问题
- 游戏结束时 `restoreAreaBlocks()` 大量修改方块后，`getTopY(Heightmap.Type.WORLD_SURFACE)` 可能因缓存延迟返回错误的低位 Y 值
- 导致玩家被传送到地底方块中（卡住无法移动）

#### 修复：新增 `findSafeSurfaceY()` 方法
- 从世界顶部向下逐格搜索，找到第一个实心方块（`blocksMovement() == true`）
- 确认其上方 2 格（脚部 + 头部）均为非碰撞方块，返回脚部 Y 坐标
- 不依赖任何可能过期的 Heightmap，完全自主计算
- 替代位置：
  - `endGame()` — 传送回世界出生点
  - `enforceBoundary()` — 越界传送回区域中心
- **注意**：MC 1.21.1 中 `getTopY()` 无参方法已移除，改用 `bottomY + world.getHeight() - 1`

### 8. 玩家死亡后复活位置修复

#### 问题
- 玩家死亡后复活在世界出生点，而非游戏区域内
- 玩家死亡后高亮（GLOWING）效果消失（MC 会清除所有状态效果）

#### 修复：准备阶段结束时设置出生点
- `startPhase2()` — 传送玩家到游戏区域地表后，调用 `player.setSpawnPoint()` 将该位置设为玩家个人出生点
  - 使用 `WorldProperties.SpawnPoint.create()` + `ServerPlayerEntity.Respawn`（Fabric 1.21.11+ API 签名）
  - MC 原生死亡复活机制自动将玩家复活到该出生点
- `endGame()` — 游戏结束时调用 `player.setSpawnPoint(null, false)` 清除个人出生点，恢复世界出生点

#### 修复：复活后重新添加高亮
- 新增 `handlePlayerRespawn()` 方法 — 玩家复活时重新添加无限时长的 `GLOWING` 效果
- `WordTriggerDetector.AFTER_RESPAWN` 事件中调用（无需延迟，出生点已由 MC 自动处理）

### 9. 出生点 3×3 黑曜石平台

- 新增 `buildObsidianPlatform()` 方法 — 在指定地表坐标生成 3×3 黑曜石平台
- `startPhase2()` — 传送玩家到游戏区域地表之前，先在出生点铺设黑曜石平台
- **目的**：防止 TNT 等爆炸物破坏出生点（黑曜石爆炸抗性 1200）

### 10. 准备阶段满血满饥饿初始化

- `startPhase2()` — 关闭无敌的同时，为每位玩家执行：
  ```java
  player.setHealth(player.getMaxHealth());      // 生命值恢复至上限
  player.getHungerManager().setFoodLevel(20);     // 饥饿值满格
  player.getHungerManager().setSaturationLevel(5.0f); // 饱和度满格
  ```
- **效果**：无论游戏范围是否启用，准备阶段结束后所有玩家均以满状态开始游戏

### 11. 烟花发射 Thread.sleep 阻塞主线程 → 连接超时

#### 问题
- "60s不潜行" 词条触发后，若导致某玩家淘汰、场上只剩 1 人，触发 `checkWinCondition` → `spawnFireworks`
- `spawnFireworks` 在服务端主线程中使用 `Thread.sleep(delay * 50)` 延迟发射烟花，累计阻塞可达 **5 秒**
- 服务端主线程长时间阻塞导致玩家 KeepAlive 超时断连

#### 修复
- 延迟发射的烟花改为在独立线程中 `sleep`，然后通过 `server.execute()` 回到主线程生成烟花实体
- 第一枚烟花（delay=0）仍在主线程立即发射
- 不再阻塞服务端主线程

### 12. 囚笼试炼苦力怕延迟修复

#### 问题
- `tickCageTrial` 每 tick 都被调用（事件总长 10s），每次立即生成苦力怕并清空 map
- 导致苦力怕在第 1 tick（即事件开始瞬间）就出现，而非预期的 5 秒后

#### 修复
- `tickCageTrial` 新增 `remainingSeconds` 参数
- 仅当 `remainingSeconds == 5`（事件已运行 5s）时才生成苦力怕
- `tickActiveEvent` 的 `CAGE_TRIAL` 分支传入 `remainingSeconds`

### 13. 玩家互换位置可靠性修复

#### 问题
- "玩家互换位置" 特殊事件小概率能触发、大部分情况不触发
- **根因 1**：`Collections.shuffle` 随机打乱，有一定概率玩家位置不变（shuffle 后原地不动）
- **根因 2**：`player.teleport(world, x, y, z, Set.of(), yaw, pitch, false)` 8 参数重载在 Fabric 中不够可靠

#### 修复
- **换位策略**：`Collections.shuffle` → **循环右移一位**（确定性，100% 每人换到另一位玩家的位置）
- **传送 API**：`player.teleport(...)` → **`player.requestTeleport(x, y, z)`**（原版 `/tp` 底层 API，最可靠）
- 玩家视角（yaw/pitch）保持不变，仅位置交换

### 14. 版本号正式发布 1.0

- `gradle.properties` — `mod_version=1.0-SNAPSHOT` → `mod_version=1.0`
- 模组核心功能已稳定，正式标记为 1.0 版本

---

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

### 5. 项目文档全面梳理
- 整理完整指令清单：7 条管理指令（start/stop/status/vote/skip/setword/triggerspecialevent）
- 梳理词条全览：171 个词条，26 个类别（基础行为/挖掘/合成/视角/环境/背包/放置/丢弃/死亡等）
- 梳理特殊事件全览：30 个特殊事件（11 瞬时 + 19 持续）
- 补充游戏设置、架构总览等项目介绍

### 当前词条总数：171 种 | 指令：7 条 | 特殊事件：30 种

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

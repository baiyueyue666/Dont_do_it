# 开发日志

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

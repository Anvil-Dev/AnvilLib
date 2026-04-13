# module.wheel TODO

## 设计摘要（目标与边界）

- 目标：在 `module.wheel` 增加统一轮盘交互模型，支持点按/长按两种打开模式、分页（每页槽位可配置，默认 8）、可扩展渲染与触发回调。
- 目标：保持与 `WheelWidget` 现有渲染链路兼容，优先复用 `RawSection` 与 `SectionRenderer`。
- 边界：本期仅实现客户端 Screen 侧交互，不扩展网络同步与服务端逻辑。
- 边界：二级菜单仅在点按模式生效；长按模式遇到二级菜单项时忽略（不进入、不触发）。
- 边界：回调参数采用最小参数设计，不暴露复杂上下文对象。

## API 草案要点（建议）

- `WheelOpenMode`：`TAP`、`HOLD`。
- `WheelActionContext`（最小参数）：`pageIndex`、`slotIndex`、`itemId`、`openMode`。
- `WheelEntry`：`id`、`label`、`renderer`、`action`、`submenu`（可空）。
- `WheelEntryRenderer`：每项渲染扩展接口。
- `WheelEntryAction`：每项触发扩展接口。
- `WheelMenuModel` / `WheelPageModel`：负责按 `slotsPerPage` 分页（默认 8）、空白槽占位、可选性判定。
- `slotsPerPage` 配置：允许调用方自定义每页槽位数量，建议约束 `>= 1`。
- `WheelScreen`（或 `PagedWheelScreen`）：负责输入状态机与导航；底层复用 `WheelWidget` 绘制与高亮能力。

## Screen 行为规则

1. `TAP` 模式：打开后显示轮盘；鼠标移动改变高亮；鼠标点击触发当前高亮项。
2. `HOLD` 模式：按下即显示；按住期间更新高亮；松开按键时触发当前高亮项并关闭。
3. 每页槽位数量由 `slotsPerPage` 决定（默认 8）：超过上限自动分页；不足上限补空白槽。
4. 空白槽不可选、不可触发。
5. 鼠标滚轮翻页（上/下滚轮切页）。
6. 翻页后高亮重置为“无选中”（`selectedIndex = -1`）。
7. `TAP` 模式下，若项包含 `submenu`，点击进入二级菜单，不触发父项动作。
8. `HOLD` 模式下，若当前项包含 `submenu`，松开时忽略（不进入、不触发）。
9. 触发后统一走关闭流程（含关闭动画语义），避免重复触发。

## 任务清单

### P0

- [ ] 定义核心类型：`WheelOpenMode`、`WheelEntry`、`WheelActionContext`、`WheelEntryAction`、`WheelEntryRenderer`。
- [ ] 实现分页模型：按 `slotsPerPage` 切片、空白槽补齐、可选性判断。
- [ ] 增加 `slotsPerPage` 默认值与边界处理（默认 8，且 `>= 1`）。
- [ ] 实现双模式输入状态机：`TAP` 点击触发、`HOLD` 松开触发。
- [ ] 实现 `HOLD` 模式下二级菜单项忽略策略。
- [ ] 实现翻页后高亮重置逻辑。
- [x] 定义核心类型：`WheelOpenMode`、`WheelEntry`、`WheelActionContext`、`WheelEntryAction`、`WheelEntryRenderer`。
- [x] 实现分页模型：按 `slotsPerPage` 切片、空白槽补齐、可选性判断。
- [x] 增加 `slotsPerPage` 默认值与边界处理（默认 8，且 `>= 1`）。
- [x] 实现双模式输入状态机：`TAP` 点击触发、`HOLD` 松开触发。
- [x] 实现 `HOLD` 模式下二级菜单项忽略策略。
- [x] 实现翻页后高亮重置逻辑。

### P1

- [x] Screen 层接入滚轮翻页与菜单导航（含二级菜单进出）。
- [x] 抽离触发链路，确保回调只接收最小参数。
- [x] 统一关闭流程，避免重复触发与状态残留。

### P2

- [x] 补充示例代码（点按模式 + 长按模式）。
- [x] 更新模块文档，明确模式限制与行为约束。
- [x] 增加基础回归测试（分页、空槽、触发次数、二级菜单限制）。

## 验收标准（可测试项）

- [ ] 默认 `slotsPerPage=8` 时，1/8/9/17 个选项分页数分别为 1/1/2/3，且每页最多 8 槽。
- [ ] 自定义 `slotsPerPage=6` 时，1/6/7/13 个选项分页数分别为 1/1/2/3，且每页最多 6 槽。
- [ ] 翻页后高亮始终重置，空白槽不可高亮、不可触发。
- [ ] `TAP` 模式：普通项点击触发一次；二级菜单项点击进入子菜单且不触发父项。
- [ ] `HOLD` 模式：松开仅普通项触发一次；二级菜单项松开零触发、零跳转。
- [ ] 自定义渲染与触发接口可按项覆盖，且无需复杂上下文。
- [ ] 触发后关闭行为一致，无重复触发、无状态残留。


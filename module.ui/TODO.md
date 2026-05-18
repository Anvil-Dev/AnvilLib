# module.ui — 声明式 UI 系统

参考 ArkUI 声明式组件体系。纯 Java API，`Consumer`/`Runnable` 尾参，Kotlin SAM 转 DSL。

---

## 容器 / 布局组件

| 组件                        | 状态    | 用途                               |
|---------------------------|-------|----------------------------------|
| **Column**                | ✅ 已实现 | 纵向线性布局，主轴=垂直，交叉轴=水平              |
| **Row**                   | ✅ 已实现 | 横向线性布局，主轴=水平，交叉轴=垂直              |
| **Box** (≡ Stack)         | ✅ 已实现 | 层叠布局，子组件按声明顺序从底到顶重叠              |
| **Grid**                  | ✅ 已实现 | 网格布局，指定列数，自动换行                   |
| **Scrollable** (≡ Scroll) | ✅ 已实现 | 可滚动容器，maxHeight 超限时裁剪 + 滚动条 + 拖拽 |
| **Flex**                  | ❎ 未实现 | 弹性布局，子组件按权重分配空间                  |
| **List** / **LazyColumn** | ❎ 未实现 | 虚拟化长列表，仅渲染可见区域                   |
| **Tabs**                  | ❎ 未实现 | 标签页切换容器                          |
| **Swiper**                | ❎ 未实现 | 轮播/滑动切换容器                        |
| **SideBarContainer**      | ❎ 未实现 | 侧边栏抽屉容器                          |
| **Panel**                 | ❎ 未实现 | 可滑动面板                            |
| **Refresh**               | ❎ 未实现 | 下拉刷新容器                           |
| **RelativeContainer**     | ❎ 未实现 | 相对定位布局                           |

---

## 基础组件

| 组件                      | 状态    | 用途                                     |
|-------------------------|-------|----------------------------------------|
| **Text**                | ✅ 已实现 | 单行文字，支持颜色/阴影/对齐(LEFT/CENTER/RIGHT)     |
| **Button**              | ✅ 已实现 | 可点击按钮，原版配色，hover 状态，点击音效               |
| **Image**               | ✅ 已实现 | 纹理精灵渲染（`blitSprite`）                   |
| **Spacer** (≡ Blank)    | ✅ 已实现 | 固定尺寸空白占位                               |
| **Checkbox**            | ✅ 已实现 | 复选框，16×16 深灰外框 + 选中时白色内填充              |
| **Slider**              | ✅ 已实现 | 水平滑块，点击/拖拽设值，`Consumer<Float>` 回调      |
| **TextInput**           | ✅ 已实现 | 单行输入框，placeholder 占位，`charTyped` 多语言输入 |
| **Divider**             | ❎ 未实现 | 分割线（水平/垂直）                             |
| **Toggle**              | ❎ 未实现 | 开关切换（不同于 Checkbox 的方块填充风格）             |
| **Radio**               | ❎ 未实现 | 单选按钮                                   |
| **Progress**            | ❎ 未实现 | 进度条（线性/圆形）                             |
| **LoadingProgress**     | ❎ 未实现 | 加载动画                                   |
| **TextArea**            | ❎ 未实现 | 多行文本输入框                                |
| **Search**              | ❎ 未实现 | 搜索输入框                                  |
| **Select**              | ❎ 未实现 | 下拉选择器                                  |
| **Menu** / **MenuItem** | ❎ 未实现 | 右键菜单 / 弹出菜单                            |
| **Hyperlink**           | ❎ 未实现 | 超链接文字                                  |
| **Marquee**             | ❎ 未实现 | 跑马灯滚动文字                                |
| **Rating**              | ❎ 未实现 | 星级评分                                   |
| **Badge**               | ❎ 未实现 | 角标/红点提示                                |
| **QRCode**              | ❎ 未实现 | 二维码显示                                  |

---

## 选择器组件

| 组件             | 状态    | 用途      |
|----------------|-------|---------|
| **TextPicker** | ❎ 未实现 | 文字滚轮选择器 |
| **DatePicker** | ❎ 未实现 | 日期选择器   |
| **TimePicker** | ❎ 未实现 | 时间选择器   |

---

## 交互 / 手势

| 组件                      | 状态    | 用途                                           |
|-------------------------|-------|----------------------------------------------|
| **onClick** (≡ Gesture) | ✅ 已实现 | 点击事件，命中测试 + Button/Checkbox/Slider/TextInput |
| **onHover**             | ✅ 已实现 | hover 状态更新（`updateHoverRecursive`）           |
| **onScroll**            | ✅ 已实现 | 滚轮事件 → Scrollable 路由                         |
| **onDrag**              | ✅ 已实现 | 拖拽事件 → Slider + Scrollable 滚动条               |
| **onKey**               | ✅ 已实现 | 键盘事件 → `KeyInputHandler` + `charTyped`       |

---

## 状态管理

| 组件                      | 状态    | 用途                                                |
|-------------------------|-------|---------------------------------------------------|
| **Ref\<T\>** (≡ @State) | ✅ 已实现 | 可观察状态持有者，读取追踪 slot，写入精确 markDirty                 |
| **remember**            | ✅ 已实现 | 跨 recompose 持久化值，按调用位置缓存                          |
| **ref()**               | ✅ 已实现 | `comp.ref(0)` 等价于 `remember(() -> new Ref<>(0))`  |
| **Animatable**          | ✅ 已实现 | tick 驱动动画值，ease-in-out，`Composition.watch()` 自动推进 |

---

## 修饰符系统

| 组件                                   | 状态    | 用途                                   |
|--------------------------------------|-------|--------------------------------------|
| **Modifier**                         | ✅ 已实现 | 链式修饰符 API（`then`/`foldIn`/`foldOut`） |
| `.size()` / `.width()` / `.height()` | ✅ 已实现 | 尺寸约束                                 |
| `.fillMaxWidth()` / `.fillMaxSize()` | ✅ 已实现 | 填充父容器                                |
| `.padding()`                         | ✅ 已实现 | 内边距                                  |
| `.background()`                      | ✅ 已实现 | SDF 圆角填充背景                           |
| `.border()`                          | ✅ 已实现 | SDF 描边边框                             |

---

## 引擎核心

| 组件                    | 状态    | 用途                                                             |
|-----------------------|-------|----------------------------------------------------------------|
| **Composition**       | ✅ 已实现 | Slot table 引擎：emit / recompose / invalidate / copyRuntimeState |
| **DeclarativeScreen** | ✅ 已实现 | Screen 宿主：dirty check → recompose → measure → layout → render  |

---

## 统计

- 总计参考组件：**52**
- 已实现：**25**（含引擎核心 + 状态管理 + 修饰符）
- 未实现：**27**

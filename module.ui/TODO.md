# module.ui TODO

受 ArkUI 启发的声明式 UI 系统。纯 Java API，Consumer/Runnable 作为尾参，Kotlin SAM 转换自动获得尾随 Lambda DSL。
状态驱动，与 Minecraft GuiGraphicsExtractor 渲染管线集成。

---

## Phase 1: 构建系统 + 核心框架

- [x] `module.ui/build.gradle` — 添加 `implementation project(':anvillib-rendering-neoforge-26.1')`
- [x] `Constraints` — min/max width/height 约束
- [x] `Modifier` — 链式 API 接口（`then` / `foldIn` / `foldOut`）
- [x] `ModifierElement` — 单个修饰符节点接口
- [x] `UIComponent` — 核心接口：`measure(Constraints): MeasuredSize` / `layout(...)` / `extractRenderState(GuiGraphicsExtractor)`
- [x] `Composition` — slot table + `emit()` / `recompose()` / `invalidate()`

## Phase 2: 状态管理

- [x] `MutableState<T>` — 可观察状态：getter 记录 reader slot，setter 精确 markDirty
- [x] `remember { }` — 按 slot 位置持久化，recompose 时回读同一对象
- [x] 脏标记传播：只重执行 dirty group，干净子树跳过
- [x] `DeclarativeTestScreen` — 端到端验证 Screen（`/anvillib_test_client declarative`）

## Phase 3: 布局容器

- [x] `Column` + `ColumnScope` — 纵向排列（重构：spacing / verticalArrangement / horizontalAlignment）
- [x] `Row` + `RowScope` — 横向排列（horizontalArrangement / verticalAlignment / spacing）
- [x] `Box` + `BoxScope` — 层叠（contentAlignment）
- [x] MeasurePolicy 内联实现（随组件复杂度提升再提取为策略对象）
- [x] `Arrangement.Vertical` / `Arrangement.Horizontal` — Top/Center/Bottom, Start/Center/End, SpaceBetween/SpaceAround/SpaceEvenly
- [x] `Alignment.Horizontal` / `Alignment.Vertical` — Start/Center/End, Top/Center/Bottom

## Phase 4: 基础组件

- [x] `Text` — 重构：加 shadow（默认开启）、Align LEFT/CENTER/RIGHT、原版默认色
- [x] `Button` — 重构：原版配色（0xFF404040 / hover 0xFF606060）、shadow 文字、hover 状态预留
- [x] `Spacer` — 新建：固定尺寸空白占位
- [x] `Image` — 新建：`blitSprite` 渲染 `Identifier` 纹理

## Phase 5: Modifier Elements

- [x] `SizeElement` — `.size()` `.fillMaxWidth()` `.fillMaxHeight()` `.fillMaxSize()`
- [x] `PaddingElement` — `.padding(all)` `.padding(horizontal, vertical)`
- [x] `BackgroundElement` — `.background(color)` → SdfGraphics.box（支持 round）
- [x] `BorderElement` — `.border(width, color)` → SdfGraphics.stroke（支持 round）
- [x] 所有组件增加 `.modifier(Modifier)` setter（`modifier` 字段改为可变）
- [x] `ClickElement` — 推迟到 Phase 6 与输入路由一起实现

## Phase 6: 屏幕集成

- [x] `DeclarativeScreen` — Screen 子类，每帧 dirty check → recompose → measure → layout → render
- [x] `extractRenderState()` — 整合 Composition.renderFrame + hover 更新
- [x] 鼠标事件 — `mouseClicked` 命中测试 + 点击分发；hover 遍历更新 ButtonComponent
- [x] 键盘事件 — `keyPressed` (ESC 关闭)
- [x] 滚轮事件 — `mouseScrolled` 预留接口

## Phase 7: 输入组件

- [ ] `TextField` — 文本输入
- [ ] `Checkbox` — 布尔切换
- [ ] `Slider` — 连续范围选择

## Phase 8: 高级特性

- [ ] `Grid` — 网格布局
- [ ] `ForEach` — 循环渲染（带 key 稳定 slot 复用）
- [ ] `if` / `when` 条件渲染
- [ ] `Animatable` — 时间驱动动画值（基于 Minecraft tick，不依赖协程）
- [ ] `LazyColumn` — 虚拟化长列表

## Phase 9: 测试

- [ ] 布局算法单元测试（Column/Row/Box measure + layout）
- [ ] Slot diffing 单元测试（recompose 后 slot table 正确性）
- [ ] State 传播单元测试（精确 markDirty 范围）
- [ ] `module.test` 中创建示例 `DeclarativeScreen` 验证端到端

---

## 设计笔记：组件扩展方式

组件 = 工厂函数 + 实现类，两者并存：

- **工厂函数** — DSL 入口。创建实例 → 注册到父容器 + Slot Table → 返回实例（链式调用）
- **实现类** — 可继承、可覆盖 `measure()` / `layout()` / `extractRenderState()`

三种扩展方式：

### 1. 组合（90% 场景）

已有组件拼出新组件，不改内部实现。

```java
public static TextComponent FancyLabel(ColumnScope scope, String text) {
    return scope.Text(text).fontSize(24).color(0xFFAAAAFF);
}
```

### 2. 继承实现类（需要新行为时）

extends 现有组件或 implements `UIComponent`，覆盖核心方法，再提供配套工厂函数。

```java
public class RainbowText extends TextComponent {
    @Override
    public void extractRenderState(GuiGraphicsExtractor e) {
        this.color = Color.HSBtoRGB(...);
        super.extractRenderState(e);
    }
}
```

### 3. Modifier 扩展（可复用样式/行为）

封装常用样式为 Modifier 工厂方法，与具体组件解耦。

```java
public static Modifier cardStyle(Modifier m) {
    return m.background(0xFF333333).roundedCorner(8).padding(12, 8);
}
```

| 方式       | 适用                       | 耦合       |
|----------|--------------------------|----------|
| 组合       | 拼装现有组件                   | 无        |
| 继承       | 全新 measure/layout/render | 与父类耦合    |
| Modifier | 可复用样式/行为                 | 无，任何组件通用 |

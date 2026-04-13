# module.wheel

`module.wheel` 提供轮盘菜单的客户端 API：

- 打开模式：`TAP`（点按打开）与 `HOLD`（长按打开，松开触发）
- 分页：`slotsPerPage` 可配置，默认 `8`
- 空白槽位：自动补齐且不可触发
- 二级菜单：仅 `TAP` 模式可进入，`HOLD` 模式自动忽略

## 快速用法

```java
WheelMenuModel model = WheelMenuBuilder.create()
    .slotsPerPage(8)
    .action(
        "heal",
        Component.literal("Heal"),
        (graphics, pose, width, height) -> {
            // render icon
        },
        ctx -> {
            // trigger callback
        }
    )
    .submenu(
        "tools",
        Component.literal("Tools"),
        (graphics, pose, width, height) -> {
            // render icon
        },
        submenu -> submenu.action(
            "tool_a",
            Component.literal("Tool A"),
            (graphics, pose, width, height) -> {
            },
            ctx -> {
            }
        )
    )
    .build();

WheelScreenController controller = new WheelScreenController();

// 点按绑定
controller.openTap(model);

// 长按绑定
controller.onHoldKeyPressed(model);
controller.onHoldKeyReleased();
```

> `WheelScreenController` 只负责 Screen 生命周期；按键注册与输入监听由调用方自行接入。


# 结构投影

使用 `ProjectionScene` 收集局部方块、流体、方块实体与实体，再调用 `ProjectionRenderer.rebuild(scene, alpha)` 缓存网格。
调用方负责在渲染线程设置相机相对的 `PoseStack`，并在世界后处理完成后绘制，例如 `RenderLevelStageEvent.Stage.AFTER_LEVEL`。

- `render(pose)` 绘制完整预览；`render(pose, level, origin)` 根据真实方块状态隐藏已匹配位置。
- 同种方块状态不同显示黄色覆盖层，不同方块显示红色覆盖层，覆盖层向外扩展 0.05 格。
- 批量摆放使用 `prepare(pose, level, origin)` 和 `renderAll(placements)`，让全部投影共享深度预绘制。
- 资源重载后检查 `isValid()` 并重新构建；使用完毕调用 `close()` 释放 GPU 缓冲。

# 结构投影（1.21.11）

使用 `ProjectionScene` 收集局部方块、流体、方块实体与实体，再调用 `ProjectionRenderer.rebuild(scene, alpha)` 缓存网格。
调用方负责在渲染线程提供相机相对的 `PoseStack` 和 `CameraRenderState`，并在世界后处理完成后绘制。

- `render(pose, camera)` 绘制完整预览；带 `level`、`origin` 的重载根据真实方块状态隐藏已匹配位置。
- 同种方块状态不同显示黄色覆盖层，不同方块显示红色覆盖层，覆盖层向外扩展 0.05 格。
- 批量摆放使用 `prepare(pose, camera, level, origin)` 和 `renderAll(placements)`，让全部投影共享深度预绘制。
- 资源重载后检查 `isValid()` 并重新构建；使用完毕调用 `close()` 释放 GPU 缓冲。
- `ProjectionFeatures` 支持独立的实体预览；`clear()` 清除场景状态，`close()` 释放调度器与缓冲。

可用 `:anvillib-rendering-neoforge-1.21.11:compileJava` 检查编译，并在客户端检查透明叠加、镜像绕序、状态覆盖层和资源重载。

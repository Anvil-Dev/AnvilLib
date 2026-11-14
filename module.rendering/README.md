# 共享结构预览

`module.rendering` 提供独立于手册和具体机器的结构渲染与只读界面预览。

- 使用 `StructureSandboxFactory.create` 将原版 `StructureTemplate` 放入隔离客户端关卡，保存原始方块实体数据，并加载结构中的实体。
- 使用 `ViewportCameraRig`、`StructurePreviewRenderer` 和 `StructurePreviewPicker` 保持渲染、射线选块及屏幕坐标投影一致。
- 使用 `StructureGuiPreview` 打开原生容器浮窗。调用方转发鼠标点击、拖动和释放事件，并在每帧更新方块锚点；界面移除或结构改变时调用 `clear`。
- 使用 `StructureGuiProviders.register` 为特殊存储或无标准菜单的方块注册只读屏幕。提供器只可使用传入的隔离玩家、关卡与结构快照，禁止发送交互请求、修改真实库存或安装当前屏幕。
- 只读会话不调用交互、tick 或关闭生命周期，初始化与绘制处于 `StructureGuiSession.renderOnly` 作用域，真实玩家菜单在异常时同样恢复。
- 浮窗构造器可接受 `BooleanSupplier` 提供减少动态效果的偏好；砧库不读取调用方配置。
- 渲染器的 `Effects` 接口允许提交附加模型与覆盖效果。1.21.1、1.21.4 在相机矩阵有效时执行回调；1.21.8、1.21.11、26.1 在 PiP 纹理目标中执行；26.2、26.3 的覆盖回调收到已完成的纹理和 GUI 状态，应显式使用该纹理并在后续 GUI 层提交覆盖内容，禁止关闭原生持有的纹理。

蓝图编辑、扫描范围、扫描特效和模组库存快照由调用方负责。此模块不依赖藿香或铁砧工艺。

`previewTest` 验证提供器选择、重复注册保护、嵌套绘制及异常后的作用域恢复，并由 `check` 执行。渲染画面和第三方容器仍需客户端验证。

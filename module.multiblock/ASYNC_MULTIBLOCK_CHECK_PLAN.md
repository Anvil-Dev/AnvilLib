# 异步化周期检测计划（DynamicMultiblockManager）

目标：将 `DynamicMultiblockManager#checkMultiblockFormed(...)` 的实际检测工作移到工作线程池异步执行，主线程只负责收集快照与最终结果的合并/更新，从而避免阻塞服务器主线程并提升可扩展性。

---

## 检查清单（Checklist）

1. 在 `DynamicMultiblockManager` 增加可控的线程池与任务队列。
2. 修改 `checkMultiblockFormed(...)`：主线程只收集候选并为每个构建轻量快照（位置 + BlockState 列表/偏移）。
3. 将快照按批提交到线程池执行“BlockState-only”谓词测试。
4. 线程任务完成后，通过 `ServerLevel#getServer().execute(Runnable)` 或等价接口把结果回调到主线程，进行：
   - 最终一致性验证（控制器是否仍存在、多方块是否仍注册等）。
   - 必要时做深度校验（序列化/读取 BlockEntity NBT）并调用 `updateFormed(...)`。
5. 为大型结构使用分批/分页策略或降级采样检查，避免一次性快照耗尽内存或造成大量并发任务。
6. 增加并发控制（任务上限、超时、异常捕获、世界卸载时取消任务）。
7. 提供配置参数（线程数、并行上限、每帧最大检查数、分批大小、超时、结构阈值）。
8. 完成后运行集成/压力测试，观察延迟与 CPU 占用并微调参数。

---

## 核心执行流程（高层次）

1. 触发点：定时器在服务器端每 tick（或按配置间隔）调用 `DynamicMultiblockManager.checkMultiblockFormed(serverLevel)`。
2. 主线程：计算是否需要检查（保留原来的间隔计数逻辑），收集需要检查的 `MultiblockState`（只取引用/键，不读取 BlockEntity）。
3. 对每个候选，构建轻量不可变快照（类型：Snapshot）：
   - controllerPos (BlockPos)
   - definitionKey (Holder.Reference/引用标识)
   - 列表/迭代器 of (relativePos / absolutePos -> BlockState)  —— 仅 BlockState
   - 标记：是否需要 BlockEntity 深度校验
4. 将快照按批（batch）提交到线程池，工作线程对每个快照执行：
   - 运行定义的 predicates（BlockStatePredicate）只用传入的 BlockState 数据进行判断
   - 如有异常或超时，记录失败并回归（不直接修改世界）
   - 得出初步结果 formedCandidate = true/false
5. 结果回主线程：通过 server.execute(() -> {...}) 提交一个 Runnable，Runnable 做：
   - 验证 multiblocks 中仍包含该 controllerPos 且 definitionKey 未发生改变
   - 若需要深度校验（BlockEntity），在主线程读取 BlockEntity 并执行最终谓词测试
   - 调用 `updateFormed(level, MultiblockState, formed)` 并持久化/广播

---

## 大结构（Large Multiblock）处理策略

当定义的 global map（需要检测的方块总数）超过某个阈值时（配置项 `largeStructureThreshold`）：

1. 分批检测（推荐）：将所有偏移位置切分为若干 batch（`batchSize`），每个 batch 单独提交到线程池执行，然后在主线程汇总所有 batch 的结果（短路：若任一 batch 返回 false，则整体为 false）。
2. 采样检测（降级）：若结构极大且性能敏感，可以先对关键或随机样本位置执行检测；仅当采样通过才进行全体同步深度校验（主线程或分批）。
3. 同步回退：当线程池拥堵或达到任务上限时，改为在主线程执行（回退到原始行为），以避免检查丢失。

建议默认采用分批 + 短路策略，兼顾性能与准确性。

---

## 推荐关键参数（可放入 mod 配置）

- threadPoolSize:  min(4, 可用CPU核数 - 1)  （示例默认 4）
- maxParallelTasks: 64  （同时运行的快照任务上限）
- maxChecksPerTick: 128  （每个 tick 最多收集并提交的 MultiblockState 数量）
- batchSize: 64  （当结构很大时，分批检测的方块数）
- largeStructureThreshold: 1024  （若某个 multiblock 需要检测的方块数 > 1024，则启用分批检测）
- blockEntitySerializationThreshold: 64  （若需要对 BlockEntity 做异步判定，单任务内序列化的 BlockEntity 数量上限；通常应尽量避免）
- taskTimeoutMillis: 1000  （单个任务的超时时间，超过则视为失败并回退）
- samplingRatio: 0.1  （当启用采样降级策略时的采样比例）

这些参数应当可通过 `AnvilLibMultiblock.CONFIG` 或类似配置暴露并在运行时微调。

---

## 数据快照粒度建议

优先级（从轻量到重）：
1. 仅 BlockState（推荐）—— 在绝大多数 `BlockStatePredicate` 中已经足够决定是否匹配。性能最高、内存最低。
2. BlockState + 标记（需 BlockEntity）—— 仅在 predicate 明确依赖 BlockEntity 时，主线程再序列化 BlockEntity NBT（或同步读取）以做深度校验。
3. 完整 BlockEntity NBT（尽量避免）—— 如果必须进行异步 BlockEntity 判定，可把 BlockEntity 的必要字段序列化到 CompoundTag，注意这会增加 GC/内存压力。

---

## 假想 API 变更 / 代码位置提示

在 `DynamicMultiblockManager` 中新增成员（示例）：

- private final ExecutorService asyncExecutor;  // 使用固定线程池
- private final Semaphore parallelTaskLimiter; // 限制并行任务数
- private final AtomicBoolean shuttingDown;    // 在世界卸载时取消任务

新增方法：
- private Snapshot buildSnapshot(Level level, MultiblockState state)
- private void submitSnapshotForCheck(ServerLevel level, Snapshot snap)
- private void onSnapshotResult(ServerLevel level, Snapshot snap, boolean formed)
- public void shutdownAsyncExecutor() // 在世界卸载或系统关闭时调用

注意：所有直接修改 world / 保存 / 广播 的操作必须在主线程；异步线程只读快照数据。

---

## 错误处理与健壮性

- 线程异常捕获：在 worker 内部 try/catch Throwable 并把异常信息记录到日志，回传失败状态。
- 超时机制：用 Future.get(timeout) 或自主管理超时标志，超时后 cancel 并在主线程以失败或回退方式处理。
- 世界卸载/重载：在世界卸载时调用 `shutdownAsyncExecutor()`，并在回调中拒绝/取消所有未完成任务。
- 最终一致性校验：所有异步判断结果到主线程后，再次校验 MultiblockState 是否仍然注册且控制器方块未改变；若变化则丢弃结果。

---

## 示例伪代码流程（精简）

主线程：
```
if (!shouldCheck) return;
List<MultiblockState> candidates = collectCandidates(maxChecksPerTick);
for (state : candidates) {
  Snapshot snap = buildSnapshot(level, state); // 只读 BlockState 列表
  submitSnapshotForCheck(level, snap);
}
```

工作线程：
```
boolean testSnapshot(Snapshot snap) {
  for (pos, blockState) in snap.blocks {
    if (!predicate.testWithBlockState(blockState)) return false;
  }
  return true;
}
```

回主线程（通过 server.execute）：
```
if (!manager.containsController(snap.controllerPos)) return;
if (snap.requiresBlockEntityCheck) doDeepCheckAndPossiblyUpdate();
else manager.updateFormed(level, manager.getAt(snap.controllerPos), result);
```

---

## 下一步（可选）

- ✅ 已完成实现。详见以下修改的文件。

---

## 已修改文件清单

| 文件 | 变更说明 |
|------|---------|
| `BlockStatePredicate.java` | 新增 `testOffThread`、`testEntityOffThread`、`requiresBlockEntity` 方法 |
| `MultiblockCheckSnapshot.java` | **新文件**：不可变快照 record，包含 `test()` 方法供工作线程调用 |
| `DynamicMultiblockManager.java` | 重构周期检测为异步：线程池、快照构建、异步提交与主线程回调；保留同步 `checkMultiblockFormedSync` 用于 `onPlace` |
| `AnvilLibMultiblockConfig.java` | 新增 `asyncThreadPoolSize`、`maxChecksPerTick` 配置项 |
| `BlockEventListener.java` | 新增 `ServerStoppedEvent` 监听，关闭线程池 |

---

作者：异步多方块设计建议文档
生成时间：2026-04-20

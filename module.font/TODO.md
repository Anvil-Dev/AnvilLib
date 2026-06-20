# 模块字体性能与渲染修复

## P0 - 关键问题（导致可见卡顿及文字缺失）

### 1. 布局缓存 -- 已修复

- **文件:** `SdfTextLayout.java`, `SdfTextRenderer.java`, `SdfTextRenderState.java`
- **问题:** 每帧都重新计算整个文本字符串布局：码点转字形查询、UV计算、按页面分桶、新建对象（LinkedHashMap、ArrayList、GlyphQuad、SdfTextRenderState）
- **修复:** 根据`(atlasKey, text, scale)`缓存`SdfTextLayout`结果。存储四边形相对原点的位置；在`buildVertices()`时应用偏移量。

### 2. 异步字形创建 -- 已修复

- **后续修复:**
    - `SdfGlyphPage.dirty`设为`volatile`——确保跨线程纹理重传可见性
    - `placeGlyph()`/`fillPaddingForCell()`同步化——与`uploadPage`建立内存屏障
    - `createGlyphAsync`现在调用`SdfGlyphPage::updateHash`——防止因哈希过时跳过上传
    - `getIfReady`对失败future进行重试——临时错误不会永久禁用图集
    - `AnvilLibFont.getSelectFont()`预加载图集——避免首帧文字不可见
- **文件:** `SdfGlyphAtlas.java`, `SdfAtlasTexture.java`
- **问题:** 渲染线程同步调用`createGlyph()`时若字形未创建，对CJK文本可能需创建数百个字形（每个涉及AWT渲染+EDT距离变换，约O(n²)/字形），导致渲染循环阻塞多帧
- **修复:** 对未创建码点返回null，在单线程后台执行器异步创建。添加`pendingGlyphs`集合防重复请求。字形创建时同步图集，纹理上传时同步页面。

### 3. 修复`quadY`基线偏移 -- 已修复

- **文件:** `SdfTextRenderer.java`
- **问题:** `quadY = y - 2`是硬编码魔法值。未考虑实际图集基线位置和缩放，导致基线下方文字（如gjpqy等降部字母）被裁剪
- **修复:** `quadY = y - Math.round((atlas.awtAscent() + 2) * scale)`

## P1 - 显著改进

### 4. 调整`cellSize`适配完整字形 -- 已修复

- **文件:** `SdfGlyphAtlas.java:55`
- **问题:** `cellSize = Math.max(24, font.getSize() + 12)`——64pt字体→cellSize=76。但64pt字体的升部+降部可能超过76px，导致裁剪
- **修复:** `cellSize = Math.max(24, awtAscent + awtDescent + 4)`

### 5. 移除`renderMask()`中的基线截断 -- 已修复

- **文件:** `SdfGlyphAtlas.java:224`
- **问题:** `Math.min(this.cellSize - 4, this.awtAscent + 2)`在升部较大时截断基线，导致降部缺失
- **修复:** `g.drawString(s, 2, this.awtAscent + 2)`——若cellSize足够大（见#4）则无需截断

### 6. 修正`awtHeight()`返回实际度量高度 -- 已修复

- **文件:** `SdfGlyphAtlas.java`
- **问题:** 原先返回`font.getSize()`（磅值如64），非字体实际像素高度。影响`scaleFor()`计算
- **修复:** 现返回`awtHeight`（实际FontMetrics高度=升部+降部+行间距）

### 7. 用单调版本号取代`hashImage()` -- 已修复

- **文件:** `SdfAtlasTexture.java`, `SdfGlyphPage.java`
- **问题:** 每次新增字形时`hashImage()`遍历所有1,048,576像素。在字形创建后和`measureText()`后调用
- **修复:** `SdfGlyphPage`使用`AtomicInteger version`原子版本计数器，修改时`version.incrementAndGet()`，`uploadPage()`比较版本号而非内容哈希。已移除`hashImage()`方法。

## P2 - 优化项

### 8. 使用批量复制优化`toNativeImage()` -- 已修复

- **文件:** `SdfAtlasTexture.java`
- **问题:** 纹理上传时通过逐像素`image.getRGB(x, y)`处理百万级像素，每次getRGB调用都涉及Java2D颜色模型转换
- **修复:** 使用`image.getRaster().getDataElements()`一次性获取所有像素字节数组，消除逐像素getRGB开销

### 9. 布局临时对象池化 -- 暂缓

- **文件:** `SdfTextLayout.java`, `SdfTextRenderer.java`
- **问题:** 每次绘制都新建`LinkedHashMap`、多个`ArrayList`、`GlyphQuad`、`SdfTextRenderState`，增加GC压力
- **状态:** 因#1布局缓存已大幅降低布局计算频率（相同字符串仅计算一次），且`SdfTextRenderState`等记录对象生命周期极短（单帧），GC在ZGC/Generational ZGC下可高效处理。暂不实施对象池化，若后续profiling显示此部分仍为热点再考虑。

---

## 汇总

| # | 任务              | 状态  | 影响              | 工作量 |
|---|-----------------|-----|-----------------|-----|
| 1 | 布局缓存            | 已完成 | 消除重复CPU计算       | 中等  |
| 2 | 异步字形创建          | 已完成 | 消除渲染线程阻塞        | 中等  |
| 3 | 修复quadY偏移       | 已完成 | 修复降部裁剪          | 简单  |
| 4 | 修正cellSize      | 已完成 | 修复图集内字形裁剪       | 较小  |
| 5 | 移除基线截断          | 已完成 | 修复降部裁剪          | 简单  |
| 6 | 修正awtHeight()   | 已完成 | 正确文本缩放          | 简单  |
| 7 | 版本号替代哈希         | 已完成 | 单字形减少约100万次像素读取 | 较小  |
| 8 | NativeImage批量复制 | 已完成 | 加速纹理上传          | 较小  |
| 9 | 对象池化            | 暂缓  | 减少GC暂停          | 中等  |

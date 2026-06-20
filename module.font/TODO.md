# module.font Performance & Rendering Fixes

## P0 - Critical (causes visible stutter & missing text)

### 1. Layout Cache -- DONE
- **File:** `SdfTextLayout.java`, `SdfTextRenderer.java`, `SdfTextRenderState.java`
- **Problem:** Every frame, the entire text string is re-laid-out: codepoint -> glyph lookup, UV calculation, quad bucketing per page, new object allocations (LinkedHashMap, ArrayList, GlyphQuad, SdfTextRenderState)
- **Fix:** Cache `SdfTextLayout` results keyed by `(atlasKey, text, scale)`. Store quad positions relative to origin; apply offset during `buildVertices()`.

### 2. Async Glyph Creation -- DONE
- **File:** `SdfGlyphAtlas.java`, `SdfAtlasTexture.java`
- **Problem:** `glyph()` calls `createGlyph()` synchronously on the render thread when a glyph is not yet in the atlas. For CJK text, hundreds of glyphs may need creation, each involving AWT rendering + EDT distance transform (~O(n^2) per glyph), blocking the render loop for multiple frames.
- **Fix:** Return null for not-yet-created codepoints, enqueue async creation on a background single-threaded executor. Added `pendingGlyphs` set to prevent duplicate creation requests. `synchronized` on atlas for glyph creation, synchronized on page for texture upload.

### 3. Fix `quadY` Baseline Offset -- DONE
- **File:** `SdfTextRenderer.java`
- **Problem:** `quadY = y - 2` is a hardcoded magic number. Doesn't account for actual atlas baseline position or scale, causing text below baseline (descenders: g, j, p, q, y) to be clipped.
- **Fix:** `quadY = y - Math.round((atlas.awtAscent() + 2) * scale)`

## P1 - Significant improvement

### 4. Fix `cellSize` to Fit Full Glyph -- DONE
- **File:** `SdfGlyphAtlas.java:55`
- **Problem:** `cellSize = Math.max(24, font.getSize() + 12)` - 64pt font -> cellSize=76. But 64pt ascent+descent can exceed 76px, clipping ascenders and descenders.
- **Fix:** `cellSize = Math.max(24, awtAscent + awtDescent + 4)`

### 5. Remove Baseline Clamp in `renderMask()` -- DONE
- **File:** `SdfGlyphAtlas.java:224`
- **Problem:** `Math.min(this.cellSize - 4, this.awtAscent + 2)` clamps baseline when ascent is large, cutting off descenders.
- **Fix:** `g.drawString(s, 2, this.awtAscent + 2)` - no clamping needed if cellSize is large enough (see #4).

### 6. Fix `awtHeight()` to Return Actual Metrics Height -- DONE
- **File:** `SdfGlyphAtlas.java`
- **Problem:** `awtHeight()` returned `font.getSize()` (point size, e.g., 64), not the actual pixel height of the font. This affected `scaleFor()` calculation.
- **Fix:** Now returns `awtHeight` (actual FontMetrics height = ascent + descent + leading).

### 7. Replace `hashImage()` with Monotonic Version Number
- **File:** `SdfAtlasTexture.java:103-111`, `SdfGlyphPage.java`
- **Problem:** `hashImage()` iterates all 1,048,576 pixels every time a new glyph is added. Called from `SdfGlyphPage.updateHash()` after each glyph creation and after `measureText()`.
- **Fix:** Use an atomic version counter: `page.version++` on mutation, compare version instead of content hash.

## P2 - Nice to have

### 8. Optimize `toNativeImage()` with Bulk Copy
- **File:** `SdfAtlasTexture.java:75-84`
- **Problem:** Per-pixel `getRGB()`/`setPixel()` loop over 1M pixels during texture upload.
- **Fix:** Use `NativeImage` bulk write or `BufferedImage.getRaster().getDataElements()` for batch transfer.

### 9. Object Pooling for Layout Temporaries
- **File:** `SdfTextLayout.java`, `SdfTextRenderer.java`
- **Problem:** Each draw call allocates new `LinkedHashMap`, multiple `ArrayList`, `GlyphQuad` records, `SdfTextRenderState` records, causing GC pressure.
- **Fix:** Thread-local pools for `ArrayList`, `StringBuilder`, and vertex buffers. Reuse layout intermediates.

---

## Summary

| # | Task                    | Status | Impact                            | Effort  |
|---|-------------------------|--------|-----------------------------------|---------|
| 1 | Layout Cache            | DONE   | Eliminates repeated CPU work      | Medium  |
| 2 | Async Glyph Creation    | DONE   | Eliminates render-thread blocking | Medium  |
| 3 | Fix quadY offset        | DONE   | Fixes descender clipping          | Trivial |
| 4 | Fix cellSize            | DONE   | Fixes glyph clipping in atlas     | Small   |
| 5 | Remove baseline clamp   | DONE   | Fixes descender clipping          | Trivial |
| 6 | Fix awtHeight()         | DONE   | Corrects text scaling             | Trivial |
| 7 | Version instead of hash | TODO   | ~1M fewer pixel reads per glyph   | Small   |
| 8 | Bulk NativeImage copy   | TODO   | Faster texture upload             | Small   |
| 9 | Object pooling          | TODO   | Reduced GC pauses                 | Medium  |

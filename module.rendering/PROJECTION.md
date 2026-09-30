# Native structure projections (26.1)

The `dev.anvilcraft.lib.v2.rendering.projection` package ports the 1.21.1 renderer contract to the existing native rendering module. It does not require OpenGL vertex-buffer calls or a second renderer module.

Populate a `ProjectionScene` with local block positions, optional block entities, and entities. Populate all neighbors before calling `ProjectionRenderer.rebuild(scene, alpha)`; alpha is in `[0, 255]`. The scene preserves model data, shifted fluid neighbors and a customizable tint callback. Entities remain caller-owned and are never spawned or ticked by the renderer.

Render on the client render thread, supplying a camera-relative pose and `CameraRenderState`. The cached block mesh combines the current model-view matrix with that pose, matching the native entity buffer path. Movement requires only a new pose. Rebuild after scene/layer changes or whenever `isValid()` becomes false after resource reload. Call `close()` on replacement, world exit and permanent removal; repeated close is safe.

`ProjectionFeatures` provides native block-entity/entity submission for GUI previews too. Alpha 255 preserves the original render type. Call `clear()` to drop retained per-scene state without destroying its reusable native dispatcher, and `close()` when the owner is disposed. `ProjectionRenderTypes.blocks()` avoids applying entity diffuse lighting a second time to already shaded block/fluid quads.

Validation: module `compileJava`, `jar`, `check`, plus the paired Ageratum `runStructureTest` real-client fixture (33 checks covering cached rendering, layers, fluid neighbors, entities, movement, resource reload, replacement and cleanup). Ageratum contains repeatable 1.21.1 preview/projection captures. Custom resource packs, shader packs and other platforms were not validated in this node.

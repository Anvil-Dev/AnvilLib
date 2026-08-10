package dev.anvilcraft.lib.v2.rendering.sdf;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.joml.Vector4i;

@Getter
public class SdfParameters extends BufferObject<SdfParameters> {

    public static final BufferObjectLayoutDefinition<SdfParameters> DEFINITION = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<SdfParameters>ofVec4f().forGetter(SdfParameters::getSharedParams).build(),
            BufferObjectLayoutEntry.<SdfParameters>ofVec4f().forGetter(SdfParameters::getShapeParams).build(),
            BufferObjectLayoutEntry.<SdfParameters>ofVec4f().forGetter(SdfParameters::getRect).build(),
            BufferObjectLayoutEntry.<SdfParameters>ofVec4i().forGetter(SdfParameters::getTypeParams).build()
    );

    private final   Vector4f    sharedParams    = new Vector4f();
    private final   Vector4f    shapeParams     = new Vector4f();
    private final   Vector4f    rect            = new Vector4f();
    private final   Vector4i    typeParams      = new Vector4i();

    int                         uboIndex        = -1;
    boolean                     uploaded        = false;

    protected SdfParameters() {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
    }

    public void box(float width, float height) {
        this                ._renderType(SdfRenderType.BOX);
        this.shapeParams    .set(width * 0.5f, height * 0.5f, 0.0f, 0.0f);
    }

    public void circle(float radius) {
        this                ._renderType(SdfRenderType.CIRCLE);
        this.shapeParams    .set(radius, 0.0f, 0.0f, 0.0f);
    }

    public void arc(float sweep, float radius, float thickness) {
        this                ._renderType(SdfRenderType.ARC);

        var radian          = sweep * Mth.DEG_TO_RAD;
        var cos             = Mth.cos(radian);
        var sin             = Mth.sin(radian);
        this.shapeParams    .set(sin, cos, radius - thickness * 0.5f, thickness);
    }

    public void sector(float sweep, float radius, float thickness) {
        this                ._renderType(SdfRenderType.SECTOR);

        var radian          = sweep * Mth.DEG_TO_RAD;
        var cos             = Mth.cos(radian);
        var sin             = Mth.sin(radian);
        this.shapeParams    .set(cos, sin, radius - thickness * 0.5f, thickness);
    }

    public void pie(float sweep, float radius) {
        this                ._renderType(SdfRenderType.PIE);

        var radian          = sweep * Mth.DEG_TO_RAD;
        var cos             = Mth.cos(radian);
        var sin             = Mth.sin(radian);
        this.shapeParams    .set(cos, sin, radius, 0.0f);
    }

    public void capsule(float topRadius, float bottomRadius, float height) {
        this                ._renderType(SdfRenderType.CAPSULE);
        this.shapeParams    .set(topRadius, bottomRadius, height, 0.0f);
    }

    public void egg(float topRadius, float bottomRadius, float height) {
        this                ._renderType(SdfRenderType.EGG);
        this.shapeParams    .set(height, bottomRadius, topRadius, 0.0f);
    }

    public void segment(float x0, float y0, float x1, float y1) {
        this                ._renderType(SdfRenderType.SEGMENT);
        this.shapeParams    .set(x0, y0, x1, y1);
    }

    public void triangleEquilateral(float radius) {
        this                ._renderType(SdfRenderType.TRIANGLE_EQUILATERAL);
        this.shapeParams    .set(radius, 0.0f, 0.0f, 0.0f);
    }

    public void triangleIsosceles(float base, float height) {
        this                ._renderType(SdfRenderType.TRIANGLE_ISOSCELES);
        this.shapeParams    .set(base, height, 0.0f, 0.0f);
    }

    public void smooth(float smooth) {
        this                ._smooth(smooth);
    }

    public void stroke(float width) {
        this                ._width(width);
    }

    public void round(float radius) {
        this                ._cornerRadius(radius);
    }

    public void fill() {
        this                ._pass(SdfPassType.FILL);
    }

    public void onion(boolean enable) {
        this.typeParams.z   = enable ? 1 : 0;
    }

    public void light(float v) {
        this                ._pass(SdfPassType.LIGHT);
        this                ._smooth(v);
        this                ._light(v);
    }

    public SdfParameters duplicate() {
        var copy            = new SdfParameters();

        copy.sharedParams   .set(this.sharedParams);
        copy.shapeParams    .set(this.shapeParams);
        copy.rect           .set(this.rect);
        copy.typeParams     .set(this.typeParams);

        return              copy;
    }

    public float getSmooth() {
        return              this.sharedParams.x;
    }

    public float getStroke() {
        return              this.sharedParams.y;
    }

    public float getRound() {
        return              this.sharedParams.z;
    }

    public SdfRenderType getRenderType() {
        return              SdfRenderType
                            .fromOrdinal(this.typeParams.y);
    }

    public boolean isOnion() {
        return              this.typeParams.z != 0;
    }

    private void _smooth(float value) {
        this.sharedParams.x = value;
        this.uploaded       = false;
    }

    private void _light(float value) {
        this.sharedParams.w = 4.605f / value;
        this.uploaded       = false;
    }

    private void _width(float value) {
        this.sharedParams.y = value;
        this.uploaded       = false;
    }

    private void _cornerRadius(float value) {
        this.sharedParams.z = value;
        this.uploaded       = false;
    }

    private void _pass(SdfPassType value) {
        this.typeParams.x   = value.ordinal();
        this.uploaded       = false;
    }

    private void _renderType(SdfRenderType value) {
        this.typeParams.y   = value.ordinal();
        this.uploaded       = false;
    }

    @Override
    protected BufferObjectLayoutDefinition<SdfParameters> getDefinition() {
        return              DEFINITION;
    }

    public void reset() {
        this.sharedParams   .set(0.0f, 0.0f, 0.0f, 0.0f);
        this.shapeParams    .set(0.0f, 0.0f, 0.0f, 0.0f);
        this.rect           .set(0.0f, 0.0f, 0.0f, 0.0f);
        this.typeParams     .set(0, 0, 0, 4);

        this.uploaded       = false;
    }

    public boolean isShared() {
        return              this.uboIndex != -1;
    }
}

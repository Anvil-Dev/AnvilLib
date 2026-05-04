package dev.anvilcraft.lib.v2.rendering.sdf;

import dev.anvilcraft.lib.v2.rendering.foundation.ubo.UboLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.ubo.UboLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.ubo.UboObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.joml.Vector4i;

@Setter
@Getter
public class SdfParametersUbo extends UboObject<SdfParametersUbo> {

    public static final UboLayoutDefinition<SdfParametersUbo> DEFINITION = UboLayoutDefinition.create(
            UboLayoutEntry.<SdfParametersUbo>ofVec4f().forGetter(SdfParametersUbo::getSharedParams).build(),
            UboLayoutEntry.<SdfParametersUbo>ofVec4f().forGetter(SdfParametersUbo::getShapeParams).build(),
            UboLayoutEntry.<SdfParametersUbo>ofVec4f().forGetter(SdfParametersUbo::getRect).build(),
            UboLayoutEntry.<SdfParametersUbo>ofVec4i().forGetter(SdfParametersUbo::getTypeParams).build()
    );

    private final   Vector4f        sharedParams    = new Vector4f();
    private final   Vector4f        shapeParams     = new Vector4f();
    private final   Vector4f        rect            = new Vector4f();
    private final   Vector4i        typeParams      = new Vector4i();

    public void box(float width, float height) {
        this                    ._renderType(SdfRenderType.BOX);
        this.shapeParams        .set(width * 0.5f, height * 0.5f, 0.0f, 0.0f);
    }

    public void circle(float radius) {
        this                    ._renderType(SdfRenderType.CIRCLE);
        this.shapeParams       .set(radius, 0.0f, 0.0f, 0.0f);
    }

    public void arc(float sweep, float radius, float thickness) {
        this                    ._renderType(SdfRenderType.ARC);

        var radian              = sweep * Mth.DEG_TO_RAD;
        var cos                 = Mth   .cos(radian);
        var sin                 = Mth   .sin(radian);
        this.shapeParams       .set(sin, cos, radius - thickness * 0.5f, thickness);
    }

    public void sector(float sweep, float radius, float thickness) {
        this                    ._renderType(SdfRenderType.SECTOR);

        var radian              = sweep * Mth.DEG_TO_RAD;
        var cos                 = Mth   .cos(radian);
        var sin                 = Mth   .sin(radian);
        this.shapeParams       .set(cos, sin, radius - thickness * 0.5f, thickness);
    }

    public void pie(float sweep, float radius) {
        this                    ._renderType(SdfRenderType.PIE);

        var radian              = sweep * Mth.DEG_TO_RAD;
        var cos                 = Mth   .cos(radian);
        var sin                 = Mth   .sin(radian);
        this.shapeParams       .set(cos, sin, radius, 0.0f);
    }

    public void smooth(float smooth) {
        this                    ._smooth(smooth);
    }

    public void stroke(float width) {
        this                    ._width(width);
    }

    public void round(float radius) {
        this                    ._cornerRadius(radius);
    }

    public void fill() {
        this                    ._pass(SdfPassType.FILL);
    }

    public void onion(boolean enable) {
        this.typeParams.z       = enable ? 1 : 0;
    }

    public void light() {
        this                    ._pass(SdfPassType.LIGHT);
    }

    public void shared(float smooth, float stroke, float round) {
        this.sharedParams.set(
                smooth,
                stroke,
                round
        );
    }

    public SdfParametersUbo duplicate() {
        var copy                = new SdfParametersUbo();

        copy.sharedParams       .set(this.sharedParams);
        copy.shapeParams        .set(this.shapeParams);
        copy.rect               .set(this.rect);
        copy.typeParams         .set(this.typeParams);

        return                  copy;
    }

    private void _smooth(float value) {
        this.sharedParams.x     = value;
    }

    private void _width(float value) {
        this.sharedParams.y     = value;
    }

    private void _cornerRadius(float value) {
        this.sharedParams.z     = value;
    }

    private void _pass(SdfPassType value) {
        this.typeParams.x       = value.ordinal();
    }

    private void _renderType(SdfRenderType value) {
        this.typeParams.y       = value.ordinal();
    }

    @Override
    protected UboLayoutDefinition<SdfParametersUbo> getDefinition() {
        return                  DEFINITION;
    }

    public void reset() {
        this.sharedParams       .set(0.0f, 0.0f, 0.0f, 0.0f);
        this.shapeParams        .set(0.0f, 0.0f, 0.0f, 0.0f);
        this.rect               .set(0.0f, 0.0f, 0.0f, 0.0f);
        this.typeParams         .set(0, 0, 0, 4);
    }
}

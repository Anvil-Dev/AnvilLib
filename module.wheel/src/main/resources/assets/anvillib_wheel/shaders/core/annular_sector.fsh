#version 150

in vec4 vertexColor;

layout (std140) uniform AnnularSectorUniform {
    vec2 Center;
    float InnerDiameter;
    float OuterDiameter;
    float AntiAliasingRadius;
    float AngleAntiAliasingRad;
    float CenterAngleRad;
    float RangeAngleRad;
};

out vec4 fragColor;

const float PI = 3.14159265358979323846;
const float TAU = 2.0 * PI;

float normalizeAngleP(float angle) {
    float normalized = mod(angle, TAU);
    return normalized < 0.0 ? normalized + TAU : normalized;
}

float normalizeAnglePN(float angle) {
    float normalized = mod(angle, TAU);
    if (normalized < -PI) {
        return normalized + TAU;
    } else if (normalized > PI) {
        return normalized - TAU;
    } else {
        return normalized;
    }
}

float calcAngleAlpha(float posAngle, float centerAngle, float rangeAngle, float aa) {
    float center = normalizeAngleP(centerAngle);
    float dist = abs(normalizeAnglePN(normalizeAngleP(posAngle) - center));
    float angleAa = max(min(aa, fwidth(dist) * 1.5), 0.0001);

    return 1.0 - smoothstep(rangeAngle - angleAa, rangeAngle + angleAa, dist);
}

void main() {
    vec2 fragPos = gl_FragCoord.xy;
    vec4 color = vertexColor;
    float distance = distance(fragPos, Center);
    float angle = atan(fragPos.y - Center.y, fragPos.x - Center.x);
    float radialAa = max(min(AntiAliasingRadius, fwidth(distance) * 1.5), 0.0001);

    color.a *= smoothstep(InnerDiameter - radialAa, InnerDiameter + radialAa, distance)
             * (1.0 - smoothstep(OuterDiameter - radialAa, OuterDiameter + radialAa, distance))
             * calcAngleAlpha(angle, CenterAngleRad, RangeAngleRad, AngleAntiAliasingRad);

    fragColor = color;
}

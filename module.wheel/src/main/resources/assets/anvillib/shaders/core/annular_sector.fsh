#version 150

in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec2 Center;
uniform float InnerDiameter;
uniform float OuterDiameter;
uniform float AntiAliasingRadius;
uniform float AngleAntiAliasingRad;
uniform float CenterAngleRad;
uniform float RangeAngleRad;

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
    float range = normalizeAngleP(rangeAngle / 2);
    float center = normalizeAngleP(centerAngle);
    float dist = normalizeAnglePN(normalizeAngleP(posAngle) - center);

    return smoothstep(rangeAngle + aa, rangeAngle - aa, dist) * smoothstep(-rangeAngle - aa, -rangeAngle + aa, dist);
}

void main() {
    vec2 fragPos = gl_FragCoord.xy;
    vec4 color = vertexColor;
    float distance = distance(fragPos, Center);
    float angle = atan(fragPos.y - Center.y, fragPos.x - Center.x);

    color.a *= smoothstep(InnerDiameter - AntiAliasingRadius, InnerDiameter + AntiAliasingRadius, distance)
             * smoothstep(OuterDiameter + AntiAliasingRadius, OuterDiameter - AntiAliasingRadius, distance)
             * calcAngleAlpha(angle, CenterAngleRad, RangeAngleRad, AngleAntiAliasingRad);

    fragColor = color * ColorModulator;
}

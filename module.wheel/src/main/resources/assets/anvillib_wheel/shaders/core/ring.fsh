#version 150

in vec4 vertexColor;

layout (std140) uniform RingUniform {
    vec2 Center;
    float InnerDiameter;
    float OuterDiameter;
    float AntiAliasingRadius;
};

out vec4 fragColor;

void main() {
    float distance = distance(gl_FragCoord.xy, Center);

    vec4 color = vertexColor;

    color.a *= smoothstep(InnerDiameter - AntiAliasingRadius, InnerDiameter + AntiAliasingRadius, distance);
    color.a *= smoothstep(OuterDiameter + AntiAliasingRadius, OuterDiameter - AntiAliasingRadius, distance);

    fragColor = color;
}

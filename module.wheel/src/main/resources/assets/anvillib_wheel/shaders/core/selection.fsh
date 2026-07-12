#version 150

in vec4 vertexColor;

layout (std140) uniform SelectionUniform {
    vec2 FramebufferSize;
    vec2 Center;
    float Radius;
    float AntiAliasingRadius;
};

out vec4 fragColor;

void main() {
    vec2 fragPos = vec2(gl_FragCoord.x, FramebufferSize.y - gl_FragCoord.y);
    float distance = distance(fragPos, Center);
    vec4 color = vec4(0, 0, 0, 0);
    if (distance <= Radius) {
        color = vertexColor;
        color.a = smoothstep(Radius, 0.0, distance) * vertexColor.a;
    }

    fragColor = color;
}

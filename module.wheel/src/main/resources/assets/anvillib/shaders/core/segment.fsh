#version 150

in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec2 FramebufferSize;
uniform vec2 Point0;
uniform vec2 Point1;
uniform float LineWidth;
uniform float AntiAliasingRadius;

out vec4 fragColor;

void main() {
    // GUI 通道的 gl_FragCoord 以窗口左下角为原点，翻转至左上角原点的 GUI 坐标
    vec2 fragPos = vec2(gl_FragCoord.x, FramebufferSize.y - gl_FragCoord.y);
    // 点到线段的最短距离
    vec2 segment = Point1 - Point0;
    float length2 = dot(segment, segment);
    float t = length2 > 0.0 ? clamp(dot(fragPos - Point0, segment) / length2, 0.0, 1.0) : 0.0;
    float dist = distance(fragPos, Point0 + segment * t);
    float aa = max(min(AntiAliasingRadius, fwidth(dist) * 1.5), 0.0001);
    // LineWidth 为线段总宽度，边缘抗锯齿过渡
    float alpha = 1.0 - smoothstep(LineWidth * 0.5 - aa, LineWidth * 0.5 + aa, dist);

    vec4 color = vertexColor;
    color.a *= alpha;
    fragColor = color * ColorModulator;
}

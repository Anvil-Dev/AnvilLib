#version 150

in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec2 FramebufferSize;
uniform vec2 Center;
uniform float Radius;
uniform float AntiAliasingRadius;

out vec4 fragColor;

void main() {
    // GUI 通道的 gl_FragCoord 以窗口左下角为原点，翻转至左上角原点的 GUI 坐标
    vec2 fragPos = vec2(gl_FragCoord.x, FramebufferSize.y - gl_FragCoord.y);
    float dist = distance(fragPos, Center);
    float aa = max(min(AntiAliasingRadius, fwidth(dist) * 1.5), 0.0001);
    // 均匀圆形遮罩：半径内不透明，边缘抗锯齿过渡
    float mask = 1.0 - smoothstep(Radius - aa, Radius + aa, dist);

    vec4 color = vertexColor;
    color.a *= mask;
    fragColor = color * ColorModulator;
}

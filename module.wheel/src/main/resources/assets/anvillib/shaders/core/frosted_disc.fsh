#version 150

in vec2 texCoord0;
in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec2 FramebufferSize;
uniform vec2 Center;
uniform float Radius;
uniform float AntiAliasingRadius;

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    // GUI 通道的 gl_FragCoord 以窗口左下角为原点，翻转至左上角原点的 GUI 坐标
    vec2 fragPos = vec2(gl_FragCoord.x, FramebufferSize.y - gl_FragCoord.y);
    float dist = distance(fragPos, Center);
    float aa = max(min(AntiAliasingRadius, fwidth(dist) * 1.5), 0.0001);
    // 圆形盘面遮罩：半径内可见，边缘抗锯齿过渡
    float mask = 1.0 - smoothstep(Radius - aa, Radius + aa, dist);

    // Sampler0 为每帧高斯模糊后的主渲染目标
    vec4 scene = texture(Sampler0, texCoord0);

    // vertexColor 作为毛玻璃着色与混合强度：rgb 调制模糊内容，a 控制叠加透明度
    fragColor = vec4(scene.rgb * vertexColor.rgb, mask * vertexColor.a) * ColorModulator;
}

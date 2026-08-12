#version 150

in vec2 texCoord0;
in vec4 vertexColor;

layout (std140) uniform FrostedDiscUniform {
    vec2 FramebufferSize;
    vec2 Center;
    float Radius;
    float AntiAliasingRadius;
};

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    // GUI 渲染通道的 gl_FragCoord 与 GUI 坐标一致（左上角原点，Y 向下）。
    vec2 fragPos = gl_FragCoord.xy;
    float dist = distance(fragPos, Center);
    float aa = max(min(AntiAliasingRadius, fwidth(dist) * 1.5), 0.0001);
    // 圆形盘面遮罩：半径内可见，边缘抗锯齿过渡。
    float mask = 1.0 - smoothstep(Radius - aa, Radius + aa, dist);

    // GUI 渲染器以 "Sampler0" 为名绑定 TextureSetup 的第一个纹理
    vec4 scene = texture(Sampler0, texCoord0);

    // vertexColor 作为毛玻璃着色与混合强度：rgb 调制模糊内容，a 控制叠加透明度。
    fragColor = vec4(scene.rgb * vertexColor.rgb, mask * vertexColor.a);
}

#version 150

in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform sampler2D DiffuseSampler;
uniform vec2 Direction;
uniform float SampleStepLength;

out vec4 fragColor;

void main() {
    // 按 Direction 做可分离高斯模糊，SampleStepLength 为采样步长（纹素）
    vec2 texelSize = 1.0 / textureSize(DiffuseSampler, 0);
    vec2 step = Direction * SampleStepLength * texelSize;
    vec4 sum = texture(DiffuseSampler, texCoord0) * 0.227027;
    sum += texture(DiffuseSampler, texCoord0 + step) * 0.1945946;
    sum += texture(DiffuseSampler, texCoord0 - step) * 0.1945946;
    sum += texture(DiffuseSampler, texCoord0 + step * 2.0) * 0.1216216;
    sum += texture(DiffuseSampler, texCoord0 - step * 2.0) * 0.1216216;
    sum += texture(DiffuseSampler, texCoord0 + step * 3.0) * 0.054054;
    sum += texture(DiffuseSampler, texCoord0 - step * 3.0) * 0.054054;
    sum += texture(DiffuseSampler, texCoord0 + step * 4.0) * 0.016216;
    sum += texture(DiffuseSampler, texCoord0 - step * 4.0) * 0.016216;
    fragColor = sum * ColorModulator;
}

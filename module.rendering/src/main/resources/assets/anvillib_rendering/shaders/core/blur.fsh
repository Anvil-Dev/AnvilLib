#version 330

#moj_import<anvillib_rendering:util.glsl>

layout(std140) uniform BlurParameters {
    vec2 BlurDir;
    float SampleStepLength;
    float ColorMultiplier;
};

uniform sampler2D DiffuseSampler;

const float weight[7] = float[] (
    0.1827621234,
    0.1566532716,
    0.1220589867,
    0.0998664144,
    0.0130542649,
    0.0089357635,
    0.0080502373
);

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 texOffset = 1.0 / textureSize(DiffuseSampler, 0);

    vec3 result = texture(DiffuseSampler, texCoord).rgb * weight[0];
    for (int i = 1; i < 7; ++i) {
        result += texture(DiffuseSampler, texCoord + vec2(BlurDir.x * texOffset.x * i * SampleStepLength, BlurDir.y * texOffset.y * i * SampleStepLength)).rgb * weight[i];
        result += texture(DiffuseSampler, texCoord - vec2(BlurDir.x * texOffset.x * i * SampleStepLength, BlurDir.y * texOffset.y * i * SampleStepLength)).rgb * weight[i];
    }
    fragColor = vec4(saturate(result * ColorMultiplier), 1.0);
}

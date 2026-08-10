#version 330

layout(std140) uniform BloomParameters {
    float BloomIntensity;
    float BloomBlendThreshold;
    float LuminanceSensitivity;
};

uniform sampler2D GameSampler;
uniform sampler2D DiffuseSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 colorGame = texture(GameSampler, texCoord);
    vec3 color = colorGame.rgb;
    vec4 bloom4 = texture(DiffuseSampler, texCoord);
    vec3 bloom = bloom4.rgb * BloomIntensity;
    vec3 finalColor = color + (bloom * pow(BloomBlendThreshold, length(color) * LuminanceSensitivity));
    fragColor = vec4(finalColor, colorGame.a + bloom4.a);
}

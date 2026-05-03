#version 330

uniform sampler2D   DiffuseSampler;

layout(std140) uniform BloomParameters {
        vec2    uResolution;
        int     uFrameIndex;
};

in      vec2        texCoord;
out     vec4        fragColor;

void main() {
    vec2 uv         = texCoord;

    vec2 texel      = uResolution;
    vec4 bloom      =
                    texture(DiffuseSampler, uv + vec2(-0.5, -0.5) * texel) +
                    texture(DiffuseSampler, uv + vec2(0.5, -0.5) * texel) +
                    texture(DiffuseSampler, uv + vec2(-0.5, 0.5) * texel) +
                    texture(DiffuseSampler, uv + vec2(0.5, 0.5) * texel);

    bloom           *= 0.25;

    fragColor       = bloom;
}
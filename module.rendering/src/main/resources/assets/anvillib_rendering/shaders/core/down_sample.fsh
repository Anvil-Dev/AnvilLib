#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D   DiffuseSampler;

layout(std140) uniform BloomParameters {
        vec2    uResolution;
        int     uFrameIndex;
};

layout(location = 0) in      vec2        texCoord;
layout(location = 0) out     vec4        fragColor;

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
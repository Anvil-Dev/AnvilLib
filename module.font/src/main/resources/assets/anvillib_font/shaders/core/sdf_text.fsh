#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D DiffuseSampler;

layout(location = 0) in vec2 vTexCoord;
layout(location = 1) in vec4 vColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 sampleColor = texture(DiffuseSampler, vTexCoord);

    float distanceValue = sampleColor.r;
    float aa = max(fwidth(distanceValue), 0.04);
    float alpha = smoothstep(0.5 - aa, 0.5 + aa, distanceValue);

    vec4 color = vec4(vColor.rgb, vColor.a * alpha);
    fragColor = color;
}


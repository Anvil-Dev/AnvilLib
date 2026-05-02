#version 150

uniform sampler2D   uDiffuseSampler;
uniform sampler2D   uScreenSampler;
uniform sampler2D   uBaseSampler;

in      vec2        texCoord;
out     vec4        fragColor;

void main() {
    vec4 bloom      = texture(uDiffuseSampler, texCoord);
    vec4 screen     = texture(uScreenSampler, texCoord);
    vec4 base       = texture(uBaseSampler, texCoord);

    vec3 hdr        = clamp(bloom.rgb, 0.0, 1.0);
    vec3 src        = mix(screen.rgb, base.rgb, base.a);

    vec3 result     = src + hdr - src * hdr;
    fragColor       = vec4(result, 1.0);
}
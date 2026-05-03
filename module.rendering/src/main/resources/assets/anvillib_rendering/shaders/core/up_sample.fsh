#version 330

uniform sampler2D   DiffuseSampler;
uniform sampler2D   PreviousSampler;

layout(std140) uniform BloomParameters {
    vec2    uResolution;
    int     uFrameIndex;
};

in      vec2        texCoord;
out     vec4        fragColor;

#define             R               2
#define             KERNEL_SIZE     5

const   float       GAUSSIAN_KERNEL[25] = float[](
        0.002968, 0.013304, 0.021945, 0.013304, 0.002968,
        0.013304, 0.059634, 0.098320, 0.059634, 0.013304,
        0.021945, 0.098320, 0.162102, 0.098320, 0.021945,
        0.013304, 0.059634, 0.098320, 0.059634, 0.013304,
        0.002968, 0.013304, 0.021945, 0.013304, 0.002968
);

vec4 samplerGaussian(
    sampler2D       tex,
    vec2            texCoord,
    vec2            stride
) {
    vec4 color      = vec4(0.0);

    for (int    y   = -R;
                y   <= R;
                y   ++
    ) {
        for (
            int x   = -R;
                x   <= R;
                x   ++
        ) {
            int idx = (y + R) * KERNEL_SIZE + (x + R);
            float w = GAUSSIAN_KERNEL[idx];

            vec2 uv = texCoord + vec2(x, y) * stride;
            color   += texture(tex, uv) * w;
        }
    }

    return          color;
}

void main() {
//    vec2 noiseUV    = mod(texCoord.xy + vec2(uFrameIndex * 17), 128.0) * 0.0078125;

    vec2 curStride  = uResolution;
    vec2 lstStride  = curStride * 0.5;

    vec4 curMipped  = samplerGaussian(DiffuseSampler, texCoord, curStride);
    vec4 prvMipped  = samplerGaussian(PreviousSampler, texCoord, lstStride);
    fragColor       = curMipped * 0.5 + prvMipped * 0.9;
}

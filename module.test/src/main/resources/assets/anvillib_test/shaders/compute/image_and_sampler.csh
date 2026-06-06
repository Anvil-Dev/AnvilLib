#version 460 core

layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

layout(std140, binding = 0) uniform BlurParam {
    int uWidth;
    int uHeight;
};

layout(binding = 1) uniform sampler2D InTexture;

layout(binding = 2, rgba8) writeonly uniform image2D OutImage;

void main() {
    ivec2 idx = ivec2(gl_GlobalInvocationID.xy);

    if (idx.x >= uWidth || idx.y >= uHeight) return;

    const int r = 4;

    vec4 accum = vec4(0.0);
    int count = 0;

    for (int dy = -r; dy <= r; dy++) {
        for (int dx = -r; dx <= r; dx++) {
            ivec2 p = clamp(idx + ivec2(dx, dy),
            ivec2(0),
            ivec2(uWidth - 1, uHeight - 1));
            accum += texelFetch(InTexture, p, 0);
            count++;
        }
    }

    accum /= float(count);
    imageStore(OutImage, idx, accum);
}

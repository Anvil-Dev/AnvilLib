#version 460 core

layout(local_size_x = 32, local_size_y = 32, local_size_z = 1) in;

layout(std140, binding = 0) uniform ConvertParam {
    int uSrcWidth;
    int uSrcHeight;
    int uPaddedWidth;
    int uPaddedHeight;
    float uPadValue;
};

layout(binding = 1) uniform sampler2D Input;

layout(binding = 2, r32f) writeonly uniform image2D Output;

void main() {
    ivec2 idx = ivec2(gl_GlobalInvocationID.xy);

    if (idx.x >= uPaddedWidth || idx.y >= uPaddedHeight) {
        return;
    }

    vec4 result;

    if (idx.x >= uSrcWidth || idx.y >= uSrcHeight) {
        result = vec4(uPadValue, 0, 0, 1);
    } else {
        result = texelFetch(Input, idx, 0);
    }
    imageStore(Output, idx, vec4(result.r, 0.0, 0.0, 1.0));
}
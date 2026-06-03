#version 460 core

layout(local_size_x = 64, local_size_y = 1, local_size_z = 1) in;

struct Value {
    vec4 va;
    vec4 vb;
};

layout(std140, binding = 0) uniform ComputeUniform {
    mat4 ProjMat;
};

layout(std430, binding = 1) readonly buffer Input {
    Value data[];
} a;

layout(std430, binding = 2) writeonly buffer Output {
    vec4 result[];
} b;

layout(binding = 3, rgba8) uniform image2D Img;

layout(binding = 4, offset = 0) uniform atomic_uint InvocationCounter;

layout(binding = 5) uniform sampler2D TestTex;

void main() {
    uint idx = gl_GlobalInvocationID.x;

    ivec2 imgSize = imageSize(Img);
    ivec2 pos = ivec2(int(idx) % imgSize.x, int(idx) / imgSize.x);

    if (pos.y >= imgSize.y) return;

    vec2 uv = (vec2(pos) + 0.5) / vec2(imgSize);
    vec4 sampled = texture(TestTex, uv);

    imageStore(Img, pos, sampled);

    b.result[idx] = sampled;

    atomicCounterIncrement(InvocationCounter);
}
#version 460 core

layout(local_size_x = 16, local_size_y = 1, local_size_z = 1) in;

layout(std430, binding = 0) readonly buffer Input {
    float data[];
} a;

layout(std430, binding = 1) writeonly buffer Output {
    float data[];
} b;

layout(std140, binding = 2) uniform AddParameter {
    float f1;
    int arraySize;
};

layout(binding = 3) uniform atomic_uint Counter;

void main() {
    uint idx = gl_GlobalInvocationID.x;
    if (idx >= arraySize) {
        return;
    }
    b.data[idx] = a.data[idx] + f1;
    atomicCounterIncrement(Counter);
}
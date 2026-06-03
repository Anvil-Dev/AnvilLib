#version 460 core

layout(local_size_x = 64, local_size_y = 1, local_size_z = 1) in;

struct Value {
    float f;
};

layout(std430, binding = 0) readonly buffer Input {
    Value data[];
} a;

layout(std430, binding = 1) writeonly buffer Output {
    Value data[];
} b;

void main() {
    uint idx = gl_GlobalInvocationID.x;
    b.data[idx].f = a.data[idx].f * 2.0;
}
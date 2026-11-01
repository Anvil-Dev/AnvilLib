#version                330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

#define MAX_SDFS 256

struct Sdf {
    vec4                Shared;
    vec4                Shape;
    vec4                Rect;
    ivec4               Types;
};

layout(std140) uniform SDFParameters {
    Sdf[MAX_SDFS]       SDFs;
};

layout(location = 0) in      vec3            Position;
layout(location = 1) in      vec4            Color;
layout(location = 2) in      vec2            UV0;
layout(location = 3) in      ivec2           UV1;

layout(location = 0) out     vec2            vPosition;
layout(location = 1) out     vec4            vColor;
layout(location = 2) flat out int            vIndex;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    int index   = UV1.x;
    vPosition   = (UV0 - 0.5) * SDFs[index].Rect.zw;
    vColor      = Color;
    vIndex      = index;
}
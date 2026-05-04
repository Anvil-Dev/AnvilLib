#version                330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

#define MAX_SDFS 128

struct Sdf {
    vec4                Shared;
    vec4                Shape;
    vec4                Rect;
    ivec4               Types;
};

layout(std140) uniform SDFParameters {
    Sdf[MAX_SDFS]       SDFs;
};

in      vec3            Position;
in      vec4            Color;
in      vec2            UV0;
in      ivec2           UV1;

out     vec2            vPosition;
out     vec4            vColor;
flat out int            vIndex;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    int index   = UV1.x;
    vPosition   = (UV0 - 0.5) * SDFs[index].Rect.zw;
    vColor      = Color;
    vIndex      = index;
}
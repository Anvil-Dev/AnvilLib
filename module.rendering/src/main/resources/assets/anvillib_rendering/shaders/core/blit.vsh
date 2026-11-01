#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Transforms {
    mat4 ProjMat;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

layout(location = 0) out vec2 texCoord;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);

    texCoord = UV0;
}

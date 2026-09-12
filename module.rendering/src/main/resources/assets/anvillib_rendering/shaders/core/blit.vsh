#version 330

layout(std140) uniform Transforms {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);

    texCoord = UV0;
}

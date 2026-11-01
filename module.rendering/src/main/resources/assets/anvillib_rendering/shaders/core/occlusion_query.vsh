#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Transforms {
    mat4 ProjMat;
    mat4 CameraViewMat;
    mat4 ModelViewMat;
};

layout(location = 0) in vec3 Position;

void main() {
    gl_Position = ProjMat * CameraViewMat * ModelViewMat * vec4(Position, 1.0);
}

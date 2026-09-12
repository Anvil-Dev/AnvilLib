#version 330

layout(std140) uniform Transforms {
    mat4 ProjMat;
    mat4 CameraViewMat;
    mat4 ModelViewMat;
};

in vec3 Position;

void main() {
    gl_Position = ProjMat * CameraViewMat * ModelViewMat * vec4(Position, 1.0);
}

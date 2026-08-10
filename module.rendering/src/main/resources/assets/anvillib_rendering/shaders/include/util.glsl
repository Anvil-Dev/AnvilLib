
vec3 saturate(vec3 color) {
    return vec3(clamp(color.r, 0, 1), clamp(color.g, 0, 1), clamp(color.b, 0, 1));
}

float toneMap(float f) {
    return - (f - 1) * (f - 1) + 1;
}

vec3 toneMap(vec3 f) {
    return vec3(toneMap(f.r), toneMap(f.g), toneMap(f.b));
}


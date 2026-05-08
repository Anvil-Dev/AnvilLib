#version 330

uniform sampler2D DiffuseSampler;

in vec2 vTexCoord;
in vec4 vColor;

out vec4 fragColor;

void main() {
    vec4 sampleColor = texture(DiffuseSampler, vTexCoord);

    // The red channel carries distance in the final SDF atlas path.
    float distanceValue = sampleColor.r;
    float aa = max(fwidth(distanceValue), 0.002);
    float alpha = smoothstep(0.5 - aa, 0.5 + aa, distanceValue);

    vec4 color = vec4(vColor.rgb, vColor.a * alpha);
    fragColor = color;
}


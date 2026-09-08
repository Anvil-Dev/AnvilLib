#version 460 core

#ifndef MAX_MIP_LEVELS
#define MAX_MIP_LEVELS 12
#endif

struct AABB { // maybe its better using 3 * vec2: {minXY, maxXY, minMaxZZ}
    vec4 minPos;// w = 0
    vec4 maxPos;// w = 0
};

layout(std140, binding = 0) uniform CB {
    int elementCount;// number of aabb elements
    int mipLevels;// number of valid entries of uInputs and mipLayers, including mip 0
    vec2 viewportSize;
    vec4 cameraPos;// avoid vec3 in std140, cameraPos.w = 1
    mat4 ProjMat;// CameraRenderState#projectionMatrix
    mat4 CameraMat;// new Matrix4f(CameraRenderState#viewRotationMatrix)
} cbOcclusionTest;

layout(std430, binding = 0) buffer ShaderInput {
    ivec2 mipLayers[MAX_MIP_LEVELS + 1];
    AABB aabbs[];
};

layout(std430, binding = 1) buffer ShaderOutput {
    int result[];
};

#ifdef HIZ_DEBUG
layout(std430, binding = 2) buffer ShaderDebug {
    vec4 debugs[];
};
#endif

layout(binding = 0, r32f) readonly uniform image2D uInputs[MAX_MIP_LEVELS + 1];

layout(local_size_x = 32, local_size_y = 1, local_size_z = 1) in;

float loadHiZ(int mip, ivec2 coord) {
    switch (mip) {
        case 0: return imageLoad(uInputs[0], coord).r;
        case 1: return imageLoad(uInputs[1], coord).r;
        case 2: return imageLoad(uInputs[2], coord).r;
        case 3: return imageLoad(uInputs[3], coord).r;
        case 4: return imageLoad(uInputs[4], coord).r;
        case 5: return imageLoad(uInputs[5], coord).r;
        case 6: return imageLoad(uInputs[6], coord).r;
        case 7: return imageLoad(uInputs[7], coord).r;
        case 8: return imageLoad(uInputs[8], coord).r;
        case 9: return imageLoad(uInputs[9], coord).r;
        case 10: return imageLoad(uInputs[10], coord).r;
        case 11: return imageLoad(uInputs[11], coord).r;
        case 12: return imageLoad(uInputs[12], coord).r;
        default: return 1.0;
    }
}

void main() {
    uint index = gl_GlobalInvocationID.x;
    if (index >= uint(cbOcclusionTest.elementCount)) {
        return;
    }

    #ifdef HIZ_DEBUG
    debugs[index] = vec4(0, 0, 0, 0);
    #endif

    AABB box = aabbs[index];
    vec3 corners[8] = vec3[](
        box.minPos.xyz,
        vec3(box.maxPos.x, box.minPos.y, box.minPos.z),
        vec3(box.minPos.x, box.maxPos.y, box.minPos.z),
        vec3(box.maxPos.x, box.maxPos.y, box.minPos.z),
        vec3(box.minPos.x, box.minPos.y, box.maxPos.z),
        vec3(box.maxPos.x, box.minPos.y, box.maxPos.z),
        vec3(box.minPos.x, box.maxPos.y, box.maxPos.z),
        box.maxPos.xyz
    );
    vec2 minPixel = vec2(1e30);
    vec2 maxPixel = vec2(-1e30);
    float nearestDepth = 1.0;
    bool projected = false;

    for (int i = 0; i < 8; ++i) {
        vec3 relative = corners[i] - cbOcclusionTest.cameraPos.xyz;
        vec4 clip = cbOcclusionTest.ProjMat * (cbOcclusionTest.CameraMat * vec4(relative, 1.0));
        if (clip.w <= 0.0) {
            continue;
        }
        vec3 ndc = clip.xyz / clip.w;
        vec2 pixel = (ndc.xy * 0.5 + 0.5) * cbOcclusionTest.viewportSize;
        //pixel *= vec2(mipLayers[0]) / cbOcclusionTest.viewportSize;
        minPixel = min(minPixel, pixel);
        maxPixel = max(maxPixel, pixel);
        nearestDepth = min(nearestDepth, ndc.z * 0.5 + 0.5);
        projected = true;
    }

    if (!projected) {
        result[index] = 1;
        return;
    }
    vec2 mip0Size = vec2(mipLayers[0]);
    minPixel = clamp(minPixel, vec2(0.0), mip0Size);
    maxPixel = clamp(maxPixel, vec2(0.0), mip0Size);
    int mip = 0;
    int validMips = clamp(cbOcclusionTest.mipLevels, 0, MAX_MIP_LEVELS + 1);

    if (validMips == 0) {
        result[index] = 1;
        return;
    }

    ivec2 lo = ivec2(0);
    ivec2 hi = ivec2(0);
    bool foundMip = false;
    for (int level = 0; level < validMips; ++level) {
        vec2 mipSize = vec2(mipLayers[level]);
        float scale = exp2(float(level));
        lo = ivec2(floor(minPixel / scale));
        hi = ivec2(ceil(maxPixel / scale) - vec2(1.0));
        lo = clamp(lo, ivec2(0), ivec2(mipSize) - 1);
        hi = clamp(hi, ivec2(0), ivec2(mipSize) - 1);
        mip = level;
        if (all(lessThanEqual(hi - lo + ivec2(1), ivec2(2)))) {
            foundMip = true;
            break;
        }
    }


    #ifdef HIZ_DEBUG
    if (foundMip) {
        debugs[index].z = mip;
    } else {
        debugs[index].z = -1.0;
    }

    debugs[index].w = nearestDepth;
    #endif

    if (!foundMip) {
        // TODO
        result[index] = 2;
        return;
    }
    int visible = 0;
    for (int y = lo.y; y <= hi.y && visible == 0; ++y) {
        for (int x = lo.x; x <= hi.x; ++x) {
            if (nearestDepth <= loadHiZ(mip, ivec2(x, y))){
                visible = 1;
                break;
            }
        }
    }
    result[index] = visible;
}

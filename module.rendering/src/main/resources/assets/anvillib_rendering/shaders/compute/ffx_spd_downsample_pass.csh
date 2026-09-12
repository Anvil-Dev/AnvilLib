#version 460 core

// This file is part of the FidelityFX SDK.
//
// Copyright (C) 2024 Advanced Micro Devices, Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and /or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

// SPD pass
// SRV  0 : SPD_InputDownsampleSrc          : r_input_downsample_src
// UAV  0 : SPD_InternalGlobalAtomic        : rw_internal_global_atomic
// UAV  1 : SPD_InputDownsampleSrcMidMip    : rw_input_downsample_src_mid_mip
// UAV  2 : SPD_InputDownsampleSrcMips      : rw_input_downsample_src_mips
// CB   0 : cbSPD

#ifndef SPD_MAX_MIP_LEVELS
#define SPD_MAX_MIP_LEVELS 12
#endif

#ifndef FFX_SPD_OPTION_DOWNSAMPLE_FILTER
#define FFX_SPD_OPTION_DOWNSAMPLE_FILTER 0
#endif

#ifndef FFX_SPD_OPTION_LINEAR_SAMPLE
#define FFX_SPD_OPTION_LINEAR_SAMPLE 0
#endif

#ifndef FFX_SPD_OPTION_WAVE_INTEROP_LDS
#define FFX_SPD_OPTION_WAVE_INTEROP_LDS 0
#endif

#if FFX_SPD_OPTION_LINEAR_SAMPLE
#define SPD_LINEAR_SAMPLER 1
#endif

#if FFX_SPD_OPTION_WAVE_INTEROP_LDS
#define FFX_SPD_NO_WAVE_OPERATIONS 1
#else
#extension GL_KHR_shader_subgroup_quad : require
#endif


layout (/*set = 0, */binding = 0, std140) uniform cbFSR1_t
{
    uint  mips;
    uint  numWorkGroups;
    uvec2 workGroupOffset;
    vec2 invInputSize; // Only used for linear sampling mode
} cbFSR1;

// separate texture and sampler objects are unavailable in opengl
//layout (/*set = 0, */binding = 1000) uniform sampler s_LinearClamp;
//// SRVs
//layout (/*set = 0, */binding = 0) uniform texture2DArray r_input_downsample_src;
// in our usage case, those arrays only have one element, so replace it with a sampler2D
layout (/*set = 0, */binding = 0) uniform sampler2D r_input_downsample_src;

// UAV declarations
// replace huge binding slot in original shader 2000 to 2
layout (/*set = 0, */binding = 0, std430) coherent buffer rw_internal_global_atomic_t
{
    uint counter[6];
} rw_internal_global_atomic;

// replace huge binding slot in original shader 2001 to 3
// bind mip map 6 to this uniform
// change format from rgba32f to r32f because we are handling depth texture
layout (/*set = 0, */binding = 1, r32f) coherent uniform image2D rw_input_downsample_src_mid_mip;

// replace huge binding slot in original shader 2002 to 4
// change format from rgba32f to r32f because we are handling depth texture
layout (/*set = 0, */binding = 2, r32f) uniform image2D rw_input_downsample_src_mips[SPD_MAX_MIP_LEVELS + 1];

uint Mips()
{
    return cbFSR1.mips;
}

uint NumWorkGroups()
{
    return cbFSR1.numWorkGroups;
}

uvec2 WorkGroupOffset()
{
    return cbFSR1.workGroupOffset;
}

vec2 InvInputSize()
{
    return cbFSR1.invInputSize;
}


/// Compute an SRGB value from a linear value.
///
/// @param [in] value           The value to convert to SRGB from linear.
///
/// @returns
/// A value in SRGB space.
///
/// @ingroup GPUCore
float ffxSrgbFromLinear(float value)
{
    vec3 j = vec3(0.0031308 * 12.92, 12.92, 1.0 / 2.4);
    vec2 k = vec2(1.055, -0.055);
    // wrong clamp order? original:
    // return clamp(j.x, value * j.y, pow(value, j.z) * k.x + k.y);
    return clamp(pow(value, j.z) * k.x + k.y, j.x, value * j.y);
}

/// A helper function performing a remap 64x1 to 8x8 remapping which is necessary for 2D wave reductions.
///
/// The 64-wide lane indices to 8x8 remapping is performed as follows:
///
///     00 01 08 09 10 11 18 19
///     02 03 0a 0b 12 13 1a 1b
///     04 05 0c 0d 14 15 1c 1d
///     06 07 0e 0f 16 17 1e 1f
///     20 21 28 29 30 31 38 39
///     22 23 2a 2b 32 33 3a 3b
///     24 25 2c 2d 34 35 3c 3d
///     26 27 2e 2f 36 37 3e 3f
///
/// @param [in] a       The input 1D coordinate to remap.
///
/// @returns
/// The remapped 2D coordinates.
///
/// @ingroup GPUCore
uvec2 ffxRemapForWaveReduction(uint a)
{
    return uvec2(((a >> 2u) & 6u) | (a & 1u), ((a >> 3u) & 4u) | ((a >> 1u) & 3u));
}

// removed slice because we are using image2D/sampler2D
vec4 SampleSrcImage(ivec2 uv)
{
    vec2 textureCoord = vec2(uv) * InvInputSize() + InvInputSize();
    // vec4 result = textureLod(sampler2DArray(r_input_downsample_src, s_LinearClamp), vec3(textureCoord, slice), 0);
    vec4 result = textureLod(r_input_downsample_src, textureCoord, 0);
    // remove srgb convert because minecraft use linear rgb8 unorm
    // return vec4(ffxSrgbFromLinear(result.x), ffxSrgbFromLinear(result.y), ffxSrgbFromLinear(result.z), result.w);
    return result;
}

vec4 LoadSrcImage(ivec2 uv)
{
    return imageLoad(rw_input_downsample_src_mips[0], uv);
}

void StoreSrcMip(vec4 value, ivec2 uv, uint mip)
{
    imageStore(rw_input_downsample_src_mips[mip], uv, value);
}

vec4 LoadMidMip(ivec2 uv)
{
    return imageLoad(rw_input_downsample_src_mid_mip, uv);
}

void StoreMidMip(vec4 value, ivec2 uv)
{
    imageStore(rw_input_downsample_src_mid_mip, uv, value);
}

void IncreaseAtomicCounter(uint slice, inout uint counter)
{
    counter = atomicAdd(rw_internal_global_atomic.counter[slice], 1);
}

void ResetAtomicCounter(uint slice)
{
    rw_internal_global_atomic.counter[slice] = 0;
}

shared uint spdCounter;

void SpdIncreaseAtomicCounter(uint slice)
{
    IncreaseAtomicCounter(slice, spdCounter);
}

uint SpdGetAtomicCounter()
{
    return spdCounter;
}

void SpdResetAtomicCounter(uint slice)
{
    ResetAtomicCounter(slice);
}

shared float spdIntermediateR[16][16];
shared float spdIntermediateG[16][16];
shared float spdIntermediateB[16][16];
shared float spdIntermediateA[16][16];

vec4 SpdLoadSourceImage(ivec2 tex)
{
    #if defined SPD_LINEAR_SAMPLER
    return SampleSrcImage(tex);
    #else
    return LoadSrcImage(tex);
    #endif // SPD_LINEAR_SAMPLER
}

vec4 SpdLoad(ivec2 tex)
{
    return LoadMidMip(tex);
}

void SpdStore(ivec2 pix, vec4 outValue, uint mip)
{
    if (mip == 5)
        StoreMidMip(outValue, pix);
    else
        StoreSrcMip(outValue, pix, mip + 1);
}

vec4 SpdLoadIntermediate(uint x, uint y)
{
    return vec4(spdIntermediateR[x][y], spdIntermediateG[x][y], spdIntermediateB[x][y], spdIntermediateA[x][y]);
}

void SpdStoreIntermediate(uint x, uint y, vec4 value)
{
    spdIntermediateR[x][y] = value.x;
    spdIntermediateG[x][y] = value.y;
    spdIntermediateB[x][y] = value.z;
    spdIntermediateA[x][y] = value.w;
}

vec4 SpdReduce4(vec4 v0, vec4 v1, vec4 v2, vec4 v3)
{
    #if FFX_SPD_OPTION_DOWNSAMPLE_FILTER == 1
    return min(min(v0, v1), min(v2, v3));
    #elif FFX_SPD_OPTION_DOWNSAMPLE_FILTER == 2
    return max(max(v0, v1), max(v2, v3));
    #else
    return (v0 + v1 + v2 + v3) * 0.25;
    #endif
}

void ffxSpdWorkgroupShuffleBarrier()
{
    groupMemoryBarrier();
    barrier();
}

// Only last active workgroup should proceed
bool SpdExitWorkgroup(uint numWorkGroups, uint localInvocationIndex)
{
    // global atomic counter
    if (localInvocationIndex == 0)
    {
        SpdIncreaseAtomicCounter(0);
    }

    ffxSpdWorkgroupShuffleBarrier();
    return (SpdGetAtomicCounter() != (numWorkGroups - 1));
}

// User defined: vec4 SpdReduce4(vec4 v0, vec4 v1, vec4 v2, vec4 v3);
vec4 SpdReduceQuad(vec4 v)
{
    #if !defined(FFX_SPD_NO_WAVE_OPERATIONS)
    vec4 v0 = v;
    vec4 v1 = subgroupQuadSwapHorizontal(v);
    vec4 v2 = subgroupQuadSwapVertical(v);
    vec4 v3 = subgroupQuadSwapDiagonal(v);
    return SpdReduce4(v0, v1, v2, v3);
    #endif
    return v;
}

vec4 SpdReduceIntermediate(uvec2 i0, uvec2 i1, uvec2 i2, uvec2 i3)
{
    vec4 v0 = SpdLoadIntermediate(i0.x, i0.y);
    vec4 v1 = SpdLoadIntermediate(i1.x, i1.y);
    vec4 v2 = SpdLoadIntermediate(i2.x, i2.y);
    vec4 v3 = SpdLoadIntermediate(i3.x, i3.y);
    return SpdReduce4(v0, v1, v2, v3);
}

vec4 SpdReduceLoad4(uvec2 i0, uvec2 i1, uvec2 i2, uvec2 i3)
{
    vec4 v0 = SpdLoad(ivec2(i0));
    vec4 v1 = SpdLoad(ivec2(i1));
    vec4 v2 = SpdLoad(ivec2(i2));
    vec4 v3 = SpdLoad(ivec2(i3));
    return SpdReduce4(v0, v1, v2, v3);
}

vec4 SpdReduceLoad4(uvec2 base)
{
    return SpdReduceLoad4(base + uvec2(0, 0), base + uvec2(0, 1), base + uvec2(1, 0), base + uvec2(1, 1));
}

vec4 SpdReduceLoadSourceImage4(uvec2 i0, uvec2 i1, uvec2 i2, uvec2 i3)
{
    vec4 v0 = SpdLoadSourceImage(ivec2(i0));
    vec4 v1 = SpdLoadSourceImage(ivec2(i1));
    vec4 v2 = SpdLoadSourceImage(ivec2(i2));
    vec4 v3 = SpdLoadSourceImage(ivec2(i3));
    return SpdReduce4(v0, v1, v2, v3);
}

vec4 SpdReduceLoadSourceImage(uvec2 base)
{
    #if defined(SPD_LINEAR_SAMPLER)
    return SpdLoadSourceImage(ivec2(base));
    #else
    return SpdReduceLoadSourceImage4(base + uvec2(0, 0), base + uvec2(0, 1), base + uvec2(1, 0), base + uvec2(1, 1));
    #endif
}

void SpdDownsampleMips_0_1_Intrinsics(uint x, uint y, uvec2 workGroupID, uint localInvocationIndex, uint mip)
{
    vec4 v[4];

    ivec2 tex = ivec2(workGroupID.xy * 64) + ivec2(x * 2, y * 2);
    ivec2 pix = ivec2(workGroupID.xy * 32) + ivec2(x, y);
    v[0] = SpdReduceLoadSourceImage(tex);
    SpdStore(pix, v[0], 0);

    tex = ivec2(workGroupID.xy * 64) + ivec2(x * 2 + 32, y * 2);
    pix = ivec2(workGroupID.xy * 32) + ivec2(x + 16, y);
    v[1] = SpdReduceLoadSourceImage(tex);
    SpdStore(pix, v[1], 0);

    tex = ivec2(workGroupID.xy * 64) + ivec2(x * 2, y * 2 + 32);
    pix = ivec2(workGroupID.xy * 32) + ivec2(x, y + 16);
    v[2] = SpdReduceLoadSourceImage(tex);
    SpdStore(pix, v[2], 0);

    tex = ivec2(workGroupID.xy * 64) + ivec2(x * 2 + 32, y * 2 + 32);
    pix = ivec2(workGroupID.xy * 32) + ivec2(x + 16, y + 16);
    v[3] = SpdReduceLoadSourceImage(tex);
    SpdStore(pix, v[3], 0);

    if (mip <= 1)
        return;

    v[0] = SpdReduceQuad(v[0]);
    v[1] = SpdReduceQuad(v[1]);
    v[2] = SpdReduceQuad(v[2]);
    v[3] = SpdReduceQuad(v[3]);

    if ((localInvocationIndex % 4) == 0)
    {
        SpdStore(ivec2(workGroupID.xy * 16) + ivec2(x / 2, y / 2), v[0], 1);
        SpdStoreIntermediate(x / 2, y / 2, v[0]);

        SpdStore(ivec2(workGroupID.xy * 16) + ivec2(x / 2 + 8, y / 2), v[1], 1);
        SpdStoreIntermediate(x / 2 + 8, y / 2, v[1]);

        SpdStore(ivec2(workGroupID.xy * 16) + ivec2(x / 2, y / 2 + 8), v[2], 1);
        SpdStoreIntermediate(x / 2, y / 2 + 8, v[2]);

        SpdStore(ivec2(workGroupID.xy * 16) + ivec2(x / 2 + 8, y / 2 + 8), v[3], 1);
        SpdStoreIntermediate(x / 2 + 8, y / 2 + 8, v[3]);
    }
}

void SpdDownsampleMips_0_1_LDS(uint x, uint y, uvec2 workGroupID, uint localInvocationIndex, uint mip)
{
    vec4 v[4];

    ivec2 tex = ivec2(workGroupID.xy * 64) + ivec2(x * 2, y * 2);
    ivec2 pix = ivec2(workGroupID.xy * 32) + ivec2(x, y);
    v[0] = SpdReduceLoadSourceImage(tex);
    SpdStore(pix, v[0], 0);

    tex = ivec2(workGroupID.xy * 64) + ivec2(x * 2 + 32, y * 2);
    pix = ivec2(workGroupID.xy * 32) + ivec2(x + 16, y);
    v[1] = SpdReduceLoadSourceImage(tex);
    SpdStore(pix, v[1], 0);

    tex = ivec2(workGroupID.xy * 64) + ivec2(x * 2, y * 2 + 32);
    pix = ivec2(workGroupID.xy * 32) + ivec2(x, y + 16);
    v[2] = SpdReduceLoadSourceImage(tex);
    SpdStore(pix, v[2], 0);

    tex = ivec2(workGroupID.xy * 64) + ivec2(x * 2 + 32, y * 2 + 32);
    pix = ivec2(workGroupID.xy * 32) + ivec2(x + 16, y + 16);
    v[3] = SpdReduceLoadSourceImage(tex);
    SpdStore(pix, v[3], 0);

    if (mip <= 1)
        return;

    for (uint i = 0; i < 4; i++)
    {
        SpdStoreIntermediate(x, y, v[i]);
        ffxSpdWorkgroupShuffleBarrier();
        if (localInvocationIndex < 64)
        {
            v[i] = SpdReduceIntermediate(uvec2(x * 2 + 0, y * 2 + 0), uvec2(x * 2 + 1, y * 2 + 0), uvec2(x * 2 + 0, y * 2 + 1), uvec2(x * 2 + 1, y * 2 + 1));
            SpdStore(ivec2(workGroupID.xy * 16) + ivec2(x + (i % 2) * 8, y + (i / 2) * 8), v[i], 1);
        }
        ffxSpdWorkgroupShuffleBarrier();
    }

    if (localInvocationIndex < 64)
    {
        SpdStoreIntermediate(x + 0, y + 0, v[0]);
        SpdStoreIntermediate(x + 8, y + 0, v[1]);
        SpdStoreIntermediate(x + 0, y + 8, v[2]);
        SpdStoreIntermediate(x + 8, y + 8, v[3]);
    }
}

void SpdDownsampleMips_0_1(uint x, uint y, uvec2 workGroupID, uint localInvocationIndex, uint mip)
{
    #if defined(FFX_SPD_NO_WAVE_OPERATIONS)
    SpdDownsampleMips_0_1_LDS(x, y, workGroupID, localInvocationIndex, mip);
    #else
    SpdDownsampleMips_0_1_Intrinsics(x, y, workGroupID, localInvocationIndex, mip);
    #endif
}

void SpdDownsampleMip_2(uint x, uint y, uvec2 workGroupID, uint localInvocationIndex, uint mip)
{
    #if defined(FFX_SPD_NO_WAVE_OPERATIONS)
    if (localInvocationIndex < 64)
    {
        vec4 v = SpdReduceIntermediate(uvec2(x * 2 + 0, y * 2 + 0), uvec2(x * 2 + 1, y * 2 + 0), uvec2(x * 2 + 0, y * 2 + 1), uvec2(x * 2 + 1, y * 2 + 1));
        SpdStore(ivec2(workGroupID.xy * 8) + ivec2(x, y), v, mip);
        // store to LDS, try to reduce bank conflicts
        // x 0 x 0 x 0 x 0 x 0 x 0 x 0 x 0
        // 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        // 0 x 0 x 0 x 0 x 0 x 0 x 0 x 0 x
        // 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        // x 0 x 0 x 0 x 0 x 0 x 0 x 0 x 0
        // ...
        // x 0 x 0 x 0 x 0 x 0 x 0 x 0 x 0
        SpdStoreIntermediate(x * 2 + y % 2, y * 2, v);
    }
    #else
    vec4 v = SpdLoadIntermediate(x, y);
    v = SpdReduceQuad(v);
    // quad index 0 stores result
    if (localInvocationIndex % 4 == 0)
    {
        SpdStore(ivec2(workGroupID.xy * 8) + ivec2(x / 2, y / 2), v, mip);
        SpdStoreIntermediate(x + (y / 2) % 2, y, v);
    }
    #endif
}

void SpdDownsampleMip_3(uint x, uint y, uvec2 workGroupID, uint localInvocationIndex, uint mip)
{
    #if defined(FFX_SPD_NO_WAVE_OPERATIONS)
    if (localInvocationIndex < 16)
    {
        // x 0 x 0
        // 0 0 0 0
        // 0 x 0 x
        // 0 0 0 0
        vec4 v = SpdReduceIntermediate(uvec2(x * 4 + 0 + 0, y * 4 + 0), uvec2(x * 4 + 2 + 0, y * 4 + 0), uvec2(x * 4 + 0 + 1, y * 4 + 2), uvec2(x * 4 + 2 + 1, y * 4 + 2));
        SpdStore(ivec2(workGroupID.xy * 4) + ivec2(x, y), v, mip);
        // store to LDS
        // x 0 0 0 x 0 0 0 x 0 0 0 x 0 0 0
        // 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        // 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        // 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
        // 0 x 0 0 0 x 0 0 0 x 0 0 0 x 0 0
        // ...
        // 0 0 x 0 0 0 x 0 0 0 x 0 0 0 x 0
        // ...
        // 0 0 0 x 0 0 0 x 0 0 0 x 0 0 0 x
        // ...
        SpdStoreIntermediate(x * 4 + y, y * 4, v);
    }
    #else
    if (localInvocationIndex < 64)
    {
        vec4 v = SpdLoadIntermediate(x * 2 + y % 2, y * 2);
        v = SpdReduceQuad(v);
        // quad index 0 stores result
        if (localInvocationIndex % 4 == 0)
        {
            SpdStore(ivec2(workGroupID.xy * 4) + ivec2(x / 2, y / 2), v, mip);
            SpdStoreIntermediate(x * 2 + y / 2, y * 2, v);
        }
    }
    #endif
}

void SpdDownsampleMip_4(uint x, uint y, uvec2 workGroupID, uint localInvocationIndex, uint mip)
{
    #if defined(FFX_SPD_NO_WAVE_OPERATIONS)
    if (localInvocationIndex < 4)
    {
        // x 0 0 0 x 0 0 0
        // ...
        // 0 x 0 0 0 x 0 0
        vec4 v = SpdReduceIntermediate(uvec2(x * 8 + 0 + 0 + y * 2, y * 8 + 0),
            uvec2(x * 8 + 4 + 0 + y * 2, y * 8 + 0),
            uvec2(x * 8 + 0 + 1 + y * 2, y * 8 + 4),
            uvec2(x * 8 + 4 + 1 + y * 2, y * 8 + 4));
        SpdStore(ivec2(workGroupID.xy * 2) + ivec2(x, y), v, mip);
        // store to LDS
        // x x x x 0 ...
        // 0 ...
        SpdStoreIntermediate(x + y * 2, 0, v);
    }
    #else
    if (localInvocationIndex < 16)
    {
        vec4 v = SpdLoadIntermediate(x * 4 + y, y * 4);
        v = SpdReduceQuad(v);
        // quad index 0 stores result
        if (localInvocationIndex % 4 == 0)
        {
            SpdStore(ivec2(workGroupID.xy * 2) + ivec2(x / 2, y / 2), v, mip);
            SpdStoreIntermediate(x / 2 + y, 0, v);
        }
    }
    #endif
}

void SpdDownsampleMip_5(uvec2 workGroupID, uint localInvocationIndex, uint mip)
{
    #if defined(FFX_SPD_NO_WAVE_OPERATIONS)
    if (localInvocationIndex < 1)
    {
        // x x x x 0 ...
        // 0 ...
        vec4 v = SpdReduceIntermediate(uvec2(0, 0), uvec2(1, 0), uvec2(2, 0), uvec2(3, 0));
        SpdStore(ivec2(workGroupID.xy), v, mip);
    }
    #else
    if (localInvocationIndex < 4)
    {
        vec4 v = SpdLoadIntermediate(localInvocationIndex, 0);
        v = SpdReduceQuad(v);
        // quad index 0 stores result
        if (localInvocationIndex % 4 == 0)
        {
            SpdStore(ivec2(workGroupID.xy), v, mip);
        }
    }
    #endif
}

void SpdDownsampleMips_6_7(uint x, uint y, uint mips)
{
    ivec2 tex = ivec2(x * 4 + 0, y * 4 + 0);
    ivec2 pix = ivec2(x * 2 + 0, y * 2 + 0);
    vec4 v0 = SpdReduceLoad4(tex);
    SpdStore(pix, v0, 6);

    tex = ivec2(x * 4 + 2, y * 4 + 0);
    pix = ivec2(x * 2 + 1, y * 2 + 0);
    vec4 v1 = SpdReduceLoad4(tex);
    SpdStore(pix, v1, 6);

    tex = ivec2(x * 4 + 0, y * 4 + 2);
    pix = ivec2(x * 2 + 0, y * 2 + 1);
    vec4 v2 = SpdReduceLoad4(tex);
    SpdStore(pix, v2, 6);

    tex = ivec2(x * 4 + 2, y * 4 + 2);
    pix = ivec2(x * 2 + 1, y * 2 + 1);
    vec4 v3 = SpdReduceLoad4(tex);
    SpdStore(pix, v3, 6);

    if (mips <= 7)
        return;
    // no barrier needed, working on values only from the same thread

    vec4 v = SpdReduce4(v0, v1, v2, v3);
    SpdStore(ivec2(x, y), v, 7);
    SpdStoreIntermediate(x, y, v);
}

void SpdDownsampleNextFour(uint x, uint y, uvec2 workGroupID, uint localInvocationIndex, uint baseMip, uint mips)
{
    if (mips <= baseMip)
        return;
    ffxSpdWorkgroupShuffleBarrier();
    SpdDownsampleMip_2(x, y, workGroupID, localInvocationIndex, baseMip);

    if (mips <= baseMip + 1)
        return;
    ffxSpdWorkgroupShuffleBarrier();
    SpdDownsampleMip_3(x, y, workGroupID, localInvocationIndex, baseMip + 1);

    if (mips <= baseMip + 2)
        return;
    ffxSpdWorkgroupShuffleBarrier();
    SpdDownsampleMip_4(x, y, workGroupID, localInvocationIndex, baseMip + 2);

    if (mips <= baseMip + 3)
        return;
    ffxSpdWorkgroupShuffleBarrier();
    SpdDownsampleMip_5(workGroupID, localInvocationIndex, baseMip + 3);
}

/// Downsamples a 64x64 tile based on the work group id.
/// If after downsampling it's the last active thread group, computes the remaining MIP levels.
///
/// @param [in] workGroupID             index of the work group / thread group
/// @param [in] localInvocationIndex    index of the thread within the thread group in 1D
/// @param [in] mips                    the number of total MIP levels to compute for the input texture
/// @param [in] numWorkGroups           the total number of dispatched work groups / thread groups for this slice
/// @param [in] slice                   the slice of the input texture
///
/// @ingroup FfxGPUSpd
void SpdDownsample(uvec2 workGroupID, uint localInvocationIndex, uint mips, uint numWorkGroups)
{
    // compute MIP level 0 and 1
    uvec2 sub_xy = ffxRemapForWaveReduction(localInvocationIndex % 64);
    uint x = sub_xy.x + 8 * ((localInvocationIndex >> 6) % 2);
    uint y = sub_xy.y + 8 * (localInvocationIndex >> 7);
    SpdDownsampleMips_0_1(x, y, workGroupID, localInvocationIndex, mips);

    // compute MIP level 2, 3, 4, 5
    SpdDownsampleNextFour(x, y, workGroupID, localInvocationIndex, 2, mips);

    if (mips <= 6)
        return;

    // increase the global atomic counter for the given slice and check if it's the last remaining thread group:
    // terminate if not, continue if yes.
    if (SpdExitWorkgroup(numWorkGroups, localInvocationIndex))
        return;

    // reset the global atomic counter back to 0 for the next spd dispatch
    SpdResetAtomicCounter(0);

    // After mip 5 there is only a single workgroup left that downsamples the remaining up to 64x64 texels.
    // compute MIP level 6 and 7
    SpdDownsampleMips_6_7(x, y, mips);

    // compute MIP level 8, 9, 10, 11
    SpdDownsampleNextFour(x, y, uvec2(0, 0), localInvocationIndex, 8, mips);
}

/// Downsamples a 64x64 tile based on the work group id and work group offset.
/// If after downsampling it's the last active thread group, computes the remaining MIP levels.
///
/// @param [in] workGroupID             index of the work group / thread group
/// @param [in] localInvocationIndex    index of the thread within the thread group in 1D
/// @param [in] mips                    the number of total MIP levels to compute for the input texture
/// @param [in] numWorkGroups           the total number of dispatched work groups / thread groups for this slice
/// @param [in] slice                   the slice of the input texture
/// @param [in] workGroupOffset         the work group offset. it's (0,0) in case the entire input texture is downsampled.
///
/// @ingroup FfxGPUSpd
void SpdDownsample(uvec2 workGroupID, uint localInvocationIndex, uint mips, uint numWorkGroups, uvec2 workGroupOffset)
{
    SpdDownsample(workGroupID + workGroupOffset, localInvocationIndex, mips, numWorkGroups);
}

void DOWNSAMPLE(uint localThreadId, uvec3 workGroupId)
{
    SpdDownsample(workGroupId.xy, localThreadId, Mips(), NumWorkGroups(), WorkGroupOffset());
}

layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;
void main()
{
    DOWNSAMPLE(gl_LocalInvocationIndex, gl_WorkGroupID.xyz);
}

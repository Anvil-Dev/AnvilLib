#version                330

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

#define RT_BOX          0
#define RT_CIRCLE       1
#define RT_ARC          2
#define RT_SECTOR       3
#define RT_PIE          4
#define RT_EGG          5

#define PASS_FILL       0
#define PASS_STROKE     1
#define PASS_LIGHT      2

in      vec2            vPosition;
in      vec4            vColor;
flat in int             vIndex;

out     vec4            fragColor;

#define uSmoothRadius   (params.Shared.x)
#define uLightDecay     (params.Shared.x)
#define uStrokeWidth    (params.Shared.y)
#define uCornerRadius   (params.Shared.z)

// wdf? why it is different to the cpu side??
#define uRenderType     (params.Types.y)
#define uPassType       (params.Types.x)

// from https://iquilezles.org/articles/distfunctions2d/
float sdRect( in vec2 p, in vec2 b ) {
    vec2 d = abs(p)-b;
    return length(max(d,0.0)) + min(max(d.x,d.y),0.0);
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdRoundRect( in vec2 p, in vec2 b, in float r ) {
    vec2 q = abs(p)-b+vec2(r);
    return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r;
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdCircle(vec2 p, float r) {
    return length(p) - r;
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdArc(in vec2 p, in vec2 sc, in float ra, in float rb) {
    // sc is the sin/cos of the arc's aperture
    p.x = abs(p.x);
    return ((sc.y*p.x>sc.x*p.y) ? length(p-sc*ra) :
    abs(length(p)-ra)) - rb;
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdRing(in vec2 p, in vec2 n, in float r, in float th) {
    p.x = abs(p.x);
    p = mat2x2(n.x,n.y,-n.y,n.x)*p;
    return max( abs(length(p)-r)-th*0.5,
            length(vec2(p.x,max(0.0,abs(r-p.y)-th*0.5)))*sign(p.x) );
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdPie(in vec2 p, in vec2 c, in float r) {
    p.x = abs(p.x);
    float l = length(p) - r;
    float m = length(p-c*clamp(dot(p,c),0.0,r)); // c=sin/cos of aperture
    return max(l,m*sign(c.y*p.x-c.x*p.y));
}

void main() {
    Sdf     params          = SDFs[vIndex];
    vec4    shape           = params.Shape;
    vec2    p               = vPosition;
    float   alpha           = 0.0;

    float   d               = 1e5;
    switch      (uRenderType) {
        case    RT_BOX:
            d   = sdRect(p, shape.xy - vec2(uCornerRadius)) - uCornerRadius;
            break;
        case    RT_CIRCLE:
            d   = sdCircle(p, shape.x);
            break;
        case    RT_ARC:
            d   = sdArc(p, shape.xy, shape.z, shape.w) - uCornerRadius;
            break;
        case    RT_SECTOR:
            d   = sdRing(p, shape.xy, shape.z, shape.w) - uCornerRadius;
            break;
        case    RT_PIE:
            d   = sdPie(p, shape.xy, shape.z);
            break;
    }

    float   aa              = max(fwidth(d) * 0.5, uSmoothRadius);

    if (uPassType == PASS_FILL) {
        alpha               = smoothstep(0.0 + aa, 0.0 - aa, d);
    }
    else if (uPassType == PASS_STROKE) {
        alpha               = smoothstep(0.0 + aa, 0.0 - aa, d) *
                              smoothstep(0.0 - aa, 0.0 + aa, d + uStrokeWidth);
    }
    else if (uPassType == PASS_LIGHT) {
        float k             = uLightDecay;
        alpha               = exp(-abs(d) * k);
    }

    vec4    color           = vColor;
    color.a                 *= alpha;
    fragColor               = color;
}

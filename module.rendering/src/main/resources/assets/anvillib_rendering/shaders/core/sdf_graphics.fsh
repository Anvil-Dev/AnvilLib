#version                330

#define MAX_SDFS        256

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
#define RT_UCAPSULE     5
#define RT_EGG          6
#define RT_SEGMENT      7
#define RT_ETRIANGLE    8
#define RT_ITRIANGLE    9

#define PASS_FILL       0
#define PASS_LIGHT      1

in      vec2            vPosition;
in      vec4            vColor;
flat in int             vIndex;

out     vec4            fragColor;

#define uSmoothRadius   (params.Shared.x)
#define uStrokeWidth    (params.Shared.y)
#define uCornerRadius   (params.Shared.z)
#define uLightDecay     (params.Shared.w)

#define uPassType       (params.Types.x)
#define uRenderType     (params.Types.y)
#define uOnion          (params.Types.z)

// from https://iquilezles.org/articles/distfunctions2d/
float sdRect( in vec2 p, in vec2 b ) {
    vec2 d = abs(p)-b;
    return length(max(d,0.0)) + min(max(d.x,d.y),0.0);
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

// from https://iquilezles.org/articles/distfunctions2d/
float sdUnevenCapsule( vec2 p, float r1, float r2, float h )
{
    p.x = abs(p.x);
    float b = (r1-r2)/h;
    float a = sqrt(1.0-b*b);
    float k = dot(p,vec2(-b,a));
    if( k < 0.0 ) return length(p) - r1;
    if( k > a*h ) return length(p-vec2(0.0,h)) - r2;
    return dot(p, vec2(a,b) ) - r1;
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdEgg( in vec2 p, in float he, in float ra, in float rb )
{
    // all this can be precomputed for any given shape
    float ce = 0.5*(he*he-(ra-rb)*(ra-rb))/(ra-rb);

    // only this needs to be run per pixel
    p.x = abs(p.x);
    if( p.y<0.0 )             return length(p)-ra;
    if( p.y*ce-p.x*he>he*ce ) return length(vec2(p.x,p.y-he))-rb;
    return length(vec2(p.x+ce,p.y))-(ce+ra);
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdSegment( in vec2 p, in vec2 a, in vec2 b )
{
    vec2 ba = b-a;
    vec2 pa = p-a;
    float h = clamp( dot(pa,ba)/dot(ba,ba), 0.0, 1.0 );
    return length(pa-h*ba);
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdEquilateralTriangle( in vec2 p, in float r )
{
    const float k = sqrt(3.0);
    p.x = abs(p.x) - r;
    p.y = p.y + r/k;
    if( p.x+k*p.y>0.0 ) p = vec2(p.x-k*p.y,-k*p.x-p.y)/2.0;
    p.x -= clamp( p.x, -2.0*r, 0.0 );
    return -length(p)*sign(p.y);
}

// from https://iquilezles.org/articles/distfunctions2d/
float sdIsoscelesTriangle( in vec2 p, in vec2 q )
{
    p.x = abs(p.x);
    vec2 a = p - q*clamp( dot(p,q)/dot(q,q), 0.0, 1.0 );
    vec2 b = p - q*vec2( clamp( p.x/q.x, 0.0, 1.0 ), 1.0 );
    float s = -sign( q.y );
    vec2 d = min( vec2( dot(a,a), s*(p.x*q.y-p.y*q.x) ),
            vec2( dot(b,b), s*(p.y-q.y)  ));
    return -sqrt(d.x)*sign(d.y);
}

void main() {
    Sdf     params          = SDFs[vIndex];
    vec4    shape           = params.Shape;
    vec2    p               = vPosition;
    float   alpha           = 0.0;

    float   d               = 1e5;

    float   r               = uCornerRadius;
    float   r2              = r + r;
    switch      (uRenderType) {
        case    RT_BOX:
            d   = sdRect(p, shape.xy - vec2(r)) - r;
            break;
        case    RT_CIRCLE:
            d   = sdCircle(p, shape.x);
            break;
        case    RT_ARC:
            d   = sdArc(p, shape.xy, shape.z, shape.w) - r;
            break;
        case    RT_SECTOR:
            d   = sdRing(p, shape.xy, shape.z, shape.w) - r;
            break;
        case    RT_PIE:
            d   = sdPie(p, shape.xy, shape.z);
            break;
        case    RT_UCAPSULE:
            d   = sdUnevenCapsule(p, shape.x, shape.y, shape.z);
            break;
        case    RT_EGG:
            d   = sdEgg(p, shape.x, shape.y, shape.z);
            break;
        case    RT_SEGMENT:
            d   = sdSegment(p, shape.xy, shape.zw) - r;
            break;
        case    RT_ETRIANGLE:
            d   = sdEquilateralTriangle(p, shape.x - r2) - r;
            break;
        case    RT_ITRIANGLE:
            d   = sdIsoscelesTriangle(p - vec2(0.0, shape.y * 0.5 - r2), vec2(shape.x - r, r2 - shape.y)) - r;
            break;
    }

    float   aa              = max(fwidth(d) * 0.5, uSmoothRadius);
    float   halfWidth       = uStrokeWidth * 0.5;
    float   useOnion        = float(uOnion);    // int to float
    d                       = mix(d, abs(d) - halfWidth, useOnion);

    float   useLight        = float(uPassType); // now only have two passes, so just directly int to float
    float   fillAlpha       = smoothstep(0.0 + aa, 0.0 - aa, d);
    float   lightAlpha      = useLight * exp(-d * uLightDecay);

    alpha                   = mix(fillAlpha, lightAlpha, useLight);

    vec4    color           = vColor;
    color.a                 *= alpha;
    fragColor               = color;
}

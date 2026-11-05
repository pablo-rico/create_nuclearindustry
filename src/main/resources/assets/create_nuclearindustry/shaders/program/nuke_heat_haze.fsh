#version 150

// Hot air bends light: the scene seen through the air over a fireball, and along the ground where a shock front has just passed,
// ripples. Each pixel's point is rebuilt from the depth buffer in view space (camera at the origin; ProjScale and DepthRange
// undo the projection). The fireball ripples what is seen through the air round it and above it, where the hot gas rises; the
// shock front ripples the ground in a band round its radius. The ripple itself is a tileable noise scrolling upwards.
//   Fire:  radius, strength          FireCentre: the fireball's centre, view space
//   Shock: radius, band width, strength      GroundCentre: ground zero, view space; Up: the world's up, view space

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NoiseSampler;

uniform vec2 OutSize;
uniform vec2 ProjScale;
uniform vec2 DepthRange;
uniform vec3 FireCentre;
uniform vec3 GroundCentre;
uniform vec3 Up;
uniform vec2 Fire;
uniform vec3 Shock;
uniform float HazeTime;

in vec2 texCoord;

out vec4 fragColor;

float linearDepth(float d) {
    float n = DepthRange.x;
    float f = DepthRange.y;
    float z = d * 2.0 - 1.0;
    return 2.0 * n * f / (f + n - z * (f - n));
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    float z = depth >= 1.0 ? DepthRange.y : linearDepth(depth);
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec3 p = vec3(ndc.x * z * ProjScale.x, ndc.y * z * ProjScale.y, -z);
    float dist = length(p);
    vec3 dir = p / max(dist, 1e-4);
    float strength = 0.0;

    if (Fire.x > 0.0 && Fire.y > 0.0) {
        float along = clamp(dot(FireCentre, dir), 0.0, dist);
        vec3 nearest = dir * along;
        float miss = length(FireCentre - nearest);
        float reach = 1.0 - smoothstep(Fire.x * 0.8, Fire.x * 2.6, miss);
        float above = smoothstep(-Fire.x * 0.6, Fire.x * 1.6, dot(nearest - FireCentre, Up));
        strength += Fire.y * reach * (0.3 + 0.7 * above);
    }
    if (Shock.z > 0.0 && depth < 1.0) {
        vec3 rel = p - GroundCentre;
        float h = dot(rel, Up);
        float r = length(rel - h * Up);
        float band = exp(-pow((r - Shock.x) / max(Shock.y, 0.5), 2.0));
        float low = 1.0 - smoothstep(Shock.y * 0.5, Shock.y * 3.0, h);
        strength += Shock.z * band * low;
    }
    strength = clamp(strength, 0.0, 1.0);
    if (strength < 0.002) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
        return;
    }
    vec2 aspect = vec2(OutSize.x / OutSize.y, 1.0);
    vec2 n1 = texture(NoiseSampler, texCoord * aspect * 5.0 + vec2(0.0, -HazeTime * 0.6)).rg - 0.5;
    vec2 n2 = texture(NoiseSampler, texCoord * aspect * 11.0 + vec2(0.3, -HazeTime * 1.3)).rg - 0.5;
    vec2 offset = (n1 + 0.5 * n2) * strength * 0.012;
    // Opaque: the blit back onto the screen blends by alpha, and the screen behind is cleared.
    fragColor = vec4(texture(DiffuseSampler, texCoord + offset).rgb, 1.0);
}

#version 150

// Light of a rocket: flames, flares, flashes and glowing plasma, added onto what is behind. UV0 picks the shape:
//   a disc     UV in [-1, 1]^2, brightest at the centre;
//   a strip    U across in [-1, 1], V = 4 + blocks along it, flickering lengthwise like a flame;
//   a ring     UV in [-1, 1]^2 moved down by 8, bright round three quarters of the radius.
// The vertex colour is the light's colour and strength; its hottest middle turns white.

uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
    // Ticks, smooth through the frame.
    float t = GameTime * 24000.0;
    float a;
    if (texCoord0.y > 3.0) {
        float along = texCoord0.y - 4.0;
        float r = abs(texCoord0.x);
        float n = 0.6 * noise(vec2(texCoord0.x * 2.2 + 7.0, along * 1.6 - t * 1.9))
                + 0.4 * noise(vec2(texCoord0.x * 5.0 - 3.0, along * 4.5 - t * 3.7));
        float body = 1.0 - smoothstep(0.0, 1.0, r + (0.5 - n) * 0.35);
        a = body * body * (0.55 + 0.8 * n);
    } else if (texCoord0.y < -3.0) {
        vec2 uv = vec2(texCoord0.x, texCoord0.y + 8.0);
        float r = length(uv);
        float n = noise(uv * 5.0 + vec2(t * 0.2, 0.0));
        a = (1.0 - smoothstep(0.0, 0.2, abs(r - 0.78))) * (0.6 + 0.6 * n);
    } else {
        float r = length(texCoord0);
        float n = noise(texCoord0 * 2.5 + vec2(t * 0.7, -t * 1.1));
        a = max(0.0, (exp(-3.5 * r * r) - exp(-3.5)) / (1.0 - exp(-3.5))) * (0.85 + 0.3 * n);
    }
    float alpha = vertexColor.a * a;
    if (alpha < 0.002) {
        discard;
    }
    vec3 rgb = mix(vertexColor.rgb, vec3(1.0), clamp(alpha * alpha * 0.7, 0.0, 0.85));
    fragColor = vec4(rgb, min(1.0, alpha)) * ColorModulator;
}

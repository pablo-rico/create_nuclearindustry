#version 150

// Smoke, steam and dust of a rocket, blended over what is behind. The same shapes as rocket_glow (disc, strip, ring), with
// billowing edges: noise eats into the outline so a trail or a puff never shows the quad it is drawn on. The pattern of a
// strip is fixed to the blocks along it, so a trail's lumps stay where they were left and only slowly churn.

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

float fbm(vec2 p) {
    return 0.55 * noise(p) + 0.3 * noise(p * 2.1 + 5.2) + 0.15 * noise(p * 4.3 - 2.7);
}

void main() {
    float t = GameTime * 24000.0;
    float r;
    float n;
    if (texCoord0.x > 6.0) {
        // A solid strip (a rocket's body from afar, U moved up by 8): a crisp edge, shaded round like a cylinder.
        float u = texCoord0.x - 8.0;
        float edge = 1.0 - smoothstep(0.8, 1.0, abs(u));
        if (vertexColor.a * edge < 0.004) {
            discard;
        }
        float round = 0.62 + 0.38 * sqrt(max(0.0, 1.0 - u * u)) + 0.12 * u;
        fragColor = vec4(vertexColor.rgb * round, vertexColor.a * edge) * ColorModulator;
        return;
    }
    if (texCoord0.y > 3.0) {
        float along = texCoord0.y - 4.0;
        r = abs(texCoord0.x);
        n = fbm(vec2(texCoord0.x * 1.4 + 3.1, along * 0.9 - t * 0.01));
    } else if (texCoord0.y < -3.0) {
        vec2 uv = vec2(texCoord0.x, texCoord0.y + 8.0);
        float d = length(uv);
        n = fbm(uv * 3.0 + vec2(t * 0.01, 0.0));
        // The ring's band, as a distance from its middle scaled to [0, 1].
        r = clamp(abs(d - 0.72) / 0.28, 0.0, 1.0);
    } else {
        r = length(texCoord0);
        n = fbm(texCoord0 * 1.7 + vec2(t * 0.004, 1.3));
    }
    float body = 1.0 - smoothstep(0.2, 1.0, r + (0.5 - n) * 0.55);
    float alpha = vertexColor.a * body * (0.7 + 0.45 * n);
    if (alpha < 0.004) {
        discard;
    }
    vec3 rgb = vertexColor.rgb * (0.82 + 0.3 * n);
    fragColor = vec4(rgb, min(1.0, alpha)) * ColorModulator;
}

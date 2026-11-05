#version 150

// Light alone, added onto the scene: a fireball's corona, a conventional fireball's core, a flash's bloom. The atlas tile's red
// is the temperature; its hottest part turns white.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    float t = tex.r * tex.a;
    float a = vertexColor.a * t;
    if (a < 0.003) {
        discard;
    }
    vec3 rgb = mix(vertexColor.rgb, vec3(1.0), clamp(t * t * 0.6, 0.0, 0.7));
    fragColor = vec4(rgb, min(1.0, a)) * ColorModulator;
}

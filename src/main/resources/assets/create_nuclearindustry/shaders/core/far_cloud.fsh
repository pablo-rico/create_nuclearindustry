#version 150

// Sampler0 is Distant Horizons' depth. With Occlude on, a puff behind its distant terrain does not show: the terrain's depth is
// turned back into a distance with the inverse of its projection, so this works whichever way its depth runs. EmptyDepth is the
// depth where it drew nothing. Sampler1 is the cloud atlas, drawn as nuke_cloud draws it (premultiplied alpha): smoke, fire
// tiles glowing by their temperature, lightning added on.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform vec4 HazeColor;
uniform mat4 InverseProjMat;
uniform vec2 ScreenSize;
uniform float Occlude;
uniform float EmptyDepth;
uniform float DepthZeroToOne;

in vec2 texCoord0;
in vec4 vertexColor;
in vec3 viewPosition;

out vec4 fragColor;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }
    if (Occlude > 0.5) {
        float depth = texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).r;
        if (depth != EmptyDepth) {
            vec2 uv = gl_FragCoord.xy / ScreenSize;
            vec4 ndc = vec4(uv * 2.0 - 1.0, DepthZeroToOne > 0.5 ? depth : depth * 2.0 - 1.0, 1.0);
            vec4 terrain = InverseProjMat * ndc;
            if (length(terrain.xyz / terrain.w) < length(viewPosition)) {
                discard;
            }
        }
    }
    vec2 uv = texCoord0;
    vec4 tex = texture(Sampler1, uv);
    float a = vertexColor.a * tex.a * ColorModulator.a;
    if (uv.y >= 0.5 && uv.y < 0.75) {
        float t = tex.r;
        float hot = smoothstep(0.12, 0.55, t);
        vec3 rgb = mix(vec3(0.09, 0.085, 0.08) * (0.6 + 0.8 * t), vertexColor.rgb * (0.35 + 1.15 * pow(t, 1.4)), hot);
        if (a < 0.003) {
            discard;
        }
        fragColor = vec4(rgb * (1.0 - 0.7 * HazeColor.a) * a, a * (0.55 + 0.45 * (1.0 - hot)));
        return;
    }
    if (uv.x >= 0.75 && uv.y >= 0.75) {
        float l = tex.a * max(tex.r, max(tex.g, tex.b));
        fragColor = vec4(vertexColor.rgb * l * vertexColor.a * 1.6 * (1.0 - 0.7 * HazeColor.a), 0.0);
        return;
    }
    if (a < 0.003) {
        discard;
    }
    vec3 rgb = mix(vertexColor.rgb * min(tex.rgb * 1.3, vec3(1.25)), HazeColor.rgb, HazeColor.a);
    fragColor = vec4(rgb * a, a);
}

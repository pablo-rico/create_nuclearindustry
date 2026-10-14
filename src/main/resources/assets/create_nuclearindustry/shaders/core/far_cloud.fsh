#version 150

// Sampler0 is Distant Horizons' depth. With Occlude on, a puff behind its distant terrain does not show:
// the terrain's depth is turned back into a distance with the inverse of its projection, so this works
// whichever way its depth runs. EmptyDepth is the depth where it drew nothing.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform mat4 InverseProjMat;
uniform vec2 ScreenSize;
uniform float Occlude;
uniform float EmptyDepth;
uniform float DepthZeroToOne;

in vec4 vertexColor;
in vec3 viewPosition;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    if (color.a == 0.0) {
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
    fragColor = color * ColorModulator;
}

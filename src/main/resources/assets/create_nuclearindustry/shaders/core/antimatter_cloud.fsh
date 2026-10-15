#version 150

#moj_import <fog.glsl>

// The sprite's brightness is the cloud's density: it scales the vertex alpha, so an opaque grey
// sprite draws a soft-edged puff in the vertex colour. With translucent blending that tints what is
// behind (seen in daylight); with additive blending it glows.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float density = texture(Sampler0, texCoord0).r;
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * density) * ColorModulator;
    if (color.a < 0.004) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

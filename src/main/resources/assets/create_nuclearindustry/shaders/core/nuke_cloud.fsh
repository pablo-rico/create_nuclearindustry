#version 150

// One pass for a whole cloud, far to near, premultiplied alpha. Which part of the atlas a puff uses says how it is drawn:
//   smoke tiles (rows 0, 1 and the condensation row 3): the tile's grey (lit from above, its mean brought to about 1), times
//   the vertex colour (the light of
//   the sun, the sky and the cloud's heat, worked out on the CPU), blended over what is behind;
//   fire tiles (row 2): the tile's red is the gas's temperature, its white core hottest; hot parts glow in the vertex colour
//   (a black body's), cool lanes are soot;
//   the lightning tile (row 3, last): light added on, covering nothing.
// A UV moved right by 2 is the far impostor's texture, drawn as smoke. HazeColor draws the whole cloud towards the air's colour
// by its alpha, for a cloud seen through kilometres of it.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 HazeColor;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord0;
    bool impostor = uv.x > 1.5;
    if (impostor) {
        uv.x -= 2.0;
    }
    vec4 tex = texture(Sampler0, uv);
    float a = vertexColor.a * tex.a * ColorModulator.a;
    if (!impostor && uv.y >= 0.5 && uv.y < 0.75) {
        float t = tex.r;
        float hot = smoothstep(0.12, 0.55, t);
        vec3 emit = vertexColor.rgb * (0.35 + 1.15 * pow(t, 1.4));
        vec3 soot = vec3(0.09, 0.085, 0.08) * (0.6 + 0.8 * t);
        vec3 rgb = mix(soot, emit, hot) * (1.0 - 0.7 * HazeColor.a);
        if (a < 0.003) {
            discard;
        }
        fragColor = vec4(rgb * a, a * (0.55 + 0.45 * (1.0 - hot)));
        return;
    }
    if (!impostor && uv.x >= 0.75 && uv.y >= 0.75) {
        float l = tex.a * max(tex.r, max(tex.g, tex.b));
        vec3 light = vertexColor.rgb * l * vertexColor.a * 1.6 * (1.0 - 0.7 * HazeColor.a);
        if (max(light.r, max(light.g, light.b)) < 0.003) {
            discard;
        }
        fragColor = vec4(light, 0.0);
        return;
    }
    if (a < 0.003) {
        discard;
    }
    vec3 rgb = mix(vertexColor.rgb * min(tex.rgb * 1.3, vec3(1.25)) * ColorModulator.rgb, HazeColor.rgb, HazeColor.a);
    fragColor = vec4(rgb * a, a);
}

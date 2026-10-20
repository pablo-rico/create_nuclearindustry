#version 150

// The rocket effects' quads: camera-relative positions, already pulled inside the far plane where they lie beyond it, so no
// fog here (the effects tint themselves for haze). UV0 says where in its shape a fragment is (see the fragment shaders).

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}

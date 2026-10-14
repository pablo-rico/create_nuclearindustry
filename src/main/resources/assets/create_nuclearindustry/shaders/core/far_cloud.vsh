#version 150

// A nuclear cloud's far puffs, drawn in Distant Horizons' pass with its projection.

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec3 viewPosition;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;

    vertexColor = Color;
    viewPosition = view.xyz;
}

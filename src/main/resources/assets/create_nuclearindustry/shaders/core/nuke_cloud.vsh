#version 150

// A mushroom cloud's puffs and a conventional blast's smoke: billboards on the cloud atlas, coloured (and already lit) by vertex.
// No fog here: the cloud is seen far beyond the render distance, and the haze it takes is set for the whole draw (HazeColor).

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

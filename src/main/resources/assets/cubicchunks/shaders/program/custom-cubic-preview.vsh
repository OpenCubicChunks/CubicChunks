#version 120

uniform mat4 ProjMat;

varying vec2 texCoord;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix*vec4(gl_Vertex.xyz, 1);
    texCoord = gl_MultiTexCoord0.xy;
}

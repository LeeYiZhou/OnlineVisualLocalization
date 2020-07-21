precision mediump float;

attribute vec4 a_Position;
uniform mat4 u_MVP;
// uniform float u_PointThickness;
attribute float u_PointThickness;

varying vec4 vColor;
attribute vec4 aColor;

/*
Dmitry Brant, 2017
*/
void main() {
    gl_Position = u_MVP * a_Position;
    gl_PointSize = u_PointThickness;
    vColor=aColor;
}

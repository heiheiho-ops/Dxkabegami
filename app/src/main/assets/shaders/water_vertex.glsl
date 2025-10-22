uniform mat4 u_MVPMatrix;
attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;

uniform float u_Time;
uniform vec2 u_TouchPos;

varying vec2 v_TexCoordinate;
varying float v_Dist;

void main() {
    v_TexCoordinate = a_TexCoordinate;
    vec4 position = a_Position;

    // Gentle wave simulation
    position.y += 0.1 * sin(position.x * 10.0 + u_Time * 2.0);
    position.y += 0.05 * sin(position.z * 15.0 + u_Time * 1.5);

    // Ripple simulation from touch
    float dist = distance(position.xz, u_TouchPos);
    v_Dist = dist;
    float ripple = 0.0;
    if (dist < 2.0) {
        ripple = 0.2 * sin(dist * 20.0 - u_Time * 10.0) / (dist + 1.0);
    }
    position.y += ripple;

    gl_Position = u_MVPMatrix * position;
}

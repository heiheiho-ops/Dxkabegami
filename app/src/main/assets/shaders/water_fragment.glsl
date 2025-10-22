precision mediump float;

uniform sampler2D u_Texture;
varying vec2 v_TexCoordinate;
varying float v_Dist;

void main() {
    // Base water color
    vec4 waterColor = vec4(0.1, 0.3, 0.8, 1.0); // Deep blue

    // Add a highlight for the ripple
    float highlight = 0.0;
    if (v_Dist < 2.0) {
        highlight = 0.2 * (1.0 - v_Dist / 2.0);
    }
    waterColor.rgb += highlight;

    gl_FragColor = waterColor;
}

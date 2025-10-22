package com.example.kakoyoikabegami;

import android.content.Context;
import android.opengl.GLES20;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderUtils {

    public static String readShader(Context context, String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = context.getAssets().open("shaders/" + filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static int createProgram(String vertexShader, String fragmentShader) {
        int program = GLES20.glCreateProgram();
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        return program;
    }
}

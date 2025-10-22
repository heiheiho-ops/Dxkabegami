package com.example.kakoyoikabegami;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Shark {

    private final FloatBuffer vertexBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mMVPMatrixHandle;

    // Number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float sharkCoords[] = { // in counterclockwise order:
            // A simple triangular prism shape
            // Top
            0.0f,  0.2f, 0.0f,
            -0.5f, -0.2f, 0.0f,
            0.5f, -0.2f, 0.0f,
            // Front
            0.0f, 0.2f, 0.0f,
            0.5f, -0.2f, 0.0f,
            0.0f, 0.0f, 0.5f,
            // Back
            0.0f, 0.2f, 0.0f,
            -0.5f, -0.2f, 0.0f,
            0.0f, 0.0f, 0.5f,
    };

    private final int vertexCount = sharkCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    public Shark(Context context) {
        ByteBuffer bb = ByteBuffer.allocateDirect(sharkCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(sharkCoords);
        vertexBuffer.position(0);

        String vertexShaderCode = ShaderUtils.readShader(context, "shark_vertex.glsl");
        String fragmentShaderCode = ShaderUtils.readShader(context, "shark_fragment.glsl");
        mProgram = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}

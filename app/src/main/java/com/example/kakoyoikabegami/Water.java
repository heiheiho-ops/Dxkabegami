package com.example.kakoyoikabegami;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Water {

    private final FloatBuffer vertexBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mMVPMatrixHandle;
    private int mTimeHandle;
    private int mTouchPosHandle;

    static final int COORDS_PER_VERTEX = 3;
    private final int vertexCount;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    public Water(Context context, int gridSize) {
        float[] waterCoords = createGrid(gridSize);
        vertexCount = waterCoords.length / COORDS_PER_VERTEX;

        ByteBuffer bb = ByteBuffer.allocateDirect(waterCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(waterCoords);
        vertexBuffer.position(0);

        String vertexShaderCode = ShaderUtils.readShader(context, "water_vertex.glsl");
        String fragmentShaderCode = ShaderUtils.readShader(context, "water_fragment.glsl");
        mProgram = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);
    }

    private float[] createGrid(int size) {
        int numVertices = size * size * 6 * COORDS_PER_VERTEX;
        float[] vertices = new float[numVertices];
        int offset = 0;
        float halfSize = size / 2.0f;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                float x1 = (i - halfSize) / halfSize;
                float z1 = (j - halfSize) / halfSize;
                float x2 = ((i + 1) - halfSize) / halfSize;
                float z2 = ((j + 1) - halfSize) / halfSize;

                // Triangle 1
                vertices[offset++] = x1; vertices[offset++] = 0; vertices[offset++] = z1;
                vertices[offset++] = x2; vertices[offset++] = 0; vertices[offset++] = z1;
                vertices[offset++] = x1; vertices[offset++] = 0; vertices[offset++] = z2;
                // Triangle 2
                vertices[offset++] = x1; vertices[offset++] = 0; vertices[offset++] = z2;
                vertices[offset++] = x2; vertices[offset++] = 0; vertices[offset++] = z1;
                vertices[offset++] = x2; vertices[offset++] = 0; vertices[offset++] = z2;
            }
        }
        return vertices;
    }

    public void draw(float[] mvpMatrix, float time, float touchX, float touchY) {
        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        mTimeHandle = GLES20.glGetUniformLocation(mProgram, "u_Time");
        GLES20.glUniform1f(mTimeHandle, time);

        mTouchPosHandle = GLES20.glGetUniformLocation(mProgram, "u_TouchPos");
        GLES20.glUniform2f(mTouchPosHandle, touchX, touchY);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}

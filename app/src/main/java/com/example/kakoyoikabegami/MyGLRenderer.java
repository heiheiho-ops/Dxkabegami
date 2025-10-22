package com.example.kakoyoikabegami;

import android.content.Context;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private Water mWater;
    private Shark mShark;
    private Context mContext;

    private final float[] mViewMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];

    private float touchX = -100.0f; // Initialize off-screen
    private float touchY = -100.0f;

    public MyGLRenderer(Context context) {
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.5f, 1.0f); // Dark blue
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        mWater = new Water(mContext, 50); // 50x50 grid
        mShark = new Shark(mContext);
        startTime = System.currentTimeMillis();
    }

    private final float[] mModelMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];
    private long startTime;

    @Override
    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 3, 4, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the time elapsed
        long now = System.currentTimeMillis();
        float time = (now - startTime) / 1000.0f;

        // Draw the water
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.scaleM(mModelMatrix, 0, 2.0f, 1.0f, 2.0f); // Make water larger
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        mWater.draw(mMVPMatrix, time, touchX, touchY);

        // Draw the shark
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -0.5f, 0.0f); // Position below water
        Matrix.rotateM(mModelMatrix, 0, time * 10.0f, 0.0f, 1.0f, 0.0f); // Slow rotation
        Matrix.scaleM(mModelMatrix, 0, 0.5f, 0.5f, 0.5f);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        mShark.draw(mMVPMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    public void handleTouch(float normalizedX, float normalizedY) {
        // In our 3D world, the water surface is on the XZ plane.
        // We will map the normalized screen coordinates to this plane.
        // For now, let's just store them.
        this.touchX = normalizedX;
        this.touchY = normalizedY; // This will actually be mapped to Z in the shader
    }
}

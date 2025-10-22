package com.example.kakoyoikabegami;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import javax.microedition.khronos.opengles.GL10;

public class MyWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    public class GLEngine extends Engine {
        private WallpaperRenderThread mRenderThread;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            mRenderThread = new WallpaperRenderThread(getApplicationContext(), getSurfaceHolder());
            mRenderThread.start();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mRenderThread.onSurfaceChanged(width, height);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mRenderThread.requestExitAndWait();
            mRenderThread = null;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                mRenderThread.onResume();
            } else {
                mRenderThread.onPause();
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            mRenderThread.onTouchEvent(event);
        }
    }

    class WallpaperRenderThread extends Thread {
        private final SurfaceHolder mSurfaceHolder;
        private final Context mContext;
        private MyGLRenderer mRenderer;
        private boolean mRunning;
        private boolean mPaused;

        private EGLDisplay mEglDisplay;
        private EGLContext mEglContext;
        private EGLSurface mEglSurface;
        private int mWidth;
        private int mHeight;

        public WallpaperRenderThread(Context context, SurfaceHolder surfaceHolder) {
            mContext = context;
            mSurfaceHolder = surfaceHolder;
            mRenderer = new MyGLRenderer(context);
        }

        @Override
        public void run() {
            initEGL();
            mRunning = true;
            mPaused = false;
            mRenderer.onSurfaceCreated(null, null); // GL10 and EGLConfig are not used in GLES20

            while (mRunning) {
                synchronized (this) {
                    while (mPaused) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                mRenderer.onDrawFrame(null); // GL10 is not used
                EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
            }

            deinitEGL();
        }

        public void onSurfaceChanged(int width, int height) {
            mWidth = width;
            mHeight = height;
            mRenderer.onSurfaceChanged(null, width, height);
        }

        public void onTouchEvent(MotionEvent event) {
             if (event != null && event.getAction() == MotionEvent.ACTION_DOWN) {
                float normalizedX = (event.getX() / mWidth) * 2 - 1;
                float normalizedY = -(event.getY() / mHeight) * 2 + 1;
                mRenderer.handleTouch(normalizedX, normalizedY);
            }
        }

        public void onResume() {
            synchronized (this) {
                mPaused = false;
                notifyAll();
            }
        }

        public void onPause() {
            synchronized (this) {
                mPaused = true;
            }
        }

        public void requestExitAndWait() {
            mRunning = false;
            onResume(); // Wake up the thread to let it exit
            try {
                join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void initEGL() {
            mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            EGL14.eglInitialize(mEglDisplay, version, 0, version, 1);

            int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            EGL14.eglChooseConfig(mEglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0);
            EGLConfig eglConfig = configs[0];

            int[] contextAttribs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            };
            mEglContext = EGL14.eglCreateContext(mEglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0);

            int[] surfaceAttribs = {
                EGL14.EGL_NONE
            };
            mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, eglConfig, mSurfaceHolder, surfaceAttribs, 0);

            EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
        }

        private void deinitEGL() {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
            EGL14.eglDestroyContext(mEglDisplay, mEglContext);
            EGL14.eglTerminate(mEglDisplay);
        }
    }
}

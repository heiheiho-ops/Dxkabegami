package com.example.dxkabegami;

import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.content.Context;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MyWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    public class GLEngine extends WallpaperService.Engine {

        private WallpaperGLSurfaceView glSurfaceView;
        private MyRenderer renderer;
        private boolean rendererSet;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            glSurfaceView = new WallpaperGLSurfaceView(MyWallpaperService.this);
            glSurfaceView.setEGLContextClientVersion(2);
            renderer = new MyRenderer();
            glSurfaceView.setRenderer(renderer);
            rendererSet = true;
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if(renderer != null) {
                    renderer.addTouch(event.getX(), event.getY());
                }
            }
            super.onTouchEvent(event);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (rendererSet) {
                if (visible) {
                    glSurfaceView.onResume();
                } else {
                    glSurfaceView.onPause();
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (glSurfaceView != null) {
                glSurfaceView.onDestroy();
            }
        }

        class WallpaperGLSurfaceView extends GLSurfaceView {
            WallpaperGLSurfaceView(Context context) { super(context); }
            @Override
            public SurfaceHolder getHolder() { return getSurfaceHolder(); }
            public void onDestroy() { super.onDetachedFromWindow(); }
        }
    }

    class MyRenderer implements GLSurfaceView.Renderer {

        private static final int MAX_RIPPLES = 10;
        private final List<Ripple> ripples = new CopyOnWriteArrayList<>();
        private FloatBuffer waterVertexBuffer, sharkVertexBuffer, sharkNormalBuffer;
        private ShortBuffer waterDrawListBuffer, sharkDrawListBuffer;
        private int waterProgram, sharkProgram;
        private int waterPositionHandle, waterTimeHandle, resolutionHandle, ripplesHandle, rippleCountHandle;
        private int sharkPositionHandle, sharkNormalHandle, sharkMvpMatrixHandle, sharkTimeHandle;

        private final float[] mvpMatrix = new float[16], projectionMatrix = new float[16], viewMatrix = new float[16], modelMatrix = new float[16];
        private long startTime;
        private int screenWidth, screenHeight;
        private Shark shark;

        private final String waterVertexShader = "attribute vec4 a_Position; void main() { gl_Position = a_Position; }";
        private final String waterFragmentShader =
            "precision mediump float; uniform float u_Time; uniform vec2 u_Resolution; uniform vec3 u_Ripples[10]; uniform int u_RippleCount;" +
            "void main() {" +
            "    vec2 uv = gl_FragCoord.xy / u_Resolution.xy; float time = u_Time * 0.3;" +
            "    float displacement = sin(uv.x * 20.0 + time * 2.0) * 0.02 + sin(uv.y * 25.0 + time * 1.5) * 0.02;" +
            "    float rippleEffect = 0.0;" +
            "    for (int i = 0; i < 10; i++) { if (i < u_RippleCount) {" +
            "        vec2 rippleCenter = u_Ripples[i].xy; float rippleStartTime = u_Ripples[i].z;" +
            "        float dist = distance(uv, rippleCenter); float rippleTime = u_Time - rippleStartTime;" +
            "        if (rippleTime > 0.0 && rippleTime < 2.0) {" +
            "            float wave = smoothstep(0.0, 1.0, rippleTime) * (1.0 - smoothstep(1.5, 2.0, rippleTime));" +
            "            rippleEffect += sin((dist * 50.0) - (rippleTime * 10.0)) * 0.05 * wave;" +
            "        }}}" +
            "    displacement += rippleEffect;" +
            "    vec3 waterColor = vec3(0.1, 0.5, 0.8);" +
            "    gl_FragColor = vec4(waterColor + vec3(displacement * 2.0), 1.0);" +
            "}";

        private final String sharkVertexShader =
            "uniform mat4 u_MVPMatrix; attribute vec4 a_Position; attribute vec3 a_Normal; varying vec3 v_Normal;" +
            "void main() { gl_Position = u_MVPMatrix * a_Position; v_Normal = a_Normal; }";
        private final String sharkFragmentShader =
            "precision mediump float; varying vec3 v_Normal; uniform float u_Time;" +
            "void main() { vec3 lightDir = normalize(vec3(sin(u_Time * 0.5), 0.5, 1.0));" +
            "    float diffuse = max(dot(normalize(v_Normal), lightDir), 0.2);" +
            "    gl_FragColor = vec4(vec3(0.4, 0.4, 0.5) * diffuse, 1.0); }";

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            setupWater();
            setupShark();
            startTime = System.currentTimeMillis();
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            screenWidth = width; screenHeight = height;
            float ratio = (float) width / height;
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        }

        @Override
        public void onDrawFrame(GL10 unused) {
            float elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0f;
            updateRipples(elapsedTime);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            drawWater(elapsedTime);
            drawShark(elapsedTime);
        }

        private void setupWater() {
            // ... (setupWater code is unchanged)
            waterProgram = createProgram(waterVertexShader, waterFragmentShader);
            waterPositionHandle = GLES20.glGetAttribLocation(waterProgram, "a_Position");
            waterTimeHandle = GLES20.glGetUniformLocation(waterProgram, "u_Time");
            resolutionHandle = GLES20.glGetUniformLocation(waterProgram, "u_Resolution");
            ripplesHandle = GLES20.glGetUniformLocation(waterProgram, "u_Ripples");
            rippleCountHandle = GLES20.glGetUniformLocation(waterProgram, "u_RippleCount");
        }

        private void setupShark() {
            shark = new Shark();
            sharkVertexBuffer = createFloatBuffer(shark.getVertices());
            sharkNormalBuffer = createFloatBuffer(shark.getNormals());
            sharkDrawListBuffer = createShortBuffer(shark.getDrawOrder());
            sharkProgram = createProgram(sharkVertexShader, sharkFragmentShader);
            sharkPositionHandle = GLES20.glGetAttribLocation(sharkProgram, "a_Position");
            sharkNormalHandle = GLES20.glGetAttribLocation(sharkProgram, "a_Normal");
            sharkMvpMatrixHandle = GLES20.glGetUniformLocation(sharkProgram, "u_MVPMatrix");
            sharkTimeHandle = GLES20.glGetUniformLocation(sharkProgram, "u_Time");
        }

        private void drawWater(float elapsedTime) {
            GLES20.glUseProgram(waterProgram);
            GLES20.glUniform1f(waterTimeHandle, elapsedTime);
            GLES20.glUniform2f(resolutionHandle, (float) screenWidth, (float) screenHeight);
            int rippleCount = Math.min(ripples.size(), MAX_RIPPLES);
            float[] rippleData = new float[MAX_RIPPLES * 3];
            for(int i = 0; i < rippleCount; i++) {
                Ripple r = ripples.get(i);
                rippleData[i*3] = r.x; rippleData[i*3+1] = r.y; rippleData[i*3+2] = r.startTime;
            }
            GLES20.glUniform3fv(ripplesHandle, MAX_RIPPLES, rippleData, 0);
            GLES20.glUniform1i(rippleCountHandle, rippleCount);

            GLES20.glEnableVertexAttribArray(waterPositionHandle);
            GLES20.glVertexAttribPointer(waterPositionHandle, 2, GLES20.GL_FLOAT, false, 8, waterVertexBuffer);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, waterDrawListBuffer);
            GLES20.glDisableVertexAttribArray(waterPositionHandle);
        }

        private void drawShark(float elapsedTime) {
            GLES20.glUseProgram(sharkProgram);

            Matrix.setLookAtM(viewMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

            Matrix.setIdentityM(modelMatrix, 0);
            float sharkX = (float)Math.sin(elapsedTime * 0.5) * 2.0f;
            float sharkY = (float)Math.sin(elapsedTime * 0.7) * 0.5f - 0.5f;
            Matrix.translateM(modelMatrix, 0, sharkX, sharkY, 0);
            Matrix.rotateM(modelMatrix, 0, (float)Math.cos(elapsedTime * 0.5) * 15.0f, 0, 1, 0);

            float[] tempMatrix = new float[16];
            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

            GLES20.glUniformMatrix4fv(sharkMvpMatrixHandle, 1, false, mvpMatrix, 0);
            GLES20.glUniform1f(sharkTimeHandle, elapsedTime);

            GLES20.glEnableVertexAttribArray(sharkPositionHandle);
            GLES20.glVertexAttribPointer(sharkPositionHandle, 3, GLES20.GL_FLOAT, false, 12, sharkVertexBuffer);
            GLES20.glEnableVertexAttribArray(sharkNormalHandle);
            GLES20.glVertexAttribPointer(sharkNormalHandle, 3, GLES20.GL_FLOAT, false, 12, sharkNormalBuffer);

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, shark.getDrawOrder().length, GLES20.GL_UNSIGNED_SHORT, sharkDrawListBuffer);

            GLES20.glDisableVertexAttribArray(sharkPositionHandle);
            GLES20.glDisableVertexAttribArray(sharkNormalHandle);
        }

        // ... (rest of the helper methods are unchanged)
        private void updateRipples(float elapsedTime) {
            List<Ripple> toRemove = new ArrayList<>();
            for(Ripple ripple : ripples) {
                if(elapsedTime - ripple.startTime > 2.0f) toRemove.add(ripple);
            }
            ripples.removeAll(toRemove);
        }

        public void addTouch(float x, float y) {
            if (ripples.size() < MAX_RIPPLES) {
                float normalizedX = x / screenWidth, normalizedY = 1.0f - (y / screenHeight);
                ripples.add(new Ripple(normalizedX, normalizedY, (System.currentTimeMillis() - startTime) / 1000.0f));
            }
        }

        private int loadShader(int type, String shaderCode){
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        private int createProgram(String vertexShaderCode, String fragmentShaderCode) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            return program;
        }

        private FloatBuffer createFloatBuffer(float[] data) {
            ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer buffer = bb.asFloatBuffer();
            buffer.put(data);
            buffer.position(0);
            return buffer;
        }

        private ShortBuffer createShortBuffer(short[] data) {
            ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 2);
            bb.order(ByteOrder.nativeOrder());
            ShortBuffer buffer = bb.asShortBuffer();
            buffer.put(data);
            buffer.position(0);
            return buffer;
        }

        private class Ripple {
            final float x, y, startTime;
            Ripple(float x, float y, float startTime) { this.x = x; this.y = y; this.startTime = startTime; }
        }

        private class Shark {
            private final float[] vertices = { /* basic shark-like shape */
                0.0f, 0.5f, 0.0f,   -0.5f, -0.5f, 0.5f,  0.5f, -0.5f, 0.5f,
                0.0f, 0.5f, 0.0f,   0.5f, -0.5f, 0.5f,   0.5f, -0.5f, -0.5f,
                0.0f, 0.5f, 0.0f,   0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,
                0.0f, 0.5f, 0.0f,   -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f,
                -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f,   -0.5f, -0.5f, 0.5f,
            };
            private final float[] normals = { /* simplified normals */
                0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,
                1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
                -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
                0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
            };
            private final short[] drawOrder = {
                0, 1, 2,  3, 4, 5,  6, 7, 8,  9, 10, 11, 12, 13, 14, 15, 16, 17
            };

            public float[] getVertices() { return vertices; }
            public float[] getNormals() { return normals; }
            public short[] getDrawOrder() { return drawOrder; }
        }
    }
}

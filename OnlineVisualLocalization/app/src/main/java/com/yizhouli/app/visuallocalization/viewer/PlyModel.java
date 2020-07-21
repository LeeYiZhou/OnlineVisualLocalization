package com.yizhouli.app.visuallocalization.viewer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yizhouli.app.visuallocalization.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.yizhouli.app.visuallocalization.viewer.Util.readIntLe;

public class PlyModel extends IndexedModel {

    @Nullable
    protected FloatBuffer colorBuffer;

    @Nullable
    protected FloatBuffer pointSizeBuffer;

    protected String cameraLoc = null;

    private String TAG = "PLYMODEL";

    public PlyModel(@NonNull InputStream inputStream) throws IOException {
        super();
        BufferedInputStream stream = new BufferedInputStream(inputStream, INPUT_BUFFER_SIZE);
        readText(stream);
        if (vertexCount <= 0 || vertexBuffer == null) {
            throw new IOException("Invalid model.");
        }
    }

    public PlyModel(@NonNull InputStream inputStream, String cameraLocInput) throws IOException {
        super();
        BufferedInputStream stream = new BufferedInputStream(inputStream, INPUT_BUFFER_SIZE);
        cameraLoc = cameraLocInput;
        readText(stream);
        if (vertexCount <= 0 || vertexBuffer == null) {
            throw new IOException("Invalid model.");
        }
        Log.e("PLYMODEL1", cameraLoc);
    }

    @Override
    public void init(float boundSize) {
        if (GLES20.glIsProgram(glProgram)) {
            GLES20.glDeleteProgram(glProgram);
            glProgram = -1;
        }
        glProgram = Util.compileProgram(R.raw.point_cloud_vertex, R.raw.single_color_fragment,
                new String[] {"a_Position"});
        initModelMatrix(boundSize);
    }

    @Override
    public void initModelMatrix(float boundSize) {
        final float yRotation = 180f;
        initModelMatrix(boundSize, 0.0f, yRotation, 0.0f);
        float scale = getBoundScale(boundSize);
        if (scale == 0.0f) { scale = 1.0f; }
        floorOffset = (minY - centerMassY) / scale;
    }

    private void readText(@NonNull BufferedInputStream stream) throws IOException {
        List<Float> vertices = new ArrayList<>();
        List<Float> colors = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream), INPUT_BUFFER_SIZE);
        String line;
        String[] lineArr;

        stream.mark(0x100000);
        boolean isBinary = false;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("format ")) {
                if (line.contains("binary")) {
                    isBinary = true;
                }
            } else if (line.startsWith("element vertex")) {
                lineArr = line.split(" ");
                vertexCount = Integer.parseInt(lineArr[2]);
            } else if (line.startsWith("end_header")) {
                break;
            }
        }

        if (vertexCount <= 0) {
            return;
        }

        if (isBinary) {
            stream.reset();
            readVerticesBinary(vertices, stream);
        } else {
            readVerticesText(vertices, colors, reader);
        }
        float[] floatArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            floatArray[i] = vertices.get(i);
        }

        float[] colorFloatArray = new float[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            colorFloatArray[i] = colors.get(i);
        }

        float[] pointSizeArray = new float[vertices.size() / 3];
        for (int i = 0; i < pointSizeArray.length; i++) {
            if (i != pointSizeArray.length - 1) {
                pointSizeArray[i] = (float) 3;
            }
            else {
                if (!TextUtils.isEmpty(cameraLoc)){
                    pointSizeArray[i] = (float) 50;
                }
                else{
                    pointSizeArray[i] = (float) 3;
                }
            }
        }

        ByteBuffer vbb = ByteBuffer.allocateDirect(floatArray.length * BYTES_PER_FLOAT);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(floatArray);
        vertexBuffer.position(0);

        ByteBuffer cdd = ByteBuffer.allocateDirect(colorFloatArray.length * BYTES_PER_FLOAT);
        cdd.order(ByteOrder.nativeOrder());
        colorBuffer = cdd.asFloatBuffer();
        colorBuffer.put(colorFloatArray);
        colorBuffer.position(0);

        ByteBuffer sdd = ByteBuffer.allocateDirect(pointSizeArray.length * BYTES_PER_FLOAT);
        sdd.order(ByteOrder.nativeOrder());
        pointSizeBuffer = sdd.asFloatBuffer();
        pointSizeBuffer.put(pointSizeArray);
        pointSizeBuffer.position(0);

    }

    private void readVerticesText(List<Float> vertices, List<Float> colors, BufferedReader reader) throws IOException {
        String[] lineArr;
        float x, y, z;
        float r, g, b, alpha;

        double centerMassX = 0.0;
        double centerMassY = 0.0;
        double centerMassZ = 0.0;

        for (int i = 0; i < vertexCount; i++) {
            lineArr = reader.readLine().split("m");
            Log.e(TAG, Arrays.toString(lineArr));
            if (lineArr.length < 10) {
                continue;
            }
            x = Float.parseFloat(lineArr[0]);
            y = Float.parseFloat(lineArr[1]);
            z = Float.parseFloat(lineArr[2]);
            vertices.add(x);
            vertices.add(y);
            vertices.add(z);

            r = Float.parseFloat(lineArr[6]);
            g = Float.parseFloat(lineArr[7]);
            b = Float.parseFloat(lineArr[8]);
            // alpha = Float.parseFloat(lineArr[9]);
            colors.add((float) (r / 255.0));
            colors.add((float) (g / 255.0));
            colors.add((float) (b / 255.0));
            colors.add((float) (255.0 / 255.0));

            adjustMaxMin(x, y, z);
            centerMassX += x;
            centerMassY += y;
            centerMassZ += z;
        }

        Log.e("PLYMODEL2: ", String.valueOf(TextUtils.isEmpty(cameraLoc)));
        if (!TextUtils.isEmpty(cameraLoc)) {
            String newLoc = cameraLoc.substring(1, cameraLoc.length()-1);
            Log.e("PLYMODEL: ", newLoc);
            String[] xyz = newLoc.trim().split(" ");
            Log.e("PLYMODEL: ", Arrays.toString(xyz));
            x = Float.parseFloat(xyz[0]);
            y = Float.parseFloat(xyz[1]);
            z = Float.parseFloat(xyz[2]);

            vertices.add(x);
            vertices.add(y);
            vertices.add(z);

            colors.add((float) (255.0 / 255.0));
            colors.add((float) (0.0 / 255.0));
            colors.add((float) (0.0 / 255.0));
            colors.add((float) (255.0 / 255.0));

            vertexCount += 1;
        }

        this.centerMassX = (float)(centerMassX / vertexCount);
        this.centerMassY = (float)(centerMassY / vertexCount);
        this.centerMassZ = (float)(centerMassZ / vertexCount);
    }

    private void readVerticesBinary(List<Float> vertices, BufferedInputStream stream) throws IOException {
        byte[] tempBytes = new byte[0x1000];
        stream.mark(1);
        stream.read(tempBytes);
        String tempStr = new String(tempBytes);
        int contentsPos = tempStr.indexOf("end_header") + 11;
        stream.reset();
        stream.skip(contentsPos);

        float x, y, z;

        double centerMassX = 0.0;
        double centerMassY = 0.0;
        double centerMassZ = 0.0;

        for (int i = 0; i < vertexCount; i++) {
            stream.read(tempBytes, 0, BYTES_PER_FLOAT * 3);
            x = Float.intBitsToFloat(readIntLe(tempBytes, 0));
            y = Float.intBitsToFloat(readIntLe(tempBytes, BYTES_PER_FLOAT));
            z = Float.intBitsToFloat(readIntLe(tempBytes, BYTES_PER_FLOAT * 2));
            vertices.add(x);
            vertices.add(y);
            vertices.add(z);

            adjustMaxMin(x, y, z);
            centerMassX += x;
            centerMassY += y;
            centerMassZ += z;
        }

        this.centerMassX = (float)(centerMassX / vertexCount);
        this.centerMassY = (float)(centerMassY / vertexCount);
        this.centerMassZ = (float)(centerMassZ / vertexCount);
    }

    @Override
    public void draw(float[] viewMatrix, float[] projectionMatrix, @NonNull Light light) {
        if (vertexBuffer == null) {
            return;
        }
        GLES20.glUseProgram(glProgram);

        int mvpMatrixHandle = GLES20.glGetUniformLocation(glProgram, "u_MVP");
        int positionHandle = GLES20.glGetAttribLocation(glProgram, "a_Position");
        // int pointThicknessHandle = GLES20.glGetUniformLocation(glProgram, "u_PointThickness");
        int pointThicknessHandle = GLES20.glGetAttribLocation(glProgram, "u_PointThickness");
        // int ambientColorHandle = GLES20.glGetUniformLocation(glProgram, "u_ambientColor");

        GLES20.glEnableVertexAttribArray(pointThicknessHandle);
        GLES20.glVertexAttribPointer(pointThicknessHandle,1,
                GLES20.GL_FLOAT,false,
                0, pointSizeBuffer);

        int mColorHandle = GLES20.glGetUniformLocation(glProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, colorBuffer);

        mColorHandle = GLES20.glGetAttribLocation(glProgram, "aColor");
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glVertexAttribPointer(mColorHandle,4,
                GLES20.GL_FLOAT,false,
                0,colorBuffer);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, vertexBuffer);

        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // GLES20.glUniform1fv(pointThicknessHandle, 1, pointSizeBuffer);
        // GLES20.glUniform1f(pointThicknessHandle, 3.0f);
        // GLES20.glUniform3fv(ambientColorHandle, 1, pointColor, 0);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}

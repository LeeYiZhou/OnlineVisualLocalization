package com.yizhouli.app.visuallocalization.mainlogics;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yizhouli.app.visuallocalization.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// This is a deprecated main activity. Please see MainActivityWithFrag and MainActivity3D
public class MainActivityOld extends AppCompatActivity {

    private static final String TAG = MainActivityOld.class.getSimpleName();
    private static final int REQUEST_IMAGE_CAPTURE = 3569;
    private static final int REQUEST_GALLERY_IMAGE = 5884;
    private static final int REQUEST_DOWNLOAD_PLY = 6669;
    private static final int REQUEST_START_SFM = 6668;
    private static final int REQUEST_VISUAL_LOCALIZATION = 6667;

    private final String URL_UPLOAD = "http://10.0.2.2:5000/upload_image";
    private final String URL_DOWNLOAD_PLY = "http://10.0.2.2:5000/download_ply";
    private final String URL_START_SFM = "http://10.0.2.2:5000/start_sfm";
    private final String URL_VISUAL_LOC = "http://10.0.2.2:5000/visual_localization";
    private TextView textTargetUri;
    private ImageView targetImage;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textTargetUri = (TextView)findViewById(R.id.targeturi);
        targetImage = (ImageView)findViewById(R.id.targetimage);
        pDialog = new ProgressDialog(this);
    }

    /**
     * User uploads image from gallery or takes a picture using the camera
     * */
//    public void uploadImage(View view) {
//        Log.d(TAG, "uploadImage: clicked");
//        openGallery();
////        openCamera();
//    }

    public void startSFM(View view) {
        StartSFMTask task = new StartSFMTask();
        task.execute();
    }

    public void downloadPLY(View view) {
        DownloadPLYTask task = new DownloadPLYTask();
        task.execute();
    }

    public void visualLoc(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_VISUAL_LOCALIZATION);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            assert extras != null;
            bitmap = (Bitmap) extras.get("data");
            targetImage.setImageBitmap(bitmap);

            SendImageTask task = new SendImageTask();
            task.execute(bitmap);
        }

        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK) {
            try {
                Uri targetUri = data.getData();
                assert targetUri != null;
                textTargetUri.setText(targetUri.toString());
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
                targetImage.setImageBitmap(bitmap);

                SendImageTask task = new SendImageTask();
                task.execute(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == REQUEST_VISUAL_LOCALIZATION && resultCode == RESULT_OK) {
            try {
                Uri targetUri = data.getData();
                assert targetUri != null;
                textTargetUri.setText(targetUri.toString());
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
                targetImage.setImageBitmap(bitmap);

                VisualLocTask task = new VisualLocTask();
                task.execute(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private class SendImageTask extends AsyncTask<Bitmap, Integer, String> {

        @Override
        protected String doInBackground(Bitmap... bitmap) {

            try {
                return initialiseImageUpload(bitmap[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            displayProgressDialog();
        }

        @Override
        protected void onPostExecute(String result) {
            dismissProgressDialog();
            Toast.makeText(MainActivityOld.this, "Uploaded image", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onPostExecute: " + result);
        }
    }

    private class StartSFMTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                return requestStartSFM();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            displayProgressDialog();
        }

        @Override
        protected void onPostExecute(String result) {
            dismissProgressDialog();
            Toast.makeText(MainActivityOld.this, "SFM complete", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onPostExecute: " + result);
        }
    }

    private class DownloadPLYTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {

            try {
                return downLoadPlyFromServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            displayProgressDialog();
        }

        @Override
        protected void onPostExecute(String result) {
            dismissProgressDialog();
            Toast.makeText(MainActivityOld.this, "Download PLY from server complete", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onPostExecute: " + result);
        }
    }

    private class VisualLocTask extends AsyncTask<Bitmap, Integer, String> {

        @Override
        protected String doInBackground(Bitmap... bitmap) {

            try {
                return initialiseVisualLoc(bitmap[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            displayProgressDialog();
        }

        @Override
        protected void onPostExecute(String result) {
            dismissProgressDialog();
            Toast.makeText(MainActivityOld.this, "Uploaded image complete", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onPostExecute: " + result);
        }
    }

    private String initialiseImageUpload(Bitmap bitmap) {
        try {
            if (bitmap != null) {
                // first convert bitmap because Okhttp only accepts files or URIs
                File file = convertBitmapToFile(bitmap);
                String response = sendImageToServer(file, "0756878434");
//                String response = sendImageToServer(uri, "0756878434");
                Log.d(TAG, "onActivityResult: RESPONSE " + response);
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String initialiseVisualLoc(Bitmap bitmap) {
        try {
            if (bitmap != null) {
                // first convert bitmap because Okhttp only accepts files or URIs
                File file = convertBitmapToFile(bitmap);
                String response = visualLocalization(file);
                Log.d(TAG, "onActivityResult: RESPONSE " + response);
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File convertBitmapToFile(@NonNull Bitmap bitmap) {
        try {
            // use getCacheDir to temporarily store the file
            File file = new File(getCacheDir(), "image.png");
            file.createNewFile();

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void displayProgressDialog() {
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();
    }

    private void dismissProgressDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    private String sendImageToServer(File file, String phoneNumber) throws IOException {
        Log.d(TAG, "sendImageToServer: started");

        try {
            MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
            String imageName = generateImageName();
            Log.d(TAG, "sendImageToServer: ImageName " + imageName);
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).writeTimeout(180, TimeUnit.SECONDS).readTimeout(180, TimeUnit.SECONDS).build();
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("theFile", imageName, RequestBody.create(MEDIA_TYPE_PNG, file))
                    .addFormDataPart("phoneNumber", phoneNumber)
                    .build();
            Request request = new Request.Builder().url(URL_UPLOAD).post(body).build();
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Network failure";
    }

    private String visualLocalization(File file) throws IOException {
        Log.d(TAG, "visualLocalization: started");

        try {
            MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
            String imageName = generateImageName();
            Log.d(TAG, "sendImageToServer: ImageName " + imageName);
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).writeTimeout(180, TimeUnit.SECONDS).readTimeout(180, TimeUnit.SECONDS).build();
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("theFile", imageName, RequestBody.create(MEDIA_TYPE_PNG, file))
                    .build();
            Request request = new Request.Builder().url(URL_VISUAL_LOC).post(body).build();
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Network failure";
    }

    private String requestStartSFM() throws IOException {
        Log.d(TAG, "requestStartSFM: started");

        try {
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).writeTimeout(180, TimeUnit.SECONDS).readTimeout(180, TimeUnit.SECONDS).build();
            Request request = new Request.Builder().url(URL_START_SFM).get().build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Network failure";
    }

    public String downLoadPlyFromServer() {

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //Check the permission
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //Apply for permission
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                }
            }
        }

        File extDir = Environment.getExternalStorageDirectory();
        final String fileName = "target.ply";
        final File file = new File(extDir, fileName);
        if (file.exists()) {
            return "File already exists";
        }
        OkHttpClient mOkHttpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).writeTimeout(180, TimeUnit.SECONDS).readTimeout(180, TimeUnit.SECONDS).build();

        final Request request = new Request.Builder().url(URL_DOWNLOAD_PLY).build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len;
                FileOutputStream fos = null;
                try {
                    assert response.body() != null;
                    long total = response.body().contentLength();
                    Log.e(TAG, "total------>" + total);
                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        Log.e(TAG, "current------>" + current);
                    }
                    fos.flush();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        });

        return "Download complete... See log for details (successful or not)";
    }

    @NonNull
    private String generateImageName() {
        String imageName = UUID.randomUUID().toString().substring(0, 5);
        return imageName + ".png";
    }
}

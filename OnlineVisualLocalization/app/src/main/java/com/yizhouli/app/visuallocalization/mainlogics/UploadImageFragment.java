package com.yizhouli.app.visuallocalization.mainlogics;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.yizhouli.app.visuallocalization.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UploadImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UploadImageFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private static final int REQUEST_GALLERY_IMAGE = 5884;
    private static final int REQUEST_IMAGE_CAPTURE = 3569;

    private final String URL_UPLOAD = "http://10.0.2.2:5000/upload_image";

    private ProgressDialog pDialog;

    private Button uploadButton;

    private GridView uploadedImageGridView;
    UploadedImageAdapter adapter;
    List<UploadedImageItem> imageData = new ArrayList<>();

    private static final String TAG = UploadImageFragment.class.getSimpleName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public UploadImageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment UploadImageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UploadImageFragment newInstance() {
        UploadImageFragment fragment = new UploadImageFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pDialog = new ProgressDialog(getContext());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_upload_image, container, false);
        uploadButton = view.findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage(v);
            }
        });

        uploadedImageGridView = view.findViewById(R.id.menu_grid);
        adapter = new UploadedImageAdapter(getActivity(), imageData);

        uploadedImageGridView.setAdapter(adapter);

        return view;
    }

    /**
     * User uploads image from gallery or takes a picture using the camera
     * */
    public void uploadImage(View view) {
        Log.d(TAG, "uploadImage: clicked");
        openGallery();
//        openCamera();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            assert extras != null;
            bitmap = (Bitmap) extras.get("data");
            // targetImage.setImageBitmap(bitmap);

            int currentImageNum = imageData.size();
            String imageTitle = Integer.toString(currentImageNum);
            UploadedImageItem item = new UploadedImageItem(imageTitle, bitmap);
            imageData.add(item);

            adapter.notifyDataSetChanged();

            SendImageTask task = new SendImageTask();
            task.execute(bitmap);
        }

        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK) {
            try {
                Uri targetUri = data.getData();
                assert targetUri != null;
                // textTargetUri.setText(targetUri.toString());
                bitmap = BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(targetUri));
                // targetImage.setImageBitmap(bitmap);
                int currentImageNum = imageData.size();
                String imageTitle = Integer.toString(currentImageNum) + ".png";
                UploadedImageItem item = new UploadedImageItem(imageTitle, bitmap);
                imageData.add(item);

                adapter.notifyDataSetChanged();

                SendImageTask task = new SendImageTask();
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
            Toast.makeText(getContext(), "Uploaded image", Toast.LENGTH_SHORT).show();
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

    private File convertBitmapToFile(@NonNull Bitmap bitmap) {
        try {
            // use getCacheDir to temporarily store the file
            File file = new File(getActivity().getCacheDir(), "image.png");
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

    @NonNull
    private String generateImageName() {
        String imageName = UUID.randomUUID().toString().substring(0, 5);
        return imageName + ".png";
    }
}
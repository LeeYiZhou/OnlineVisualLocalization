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
import android.widget.Toast;

import com.yizhouli.app.visuallocalization.R;
import com.yizhouli.app.visuallocalization.viewer.MainActivity3D;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * Use the {@link ModelPreviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ModelPreviewFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = ModelPreviewFragment.class.getSimpleName();
    private final String URL_VISUAL_LOC = "http://10.0.2.2:5000/visual_localization";
    private static final int REQUEST_VISUAL_LOCALIZATION = 6667;
    private Button previewButton;
    private Button visualLocButton;
    private ProgressDialog pDialog;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ModelPreviewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ModelPreviewFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ModelPreviewFragment newInstance() {
        ModelPreviewFragment fragment = new ModelPreviewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_model_preview, container, false);
        previewButton = view.findViewById(R.id.preview_button);
        previewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start3DPreview();
            }
        });

        visualLocButton = view.findViewById(R.id.visual_loc_button);
        visualLocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visualLoc(v);
            }
        });

        pDialog = new ProgressDialog(getContext());

        return view;
    }

    private void start3DPreview(){
        Intent intent = new Intent(getActivity(), MainActivity3D.class);
        startActivity(intent);
    }

    public void visualLoc(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_VISUAL_LOCALIZATION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (requestCode == REQUEST_VISUAL_LOCALIZATION && resultCode == RESULT_OK) {
            try {
                Uri targetUri = data.getData();
                assert targetUri != null;
                bitmap = BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(targetUri));

                VisualLocTask task = new VisualLocTask();
                task.execute(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            Toast.makeText(getContext(), "Uploaded image complete", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onPostExecute: " + result);

            Intent intent = new Intent(getContext(), MainActivity3D.class);
            intent.putExtra("camera_loc", result);
            startActivity(intent);
        }
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

    private void displayProgressDialog() {
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();
    }

    private void dismissProgressDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    @NonNull
    private String generateImageName() {
        String imageName = UUID.randomUUID().toString().substring(0, 5);
        return imageName + ".png";
    }
}
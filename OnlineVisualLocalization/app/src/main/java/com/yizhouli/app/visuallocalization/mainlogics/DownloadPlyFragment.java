package com.yizhouli.app.visuallocalization.mainlogics;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.yizhouli.app.visuallocalization.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DownloadPlyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DownloadPlyFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = DownloadPlyFragment.class.getSimpleName();

    private final String URL_DOWNLOAD_PLY = "http://10.0.2.2:5000/download_ply";
    private ProgressDialog pDialog;

    private Button downloadButton;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public DownloadPlyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment DownloadPlyFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DownloadPlyFragment newInstance() {
        DownloadPlyFragment fragment = new DownloadPlyFragment();
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
        View view = inflater.inflate(R.layout.fragment_download_ply, container, false);
        downloadButton = view.findViewById(R.id.download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadPLY(v);
            }
        });

        return view;
    }

    public void downloadPLY(View view) {
        DownloadPLYTask task = new DownloadPLYTask();
        task.execute();
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
            Toast.makeText(getContext(), "Download PLY from server complete", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onPostExecute: " + result);
        }
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

    public String downLoadPlyFromServer() {

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //Check the permission
            for (String str : permissions) {
                if (getActivity().checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
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
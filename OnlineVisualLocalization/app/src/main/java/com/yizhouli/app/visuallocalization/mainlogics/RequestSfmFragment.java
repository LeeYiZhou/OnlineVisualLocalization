package com.yizhouli.app.visuallocalization.mainlogics;

import android.app.ProgressDialog;
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

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RequestSfmFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RequestSfmFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private static final String TAG = RequestSfmFragment.class.getSimpleName();
    private final String URL_START_SFM = "http://10.0.2.2:5000/start_sfm";
    private ProgressDialog pDialog;

    private Button requestButtom;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RequestSfmFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment RequestSfmFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RequestSfmFragment newInstance() {
        RequestSfmFragment fragment = new RequestSfmFragment();
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
        View view = inflater.inflate(R.layout.fragment_request_sfm, container, false);
        requestButtom = view.findViewById(R.id.request_buttom);

        requestButtom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSFM(v);
            }
        });

        return view;
    }

    public void startSFM(View view) {
        StartSFMTask task = new StartSFMTask();
        task.execute();
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
            Toast.makeText(getContext(), "SFM complete", Toast.LENGTH_SHORT).show();
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

    @NonNull
    private String generateImageName() {
        String imageName = UUID.randomUUID().toString().substring(0, 5);
        return imageName + ".png";
    }
}
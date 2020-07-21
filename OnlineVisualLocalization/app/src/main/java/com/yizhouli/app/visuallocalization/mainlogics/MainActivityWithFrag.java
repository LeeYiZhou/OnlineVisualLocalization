package com.yizhouli.app.visuallocalization.mainlogics;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.yizhouli.app.visuallocalization.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivityWithFrag extends AppCompatActivity {

    private static final String TAG = MainActivityWithFrag.class.getSimpleName();

    private FrameLayout mainFrame;
    private Fragment[] fragments;
    private UploadImageFragment uploadImageFragment;
    private RequestSfmFragment requestSfmFragment;
    private DownloadPlyFragment downloadPlyFragment;
    private ModelPreviewFragment modelPreviewFragment;
    private int lastfragment = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_with_frag);

        uploadImageFragment = UploadImageFragment.newInstance();
        requestSfmFragment = RequestSfmFragment.newInstance();
        downloadPlyFragment = DownloadPlyFragment.newInstance();
        modelPreviewFragment = ModelPreviewFragment.newInstance();
        fragments = new Fragment[]{uploadImageFragment, requestSfmFragment, downloadPlyFragment, modelPreviewFragment};
        mainFrame = findViewById(R.id.main_container);

        getSupportFragmentManager().beginTransaction().replace(R.id.main_container, uploadImageFragment).show(uploadImageFragment).commit();

        BottomNavigationView bottomNavigationView = findViewById(R.id.nav_view);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_upload:
                        if (lastfragment != 0) {
                            switchFragment(lastfragment, 0);
                            lastfragment = 0;
                        }
                        return true;
                    case R.id.navigation_sfm:
                        if (lastfragment != 1) {
                            switchFragment(lastfragment, 1);
                            lastfragment = 1;
                        }
                        return true;
                    case R.id.navigation_download:
                        if (lastfragment != 2) {
                            switchFragment(lastfragment, 2);
                            lastfragment = 2;
                        }
                        return true;
                    case R.id.navigation_model_viewer:
                        if (lastfragment != 3) {
                            switchFragment(lastfragment, 3);
                            lastfragment = 3;
                        }
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void switchFragment(int lastfragment, int index) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //Hide last Fragment
        transaction.hide(fragments[lastfragment]);
        if (!fragments[index].isAdded()) {
            transaction.add(R.id.main_container, fragments[index]);
        }
        transaction.show(fragments[index]).commitAllowingStateLoss();
    }

}

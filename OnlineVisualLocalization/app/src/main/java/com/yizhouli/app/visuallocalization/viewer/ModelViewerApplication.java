package com.yizhouli.app.visuallocalization.viewer;

import android.app.Application;
import androidx.annotation.Nullable;

public class ModelViewerApplication extends Application
{
    private static ModelViewerApplication INSTANCE;

    // Store the current model globally, so that we don't have to re-decode it upon relaunching
    @Nullable private Model currentModel;

    public static ModelViewerApplication getInstance() {
        return INSTANCE;
    }

    @Nullable
    public Model getCurrentModel() {
        return currentModel;
    }

    public void setCurrentModel(@Nullable Model model) {
        currentModel = model;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        INSTANCE = this;
    }
}

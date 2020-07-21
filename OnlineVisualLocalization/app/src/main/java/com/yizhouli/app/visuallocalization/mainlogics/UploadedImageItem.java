package com.yizhouli.app.visuallocalization.mainlogics;

import android.graphics.Bitmap;

public class UploadedImageItem {

    private String imageTitle;
    private Bitmap bitmap;

    public UploadedImageItem(String imageTitle, Bitmap bitmap) {
        this.imageTitle = imageTitle;
        this.bitmap = bitmap;
    }

    public String getImageTitle() {
        return imageTitle;
    }

    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}

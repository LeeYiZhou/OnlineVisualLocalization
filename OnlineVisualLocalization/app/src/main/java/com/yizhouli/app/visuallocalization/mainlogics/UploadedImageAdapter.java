package com.yizhouli.app.visuallocalization.mainlogics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yizhouli.app.visuallocalization.R;

import java.util.ArrayList;
import java.util.List;

public class UploadedImageAdapter extends BaseAdapter {

    private Context context;
    private List<UploadedImageItem> data = new ArrayList<>();

    public UploadedImageAdapter(Context context, List<UploadedImageItem> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View menuItem = LayoutInflater.from(context).inflate(R.layout.grid_adapter, null);
        ImageView menu_img = menuItem.findViewById(R.id.menu_img);
        TextView menu_text = menuItem.findViewById(R.id.menu_text);
        menu_img.setImageBitmap(data.get(position).getBitmap());
        menu_text.setText(data.get(position).getImageTitle());
        return menuItem;
    }
}
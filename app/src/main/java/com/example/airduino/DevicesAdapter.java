package com.example.airduino;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {
    private Context context;
    private ArrayList<DeviceItem> m_data;

    public DevicesAdapter(Context _context) {
        context = _context;
        m_data = new ArrayList<DeviceItem>();
    }

    /* Méthodes redéfinies */

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.ligne, viewGroup, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder deviceViewHolder, int i) {
        DeviceItem item = m_data.get(i);
        deviceViewHolder.iv_device.setImageResource(item.ressource);
        deviceViewHolder.tv_device_name.setText(item.name);
        deviceViewHolder.tv_device_adr.setText(item.adr);
        deviceViewHolder.itemView.setTag(i);
    }

    @Override
    public int getItemCount() {
        return m_data.size();
    }

    /* Autres méthodes */

    public void ajoute(int ressource, String name, String adr){
        DevicesAdapter.DeviceItem item = new DevicesAdapter.DeviceItem();
        item.ressource = ressource;
        item.name = name;
        item.adr = adr;
        m_data.add(item);
        this.notifyItemInserted(m_data.size()-1);
    }

    public DeviceItem getItem(int position) {
        return m_data.get(position);
    }

    /* class */

    static class DeviceItem {
        int ressource;
        String name;
        String adr;
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private ImageView iv_device;
        private TextView tv_device_name;
        private TextView tv_device_adr;

        public DeviceViewHolder(View view) {
            super(view);
            iv_device = (ImageView)view.findViewById(R.id.iv_device);
            tv_device_name = (TextView)view.findViewById(R.id.tv_device_name);
            tv_device_adr = (TextView)view.findViewById(R.id.tv_device_adr);
        }
    }
}
package kz.spelost.agroapp.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.model.Crop;

public class CropTabAdapter extends RecyclerView.Adapter<CropTabAdapter.ViewHolder> {

    public interface OnCropTabClick {
        void onCropTabClick(Crop crop);
    }

    private final List<Crop> crops;
    private final OnCropTabClick listener;
    private String selectedId;

    public CropTabAdapter(List<Crop> crops, String selectedId, OnCropTabClick listener) {
        this.crops = crops;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    public void setSelectedId(String id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crop_tab, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Crop crop = crops.get(position);
        TextView tv = (TextView) holder.itemView;
        tv.setText(crop.icon + " " + crop.label);

        boolean isSelected = crop.id.equals(selectedId);
        tv.setBackgroundResource(isSelected ? R.drawable.bg_chip_active : R.drawable.bg_chip);
        tv.setTextColor(tv.getContext().getColor(isSelected ? R.color.text_primary : R.color.text_dim));

        tv.setOnClickListener(v -> listener.onCropTabClick(crop));
    }

    @Override
    public int getItemCount() {
        return crops.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) { super(itemView); }
    }
}

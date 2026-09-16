package kz.spelost.agroapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.model.Crop;

public class CropChipAdapter extends RecyclerView.Adapter<CropChipAdapter.ViewHolder> {

    public interface OnCropClick {
        void onCropClick(Crop crop);
    }

    private final List<Crop> crops;
    private final OnCropClick listener;

    public CropChipAdapter(List<Crop> crops, OnCropClick listener) {
        this.crops = crops;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crop_chip, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Crop crop = crops.get(position);
        holder.emoji.setText(crop.icon);
        holder.label.setText(crop.label);
        holder.itemView.setOnClickListener(v -> listener.onCropClick(crop));
    }

    @Override
    public int getItemCount() {
        return crops.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView emoji, label;
        ViewHolder(View itemView) {
            super(itemView);
            emoji = itemView.findViewById(R.id.chip_emoji);
            label = itemView.findViewById(R.id.chip_label);
        }
    }
}

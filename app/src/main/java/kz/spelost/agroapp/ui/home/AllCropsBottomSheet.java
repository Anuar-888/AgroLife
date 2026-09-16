package kz.spelost.agroapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.model.Crop;

public class AllCropsBottomSheet extends BottomSheetDialogFragment {

    public interface OnCropSelectedListener {
        void onCropSelected(Crop crop);
    }

    private OnCropSelectedListener listener;

    public void setListener(OnCropSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_all_crops_bottom_sheet, container, false);
        
        RecyclerView recyclerView = root.findViewById(R.id.all_crops_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        
        CropRepository.getInstance(requireContext()).getAll(crops -> {
            CropChipAdapter adapter = new CropChipAdapter(crops, crop -> {
                if (listener != null) {
                    listener.onCropSelected(crop);
                }
                dismiss();
            });
            recyclerView.setAdapter(adapter);
        });
        
        return root;
    }
}

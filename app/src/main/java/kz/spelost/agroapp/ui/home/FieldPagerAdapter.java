package kz.spelost.agroapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.model.UserField;
import kz.spelost.agroapp.ui.view.GrowthTrackerView;
import kz.spelost.agroapp.util.AgroLogic;

public class FieldPagerAdapter extends RecyclerView.Adapter<FieldPagerAdapter.FieldViewHolder> {

    private final List<UserField> fields;
    private final SimpleDateFormat sdfShort = new SimpleDateFormat("dd.MM", Locale.getDefault());

    public FieldPagerAdapter(List<UserField> fields) {
        this.fields = fields;
    }

    @NonNull
    @Override
    public FieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_field_card, parent, false);
        return new FieldViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FieldViewHolder holder, int position) {
        UserField field = fields.get(position);
        holder.name.setText(field.name);
        holder.area.setText(field.areaHectares + " Га • Посажено " + sdfShort.format(new Date(field.plantDateMillis)));
        
        AgroLogic.AgroAdvice advice = AgroLogic.getAdvice(field);
        
        if (holder.circularProgress != null) {
            holder.circularProgress.setProgress(advice.progressPercent);
        }
        
        holder.progressText.setText(advice.progressPercent + "%");
        holder.currentStage.setText("Стадия: " + advice.currentStageTitle);
        
        if (advice.isWateringRequired) {
            holder.nextStep.setText("Полив сегодня");
            holder.nextStep.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.task_water));
        } else if (advice.isFeedingRequired) {
            holder.nextStep.setText("Подкормка");
            holder.nextStep.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.task_feeding));
        } else {
            holder.nextStep.setText("Всё в порядке");
            holder.nextStep.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.accent_green));
        }

        holder.fertilizer.setText("💡 Совет: " + advice.organicTip);
    }

    @Override
    public int getItemCount() {
        return fields.size();
    }

    static class FieldViewHolder extends RecyclerView.ViewHolder {
        TextView name, area, nextStep, fertilizer;
        ProgressBar circularProgress;
        TextView progressText, currentStage;

        FieldViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.field_item_name);
            area = itemView.findViewById(R.id.field_item_area);
            nextStep = itemView.findViewById(R.id.field_item_next_step);
            fertilizer = itemView.findViewById(R.id.field_item_fertilizer);
            circularProgress = itemView.findViewById(R.id.field_item_progress_bar_circular);
            progressText = itemView.findViewById(R.id.field_item_progress_text);
            currentStage = itemView.findViewById(R.id.field_item_current_stage);
        }
    }
}

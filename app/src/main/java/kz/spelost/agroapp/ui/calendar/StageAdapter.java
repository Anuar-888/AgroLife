package kz.spelost.agroapp.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.util.DateUtils;

public class StageAdapter extends RecyclerView.Adapter<StageAdapter.ViewHolder> {

    private final List<CropStage> stages;
    private final Calendar plantDate;
    private Calendar selectedDate;
    private int expandedPosition = -1;

    public StageAdapter(List<CropStage> stages, Calendar plantDate, Calendar selectedDate) {
        this.stages = stages;
        this.plantDate = plantDate;
        this.selectedDate = (Calendar) selectedDate.clone();
    }

    public void setSelectedDate(Calendar date) {
        this.selectedDate = (Calendar) date.clone();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CropStage stage = stages.get(position);
        Calendar start = DateUtils.addDays(plantDate, stage.offsetDays);
        Calendar end = stage.hasRange() ? DateUtils.addDays(plantDate, stage.offsetEndDays) : start;

        holder.node.setText(stage.icon);

        String dateText = DateUtils.formatShort(start);
        if (stage.hasRange()) dateText += " – " + DateUtils.formatShort(end);
        holder.date.setText(dateText);
        holder.task.setText(stage.task);

        long diffMillis = selectedDate.getTimeInMillis() - plantDate.getTimeInMillis();
        int offsetDays = (int) Math.round((double) diffMillis / (24 * 60 * 60 * 1000));

        boolean isActive;
        if (stage.hasRange()) {
            isActive = offsetDays >= stage.offsetDays && offsetDays <= stage.offsetEndDays;
        } else {
            isActive = offsetDays == stage.offsetDays;
        }
        boolean isPast = offsetDays > (stage.hasRange() ? stage.offsetEndDays : stage.offsetDays);

        int primary = holder.itemView.getContext().getColor(R.color.primary);
        int dim = holder.itemView.getContext().getColor(R.color.text_secondary);
        int line = holder.itemView.getContext().getColor(R.color.line);

        if (isActive) {
            holder.node.setBackgroundResource(R.drawable.node_current);
            holder.title.setTextColor(primary);
            holder.stemTop.setBackgroundColor(primary);
            holder.stemBottom.setBackgroundColor(primary);
            holder.card.setBackgroundResource(R.drawable.bg_selected_day_panel);
        } else if (isPast) {
            holder.node.setBackgroundResource(R.drawable.node_done);
            holder.title.setTextColor(holder.itemView.getContext().getColor(R.color.text_main));
            holder.stemTop.setBackgroundColor(primary);
            holder.stemBottom.setBackgroundColor(primary);
            holder.card.setBackgroundResource(R.drawable.bg_card_elevated);
        } else {
            holder.node.setBackgroundResource(R.drawable.node_future);
            holder.title.setTextColor(dim);
            holder.stemTop.setBackgroundColor(line);
            holder.stemBottom.setBackgroundColor(line);
            holder.card.setBackgroundResource(R.drawable.bg_card_elevated);
        }

        holder.stemTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.stemBottom.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);

        holder.title.setText(stage.title + (isActive ? "  •  сейчас" : ""));

        boolean isExpanded = position == expandedPosition;
        holder.details.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.expandIcon.setImageResource(isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);

        holder.itemView.setOnClickListener(v -> {
            int prev = expandedPosition;
            expandedPosition = isExpanded ? -1 : position;
            notifyItemChanged(prev);
            notifyItemChanged(expandedPosition);
        });

        if (stage.hasFertilizer()) {
            holder.fert.setVisibility(View.VISIBLE);
            holder.fert.setText("🌿 " + stage.fertilizer);
        } else {
            holder.fert.setVisibility(View.GONE);
        }

        if (isExpanded) {
            StringBuilder sb = new StringBuilder();
            if (stage.description != null && !stage.description.isEmpty()) {
                sb.append(stage.description).append("\n\n");
            }
            
            sb.append("📅 Регулярные работы:\n");
            int count = 0;
            int interval = stage.task.toLowerCase().contains("полив") ? 4 : 10;
            int startDay = stage.offsetDays;
            int endDay = stage.hasRange() ? stage.offsetEndDays : stage.offsetDays + 14;
            
            for (int d = startDay; d <= endDay; d += interval) {
                if (count > 0) sb.append("  •  ");
                sb.append(DateUtils.formatShort(DateUtils.addDays(plantDate, d)));
                count++;
                if (count >= 5) break;
            }
            
            if (stage.hasFertilizer()) {
                sb.append("\n\n🌿 Подкормка: ").append(stage.fertilizer);
            }
            
            holder.concreteDates.setText(sb.toString());
        }
    }

    @Override
    public int getItemCount() {
        return stages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView node, title, date, task, fert, concreteDates;
        View details, stemTop, stemBottom, card;
        ImageView expandIcon;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.stage_card);
            node = itemView.findViewById(R.id.stage_node);
            title = itemView.findViewById(R.id.stage_title);
            date = itemView.findViewById(R.id.stage_date);
            task = itemView.findViewById(R.id.stage_task);
            fert = itemView.findViewById(R.id.stage_fert);
            details = itemView.findViewById(R.id.stage_details);
            stemTop = itemView.findViewById(R.id.stage_stem_top);
            stemBottom = itemView.findViewById(R.id.stage_stem_bottom);
            expandIcon = itemView.findViewById(R.id.expand_icon);
            concreteDates = itemView.findViewById(R.id.stage_concrete_dates);
        }
    }
}

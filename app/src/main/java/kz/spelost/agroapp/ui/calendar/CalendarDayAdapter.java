package kz.spelost.agroapp.ui.calendar;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.model.UserField;
import kz.spelost.agroapp.util.AgroLogic;
import kz.spelost.agroapp.util.DateUtils;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(Calendar date);
    }

    private final List<Calendar> days;
    private final OnDayClickListener listener;
    private Calendar selectedDate;
    private UserField userField;

    private final SimpleDateFormat dayNumberFormat = new SimpleDateFormat("d", new Locale("ru"));

    public CalendarDayAdapter(List<Calendar> days, Calendar selectedDate, OnDayClickListener listener) {
        this.days = days;
        this.selectedDate = (Calendar) selectedDate.clone();
        this.listener = listener;
    }

    public void setUserField(UserField field) {
        this.userField = field;
    }

    public void setSelectedDate(Calendar date) {
        this.selectedDate = (Calendar) date.clone();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Calendar day = days.get(position);

        if (day == null) {
            holder.dayNumber.setText("");
            holder.itemView.setVisibility(View.INVISIBLE);
            holder.itemView.setOnClickListener(null);
            return;
        }

        holder.itemView.setVisibility(View.VISIBLE);
        holder.dayNumber.setText(dayNumberFormat.format(day.getTime()));

        Calendar today = DateUtils.today();
        boolean isSelected = isSameDay(day, selectedDate);
        boolean isToday = isSameDay(day, today);

        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_day_selected);
            holder.dayNumber.setTextColor(Color.WHITE);
        } else if (isToday) {
            holder.itemView.setBackgroundResource(R.drawable.bg_day_today);
            holder.dayNumber.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_day_normal);
            holder.dayNumber.setTextColor(holder.itemView.getContext().getColor(R.color.text_main));
        }

        holder.dotWater.setVisibility(View.GONE);
        holder.dotFeeding.setVisibility(View.GONE);
        holder.dotHarvest.setVisibility(View.GONE);

        if (userField != null) {
            List<AgroLogic.TaskType> tasks = AgroLogic.getDayTasks(userField, day);
            for (AgroLogic.TaskType task : tasks) {
                switch (task) {
                    case WATERING:
                        holder.dotWater.setVisibility(View.VISIBLE);
                        break;
                    case FERTILIZER:
                        holder.dotFeeding.setVisibility(View.VISIBLE);
                        break;
                    case HARVEST:
                        holder.dotHarvest.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(day);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        if (c1 == null || c2 == null) return false;
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dayNumber;
        View dotWater, dotFeeding, dotHarvest;

        ViewHolder(View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.day_number);
            dotWater = itemView.findViewById(R.id.dot_water);
            dotFeeding = itemView.findViewById(R.id.dot_feeding);
            dotHarvest = itemView.findViewById(R.id.dot_harvest);
        }
    }
}

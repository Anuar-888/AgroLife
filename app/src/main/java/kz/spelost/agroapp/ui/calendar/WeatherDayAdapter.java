package kz.spelost.agroapp.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.network.WeatherClient;

public class WeatherDayAdapter extends RecyclerView.Adapter<WeatherDayAdapter.ViewHolder> {

    private final List<WeatherClient.DayForecast> days;
    private static final SimpleDateFormat INPUT_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat OUTPUT_FMT = new SimpleDateFormat("EEE", new Locale("ru"));

    public WeatherDayAdapter(List<WeatherClient.DayForecast> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weather_day, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherClient.DayForecast d = days.get(position);
        String label = d.date;
        try {
            label = OUTPUT_FMT.format(INPUT_FMT.parse(d.date));
        } catch (Exception ignored) {}
        holder.label.setText(label);
        holder.icon.setText(WeatherClient.weatherEmoji(d.weatherCode));
        holder.temp.setText(Math.round(d.tempMax) + "°/" + Math.round(d.tempMin) + "°");
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView label, icon, temp;
        ViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.wday_label);
            icon = itemView.findViewById(R.id.wday_icon);
            temp = itemView.findViewById(R.id.wday_temp);
        }
    }
}

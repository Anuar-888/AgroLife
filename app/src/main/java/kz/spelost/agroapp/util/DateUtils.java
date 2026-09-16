package kz.spelost.agroapp.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.PlantingWindow;

public class DateUtils {

    private static final SimpleDateFormat DAY_MONTH = new SimpleDateFormat("d MMM", new Locale("ru"));

    public static Calendar addDays(Calendar base, int days) {
        Calendar c = (Calendar) base.clone();
        c.add(Calendar.DAY_OF_YEAR, days);
        return c;
    }

    public static String formatShort(Calendar c) {
        return DAY_MONTH.format(c.getTime());
    }

    public static Calendar today() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** true, если calendar лежит внутри окна [start, end] в указанном году (включительно). */
    private static boolean inRangeForYear(Calendar date, PlantingWindow w, int year) {
        Calendar start = Calendar.getInstance();
        start.set(year, w.startMonth - 1, w.startDay, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(year, w.endMonth - 1, w.endDay, 0, 0, 0);
        end.set(Calendar.MILLISECOND, 0);

        return !date.before(start) && !date.after(end);
    }

    public static boolean isDateInAnyWindow(Calendar date, List<PlantingWindow> windows) {
        int year = date.get(Calendar.YEAR);
        for (PlantingWindow w : windows) {
            if (inRangeForYear(date, w, year)) return true;
        }
        return false;
    }

    /** Ближайшая допустимая дата посадки: сегодня, если сезон уже идёт, иначе ближайшее начало окна. */
    public static Calendar nearestValidPlantingDate(Crop crop) {
        Calendar today = today();
        Calendar best = null;
        long bestDiff = Long.MAX_VALUE;

        for (PlantingWindow w : crop.plantingWindows) {
            for (int yearOffset = 0; yearOffset <= 1; yearOffset++) {
                int year = today.get(Calendar.YEAR) + yearOffset;

                Calendar start = Calendar.getInstance();
                start.set(year, w.startMonth - 1, w.startDay, 0, 0, 0);
                start.set(Calendar.MILLISECOND, 0);

                Calendar end = Calendar.getInstance();
                end.set(year, w.endMonth - 1, w.endDay, 0, 0, 0);
                end.set(Calendar.MILLISECOND, 0);

                Calendar candidate;
                if (!today.before(start) && !today.after(end)) {
                    candidate = today;
                } else if (today.before(start)) {
                    candidate = start;
                } else {
                    continue;
                }

                long diff = Math.abs(candidate.getTimeInMillis() - today.getTimeInMillis());
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = candidate;
                }
            }
        }
        return best != null ? best : today;
    }
}

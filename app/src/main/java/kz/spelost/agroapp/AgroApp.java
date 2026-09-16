package kz.spelost.agroapp;

import android.app.Application;
import kz.spelost.agroapp.data.AppState;
import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.notifications.NotificationHelper;
import kz.spelost.agroapp.notifications.WateringReminderScheduler;

public class AgroApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppState.getInstance().load(this);
        CropRepository.getInstance(this);
        NotificationHelper.ensureChannels(this);
        WateringReminderScheduler.reschedule(this);
    }
}

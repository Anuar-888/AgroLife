package kz.spelost.agroapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import kz.spelost.agroapp.data.AppState;
import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.notifications.NotificationHelper;
import kz.spelost.agroapp.notifications.WateringReminderScheduler;
import kz.spelost.agroapp.ui.calendar.CalendarFragment;
import kz.spelost.agroapp.ui.chat.ChatFragment;
import kz.spelost.agroapp.ui.home.HomeFragment;
import kz.spelost.agroapp.ui.profile.ProfileFragment;
import kz.spelost.agroapp.ui.tools.ToolsFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) {
            showFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_tools) {
                showFragment(new ToolsFragment());
                return true;
            } else if (id == R.id.nav_chat) {
                showFragment(new ChatFragment());
                return true;
            } else if (id == R.id.nav_calendar) {
                showFragment(new CalendarFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                showFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        );
        tx.replace(R.id.fragment_container, fragment);
        tx.commit();
    }

    /** Позволяет фрагментам (например, чипу культуры на Home) переключить нижнюю вкладку. */
    public void navigateTo(int menuItemId) {
        bottomNav.setSelectedItemId(menuItemId);
    }
}

package kz.spelost.agroapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import kz.spelost.agroapp.MainActivity;
import kz.spelost.agroapp.R;
import kz.spelost.agroapp.data.AppState;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        
        AppState state = AppState.getInstance();
        
        // If name already set and not default, skip login (simplified demo logic)
        if (state.userName != null && !state.userName.isEmpty() && !"Guest".equals(state.userName) && !"Anuar".equals(state.userName)) {
             startActivity(new Intent(this, MainActivity.class));
             finish();
             return;
        }

        setContentView(R.layout.activity_login);

        EditText input = findViewById(R.id.login_input_name);
        findViewById(R.id.login_btn_enter).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                state.userName = name;
                state.save(this);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
}

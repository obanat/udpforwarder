package com.obana.vpnforwarder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {
    private EditText editTextToAddress;
    private EditText editTextPort;
    private EditText editTextMAC;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }

        initViews();
        loadSettings();
    }

    private void initViews() {
        editTextToAddress = findViewById(R.id.editTextToAddress);
        editTextPort = findViewById(R.id.editTextPort);
        editTextMAC = findViewById(R.id.editTextMAC);
        sharedPreferences = getSharedPreferences("sms_settings", MODE_PRIVATE);
    }

    private void loadSettings() {
        String ip = sharedPreferences.getString("redisIp", "");
        String port = sharedPreferences.getString("redisPort", "");
        String mac = sharedPreferences.getString("mac", "");
        editTextToAddress.setText(ip);
        editTextPort.setText(port);
        editTextMAC.setText(mac);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    private void saveSettings() {
        String ip = editTextToAddress.getText().toString();
        String port = editTextPort.getText().toString();
        String mac = editTextMAC.getText().toString();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("redisIp", ip);
        editor.putString("redisPort", port);
        editor.putString("mac", mac);
        editor.apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}


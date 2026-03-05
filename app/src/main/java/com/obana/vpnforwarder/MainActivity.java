
package com.obana.vpnforwarder;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int VPN_REQUEST_CODE = 0x0F;

    public static final int MSG_PLAY_ANIMATION_1 = 1001;
    public static final int MSG_PLAY_ANIMATION_2 = 1002;
    public static final int MSG_PLAY_ANIMATION_3 = 1003;
    public static final int MSG_PLAY_ANIMATION_4 = 1004;

    private ImageButton mSetttingsBtn;
    private Button mStartButton;
    private TextView textViewSettings;
    private ImageView imageViewAnim1, imageViewAnim2, imageViewAnim3, imageViewAnim4;
    private Animation blinkAnimation;
    private boolean isServiceRunning = false;
    private VectorDrawableCompat arrowDrawable1, arrowDrawable2, arrowDrawable3, arrowDrawable4;

    private Handler animationHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PLAY_ANIMATION_1:
                    playAnimation1();
                    break;
                case MSG_PLAY_ANIMATION_2:
                    playAnimation2();
                    break;
                case MSG_PLAY_ANIMATION_3:
                    playAnimation3();
                    break;
                case MSG_PLAY_ANIMATION_4:
                    playAnimation4();
                    break;
            }
        }
    };

    public Handler getAnimationHandler() {
        return animationHandler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSetttingsBtn = findViewById(R.id.setting_button);
        mSetttingsBtn.setOnClickListener(this);
        checkPermissions();

        mStartButton = findViewById(R.id.btn_start_vpn);
        mStartButton.setOnClickListener(this);

        textViewSettings = findViewById(R.id.textViewSettings);

        imageViewAnim1 = findViewById(R.id.imageViewAnim1);
        imageViewAnim2 = findViewById(R.id.imageViewAnim2);
        imageViewAnim3 = findViewById(R.id.imageViewAnim3);
        imageViewAnim4 = findViewById(R.id.imageViewAnim4);

        blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink_arrow);

        arrowDrawable1 = VectorDrawableCompat.create(getResources(), R.drawable.ic_arrow_vector, getTheme());
        arrowDrawable2 = VectorDrawableCompat.create(getResources(), R.drawable.ic_arrow_vector, getTheme());
        arrowDrawable3 = VectorDrawableCompat.create(getResources(), R.drawable.ic_arrow_vector_left, getTheme());
        arrowDrawable4 = VectorDrawableCompat.create(getResources(), R.drawable.ic_arrow_vector_left, getTheme());

        updateButtonText();
        loadSettings();
    }

    private void loadSettings() {
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("sms_settings", MODE_PRIVATE);
        String ip = sharedPreferences.getString("redisIp", "");
        String port = sharedPreferences.getString("redisPort", "");
        String mac = sharedPreferences.getString("mac", "");

        StringBuilder settingsText = new StringBuilder();
        settingsText.append("IP: ").append(ip.isEmpty() ? "未设置" : ip).append("\n");
        settingsText.append("端口: ").append(port.isEmpty() ? "未设置" : port).append("\n");
        settingsText.append("MAC: ").append(mac.isEmpty() ? "未设置" : mac);

        textViewSettings.setText(settingsText.toString());

    }

    private void playAnimation1() {
        if (imageViewAnim1 != null && blinkAnimation != null) {
            arrowDrawable1.setTint(0xFF4CAF50);
            imageViewAnim1.setImageDrawable(arrowDrawable1);
            imageViewAnim1.startAnimation(blinkAnimation);
            animationHandler.postDelayed(() -> {
                imageViewAnim1.clearAnimation();
            }, 2000);
            animationHandler.postDelayed(() -> {
                arrowDrawable1.setTint(0xFF9E9E9E);
                imageViewAnim1.setImageDrawable(arrowDrawable1);
            }, 5000);
        }
    }

    private void playAnimation2() {
        if (imageViewAnim2 != null && blinkAnimation != null) {
            arrowDrawable2.setTint(0xFF4CAF50);
            imageViewAnim2.setImageDrawable(arrowDrawable2);
            imageViewAnim2.startAnimation(blinkAnimation);
            animationHandler.postDelayed(() -> {
                imageViewAnim2.clearAnimation();
            }, 2000);
            animationHandler.postDelayed(() -> {
                arrowDrawable2.setTint(0xFF9E9E9E);
                imageViewAnim2.setImageDrawable(arrowDrawable2);
            }, 5000);
        }
    }

    private void playAnimation3() {
        if (imageViewAnim3 != null && blinkAnimation != null) {
            arrowDrawable3.setTint(0xFF4CAF50);
            imageViewAnim3.setImageDrawable(arrowDrawable3);
            imageViewAnim3.startAnimation(blinkAnimation);
            animationHandler.postDelayed(() -> {
                imageViewAnim3.clearAnimation();
            }, 2000);
            animationHandler.postDelayed(() -> {
                arrowDrawable3.setTint(0xFF9E9E9E);
                imageViewAnim3.setImageDrawable(arrowDrawable3);
            }, 5000);
        }
    }

    private void playAnimation4() {
        if (imageViewAnim4 != null && blinkAnimation != null) {
            arrowDrawable4.setTint(0xFF4CAF50);
            imageViewAnim4.setImageDrawable(arrowDrawable4);
            imageViewAnim4.startAnimation(blinkAnimation);
            animationHandler.postDelayed(() -> {
                imageViewAnim4.clearAnimation();
            }, 2000);
            animationHandler.postDelayed(() -> {
                arrowDrawable4.setTint(0xFF9E9E9E);
                imageViewAnim4.setImageDrawable(arrowDrawable4);
            }, 5000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    private void checkPermissions() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"请检查权限设置",Toast.LENGTH_LONG).show();
                finish(); // 权限被拒绝则退出应用
            } else {

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view == mSetttingsBtn) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (view == mStartButton) {
            if (isServiceRunning) {
                stopVpnService();
            } else {
                // 1. 准备VPN服务，检查是否已有VPN在运行或是否需要授权
                Intent vpnIntent = VpnService.prepare(this);

                if (vpnIntent != null) {
                    // 情况A：未授权，弹出系统VPN授权对话框
                    // 注意：此时不能启动Service，必须等待 onActivityResult 返回
                    startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
                } else {
                    // 情况B：已经授权过，直接启动服务
                    startVpnService();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // 用户点击了"确定"授权
                startVpnService();
            } else {
                // 用户点击了"取消"或返回
                Toast.makeText(this, "权限被拒绝，无法启动", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startVpnService() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        Intent serviceIntent = new Intent(this, UdpFilterVpnService.class);

        Messenger activityMessenger = new Messenger(animationHandler);
        serviceIntent.putExtra(UdpFilterVpnService.EXTRA_MESSENGER, activityMessenger);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        isServiceRunning = true;
        updateButtonText();
        Toast.makeText(this, "UDP转发服务已启动", Toast.LENGTH_SHORT).show();
    }

    private void stopVpnService() {
        Intent serviceIntent = new Intent(this, UdpFilterVpnService.class);
        serviceIntent.putExtra("stop_service", true);
        startService(serviceIntent);

        isServiceRunning = false;
        updateButtonText();
        Toast.makeText(this, "UDP转发服务已停止", Toast.LENGTH_SHORT).show();
    }

    private void updateButtonText() {
        if (isServiceRunning) {
            mStartButton.setText("停止 UDP 转发");
        } else {
            mStartButton.setText("启动 御Pro UDP 转发");
        }
    }
}

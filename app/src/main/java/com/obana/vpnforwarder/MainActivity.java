
package com.obana.vpnforwarder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int VPN_REQUEST_CODE = 0x0F;
    private ImageButton mSetttingsBtn;
    private Button mStartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSetttingsBtn = findViewById(R.id.setting_button);
        mSetttingsBtn.setOnClickListener(this);
        checkPermissions();

        mStartButton = findViewById(R.id.btn_start_vpn);
        mStartButton.setOnClickListener(this);
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
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        Intent serviceIntent = new Intent(this, UdpFilterVpnService.class);

        // Android 8.0+ 后台启动限制：如果是启动前台服务，需使用 startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "UDP转发服务已启动", Toast.LENGTH_SHORT).show();
        // 为了用户体验，建议在这里将按钮置灰或显示"运行中"状态
    }
}

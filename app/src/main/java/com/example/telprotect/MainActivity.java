package com.example.telprotect;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements KeywordAdapter.OnKeywordDeleteListener {
    
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int ROLE_REQUEST_CODE = 200;
    
    private TextView statusTextView;
    private Button toggleServiceButton;
    private TextInputEditText keywordEditText;
    private Button addKeywordButton;
    private RecyclerView keywordRecyclerView;
    
    private KeywordDBHelper dbHelper;
    private KeywordAdapter keywordAdapter;
    private List<String> keywordList;
    
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化视图
        initViews();
        
        // 初始化数据库
        dbHelper = new KeywordDBHelper(this);
        
        // 初始化关键字列表
        keywordList = new ArrayList<>();
        loadKeywords();
        
        // 初始化适配器
        keywordAdapter = new KeywordAdapter(keywordList, this);
        keywordRecyclerView.setAdapter(keywordAdapter);
        keywordRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 设置按钮点击监听器
        setupClickListeners();
        
        // 检查权限
        checkPermissions();
    }
    
    private void initViews() {
        statusTextView = findViewById(R.id.statusTextView);
        toggleServiceButton = findViewById(R.id.toggleServiceButton);
        keywordEditText = findViewById(R.id.keywordEditText);
        addKeywordButton = findViewById(R.id.addKeywordButton);
        keywordRecyclerView = findViewById(R.id.keywordRecyclerView);
    }
    
    private void setupClickListeners() {
        // 添加关键字按钮
        addKeywordButton.setOnClickListener(v -> {
            String keyword = keywordEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(keyword)) {
                addKeyword(keyword);
                keywordEditText.setText("");
            }
        });
        
        // 切换服务按钮
        toggleServiceButton.setOnClickListener(v -> {
            if (isServiceRunning) {
                stopCallBlockerService();
            } else {
                startCallBlockerService();
            }
        });
    }
    
    private void loadKeywords() {
        keywordList.clear();
        keywordList.addAll(dbHelper.getAllKeywords());
        if (keywordAdapter != null) {
            keywordAdapter.notifyDataSetChanged();
        }
    }
    
    private void addKeyword(String keyword) {
        if (dbHelper.addKeyword(keyword)) {
            loadKeywords();
            Toast.makeText(this, "关键字已添加", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "关键字添加失败或已存在", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onKeywordDelete(String keyword) {
        if (dbHelper.deleteKeyword(keyword)) {
            loadKeywords();
            Toast.makeText(this, "关键字已删除", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "关键字删除失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // 检查电话状态权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        
        // 检查接听/挂断电话权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ANSWER_PHONE_CALLS);
        }
        
        // 检查拨打电话权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }
        
        // 检查读取联系人权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        
        // 如果需要权限，请求它们
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }
    
    private void requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
            Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
            startActivityForResult(intent, ROLE_REQUEST_CODE);
        } else {
            // 对于Android 9及以下版本，引导用户手动设置
            showManualSettingsDialog();
        }
    }
    
    private void showManualSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要设置")
                .setMessage("请在设置中将此应用设置为默认的电话应用或启用来电过滤功能")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void startCallBlockerService() {
        // 请求电话筛选角色
        requestCallScreeningRole();
        
        // 启动前台服务
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        isServiceRunning = true;
        updateServiceStatus();
    }
    
    private void stopCallBlockerService() {
        // 停止前台服务
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
        
        isServiceRunning = false;
        updateServiceStatus();
    }
    
    private void updateServiceStatus() {
        if (isServiceRunning) {
            statusTextView.setText(R.string.service_running);
            toggleServiceButton.setText(R.string.stop_service);
        } else {
            statusTextView.setText(R.string.service_stopped);
            toggleServiceButton.setText(R.string.start_service);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, R.string.permission_message, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ROLE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "电话拦截服务已启用", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未授予电话筛选权限，服务可能无法正常工作", Toast.LENGTH_LONG).show();
                isServiceRunning = false;
                updateServiceStatus();
            }
        }
    }
} 
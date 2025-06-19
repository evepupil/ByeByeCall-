package com.example.telprotect;

import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.Connection;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class CallBlockerService extends CallScreeningService {
    
    private static final String TAG = "CallBlockerService";
    private KeywordDBHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new KeywordDBHelper(this);
    }

    @Override
    public void onScreenCall(Call.Details callDetails) {
        // 获取来电号码和名称
        String incomingNumber = callDetails.getHandle() != null ? 
                callDetails.getHandle().getSchemeSpecificPart() : "";
        
        // 获取来电显示名称
        CharSequence callerDisplayName = callDetails.getCallerDisplayName();
        String displayName = callerDisplayName != null ? callerDisplayName.toString() : "";
        
        Log.d(TAG, "来电: " + incomingNumber + ", 名称: " + displayName);
        
        // 检查是否应该拦截此来电
        boolean shouldBlock = shouldBlockCall(displayName);
        
        // 创建响应
        CallResponse.Builder responseBuilder = new CallResponse.Builder();
        
        if (shouldBlock) {
            Log.d(TAG, "拦截来电: " + displayName);
            // 拒绝来电
            responseBuilder.setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)  // 仍然记录到通话记录
                    .setSkipNotification(true);  // 跳过通知
        } else {
            // 允许来电
            responseBuilder.setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false);
        }
        
        // 发送响应
        respondToCall(callDetails, responseBuilder.build());
    }

    private boolean shouldBlockCall(String displayName) {
        if (TextUtils.isEmpty(displayName)) {
            return false;
        }
        
        // 获取所有关键字
        List<String> keywords = dbHelper.getAllKeywords();
        
        // 检查显示名称是否包含任何关键字
        for (String keyword : keywords) {
            if (!TextUtils.isEmpty(keyword) && displayName.contains(keyword)) {
                Log.d(TAG, "匹配关键字: " + keyword);
                return true;
            }
        }
        
        return false;
    }
} 
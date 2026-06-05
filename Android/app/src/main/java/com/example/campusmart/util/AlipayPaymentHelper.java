package com.example.campusmart.util;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.alipay.sdk.app.EnvUtils;
import com.alipay.sdk.app.PayTask;

import java.util.Map;

public class AlipayPaymentHelper {
    public interface Callback {
        void onResult(PayResult result);
    }

    public static void pay(Activity activity, String orderString, Callback callback) {
        new Thread(() -> {
            EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX);
            PayTask alipay = new PayTask(activity);
            Map<String, String> rawResult = alipay.payV2(orderString, true);
            PayResult result = new PayResult(rawResult);
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
        }).start();
    }

    public static class PayResult {
        private final String resultStatus;
        private final String result;
        private final String memo;

        public PayResult(Map<String, String> rawResult) {
            resultStatus = value(rawResult, "resultStatus");
            result = value(rawResult, "result");
            memo = value(rawResult, "memo");
        }

        public boolean isSuccess() {
            return "9000".equals(resultStatus);
        }

        public boolean isProcessing() {
            return "8000".equals(resultStatus) || "6004".equals(resultStatus);
        }

        public boolean isCancelled() {
            return "6001".equals(resultStatus);
        }

        public String message() {
            if (isSuccess()) {
                return "Payment submitted successfully. Syncing order status.";
            }
            if (isProcessing()) {
                return "Payment result is processing. Waiting for server confirmation.";
            }
            if (isCancelled()) {
                return "Payment cancelled.";
            }
            if (memo != null && !memo.trim().isEmpty()) {
                return memo;
            }
            return "Payment failed. Status: " + resultStatus;
        }

        public String getResult() {
            return result;
        }

        private static String value(Map<String, String> rawResult, String key) {
            return rawResult != null ? rawResult.get(key) : "";
        }
    }
}

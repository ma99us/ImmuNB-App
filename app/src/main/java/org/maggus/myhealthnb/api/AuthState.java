package org.maggus.myhealthnb.api;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.Data;

@Data
public class AuthState {
    private String username;
    private String password;
    private String loginFormAction;
    private String authorizationCode;
    private String authorizationToken;

    public static String generateDeviceId(Context ctx) {
        final TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        String tmDevice = "";
        try {
            tmDevice += tm.getDeviceId();
        } catch (SecurityException ex) {
            // newer android will reject this
        }
        String tmSerial = "";
        try {
            tmSerial += tm.getSimSerialNumber();
        } catch (SecurityException ex) {
            // newer android will reject this
        }
        String androidId = "";
        try {
            androidId += android.provider.Settings.Secure.getString(ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        } catch (SecurityException ex) {
            // no-op
        }

        if (tmDevice.isEmpty() && tmSerial.isEmpty() && androidId.isEmpty()) {
            Log.w("DeviceId", "Failed to get any device unique info");
        }

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }

    public static String base64UrlEncodeString(String str) {
        String encoded = Base64.encodeToString(str.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        encoded = encoded.replaceAll("\\+", "-").
                replaceAll("\\/", "_").
                replaceAll("=", "-");
        return encoded;
    }

    public static String generateRandomString(int len) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < len) {
            sb.append(UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", ""));
        }
        return sb.toString().substring(0, len);
    }
}

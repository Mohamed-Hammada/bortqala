package com.bemo.erp.nativebridge;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.concurrent.Executor;

/**
 * WP-14 AC-6: thin bridge over androidx.biometric for the app-resume unlock gate.
 * Falls back to the device credential (PIN/pattern) when biometrics are enrolled-unavailable.
 */
@CapacitorPlugin(name = "BiometricGate")
public class BiometricGatePlugin extends Plugin {

    @PluginMethod
    public void isAvailable(PluginCall call) {
        BiometricManager manager = BiometricManager.from(getContext());
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        boolean available = manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS;
        JSObject result = new JSObject();
        result.put("available", available);
        call.resolve(result);
    }

    @PluginMethod
    public void authenticate(PluginCall call) {
        String reason = call.getString("reason", "Unlock Bemo ERP");
        String fallbackTitle = call.getString("fallbackTitle", "Use PIN");

        AppCompatActivity activity = (AppCompatActivity) bridge.getActivity();
        if (activity == null || activity.isFinishing()) {
            call.resolve(toResult(false, true));
            return;
        }
        Executor executor = ContextCompat.getMainExecutor(getContext());
        BiometricPrompt prompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                call.resolve(toResult(true, false));
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                boolean cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED
                        || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON;
                call.resolve(toResult(false, cancelled));
            }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(reason)
                .setSubtitle(fallbackTitle)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        prompt.authenticate(info);
    }

    private JSObject toResult(boolean ok, boolean cancelled) {
        JSObject result = new JSObject();
        result.put("ok", ok);
        result.put("cancelled", cancelled);
        return result;
    }
}

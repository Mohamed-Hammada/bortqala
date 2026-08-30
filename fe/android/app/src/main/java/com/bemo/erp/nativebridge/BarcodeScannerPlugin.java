package com.bemo.erp.nativebridge;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

/**
 * WP-14 AC-4: barcode capture via Google play-services-code-scanner (no camera permission
 * needed — the scanner activity is provided by Play Services). Resolves the raw value or a
 * cancelled flag; failures surface as plugin errors the TS layer maps to null.
 */
@CapacitorPlugin(name = "BarcodeScanner")
public class BarcodeScannerPlugin extends Plugin {

    @PluginMethod
    public void scan(PluginCall call) {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .enableAutoZoom()
                .build();
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(getContext(), options);
        scanner.startScan()
                .addOnSuccessListener(barcode -> call.resolve(toResult(barcode, false)))
                .addOnCanceledListener(() -> call.resolve(toResult(null, true)))
                .addOnFailureListener(exception -> call.reject("SCAN_FAILED", exception.getMessage(), exception));
    }

    private JSObject toResult(Barcode barcode, boolean cancelled) {
        JSObject result = new JSObject();
        result.put("cancelled", cancelled);
        result.put("value", barcode == null ? null : barcode.getRawValue());
        return result;
    }
}

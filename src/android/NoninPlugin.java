package org.apache.cordova.nonin;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;

public class NoninPlugin extends CordovaPlugin {

    BluetoothAdapter adapter;
    private static final String LOG_NAME = NoninPlugin.class.getName();
    private Nonin device;
    private CallbackContext callbackContext;
    private Activity activity;
    private final int PERMISSIONS_REQUEST = 142;

    /**
     * Sets the context of the Command.
     *
     * @param cordova the context of the main Activity.
     * @param webView the associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.activity = cordova.getActivity();
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
    }

    /* @Override */
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            for(int result:grantResults) {
                if(result == PackageManager.PERMISSION_DENIED) {
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Permissions denied"));
                    return;
                }
            }
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "Permissions granted"));
        }
    }

    /**
     * Executes the request.
     *
     * @param action the action to execute.
     * @param args the exec() arguments.
     * @param callbackContext the callback context used when calling back into JavaScript.
     * @return whether the action was valid.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action.equalsIgnoreCase("askPermissions")) {
            if(!PermissionHelper.hasPermission(this, ACCESS_COARSE_LOCATION) ||
                    !PermissionHelper.hasPermission(this, BLUETOOTH) ||
                    !PermissionHelper.hasPermission(this, BLUETOOTH_ADMIN)) {
               PermissionHelper.requestPermissions(this, PERMISSIONS_REQUEST, new String[] { ACCESS_COARSE_LOCATION, BLUETOOTH, BLUETOOTH_ADMIN });
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            }
            return true;
        } else if (action.equalsIgnoreCase("isBTON")) {
            boolean r = adapter.isEnabled();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, r));
            return true;
        } else if (action.equalsIgnoreCase("askBTON")) {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            BroadcastReceiver recevier = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if(state == BluetoothAdapter.STATE_ON){
                            //unregister itself
                            final BroadcastReceiver me = this;
                            activity.unregisterReceiver(me);
                            callbackContext.success();
                        }
                    }
                }
            };
            activity.registerReceiver(recevier, filter);
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, activity.RESULT_OK);

            return true;
        } else if (action.equalsIgnoreCase("isPaired")) {
            try{
                String addr = args.getString(0);
                boolean r = Nonin.isBonded(adapter, addr);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, r));
            } catch (Exception ex) {
                Log.e(LOG_NAME, "Wrong address specified", ex);
                callbackContext.error("You must specify a valid address");
            }
            return true;
        } else if (action.equalsIgnoreCase("start")) {
            String addr;
            try{
                addr = args.getString(0);
            } catch (Exception ex) {
                Log.e(LOG_NAME, "Wrong address specified", ex);
                callbackContext.error("You must specify a valid address");
                return true;
            }
            device = new Nonin(adapter, addr, new Nonin.NoninHandler() {
                @Override
                public void handle(final NoninPacket packet) {
                    JSONObject r = new JSONObject();
                    // data.spo2 -> blood saturation (avg over 4 pulses)
                    // data.instantSpo2 -> instantaneous (non averaged) spo2
                    // data.hr -> heart rate (avg over 4 pulses)
                    // data.timestamp -> ms since 1970
                    // data.timer -> internal device timer
                    // data.hasArtifacts -> true if the signal has artifacts (low quality)
                    // data.hasSustainedArtifacts -> true if the signal has sustained artifacts (even lower quality)
                    // data.nofinger -> true if the finger was removed from the device
                    // data.batterylow -> true if batteries are low
                    // data.sensorAlarm -> true if data is unusable
                    // data.smartPoint -> true if very precise measurement
                    // data.PPG -> array of PPG samples
                    try {
                        r.put("spo2", packet.getSpO2Average());
                        r.put("instantSpo2", packet.getBeatToBeatSpO2());
                        r.put("hr", packet.getHRAverage());
                        r.put("timestamp", System.currentTimeMillis());
                        r.put("timer", packet.getTimer());
                        r.put("hasArtifacts", packet.hasAnyArtifact());
                        r.put("hasSustainedArtifacts", packet.hasAnyOutOfTrack());
                        r.put("nofinger", packet.hasAnySensorAlarm());
                        r.put("batterylow", packet.isBatteryLow());
                        r.put("sensorAlarm", packet.hasAnySensorAlarm());
                        r.put("smartPoint", packet.isSmartPointMeasurement());
                        JSONArray ppgsamples = new JSONArray();
                        for(int s : packet.getPlethSamples()) {
                            ppgsamples.put(s);
                        }
                        r.put("PPG", ppgsamples);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    PluginResult result = new PluginResult(PluginResult.Status.OK, r);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                }
            });

            try {
                device.start();
            } catch (IOException ex) {
                Log.e(LOG_NAME, "Cannot connect to "+addr, ex);
                callbackContext.error("Cannot connect to "+addr);
                return true;
            }
            return true;
        } else if (action.equalsIgnoreCase("stop")) {
            if(device != null){
                device.stop();
            }
            callbackContext.success();
            return true;
        } else {
            // Unsupported action
            return false;
        }
    }
}

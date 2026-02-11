package com.termux.app.event;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.TermuxService;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.termux.TermuxUtils;

public class SystemEventReceiver extends BroadcastReceiver {

    private static SystemEventReceiver mInstance;
    private static Context context;
    static final String alarmId = "alarm";
    static int alarmFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
    static AlarmManager am;
    private static final String LOG_TAG = "SystemEventReceiver";

    public static synchronized SystemEventReceiver getInstance() {
        if (mInstance == null) {
            mInstance = new SystemEventReceiver();
        }
        return mInstance;
    }

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) return;
        Logger.logDebug(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));

        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                onActionBootCompleted(context, intent);
                break;
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REMOVED:
            case Intent.ACTION_PACKAGE_REPLACED:
                onActionPackageUpdated(context, intent);
                break;
            case Intent.ACTION_SCREEN_OFF:
                setAlarm();
                break;
            case Intent.ACTION_SCREEN_ON:
                cancelAlarm();
                break;
            case alarmId:
                alarm();
                break;
            default:
                Logger.logError(LOG_TAG, "Invalid action \"" + action + "\" passed to " + LOG_TAG);
        }
    }

    Intent alarmIntent(){
        Intent intent = new Intent(context, SystemEventReceiver.class);
        intent.setAction(alarmId);
        return intent;
    }

    int alarmId() {
        return alarmId.hashCode();
    }

    void setAlarm(){
        if (context == null) return;
        int timeout = idleTimeout(context);
        if (timeout < 1) return;
        Logger.logDebug(LOG_TAG, " set alarm in "+timeout+" minutes");
        PendingIntent pi = PendingIntent.getBroadcast(context, alarmId(), alarmIntent(), alarmFlags);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout * 60 * 1000, pi);
    }

    void cancelAlarm( ){
        if (context == null) return;
        int timeout = idleTimeout(context);
        if (timeout < 1) return;
        Logger.logDebug(LOG_TAG, "  cancel  alarm ");
        PendingIntent pi = PendingIntent.getBroadcast(context , alarmId(), alarmIntent(), alarmFlags);
        am.cancel(pi);
    }

    void alarm(){
        Logger.logDebug(LOG_TAG, "  ALARM shut down  battery level   " + batteryLevel(context) + "%");
        stopServiceIntent();
    }

    int idleTimeout(Context context){
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("config", Context.MODE_MULTI_PROCESS);
            return sharedPref.getInt("idle_timeout", 0);
        } catch (Exception e) {
            return 0;
        }
    }

    int batteryLevel(Context context){
        Intent bs = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return bs.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    }

    void stopServiceIntent(){
        Intent exitIntent = new Intent(context, TermuxService.class).setAction(TERMUX_SERVICE.ACTION_STOP_SERVICE);
        context.startService(exitIntent);
    }

    public synchronized void onActionBootCompleted(@NonNull Context context, @NonNull Intent intent) {
        TermuxShellManager.onActionBootCompleted(context, intent);
    }

    public synchronized void onActionPackageUpdated(@NonNull Context context, @NonNull Intent intent) {
        Uri data = intent.getData();
        if (data != null && TermuxUtils.isUriDataForTermuxPluginPackage(data)) {
            Logger.logDebug(LOG_TAG, intent.getAction().replaceAll("^android.intent.action.", "") +
                " event received for \"" + data.toString().replaceAll("^package:", "") + "\"");
            if (TermuxFileUtils.isTermuxFilesDirectoryAccessible(context, false, false) == null)
                TermuxShellEnvironment.writeEnvironmentToFile(context);
        }
    }



    /**
     * Register {@link SystemEventReceiver} to listen to {@link Intent#ACTION_PACKAGE_ADDED},
     * {@link Intent#ACTION_PACKAGE_REMOVED} and {@link Intent#ACTION_PACKAGE_REPLACED} broadcasts.
     * They must be registered dynamically and cannot be registered implicitly in
     * the AndroidManifest.xml due to Android 8+ restrictions.
     *
     *  https://developer.android.com/guide/components/broadcast-exceptions
     */
    public synchronized static void registerPackageUpdateEvents(@NonNull Context c) {
        Logger.logDebug(LOG_TAG, "registerPackageUpdateEvents");
        context = c;
        am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(getInstance(), intentFilter);
    }

    public synchronized static void unregisterPackageUpdateEvents(@NonNull Context context) {
        Logger.logDebug(LOG_TAG, "unregisterPackageUpdateEvents");
        context.unregisterReceiver(getInstance());
    }

}

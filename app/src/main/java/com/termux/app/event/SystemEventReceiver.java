package com.termux.app.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;
import com.termux.shared.termux.shell.TermuxShellManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

public class SystemEventReceiver extends BroadcastReceiver {

    private static SystemEventReceiver mInstance;
    private static TermuxService mService;

    private static final String LOG_TAG = "SystemEventReceiver";

    public static synchronized SystemEventReceiver getInstance() {
        if (mInstance == null) {
            mInstance = new SystemEventReceiver();
        }
        return mInstance;
    }

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null)
            return;
        Logger.logDebug(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));
        String action = intent.getAction();
        if (action == null)
            return;
        switch(action) {
            case Intent.ACTION_BOOT_COMPLETED:
                onActionBootCompleted(context, intent);
                break;
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REMOVED:
            case Intent.ACTION_PACKAGE_REPLACED:
                onActionPackageUpdated(context, intent);
                break;
            case Intent.ACTION_SCREEN_OFF:
                screenOff(context, "alarm");
                break;
            case Intent.ACTION_SCREEN_ON:
                screenOn(context);
                break;
            case "alarm":
                alarm(context);
                break;
            case "alarm2":
                alarm2(context);
                break;
            case "alarm3":
                alarm3(context);
                break;
            default:
                Logger.logError(LOG_TAG, "Invalid action \"" + action + "\" passed to " + LOG_TAG);
        }
    }

    void screenOff(Context context, String action){
        int timeout = idleTimeout(context);
        Logger.logDebug(LOG_TAG, " set alarm in "+timeout+" minutes"
                +" action="+action
                ); 
        if (timeout < 1) return;
        Intent intent = new Intent(context, SystemEventReceiver.class);
        intent.setAction("alarm");
        int requestCode = 99;
        int flags = 0;
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout * 60 * 1000, pi);

    }


    void screenOn(Context context){
        int timeout = idleTimeout(context);
        Logger.logDebug(LOG_TAG, "screen on idle timeout="
                +timeout
                +" battery level   " + batteryLevel(context) + "%"
                ); 
        if (timeout < 1) return;
        Logger.logDebug(LOG_TAG, "screen on cancel  alarm "); 
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SystemEventReceiver.class);
        int requestCode = 99;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getBroadcast(context , requestCode, intent, flags);
        am.cancel(pi);
    }

    void alarm(Context context){
        Logger.logDebug(LOG_TAG, "  ALARM 1 service intent battery level   " + batteryLevel(context) + "%"); 
        screenOff(context, "alarm2");
        stopServiceIntent(context);
    }
    void alarm2(Context context){
        Logger.logDebug(LOG_TAG, "  ALARM 2 stop service battery level   " + batteryLevel(context) + "%"); 
        stopService(context);
    }
    void alarm3(Context context){
        Logger.logDebug(LOG_TAG, "  ALARM 3 kill app battery level   " + batteryLevel(context) + "%"); 
        killApp();
    }

    int idleTimeout(Context context){
        // TermuxAppSharedPreferences mPreferences = TermuxAppSharedPreferences.build(context, true);
        // int timeout = mPreferences.idleTimeout();
        SharedPreferences sharedPref = context.getSharedPreferences("config", Context.MODE_MULTI_PROCESS);
        return sharedPref.getInt("idle_timeout", 0);
    }

    public static void setService(TermuxService s) {
        mService = s;
    }

    int batteryLevel(Context context){
        Intent bs = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return bs.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    }

    void killApp() {
        int pid = android.os.Process.myPid();
        try {
            android.system.Os.kill(pid, android.system.OsConstants.SIGKILL);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, e.toString());
        }
    }

    void stopService(Context context){
        mService.actionStopService();
    }

    void stopServiceIntent(Context context){
        Intent exitIntent = new Intent(context, TermuxService.class).setAction(TERMUX_SERVICE.ACTION_STOP_SERVICE);
        PendingIntent.getService(context, 99, exitIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    public synchronized void onActionBootCompleted(@NonNull Context context, @NonNull Intent intent) {
        TermuxShellManager.onActionBootCompleted(context, intent);
    }

    public synchronized void onActionPackageUpdated(@NonNull Context context, @NonNull Intent intent) {
        Uri data = intent.getData();
        if (data != null && TermuxUtils.isUriDataForTermuxPluginPackage(data)) {
            Logger.logDebug(LOG_TAG, intent.getAction().replaceAll("^android.intent.action.", "") + " event received for \"" + data.toString().replaceAll("^package:", "") + "\"");
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
    public synchronized static void registerPackageUpdateEvents(@NonNull Context context) {
        Logger.logDebug(LOG_TAG, "registerPackageUpdateEvents");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        // intentFilter.addDataScheme("package");
        context.registerReceiver(getInstance(), intentFilter);
    }

    public synchronized static void unregisterPackageUpdateEvents(@NonNull Context context) {
        Logger.logDebug(LOG_TAG, "unregisterPackageUpdateEvents");
        context.unregisterReceiver(getInstance());
    }
}

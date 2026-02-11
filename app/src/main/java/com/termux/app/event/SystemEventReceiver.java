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
// import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

public class SystemEventReceiver extends BroadcastReceiver {

    private static SystemEventReceiver mInstance;
    // private static TermuxService service;
    private static Context context;
    // static alarmId = 99;
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

    // int alarmId(Intent i) {
        // return i.getAction().hashCode();
    int alarmId() {
        return alarmId.hashCode(); 
    }

    void setAlarm(){
        int timeout = idleTimeout(context);
        Logger.logDebug(LOG_TAG, " set alarm in "+timeout+" minutes  battery level   " + batteryLevel());
        if (timeout < 1) return;
        // int flags = PendingIntent.FLAG_MUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(context, alarmId(), alarmIntent(), alarmFlags);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout * 60 * 1000, pi);
    }

    void cancelAlarm( ){
        int timeout = idleTimeout(context);
        if (timeout < 1) return;
        Logger.logDebug(LOG_TAG, "  cancel  alarm "); 
        // int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_NO_CREATE;
        PendingIntent pi = PendingIntent.getBroadcast(context , alarmId(), alarmIntent(), alarmFlags);
        // if (pi != null)
        am.cancel(pi);
    }

    void alarm(){
        Logger.logDebug(LOG_TAG, "  ALARM shut down  battery level   " + batteryLevel());
        stopServiceIntent();
        // stopService();
        // killApp();
    }

    int idleTimeout(Context context){
        try {
            // TermuxAppSharedPreferences mPreferences = TermuxAppSharedPreferences.build(context, true);
            // int timeout = mPreferences.idleTimeout();
            SharedPreferences sharedPref = context.getSharedPreferences("config", Context.MODE_MULTI_PROCESS);
            return sharedPref.getInt("idle_timeout", 0);
        } catch (Exception e) {
            // log(e.toString());
            return 0;
        }
    }

    // public static void setService(TermuxService s) {
    //     mService = s;
    // }

    int batteryLevel(){
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

    // void stopService(){
    //     service.actionStopService();
    // }

    void stopServiceIntent(){
        Intent exitIntent = new Intent(context, TermuxService.class).setAction(TERMUX_SERVICE.ACTION_STOP_SERVICE);
        context.startService(exitIntent);
        // PendingIntent.getService(context, 99, exitIntent, PendingIntent.FLAG_IMMUTABLE);
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
    public synchronized static void registerPackageUpdateEvents(@NonNull Context c) {
    // public synchronized static void registerPackageUpdateEvents(@NonNull TermuxService s) {
        Logger.logDebug(LOG_TAG, "registerPackageUpdateEvents");
        context = c;
        // service = s;
        am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
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

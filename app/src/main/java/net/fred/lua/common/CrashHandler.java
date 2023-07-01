package net.fred.lua.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import net.fred.lua.App;
import net.fred.lua.PathConstants;
import net.fred.lua.R;
import net.fred.lua.common.activity.CrashActivity;
import net.fred.lua.common.utils.DateUtils;
import net.fred.lua.common.utils.FileUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    public static final String EXTRA_ERROR_CONTENT = "ErrorContent";

    public File errorSaveDir;
    private Context ctx;
    private Thread.UncaughtExceptionHandler defaultExceptionHandler;
    private static CrashHandler instance;
    private boolean showError;

    public void install(Context ctx) {
        this.ctx = ctx;
        this.defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        errorSaveDir = new File(PathConstants.CRASH_FILE_SAVE_DIR);
        showError = true;
        Logger.i("Crash handler installed in package: " + ctx.getPackageName());
    }

    public void showError(boolean bool) {
        this.showError = bool;
    }

    public static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    private String getThrowableMessages(Throwable ta) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ta.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    private void startCrashActivity(String content) {
        Intent intent = new Intent(this.ctx, CrashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_ERROR_CONTENT, content);
        ctx.startActivity(intent);
    }

    private StringBuilder writeInfoToSdCard(Thread p1, Throwable p2) {
        StringBuilder sb = new StringBuilder();
        String versionName = "Unknown";
        long versionCode = 0;
        if (ctx != null) { //可能在没有install时调用
            try {
                PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
                versionName = packageInfo.packageName;
                versionCode = Build.VERSION.SDK_INT >= 28 ?
                        packageInfo.getLongVersionCode() :
                        packageInfo.versionCode;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        String time = DateUtils.getCurrentTimeString();

        sb.append("************* Crash Head ****************\n");
        sb.append("Time Of Crash      : ").append(time).append("\n");
        sb.append("Device Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        sb.append("Device Model       : ").append(Build.MODEL).append("\n");
        sb.append("Android Version    : ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("Android SDK        : ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("App VersionName    : ").append(versionName).append("\n");
        sb.append("App VersionCode    : ").append(versionCode).append("\n");
        sb.append("Crash Thread       : ").append(p1.getName()).append("\n");
        sb.append("************* Crash Head ****************\n\n");
        sb.append(getThrowableMessages(p2));

        FileUtils.writeFile(new File(errorSaveDir, time + "-crash.log"), sb.toString());
        return sb;
    }

    @Override
    public void uncaughtException(Thread p1, Throwable p2) {
        try {
            Logger.e("--Making Crash--");
            StringBuilder sb = writeInfoToSdCard(p1, p2);
            if (showError && ctx != null) {
                Logger.i("Starting a new activity");
                startCrashActivity(sb.toString());
                showError = false;
            }
            Logger.i("Killing self");
            App.forceKillSelf();
        } catch (Throwable e) {
            if (this.defaultExceptionHandler != null)
                this.defaultExceptionHandler.uncaughtException(p1, p2);
        }
    }

    /**
     * Usually use in try-catch
     *
     * @param exception The exception you want to deal.
     */
    public static void fastHandleException(Throwable exception) {
        StringBuilder sb = getInstance().writeInfoToSdCard(Thread.currentThread(), exception);
        Context ctx = getInstance().ctx;
        new AlertDialog.Builder(ctx)
                .setTitle(ctx.getString(R.string.unknown_exception_happened))
                .setMessage(sb.toString())
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }
}

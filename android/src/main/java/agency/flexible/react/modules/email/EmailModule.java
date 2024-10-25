package agency.flexible.react.modules.email;

import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class EmailModule extends ReactContextBaseJavaModule {

    public EmailModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "Email";
    }

    @ReactMethod
    public void getEmailClients(final Promise promise) {
        Intent emailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"));
        PackageManager pm = getCurrentActivity().getPackageManager();
        List<ResolveInfo> resInfo = pm.queryIntentActivities(emailIntent, 0);
        if (resInfo.size() > 0) {
            WritableArray emailApps = new WritableNativeArray();
            for (ResolveInfo ri : resInfo) {
                String packageName = ri.activityInfo.packageName;
                emailApps.pushString(packageName);
            }
            promise.resolve(emailApps);
        } else {
            promise.reject("NoEmailAppsAvailable", "No email apps available");
        }
    }

    @ReactMethod
    public void open(final String title, final boolean newTask, final Promise promise) {
        Intent emailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"));
        PackageManager pm = getCurrentActivity().getPackageManager();

        List<ResolveInfo> resInfo = pm.queryIntentActivities(emailIntent, 0);
        if (resInfo.size() > 0) {
            ResolveInfo ri = resInfo.get(0);
            // First create an intent with only the package name of the first registered email app
            // and build a picked based on it
            Intent intentChooser = createLaunchIntent(ri, newTask);

            if (intentChooser != null) {
                Intent openInChooser = Intent.createChooser(intentChooser, title);

                // Then create a list of LabeledIntent for the rest of the registered email apps
                List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();
                for (int i = 1; i < resInfo.size(); i++) {
                    // Extract the label and repackage it in a LabeledIntent
                    ri = resInfo.get(i);
                    String packageName = ri.activityInfo.packageName;
                    Intent intent = createLaunchIntent(ri, newTask);

                    if (intent != null) {
                        intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
                    }
                }

                LabeledIntent[] extraIntents = intentList.toArray(new LabeledIntent[intentList.size()]);
                // Add the rest of the email apps to the picker selection
                openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
                setNewTaskFlag(openInChooser, newTask);
                getCurrentActivity().startActivity(openInChooser);
            }

            promise.resolve(true);
        } else {
            promise.reject("NoEmailAppsAvailable", "No email apps available");
        }
    }

    @ReactMethod
    public void openWith(String packageName, final boolean newTask, final Promise promise) {
        Intent launchIntent = new Intent(Intent.ACTION_SENDTO);
        launchIntent.setPackage(packageName);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (launchIntent.resolveActivity(getCurrentActivity().getPackageManager()) != null) {
            getCurrentActivity().startActivity(launchIntent);
            promise.resolve("Success");
        } else {
            promise.reject("AppNotFound", "Application not found");
        }
    }

    @ReactMethod
    public void composeWith(String packageName, final String title, final String to, final String subject, final String body, final String cc, final String bcc, final Promise promise) {
        Intent launchIntent = new Intent(Intent.ACTION_SENDTO);
        launchIntent.setPackage(packageName);

        String uriText = "mailto:" + Uri.encode(to) +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body);
        if(cc != null) {
            uriText += "&cc=" + Uri.encode(cc);
        }
        if(bcc != null) {
            uriText += "&bcc=" + Uri.encode(bcc);
        }

        Uri uri = Uri.parse(uriText);

        launchIntent.setData(uri);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (launchIntent.resolveActivity(getCurrentActivity().getPackageManager()) != null) {
            getCurrentActivity().startActivity(launchIntent);
            promise.resolve("Success");
        } else {
            promise.reject("AppNotFound", "Application not found");
        }
    }

    @ReactMethod
    public void compose(final String title, final String to, final String subject, final String body, final String cc, final String bcc) {
        Intent send = new Intent(Intent.ACTION_SENDTO);
        String uriText = "mailto:" + Uri.encode(to) +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body);
        if(cc != null) {
            uriText += "&cc=" + Uri.encode(cc);
        }
        if(bcc != null) {
            uriText += "&bcc=" + Uri.encode(bcc);
        }
        Uri uri = Uri.parse(uriText);

        send.setData(uri);
        Intent chooserIntent = Intent.createChooser(send, title);
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getReactApplicationContext().startActivity(chooserIntent);
    }

    @Nullable
    private Intent createLaunchIntent(final ResolveInfo resolveInfo, final boolean newTask) {
        PackageManager packageManager = getCurrentActivity().getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName);
        if (launchIntent != null) {
            // getLaunchIntentForPackage internally adds the FLAG_ACTIVITY_NEW_TASK.
            // See: https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/app/ApplicationPackageManager.java#L233
            // So if we want to remove it, we must explicitly unset it.
            setNewTaskFlag(launchIntent, newTask);
        }
        return launchIntent;
    }

    private void setNewTaskFlag(final Intent intent, final boolean newTask) {
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }
}

package com.trademarked.gamemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Receiver for package update broadcast.
 */
public final class PackageReceiver extends BroadcastReceiver {

    private static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    private static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
    private static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case ACTION_PACKAGE_ADDED:
            case ACTION_PACKAGE_REPLACED:
                updatePackageRecord(context, getPackageName(intent), false /* removing */);
                break;
            case ACTION_PACKAGE_REMOVED:
                updatePackageRecord(context, getPackageName(intent), true /* removing */);
                break;
            default:
                // Do nothing if we don't recognize the intent.
                break;
        }
    }

    private String getPackageName(Intent intent) {
        Uri data = intent.getData();
        return data.getSchemeSpecificPart();
    }

    private void updatePackageRecord(Context context, String packageName, boolean removing) {
        GameScanner scanner = new GameScanner(context);
        if (removing) {
            scanner.removeGameRecord(packageName);
        } else {
            scanner.updateGameRecord(packageName);
        }
    }
}

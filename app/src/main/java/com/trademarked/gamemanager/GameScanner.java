package com.trademarked.gamemanager;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;

import com.trademarked.gamemanager.models.GameRecord;
import com.trademarked.gamemanager.models.ScanRecord;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Helper class to scan the package manager for games.
 */
public final class GameScanner {

    private static final String GAME_METADATA = "com.google.android.gms.games.APP_ID";
    private static final String PKG_PLAY_GAMES = "com.google.android.play.games";
    private static final long SCAN_INTERVAL_MILLIS = DateUtils.MINUTE_IN_MILLIS;

    // TODO: Inject instead.
    private final Context mContext;
    private final Executor mExecutor;

    public GameScanner(Context context) {
        mContext = context.getApplicationContext();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    public void performScan(final boolean forceRefresh ) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try (Realm realm = Realm.getDefaultInstance()) {
                    performScan_inner(forceRefresh, realm);
                }
            }
        });
    }

    void performScan_inner(boolean forceRefresh, Realm realm) {
        // Check to see if we've done a scan recently. Generally, we should only need to do this
        // once per device, but it doesn't hurt to do it every so often when the user opens the
        // app (in case we miss a package install/etc).
        long current_millis = System.currentTimeMillis();
        long delta = current_millis - getLastScanTime(realm);
        if (!forceRefresh && delta < SCAN_INTERVAL_MILLIS) {
            return;
        }

        // Retrieve the usage stat map. This is for all packages on the device.
        Map<String, UsageStats> stats = getUsageStats(realm);

        // Loop through all packages on the device.
        List<GameRecord> records = new ArrayList<>();
        PackageManager pm = mContext.getPackageManager();
        int flags = PackageManager.GET_META_DATA;
        List<PackageInfo> packages = pm.getInstalledPackages(flags);
        for (PackageInfo pkg : packages) {
            GameRecord record = getRecord(pm, pkg, stats.get(pkg.packageName));
            if (record != null) {
                records.add(record);
            }
        }

        // Now insert the Realm objects into the database. Note that we convert to JSON in order
        // to handle updating previously found records.
        realm.beginTransaction();
        realm.createOrUpdateAllFromJson(GameRecord.class, toJson(records));
        ScanRecord scanRecord = new ScanRecord();
        scanRecord.timestamp_millis = current_millis;
        scanRecord.complete = true;
        realm.insert(scanRecord);
        realm.commitTransaction();
    }

    private JSONArray toJson(List<GameRecord> records) {
        try {
            JSONArray array = new JSONArray();
            for (GameRecord record : records) {
                array.put(record.toJson());
            }
            return array;
        } catch (JSONException jsone) {
            Timber.d(jsone, "Failed to parse to JSON");
            return null;
        }
    }

    public void updateGameRecord(final String packageName) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try (Realm realm = Realm.getDefaultInstance()) {
                    updateGameRecord_inner(packageName, realm);
                }
            }
        });
    }

    void updateGameRecord_inner(String packageName, Realm realm) {
        PackageManager pm = mContext.getPackageManager();
        try {
            Map<String, UsageStats> stats = getUsageStats(realm);
            PackageInfo pkg = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            GameRecord record = getRecord(pm, pkg, stats.get(packageName));
            if (record != null) {
                realm.beginTransaction();
                realm.createOrUpdateObjectFromJson(GameRecord.class, record.toJson());
                realm.commitTransaction();
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            // Nothing to do here - move on!
        } catch (JSONException jsone) {
            Timber.d(jsone, "Failed to update object!");
        }
    }

    private GameRecord getRecord(PackageManager pm, PackageInfo pkg, UsageStats usageStats) {
        // Exclude the Google Play Games package - it pretends to be a game, but isn't really.
        if (pkg == null || PKG_PLAY_GAMES.equals(pkg.packageName)) {
            return null;
        }

        // An application is a game if it is marked as such, or if it has the Google Play
        // services metadata field for games. Most packages on most devices will not be games.
        boolean isGame = false;
        ApplicationInfo appInfo = pkg.applicationInfo;
        if ((appInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
            isGame = true;
        }

        Bundle bundle = appInfo.metaData;
        String appId = null;
        if (bundle != null) {
            appId = bundle.getString(GAME_METADATA);
            if (appId != null) {
                isGame = true;
            }
        }

        // If this isn't a game, we're done.
        if (!isGame) {
            return null;
        }

        // Assemble the record and return.
        GameRecord record = new GameRecord();
        record.package_name = pkg.packageName;
        record.display_name = (String) pm.getApplicationLabel(appInfo);
        record.installed_millis = pkg.firstInstallTime;
        if (usageStats != null) {
            record.played_millis = usageStats.getLastTimeUsed();
        }
        return record;
    }

    private Map<String, UsageStats> getUsageStats(Realm realm) {
        long currentMillis = System.currentTimeMillis();
        UsageStatsManager usageManager =
                (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        return usageManager.queryAndAggregateUsageStats(getLastScanTime(realm), currentMillis);
    }

    private long getLastScanTime(Realm realm) {
        RealmResults<ScanRecord> scans = realm.where(ScanRecord.class)
                .equalTo("complete", true)
                .findAllSorted("timestamp_millis");
        ScanRecord previous = scans.isEmpty() ? null : scans.last();
        return previous != null ? previous.timestamp_millis : 0;
    }

    public void removeGameRecord(final String packageName) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try (Realm realm = Realm.getDefaultInstance()) {
                    removeGameRecord_inner(packageName, realm);
                }
            }
        });
    }

    void removeGameRecord_inner(String packageName, Realm realm) {
        realm.beginTransaction();
        realm.where(GameRecord.class)
                .equalTo("package_name", packageName)
                .findAll()
                .deleteAllFromRealm();
        realm.commitTransaction();
    }
}

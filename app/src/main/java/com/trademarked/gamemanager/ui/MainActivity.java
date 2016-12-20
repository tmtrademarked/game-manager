package com.trademarked.gamemanager.ui;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.trademarked.gamemanager.Constants;
import com.trademarked.gamemanager.GameScanner;
import com.trademarked.gamemanager.R;
import com.trademarked.gamemanager.models.Permissions;
import com.trademarked.gamemanager.models.Preferences;
import com.trademarked.gamemanager.ui.fragments.AllGamesFragment;
import com.trademarked.gamemanager.ui.fragments.InstalledGamesFragment;
import com.trademarked.gamemanager.ui.fragments.PlayedGamesFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;

public class MainActivity extends AppCompatActivity implements
        BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String KEY_CURRENT_TAB_ID = "current_tab_id";

    @BindView(R.id.ad_view) AdView mAdView;
    @BindView(R.id.nav_view) BottomNavigationView mBottomNav;
    private GameScanner mScanner;
    private Realm mRealm;
    private int mCurrentTabId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mScanner = new GameScanner(getApplicationContext());
        mRealm = Realm.getDefaultInstance();

        // Initialize the bottom nav properly.
        int tabIdToSelect = initCurrentTabId(savedInstanceState);
        mBottomNav.setOnNavigationItemSelectedListener(this);
        mBottomNav.findViewById(tabIdToSelect).performClick();

        // Load up the ad if we can.
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private int initCurrentTabId(Bundle savedInstanceState) {
        // Initialize from shortcut if relevant.
        String action = getIntent().getAction();
        if (Constants.SHORTCUT_ALL_GAMES.equals(action)) {
            mCurrentTabId = R.id.nav_all;
        } else if (Constants.SHORTCUT_RECENTLY_INSTALLED.equals(action)) {
            mCurrentTabId = R.id.nav_recently_installed;
        } else if (Constants.SHORTCUT_RECENTLY_PLAYED.equals(action)) {
            mCurrentTabId = R.id.nav_recently_played;
        }

        // If we have a previous state, use it.
        if (savedInstanceState != null) {
            mCurrentTabId = savedInstanceState.getInt(KEY_CURRENT_TAB_ID);
        }

        // If we still don't have one, check the database.
        if (mCurrentTabId <= 0) {
            Preferences prefs = mRealm.where(Preferences.class).findFirst();
            if (prefs != null) {
                mCurrentTabId = prefs.current_tab_id;
            }
        }

        // If all else fails, just start with the all tab.
        if (mCurrentTabId <= 0) {
            mCurrentTabId = R.id.nav_all;
        }

        // Store the tab ID and return.
        storeCurrentTabId();
        return mCurrentTabId;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(KEY_CURRENT_TAB_ID, mCurrentTabId);
        storeCurrentTabId();
        super.onSaveInstanceState(savedInstanceState);
    }

    private void storeCurrentTabId() {
        mRealm.beginTransaction();
        Preferences prefs = mRealm.where(Preferences.class).findFirst();
        if (prefs == null) {
            prefs = new Preferences();
        }
        prefs.current_tab_id = mCurrentTabId;
        mRealm.insertOrUpdate(prefs);
        mRealm.commitTransaction();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check to see if we have the stats usage permission, if we need to check.
        if (shouldCheckUsage() && !hasUsagePermission()) {
            PermissionDialog dialog = new PermissionDialog();
            dialog.show(getSupportFragmentManager(), "permission_dialog");
        }

        // Make sure our game records are up-to-date if we can.
        mScanner.performScan(false /* forceRefresh */);
    }

    private boolean shouldCheckUsage() {
        Permissions permissions = mRealm.where(Permissions.class).findFirst();
        if (permissions == null) {
            return true;
        }
        return permissions.usage_stats != Permissions.CheckState.IGNORED;
    }

    void showUsagePermission() {
        setShouldCheckUsagePermission(true /* allow */);
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    void setShouldCheckUsagePermission(boolean allow) {
        mRealm.beginTransaction();
        Permissions permissions = mRealm.where(Permissions.class).findFirst();
        if (permissions == null) {
            permissions = new Permissions();
        }
        permissions.usage_stats = allow ? Permissions.CheckState.GRANTED
                : Permissions.CheckState.IGNORED;
        mRealm.insertOrUpdate(permissions);
        mRealm.commitTransaction();
    }

    private boolean hasUsagePermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(),
                getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_all:
                attachFragment(AllGamesFragment.class.getName());
                break;
            case R.id.nav_recently_installed:
                attachFragment(InstalledGamesFragment.class.getName());
                break;
            case R.id.nav_recently_played:
                attachFragment(PlayedGamesFragment.class.getName());
                break;
        }

        // Set the current ID and store it.
        mCurrentTabId = item.getItemId();
        storeCurrentTabId();
        return true;
    }

    private void attachFragment(String fragmentName) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = Fragment.instantiate(this, fragmentName);
        manager.beginTransaction().replace(R.id.content_main, fragment).commit();
    }

    public Realm getRealm() {
        return mRealm;
    }

    public void forceScan() {
        mScanner.performScan(true /* forceRefresh */);
    }

    public static final class PermissionDialog extends DialogFragment implements
            DialogInterface.OnClickListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.msg_grant_stats_permission)
                    .setNegativeButton(R.string.msg_no_thanks, this)
                    .setPositiveButton(R.string.msg_allow, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            MainActivity activity = (MainActivity) getActivity();
            if (!isAdded()) {
                return;
            }

            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    activity.setShouldCheckUsagePermission(false /* allow */);
                    break;
                case DialogInterface.BUTTON_POSITIVE:
                    activity.showUsagePermission();
                    break;
            }
        }
    }
}

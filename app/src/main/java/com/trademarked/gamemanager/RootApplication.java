package com.trademarked.gamemanager;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import timber.log.Timber;

/**
 * Base application for the Game Manager app.
 */
public final class RootApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO - move to dagger modules when ready to convert.
        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);

        // Initialize Timber.
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}

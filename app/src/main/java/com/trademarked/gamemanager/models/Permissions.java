package com.trademarked.gamemanager.models;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static com.trademarked.gamemanager.models.Permissions.CheckState.GRANTED;
import static com.trademarked.gamemanager.models.Permissions.CheckState.IGNORED;
import static com.trademarked.gamemanager.models.Permissions.CheckState.UNKNOWN;

/**
 * Model representing the state of our permission checking.
 */
public class Permissions extends RealmObject {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UNKNOWN, GRANTED, IGNORED})
    public @interface CheckState {
        int UNKNOWN = 0;
        int GRANTED = 1;
        int IGNORED = 2;
    }

    // There's only one of these - therefore, we can hardcode the ID.
    @PrimaryKey
    public int id = 1;
    public int usage_stats;
}

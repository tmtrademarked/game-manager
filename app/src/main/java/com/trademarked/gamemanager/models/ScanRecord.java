package com.trademarked.gamemanager.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Basic record indicating when a scan was last performed.
 */
public class ScanRecord extends RealmObject {
    @PrimaryKey
    public long timestamp_millis;
    public boolean complete;
}

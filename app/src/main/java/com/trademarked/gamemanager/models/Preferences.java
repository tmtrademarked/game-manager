package com.trademarked.gamemanager.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Model object used to persist data about the user's preferences.
 */
public class Preferences extends RealmObject {
    // This is a singleton object, so we hardcode the id.
    @PrimaryKey
    public int id = 1;
    public int current_tab_id;
}

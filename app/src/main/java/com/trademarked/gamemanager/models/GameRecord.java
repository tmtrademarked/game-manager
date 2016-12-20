package com.trademarked.gamemanager.models;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Model object representing a game on the device.
 */
public class GameRecord extends RealmObject {
    @PrimaryKey
    public String package_name;
    // TODO - sorting name?
    public String display_name;
    public long installed_millis;
    public long played_millis;

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.putOpt("package_name", package_name);
        obj.putOpt("display_name", display_name);
        if (installed_millis > 0) {
            obj.put("installed_millis", installed_millis);
        }
        if (played_millis > 0) {
            obj.put("played_millis", played_millis);
        }
        return obj;
    }
}

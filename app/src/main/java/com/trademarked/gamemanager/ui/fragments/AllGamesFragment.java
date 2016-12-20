package com.trademarked.gamemanager.ui.fragments;

import com.trademarked.gamemanager.Constants;
import com.trademarked.gamemanager.models.GameRecord;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Fragment for rendering recently installed games.
 */
public final class AllGamesFragment extends BaseGameFragment {

    @Override
    protected String getShortcutId() {
        return Constants.SHORTCUT_ALL_GAMES;
    }

    @Override
    protected RealmResults<GameRecord> getRecords(Realm realm) {
        return realm.where(GameRecord.class).findAllSorted("display_name");
    }
}

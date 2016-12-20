package com.trademarked.gamemanager.ui.fragments;

import com.trademarked.gamemanager.Constants;
import com.trademarked.gamemanager.models.GameRecord;
import com.trademarked.gamemanager.ui.fragments.BaseGameFragment;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Fragment for rendering recently installed games.
 */
public final class PlayedGamesFragment extends BaseGameFragment {

    @Override
    protected String getShortcutId() {
        return Constants.SHORTCUT_RECENTLY_PLAYED;
    }

    @Override
    protected RealmResults<GameRecord> getRecords(Realm realm) {
        return realm.where(GameRecord.class).findAllSorted("played_millis", Sort.DESCENDING);
    }
}

package com.trademarked.gamemanager.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.trademarked.gamemanager.BR;
import com.trademarked.gamemanager.R;
import com.trademarked.gamemanager.databinding.GameCardBinding;
import com.trademarked.gamemanager.models.GameRecord;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Adapter used to render game cards.
 */
public final class GameCardAdapter extends RecyclerView.Adapter<GameCardAdapter.BindingHolder>
        implements RealmChangeListener<RealmResults<GameRecord>> {

    public interface OnDataChangeListener {
        void onDataChanged();
    }

    private final Activity mActivity;
    private final RealmResults<GameRecord> mRealmData;
    private final OnDataChangeListener mListener;

    public GameCardAdapter(Activity activity, RealmResults<GameRecord> realmData,
            OnDataChangeListener listener) {
        mActivity = activity;
        mRealmData = realmData;
        mRealmData.addChangeListener(this);
        mListener = listener;
    }

    @Override
    public BindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        GameCardBinding binding = DataBindingUtil.inflate(inflater, R.layout.game_card, parent,
                false /* attachToRoot */);
        return new BindingHolder(this, binding);
    }

    @Override
    public int getItemCount() {
        return mRealmData.isValid() ? mRealmData.size() : 0;
    }

    @Override
    public void onBindViewHolder(BindingHolder holder, int position) {
        // Bind the view to the data.
        GameRecord record = mRealmData.get(position);
        holder.getBinding().setVariable(BR.game, record);
        holder.getBinding().setVariable(BR.adapter, this);
        holder.getBinding().executePendingBindings();
    }

    public void launchGame(GameRecord record) {
        PackageManager pm = mActivity.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(record.package_name);
        mActivity.startActivity(intent);
        mActivity.finish();
    }

    public void launchSettings(GameRecord record) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + record.package_name));
        mActivity.startActivity(intent);
    }

    @Override
    public void onChange(RealmResults<GameRecord> element) {
        notifyDataSetChanged();
        mListener.onDataChanged();
    }

    public static final class BindingHolder extends RecyclerView.ViewHolder implements
            View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        private GameCardAdapter adapter;
        private GameCardBinding binding;

        public BindingHolder(GameCardAdapter adapter, GameCardBinding binding) {
            super(binding.getRoot());
            this.adapter = adapter;
            this.binding = binding;
            binding.cardView.setOnCreateContextMenuListener(this);
        }

        public GameCardBinding getBinding() {
            return binding;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            GameRecord game = binding.getGame();
            if (game == null) {
                return false;
            }

            switch (item.getItemId()) {
                case R.id.card_menu_app_info:
                    adapter.launchSettings(game);
                    return true;
                case R.id.card_menu_launch_game:
                    adapter.launchGame(game);
                    return true;
            }
            Timber.i("Unexpected menu item %s", item.getTitle());
            return false;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenu.ContextMenuInfo menuInfo) {
            MenuItem launchItem = menu.add(Menu.NONE /* groupId */, R.id.card_menu_launch_game,
                    Menu.NONE /* orderId */, R.string.card_menu_launch_game);
            launchItem.setOnMenuItemClickListener(this);
            MenuItem settingsItem = menu.add(Menu.NONE /* groupId */, R.id.card_menu_app_info,
                    Menu.NONE /* orderId */, R.string.card_menu_app_info);
            settingsItem.setOnMenuItemClickListener(this);
        }
    }
}

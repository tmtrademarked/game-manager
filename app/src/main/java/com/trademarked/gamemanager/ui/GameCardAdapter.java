package com.trademarked.gamemanager.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.trademarked.gamemanager.BR;
import com.trademarked.gamemanager.R;
import com.trademarked.gamemanager.models.GameRecord;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_card, parent,
                false /* attachToRoot */);
        return new BindingHolder(view);
    }

    @Override
    public int getItemCount() {
        return mRealmData.isValid() ? mRealmData.size() : 0;
    }

    @Override
    public void onBindViewHolder(BindingHolder holder, int position) {
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

    @Override
    public void onChange(RealmResults<GameRecord> element) {
        notifyDataSetChanged();
        mListener.onDataChanged();
    }

    public static final class BindingHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;

        public BindingHolder(View rowView) {
            super(rowView);
            binding = DataBindingUtil.bind(rowView);
        }

        public ViewDataBinding getBinding() {
            return binding;
        }
    }
}

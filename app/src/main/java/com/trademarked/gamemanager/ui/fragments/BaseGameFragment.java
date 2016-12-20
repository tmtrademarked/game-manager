package com.trademarked.gamemanager.ui.fragments;

import android.annotation.TargetApi;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.trademarked.gamemanager.R;
import com.trademarked.gamemanager.models.GameRecord;
import com.trademarked.gamemanager.ui.GameCardAdapter;
import com.trademarked.gamemanager.ui.MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Base class for fragments showing lists of games.
 */
public abstract class BaseGameFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener, GameCardAdapter.OnDataChangeListener {
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mRefreshLayout;
    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    private GameCardAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            reportShortcut();
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void reportShortcut() {
        String shortcutId = getShortcutId();
        if (shortcutId != null) {
            ShortcutManager sm = getContext().getSystemService(ShortcutManager.class);
            sm.reportShortcutUsed(shortcutId);
        }
    }

    protected abstract String getShortcutId();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.base_grid_game_fragment, container,
                false /* attachToRoot */);
        ButterKnife.bind(this, root);

        int columnCount = getColumnCount();
        GridLayoutManager manager = new GridLayoutManager(getContext(), columnCount);
        mRecyclerView.setLayoutManager(manager);
        mRefreshLayout.setOnRefreshListener(this);

        // Load the data from Realm into an adapter.
        RealmResults<GameRecord> records = getRecords(getRealm());
        mAdapter = new GameCardAdapter(getActivity(), records, this);
        mRecyclerView.setAdapter(mAdapter);

        int padding = getCardPadding(columnCount);
        mRecyclerView.addItemDecoration(new EvenSpacedDecorator(padding));
        return root;
    }

    protected abstract RealmResults<GameRecord> getRecords(Realm realm);

    private MainActivity getParent() {
        return (MainActivity) getActivity();
    }

    private Realm getRealm() {
        return getParent().getRealm();
    }

    @Override
    public void onRefresh() {
        getParent().forceScan();
    }

    @Override
    public void onDataChanged() {
        mRefreshLayout.setRefreshing(false);
    }

    private int getColumnCount() {
        Resources res = getContext().getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        float columnWidth = res.getDimension(R.dimen.column_width);
        int columnCount = (int) (displayMetrics.widthPixels / columnWidth);

        // Verify that we still have enough room for our minimum padding.
        int padding = getCardPadding(columnCount);
        int minPadding = res.getDimensionPixelSize(R.dimen.min_card_padding);
        if (padding < minPadding) {
            --columnCount;
        }

        // We can never have less than 1 column. So if we somehow over-constrain this...
        return Math.max(columnCount, 1);
    }

    private int getCardPadding(int columnCount) {
        Resources res = getContext().getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        float spaceForCards = (float) columnCount * res.getDimension(R.dimen.column_width);
        float remainingSpace = displayMetrics.widthPixels - spaceForCards;
        // Padding should be equal on each side, so divide in half.
        return (int) remainingSpace / (2 *columnCount);
    }

    private static final class EvenSpacedDecorator extends RecyclerView.ItemDecoration {

        private final int mPadding;

        public EvenSpacedDecorator(int padding) {
            mPadding = padding;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            GridLayoutManager.LayoutParams layoutParams =
                    (GridLayoutManager.LayoutParams) view.getLayoutParams();
            GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
            float spanSize = layoutParams.getSpanSize();
            float totalSpanSize = layoutManager.getSpanCount();

            float numCols = totalSpanSize / spanSize;
            float colIndex = layoutParams.getSpanIndex() / spanSize;

            float leftPadding = mPadding * ((numCols - colIndex) / numCols);
            float rightPadding = mPadding * ((colIndex + 1) / numCols);

            outRect.left = (int) leftPadding;
            outRect.right = (int) rightPadding;
            outRect.bottom = mPadding;
        }
    }
}

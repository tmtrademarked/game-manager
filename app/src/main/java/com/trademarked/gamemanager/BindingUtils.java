package com.trademarked.gamemanager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.databinding.BindingAdapter;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.trademarked.gamemanager.models.GameRecord;

/**
 * Helper class for data binding utils.
 */
public final class BindingUtils {

    private BindingUtils() {}

    @BindingAdapter({"gameIcon"})
    public static void bindGameIcon(ImageView view, GameRecord record) {
        Context context = view.getContext();
        PackageManager pm = context.getPackageManager();
        try {
            Drawable icon = pm.getApplicationIcon(record.package_name);
            view.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException nnfe) {
            // This should never happen, so just do nothing in this case.
        }
    }
}

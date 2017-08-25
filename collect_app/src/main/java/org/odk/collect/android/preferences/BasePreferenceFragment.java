package org.odk.collect.android.preferences;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.odk.collect.android.R;

import static org.odk.collect.android.preferences.PreferencesActivity.INTENT_KEY_ADMIN_MODE;

public class BasePreferenceFragment extends PreferenceFragment {

    protected AppBarLayout appBarLayout;
    protected Toolbar toolbar;
    private LinearLayout root;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        initToolbar(getPreferenceScreen(), view);

        // removes disabled preferences if in general settings
        if (getActivity() instanceof PreferencesActivity) {
            Bundle args = getArguments();
            if (args != null) {
                final boolean adminMode = getArguments().getBoolean(INTENT_KEY_ADMIN_MODE, false);
                if (!adminMode) {
                    removeAllDisabledPrefs();
                }
            } else {
                removeAllDisabledPrefs();
            }
        }

        super.onViewCreated(view, savedInstanceState);
    }

    // inflates toolbar in the preference fragments
    public void initToolbar(PreferenceScreen preferenceScreen, View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            if (getActivity() instanceof PreferencesActivity) {
                root = (LinearLayout) ((ViewGroup) view.findViewById(android.R.id.list).getRootView()).getChildAt(0);
                appBarLayout = (AppBarLayout) root.findViewById(R.id.appbarLayout);
                toolbar = (Toolbar) appBarLayout.findViewById(R.id.toolbar);

            } else {
                root = (LinearLayout) view.findViewById(android.R.id.list).getParent().getParent();
                appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(R.layout.toolbar, root, false);
                toolbar = (Toolbar) appBarLayout.findViewById(R.id.toolbar);

                inflateToolbar(preferenceScreen.getTitle());
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            root = (LinearLayout) view.findViewById(android.R.id.list).getParent();
            appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(R.layout.toolbar, root, false);
            toolbar = (Toolbar) appBarLayout.findViewById(R.id.toolbar);

            inflateToolbar(preferenceScreen.getTitle());
        }
    }

    private void inflateToolbar(CharSequence title) {
        toolbar.setTitle(title);
        root.addView(appBarLayout, 0);
    }

    private void removeAllDisabledPrefs() {
        DisabledPreferencesRemover preferencesRemover = new DisabledPreferencesRemover((PreferencesActivity) getActivity(), this);
        preferencesRemover.remove(AdminKeys.adminToGeneral);
        preferencesRemover.removeEmptyCategories();
    }
}
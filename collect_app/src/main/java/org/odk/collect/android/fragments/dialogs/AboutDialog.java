package org.odk.collect.android.fragments.dialogs;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.OpenSourceLicensesActivity;
import org.odk.collect.android.utilities.CustomTabHelper;

public class AboutDialog extends DialogFragment implements View.OnClickListener {

    private static final String ODK_WEBSITE = "https://opendatakit.org";
    private static final String ODK_FORUM = "https://forum.opendatakit.org";

    private CustomTabHelper websiteTabHelper;
    private CustomTabHelper forumTabHelper;

    private Uri websiteUri;
    private Uri forumUri;

    @Override
    public void onStart() {
        super.onStart();
        websiteTabHelper.bindCustomTabsService(this.getActivity(), websiteUri);
        forumTabHelper.bindCustomTabsService(this.getActivity(), forumUri);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        websiteTabHelper = new CustomTabHelper();
        forumTabHelper = new CustomTabHelper();
        websiteUri = Uri.parse(ODK_WEBSITE);
        forumUri = Uri.parse(ODK_FORUM);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.about_dialog, container, false);


        view.findViewById(R.id.website).setOnClickListener(this);
        view.findViewById(R.id.forum).setOnClickListener(this);
        view.findViewById(R.id.licenses).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.website:
                if (websiteTabHelper.getPackageName(getActivity()).size() != 0) {
                    CustomTabsIntent customTabsIntent =
                            new CustomTabsIntent.Builder()
                                    .build();
                    customTabsIntent.intent.setPackage(websiteTabHelper.getPackageName(getActivity()).get(0));
                    customTabsIntent.launchUrl(getActivity(), websiteUri);
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ODK_WEBSITE)));
                }
                break;

            case R.id.forum:
                if (forumTabHelper.getPackageName(getActivity()).size() != 0) {
                    CustomTabsIntent customTabsIntent =
                            new CustomTabsIntent.Builder()
                                    .build();
                    customTabsIntent.intent.setPackage(forumTabHelper.getPackageName(getActivity()).get(0));
                    customTabsIntent.launchUrl(getActivity(), forumUri);
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ODK_FORUM)));
                }
                break;

            case R.id.licenses:
                startActivity(new Intent(getActivity().getApplicationContext(),
                        OpenSourceLicensesActivity.class));
                break;
        }
    }
}

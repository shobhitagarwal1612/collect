package org.odk.collect.android.listeners;

import android.view.View;


public interface FormClickListener {

    void editSavedClicked(String formID, int position);

    void sendFinalizedClicked(String formID);

    void updateCount(View view, String formId);

    void viewSentClicked(String formID);

    void itemClicked(int position);
}

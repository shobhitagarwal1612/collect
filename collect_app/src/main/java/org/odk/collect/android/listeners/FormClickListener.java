package org.odk.collect.android.listeners;

import android.view.View;

/**
 * Created by shobhit on 27/7/17.
 */

public interface FormClickListener {

    void editSavedClicked(String formID, int position);

    void sendFinalizedClicked(String formID);

    void updateCount(View view, String formID);

    void viewSentClicked(String formID);
}

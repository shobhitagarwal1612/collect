package org.odk.collect.android.listeners;

import android.view.View;

import org.odk.collect.android.logic.FormGroup;

/**
 * Created by shobhit on 27/7/17.
 */

public interface FormClickListener {

    void editSavedClicked(String formID, int position);

    void sendFinalizedClicked(String formID);

    void updateCount(View view, FormGroup form);

    void viewSentClicked(String formID);
}

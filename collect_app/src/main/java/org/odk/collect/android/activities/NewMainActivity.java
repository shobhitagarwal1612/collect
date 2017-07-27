package org.odk.collect.android.activities;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.odk.collect.android.R;
import org.odk.collect.android.adapters.FormCursorAdapter;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.listeners.DiskSyncListener;
import org.odk.collect.android.listeners.FormClickListener;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.tasks.DiskSyncTask;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.PlayServicesUtil;

import timber.log.Timber;

/**
 * Created by shobhit on 26/7/17.
 */

public class NewMainActivity extends FormListActivity implements DiskSyncListener, AdapterView.OnItemClickListener, FormClickListener, View.OnClickListener {
    private static final String FORM_CHOOSER_LIST_SORTING_ORDER = "formChooserListSortingOrder";

    private static final boolean EXIT = true;
    private static final String syncMsgKey = "syncmsgkey";

    private DiskSyncTask diskSyncTask;
    private FloatingActionButton fab;
    private FloatingActionButton fab1;
    private FloatingActionButton fab2;
    private Animation fabOpen;
    private Animation fabClose;
    private Animation rotateForward;
    private Animation rotateBackward;
    private boolean isFabOpen = false;
    private CardView cardViewAggregate;
    private CardView cardViewGoogleDrive;
    private FrameLayout frameLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // must be at the beginning of any activity that can be called from an external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        setContentView(R.layout.activity_main_layout);
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.enter_data));

        setupAdapter();

        if (savedInstanceState != null && savedInstanceState.containsKey(syncMsgKey)) {
            TextView tv = (TextView) findViewById(R.id.status_text);
            tv.setText((savedInstanceState.getString(syncMsgKey)).trim());
        }

        // DiskSyncTask checks the disk for any forms not already in the content provider
        // that is, put here by dragging and dropping onto the SDCard
        diskSyncTask = (DiskSyncTask) getLastCustomNonConfigurationInstance();
        if (diskSyncTask == null) {
            Timber.i("Starting new disk sync task");
            diskSyncTask = new DiskSyncTask();
            diskSyncTask.setDiskSyncListener(this);
            diskSyncTask.execute((Void[]) null);
        }
        sortingOptions = new String[]{
                getString(R.string.sort_by_name_asc), getString(R.string.sort_by_name_desc),
                getString(R.string.sort_by_date_asc), getString(R.string.sort_by_date_desc),
        };

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        fab2 = (FloatingActionButton) findViewById(R.id.fab2);

        cardViewAggregate = (CardView) findViewById(R.id.cv_aggregate);
        cardViewGoogleDrive = (CardView) findViewById(R.id.cv_gdrive);

        fabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backward);

        fab.setOnClickListener(this);
        fab1.setOnClickListener(this);
        fab2.setOnClickListener(this);

        frameLayout = (FrameLayout) findViewById(R.id.frame_layout);
        frameLayout.getBackground().setAlpha(0);
    }


    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // pass the thread on restart
        return diskSyncTask;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView tv = (TextView) findViewById(R.id.status_text);
        outState.putString(syncMsgKey, tv.getText().toString().trim());
    }


    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // get uri to form
        long idFormsTable = listView.getAdapter().getItemId(position);
        Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, idFormsTable);

        Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick",
                formUri.toString());

        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)) {
            // caller is waiting on a picked form
            setResult(RESULT_OK, new Intent().setData(formUri));
        } else {
            // caller wants to view/edit a form, so launch formentryactivity
            Intent intent = new Intent(Intent.ACTION_EDIT, formUri);
            intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
            startActivity(intent);
        }

        finish();
    }


    @Override
    protected void onResume() {
        diskSyncTask.setDiskSyncListener(this);
        super.onResume();

        if (diskSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
            syncComplete(diskSyncTask.getStatusMessage());
        }
    }


    @Override
    protected void onPause() {
        diskSyncTask.setDiskSyncListener(null);

        if (isFabOpen) {
            animateFAB();
        }

        super.onPause();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Collect.getInstance().getActivityLogger().logOnStart(this);
    }

    @Override
    protected void onStop() {
        Collect.getInstance().getActivityLogger().logOnStop(this);
        super.onStop();
    }


    /**
     * Called by DiskSyncTask when the task is finished
     */

    @Override
    public void syncComplete(String result) {
        Timber.i("Disk sync task complete");
        TextView tv = (TextView) findViewById(R.id.status_text);
        tv.setText(result.trim());
    }

    private void setupAdapter() {
        String[] data = new String[]{
                FormsProviderAPI.FormsColumns.DISPLAY_NAME,
                FormsProviderAPI.FormsColumns.DISPLAY_SUBTEXT,
                FormsProviderAPI.FormsColumns.JR_VERSION,
                FormsProviderAPI.FormsColumns.JR_FORM_ID
        };
        int[] view = new int[]{R.id.text1, R.id.text2, R.id.text3, R.id.text4};

        listAdapter =
                new FormCursorAdapter(FormsProviderAPI.FormsColumns.JR_VERSION, this, R.layout.form_list_item, getCursor(), data, view, this);

        listView.setAdapter(listAdapter);
    }

    @Override
    protected String getSortingOrderKey() {
        return FORM_CHOOSER_LIST_SORTING_ORDER;
    }

    @Override
    protected void updateAdapter() {
        listAdapter.changeCursor(getCursor());
    }

    private Cursor getCursor() {
        return new FormsDao().getFormsCursor(getFilterText(), getSortingOrder());
    }

    /**
     * Creates a dialog with the given message. Will exit the activity when the user preses "ok" if
     * shouldExit is set to true.
     */
    private void createErrorDialog(String errorMsg, final boolean shouldExit) {

        Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        alertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance().getActivityLogger().logAction(this,
                                "createErrorDialog",
                                shouldExit ? "exitApplication" : "OK");
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        alertDialog.setCancelable(false);
        alertDialog.setButton(getString(R.string.ok), errorListener);
        alertDialog.show();
    }

    @Override
    public void editSavedClicked(String formID) {
        Collect.getInstance().getActivityLogger()
                .logAction(this, ApplicationConstants.FormModes.EDIT_SAVED, "click");
        Intent intent = new Intent(this, InstanceChooserList.class);
        intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                ApplicationConstants.FormModes.EDIT_SAVED);
        intent.putExtra(FormsProviderAPI.FormsColumns.JR_FORM_ID, formID);
        startActivity(intent);
    }

    @Override
    public void sendFinalizedClicked(String formID) {
        Collect.getInstance().getActivityLogger()
                .logAction(this, "uploadForms", "click");
        Intent intent = new Intent(this, InstanceUploaderList.class);
        intent.putExtra(FormsProviderAPI.FormsColumns.JR_FORM_ID, formID);
        startActivity(intent);
    }

    public void animateFAB() {

        if (isFabOpen) {
            frameLayout.getBackground().setAlpha(0);
            fab.startAnimation(rotateBackward);
            fab1.startAnimation(fabClose);
            cardViewAggregate.startAnimation(fabClose);
            fab2.startAnimation(fabClose);
            cardViewGoogleDrive.startAnimation(fabClose);
            fab1.setClickable(false);
            fab2.setClickable(false);
            isFabOpen = false;
        } else {
            frameLayout.getBackground().setAlpha(240);
            fab.startAnimation(rotateForward);
            fab1.startAnimation(fabOpen);
            cardViewAggregate.startAnimation(fabOpen);
            fab2.startAnimation(fabOpen);
            cardViewGoogleDrive.startAnimation(fabOpen);
            fab1.setClickable(true);
            fab2.setClickable(true);
            isFabOpen = true;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab:
                animateFAB();
                break;
            case R.id.fab1:
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "downloadBlankForms: google drive", "click");
                Intent i;
                if (PlayServicesUtil.isGooglePlayServicesAvailable(this)) {
                    i = new Intent(getApplicationContext(),
                            GoogleDriveActivity.class);
                } else {
                    PlayServicesUtil.showGooglePlayServicesAvailabilityErrorDialog(this);
                    return;
                }
                startActivity(i);
                break;
            case R.id.fab2:

                Collect.getInstance().getActivityLogger()
                        .logAction(this, "downloadBlankForms: aggregate", "click");
                i = new Intent(getApplicationContext(),
                        FormDownloadList.class);
                startActivity(i);
                break;
        }
    }

    public void clear(View view) {
        animateFAB();
    }

}

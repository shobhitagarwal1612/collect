package org.odk.collect.android.activities;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
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
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.listeners.DiskSyncListener;
import org.odk.collect.android.listeners.FormClickListener;
import org.odk.collect.android.preferences.AboutPreferencesActivity;
import org.odk.collect.android.preferences.AdminKeys;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.preferences.AdminSharedPreferences;
import org.odk.collect.android.preferences.PreferencesActivity;
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
    private static final int PASSWORD_DIALOG = 1;
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
    private Cursor finalizedCursor;
    private Cursor savedCursor;
    private Cursor viewSentCursor;
    private int completedCount;
    private int savedCount;
    private int viewSentCount;
    private CoordinatorLayout coordinatorLayout;
    private String status;

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

        setTitle(getString(R.string.app_name));

        setupAdapter();

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

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        fab2 = (FloatingActionButton) findViewById(R.id.fab2);

        cardViewAggregate = (CardView) findViewById(R.id.cv_aggregate);
        cardViewGoogleDrive = (CardView) findViewById(R.id.cv_gdrive);

        frameLayout = (FrameLayout) findViewById(R.id.frame_layout);

        fabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backward);

        fab.setOnClickListener(this);
        fab1.setOnClickListener(this);
        fab2.setOnClickListener(this);
        frameLayout.setOnClickListener(this);
        cardViewAggregate.setOnClickListener(this);
        cardViewGoogleDrive.setOnClickListener(this);

        frameLayout.getBackground().setAlpha(0);
        frameLayout.setClickable(false);

        if (savedInstanceState != null && savedInstanceState.containsKey(syncMsgKey)) {
            status = (savedInstanceState.getString(syncMsgKey)).trim();
            Snackbar.make(fab, status, Snackbar.LENGTH_LONG).show();
        } else {
            status = getString(R.string.form_scan_starting);
            Snackbar.make(fab, status, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // pass the thread on restart
        return diskSyncTask;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(syncMsgKey, status);
    }

    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // get uri to form
        long idFormsTable = listView.getAdapter().getItemId(position);
        View childView = listView.getChildAt(position);
        String formID = (String) ((TextView) childView.findViewById(R.id.text4)).getText();
        Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, idFormsTable);

        Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick",
                formUri.toString());

        Intent intent = new Intent(this, InstanceChooserList.class);
        intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                ApplicationConstants.FormModes.EDIT_SAVED);
        intent.putExtra(FormsProviderAPI.FormsColumns.JR_FORM_ID, formID);
        intent.putExtra("formUri", formUri);
        startActivity(intent);
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
        status = result.trim();
        Snackbar.make(fab, status, Snackbar.LENGTH_LONG).show();
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

    @Override
    public void updateCount(View view, String formID) {

        InstancesDao instancesDao = new InstancesDao();

        // count for finalized instances
        try {
            finalizedCursor = instancesDao.getFinalizedInstancesCursor(formID);
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        if (finalizedCursor != null) {
            startManagingCursor(finalizedCursor);
        }
        completedCount = finalizedCursor != null ? finalizedCursor.getCount() : 0;

        // count for saved instances
        try {
            savedCursor = instancesDao.getUnsentInstancesCursor(formID);
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        if (savedCursor != null) {
            startManagingCursor(savedCursor);
        }
        savedCount = savedCursor != null ? savedCursor.getCount() : 0;

        //count for view sent form
        try {
            viewSentCursor = instancesDao.getSentInstancesCursor();
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }
        if (viewSentCursor != null) {
            startManagingCursor(viewSentCursor);
        }
        viewSentCount = viewSentCursor != null ? viewSentCursor.getCount() : 0;

        updateButtons(view);
    }

    public void animateFAB() {

        if (isFabOpen) {
            frameLayout.getBackground().setAlpha(0);
            fab.startAnimation(rotateBackward);
            fab1.startAnimation(fabClose);
            fab2.startAnimation(fabClose);
            cardViewAggregate.startAnimation(fabClose);
            cardViewGoogleDrive.startAnimation(fabClose);
            fab1.setClickable(false);
            fab2.setClickable(false);
            frameLayout.setClickable(false);
            cardViewAggregate.setClickable(false);
            cardViewGoogleDrive.setClickable(false);
            isFabOpen = false;
        } else {
            frameLayout.getBackground().setAlpha(240);
            fab.startAnimation(rotateForward);
            fab1.startAnimation(fabOpen);
            fab2.startAnimation(fabOpen);
            cardViewAggregate.startAnimation(fabOpen);
            cardViewGoogleDrive.startAnimation(fabOpen);
            fab1.setClickable(true);
            fab2.setClickable(true);
            frameLayout.setClickable(true);
            cardViewAggregate.setClickable(true);
            cardViewGoogleDrive.setClickable(true);
            isFabOpen = true;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab1:
            case R.id.cv_gdrive:
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
            case R.id.cv_aggregate:
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "downloadBlankForms: aggregate", "click");
                i = new Intent(getApplicationContext(),
                        FormDownloadList.class);
                startActivity(i);
                break;
            case R.id.fab:
            case R.id.frame_layout:
                animateFAB();
                break;
        }
    }

    private void updateButtons(View view) {
        TextView editSaved = (TextView) view.findViewById(R.id.edit_saved);
        TextView sendFinalized = (TextView) view.findViewById(R.id.send_finalized);

        if (finalizedCursor != null && !finalizedCursor.isClosed()) {
            finalizedCursor.requery();
            completedCount = finalizedCursor.getCount();
            if (completedCount > 0) {
                sendFinalized.setText(getString(R.string.send_finalized) + "\n(" + completedCount + ")");
            } else {
                sendFinalized.setOnClickListener(null);
                sendFinalized.setText(getString(R.string.send_finalized));
            }
        } else {
            sendFinalized.setOnClickListener(null);
            sendFinalized.setText(getString(R.string.send_finalized));
            Timber.d("Cannot update \"Send Finalized\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }

        if (savedCursor != null && !savedCursor.isClosed()) {
            savedCursor.requery();
            savedCount = savedCursor.getCount();
            if (savedCount > 0) {
                editSaved.setText(getString(R.string.edit_saved) + "\n(" + savedCount + ")");
            } else {
                editSaved.setOnClickListener(null);
                editSaved.setText(getString(R.string.edit_saved));
            }
        } else {
            editSaved.setOnClickListener(null);
            editSaved.setText(getString(R.string.edit_saved));
            Timber.d("Cannot update \"Edit Form\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }

        if (viewSentCursor != null && !viewSentCursor.isClosed()) {
            viewSentCursor.requery();
            viewSentCount = viewSentCursor.getCount();
            if (viewSentCount > 0) {
                Timber.d(getString(R.string.view_sent_forms_button, String.valueOf(viewSentCount)));
            } else {
                Timber.d(getString(R.string.view_sent_forms));
            }
        } else {
            Timber.d(getString(R.string.view_sent_forms));
            Timber.d("Cannot update \"View Sent\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Collect.getInstance().getActivityLogger()
                .logAction(this, "onCreateOptionsMenu", "show");
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                Collect.getInstance()
                        .getActivityLogger()
                        .logAction(this, "onOptionsItemSelected",
                                "MENU_ABOUT");
                Intent aboutIntent = new Intent(this, AboutPreferencesActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.menu_general_preferences:
                Collect.getInstance()
                        .getActivityLogger()
                        .logAction(this, "onOptionsItemSelected",
                                "MENU_PREFERENCES");
                Intent ig = new Intent(this, PreferencesActivity.class);
                startActivity(ig);
                return true;
            case R.id.menu_admin_preferences:
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "onOptionsItemSelected", "MENU_ADMIN");
                String pw = (String) AdminSharedPreferences.getInstance().get(AdminKeys.KEY_ADMIN_PW);
                if ("".equalsIgnoreCase(pw)) {
                    Intent i = new Intent(getApplicationContext(),
                            AdminPreferencesActivity.class);
                    startActivity(i);
                } else {
                    showDialog(PASSWORD_DIALOG);
                    Collect.getInstance().getActivityLogger()
                            .logAction(this, "createAdminPasswordDialog", "show");
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

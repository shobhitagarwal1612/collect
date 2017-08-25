package org.odk.collect.android.activities;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.CardView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import org.odk.collect.android.R;
import org.odk.collect.android.adapters.FormCursorAdapter;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.listeners.DeleteFormsListener;
import org.odk.collect.android.listeners.DiskSyncListener;
import org.odk.collect.android.listeners.FormClickListener;
import org.odk.collect.android.preferences.AboutPreferencesActivity;
import org.odk.collect.android.preferences.AdminKeys;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.preferences.AdminSharedPreferences;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.tasks.DeleteFormsTask;
import org.odk.collect.android.tasks.DiskSyncTask;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.PlayServicesUtil;
import org.odk.collect.android.utilities.ToastUtils;

import timber.log.Timber;

import static org.odk.collect.android.preferences.PreferenceKeys.KEY_ADD_FORM_SHOWCASE_VIEW;

public class MainActivity extends FormListActivity
        implements NavigationView.OnNavigationItemSelectedListener, DiskSyncListener,
        FormClickListener, View.OnClickListener, DeleteFormsListener {

    private static final boolean EXIT = true;
    private static final int PASSWORD_DIALOG = 1;
    private static final String syncMsgKey = "syncMsgKey";
    private static final String FORM_CHOOSER_LIST_SORTING_ORDER = "formChooserListSortingOrder";
    private static final String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=";
    BackgroundTasks backgroundTasks;
    private String status;
    // floating buttons
    private FloatingActionButton fab;
    private FloatingActionButton fab1;
    private FloatingActionButton fab2;
    private CardView cardViewAggregate;
    private CardView cardViewGoogleDrive;
    private FrameLayout frameLayout;
    private Animation fabOpen;
    private Animation fabClose;
    private Animation rotateForward;
    private Animation rotateBackward;
    private AlertDialog alertDialog;
    // cursors
    private Cursor finalizedCursor;
    private Cursor savedCursor;
    private Cursor viewSentCursor;
    private int completedCount;
    private int savedCount;
    private int viewSentCount;
    private boolean isFabOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // must be at the beginning of any activity that can be called from an external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        // DiskSyncTask checks the disk for any forms not already in the content provider
        // that is, put here by dragging and dropping onto the SDCard

        sortingOptions = new String[]{
                getString(R.string.sort_by_name_asc), getString(R.string.sort_by_name_desc),
                getString(R.string.sort_by_date_asc), getString(R.string.sort_by_date_desc),
        };

        setupViews();

        if (savedInstanceState != null && savedInstanceState.containsKey(syncMsgKey)) {
            status = (savedInstanceState.getString(syncMsgKey)).trim();
            Snackbar.make(fab, status, Snackbar.LENGTH_LONG).show();
        } else {
            status = getString(R.string.form_scan_starting);
            Snackbar.make(fab, status, Snackbar.LENGTH_LONG).show();
        }

        if (backgroundTasks == null) {
            backgroundTasks = new BackgroundTasks();
            backgroundTasks.diskSyncTask = new DiskSyncTask();
            backgroundTasks.diskSyncTask.setDiskSyncListener(this);
            backgroundTasks.diskSyncTask.execute((Void[]) null);
        }

        boolean displayShowCase = (boolean) GeneralSharedPreferences.getInstance().get(KEY_ADD_FORM_SHOWCASE_VIEW);
        if (displayShowCase) {
            RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
            lps.setMargins(margin, margin, margin, margin);

            ViewTarget target = new ViewTarget(R.id.fab, this);
            ShowcaseView sv = new ShowcaseView.Builder(this)
                    .withMaterialShowcase()
                    .setTarget(target)
                    .hideOnTouchOutside()
                    .setStyle(R.style.CustomShowcaseTheme)
                    .setContentTitle("Add Button")
                    .setContentText("Tap here to add new forms")
                    .build();

            sv.setButtonPosition(lps);

            GeneralSharedPreferences.getInstance().save(KEY_ADD_FORM_SHOWCASE_VIEW, false);
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // pass the thread on restart
        return backgroundTasks;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(syncMsgKey, status);
    }

    @Override
    protected void onResume() {
        setupAdapter();

        // hook up to receive completion events
        backgroundTasks.diskSyncTask.setDiskSyncListener(this);
        if (backgroundTasks.deleteFormsTask != null) {
            backgroundTasks.deleteFormsTask.setDeleteListener(this);
        }
        super.onResume();
        // async task may have completed while we were reorienting...
        if (backgroundTasks.diskSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
            syncComplete(backgroundTasks.diskSyncTask.getStatusMessage());
        }
        if (backgroundTasks.deleteFormsTask != null
                && backgroundTasks.deleteFormsTask.getStatus() == AsyncTask.Status.FINISHED) {
            deleteComplete(backgroundTasks.deleteFormsTask.getDeleteCount());
        }
    }

    /**
     * Create the form delete dialog
     */
    private void createDeleteDialog(final String formName, final long id, final boolean canDelete) {

        Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");

        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setIcon(R.drawable.ic_delete_black_36dp);
        alertDialog.setTitle("Delete form");
        alertDialog.setMessage(String.format(canDelete ? "Are you sure you want to delete \"%s\" form?" :
                "You still have some unsent instances of \"%s\" form.\n\nTry deleting them or sending them before removing this form.", formName));
        alertDialog.setCancelable(true);
        if (canDelete) {
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteSelectedForm(id);
                }
            });
        }
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, canDelete ? "Cancel" : "Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void setupViews() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // floating buttons
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
    }

    private void setupAdapter() {
        String[] data = new String[]{
                FormsProviderAPI.FormsColumns.DISPLAY_NAME,
                FormsProviderAPI.FormsColumns.DISPLAY_SUBTEXT,
                FormsProviderAPI.FormsColumns.JR_VERSION,
                FormsProviderAPI.FormsColumns.JR_FORM_ID
        };
        int[] view = new int[]{R.id.text1, R.id.text2, R.id.text3, R.id.text4};

        listAdapter = new FormCursorAdapter(FormsProviderAPI.FormsColumns.JR_VERSION,
                this, getCursor(), data, view, this);

        listView.setAdapter(listAdapter);
    }

    private Cursor getCursor() {
        return new FormsDao().getFormsCursor(getFilterText(), getSortingOrder());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void updateAdapter() {
        listAdapter.changeCursor(getCursor());

        if (getFilterText().equals("") && listAdapter.getCursor().getCount() == 0) {
            GeneralSharedPreferences.getInstance().save(KEY_ADD_FORM_SHOWCASE_VIEW, true);
        }
    }

    @Override
    protected String getSortingOrderKey() {
        return FORM_CHOOSER_LIST_SORTING_ORDER;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_about:
                Intent aboutIntent = new Intent(this, AboutPreferencesActivity.class);
                startActivity(aboutIntent);
                break;

            case R.id.nav_general_preferences:
                Intent ig = new Intent(this, PreferencesActivity.class);
                startActivity(ig);
                break;

            case R.id.nav_admin_preferences:
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
                break;

            case R.id.nav_share:
                final String APP_PACKAGE_NAME = getPackageName();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.tell_your_friends_msg) + " " + GOOGLE_PLAY_URL
                                + APP_PACKAGE_NAME);
                startActivity(Intent.createChooser(shareIntent,
                        getString(R.string.tell_your_friends)));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

    @Override
    public void editSavedClicked(String formID, int position) {
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
    public void viewSentClicked(String formID) {
        Collect.getInstance().getActivityLogger()
                .logAction(this, "viewSent", "click");
        Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
        i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                ApplicationConstants.FormModes.VIEW_SENT);
        i.putExtra(FormsProviderAPI.FormsColumns.JR_FORM_ID, formID);
        startActivity(i);
    }

    @Override
    public void updateCount(View view, String formId) {
        InstancesDao instancesDao = new InstancesDao();

        // count for finalized instances
        try {
            finalizedCursor = instancesDao.getFinalizedInstancesCursor(formId);
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
            savedCursor = instancesDao.getUnsentInstancesCursor(formId);
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
            viewSentCursor = instancesDao.getSentInstancesCursor(formId);
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

    private void updateButtons(View view) {
        TextView editSaved = (TextView) view.findViewById(R.id.edit_saved);
        TextView sendFinalized = (TextView) view.findViewById(R.id.send_finalized);
        TextView viewSent = (TextView) view.findViewById(R.id.view_sent);

        if (finalizedCursor != null && !finalizedCursor.isClosed()) {
            finalizedCursor.requery();
            completedCount = finalizedCursor.getCount();
            if (completedCount > 0) {
                sendFinalized.setText(completedCount + " " + getString(R.string.send_finalized));
            } else {
                sendFinalized.setVisibility(View.GONE);
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
                editSaved.setText(savedCount + " " + getString(R.string.edit_saved));
            } else {
                editSaved.setVisibility(View.GONE);
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
                viewSent.setText(viewSentCount + " " + getString(R.string.view_sent));
            } else {
                viewSent.setVisibility(View.GONE);
            }
        } else {
            Timber.d(getString(R.string.view_sent_forms));
            Timber.d("Cannot update \"View Sent\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }
    }

    @Override
    public void itemClicked(int position) {
        // get uri to form
        long idFormsTable = listView.getAdapter().getItemId(position);
        Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, idFormsTable);
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
    }

    @Override
    public void onLongClick(View view, long id) {
        String formName = ((TextView) view.findViewById(R.id.text1)).getText().toString();
        String editSaved = ((TextView) view.findViewById(R.id.edit_saved)).getText().toString();
        editSaved = editSaved.split(" ")[0];
        int savedCount = 0;
        if (!editSaved.equals("")) {
            savedCount = Integer.parseInt(editSaved);
        }
        if (savedCount > 0) {
            createDeleteDialog(formName, id, false);
        } else {
            createDeleteDialog(formName, id, true);
        }
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

    @Override
    public void deleteComplete(int deletedForms) {
        Timber.i("Delete forms complete");
        logger.logAction(this, "deleteComplete", Integer.toString(deletedForms));
        final int toDeleteCount = backgroundTasks.deleteFormsTask.getToDeleteCount();

        if (deletedForms == toDeleteCount) {
            // all deletes were successful
            ToastUtils.showShortToast(getString(R.string.file_deleted_ok, String.valueOf(deletedForms)));
        } else {
            // had some failures
            Timber.e("Failed to delete %d forms", (toDeleteCount - deletedForms));
            ToastUtils.showLongToast(getString(R.string.file_deleted_error, String.valueOf(getCheckedCount()
                    - deletedForms), String.valueOf(getCheckedCount())));
        }
        backgroundTasks.deleteFormsTask = null;
        listAdapter.notifyDataSetChanged();
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

    /**
     * Deletes the selected files.First from the database then from the file
     * system
     */
    private void deleteSelectedForm(long id) {
        // only start if no other task is running
        if (backgroundTasks.deleteFormsTask == null) {
            backgroundTasks.deleteFormsTask = new DeleteFormsTask();
            backgroundTasks.deleteFormsTask
                    .setContentResolver(getContentResolver());
            backgroundTasks.deleteFormsTask.setDeleteListener(this);
            backgroundTasks.deleteFormsTask.execute(id);
        } else {
            ToastUtils.showLongToast(R.string.file_delete_in_progress);
        }
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

    @Override
    protected void onPause() {
        backgroundTasks.diskSyncTask.setDiskSyncListener(null);
        if (backgroundTasks.deleteFormsTask != null) {
            backgroundTasks.deleteFormsTask.setDeleteListener(null);
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        if (isFabOpen) {
            animateFAB();
        }

        super.onPause();
    }

    private static class BackgroundTasks {
        DiskSyncTask diskSyncTask = null;
        DeleteFormsTask deleteFormsTask = null;

        BackgroundTasks() {
        }
    }
}

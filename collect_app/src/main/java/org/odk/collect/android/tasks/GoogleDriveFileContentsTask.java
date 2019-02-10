package org.odk.collect.android.tasks;

import android.os.AsyncTask;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import org.odk.collect.android.activities.GoogleDriveActivity;
import org.odk.collect.android.listeners.TaskListener;
import org.odk.collect.android.logic.DriveListItem;
import org.odk.collect.android.utilities.gdrive.DriveHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import timber.log.Timber;

import static org.odk.collect.android.activities.GoogleDriveActivity.PARENT_ID_KEY;
import static org.odk.collect.android.activities.GoogleDriveActivity.ROOT_KEY;

public class GoogleDriveFileContentsTask extends AsyncTask<String, HashMap<String, Object>, HashMap<String, Object>> {

    public static final int AUTHORIZATION_REQUEST_CODE = 4322;
    private static final String FILE_LIST_KEY = "fileList";
    private static final String CURRENT_ID_KEY = "currentDir";

    private TaskListener listener;
    private DriveHelper driveHelper;
    private Stack<String> folderIdStack;
    private List<DriveListItem> driveList;
    private boolean myDrive;
    private String rootId;
    private WeakReference<GoogleDriveActivity> activityWeakReference;

    public GoogleDriveFileContentsTask(DriveHelper driveHelper, Stack<String> folderIdStack, boolean myDrive, List<DriveListItem> driveList, GoogleDriveActivity activity) {
        this.driveHelper = driveHelper;
        this.folderIdStack = folderIdStack;
        this.myDrive = myDrive;
        this.driveList = driveList;
        activityWeakReference = new WeakReference<>(activity);
    }

    public void setTaskListener(TaskListener tl) {
        listener = tl;
    }

    @Override
    protected HashMap<String, Object> doInBackground(String... params) {
        GoogleDriveActivity googleDriveActivity = activityWeakReference.get();

        if (googleDriveActivity == null) {
            return null;
        }

        if (rootId == null) {
            try {
                rootId = driveHelper.getRootFolderId();
            } catch (UserRecoverableAuthIOException e) {
                googleDriveActivity.startActivityForResult(e.getIntent(), AUTHORIZATION_REQUEST_CODE);
            } catch (IOException e) {
                Timber.e(e);
                googleDriveActivity.displayAuthException();
            }
            if (rootId == null) {
                Timber.e("Unable to fetch drive contents");
                return null;
            }
        }

        String parentId;
        if (folderIdStack.isEmpty()) {
            parentId = rootId;
        } else {
            parentId = folderIdStack.peek();
        }
        String query = "'" + parentId + "' in parents";

        if (params.length == 2) {
            // TODO: *.xml or .xml or xml
            // then search mimetype
            query = "fullText contains '" + params[1] + "'";
        }

        // SharedWithMe, and root:
        String currentDir = params[0];

        if (!myDrive) {
            if (currentDir.equals(ROOT_KEY) || folderIdStack.isEmpty()) {
                query = "sharedWithMe=true";
            }
        }

        query += " and trashed=false";

        String fields = "nextPageToken, files(modifiedTime, id, name, mimeType)";
        Drive.Files.List request = null;
        try {
            request = driveHelper.buildRequest(query, fields);
        } catch (IOException e) {
            Timber.e(e);
        }

        HashMap<String, Object> results = new HashMap<>();
        results.put(PARENT_ID_KEY, parentId);
        results.put(CURRENT_ID_KEY, currentDir);
        if (request != null) {
            List<com.google.api.services.drive.model.File> driveFileListPage;
            do {
                try {
                    driveFileListPage = new ArrayList<>();
                    driveHelper.fetchFilesForCurrentPage(request, driveFileListPage);

                    HashMap<String, Object> nextPage = new HashMap<>();
                    nextPage.put(PARENT_ID_KEY, parentId);
                    nextPage.put(CURRENT_ID_KEY, currentDir);
                    nextPage.put(FILE_LIST_KEY, driveFileListPage);
                    publishProgress(nextPage);
                } catch (IOException e) {
                    Timber.e(e, "Exception thrown while accessing the file list");
                }
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        }
        return results;

    }

    @Override
    protected void onPostExecute(HashMap<String, Object> results) {
        super.onPostExecute(results);
        if (results == null) {
            // was an auth request
            return;
        }
        if (listener != null) {
            listener.taskComplete(results);
        }
    }

    @SafeVarargs
    @Override
    protected final void onProgressUpdate(HashMap<String, Object>... values) {
        super.onProgressUpdate(values);
        List<File> fileList =
                (List<com.google.api.services.drive.model.File>) values[0]
                        .get(FILE_LIST_KEY);
        String parentId = (String) values[0].get(PARENT_ID_KEY);
        String currentDir = (String) values[0].get(CURRENT_ID_KEY);

        List<DriveListItem> dirs = new ArrayList<>();
        List<DriveListItem> forms = new ArrayList<>();

        for (com.google.api.services.drive.model.File f : fileList) {
            String type = f.getMimeType();
            switch (type) {
                case "application/xml":
                case "text/xml":
                case "application/xhtml":
                case "text/xhtml":
                case "application/xhtml+xml":
                    forms.add(new DriveListItem(f.getName(), "", f.getModifiedTime(), "", "",
                            DriveListItem.FILE, f.getId(), currentDir));
                    break;
                case "application/vnd.google-apps.folder":
                    dirs.add(new DriveListItem(f.getName(), "", f.getModifiedTime(), "", "",
                            DriveListItem.DIR, f.getId(), parentId));
                    break;
                default:
                    // skip the rest of the files
                    break;
            }
        }
        Collections.sort(dirs);
        Collections.sort(forms);
        driveList.addAll(dirs);
        driveList.addAll(forms);

        if (activityWeakReference.get() != null) {
            activityWeakReference.get().updateAdapter();
        }
    }
}

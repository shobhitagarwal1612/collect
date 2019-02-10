package org.odk.collect.android.tasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.exception.MultipleFoldersFoundException;
import org.odk.collect.android.listeners.GoogleDriveFormDownloadListener;
import org.odk.collect.android.logic.DriveListItem;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.gdrive.DriveHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class GetFileTask extends AsyncTask<ArrayList<DriveListItem>, Boolean, HashMap<String, Object>> {

    private GoogleDriveFormDownloadListener listener;
    private DriveHelper driveHelper;

    public GetFileTask(DriveHelper driveHelper) {
        this.driveHelper = driveHelper;
    }

    public void setGoogleDriveFormDownloadListener(GoogleDriveFormDownloadListener gl) {
        listener = gl;
    }

    @SafeVarargs
    @Override
    protected final HashMap<String, Object> doInBackground(ArrayList<DriveListItem>... params) {
        HashMap<String, Object> results = new HashMap<>();

        ArrayList<DriveListItem> fileItems = params[0];

        for (int k = 0; k < fileItems.size(); k++) {
            DriveListItem fileItem = fileItems.get(k);

            try {
                downloadFile(fileItem.getDriveId(), fileItem.getName());
                results.put(fileItem.getName(), Collect.getInstance().getString(R.string.success));

                String mediaDirName = FileUtils.constructMediaPath(fileItem.getName());

                String folderId;
                try {
                    folderId = driveHelper.getIDOfFolderWithName(mediaDirName, fileItem.getParentId(), false);
                } catch (MultipleFoldersFoundException exception) {
                    results.put(fileItem.getName(), Collect.getInstance().getString(R.string.multiple_media_folders_detected_notification));
                    return results;
                }

                if (folderId != null) {
                    List<com.google.api.services.drive.model.File> mediaFileList;
                    mediaFileList = driveHelper.getFilesFromDrive(null, folderId);

                    FileUtils.createFolder(Collect.FORMS_PATH + File.separator + mediaDirName);

                    for (com.google.api.services.drive.model.File mediaFile : mediaFileList) {
                        String filePath = mediaDirName + File.separator + mediaFile.getName();
                        downloadFile(mediaFile.getId(), filePath);
                        results.put(filePath, Collect.getInstance().getString(R.string.success));
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
                results.put(fileItem.getName(), e.getMessage());
                return results;
            }
        }
        return results;
    }

    private void downloadFile(@NonNull String fileId, String fileName) throws IOException {
        File file = new File(Collect.FORMS_PATH + File.separator + fileName);
        driveHelper.downloadFile(fileId, file);
    }

    @Override
    protected void onPostExecute(HashMap<String, Object> results) {
        listener.formDownloadComplete(results);
    }
}


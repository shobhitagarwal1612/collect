package org.odk.collect.android.activities;

import android.app.AlertDialog;

import org.odk.collect.android.listeners.DeleteInstancesListener;
import org.odk.collect.android.listeners.DiskSyncListener;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.tasks.DeleteInstancesTask;
import org.odk.collect.android.tasks.InstanceSyncTask;

import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_DESC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_DESC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_STATUS_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_STATUS_DESC;

abstract class InstanceListActivity extends AppListActivity implements DeleteInstancesListener, DiskSyncListener {
    protected AlertDialog alertDialog;
    protected DeleteInstancesTask deleteInstancesTask;
    protected InstanceSyncTask instanceSyncTask;

    protected String getSortingOrder() {
        String sortingOrder = InstanceColumns.DISPLAY_NAME + " ASC, " + InstanceColumns.STATUS + " DESC";
        switch (getSelectedSortingOrder()) {
            case BY_NAME_ASC:
                sortingOrder = InstanceColumns.DISPLAY_NAME + " ASC, " + InstanceColumns.STATUS + " DESC";
                break;
            case BY_NAME_DESC:
                sortingOrder = InstanceColumns.DISPLAY_NAME + " DESC, " + InstanceColumns.STATUS + " DESC";
                break;
            case BY_DATE_ASC:
                sortingOrder = InstanceColumns.LAST_STATUS_CHANGE_DATE + " ASC";
                break;
            case BY_DATE_DESC:
                sortingOrder = InstanceColumns.LAST_STATUS_CHANGE_DATE + " DESC";
                break;
            case BY_STATUS_ASC:
                sortingOrder = InstanceColumns.STATUS + " ASC, " + InstanceColumns.DISPLAY_NAME + " ASC";
                break;
            case BY_STATUS_DESC:
                sortingOrder = InstanceColumns.STATUS + " DESC, " + InstanceColumns.DISPLAY_NAME + " ASC";
                break;
        }
        return sortingOrder;
    }

    /**
     * Returns the IDs of the checked items, as an array of Long
     */
    protected Long[] getCheckedIdObjects() {
        int i = 0;
        Long[] checkedIdObjects = new Long[selectedInstances.size()];

        for (Long id : selectedInstances) {
            checkedIdObjects[i++] = id;
        }

        return checkedIdObjects;
    }
}
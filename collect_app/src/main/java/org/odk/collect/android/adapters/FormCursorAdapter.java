/*
 * Copyright (C) 2012 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.InstanceChooserList;
import org.odk.collect.android.activities.InstanceUploaderList;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.utilities.ApplicationConstants;

/**
 * Implementation of cursor adapter that displays the version of a form if a form has a version.
 *
 * @author mitchellsundt@gmail.com
 */
public class FormCursorAdapter extends SimpleCursorAdapter implements View.OnClickListener {

    private final Context context;
    private final String versionColumnName;
    private final ViewBinder originalBinder;

    public FormCursorAdapter(String versionColumnName, Context context, int layout,
                             Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
        this.versionColumnName = versionColumnName;
        this.context = context;
        originalBinder = getViewBinder();
        setViewBinder(new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor,
                                        int columnIndex) {
                String columnName = cursor.getColumnName(columnIndex);
                if (!columnName.equals(FormCursorAdapter.this.versionColumnName)) {
                    return originalBinder != null && originalBinder.setViewValue(view, cursor, columnIndex);
                } else {
                    String version = cursor.getString(columnIndex);
                    TextView v = (TextView) view;
                    if (version != null) {
                        v.setText(String.format(FormCursorAdapter.this.context.getString(R.string.version_number), version));
                        v.setVisibility(View.VISIBLE);
                    } else {
                        v.setText(null);
                        v.setVisibility(View.GONE);
                    }
                }
                return true;
            }
        });
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        view.findViewById(R.id.edit_saved).setOnClickListener(this);
        view.findViewById(R.id.send_finalized).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;

        switch (v.getId()) {
            case R.id.edit_saved:
                Collect.getInstance().getActivityLogger()
                        .logAction(this, ApplicationConstants.FormModes.EDIT_SAVED, "click");
                intent = new Intent(context, InstanceChooserList.class);
                intent.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                        ApplicationConstants.FormModes.EDIT_SAVED);
                break;
            case R.id.send_finalized:
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "uploadForms", "click");
                intent = new Intent(context, InstanceUploaderList.class);
                break;
        }

        if (intent != null) {
            context.startActivity(intent);
        }
    }
}
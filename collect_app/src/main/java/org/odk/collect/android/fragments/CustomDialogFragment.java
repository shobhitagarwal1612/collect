package org.odk.collect.android.fragments;

/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import java.io.Serializable;

public class CustomDialogFragment extends DialogFragment {

    public static final String CUSTOM_DIALOG_BUNDLE = "customDialogBundle";

    public static CustomDialogFragment newInstance(ODKDialogBundle odkDialogBundle) {
        CustomDialogFragment odkDialogFragment = new CustomDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(CUSTOM_DIALOG_BUNDLE, odkDialogBundle);
        odkDialogFragment.setArguments(bundle);
        return odkDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ODKDialogBundle odkDialogBundle = (ODKDialogBundle) getArguments().getSerializable(CUSTOM_DIALOG_BUNDLE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(odkDialogBundle.getDialogTitle())
                .setMessage(odkDialogBundle.getDialogMessage())
                .setCancelable(odkDialogBundle.isCancelable());

        if (odkDialogBundle.getLeftButtonText() != null) {
            builder.setNegativeButton(odkDialogBundle.getLeftButtonText(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    resolveButtonAction(odkDialogBundle.getLeftButtonAction());
                }
            });
        }

        if (odkDialogBundle.getRightButtonText() != null) {
            builder.setPositiveButton(odkDialogBundle.getRightButtonText(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    resolveButtonAction(odkDialogBundle.getRightButtonAction());
                }
            });
        }

        if (odkDialogBundle.getIcon() != null) {
            builder.setIcon(odkDialogBundle.getIcon());
        }

        return builder.create();
    }

    private void resolveButtonAction(final Action action) {
        switch (action) {
            case RECREATE:
                dismiss();
                getActivity().recreate();
                break;
            default:
        }
    }

    public enum Action {
        RECREATE
    }

    public static class ODKDialogBundle implements Serializable {
        private String dialogTitle;
        private String dialogMessage;
        private String leftButtonText;
        private String rightButtonText;

        private Action leftButtonAction;
        private Action rightButtonAction;

        private boolean cancelable;

        private int icon;

        ODKDialogBundle(Builder builder) {
            dialogTitle = builder.dialogTitle;
            dialogMessage = builder.dialogMessage;
            leftButtonText = builder.leftButtonText;
            rightButtonText = builder.rightButtonText;
            leftButtonAction = builder.leftButtonAction;
            rightButtonAction = builder.rightButtonAction;
            cancelable = builder.cancelable;
            icon = builder.icon;
        }

        public String getDialogTitle() {
            return dialogTitle;
        }

        String getDialogMessage() {
            return dialogMessage;
        }

        String getLeftButtonText() {
            return leftButtonText;
        }

        String getRightButtonText() {
            return rightButtonText;
        }

        Action getLeftButtonAction() {
            return leftButtonAction;
        }

        Action getRightButtonAction() {
            return rightButtonAction;
        }

        boolean isCancelable() {
            return cancelable;
        }

        public Integer getIcon() {
            return icon;
        }

        public static class Builder {
            private String dialogTitle;
            private String dialogMessage;
            private String leftButtonText;
            private String rightButtonText;

            private Action leftButtonAction;
            private Action rightButtonAction;

            private boolean cancelable;

            private int icon;

            public Builder() {
            }

            public Builder setDialogTitle(String dialogTitle) {
                this.dialogTitle = dialogTitle;
                return this;
            }

            public Builder setDialogMessage(String dialogMessage) {
                this.dialogMessage = dialogMessage;
                return this;
            }

            public Builder setLeftButtonText(String leftButtonText) {
                this.leftButtonText = leftButtonText;
                return this;
            }

            public Builder setRightButtonText(String rightButtonText) {
                this.rightButtonText = rightButtonText;
                return this;
            }

            public Builder setLeftButtonAction(Action leftButtonAction) {
                this.leftButtonAction = leftButtonAction;
                return this;
            }

            public Builder setRightButtonAction(Action rightButtonAction) {
                this.rightButtonAction = rightButtonAction;
                return this;
            }

            public Builder setCancelable(Boolean cancelable) {
                this.cancelable = cancelable;
                return this;
            }

            public Builder setIcon(int icon) {
                this.icon = icon;
                return this;
            }

            public ODKDialogBundle build() {
                return new ODKDialogBundle(this);
            }
        }
    }
}
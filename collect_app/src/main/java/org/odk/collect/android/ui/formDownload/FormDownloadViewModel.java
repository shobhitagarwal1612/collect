/*
 * Copyright 2019 Nafundi
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

package org.odk.collect.android.ui.formDownload;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.logic.FormDetails;
import org.odk.collect.android.ui.base.BaseViewModel;
import org.odk.collect.android.utilities.rx.SchedulerProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class FormDownloadViewModel extends BaseViewModel<FormDownloadNavigator> {

    private HashMap<String, FormDetails> formNamesAndURLs = new HashMap<>();

    private final ArrayList<HashMap<String, String>> formList = new ArrayList<>();

    private final LinkedHashSet<String> selectedForms = new LinkedHashSet<>();

    private final BehaviorSubject<AlertDialogUiModel> alertDialogSubject;
    private final BehaviorSubject<Boolean> progressDialogSubject;

    private String progressDialogMsg;

    private boolean cancelDialogShowing;
    private boolean loadingCanceled;

    // Variables used when the activity is called from an external app
    private boolean isDownloadOnlyMode;
    private String[] formIdsToDownload;
    private String url;
    private String username;
    private String password;
    private final HashMap<String, Boolean> formResults = new HashMap<>();

    private AlertDialogUiModel uiModel;

    public FormDownloadViewModel(SchedulerProvider schedulerProvider) {
        super(schedulerProvider);

        alertDialogSubject = BehaviorSubject.create();
        progressDialogSubject = BehaviorSubject.create();
    }

    public HashMap<String, FormDetails> getFormNamesAndURLs() {
        return formNamesAndURLs;
    }

    public void setFormNamesAndURLs(HashMap<String, FormDetails> formNamesAndURLs) {
        this.formNamesAndURLs = formNamesAndURLs;
    }

    public void clearFormNamesAndURLs() {
        formNamesAndURLs.clear();
    }

    public String getProgressDialogMsg() {
        return progressDialogMsg == null ? Collect.getInstance().getString(R.string.please_wait) : progressDialogMsg;
    }

    public void setProgressDialogMsg(String progressDialogMsg) {
        this.progressDialogMsg = progressDialogMsg;
    }

    public ArrayList<HashMap<String, String>> getFormList() {
        return formList;
    }

    public void clearFormList() {
        formList.clear();
    }

    public void addForm(HashMap<String, String> item) {
        formList.add(item);
    }

    public void addForm(int index, HashMap<String, String> item) {
        formList.add(index, item);
    }

    public LinkedHashSet<String> getSelectedForms() {
        return selectedForms;
    }

    public void addSelectedForm(String form) {
        selectedForms.add(form);
    }

    public void removeSelectedForm(String form) {
        selectedForms.remove(form);
    }

    public void clearSelectedForms() {
        selectedForms.clear();
    }

    public boolean isDownloadOnlyMode() {
        return isDownloadOnlyMode;
    }

    public void setDownloadOnlyMode(boolean downloadOnlyMode) {
        isDownloadOnlyMode = downloadOnlyMode;
    }

    public HashMap<String, Boolean> getFormResults() {
        return formResults;
    }

    public void putFormResult(String formId, boolean result) {
        formResults.put(formId, result);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String[] getFormIdsToDownload() {
        return Arrays.copyOf(formIdsToDownload, formIdsToDownload.length);
    }

    public void setFormIdsToDownload(String[] formIdsToDownload) {
        this.formIdsToDownload = formIdsToDownload;
    }

    public void setProgressDialogShowing(boolean progressDialogShowing) {
        progressDialogSubject.onNext(progressDialogShowing);
    }

    public boolean isCancelDialogShowing() {
        return cancelDialogShowing;
    }

    public void setCancelDialogShowing(boolean cancelDialogShowing) {
        this.cancelDialogShowing = cancelDialogShowing;
    }

    public boolean wasLoadingCanceled() {
        return loadingCanceled;
    }

    public void setLoadingCanceled(boolean loadingCanceled) {
        this.loadingCanceled = loadingCanceled;
    }

    public Observable<Boolean> getProgressDialog() {
        return progressDialogSubject;
    }

    public Observable<AlertDialogUiModel> getAlertDialog() {
        return alertDialogSubject
                .filter(__ -> uiModel != null)
                .doOnSubscribe(disposable -> alertDialogSubject.onNext(uiModel));
    }

    public void setAlertDialog(String title, String message, boolean shouldExit) {
        uiModel = new AlertDialogUiModel(title, message, shouldExit);

        alertDialogSubject.onNext(uiModel);
    }

    public void removeAlertDialog() {
        uiModel = null;
    }
}

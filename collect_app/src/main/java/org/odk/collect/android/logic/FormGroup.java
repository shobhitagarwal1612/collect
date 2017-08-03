package org.odk.collect.android.logic;


public class FormGroup {

    private String formId;
    private int saved;
    private int finalized;
    private int sent;

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public int getSaved() {
        return saved;
    }

    public void setSaved(int saved) {
        this.saved = saved;
    }

    public int getFinalized() {
        return finalized;
    }

    public void setFinalized(int finalized) {
        this.finalized = finalized;
    }

    public int getSent() {
        return sent;
    }

    public void setSent(int sent) {
        this.sent = sent;
    }
}

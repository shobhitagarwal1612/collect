package org.odk.collect.android;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.injection.AndroidTestComponent;
import org.odk.collect.android.injection.DaggerAndroidTestComponent;

import javax.annotation.OverridingMethodsMustInvokeSuper;

public abstract class DaggerAndroidTest {

    protected AndroidTestComponent androidTestComponent;

    protected abstract void injectDependencies();

    @OverridingMethodsMustInvokeSuper
    public void setUp() {
        androidTestComponent = DaggerAndroidTestComponent.builder().application(Collect.getInstance()).build();
        Collect.getInstance().setComponent(androidTestComponent);

        injectDependencies();
    }
}

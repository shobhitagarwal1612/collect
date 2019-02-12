package org.odk.collect.android.views;

import android.support.v7.widget.AppCompatImageButton;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import junit.framework.Assert;

import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.logic.FileReference;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Random;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class MediaLayoutTest {

    private static final String RANDOM_URI = "randomMediaURI";

    private final String audioUri;
    private final String imageUri;
    private final String videoUri;

    private ReferenceManager referenceManager;
    private FileReference reference;

    private MediaLayout mediaLayout;
    private AppCompatImageButton audioButton;
    private AppCompatImageButton videoButton;
    private ImageView imageView;
    private TextView textView;
    private TextView missingImage;
    private ImageView divider;
    private boolean isReferenceManagerStubbed;

    public MediaLayoutTest(String audioUri, String imageUri, String videoUri) {
        this.audioUri = audioUri;
        this.imageUri = imageUri;
        this.videoUri = videoUri;
    }

    @ParameterizedRobolectricTestRunner.Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null,          null,           null},
                {RANDOM_URI,    null,           null},
                {null,          RANDOM_URI,     null},
                {null,          null,           RANDOM_URI},
                {RANDOM_URI,    RANDOM_URI,     null},
                {RANDOM_URI,    null,           RANDOM_URI},
                {null,          RANDOM_URI,     RANDOM_URI},
                {RANDOM_URI,    RANDOM_URI,     RANDOM_URI}
        });
    }

    @Before
    public void setUp() throws InvalidReferenceException {
        reference = mock(FileReference.class);
        referenceManager = mock(ReferenceManager.class);
        textView = new TextView(RuntimeEnvironment.application);

        mediaLayout = new MediaLayout(RuntimeEnvironment.application);

        audioButton = mediaLayout.audioButton;
        videoButton = mediaLayout.videoButton;
        imageView = mediaLayout.imageView;
        missingImage = mediaLayout.missingImage;
        divider = mediaLayout.divider;

        /*
         * Stub reference manager randomly to account for both illegal URI and proper URI while
         * attempting to load image view
         */
        if (new Random().nextBoolean()) {
            stubReferenceManager();
        }
    }

    @Test
    public void viewShouldBecomeVisibleIfUriPresent() {
        Assert.assertNotNull(mediaLayout);
        Assert.assertEquals(VISIBLE, mediaLayout.getVisibility());
        assertVisibility(GONE, audioButton, videoButton, imageView, missingImage, divider);

        mediaLayout.setAVT(textView, audioUri, imageUri, videoUri, null);

        // we do not check for the validity of the URIs for the audio and video while loading MediaLayout
        assertVisibility(audioUri == null ? GONE : VISIBLE, audioButton);
        assertVisibility(videoUri == null ? GONE : VISIBLE, videoButton);

        if (imageUri == null || !isReferenceManagerStubbed) {
            // either the URI wasn't provided or it encountered InvalidReferenceException
            assertVisibility(GONE, imageView, missingImage);
        } else {
            // either the bitmap was successfully loaded or the file was missing
            Assert.assertNotSame(imageView.getVisibility(), missingImage.getVisibility());
        }
    }

    /*
     * Stubbing {@link ReferenceManager} to return random file name in order to prevent
     * {@link InvalidReferenceException}
     */
    private void stubReferenceManager() throws InvalidReferenceException {
        isReferenceManagerStubbed = true;

        doReturn(reference).when(referenceManager).DeriveReference(RANDOM_URI);
        doReturn(RANDOM_URI).when(reference).getLocalURI();
        mediaLayout.setReferenceManager(referenceManager);
    }

    /**
     * @param visibility Expected visibility
     * @param views      Views whose actual visibility is to be asserted
     */
    private void assertVisibility(int visibility, View... views) {
        for (View view : views) {
            Assert.assertEquals(visibility, view.getVisibility());
        }
    }
}

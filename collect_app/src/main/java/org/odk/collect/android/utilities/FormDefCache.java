package org.odk.collect.android.utilities;

import android.annotation.SuppressLint;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.odk.collect.android.application.Collect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Methods for reading from and writing to the FormDef cache
 */
public class FormDefCache {
    /**
     * Serializing a FormDef and saving it in cache.
     *
     * @param formDef           - The FormDef to be cached.
     * @param cachedFormDefFile - The File object used for saving the cached FormDef
     */
    @SuppressLint("CheckResult")
    public static void writeCacheAsync(FormDef formDef, File cachedFormDefFile) {
        Observable.create(emitter -> {
            final long formSaveStart = System.currentTimeMillis();
            final File tempCacheFile = File.createTempFile("cache", null,
                    new File(Collect.CACHE_PATH));
            Timber.i("Started saving %s to the cache via temp file %s",
                    formDef.getTitle(), tempCacheFile.getName());

            boolean exceptionCaught = false;
            try {
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(tempCacheFile));
                formDef.writeExternal(dos);
                dos.close();
            } catch (IOException exception) {
                Timber.e(exception);
                exceptionCaught = true;
            }

            // Delete or rename the temp file
            if (exceptionCaught) {
                Timber.i("Deleting no-longer-wanted temp cache file %s for form %s",
                        tempCacheFile.getName(), formDef.getTitle());
                if (!tempCacheFile.delete()) {
                    Timber.e("Unable to delete " + tempCacheFile.getName());
                }
            } else {
                if (tempCacheFile.renameTo(cachedFormDefFile)) {
                    Timber.i("Renamed %s to %s",
                            tempCacheFile.getName(), cachedFormDefFile.getName());
                    Timber.i("Caching %s took %.3f seconds.", formDef.getTitle(),
                            (System.currentTimeMillis() - formSaveStart) / 1000F);
                } else {
                    Timber.e("Unable to rename temporary file %s to cache file %s",
                            tempCacheFile.toString(), cachedFormDefFile.toString());
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    /**
     * If a form is present in the cache, deserializes and returns it as as FormDef.
     *
     * @param formXml a File containing the XML version of the form
     * @return a FormDef, or null if the form is not present in the cache
     */
    public static FormDef readCache(File formXml) {
        final File cachedForm = getCacheFile(formXml);
        if (cachedForm.exists()) {
            Timber.i("Attempting to load %s from cached file: %s.",
                    formXml.getName(), cachedForm.getName());
            final long start = System.currentTimeMillis();
            final FormDef deserializedFormDef = deserializeFormDef(cachedForm);
            if (deserializedFormDef != null) {
                Timber.i("Loaded in %.3f seconds.", (System.currentTimeMillis() - start) / 1000F);
                return deserializedFormDef;
            }

            // An error occurred with deserialization. Remove the file, and make a
            // new .formdef from xml.
            Timber.w("Deserialization FAILED! Deleting cache file: %s",
                    cachedForm.getAbsolutePath());
            cachedForm.delete();
        }
        return null;
    }

    /**
     * Builds and returns a File object for the cached version of a form.
     *
     * @param formXml the File containing the XML form
     * @return a File object
     */
    public static File getCacheFile(File formXml) {
        return new File(Collect.CACHE_PATH + File.separator +
                FileUtils.getMd5Hash(formXml) + ".formdef");
    }

    private static FormDef deserializeFormDef(File serializedFormDef) {
        FileInputStream fis;
        FormDef fd;
        try {
            // create new form def
            fd = new FormDef();
            fis = new FileInputStream(serializedFormDef);
            DataInputStream dis = new DataInputStream(fis);

            // read serialized formdef into new formdef
            fd.readExternal(dis, ExtUtil.defaultPrototypes());
            dis.close();
        } catch (Exception e) {
            Timber.e(e);
            fd = null;
        }

        return fd;
    }
}

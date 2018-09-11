package com.machfour.koala;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utils {
    private Utils() {}

    // Converts a URI into a File object, or returns null if not possible for some reason.
    // If not null, it must be closed by the caller
    // Logs a message if the operation failed
    static @NonNull ParcelFileDescriptor fdFromUri(ContentResolver c, Uri uri, String mode) throws UriOpenException {
        try {
            ParcelFileDescriptor pfd = c.openFileDescriptor(uri, mode);
            if (pfd == null) {
                throw new UriOpenException("ParcelableFileDescriptor was null");
            }
            return pfd;
        } catch (FileNotFoundException e) {
            throw new UriOpenException(e);
        }
    }

    private static Rect convertToAbsolute(RectF proportional, int width, int height) {
        // left, top, right, bottom
        return new Rect(
            Math.round(width*proportional.left),
            Math.round(height*proportional.top),
            Math.round(width*proportional.right),
            Math.round(height*proportional.bottom)
        );
    }

    private static void handleUriOpenException(Uri u, UriOpenException e) {
        Log.w("Utils", String.format("loadImageWithBounds(): Could not open URI: %s. Reason: %s",
                u, e.getLocalizedMessage()));
    }

    private static void handleIoException(IOException e) {
        Log.w("Utils", "loadImageWithBounds(): Error closing parcel file descriptor: "
                + e.getLocalizedMessage());
    }

    /*
     * Roi selects (proportionally) a section of the image to load
     */
    public static @Nullable Bitmap loadImageSelection(ContentResolver c, Uri uri, RectF proportionalRoi) {
        Bitmap b = null;
        try (ParcelFileDescriptor imagePfd = Utils.fdFromUri(c, uri, "r")) {
            FileDescriptor imageFd = imagePfd.getFileDescriptor();
            BitmapFactory.Options opts = new BitmapFactory.Options();

            // Calculate absolute dimensions of region of interest
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(imageFd, null, opts);
            Rect absoluteRoi = convertToAbsolute(proportionalRoi, opts.outWidth, opts.outHeight);

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = 2; // downsample by 2
            b = BitmapRegionDecoder.newInstance(imageFd, true).decodeRegion(absoluteRoi, opts);
        } catch (UriOpenException e1) {
            handleUriOpenException(uri, e1);
        } catch (IOException e2) {
            // IO exception is only thrown on close
            handleIoException(e2);
        }
        return b;

    }
    public static @Nullable Bitmap loadImageWithBounds(ContentResolver c, Uri uri, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height bounds must be positive");
        }
        Bitmap b = null;
        try (ParcelFileDescriptor imagePfd = Utils.fdFromUri(c, uri, "r")) {
            FileDescriptor imageFd = imagePfd.getFileDescriptor();
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();

            // Determine how much to scale down the image
            int scaleFactor;
            {
                // Get the dimensions of the View
                // Get the dimensions of the bitmap
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(imageFd, null, bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;
                // assumes photo is bigger than image view, so the integer division will be >0
                int scaleFactorForLargerPhoto = Math.min(photoW/width, photoH/height);
                // just in case
                scaleFactor = Math.max(1, scaleFactorForLargerPhoto);
            }
            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            b = BitmapFactory.decodeFileDescriptor(imageFd, null, bmOptions);
        } catch (UriOpenException e1) {
            handleUriOpenException(uri, e1);
        } catch (IOException e2) {
            // IO exception is only thrown on close
            handleIoException(e2);
        }
        return b;
    }

    // resolves activities that are able to fulfill the image capture intent
    // returns an array of image capture intents qualified by each such activity,
    // in a form that can be used with Intent.putExtra(EXTRA_INITIAL_INTENTS, ...)
    static Parcelable[] makeImgCaptureIntents(@NonNull PackageManager pm, Uri outputFileUri) {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            // allow writing to URI, because private storage is not normally writable by other apps
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        List<Intent> captureIntents = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentActivities(captureIntent, 0)) {
            // copy intent (copy constructor makes a deep copy)
            Intent intent = new Intent(captureIntent);
            // specify exact component to fulfill intent
            intent.setComponent(new ComponentName(r.activityInfo.packageName, r.activityInfo.name));
            captureIntents.add(intent);
        }
        return captureIntents.toArray(new Parcelable[0]);
    }

    static Intent makeGalleryIntent() {
        return new Intent(Intent.ACTION_GET_CONTENT)
               .setType("image/*")
               .addCategory(Intent.CATEGORY_OPENABLE)
               .putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        //return = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    // MUST be image cache, so that appropriate permissions can be granted
    static File makeImageCaptureFile(Context c) {
        File imageCache = new File(c.getCacheDir(), Config.IMAGE_CACHE_SUBDIR);
        // ensure it exists
        if (!imageCache.exists()) {
            imageCache.mkdir();
        }
        String filename = makeImageCaptureFileName();
        return new File(imageCache, filename);
    }

    private static String makeImageCaptureFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return String.format("img_%s.jpg", timeStamp);
    }
}

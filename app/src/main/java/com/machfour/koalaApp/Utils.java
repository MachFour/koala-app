package com.machfour.koalaApp;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utils {
    private Utils() {}
    private static final String TAG = "Utils";

    // Converts a URI into a File object, or returns null if not possible for some reason.
    // If not null, it must be closed by the caller
    // Logs a message if the operation failed
    private static @NonNull ParcelFileDescriptor fdFromUri(ContentResolver c, Uri uri, String mode) throws UriOpenException {
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

    private static @NonNull InputStream inputStreamFromUri(ContentResolver c, Uri uri) throws UriOpenException {
        try {
            InputStream is = c.openInputStream(uri);
            if (is == null) {
                throw new UriOpenException("InputStream was null");
            }
            return is;
        } catch (FileNotFoundException e) {
            throw new UriOpenException(e);
        }
    }

    @SuppressWarnings("deprecation")
    public static Drawable getDrawable(Resources r, int resid) {
        if (Build.VERSION.SDK_INT >= 21) {
            return r.getDrawable(resid, null);
        } else {
            return r.getDrawable(resid);
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
     * no rotation is done
     */
    public static @Nullable Bitmap loadImageSelection(ContentResolver c, Uri uri, RectF proportionalRoi) {
        Bitmap b = null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        try (ParcelFileDescriptor pfd = Utils.fdFromUri(c, uri, "r")) {
            FileDescriptor imageFd = pfd.getFileDescriptor();
            // Calculate absolute dimensions of region of interest
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(imageFd, null, opts);
            Rect absoluteRoi = convertToAbsolute(proportionalRoi, opts.outWidth, opts.outHeight);
            // now get the real thing
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

    /*
     * Returns one of
     *
     * ExifInterface.ORIENTATION_NORMAL
     * ExifInterface.ORIENTATION_ROTATE_90
     * ExifInterface.ORIENTATION_ROTATE_180
     * ExifInterface.ORIENTATION_ROTATE_270
     *
     * reference:
     * https://stackoverflow.com/questions/14066038
     * /why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
     */
    static int getImageRotation(ContentResolver c, Uri imageUri) { //, @Nullable ParcelFileDescriptor imagePfd) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            ExifInterface ei;
            if (Build.VERSION.SDK_INT > 23) {
                try (InputStream is = c.openInputStream(imageUri)) {
                    if (is != null) {
                        ei = new ExifInterface(is);
                        orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    } else {
                        Log.d(TAG, "getImageRotation(): Content resolver returned null input stream. Returning ORIENTATION_NORMAL");
                    }
                }
            } else {
                ei = new ExifInterface(imageUri.getPath());
                orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (IOException e) {
            Log.d(TAG, "getImageRotation(): IO exception while reading EXIF data. Returning ORIENTATION_NORMAL");
        }

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            case ExifInterface.ORIENTATION_NORMAL:
                /* fall-through */
            default:
                return 0;
        }
    }
    public static @Nullable Bitmap loadRotatedImageWithBounds(ContentResolver c, Uri uri, int width, int height) {
        int rotation = Utils.getImageRotation(c, uri);
        Bitmap unrotated = loadUnRotatedImageWithRotatedBounds(c, uri, width, height, rotation);
        return rotateBitmap(unrotated, rotation);
    }

    public static @Nullable Bitmap loadUnRotatedImageWithRotatedBounds(ContentResolver c, Uri uri, int width, int height, int rotation) {
        // just check to see if the width and height targets need swapping for the rescaling calculation
        int postRotationW;
        int postRotationH;
        switch (rotation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_ROTATE_270:
                // flipped dimensions
                postRotationW = height;
                postRotationH = width;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                postRotationW = width;
                postRotationH = height;
        }
        return loadImageWithBounds(c, uri, postRotationW, postRotationH);
    }

    public static Bitmap rotateBitmap(Bitmap b, int rotation) {
        if (rotation != 0) {
            Matrix m = new Matrix();
            m.setRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            // free memory from old bitmap
            b.recycle();
            return rotated;
        } else {
            return b;
        }
    }

    // assumes no rotation is necessary
    public static @Nullable Bitmap loadImageWithBounds(ContentResolver c, Uri uri, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height bounds must be positive");
        }
        Bitmap b = null;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        try (ParcelFileDescriptor pfd = Utils.fdFromUri(c, uri, "r")) {
            FileDescriptor imageFd = pfd.getFileDescriptor();
            // Determine how much to scale down the image
            // first get the raw width and height of the image
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(imageFd, null, bmOptions);
            int scaleFactor;
            {
                int photoH = bmOptions.outHeight;
                int photoW = bmOptions.outWidth;
                // typically the photo will be bigger than the requested size, so *hopefully*
                // the integer division will have a result greater than zero.
                int scaleFactorForLargerPhoto = Math.max(photoW/width, photoH/height);
                // but just in case...
                scaleFactor = Math.max(1, scaleFactorForLargerPhoto);
            }
            // Decode the image file into a Bitmap approximately sized to fill the View
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

    // files dir  /data/user/0/com.machfour.koala/files
    // external files dir: /storage/emulated/0/Android/data/com.machfour.koala/files

    private static File getTessDataDir(@NonNull File parentDir) {
        File tessdata = new File(parentDir, "tessdata");
        if (!tessdata.exists()) {
            Log.d(TAG, "tessdata dir doesn't exist, creating");
            boolean mkDirSuccess = tessdata.mkdirs();
            if (!mkDirSuccess) {
                Log.w(TAG, "getTessDataDir(): tessdata.mkdirs() failed");
            }
        }
        return tessdata;
    }

    private static File getTessConfigFile(@NonNull File parentDir) {
        return new File(parentDir, "tesseract.config");
    }


    // throws exception if no external files directory. Creates it if it doesn't exist
    private static @NonNull File ensureExternalFilesDir(Context c) {
        File externalFilesDir = c.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            Log.e(TAG, "external files dir was null!");
            throw new IllegalStateException("No external files directory");
        } else if (!externalFilesDir.exists()) {
            boolean mkDirSuccess = externalFilesDir.mkdirs();
            if (!mkDirSuccess) {
                Log.w(TAG, "ensureExternalFilesDir(): externalFilesDir.mkdirs() failed");
            }
        }
        return externalFilesDir;
    }
    static File getTessDataDir(Context c) {
        File parentDir = ensureExternalFilesDir(c);
        return getTessDataDir(parentDir);
    }
    static File getTessConfigFile(Context c) {
        File parentDir = ensureExternalFilesDir(c);
        return getTessConfigFile(parentDir);
    }
}

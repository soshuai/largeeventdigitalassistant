package com.largeevent.management.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 现场拍照转 Base64，供百度人脸比对等场景使用。
 * 处理 EXIF 旋转、缩放与体积限制（百度要求解码后图片不超过 2MB）。
 */
public final class ImageBase64Helper {

    private static final String TAG = "ImageBase64Helper";
    /** 百度人脸比对：图片 Base64 解码后不超过 2MB，留余量 */
    private static final int MAX_IMAGE_BYTES = 1800 * 1024;
    private static final int MAX_LONG_EDGE = 1280;

    private static final OkHttpClient DOWNLOAD_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    private ImageBase64Helper() {
    }

    /**
     * 将证件头像源（Base64 / data:image / HTTP URL / 相对路径）转为 faceMatch 所需的纯 Base64。
     */
    @Nullable
    public static String encodeCertPhotoSourceForFaceMatch(
            @Nullable String photoSource,
            @Nullable String apiBaseUrl) {
        if (TextUtils.isEmpty(photoSource)) {
            return null;
        }
        String trimmed = photoSource.trim();
        if (trimmed.startsWith("data:image") || isLikelyBase64Image(trimmed)) {
            return encodeFromBase64SourceForFaceMatch(trimmed);
        }
        if (isHttpUrl(trimmed)) {
            return encodeFromHttpUrlForFaceMatch(trimmed);
        }
        if (!TextUtils.isEmpty(apiBaseUrl)) {
            return encodeFromHttpUrlForFaceMatch(joinBaseUrl(apiBaseUrl, trimmed));
        }
        Log.w(TAG, "Unsupported cert photo source: " + trimmed);
        return null;
    }

    public static boolean isHttpUrl(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        String u = url.trim();
        return u.startsWith("http://") || u.startsWith("https://");
    }

    @Nullable
    public static String resolveAbsolutePhotoUrl(
            @Nullable String photoSource,
            @Nullable String apiBaseUrl) {
        if (TextUtils.isEmpty(photoSource)) {
            return null;
        }
        String trimmed = photoSource.trim();
        if (isHttpUrl(trimmed)) {
            return trimmed;
        }
        if (!TextUtils.isEmpty(apiBaseUrl)) {
            return joinBaseUrl(apiBaseUrl, trimmed);
        }
        return null;
    }

    /**
     * 下载 HTTP(S) 图片并转为纯 Base64（无 data:image 前缀），适配 faceMatch 接口。
     */
    @Nullable
    public static String encodeFromHttpUrlForFaceMatch(@Nullable String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            return null;
        }
        try {
            byte[] bytes = downloadImageBytes(imageUrl.trim());
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            Log.d(TAG, "Downloaded cert photo bytes=" + bytes.length + ", url=" + imageUrl);
            return encodeJpegBytesForFaceMatch(bytes);
        } catch (Exception e) {
            Log.e(TAG, "encodeFromHttpUrlForFaceMatch failed: " + imageUrl, e);
            return null;
        }
    }

    @Nullable
    private static String encodeFromBase64SourceForFaceMatch(@Nullable String source) {
        if (TextUtils.isEmpty(source)) {
            return null;
        }
        try {
            String base64 = source.trim();
            int comma = base64.indexOf(',');
            if (comma >= 0) {
                base64 = base64.substring(comma + 1);
            }
            byte[] bytes = Base64.decode(base64.trim(), Base64.DEFAULT);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            Log.d(TAG, "Use cert photo base64 bytes=" + bytes.length);
            return encodeJpegBytesForFaceMatch(bytes);
        } catch (Exception e) {
            Log.e(TAG, "encodeFromBase64SourceForFaceMatch failed", e);
            return null;
        }
    }

    /**
     * 将 JPEG 字节转为纯 Base64（无 data:image 前缀），适配 faceMatch 接口。
     */
    @Nullable
    public static String encodeJpegBytesForFaceMatch(@Nullable byte[] jpegBytes) {
        if (jpegBytes == null || jpegBytes.length == 0) {
            return null;
        }
        if (jpegBytes.length <= MAX_IMAGE_BYTES) {
            return Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
        }

        Bitmap decoded = null;
        Bitmap scaled = null;
        try {
            decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
            if (decoded == null) {
                return null;
            }
            scaled = scaleDown(decoded, MAX_LONG_EDGE);
            if (scaled != decoded) {
                decoded.recycle();
                decoded = null;
            }
            byte[] compressed = compressToLimit(scaled, MAX_IMAGE_BYTES);
            if (compressed == null || compressed.length == 0) {
                return null;
            }
            return Base64.encodeToString(compressed, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "encodeJpegBytesForFaceMatch failed", e);
            return null;
        } finally {
            if (scaled != null) {
                scaled.recycle();
            } else if (decoded != null) {
                decoded.recycle();
            }
        }
    }

    /**
     * 将本地 JPEG 转为纯 Base64（无 data:image 前缀），适配 faceMatch 接口。
     */
    @Nullable
    public static String encodeJpegFileForFaceMatch(@Nullable String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            return null;
        }
        File file = new File(imagePath);
        if (!file.exists() || file.length() == 0) {
            Log.w(TAG, "Image file missing: " + imagePath);
            return null;
        }

        int orientation = readExifOrientation(imagePath);
        try {
            if (orientation == ExifInterface.ORIENTATION_NORMAL
                    && file.length() <= MAX_IMAGE_BYTES) {
                byte[] rawBytes = readAllBytes(file);
                if (rawBytes != null && rawBytes.length > 0) {
                    Log.d(TAG, "Use original jpeg bytes, size=" + rawBytes.length);
                    return Base64.encodeToString(rawBytes, Base64.NO_WRAP);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Fast path failed, fallback to decode pipeline", e);
        }

        Bitmap decoded = null;
        Bitmap oriented = null;
        Bitmap scaled = null;
        try {
            decoded = decodeSampledBitmap(imagePath, MAX_LONG_EDGE);
            if (decoded == null) {
                return null;
            }
            oriented = applyExifOrientation(decoded, orientation);
            if (oriented != decoded) {
                decoded.recycle();
                decoded = null;
            }
            scaled = scaleDown(oriented, MAX_LONG_EDGE);
            if (scaled != oriented) {
                oriented.recycle();
                oriented = null;
            }
            byte[] jpegBytes = compressToLimit(scaled, MAX_IMAGE_BYTES);
            if (jpegBytes == null || jpegBytes.length == 0) {
                return null;
            }
            Log.d(TAG, "Encoded jpeg bytes=" + jpegBytes.length + ", base64Len="
                    + Base64.encodeToString(jpegBytes, Base64.NO_WRAP).length());
            return Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "encodeJpegFileForFaceMatch failed", e);
            return null;
        } finally {
            if (scaled != null) {
                scaled.recycle();
            } else if (oriented != null) {
                oriented.recycle();
            } else if (decoded != null) {
                decoded.recycle();
            }
        }
    }

    private static int readExifOrientation(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            return exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    private static boolean isLikelyBase64Image(String value) {
        if (TextUtils.isEmpty(value) || isHttpUrl(value)) {
            return false;
        }
        return value.length() > 64 && !value.contains(" ");
    }

    private static String joinBaseUrl(String baseUrl, String path) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String relative = path.startsWith("/") ? path : ("/" + path);
        return base + relative;
    }

    @Nullable
    private static byte[] downloadImageBytes(String imageUrl) throws IOException {
        Request request = new Request.Builder()
                .url(imageUrl)
                .get()
                .header("User-Agent", "LargeEventManagement/1.0 Android")
                .header("Accept", "image/*,*/*")
                .build();
        try (Response response = DOWNLOAD_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "Download image HTTP " + response.code() + ": " + imageUrl);
                return null;
            }
            return response.body().bytes();
        }
    }

    @Nullable
    private static byte[] readAllBytes(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(buffer);
            if (read <= 0) {
                return null;
            }
            if (read == buffer.length) {
                return buffer;
            }
            byte[] exact = new byte[read];
            System.arraycopy(buffer, 0, exact, 0, read);
            return exact;
        }
    }

    @Nullable
    private static Bitmap decodeSampledBitmap(String imagePath, int maxLongEdge) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds, maxLongEdge);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(imagePath, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int maxLongEdge) {
        int height = options.outHeight;
        int width = options.outWidth;
        int longEdge = Math.max(width, height);
        int sampleSize = 1;
        while (longEdge / sampleSize > maxLongEdge * 2) {
            sampleSize *= 2;
        }
        return Math.max(1, sampleSize);
    }

    private static Bitmap applyExifOrientation(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270f);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1f, -1f);
                break;
            default:
                return bitmap;
        }
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return rotated != null ? rotated : bitmap;
    }

    private static Bitmap scaleDown(Bitmap bitmap, int maxLongEdge) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int longEdge = Math.max(width, height);
        if (longEdge <= maxLongEdge) {
            return bitmap;
        }
        float scale = maxLongEdge / (float) longEdge;
        int targetW = Math.round(width * scale);
        int targetH = Math.round(height * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true);
        return scaled != null ? scaled : bitmap;
    }

    @Nullable
    private static byte[] compressToLimit(Bitmap bitmap, int maxBytes) {
        int quality = 90;
        byte[] best = null;
        while (quality >= 40) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] bytes = baos.toByteArray();
            best = bytes;
            if (bytes.length <= maxBytes) {
                return bytes;
            }
            quality -= 10;
        }
        return best;
    }
}

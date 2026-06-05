package com.example.campusmart.util;

import android.content.Context;
import android.net.Uri;

import com.example.campusmart.R;

public final class ImageUrlUtils {
    private static final String APP_FILES_PREFIX = "/app/files/";

    private ImageUrlUtils() {
    }

    public static String normalize(Context context, String rawUrl) {
        if (rawUrl == null) {
            return null;
        }

        String url = rawUrl.trim();
        if (url.isEmpty()
                || url.startsWith("content:")
                || url.startsWith("file:")
                || url.startsWith("android.resource:")) {
            return rawUrl;
        }

        String baseUrl = context.getString(R.string.base_url).replaceAll("/+$", "");
        int appFilesIndex = url.indexOf(APP_FILES_PREFIX);
        if (appFilesIndex >= 0) {
            return baseUrl + url.substring(appFilesIndex);
        }

        if (url.startsWith("http://minio:9000/") || url.startsWith("https://minio:9000/")) {
            Uri uri = Uri.parse(url);
            String objectName = stripBucket(uri.getPath());
            return toFileUrl(baseUrl, objectName);
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        return toFileUrl(baseUrl, url);
    }

    private static String stripBucket(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replaceAll("^/+", "");
        if (normalized.startsWith("campusmart/")) {
            return normalized.substring("campusmart/".length());
        }
        return normalized;
    }

    private static String toFileUrl(String baseUrl, String objectName) {
        String normalized = stripBucket(objectName);
        if (normalized.startsWith("app/files/")) {
            normalized = normalized.substring("app/files/".length());
        }
        return baseUrl + APP_FILES_PREFIX + encodePath(normalized);
    }

    private static String encodePath(String path) {
        String[] parts = path.replaceAll("^/+", "").split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(Uri.encode(parts[i]));
        }
        return builder.toString();
    }
}

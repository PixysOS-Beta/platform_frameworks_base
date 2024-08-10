/*
 * Copyright (C) 2021 The ProtonAOSP Project
 * Copyright (C) 2022-2024 GrapheneOS
 * Copyright (C) 2024 TheParasiteProject
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

package com.android.internal.util.pixys;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.List;
import java.util.Map;

import com.android.internal.R;

/**
 * @hide
 */
public class PhenotypeFlagsUtils {

    private static final String TAG = PhenotypeFlagsUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String SHARED_PREFS = "SharedPreferences";

    public static final String NAMESPACE_GSERVICES = "gservices";

    public static final String PACKAGE_GMS = "com.google.android.gms";
    public static final String PACKAGE_GSF = "com.google.android.gsf";

    public static final String PHENOTYPE_URI_PREFIX = "content://"
            + PACKAGE_GMS + ".phenotype/";

    public static final String GSERVICES_URI = "content://"
            + PACKAGE_GSF + '.' + NAMESPACE_GSERVICES + "/prefix";

    private static ArrayList<String> getNamespacesList(
            String namespaceArg, boolean isSharedPref) {
        if (namespaceArg == null) return null;

        String[] global =
            Resources.getSystem().getStringArray(R.array.global_phenotype_package_namespaces);
        String[] device =
            Resources.getSystem().getStringArray(R.array.device_phenotype_package_namespaces);
        String[] all = Arrays.copyOf(global, global.length + device.length);
        System.arraycopy(device, 0, all, global.length, device.length);

        final ArrayMap<String, ArrayList<String>> nsMap = new ArrayMap();
        for (String p : all) {
            String[] pn = p.split("=");
            String pkg = pn[0];
            String[] ns = pn[1].split(",");
            for (String n : ns) {
                if (n.startsWith(".")) {
                    nsMap.computeIfAbsent(pkg, k -> new ArrayList<>()).add(pkg + n);
                } else {
                    nsMap.computeIfAbsent(pkg, k -> new ArrayList<>()).add(n);
                }
            }
            nsMap.computeIfAbsent(pkg, k -> new ArrayList<>()).add(pkg);
            nsMap.computeIfAbsent(pkg, k -> new ArrayList<>()).add(pkg + "#" + pkg);
        }

        String pkg = "";
        if (isSharedPref) {
            pkg = namespaceArg.split("/")[0]; // package name
        } else {
            pkg = namespaceArg;
            if (namespaceArg.contains("#")) {
                pkg = namespaceArg.split("#")[1];
            }
        }

        return nsMap.get(pkg);
    }

    private static String[] getFlagsOverride() {
        String[] globalFlags =
            Resources.getSystem().getStringArray(R.array.global_phenotype_flags_override);
        String[] deviceFlags =
            Resources.getSystem().getStringArray(R.array.device_phenotype_flags_override);
        String[] allFlags = Arrays.copyOf(globalFlags, globalFlags.length + deviceFlags.length);
        System.arraycopy(deviceFlags, 0, allFlags, globalFlags.length, deviceFlags.length);
        return allFlags;
    }

    private static final int PHENOTYPE_BASE64_FLAGS = Base64.NO_PADDING | Base64.NO_WRAP;

    private static boolean maybeUpdateMap(
            String namespaceArg,
            @Nullable String[] selectionArgs,
            Map map, boolean isSharedPref) {
        if (namespaceArg == null
            || map == null) {
            return false;
        }

        final ArrayMap<String, ArrayMap<String, Object>> flagMap = new ArrayMap();
        for (String p : getFlagsOverride()) {
            String[] kv = p.split("=");
            String fullKey = kv[0];
            String[] nsKey = fullKey.split("/");

            if (nsKey.length != 3) {
                logd("Invalid config: " + p);
                continue;
            }

            String namespace = nsKey[0];
            String key = nsKey[1];
            String type = nsKey[2];

            Object value = "";
            if (kv.length < 1) {
                flagMap.computeIfAbsent(namespace, k -> new ArrayMap<>()).put(key, value);
                continue;
            }

            if (isSharedPref) {
                if (type.equals("bool")) {
                    value = kv[1].equals("true") ? "1" : "0";
                }
                flagMap.computeIfAbsent(namespace, k -> new ArrayMap<>()).put(key, value);
                continue;
            }

            switch (type) {
                case "int":
                    value = Long.parseLong(kv[1]);
                    break;
                case "bool":
                    value = Boolean.parseBoolean(kv[1]);
                    break;
                case "float":
                    value = Float.parseFloat(kv[1]);
                    break;
                case "string":
                    value = kv[1];
                    break;
                case "extension":
                    value = Base64.decode(kv[1], PHENOTYPE_BASE64_FLAGS);
                    break;
                default:
                    logd("Unsupported type specifier: " + type + " for config: " + p);
                    continue;
            }

            flagMap.computeIfAbsent(namespace, k -> new ArrayMap<>()).put(key, value);
        }

        // Add extra check for gservices flag
        if (selectionArgs != null
            && namespaceArg.equals(NAMESPACE_GSERVICES)) {
            final ArrayMap<String, Object> gflags = flagMap.get(NAMESPACE_GSERVICES);
            if (gflags == null) return false;

            boolean isMapModified = false;
            for (String sel : selectionArgs) {
                for (String key : gflags.keySet()) {
                    if (key.startsWith(sel)) {
                        logd("maybeUpdateMap: " + namespaceArg + "/" + sel);
                        map.put(key, gflags.get(key));
                        isMapModified = true;
                    }
                }
            }

            return isMapModified;
        } else {
            ArrayList<String> namespaces = getNamespacesList(namespaceArg, isSharedPref);
            if (isSharedPref) {
                if (namespaces == null) return false;
    
                String fileName = namespaceArg.split("/")[1]; // file name
                if (!namespaces.contains(fileName)) {
                    return false;
                }
            }

            final ArrayMap<String, Object> pflags = new ArrayMap<>();
            if (namespaces != null) {
                for (String ns : namespaces) {
                    if (!flagMap.keySet().contains(ns)) {
                        continue;
                    }
                    pflags.putAll(flagMap.get(ns));
                }
            }
            if (flagMap.keySet().contains(namespaceArg)) {
                pflags.putAll(flagMap.get(namespaceArg));
            }

            if (!pflags.isEmpty()) {
                map.putAll(pflags);
                return true;
            }
        }

        return false;
    }

    // ContentResolver#query(Uri, String[], Bundle, CancellationSignal)
    public static Cursor maybeModifyQueryResult(
            Uri uri, @Nullable String[] projection,
            @Nullable Bundle queryArgs, @Nullable Cursor origCursor) {

        String uriString = uri.toString();

        Consumer<ArrayMap<String, Object>> mutator = null;

        if (GSERVICES_URI.equals(uriString)) {
            if (queryArgs == null) {
                return null;
            }
            String[] selectionArgs = queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS);
            if (selectionArgs == null) {
                return null;
            }

            mutator = map -> maybeUpdateMap(NAMESPACE_GSERVICES, selectionArgs, map, false);
        } else if (uriString.startsWith(PHENOTYPE_URI_PREFIX)) {
            List<String> path = uri.getPathSegments();
            if (path.size() != 1) {
                Log.e(TAG, "unknown phenotype uri " + uriString, new Throwable());
                return null;
            }

            String namespace = path.get(0);

            mutator = map -> maybeUpdateMap(namespace, null, map, false);
        }

        if (mutator != null) {
            return modifyKvCursor(origCursor, projection, mutator);
        }

        return null;
    }

    private static Cursor modifyKvCursor(@Nullable Cursor origCursor, @Nullable String[] projection,
                                         Consumer<ArrayMap<String, Object>> mutator) {
        final int keyIndex = 0;
        final int valueIndex = 1;
        final int projectionLength = 2;

        if (origCursor != null) {
            projection = origCursor.getColumnNames();
        }

        boolean expectedProjection = projection != null && projection.length == projectionLength
                && "key".equals(projection[keyIndex]) && "value".equals(projection[valueIndex]);

        if (!expectedProjection) {
            Log.e(TAG, "unexpected projection " + Arrays.toString(projection), new Throwable());
            return null;
        }

        final ArrayMap<String, Object> map;
        if (origCursor == null) {
            map = new ArrayMap<>();
        } else {
            map = new ArrayMap<>(origCursor.getColumnCount() + 10);
            try (Cursor orig = origCursor) {
                while (orig.moveToNext()) {
                    String key = orig.getString(keyIndex);
                    String value = orig.getString(valueIndex);

                    map.put(key, value);
                }
            }
        }

        mutator.accept(map);

        final int mapSize = map.size();
        MatrixCursor result = new MatrixCursor(projection, mapSize);

        for (int i = 0; i < mapSize; ++i) {
            Object[] row = new Object[projectionLength];
            row[keyIndex] = map.keyAt(i);
            row[valueIndex] = map.valueAt(i);

            result.addRow(row);
        }

        return result;
    }

    // SharedPreferencesImpl#getAll
    public static void maybeModifySharedPreferencesValues(String path, Map<String, Object> map) {

        if (path == null || !path.endsWith(".xml")) {
            return;
        }

        Map<String, Object> mapTmp = map;
        String[] pathStr = path.split("/");
        String pkg = pathStr[4];
        String fileName = pathStr[pathStr.length - 1];
        // some PhenotypeFlags are stored in SharedPreferences instead of phenotype.db database
        if (maybeUpdateMap(pkg + "/" + fileName, null, mapTmp, true)) {
            map.putAll(mapTmp);
        }
    }

    private static void logd(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}

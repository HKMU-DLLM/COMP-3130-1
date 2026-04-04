package com.example.myapplication;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SchoolRepository {

    private static final String API_URL =
            "https://www.edb.gov.hk/attachment/en/student-parents/sch-info/sch-search/sch-location-info/SCH_LOC_EDB.json";

    private static final String CACHE_FILENAME = "schools_cache.json";

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private final List<School> inMemory = new ArrayList<>();
    private boolean loaded = false;

    public interface LoadCallback {
        void onLoaded(boolean fromCache);
        void onError(String message);
    }

    public interface RefreshCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public SchoolRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public List<School> getAll() {
        return new ArrayList<>(inMemory); // 返回副本，避免同步問題
    }

    public void loadCacheOrFetch(LoadCallback cb) {
        executor.execute(() -> {
            try {
                File f = new File(context.getFilesDir(), CACHE_FILENAME);
                if (f.exists()) {
                    String json = readFile(f);
                    List<School> parsed = parseSchools(json);
                    if (!parsed.isEmpty()) {
                        synchronized (inMemory) {
                            inMemory.clear();
                            inMemory.addAll(parsed);
                        }
                        loaded = true;
                        cb.onLoaded(true);
                        return;
                    }
                }
                // No usable cache, fetch from API
                fetchAndCache(cb);
            } catch (Exception e) {
                cb.onError("Failed to load cache: " + e.getMessage());
            }
        });
    }

    public void refreshFromApi(RefreshCallback cb) {
        executor.execute(() -> fetchAndCache(new LoadCallback() {
            @Override
            public void onLoaded(boolean fromCache) {
                cb.onSuccess();
            }

            @Override
            public void onError(String message) {
                cb.onFailure(message);
            }
        }));
    }

    private void fetchAndCache(LoadCallback cb) {
        try {
            String json = httpGet(API_URL);
            List<School> parsed = parseSchools(json);

            if (parsed.isEmpty()) {
                cb.onError("API returned JSON but parsing produced 0 schools.");
                return;
            }

            writeFile(new File(context.getFilesDir(), CACHE_FILENAME), json);

            synchronized (inMemory) {
                inMemory.clear();
                inMemory.addAll(parsed);
            }
            loaded = true;
            cb.onLoaded(false);
        } catch (Exception e) {
            cb.onError("Failed to fetch from API: " + e.getMessage());
        }
    }

    public List<School> searchLocal(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<School> out = new ArrayList<>();
        if (q.isEmpty()) return out;

        synchronized (inMemory) {
            for (School s : inMemory) {
                if (s.name != null && s.name.toLowerCase(Locale.ROOT).contains(q)) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    public List<School> searchWithFilters(String query, String levelFilter, String categoryFilter) {
        String q = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT));
        boolean allLevels = "All Levels".equals(levelFilter);
        boolean allCategories = "All Categories".equals(categoryFilter);

        List<School> out = new ArrayList<>();

        synchronized (inMemory) {
            for (School s : inMemory) {
                boolean matchName = q.isEmpty() ||
                        (s.name != null && s.name.toLowerCase(Locale.ROOT).contains(q));

                boolean matchLevel = allLevels ||
                        (s.level != null && s.level.equals(levelFilter));

                boolean matchCategory = allCategories ||
                        (s.category != null && s.category.equals(categoryFilter));

                if (matchName && matchLevel && matchCategory) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    // ==================== JSON Parsing ====================
    private List<School> parseSchools(String json) {
        List<School> out = new ArrayList<>();

        JsonElement root = JsonParser.parseString(json);

        if (!root.isJsonArray()) return out;

        JsonArray arr = root.getAsJsonArray();

        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();

            School s = new School();

            s.schoolId = getString(o, "SCHOOL NO.");
            s.name = getString(o, "ENGLISH NAME");
            if (s.name == null) s.name = getString(o, "中文名稱");

            s.category = getString(o, "ENGLISH CATEGORY");
            if (s.category == null) s.category = getString(o, "中文類別");

            s.level = getString(o, "SCHOOL LEVEL");
            if (s.level == null) s.level = getString(o, "學校類型");

            s.address = getString(o, "ENGLISH ADDRESS");
            if (s.address == null) s.address = getString(o, "中文地址");

            s.latitude = getDoubleNullable(o, "LATITUDE");
            if (s.latitude == null) s.latitude = getDoubleNullable(o, "緯度");

            s.longitude = getDoubleNullable(o, "LONGITUDE");
            if (s.longitude == null) s.longitude = getDoubleNullable(o, "經度");

            // 解析學校官方網站
            s.website = getString(o, "WEBSITE");

            out.add(s);
        }

        return out;
    }

    private String getString(JsonObject o, String key) {
        try {
            JsonElement element = o.get(key);
            if (element == null || element.isJsonNull()) return null;
            return element.getAsString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDoubleNullable(JsonObject o, String key) {
        try {
            JsonElement element = o.get(key);
            if (element == null || element.isJsonNull()) return null;
            return element.getAsDouble();
        } catch (Exception e) {
            try {
                String str = o.get(key).getAsString();
                return Double.parseDouble(str);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    // ==================== Network & File IO ====================
    private String httpGet(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP error " + code);
        }

        try (InputStream in = conn.getInputStream()) {
            return streamToString(in);
        }
    }

    private String readFile(File f) throws Exception {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
            return streamToString(fis);
        }
    }

    private void writeFile(File f, String s) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f, false)) {
            fos.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String streamToString(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    // Utilities
    public static String toJson(School s) {
        return new Gson().toJson(s);
    }

    public static School fromJson(String json) {
        return new Gson().fromJson(json, School.class);
    }
}
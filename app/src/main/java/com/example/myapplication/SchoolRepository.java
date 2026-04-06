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

    private static final String API_URL = "https://www.edb.gov.hk/attachment/en/student-parents/sch-info/sch-search/sch-location-info/SCH_LOC_EDB.json";
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

    private static SchoolRepository instance;
    public static synchronized SchoolRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SchoolRepository(context);
        }
        return instance;
    }
    private SchoolRepository(Context context) {
        this.context = context.getApplicationContext();
    }
    public boolean isLoaded() {
        return loaded;
    }

    public List<School> getAll() {
        return new ArrayList<>(inMemory);
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

    public List<School> searchWithFilters(String queryName,
                                          String queryAddress,
                                          String levelFilter,
                                          String categoryFilter,
                                          String genderFilter,
                                          String religionFilter,
                                          String financeFilter,
                                          String sessionFilter,
                                          String districtFilter) {

        String qName = (queryName == null ? "" : queryName.trim().toLowerCase(Locale.ROOT))
                .replace("　", " ");
        String qAddr = (queryAddress == null ? "" : queryAddress.trim().toLowerCase(Locale.ROOT))
                .replace("　", " ");

        String allLvl = context.getString(R.string.filter_all_levels);
        String allCat = context.getString(R.string.filter_all_categories);
        String allGen = context.getString(R.string.filter_all_genders);
        String allRel = context.getString(R.string.filter_all_religions);
        String allFin = context.getString(R.string.filter_all_finances);
        String allSes = context.getString(R.string.filter_all_sessions);
        String allDis = context.getString(R.string.filter_all_districts);

        boolean allLevels = allLvl.equals(levelFilter) || "All Levels".equals(levelFilter);
        boolean allCategories = allCat.equals(categoryFilter) || "All Categories".equals(categoryFilter);
        boolean allGenders = allGen.equals(genderFilter) || "All Genders".equals(genderFilter);
        boolean allReligions = allRel.equals(religionFilter) || "All Religions".equals(religionFilter);
        boolean allFinances = allFin.equals(financeFilter) || "All Finances".equals(financeFilter);
        boolean allSessions = allSes.equals(sessionFilter) || "All Sessions".equals(sessionFilter);
        boolean allDistricts = allDis.equals(districtFilter) || "All Districts".equals(districtFilter);

        List<School> out = new ArrayList<>();

        synchronized (inMemory) {
            for (School s : inMemory) {

                boolean matchName =
                        qName.isEmpty() ||
                                containsIgnoreCase(s.name, qName) ||
                                containsIgnoreCase(s.chineseName, qName);

                boolean matchAddress =
                        qAddr.isEmpty() ||
                                containsIgnoreCase(s.address, qAddr) ||
                                containsIgnoreCase(s.chineseAddress, qAddr);

                // === 關鍵修正：支援同類別多選（逗號分隔）===
                boolean matchLevel = allLevels || matchesAny(s.level, s.chineseLevel, levelFilter);
                boolean matchCategory = allCategories || matchesAny(s.category, s.chineseCategory, categoryFilter);
                boolean matchGender = allGenders || matchesAny(s.gender, s.chineseGender, genderFilter);
                boolean matchReligion = allReligions || matchesAny(s.religion, s.chineseReligion, religionFilter);
                boolean matchFinance = allFinances || matchesAny(s.finance, s.chineseFinance, financeFilter);
                boolean matchSession = allSessions || matchesAny(s.session, s.chineseSession, sessionFilter);
                boolean matchDistrict = allDistricts || matchesAny(s.district, s.chineseDistrict, districtFilter);

                if (matchName && matchAddress && matchLevel && matchCategory && matchGender &&
                        matchReligion && matchFinance && matchSession && matchDistrict) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    // 新增方法：支援逗號分隔的多選
    private boolean matchesAny(String enValue, String zhValue, String filterValue) {
        if (filterValue == null || filterValue.trim().isEmpty()) return true;

        String[] filters = filterValue.split(",");
        for (String f : filters) {
            String trimmed = f.trim();
            if (trimmed.isEmpty()) continue;
            if (equalsIgnoreCase(enValue, trimmed) || equalsIgnoreCase(zhValue, trimmed)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null) return false;
        return text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean equalsIgnoreCase(String schoolValue, String filterValue) {
        if (schoolValue == null || filterValue == null) return false;
        return schoolValue.trim().equalsIgnoreCase(filterValue.trim());
    }

    private List<School> parseSchools(String json) {
        List<School> out = new ArrayList<>();

        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) return out;

            JsonArray arr = root.getAsJsonArray();

            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();

                School s = new School();

                s.schoolId = getString(o, "SCHOOL NO.");

                s.name = getString(o, "ENGLISH NAME");
                s.chineseName = getString(o, "中文名稱");

                s.category = getString(o, "ENGLISH CATEGORY");
                s.chineseCategory = getString(o, "中文類別");

                s.level = getString(o, "SCHOOL LEVEL");
                s.chineseLevel = getString(o, "學校類型");

                s.address = getString(o, "ENGLISH ADDRESS");
                s.chineseAddress = getString(o, "中文地址");

                s.gender = getString(o, "STUDENTS GENDER");
                s.chineseGender = getString(o, "就讀學生性別");

                s.phonenumber = getString(o, "TELEPHONE");

                s.website = getString(o, "WEBSITE");

                s.religion = getString(o, "RELIGION");
                s.chineseReligion = getString(o, "宗教");

                s.district = getString(o, "DISTRICT");
                s.chineseDistrict = getString(o, "分區");

                s.session = getString(o, "SESSION");
                s.chineseSession = getString(o, "學校授課時間");

                s.finance = getString(o,"FINANCE TYPE");
                s.chineseFinance = getString(o, "資助種類");

                s.latitude = getDoubleNullable(o, "LATITUDE");
                if (s.latitude == null) s.latitude = getDoubleNullable(o, "緯度");

                s.longitude = getDoubleNullable(o, "LONGITUDE");
                if (s.longitude == null) s.longitude = getDoubleNullable(o, "經度");

                out.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }

    private String getString(JsonObject o, String key) {
        try {
            JsonElement el = o.get(key);
            if (el == null || el.isJsonNull()) return null;
            String value = el.getAsString();
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDoubleNullable(JsonObject o, String key) {
        try {
            JsonElement el = o.get(key);
            if (el == null || el.isJsonNull()) return null;
            return el.getAsDouble();
        } catch (Exception e) {
            try {
                return Double.parseDouble(o.get(key).getAsString());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

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
        while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toString(StandardCharsets.UTF_8);
    }

    public static String toJson(School s) {
        return new Gson().toJson(s);
    }

    public static School fromJson(String json) {
        return new Gson().fromJson(json, School.class);
    }
}
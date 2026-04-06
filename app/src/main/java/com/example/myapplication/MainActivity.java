package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText searchBox;
    private EditText addresssearchBox;
    private Button btnSearch;
    private Button btnRefresh;
    private ProgressBar progressBar;
    private Spinner spinnerLevel;
    private Spinner spinnerCategory;
    private Spinner spinnerGender;
    private Spinner spinnerReligion;
    private Spinner spinnerFinance;
    private Spinner spinnerSession;
    private Spinner spinnerDistrict;

    private SchoolRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchBox = findViewById(R.id.searchBox);
        addresssearchBox = findViewById(R.id.addresssearchBox);
        btnSearch = findViewById(R.id.btnSearch);
        btnRefresh = findViewById(R.id.btnRefresh);
        progressBar = findViewById(R.id.progressBar);
        spinnerLevel = findViewById(R.id.spinnerLevel);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerReligion = findViewById(R.id.spinnerReligion);
        spinnerFinance = findViewById(R.id.spinnerFinance);
        spinnerSession = findViewById(R.id.spinnerSession);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);

        repo = SchoolRepository.getInstance(this);

        loadOnStartup();

        btnSearch.setOnClickListener(v -> performSearch());

        btnRefresh.setOnClickListener(v -> {
            btnRefresh.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            repo.refreshFromApi(new SchoolRepository.RefreshCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnRefresh.setEnabled(true);
                        setupFilters();
                        Toast.makeText(MainActivity.this, "Data refreshed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnRefresh.setEnabled(true);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void loadOnStartup() {
        progressBar.setVisibility(View.VISIBLE);
        repo.loadCacheOrFetch(new SchoolRepository.LoadCallback() {
            @Override
            public void onLoaded(boolean fromCache) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                    setupFilters();
                    Toast.makeText(MainActivity.this,
                            fromCache ? "Loaded from cache" : "Loaded from API",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupFilters() {
        List<String> levels = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<String> genders = new ArrayList<>();
        List<String> religions = new ArrayList<>();
        List<String> finances = new ArrayList<>();
        List<String> sessions = new ArrayList<>();
        List<String> districts = new ArrayList<>();


        levels.add(getString(R.string.filter_all_levels));
        categories.add(getString(R.string.filter_all_categories));
        genders.add(getString(R.string.filter_all_genders));
        religions.add(getString(R.string.filter_all_religions));
        finances.add(getString(R.string.filter_all_finances));
        sessions.add(getString(R.string.filter_all_sessions));
        districts.add(getString(R.string.filter_all_districts));

        boolean isChinese = getResources().getConfiguration().getLocales().get(0)
                .getLanguage().equals("zh");

        for (School s : repo.getAll()) {
            String currentLevel = isChinese ? s.chineseLevel : s.level;
            String currentCat = isChinese ? s.chineseCategory : s.category;
            String currentGen = isChinese ? s.chineseGender : s.gender;
            String currentFin = isChinese ? s.chineseFinance : s.finance;
            String currentSes = isChinese ? s.chineseSession : s.session;

            String currentRel;
            if (isChinese) {
                if (s.chineseReligion == null || s.chineseReligion.equalsIgnoreCase("N.A.")) {
                    currentRel = "不適用";
                } else {
                    currentRel = s.chineseReligion;
                }
            } else {
                currentRel = (s.religion == null) ? "N.A." : s.religion;
            }            String currentDist = isChinese ? s.chineseDistrict : s.district;

            if (currentLevel != null && !levels.contains(currentLevel)) {
                levels.add(currentLevel);
            }
            if (currentCat != null && !categories.contains(currentCat)) {
                categories.add(currentCat);
            }
            if (currentGen != null && !genders.contains(currentGen)) {
                genders.add(currentGen);
            }
            if (currentRel != null && !religions.contains(currentRel)) {
                religions.add(currentRel);
            }
            if (currentFin != null && !finances.contains(currentFin)) {
                finances.add(currentFin);
            }
            if (currentSes != null && !sessions.contains(currentSes)) {
                sessions.add(currentSes);
            }
            if (currentDist != null && !districts.contains(currentDist)) {
                districts.add(currentDist);
            }
        }

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(levelAdapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> genAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        genAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genAdapter);

        ArrayAdapter<String> relAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, religions);
        relAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReligion.setAdapter(relAdapter);

        ArrayAdapter<String> finAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, finances);
        finAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFinance.setAdapter(finAdapter);

        ArrayAdapter<String> sesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sessions);
        relAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSession.setAdapter(sesAdapter);

        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);
    }

    private void performSearch() {
        String name = searchBox.getText().toString();
        String addr = addresssearchBox.getText().toString();
        String lvl = spinnerLevel.getSelectedItem().toString();
        String cat = spinnerCategory.getSelectedItem().toString();
        String gen = spinnerGender.getSelectedItem().toString();
        String rel = spinnerReligion.getSelectedItem().toString();
        String fin = spinnerFinance.getSelectedItem().toString();
        String ses = spinnerSession.getSelectedItem().toString();
        String dist = spinnerDistrict.getSelectedItem().toString();

        ResultsActivity.start(this, name, addr, lvl, cat, gen, rel, fin, ses, dist);
    }
}
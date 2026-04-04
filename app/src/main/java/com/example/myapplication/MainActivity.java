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
    private Button btnSearch;
    private Button btnRefresh;
    private ProgressBar progressBar;
    private Spinner spinnerLevel;
    private Spinner spinnerCategory;
    private Spinner spinnerDistrict;

    private SchoolRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchBox = findViewById(R.id.searchBox);
        btnSearch = findViewById(R.id.btnSearch);
        btnRefresh = findViewById(R.id.btnRefresh);
        progressBar = findViewById(R.id.progressBar);
        spinnerLevel = findViewById(R.id.spinnerLevel);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);

        repo = new SchoolRepository(this);

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
        List<String> districts = new ArrayList<>();

        levels.add("All Levels");
        categories.add("All Categories");
        districts.add("All Districts");

        for (School s : repo.getAll()) {
            if (s.level != null && !levels.contains(s.level)) levels.add(s.level);
            if (s.chineseLevel != null && !levels.contains(s.chineseLevel)) levels.add(s.chineseLevel);

            if (s.category != null && !categories.contains(s.category)) categories.add(s.category);
            if (s.chineseCategory != null && !categories.contains(s.chineseCategory)) categories.add(s.chineseCategory);

            if (s.district != null && !districts.contains(s.district)) districts.add(s.district);
            if (s.chineseDistrict != null && !districts.contains(s.chineseDistrict)) districts.add(s.chineseDistrict);
        }

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(levelAdapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);
    }

    private void performSearch() {
        if (!repo.isLoaded()) {
            Toast.makeText(this, "Data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        String query = searchBox.getText().toString().trim();
        String levelFilter = spinnerLevel.getSelectedItem().toString();
        String categoryFilter = spinnerCategory.getSelectedItem().toString();
        String districtFilter = spinnerDistrict.getSelectedItem().toString();

        List<School> results = repo.searchWithFilters(query, levelFilter, categoryFilter, districtFilter);

        if (results.isEmpty()) {
            Toast.makeText(this, "No results found with current filters.", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayQuery = query.isEmpty() ? "All Schools" : query;
        ResultsActivity.start(this, displayQuery, results);
    }
}
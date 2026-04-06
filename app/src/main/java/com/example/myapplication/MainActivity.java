package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SchoolRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        repo = SchoolRepository.getInstance(this);

        loadOnStartup();

        binding.btnSearch.setOnClickListener(v -> performSearch());

        binding.btnRefresh.setOnClickListener(v -> {
            binding.btnRefresh.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);

            repo.refreshFromApi(new SchoolRepository.RefreshCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnRefresh.setEnabled(true);
                        setupFilters();
                        Toast.makeText(MainActivity.this, "Data refreshed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(String message) {
                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnRefresh.setEnabled(true);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void loadOnStartup() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repo.loadCacheOrFetch(new SchoolRepository.LoadCallback() {
            @Override
            public void onLoaded(boolean fromCache) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRefresh.setEnabled(true);
                    setupFilters();
                    Toast.makeText(MainActivity.this,
                            fromCache ? "Loaded from cache" : "Loaded from API",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRefresh.setEnabled(true);
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
            }
            String currentDist = isChinese ? s.chineseDistrict : s.district;

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
        binding.spinnerLevel.setAdapter(levelAdapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> genAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        genAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGender.setAdapter(genAdapter);

        ArrayAdapter<String> relAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, religions);
        relAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerReligion.setAdapter(relAdapter);

        ArrayAdapter<String> finAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, finances);
        finAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFinance.setAdapter(finAdapter);

        ArrayAdapter<String> sesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sessions);
        sesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSession.setAdapter(sesAdapter);

        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDistrict.setAdapter(districtAdapter);
    }

    private void performSearch() {
        String name = binding.searchBox.getText().toString();
        String addr = binding.addresssearchBox.getText().toString();
        String lvl = binding.spinnerLevel.getSelectedItem().toString();
        String cat = binding.spinnerCategory.getSelectedItem().toString();
        String gen = binding.spinnerGender.getSelectedItem().toString();
        String rel = binding.spinnerReligion.getSelectedItem().toString();
        String fin = binding.spinnerFinance.getSelectedItem().toString();
        String ses = binding.spinnerSession.getSelectedItem().toString();
        String dist = binding.spinnerDistrict.getSelectedItem().toString();

        ResultsActivity.start(this, name, addr, lvl, cat, gen, rel, fin, ses, dist);
    }
}
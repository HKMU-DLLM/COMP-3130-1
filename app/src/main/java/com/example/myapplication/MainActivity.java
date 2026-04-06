package com.example.myapplication;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SchoolRepository repo;

    private Map<String, List<String>> selectedSubFilters = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        binding.btnFilter.setOnClickListener(v -> showAdvancedFilterDialog());
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

            if (currentLevel != null && !levels.contains(currentLevel)) levels.add(currentLevel);
            if (currentCat != null && !categories.contains(currentCat)) categories.add(currentCat);
            if (currentGen != null && !genders.contains(currentGen)) genders.add(currentGen);
            if (currentRel != null && !religions.contains(currentRel)) religions.add(currentRel);
            if (currentFin != null && !finances.contains(currentFin)) finances.add(currentFin);
            if (currentSes != null && !sessions.contains(currentSes)) sessions.add(currentSes);
            if (currentDist != null && !districts.contains(currentDist)) districts.add(currentDist);
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
        String name = binding.searchBox.getText().toString().trim();
        String addr = binding.addresssearchBox.getText().toString().trim();

        String lvl = getJoinedFilter("Level");
        String cat = getJoinedFilter("Category");
        String gen = getJoinedFilter("Gender");
        String rel = getJoinedFilter("Religion");
        String fin = getJoinedFilter("Finance");
        String ses = getJoinedFilter("Session");
        String dist = getJoinedFilter("District");

        ResultsActivity.start(this, name, addr, lvl, cat, gen, rel, fin, ses, dist);
    }

    private String getJoinedFilter(String key) {
        List<String> list = selectedSubFilters.getOrDefault(key, new ArrayList<>());
        return list.isEmpty() ? "" : String.join(",", list);
    }

    private void showAdvancedFilterDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_advanced_filter, null);

        PopupWindow popupWindow = new PopupWindow(dialogView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setElevation(16);
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAtLocation(binding.getRoot(), Gravity.CENTER, 0, 0);

        LinearLayout leftColumn = dialogView.findViewById(R.id.leftColumn);
        LinearLayout rightColumn = dialogView.findViewById(R.id.rightColumn);
        Button btnReset = dialogView.findViewById(R.id.btnReset);
        Button btnContinue = dialogView.findViewById(R.id.btnContinue);
        Button btnSearchNow = dialogView.findViewById(R.id.btnSearchNow);

        String[] mainFilters = {"Level", "Category", "District", "Gender", "Religion", "Finance", "Session"};

        for (String filter : mainFilters) {
            Button leftButton = new Button(this);
            leftButton.setText(filter);
            leftButton.setOnClickListener(v -> {
                rightColumn.removeAllViews();
                List<String> options = getSubOptions(filter);
                List<String> previouslySelected = selectedSubFilters.getOrDefault(filter, new ArrayList<>());

                for (String option : options) {
                    CheckBox checkBox = new CheckBox(this);
                    checkBox.setText(option);
                    checkBox.setChecked(previouslySelected.contains(option));
                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        List<String> list = selectedSubFilters.getOrDefault(filter, new ArrayList<>());
                        if (isChecked) {
                            if (!list.contains(option)) list.add(option);
                        } else {
                            list.remove(option);
                        }
                        selectedSubFilters.put(filter, list);
                    });
                    rightColumn.addView(checkBox);
                }
            });
            leftColumn.addView(leftButton);
        }

        btnReset.setOnClickListener(v -> {
            selectedSubFilters.clear();
            Toast.makeText(this, "Filter reset", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        btnContinue.setOnClickListener(v -> popupWindow.dismiss());

        btnSearchNow.setOnClickListener(v -> {
            // Search Now 只用 Filter，強制清空 search bar
            binding.searchBox.setText("");
            binding.addresssearchBox.setText("");
            popupWindow.dismiss();
            performSearch();
        });
    }

    private List<String> getSubOptions(String mainFilter) {
        List<String> options = new ArrayList<>();
        for (School s : repo.getAll()) {
            String value = null;
            if (mainFilter.equals("Level")) value = s.level;
            else if (mainFilter.equals("Category")) value = s.category;
            else if (mainFilter.equals("District")) value = s.district;
            else if (mainFilter.equals("Gender")) value = s.gender;
            else if (mainFilter.equals("Religion")) value = s.religion;
            else if (mainFilter.equals("Finance")) value = s.finance;
            else if (mainFilter.equals("Session")) value = s.session;

            if (value != null && !value.isEmpty() && !options.contains(value)) {
                options.add(value);
            }
        }
        return options;
    }
}
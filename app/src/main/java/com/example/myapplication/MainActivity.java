package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class MainActivity extends AppCompatActivity {

        private EditText searchBox;
        private Button btnSearch;
        private Button btnRefresh;
        private ProgressBar progressBar;

        private SchoolRepository repo;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            searchBox = findViewById(R.id.searchBox);
            btnSearch = findViewById(R.id.btnSearch);
            btnRefresh = findViewById(R.id.btnRefresh);
            progressBar = findViewById(R.id.progressBar);

            repo = new SchoolRepository(this);

            loadOnStartup();

            btnSearch.setOnClickListener(v -> {
                if (!repo.isLoaded()) {
                    Toast.makeText(this, "Data not loaded yet.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String q = searchBox.getText().toString().trim();
                if (q.isEmpty()) {
                    Toast.makeText(this, "Enter a school name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<School> results = repo.searchLocal(q);
                if (results.isEmpty()) {
                    Toast.makeText(this, "No results found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Start results screen (no need to create resultsJson here)
                ResultsActivity.start(this, q, results);
            });

            btnRefresh.setOnClickListener(v -> {
                btnRefresh.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);

                repo.refreshFromApi(new SchoolRepository.RefreshCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnRefresh.setEnabled(true);
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

        // Helper payload class for passing list to JSON if you want it; not used below.
        private static class ResultsPayload {
            List<School> results;
            ResultsPayload(List<School> results) { this.results = results; }
        }
    }

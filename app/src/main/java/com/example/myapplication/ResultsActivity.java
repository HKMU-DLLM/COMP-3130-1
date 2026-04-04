package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ResultsActivity extends AppCompatActivity {

    private ListView listView;
    private TextView titleView;

    public static void start(Context context, String query, List<School> results) {
        if (results == null || results.isEmpty()) {
            Toast.makeText(context, "No results found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(context, ResultsActivity.class);

        String safeQuery = (query == null || query.trim().isEmpty()) ? "All Schools" : query.trim();

        String resultsJson = new Gson().toJson(new ResultsWrapper(results));

        i.putExtra("query", safeQuery);
        i.putExtra("resultsJson", resultsJson);

        context.startActivity(i);
    }

    private static class ResultsWrapper {
        List<School> results;
        ResultsWrapper(List<School> results) {
            this.results = results;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        listView = findViewById(R.id.listView);
        titleView = findViewById(R.id.titleView);

        String query = getIntent().getStringExtra("query");
        String resultsJson = getIntent().getStringExtra("resultsJson");

        titleView.setText("Results for: " + (query != null ? query : "All Schools"));

        // 解析結果
        List<School> results = new ArrayList<>();
        try {
            ResultsWrapper wrapper = new Gson().fromJson(resultsJson, ResultsWrapper.class);
            if (wrapper != null && wrapper.results != null) {
                results = wrapper.results;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading results data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (results.isEmpty()) {
            Toast.makeText(this, "No schools found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 建立顯示列表
        List<String> displayList = new ArrayList<>();
        for (School s : results) {
            String address = (s.address != null && !s.address.isEmpty()) ? " - " + s.address : "";
            displayList.add((s.name != null ? s.name : "Unknown School") + address);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        // 點擊進入詳細頁面 - 修正 lambda 變數問題
        final List<School> finalResults = results;   // ← 關鍵修正：改成 effectively final

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= finalResults.size()) return;

            School selected = finalResults.get(position);
            if (selected == null) return;

            String schoolJson = SchoolRepository.toJson(selected);
            Intent intent = new Intent(this, SchoolDetailActivity.class);
            intent.putExtra("schoolJson", schoolJson);
            startActivity(intent);
        });
    }
}
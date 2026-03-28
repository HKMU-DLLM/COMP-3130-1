package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ResultsActivity extends AppCompatActivity {

    private ListView listView;
    private TextView titleView;

    public static void start(Context context, String query, List<School> results) {
        Intent i = new Intent(context, ResultsActivity.class);

        String resultsJson = new com.google.gson.Gson().toJson(new ResultsWrapper(results));
        i.putExtra("query", query);
        i.putExtra("resultsJson", resultsJson);

        context.startActivity(i);
    }

    private static class ResultsWrapper {
        List<School> results;
        ResultsWrapper(List<School> results) { this.results = results; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        listView = findViewById(R.id.listView);
        titleView = findViewById(R.id.titleView);

        String query = getIntent().getStringExtra("query");
        String resultsJson = getIntent().getStringExtra("resultsJson");

        titleView.setText("Results: " + query);

        ResultsWrapper wrapper = new com.google.gson.Gson().fromJson(
                resultsJson, ResultsWrapper.class
        );

        List<School> results = wrapper == null || wrapper.results == null
                ? new ArrayList<>()
                : wrapper.results;

        List<String> rows = new ArrayList<>();
        for (School s : results) {
            String address = (s.address == null) ? "" : (" - " + s.address);
            rows.add(s.name + address);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, rows);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            School selected = results.get(position);

            if (selected == null || selected.latitude == null || selected.longitude == null) {
                Toast.makeText(this, "No coordinates to open map.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = Uri.parse("https://www.google.com/maps?q="
                    + selected.latitude + "," + selected.longitude);

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        });
    }
}
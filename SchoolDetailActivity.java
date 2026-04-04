package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SchoolDetailActivity extends AppCompatActivity {

    private TextView nameView, typeView, addressView, coordView;
    private Button btnOpenMap;
    private Button btnSchoolPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_detail);

        nameView = findViewById(R.id.nameView);
        typeView = findViewById(R.id.typeView);
        addressView = findViewById(R.id.addressView);
        coordView = findViewById(R.id.coordView);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnSchoolPage = findViewById(R.id.btnSchoolPage);

        String schoolJson = getIntent().getStringExtra("schoolJson");
        School s = SchoolRepository.fromJson(schoolJson);

        if (s == null) {
            Toast.makeText(this, "School data error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        nameView.setText(s.name != null ? s.name : "(Unknown School)");

        String type = joinNonNull(" • ", s.category, s.level);
        typeView.setText(type.isEmpty() ? "No type information" : type);

        addressView.setText(s.address != null && !s.address.isEmpty() ? s.address : "(No address available)");

        if (s.latitude != null && s.longitude != null) {
            coordView.setText("Latitude: " + s.latitude + "\nLongitude: " + s.longitude);
        } else {
            coordView.setText("Coordinates not available");
        }

        btnOpenMap.setOnClickListener(v -> {
            if (s.latitude == null || s.longitude == null) {
                Toast.makeText(this, "No coordinates available", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = Uri.parse("https://www.google.com/maps?q=" + s.latitude + "," + s.longitude);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        });

        btnSchoolPage.setOnClickListener(v -> {
            if (s.website != null && !s.website.isEmpty()) {
                Uri uri = Uri.parse(s.website);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No official website available for this school", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String joinNonNull(String sep, String a, String b) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.isEmpty()) sb.append(a);
        if (b != null && !b.isEmpty()) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(b);
        }
        return sb.toString();
    }
}
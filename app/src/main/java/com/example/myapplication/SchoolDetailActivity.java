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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_detail);

        nameView = findViewById(R.id.nameView);
        typeView = findViewById(R.id.typeView);
        addressView = findViewById(R.id.addressView);
        coordView = findViewById(R.id.coordView);
        btnOpenMap = findViewById(R.id.btnOpenMap);

        String schoolJson = getIntent().getStringExtra("schoolJson");
        School s = SchoolRepository.fromJson(schoolJson);

        nameView.setText(s != null && s.name != null ? s.name : "(unknown)");
        typeView.setText(joinNonNull(" • ",
                s != null ? s.category : null,
                s != null ? s.level : null
        ));
        addressView.setText(s != null && s.address != null ? s.address : "(no address)");

        if (s != null && s.latitude != null && s.longitude != null) {
            coordView.setText("Lat: " + s.latitude + "\nLon: " + s.longitude);
        } else {
            coordView.setText("Coordinates not available.");
        }

        btnOpenMap.setOnClickListener(v -> {
            if (s == null || s.latitude == null || s.longitude == null) {
                Toast.makeText(this, "No coordinates to open map.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = Uri.parse("https://www.google.com/maps?q="
                    + s.latitude + "," + s.longitude);

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        });
    }

    private String joinNonNull(String sep, String a, String b) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.isEmpty()) sb.append(a);
        if (b != null && !b.isEmpty()) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(b);
        }
        return sb.length() == 0 ? "" : sb.toString();
    }
}
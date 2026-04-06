package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SchoolDetailActivity extends AppCompatActivity {

    private TextView nameView, typeView, sessionView, phoneView, addressView;
    private Button btnOpenMap;
    private Button btnSchoolPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_detail);

        nameView = findViewById(R.id.nameView);
        typeView = findViewById(R.id.typeView);
        sessionView = findViewById(R.id.sessionView);
        phoneView = findViewById(R.id.phoneView);
        addressView = findViewById(R.id.addressView);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnSchoolPage = findViewById(R.id.btnSchoolPage);

        String schoolJson = getIntent().getStringExtra("schoolJson");
        School s = SchoolRepository.fromJson(schoolJson);

        boolean isChinese = getResources().getConfiguration().getLocales().get(0)
                .getLanguage().equals("zh");

        String displayname = isChinese ? s.chineseName : s.name;
        nameView.setText(displayname);

        String displayfin = isChinese ? s.chineseFinance : s.finance;
        String displaycat = isChinese ? s.chineseCategory : s.category;
        String displaylel = isChinese ? s.chineseLevel : s.level;
        String type = joinNonNull(" • ", displayfin, displaycat, displaylel);
        typeView.setText(type);

        String displayses = isChinese ? s.chineseSession : s.session;
        sessionView.setText(getString(R.string.sessiontitle) + "   " + displayses);

        phoneView.setText(getString(R.string.phonetitle) + "   "+s.phonenumber);

        String displayadd = isChinese ? s.chineseAddress : s.address;
        addressView.setText(getString(R.string.addresstitle) +"  "+displayadd);


        btnOpenMap.setOnClickListener(v -> {
            Uri uri = Uri.parse("geo:" + s.latitude + "," + s.longitude + "?q=" + s.latitude + "," + s.longitude);
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

    private String joinNonNull(String sep, String a, String b, String displaylel) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.isEmpty()) sb.append(a);
        if (b != null && !b.isEmpty()) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(b);
        }
        return sb.toString();
    }
}
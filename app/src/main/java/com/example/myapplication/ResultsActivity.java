package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityResultsBinding;

import java.util.ArrayList;
import java.util.List;

public class ResultsActivity extends AppCompatActivity {

    private ActivityResultsBinding binding;

    public static void start(Context context, String name, String addr, String level,
                             String cat, String gen, String rel, String fin,
                             String ses, String dist) {

        Intent i = new Intent(context, ResultsActivity.class);
        i.putExtra("qName", name);
        i.putExtra("qAddr", addr);
        i.putExtra("qLvl", level);
        i.putExtra("qCat", cat);
        i.putExtra("qGen", gen);
        i.putExtra("qRel", rel);
        i.putExtra("qFin", fin);
        i.putExtra("qSes", ses);
        i.putExtra("qDist", dist);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityResultsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String qName = getIntent().getStringExtra("qName");
        String qAddr = getIntent().getStringExtra("qAddr");
        String qLvl = getIntent().getStringExtra("qLvl");
        String qCat = getIntent().getStringExtra("qCat");
        String qGen = getIntent().getStringExtra("qGen");
        String qRel = getIntent().getStringExtra("qRel");
        String qFin = getIntent().getStringExtra("qFin");
        String qSes = getIntent().getStringExtra("qSes");
        String qDist = getIntent().getStringExtra("qDist");

        String displayQuery = (qName != null && !qName.isEmpty()) ? qName : getString(R.string.resultall);
        binding.titleView.setText(getString(R.string.resulttitle) + " " + displayQuery);

        SchoolRepository repo = SchoolRepository.getInstance(this);
        List<School> results = repo.searchWithFilters(qName, qAddr, qLvl, qCat, qGen, qRel, qFin, qSes, qDist);

        if (results.isEmpty()) {
            Toast.makeText(this, "No schools found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean isZh = getResources().getConfiguration().getLocales().get(0).getLanguage().equals("zh");
        List<String> displayList = new ArrayList<>();

        for (School s : results) {
            String name = isZh ? s.chineseName : s.name;
            String addr = isZh ? s.chineseAddress : s.address;
            String suffix = (addr != null && !addr.isEmpty() && !addr.equalsIgnoreCase("N.A.")) ? " - " + addr : "";
            displayList.add(name + suffix);
        }

        binding.listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList));

        binding.listView.setOnItemClickListener((parent, view, position, id) -> {
            School selected = results.get(position);
            Intent intent = new Intent(this, SchoolDetailActivity.class);
            intent.putExtra("schoolJson", SchoolRepository.toJson(selected));
            startActivity(intent);
        });

        // 返回按鈕（放在下方）
        binding.btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
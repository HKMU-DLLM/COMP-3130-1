package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ActivityResultsBinding;

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
        String displayQuery = (qName != null && !qName.isEmpty()) ? qName : getString(R.string.resultall);
        binding.titleView.setText(getString(R.string.resulttitle) + " " + displayQuery);

        SchoolRepository repo = SchoolRepository.getInstance(this);
        List<School> results = repo.searchWithFilters(
                getIntent().getStringExtra("qName"),
                getIntent().getStringExtra("qAddr"),
                getIntent().getStringExtra("qLvl"),
                getIntent().getStringExtra("qCat"),
                getIntent().getStringExtra("qGen"),
                getIntent().getStringExtra("qRel"),
                getIntent().getStringExtra("qFin"),
                getIntent().getStringExtra("qSes"),
                getIntent().getStringExtra("qDist")
        );

        if (results.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_results), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ResultsAdapter adapter = new ResultsAdapter(results, this::onSchoolClicked);
        binding.recyclerView.setAdapter(adapter);
        binding.btnBack.setText(getString(R.string.back_to_search));
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void onSchoolClicked(School school) {
        Intent intent = new Intent(this, SchoolDetailActivity.class);
        intent.putExtra("schoolJson", SchoolRepository.toJson(school));
        startActivity(intent);
    }
}
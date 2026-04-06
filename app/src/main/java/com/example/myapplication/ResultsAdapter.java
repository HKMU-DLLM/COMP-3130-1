package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {

    private final List<School> schools;
    private final OnSchoolClickListener listener;

    public interface OnSchoolClickListener {
        void onSchoolClick(School school);
    }

    public ResultsAdapter(List<School> schools, OnSchoolClickListener listener) {
        this.schools = schools;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_school_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        School school = schools.get(position);
        holder.bind(school);
    }

    @Override
    public int getItemCount() {
        return schools.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView addressText;
        private final TextView categoryText;
        private final TextView districtText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            addressText = itemView.findViewById(R.id.addressText);
            categoryText = itemView.findViewById(R.id.categoryText);
            districtText = itemView.findViewById(R.id.districtText);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSchoolClick(schools.get(position));
                }
            });
        }

        void bind(School school) {
            boolean isChinese = itemView.getContext().getResources()
                    .getConfiguration().getLocales().get(0).getLanguage().equals("zh");

            String name = isChinese ? school.chineseName : school.name;
            String address = isChinese ? school.chineseAddress : school.address;
            String category = isChinese ? school.chineseCategory : school.category;
            String district = isChinese ? school.chineseDistrict : school.district;

            nameText.setText(name != null ? name : "Unknown School");
            addressText.setText(address != null ? address : "");
            categoryText.setText(category != null ? category : "");
            districtText.setText(district != null ? district : "");
        }
    }
}
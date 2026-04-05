package com.example.myapplication;

public class School {
    public String schoolId;
    public String name;
    public String chineseName;
    public String category;
    public String chineseCategory;
    public String level;
    public String chineseLevel;
    public String address;
    public String chineseAddress;
    public String gender;
    public String chineseGender;
    public String phonenumber;
    public String website;
    public String religion;
    public String chineseReligion;
    public Double latitude;
    public Double longitude;
    public String district;
    public String chineseDistrict;

    public String getDisplayName() {
        return (name != null && !name.trim().isEmpty()) ? name : chineseName;
    }

    public String getDisplayAddress() {
        return (address != null && !address.trim().isEmpty()) ? address : chineseAddress;
    }

    public String getDisplayType() {
        String type = "";
        if (category != null && !category.trim().isEmpty()) type += category;
        if (level != null && !level.trim().isEmpty()) {
            if (!type.isEmpty()) type += " • ";
            type += level;
        }
        return type.isEmpty() ? "No type information" : type;
    }
}
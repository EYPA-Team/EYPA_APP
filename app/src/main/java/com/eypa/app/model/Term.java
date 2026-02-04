package com.eypa.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * 分类(Category)和标签(Tag)的通用基类.
 * 包含一个关键的 "taxonomy" 字段，用于在反序列化时区分具体类型.
 */
public class Term {
    protected int id;
    protected String name;

    @SerializedName("taxonomy")
    protected String taxonomy;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTaxonomy() {
        return taxonomy;
    }
}
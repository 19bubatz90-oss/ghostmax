package com.ghostmax;

public class CustomProvider {
    public String name, baseUrl, apiKey, model, category;

    public CustomProvider(String name, String baseUrl, String apiKey, String model, String category) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.category = category;
    }
}

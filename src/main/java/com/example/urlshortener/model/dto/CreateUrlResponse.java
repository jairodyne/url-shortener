package com.example.urlshortener.model.dto;

public class CreateUrlResponse {

    private final String shortUrl;
    private final String code;
    private final String url;

    public CreateUrlResponse(String shortUrl, String code, String url) {
        this.shortUrl = shortUrl;
        this.code = code;
        this.url = url;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public String getCode() {
        return code;
    }

    public String getUrl() {
        return url;
    }
}
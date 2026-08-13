package com.example.urlshortener.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "urls",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_urls_code", columnNames = "code")
        })
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    protected UrlEntity() {
    }

    public UrlEntity(String code, String originalUrl, long createdAt) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.clickCount = 0;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getClickCount() {
        return clickCount;
    }
}

package com.example.urlshortener.api;

import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class UrlShortenerApiIT {

    private static final String BASE = System.getProperty("it.baseUrl", "http://localhost:8080/url-shortener");

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = BASE;
    }

    @Test
    public void createsUrlWithoutAliasAndRedirects() {
String shortUrl = given()
                .contentType("application/json")
                .body(map("url", "https://example.com/original/path"))
                .when()
                .post("/api/urls")
                .then()
                .statusCode(201)
                .body("code", notNullValue())
                .body("shortUrl", startsWith(BASE + "/r/"))
                .body("url", equalTo("https://example.com/original/path"))
                .extract()
                .path("shortUrl");

        given()
                .redirects().follow(false)
                .when()
                .get(shortUrl)
                .then()
                .statusCode(302)
                .header("Location", equalTo("https://example.com/original/path"));
    }

    @Test
    public void createsUrlWithCustomAlias() {
        String alias = "site-" + System.nanoTime();

        given()
                .contentType("application/json")
                .body(map("url", "https://example.com", "alias", alias))
                .when()
                .post("/api/urls")
                .then()
                .statusCode(201)
                .body("code", equalTo(alias))
                .body("shortUrl", equalTo(BASE + "/r/" + alias));
    }

    @Test
    public void rejectsTakenAliasWith409() {
        String alias = "ocupado-" + System.nanoTime();
        given().contentType("application/json").body(map("url", "https://a.com", "alias", alias))
                .when().post("/api/urls").then().statusCode(201);

        given().contentType("application/json").body(map("url", "https://b.com", "alias", alias))
                .when().post("/api/urls")
                .then().statusCode(409).body("message", notNullValue());
    }

    @Test
    public void rejectsInvalidUrlWith400() {
        given()
                .contentType("application/json")
                .body(map("url", "sem-scheme"))
                .when()
                .post("/api/urls")
                .then()
                .statusCode(400)
                .body("message", notNullValue());
    }

    @Test
    public void returns404ForUnknownCode() {
        when()
                .get("/r/naoexiste123")
                .then()
                .statusCode(404);
    }

    private static Map<String, String> map(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static Map<String, String> map(String key1, String v1, String key2, String v2) {
        Map<String, String> map = new HashMap<>();
        map.put(key1, v1);
        map.put(key2, v2);
        return map;
    }
}
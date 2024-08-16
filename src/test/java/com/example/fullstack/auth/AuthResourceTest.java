package com.example.fullstack.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class AuthResourceTest {
    @Test
    void loginValidCredentials() {
        given()
                .body("{\"name\":\"admin\",\"password\":\"quarkus\"}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body(Matchers.not(Matchers.emptyString()));
    }
    @Test
    void loginInvalidCredentials() {
        given()
                .body("{\"name\":\"admin\",\"password\":\"not-quarkus\"}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/auth/login")
                .then()
                .statusCode(401);
    }
}